/*========================================================================
 * ResourceHelper.java
 * June 6, 2013 ttiemens
 * Copyright (c) 2013 Tim Tiemems
 *========================================================================
 * This file is part of ClockInJava2.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *    
 *    Release History:
 *    v1.0.0    Initial Release  
 *              2168b72f926a8b4b0bbb52f491e4883fdc45ee10 git sha ClockInJava2
 *    v1.0.1    Create the builder, debug dump structure, debug runtime methods
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
 * @author timtiemens
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
     * @param inZipFileName (short) name of a .zip or .jar file that contains resources
     * @param inZipNeedsGzipUncompress if true, uses a GZipInputStream (".gz") to decompress the file
     *                                 if null, use true if the name ends in ".gz", otherwise false
     * @param inZipCacheAll if true, load every resource in the zip/jar file once and cache it in memory [doubles ram required]
     *                      if false, parse the zip/jar file each time a resource is requested
     * @param inClassForResource use this class's .getResourceAsStream(name) method
     * @param inFilePrefix check the file system using this prefix first
     * @param inFilePrefix2 then check the file system using this prefix
     * @return resource loader that works very hard to find the requested resource
     */
    public static ResourceLoader buildZipfileLoader(String inZipFileName, Boolean inZipNeedsGzipUncompress, boolean inZipCacheAll,
                                                    Class<?> inClassForResource, String inFilePrefix, String inFilePrefix2) {
        ResourceHelperBuilder builder = new ResourceHelperBuilder();

        return builder
                .lookInClassResource(ResourceHelper.class)
                .lookInFileSystem(inFilePrefix, inFilePrefix2)
                .nameThat("BaseClassResourceFileSystem")
                .lookInZipFile(inZipFileName, inZipNeedsGzipUncompress, builder.previousNamedLoader(), inZipCacheAll)
                .nameThat("ZipFileLoader")
                .combineNamedLoaders("ZipFileLoader", "BaseClassResourceFileSystem")
                .build();
    }

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
     * Create a "relative" class-based loader.
     * 
     * Directory/Jar structure for getResource("images/b/c.png"):
     *    /com/a/b/BaseLoader.class
     *    /com/a/b/images/b/c.png
     *    
     * @param baseLoader
     * @return wrapped with "" as a prefix
     */
    private static ResourceLoader createCRLRelative(ResourceLoader baseLoader) {
        return wrapPrefix(baseLoader, "");
    }

    /**
     * Create an "absolute" class-based loader.
     * 
     * Directory/Jar structure for getResource("images/b/c.png"):
     *    /com/a/b/BaseLoader.class
     *    /images/b/c.png
     *    
     * @param baseLoader
     * @return wrapped with "/" as a prefix
     */
    private static ResourceLoader createCRLAbsolute(ResourceLoader baseLoader) {
        return wrapPrefix(baseLoader, "/");
    }

    /**
     * Wrap with a hard-coded "/".
     * 
     * @param basis loader 
     * @return a loader that puts "/" in front of the requested name
     */
    public static ResourceLoader wrapPrefixSlash(ResourceLoader basis) {
        return wrapPrefix(basis, "/");
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
    
    // =========== classes intended for extension start
    
    public static abstract class RhHardCodedContentLoader extends RhBaseAbstract implements ResourceLoader, ResourceLoaderDebug {
        public abstract byte[] getResourceBytes(String path);
        
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
        
        public byte[] frombase64(String... lines) {
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                sb.append(line);
            }
            String lexicalXSDBase64Binary = sb.toString();
            return javax.xml.bind.DatatypeConverter.parseBase64Binary(lexicalXSDBase64Binary);
        }
        
        @Override
        public List<?> dumpStructure() {
            return Collections.singletonList(loaderDescription());
        }

    }
    
    // =========== classes intended for extension end
    
    // =========== concrete implementation classes start

    /*default*/ static abstract class RhBaseAbstract {
        public abstract String loaderDescription();


        public final void noteIOException(String name, IOException e) {
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

    public static abstract class RhWrapperAbstract extends RhBaseAbstract implements ResourceLoader, ResourceLoaderDebug {
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
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            try {
                if (is != null) {
                    is.close();
                    is = null;
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            logGetResourceConcrete(name, ret);
            return ret;
        }
        private InputStream entryToInputStream(ZipInputStream stream, ZipEntry zipEntry) {
            // Does not work:
            //return stream;

            byte[] buf = entryToByteArray(stream, zipEntry);
            return new ByteArrayInputStream(buf);
        }
        public final byte[] entryToByteArray(ZipInputStream stream, ZipEntry zipEntry) {

            return streamToByteArray(stream);
        }
        public final byte[] streamToByteArray(InputStream stream) {
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
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                ret = null;
            } finally {
                if (bos != null) {
                    try {
                        bos.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }

            return ret;
        }

        public final byte[] streamToByteArrayList(InputStream stream) {
            byte[] ret = null;
            List<Byte> list = new ArrayList<Byte>();
            byte[] buf = new byte[1024 * 16];
            int read;

            try {
                while ( (read = stream.read(buf)) > 0)
                {
                    for (int i = 0, n = read; i < n ; i++) {
                        list.add(buf[i]);
                    }
                }
                ret = new byte[list.size()];
                int i = 0;
                for (Byte b : list) {
                    ret[i] = b;
                    i++;
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
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
                        e.printStackTrace();
                    }
                }
                ret = new ZipInputStream(is);
            }
            return ret;
        }
    }

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
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                try {
                    if (zis != null) {
                        zis.close();
                        zis = null;
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
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
            logGetResource(name);
            return wrapped.getResource(prefix + name);
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

        // TODO: java.lang.Property
        // TODO: Zipfileinputstream?


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
                if (prefix != null) {
                    preparing.add(wrapPrefix(basis, prefix));
                }
            }

            return this;
        }

        public ResourceHelperBuilder lookInClassResource(Class<ResourceHelper> class1) {
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
                throw new RuntimeException("Builder.build called with empty prepared list");
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
                    throw new RuntimeException("subclass failed: class=" + obj.getClass().getName());
                }
            }
        }
    }
    
    // =========== debug structure end
    
}
