/*========================================================================
 * ResourceHelper.java
 * June 6, 2013 ttiemens
 *========================================================================
 * This file is part of ClockInJava2.
 *
 * Copyright (c) 2013, Tim Tiemens
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * You should have received a copy of the BSD-2-Clause license
 * along with this program.  If not, see <http://opensource.org/licenses/BSD-2-Clause>.
 *    
 *    Release History:
 *    v1.0.0    Initial Release  
 *              2168b72f926a8b4b0bbb52f491e4883fdc45ee10 git sha ClockInJava2
 *    v1.0.1    Create the builder, debug dump structure, debug runtime methods
 *              cfe32062c0e6b3f374061fa1bf75ca8d250710ee git sha ClockInJava2
 *    v1.0.2    Update license
 *    
 */

package util;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;


/**
 * 
 *  <b>Basic Problem:</b> 
 *  You want the program to get resources from the file system (src/main/resources) while in development, 
 *   but you want the program to get resources from a single jar while in production,
 *   without having to change the program.
 *  And you're tired of writing the code that does this task.
 *  
 *  <b>Advanced Problem:</b>
 *  You want the program to load "default" resources from its .jar file, 
 *   but also allow an "override" resource file to be placed on the file system, and used instead.
 *  And you're tired of writing the code that does this task.
 *   
 *  <b>Getting Started</b>.
 *  <li>Copy this file into your project.
 *      "The BSD License allows proprietary use and allows the software released under the license to be 
 *          incorporated into proprietary products." 
 *
 *  <li>(Optional) Copy src/main/resources/images/icon/spinner.gif into your project
 *       and run
 *       $ java -cp build/classes util.ResourceHelper\$MyMain
 *       and make sure it ends with "Image load success"
 *        
 *  <li>Wire in your ResourceHelper
 *  <pre>
 *   public static class MyMain {
 *       public static void main(String[] arguments) throws Exception {
 *           ResourceLoader loader = ResourceHelper.createClassResourceFileSystem(MyMain.class, "", "src/main/resources/");
 *           Image image = ImageIO.read(loader.getResource("images/icons/spinner.gif"));
 *           System.out.println("Image load success, info=" + image.toString());
 *       }
 *   }
 *  </pre>
 *  Then, create a file "spinner.gif", and place it at src/main/resources/images/icons/spinner.gif
 * 
 *  If you have no build system, then when you run your program, it will load it from the 'src/' directory.
 *  Otherwise, the build system should copy resources into your classes directory, and it will load from there.
 *  Finally, if you place it all into a .jar, then MyMain.class will load the image as a resource from the .jar file.
 * 
 * 
 *  <b>Next Step</b>.
 *  Look at ResourceHelper.builder() sub-system for examples of how to create really complex sequences of
 *  ResourceLoader instances.  Mix-and-match to create all combinations from Class, FileSystem, and Zip loaders.
 *  
 *  <b>One Last Special Case</b>.
 *  Sometimes, you just want to put your resource data into .java directly.
 *  See the class RhHardCodedContentLoader, which provides a starting point, and allows you to place base-64 encoded
 *    data into your .java, and return it as a resource.
 * 
 * @author timtiemens
 * @version 1.0.2
 *
 */
public class ResourceHelper {

    public interface ResourceLoader {
        /**
         * @param name that identifies the resource (e.g. a path like "a/b/c.gif")
         * @return null if this resource could not be resolved, otherwise an open InputStream
         * @throws nothing since it returns null on error conditions
         */
        public InputStream getResource(final String name);

    }

    // ===========

    public static ResourceHelperBuilder builder() {
        return new ResourceHelperBuilder();
    }

    // ===========

    /**
     * Simple builder example.
     * 
     * @param prefixes on file system, e.g. "", "./src/main/resources/", "/opt/stuff/", etc.  
     *       END with "/" - since best practice is for the resource name to NOT begin with a "/", the prefix must END with a "/".
     *       START with "/" - your choice, starting with "/" will make an absolute path, otherwise you get path relative to the
     *                         current working directory. 
     * @return resource loader that will try each file system location, in order given
     */
    public static ResourceLoader buildFileSystem(String... prefixes) {
        ResourceHelperBuilder builder = new ResourceHelperBuilder();

        for (String prefix : prefixes) {
            builder.lookInFileSystem(prefix);
        }

        return builder.build();
    }

    /**
     * Complex builder example.
     * 
     * @param inZipFileName file name (not the full path) of a .zip or .jar file that contains resources
     *     The "full path" is determined by "inClassForResource", "inFilePrefix" and "inFilePrefix2".
     * @param inZipNeedsGzipUncompress if true, uses a GZipInputStream (".gz") to decompress the file
     *                                 if null, use true if the name ends in ".gz", otherwise false
     * @param inZipCacheAll if true, load every resource in the zip/jar file once and cache it in memory [doubles ram required]
     *                      if false, parse the zip/jar file each time a resource is requested
     * @param inClassForResource use this class's .getResourceAsStream(name) method
     * @param inFilePrefix check the file system using this prefix first [usually "" or "./"]
     * @param inFilePrefix2 then check the file system using this prefix [usually "src/main/resources/"]
     * @return resource loader that works very hard to find the requested resource
     */
    public static ResourceLoader buildZipfileLoader(String inZipFileName, Boolean inZipNeedsGzipUncompress, boolean inZipCacheAll,
                                                    Class<?> inClassForResource, 
                                                    String inFilePrefix, String inFilePrefix2) {
        ResourceHelperBuilder builder = new ResourceHelperBuilder();

        return builder
                .lookInClassResource(inClassForResource)
                .lookInFileSystem(inFilePrefix, inFilePrefix2)
                .nameThat("BaseClassResourceFileSystem")
                .lookInZipFile(inZipFileName, inZipNeedsGzipUncompress, builder.previousNamedLoader(), inZipCacheAll)
                .nameThat("ZipFileLoader")
                .combineNamedLoaders("ZipFileLoader", "BaseClassResourceFileSystem")
                .build();
    }

    /**
     * @return really really complex ResourceLoader, which uses a zip file first, and then the file system,
     *          but also looks for the zip file in a different location
     */
    public static ResourceLoader exampleBuildZipThenFileSystemThenClassResource() {
        ResourceHelperBuilder builder = new ResourceHelperBuilder();

        return builder
                // look in images.zip.gz
                //   to find images.zip.gz, look in class first, then file system
                .lookInClassResource(ResourceHelper.class)
                .lookInFileSystem("", "build/jars/")
                .nameThat("ZipFileSource")
                .lookInZipFile("images.zip.gz", true, builder.previousNamedLoader(), true)
                .nameThat("ZipFile")
                
                 // look in filesystem, then class 
                .lookInFileSystem("", "src/main/resources/", "/opt/prog/resources/")
                .lookInClassResource(ResourceHelper.class)
                .nameThat("IndividualLoader")
                
                // final loader: look in "zip" first, then "individual" 
                .combineNamedLoaders("ZipFile", builder.previousNamedLoader())
                .build();
    }

    // =========== builder examples end

    // =========== create factory methods start

    public static ResourceLoader createClassResourceFileSystem(Class<?> classForResource, String filePrefix, String filePrefix2) {
        ResourceLoader ret;

        ResourceLoader classLoader = new RhClassLoader(classForResource);
        ResourceLoader fileLoader = new RhFileSystem();


        ret = new RhChainWrapper(createCRLRelative(classLoader),      // wrapPrefix(classLoader, ""),
                                 createCRLAbsolute(classLoader),      // wrapPrefix(classLoader, "/"),
                                 wrapPrefix(fileLoader, filePrefix),  // usually "" or "./"
                                 wrapPrefix(fileLoader, filePrefix2)  // usually "src/main/resources"
                );

        return ret;
    }

    public static ResourceLoader createZipfileLoader(String inZipFileName, boolean inZipNeedsGzipUncompress, boolean inZipCacheAll,
                                                     Class<?> classForResource, String filePrefix, String filePrefix2) {
        ResourceLoader ret;
        ResourceLoader wrapped = createClassResourceFileSystem(classForResource, filePrefix, filePrefix2);
        ResourceLoader sourceOfZipStream = new RhFixedNameLoader(wrapped, inZipFileName);

        ResourceLoader fromzip;
        if (inZipCacheAll) {
            // this version loads all entries at once 
            fromzip = new RhZipfileCacheAllLoader(sourceOfZipStream, inZipNeedsGzipUncompress);
        } else {
            // this version re-reads the zip file for each request
            fromzip = new RhZipfileLoader(sourceOfZipStream, inZipNeedsGzipUncompress);
        }

        ret = new RhChainWrapper(fromzip,
                wrapped);

        return ret;
    }


    /**
     * Create a "relative" class-based loader.  i.e. one "root".
     * 
     * Directory/Jar structure for getResource("images/icons/c.png"):
     *    /com
     *      /foo
     *        /BaseLoader.class
     *        /images
     *          /icons
     *            /c.png
     *    
     * @param basis loader
     * @return wrapped with "" as a prefix
     */
    private static ResourceLoader createCRLRelative(ResourceLoader basis) {
        return wrapPrefix(basis, "");
    }

    /**
     * Create an "absolute" class-based loader.  i.e. two "roots".
     * 
     * Directory/Jar structure for getResource("images/icons/c.png"):
     *    /com
     *      /foo
     *        /BaseLoader.class
     *    /images
     *      /icons
     *        /c.png 
     *    
     * @param basis loader
     * @return wrapped with "/" as a prefix
     */
    private static ResourceLoader createCRLAbsolute(ResourceLoader basis) {
        return wrapPrefix(basis, "/");
    }

    /**
     * Wrap with a hard-coded "/".
     * 
     * @param basis loader 
     * @return a loader that puts "/" in front of the requested name
     */
    public static ResourceLoader wrapPrefixSlash(ResourceLoader basis) {
        return createCRLAbsolute(basis);
    }

    /**
     * Wrap with an arbitrary string.
     * 
     * @param basis loader
     * @param prefix to put in front of every requested name
     * @return a loader that puts prefix in front of the requested name
     */
    public static ResourceLoader wrapPrefix(ResourceLoader basis, String prefix) {
        return new RhPrefixNameWrapper(basis, prefix);
    }

    // =========== create factory methods end
    
    // =========== classes intended for (public) extension start
    
    /**
     * Abstract base class for creating "hard-coded" resource loaders.
     * I.e. the resource is base64 encoded in the actual .java file itself
     *
     */
    public static abstract class RhHardCodedContentLoader extends RhBaseAbstract implements ResourceLoader, ResourceLoaderDebug {
        
        /**
         * Your responsibility as a subclass: provide the bytes for a requested path.
         * It is up to you to decide how many paths you want to serve.
         * 
         * @param path that identifies the resource
         * @return the byte array for the requested resource
         */
        public abstract byte[] getResourceBytes(String path);
        
        /**
         * You are also responsible for a 1-line description of this loader [for troubleshooting].
         * @see util.ResourceHelper.RhBaseAbstract#loaderDescription()
         */
        public abstract String loaderDescription();
        
        
        public final InputStream getResource(String path) {
            InputStream ret = null;
            byte[] bytes = getResourceBytes(path);
            if (bytes != null) {
                ret = new ByteArrayInputStream(bytes);
            } else {
                ret = null;
            }
            logGetResourceConcrete(path, ret);
            return ret;
        }
        
        public final byte[] frombase64(String... lines) {
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                sb.append(line);
            }
            String lexicalXSDBase64Binary = sb.toString();
            return javax.xml.bind.DatatypeConverter.parseBase64Binary(lexicalXSDBase64Binary);
        }
        
        @Override
        public final List<?> dumpStructure() {
            return Collections.singletonList(loaderDescription());
        }

    }
    
    // =========== classes intended for extension end
    
    // =========== concrete implementation classes start

    /*default*/ static abstract class RhBaseAbstract {
        public abstract String loaderDescription();

        /**
         * Make note of an IOException where we were "really trying", i.e. it is a real exception.
         * @param name of the resource being read
         * @param e exception
         */
        public final void noteIOException(String name, IOException e) {
        }
        
        /**
         * Make note of a "fake" IOException, i.e. from the result of a .close().
         * @param name of the resource being read
         * @param e exception
         */
        public final void noteIOExceptionIgnorable(String name, IOException e) {
        }
        public final void logGetResource(String path) {
            log("", path);
        }
        public final void logGetResourceConcrete(String path, InputStream result) {
            log("result is " + ((result == null) ? "empty" : "success"), path);
        }

        /**
         * Used to capture information about initialization of the loader.
         * @param info line
         */
        public final void logInitialize(String info) {
            log(info, NO_PATH_MARKER);
        }

        /**
         * Specialized for file system loader.
         * @param file being accessed
         */
        public final void attemptFor(File file) {
            log("Loading from file " + file.getPath(), NO_PATH_MARKER);
        }
        
        
        private void log(String line, String optionalPath) {
            String prefix = loaderDescription();
            if (optionalPath != NO_PATH_MARKER) {
                prefix = prefix + "(" + optionalPath + ")";
            }
            prefix = prefix + " ";
            System.out.println(prefix + line);
        }
        private static final String NO_PATH_MARKER = "**nopath**##";
    }

    /*default*/ static abstract class RhWrapperAbstract extends RhBaseAbstract implements ResourceLoader, ResourceLoaderDebug {
        /*default*/ final ResourceLoader wrapped;
        public RhWrapperAbstract(final ResourceLoader wrap) {
            wrapped = wrap;
        }
        public abstract String dumpStructureSubclass();

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public final List<?> dumpStructure() {
            List ret = new ArrayList();
            ret.add(dumpStructureSubclass());
            if (wrapped instanceof ResourceLoaderDebug) {
                ret.add(( (ResourceLoaderDebug) wrapped).dumpStructure());
            }
            return ret;
        }
    }

    public static class RhFileSystem extends RhBaseAbstract implements ResourceLoader, ResourceLoaderDebug {
        @Override
        public InputStream getResource(String name) {
            InputStream ret = null;
            File file = new File(name);
            attemptFor(file);
            if (file.exists()) {
                if (file.canRead()) {
                    try {
                        ret = new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        noteIOException(name, e);
                    }
                }
            }
            logGetResourceConcrete(name, ret);
            return ret;
        }

        @Override
        public String loaderDescription() {
            return "FileSystem";
        }

        @Override
        public List<String> dumpStructure() {
            return Collections.singletonList(loaderDescription());
        }
    }

    public static class RhFixedNameLoader extends RhWrapperAbstract implements ResourceLoader {
        private final String fixedName;
        public RhFixedNameLoader(final ResourceLoader wrapped, final String inFixedName) {
            super(wrapped);
            fixedName = inFixedName;
        }

        @Override
        public InputStream getResource(String name) {
            logGetResource(name);
            // NOTE: we purposely IGNORE the name parameter.
            // This implementation uses a FIXED name parameter.
            return wrapped.getResource(fixedName);
        }

        @Override
        public String loaderDescription() {
            return "FixedNamed(" + fixedName + ")";
        }
        @Override
        public String dumpStructureSubclass() {
            return loaderDescription();
        }
    }

    public static class RhZipfileLoader extends RhWrapperAbstract implements ResourceLoader {
        private boolean needsGzipUncompress;
        public RhZipfileLoader(final ResourceLoader sourceOfZipStream, final boolean inNeedsGzipUncompress) {
            super(sourceOfZipStream);
            needsGzipUncompress = inNeedsGzipUncompress;
        }

        @Override
        public String loaderDescription() {
            return "ZipfileLoader(" + needsGzipUncompress + ")";
        }

        @Override
        public String dumpStructureSubclass() {
            return loaderDescription();
        }

        @Override
        public InputStream getResource(String name) {
            InputStream ret = null;
            InputStream is = getSourceOfZipStream().getResource(name);
            ZipInputStream zis = createZipInputStream(is);

            if (zis != null) {
                ZipEntry entry;
                try {
                    entry = zis.getNextEntry();
                    while ((ret == null) && (entry != null)) {
                        if (entry.getName().equals(name)) {
                            ret = entryToInputStream(zis, entry);
                        }
                        entry = zis.getNextEntry();
                    }
                } catch (IOException e) {
                    noteIOException(name, e);
                }
            }
            try {
                if (is != null) {
                    is.close();
                    is = null;
                }
            } catch (IOException e) {
                noteIOExceptionIgnorable(name, e);
            }
            logGetResourceConcrete(name, ret);
            return ret;
        }
        private InputStream entryToInputStream(ZipInputStream stream, ZipEntry zipEntry) throws IOException {
            // Does not work:
            //return stream;

            byte[] buf = entryToByteArray(stream, zipEntry);
            return new ByteArrayInputStream(buf);
        }
        public final byte[] entryToByteArray(ZipInputStream stream, ZipEntry zipEntry) throws IOException {
            return streamToByteArray(stream);
        }
        public final byte[] streamToByteArray(InputStream stream) throws IOException {
            byte[] ret;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024 * 16];
            int read;
            try {
                while ((read = stream.read(buf)) > 0) {
                    bos.write(buf, 0, read);
                }
                bos.flush();
                ret = bos.toByteArray();
            } finally {
                if (bos != null) {
                    try {
                        bos.close();
                    } catch (IOException e) {
                        noteIOExceptionIgnorable("streamToByteArray", e);
                    }
                }
            }

            return ret;
        }

        public final ResourceLoader getSourceOfZipStream() {
            return wrapped;
        }

        /*default*/ final ZipInputStream createZipInputStream(InputStream is) {
            ZipInputStream ret = null;
            if (is != null) {
                if (needsGzipUncompress) {
                    try {
                        is = new GZIPInputStream(is);
                    } catch (IOException e) {
                        // probably not actually a .gz file:
                        noteIOException("createZipInputStream", e);
                    }  finally {
                        
                    }
                }
                ret = new ZipInputStream(is);
            }
            return ret;
        }
    }

    /**
     * A version of the .zip/.jar loader that loads ALL of the available resources at creation, 
     * in order to avoid re-parsing for every single .getResource() call.
     *
     */
    public static class RhZipfileCacheAllLoader extends RhZipfileLoader implements ResourceLoader {
        private Map<String, byte[]> path2buf = null;
        private boolean initialized = false;

        public RhZipfileCacheAllLoader(final ResourceLoader sourceOfZipStream, final boolean inNeedsGzipUncompress) {
            super(sourceOfZipStream, inNeedsGzipUncompress);
        }

        private void initialize(String name) {
            if (! initialized) {
                initialized = true;
                path2buf = new HashMap<String, byte[]>();

                InputStream is = getSourceOfZipStream().getResource(name);
                ZipInputStream zis = createZipInputStream(is);

                if (zis != null) {
                    ZipEntry entry;
                    try {
                        entry = zis.getNextEntry();
                        while (entry != null) {

                            byte[] buf = entryToByteArray(zis, entry);
                            logInitialize("precaching entry.name=" + entry.getName() + " isDirectory=" + entry.isDirectory() + " .size=" + entry.getSize() + " bufsize=" + buf.length);
                            if (! entry.isDirectory()) {
                                path2buf.put(entry.getName(), buf);
                            }

                            entry = zis.getNextEntry();
                        }
                    } catch (IOException e) {
                        logInitialize("precaching failed with IOException: " + e);
                        noteIOException(name, e);
                    }
                }
                try {
                    if (zis != null) {
                        zis.close();
                        zis = null;
                    }
                } catch (IOException e) {
                    noteIOExceptionIgnorable(name, e);
                }
            }
        }

        @Override
        public InputStream getResource(String name) {
            InputStream ret;
            initialize(name);

            if (path2buf.containsKey(name)) {
                ret = new ByteArrayInputStream(path2buf.get(name));
            } else {
                ret = null;
            }
            logGetResourceConcrete(name, ret);
            return ret;
        }
    }

    public static class RhClassLoader extends RhBaseAbstract implements ResourceLoader, ResourceLoaderDebug {
        private final Class<?> classForResourceLoading;
        public RhClassLoader(final Class<?> inClassForResourceLoading) {
            this.classForResourceLoading = inClassForResourceLoading;
        }
        public Class<?> getClassForResourceLoading() {
            return classForResourceLoading;
        }
        @Override
        public InputStream getResource(String name) {
            InputStream ret = getClassForResourceLoading().getResourceAsStream(name);
            logGetResourceConcrete(name, ret);
            return ret;
        }

        @Override
        public String loaderDescription() {
            return "ClassLoader(" + classForResourceLoading.getName() + ")";
        }

        @Override
        public List<?> dumpStructure() {
            return Collections.singletonList(loaderDescription());
        }
    }


    public static class RhChainWrapper extends RhBaseAbstract implements ResourceLoader, ResourceLoaderDebug {
        private final List<ResourceLoader> list;
        public RhChainWrapper(List<ResourceLoader> inList) {
            list = new ArrayList<ResourceLoader>();
            list.addAll(inList);
        }
        public RhChainWrapper(ResourceLoader... inList)
        {
            list = new ArrayList<ResourceLoader>();
            for (ResourceLoader item : inList) {
                list.add(item);
            }
        }
        @Override
        public InputStream getResource(String name) {
            logGetResource(name);
            InputStream ret = null;
            for (ResourceLoader current : list) {
                ret = current.getResource(name);
                if (ret != null) {
                    break;
                }
            }
            return ret;
        }

        @Override
        public String loaderDescription() {
            return "ChainWrapper(size=" + list.size() + ")";
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public final List<?> dumpStructure() {
            List ret = new ArrayList();
            ret.add(loaderDescription());
            for (ResourceLoader current : list) {
                if (current instanceof ResourceLoaderDebug) {
                    ret.add(((ResourceLoaderDebug) current).dumpStructure());
                }
            }
            return ret;
        }

    }

    public static class RhPrefixNameWrapper extends RhWrapperAbstract implements ResourceLoader, ResourceLoaderDebug {
        private final String prefix;
        public RhPrefixNameWrapper(final ResourceLoader inWrapped, final String inPrefix) {
            super(inWrapped);
            this.prefix = inPrefix;
        }
        @Override
        public InputStream getResource(String name) {
            String passOnName = prefix + name;
            logGetResource(passOnName);
            return wrapped.getResource(passOnName);
        }
        @Override
        public String loaderDescription() {
            return "PrefixName(" + prefix + ")";
        }

        @Override
        public String dumpStructureSubclass() {
            return loaderDescription();
        }
    }


    /**
     * Provide helper that converts InputStream into useful types like Image, Properties, etc.
     *
     */
    public static class RhTypeConverterWrapper extends RhWrapperAbstract implements ResourceLoader {
        public RhTypeConverterWrapper(final ResourceLoader basis) {
            super(basis);
        }

        public String dumpStructureSubwrapper() {
            return "ConverterWrapper()";
        }

        @Override
        public InputStream getResource(String name) {
            return wrapped.getResource(name);
        }

        public Image getResourceAsImage(String name) {
            Image ret = null;
            InputStream is = getResource(name);
            if (is != null) {
                try {
                    ret = ImageIO.read(is);
                } catch (IOException e) {
                    noteIOException(name, e);
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
            return ret;
        }

        // ENHANCEMENT: public java.lang.Property getResourceAsProperty(String name)
        // ENHANCEMENT: ?public Zipfileinputstream getResourceAsZipfileInputstream(String name)


        @Override
        public String loaderDescription() {
            return "ConverterWrapper(type-specific-helper-methods)";
        }


        @Override
        public String dumpStructureSubclass() {
            return loaderDescription();
        }
    }


    public static class ResourceHelperBuilder {
        private List<ResourceLoader> preparing = new ArrayList<ResourceLoader>();
        private Map<String, ResourceLoader> name2loader = new HashMap<String, ResourceLoader>();
        private String lastName = null;

        public ResourceHelperBuilder() {

        }

        public ResourceLoader build() {
            return build(preparing);
        }

        public ResourceHelperBuilder combineNamedLoaders(String... nameOfBuilder) {
            List<ResourceLoader> lst = new ArrayList<ResourceLoader>();
            for (String name : nameOfBuilder) {
                lst.add(name2loader.get(name));
            }
            ResourceLoader add = new RhChainWrapper(lst);
            preparing.add(add);
            return this;
        }

        public ResourceHelperBuilder lookInZipFileDoNotPrecache(String name, boolean needsGzip, String previousNamedLoader) {
            return lookInZipFile(name, needsGzip, previousNamedLoader, false);
        }
        
        public ResourceHelperBuilder lookInZipFileDoPrecache(String name, boolean needsGzip, String previousNamedLoader) {
            return lookInZipFile(name, needsGzip, previousNamedLoader, true);
        }
        
        public ResourceHelperBuilder lookInZipFile(String zipFilename, Boolean needsGzipUncompress, String previousNamedLoader, boolean preCache) {
            if (needsGzipUncompress == null) {
                needsGzipUncompress = zipFilename.endsWith(".gz");
            }
            ResourceLoader basis = name2loader.get(previousNamedLoader);
            ResourceLoader sourceOfZipStream = new RhFixedNameLoader(basis, zipFilename);
            ResourceLoader add;
            if (preCache) {
                add = new RhZipfileCacheAllLoader(sourceOfZipStream, needsGzipUncompress);
            } else {
                add = new RhZipfileLoader(sourceOfZipStream, needsGzipUncompress);
            }
            preparing.add(add);
            return this;
        }
        
        public String previousNamedLoader() {
            return lastName;
        }
        
        public ResourceHelperBuilder nameThat(String name) {
            ResourceLoader fromList = build(preparing);
            preparing.clear();
            name2loader.put(name, fromList);
            lastName = name;
            return this;
        }

        public ResourceHelperBuilder lookInFileSystem(String... locations) {
            ResourceLoader basis = new RhFileSystem();

            for (String prefix : locations) {
                boolean endsWithSeparator = prefix.endsWith("/") || prefix.endsWith("\\");
                boolean isempty = prefix.isEmpty();
                if (!isempty && !endsWithSeparator) {
                    throw exceptionCreate("File system prefix location did not end with file separator: '" + prefix + "'");
                }
                if (prefix != null) {
                    preparing.add(wrapPrefix(basis, prefix));
                }
            }

            return this;
        }

        public ResourceHelperBuilder lookInClassResource(Class<?> class1) {
            ResourceLoader basis = new RhClassLoader(class1);

            preparing.add(wrapPrefix(basis, ""));
            preparing.add(wrapPrefix(basis, "/"));

            return this;
        }
        
        private ResourceLoader build(List<ResourceLoader> fromlist) {
            if (fromlist.size() == 1) {
                return fromlist.get(0);
            } else if (fromlist.size() > 1) {
                return new RhChainWrapper(preparing);
            } else {
                throw exceptionCreate("Builder.build called with empty prepared list");
            }
        }
        
    }

    public static void main(String[] args) throws Exception {
        ResourceLoader rl = createClassResourceFileSystem(ResourceHelper.class, "", "src/main/resources/");
        String name = "images.zip.gz";
        InputStream is = rl.getResource(name);
        System.out.println("load(" + name + ") result is " + ( (is == null) ? "null" : "notnull"));

        boolean inNeedsGzipUncompress = name.endsWith(".gz");
        boolean inLoadAllAtOnce = true;
        ResourceLoader zl = createZipfileLoader(name, inNeedsGzipUncompress, inLoadAllAtOnce, 
                                                ResourceHelper.class, "", "src/main/resources/");
        String imgname = "images/small_6x9/black/8.gif";
        imgname = "images/lcd_14x23/gray/0.gif";
        imgname = "images/hand_26x31/6.gif";
        
        is = zl.getResource(imgname);
        System.out.println("load(" + imgname + ") result is " + ( (is == null) ? "null" : "notnull"));
        if (is != null) {
            Image image = ImageIO.read(is);
            System.out.println("Image load, image=" + ( (image == null) ? "null" : image.toString()));
        }
        String smallimgname = "images/small_6x9/black/sep.gif";
        ResourceLoader hcoded = new ExampleStaticContent();
        Image fromZip = ImageIO.read(zl.getResource(smallimgname));
        Image fromStatic = ImageIO.read(hcoded.getResource(smallimgname));
        System.out.println("ZIP Image load,    image=" + ( (fromZip == null) ? "null" : fromZip.toString()));        
        System.out.println("STATIC Image load, image=" + ( (fromStatic == null) ? "null" : fromStatic.toString()));
        // BufferedImage.equals is not implemented, this is always 'false':
        //System.out.println("Images are " + ((fromZip.equals(fromStatic) ? "equal" : "not equal")));
    }
    
    public static class ExampleStaticContent extends RhHardCodedContentLoader {

        @Override
        public byte[] getResourceBytes(String path) {
            byte[] ret = null;
            String[] pieces = path.split("/");
            String last = pieces[pieces.length - 1];
            if (last.equals("sep.gif")) {
                ret = frombase64("R0lGODlhBgAJAIAAAAAAAP///yH5BAkAAAEALAAAAAAGAAkAAAIIjI8IkO3KXioA",
                                 "Ow==");
            } else if (last.equals("pos.gif")) {
                ret = frombase64("R0lGODlhBgAJAIAAAAAAAP///yH5BAkAAAEALAAAAAAGAAkAAAILjI8IuQb/TpOp",
                                 "ogIAOw==");
            } else {
                ret = null;
            }
            return ret;
        }

        @Override
        public String loaderDescription() {
            return "HardCoded[sep.gif, pos.gif]";
        }
    }

    // =========== exception start
    private static RuntimeException exceptionCreate(String msg) {
        return new ResourceHelper.ResourceHelperException(msg);
    }
    public static class ResourceHelperException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ResourceHelperException() {
            super();
        }

        public ResourceHelperException(String message, Throwable cause) {
            super(message, cause);
        }

        public ResourceHelperException(String message) {
            super(message);
        }

        public ResourceHelperException(Throwable cause) {
            super(cause);
        }
    }
    
    // =========== debug structure start
    
    public interface ResourceLoaderDebug {
        /**
         * @return debug information about the structure and sub-structure of this loader as a "tree"
         *     Type of ? is either String or List<String>
         *     i.e. final type is either List<String> or List<List<String>>
         */
        public List<?> dumpStructure();
    }


    public static class ResourceLoaderDebugUtil {

        public static void dump(ResourceLoader zl) {
            if (zl instanceof ResourceLoaderDebug) { 
                List<?> root = ((ResourceLoaderDebug) zl).dumpStructure();
                List<String> lines = new ArrayList<String>();
                dump(0, lines, root);
                for (String line : lines) {
                    System.out.println(line);
                }
            }
        }

        private static String indent(int depth) {
            return "                                                 ".substring(0, depth * 2);
        }

        @SuppressWarnings("rawtypes")
        private static void dump(int depth, List<String> lines, List<?> list) {
            for (Object obj : list) {
                if (obj instanceof String) {
                    String add = indent(depth) + (String) obj;
                    lines.add(add);
                } else if (obj instanceof List) {
                    List sublist = (List) obj;
                    dump(depth +1, lines, sublist);
                } else {
                    throw exceptionCreate("subclass failed: class=" + obj.getClass().getName());
                }
            }
        }
    }
    
    // =========== debug structure end
    
    
    public static class MyMain {
        public static void main(String[] arguments) throws Exception {
            ResourceLoader loader = ResourceHelper.createClassResourceFileSystem(MyMain.class, "", "src/main/resources/");
            Image image = ImageIO.read(loader.getResource("images/icons/spinner.gif"));
            System.out.println("Image load success, info=" + image.toString());
        }
    }
}
