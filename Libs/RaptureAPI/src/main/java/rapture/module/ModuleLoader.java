/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package rapture.module;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;

/**
 * The module loader is used to load jars from within jars and to put them on
 * the class path, and to put a jar file itself on the classpath.
 * 
 * A jar file can have jar files in the "lib" folder of the jar. Any objects
 * with an extension of "jar" will be extracted to a temp location and then that
 * jar will be added using the same module loader (in case of dependencies).
 * 
 * All files added to the classpath are remembered in ModuleLoader so we don't
 * go into a loop.
 * 
 * This class is instantiated as a singleton, and not necessarily via
 * RaptureKernel, as it needs to be used in ReflexRunner
 * 
 * @author amkimian
 * 
 */
public class ModuleLoader {
    private static Logger log = Logger.getLogger(ModuleLoader.class);
    private static final Class<?>[] parameters = new Class[] { URL.class };

    public ModuleLoader() {
    }

    public boolean addJar(String jarFile) throws IOException {
        // Look at the contents of the jar file for anything in the lib folder,
        // extract that into a temp area and call addJar
        // on that.
        // Then add this to the classpath
        log.debug(String.format(Messages.getString("ModuleLoader.loadingJar"), jarFile)); //$NON-NLS-1$
        addJarResource(new File(jarFile));
        return true;
    }

    private static boolean isJar(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".jar"); //$NON-NLS-1$
    }

    public static void addURL(URL u) throws IOException {
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<?> sysclass = URLClassLoader.class;

        log.debug(String.format(Messages.getString("ModuleLoader.AddingToClasspath"), u.toString())); //$NON-NLS-1$
        try {
            Method method = sysclass.getDeclaredMethod("addURL", parameters); //$NON-NLS-1$
            method.setAccessible(true);
            method.invoke(sysloader, new Object[] { u });
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException(Messages.getString("ModuleLoader.errorSystemClassloader")); //$NON-NLS-1$
        }

    }

    private void addJarResource(File file) throws IOException {
        JarFile jarFile = new JarFile(file);
        addURL(file.toURI().toURL());
        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            if (!jarEntry.isDirectory() && isJar(jarEntry.getName())) {
                addJarResource(jarEntryAsFile(jarFile, jarEntry));
            }
        }
    }

    private static File jarEntryAsFile(JarFile jarFile, JarEntry jarEntry) throws IOException {
        InputStream input = null;
        OutputStream output = null;
        try {
            String name = jarEntry.getName().replace('/', '_');
            int i = name.lastIndexOf("."); //$NON-NLS-1$
            String extension = i > -1 ? name.substring(i) : ""; //$NON-NLS-1$
            File file = File.createTempFile(name.substring(0, name.length() - extension.length()) + ".", extension); //$NON-NLS-1$
            file.deleteOnExit();
            input = jarFile.getInputStream(jarEntry);
            output = new FileOutputStream(file);
            int readCount;
            byte[] buffer = new byte[4096];
            while ((readCount = input.read(buffer)) != -1) {
                output.write(buffer, 0, readCount);
            }
            return file;
        } finally {
            close(input);
            close(output);
        }
    }

    private static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void addJar(File f) throws IOException {
        addJarResource(f);
    }

}
