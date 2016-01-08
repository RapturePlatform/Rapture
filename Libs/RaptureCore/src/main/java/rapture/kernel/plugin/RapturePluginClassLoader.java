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
package rapture.kernel.plugin;

import rapture.common.RaptureFolderInfo;
import rapture.common.api.JarApi;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureClassLoader;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Created by zanniealvarez on 10/9/15.
 */
public class RapturePluginClassLoader extends SecureClassLoader {
    private Hashtable<String, Class> loadedClasses = new Hashtable();
    private Hashtable<String, String> examinedJarsByClass = new Hashtable();
    private Set<String> examinedJars = new HashSet();
    JarApi jarApi = null;

    public RapturePluginClassLoader() {
        super(RapturePluginClassLoader.class.getClassLoader());
        jarApi = Kernel.getJar();
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException  {
        Class loadedClass = loadedClasses.get(className);
        if (loadedClass != null) {
            return loadedClass;
        }

        byte[] classBytes = getClassBytes(className);
        if (classBytes == null) {
            throw new ClassNotFoundException();
        }

        Class klass = defineClass(className, classBytes, 0, classBytes.length);
        loadedClasses.put(className, klass);

        return klass;
    }

    private byte[] getClassBytes(String className) throws ClassNotFoundException {
        String pathToClass = className.replace('.', '/') + ".class";
        byte[] classBytes = getClassBytesFromPreviouslyExaminedJar(pathToClass);

        if (classBytes == null) {
            classBytes = getClassBytesFromJarApi(pathToClass);
        }

        return classBytes;
    }

    private byte[] getClassBytesFromPreviouslyExaminedJar(String pathToClass) throws ClassNotFoundException {
        String jarUri = examinedJarsByClass.get(pathToClass);
        if (jarUri != null && jarApi.jarIsEnabled(ContextFactory.getKernelUser(), jarUri)) {
            return getClassBytesFromJar(pathToClass, jarUri);
        }

        return null;
    }

    private byte[] getClassBytesFromJarApi(String pathToClass) throws ClassNotFoundException {
        Map<String, RaptureFolderInfo> fullJarList = jarApi.listJarsByUriPrefix(ContextFactory.getKernelUser(), "jar://", 0);
        if (fullJarList == null) {
            return null;
        }

        for(Map.Entry<String, RaptureFolderInfo> entry: fullJarList.entrySet()) {
            String jarUri = entry.getKey();
            if (!entry.getValue().isFolder() &&
                    !examinedJars.contains(jarUri) &&
                    jarApi.jarIsEnabled(ContextFactory.getKernelUser(), jarUri)) {
                byte[] classBytes = getClassBytesFromJar(pathToClass, jarUri);
                if (classBytes != null) {
                    return classBytes;
                }
            }
        }

        // This shouldn't ever do anything for us because we've already looked in all these jars,
        // but just in case the user changed their contents while our backs were turned.
        for (String jarUri: examinedJars) {
            if (jarApi.jarIsEnabled(ContextFactory.getKernelUser(), jarUri)) {
                byte[] classBytes = getClassBytesFromJar(pathToClass, jarUri);
                if (classBytes != null) {
                    return classBytes;
                }
            }
        }

        return null;
    }

    private byte[] getClassBytesFromJar(String pathToClass, String jarUri) throws ClassNotFoundException {
        byte[] classBytes = null;
        byte[] jarBytes = jarApi.getJar(ContextFactory.getKernelUser(), jarUri).getContent();
        examinedJars.add(jarUri);

        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().endsWith("/")) {
                    continue;
                }

                // For consistency with other ClassLoaders, let's not load this class now
                // if we haven't been asked for it. But keep track of where it is for faster
                // lookup when we are asked for it.
                examinedJarsByClass.put(entry.getName(), jarUri);

                if (entry.getName().equals(pathToClass)) {
                    if (entry.getSize() != -1) {
                        classBytes = new byte[(int)entry.getSize()];
                        jis.read(classBytes, 0, classBytes.length);
                    }
                    else {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        while(true){
                            int classByte = jis.read();
                            if(classByte == -1)
                                break;
                            baos.write(classByte);
                        }
                        classBytes = baos.toByteArray();
                    }
                }
            }
        } catch (IOException e) {
            throw new ClassNotFoundException("Got IOException when retrieving " + jarUri, e);
        }

        return classBytes;
    }
}
