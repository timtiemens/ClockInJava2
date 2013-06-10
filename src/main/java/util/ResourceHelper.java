/*========================================================================
 * ResourceHelper.java
 * June 6, 2013 ttiemens
 * Copyright (c) 2013 Tim Tiemems
 *========================================================================
 * This file is a "standalone" product.
 * Current distributions: part of ClockInJava2
 *
 *    ResourceHelper is released to the public domain.
 *    
 *    I, the copyright holder of this work, hereby release it into the public domain. This applies worldwide.
 *    In case this is not legally possible, I grant any entity the right to use this work for any purpose, 
 *    without any conditions, unless such conditions are required by law.

 *    ResourceHelper is a "project in and of itself".
 *    It is the result of multiple frustrations of writing a utility that can load a resource either
 *    from the file system or from a class's resource loader, without making the caller care about that detail.  
 *
 *    ResourceHelper is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *    
 *    Release History:
 *    v1.0.0    Initial Release
 *    
 *    Md5sum (without the md5sum line itself)
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
	
	public static ResourceLoader wrapPrefixSlash(ResourceLoader basis) {
		return wrapPrefix(basis, "/");
	}
	public static ResourceLoader wrapPrefix(ResourceLoader basis, String prefix) {
		return new RhPrefixNameWrapper(basis, prefix);
	}
	
	public static ResourceLoader createResourceFileSystem(Class<?> classForResource, String filePrefix, String filePrefix2) {
		ResourceLoader ret;
		
		ResourceLoader classLoader = new RhClassLoader(classForResource);
		ResourceLoader fileLoader = new RhFileSystem();
		
		
		ret = new RhChainWrapper(wrapPrefix(classLoader, ""),
					             wrapPrefix(classLoader, "/"),
				                 wrapPrefix(fileLoader, filePrefix),
					             wrapPrefix(fileLoader, filePrefix2)
				);
		
		return ret;
	}

	public static ResourceLoader createZipfileLoader(String inZipFileName, boolean inZipNeedsGzipUncompress, boolean inZipCacheAll,
			                                                  Class<?> classForResource, String filePrefix, String filePrefix2) {
		ResourceLoader ret;
		ResourceLoader wrapped = createResourceFileSystem(classForResource, filePrefix, filePrefix2);
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
	
	public static void main(String[] args) throws Exception {
		ResourceLoader rl = createResourceFileSystem(ResourceHelper.class, "", "src/main/resources/");
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
	}
	
	// ===========	

	public static class RhFileSystem implements ResourceLoader {
		@Override
		public InputStream getResource(String name) {
			InputStream ret = null;
			File file = new File(name);
			attemptFor("File", file);
			if (file.exists()) {
				if (file.canRead()) {
					try {
						ret = new FileInputStream(file);
					} catch (FileNotFoundException e) {
						noteIOException(name, e);
					}
				}
			}
			return ret;
		}
		/*default*/ void attemptFor(String name, File file) {
			System.out.println("Loading from file " + file.getPath());
		}
		/*default*/ void noteIOException(String name, IOException e) {
			
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
			// NOTE: we purposely IGNORE the name parameter
			return wrapped.getResource(fixedName);
		}		
	}

	public static class RhZipfileLoader extends RhWrapperAbstract implements ResourceLoader {
		private boolean needsGzipUncompress;
		public RhZipfileLoader(final ResourceLoader sourceOfZipStream, final boolean inNeedsGzipUncompress) {
			super(sourceOfZipStream);
			needsGzipUncompress = inNeedsGzipUncompress;
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
			return ret;
		}
		// First attempt: this fails in strange ways - some images work, some throw "Stream closed"
		@SuppressWarnings("unused")
		private InputStream entryToInputStream2(ZipInputStream stream, ZipEntry entry) {
			InputStream ret = null;
			long size = entry.getSize();
			System.out.println("size is " + size);

			ret = stream;
			return ret;
		}
		// Second attempt: this fails in strange ways - some images work, some throw "Stream closed"
		@SuppressWarnings("unused")		
		private InputStream entryToInputStream3(ZipInputStream stream, ZipEntry entry) {
			InputStream ret = null;
			long size = entry.getSize();
			System.out.println("Number of bytes = " + size);
			byte[] buf = new byte[(int) size];
			try {
				stream.read(buf);
				ret  = new ByteArrayInputStream(buf);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
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
							System.out.println("cZIP: looking at entry.name=" + entry.getName() + " isDirectory=" + entry.isDirectory() + " .size=" + entry.getSize() + " bufsize=" + buf.length);
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
			initialize(name);

			if (path2buf.containsKey(name)) {
				return new ByteArrayInputStream(path2buf.get(name));
			} else {
				return null;
			}
		}	


	}
	
	public static class RhClassLoader implements ResourceLoader {
		private final Class<?> classForResourceLoading;
		public RhClassLoader(final Class<?> inClassForResourceLoading) {
			this.classForResourceLoading = inClassForResourceLoading;
		}
		public Class<?> getClassForResourceLoading() {
			return classForResourceLoading;
		}
		@Override
		public InputStream getResource(String name) {
			attemptFor("Class", name);
			return getClassForResourceLoading().getResourceAsStream(name);
		}
		/*default*/ void attemptFor(String where, String name) {
			System.out.println("Loading from class " + name);
		}
	}
	
	public static abstract class RhWrapperAbstract implements ResourceLoader {
		/*default*/ final ResourceLoader wrapped;
		public RhWrapperAbstract(final ResourceLoader wrap) {
			wrapped = wrap;
		}
	}
	
	public static class RhChainWrapper implements ResourceLoader {
		private final List<ResourceLoader> list;
		public RhChainWrapper(ResourceLoader... inList)
		{
			list = new ArrayList<ResourceLoader>();
			for (ResourceLoader item : inList) {
				list.add(item);
			}
		}
		@Override
		public InputStream getResource(String name) {
			InputStream ret = null;
			for (ResourceLoader current : list) {
				ret = current.getResource(name);
				if (ret != null) {
					break;
				}
			}
			return ret;
		}
	}
	
	public static class RhPrefixNameWrapper extends RhWrapperAbstract implements ResourceLoader {
		private final String prefix;
		public RhPrefixNameWrapper(final ResourceLoader inWrapped, final String inPrefix) {
			super(inWrapped);
			this.prefix = inPrefix;
		}
		@Override
		public InputStream getResource(String name) {
			return wrapped.getResource(prefix + name);
		}
	}
	

	public static class RhTypeConverterWrapper extends RhWrapperAbstract implements ResourceLoader {
		public RhTypeConverterWrapper(final ResourceLoader basis) {
			super(basis);
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
		
		
		/*default*/ void noteIOException(String name, IOException e) {
			
		}
	}
	
}
