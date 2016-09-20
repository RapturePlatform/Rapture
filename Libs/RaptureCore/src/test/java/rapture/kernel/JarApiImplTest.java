/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Incapture Technologies LLC
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
package rapture.kernel;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import rapture.common.RaptureFolderInfo;
import rapture.common.jar.JarCache;
import rapture.kernel.script.KernelScript;
import rapture.kernel.script.ScriptJar;

public class JarApiImplTest {

    private static final Logger log = Logger.getLogger(JarApiImplTest.class);

    private static final String SAMPLE_JAR_URI = "jar://testjar.jar";
    private static final byte[] SAMPLE_JAR = "Wesa got a grand army. That’s why you no liking us meesa thinks.".getBytes();

    private ScriptJar jarApi;
    private KernelScript ks = new KernelScript();
    private static final String TEST_JAR_PATH = StringUtils.join(new String[] { "build", "classes", "test" }, File.separator);

    @Before
    public void setUp() {
        Kernel.getKernel().restart();
        Kernel.initBootstrap();
        ks.setCallingContext(ContextFactory.getKernelUser());
        jarApi = ks.getJar();
        Kernel.setCategoryMembership("alpha");
    }

    @Test
    public void testJarExists() {
        assertFalse(jarApi.jarExists(SAMPLE_JAR_URI));
        jarApi.putJar(SAMPLE_JAR_URI, SAMPLE_JAR);
        assertTrue(jarApi.jarExists(SAMPLE_JAR_URI));
        assertFalse(jarApi.jarExists("jar://something/i/just/made/up.jar"));
    }

    @Test
    public void testPutAndGetJar() {
        byte[] testJar = createTestJar();
        if (testJar == null) {
            return;
        }
        String uri = "jar://mytest/testjar1.jar";
        jarApi.putJar(uri, testJar);
        assertArrayEquals(testJar, jarApi.getJar(uri).getContent());
    }

    @Test
    public void testDeleteJar() {
        jarApi.deleteJar(SAMPLE_JAR_URI);
        jarApi.putJar(SAMPLE_JAR_URI, SAMPLE_JAR);
        assertTrue(jarApi.jarExists(SAMPLE_JAR_URI));
        jarApi.deleteJar(SAMPLE_JAR_URI);
        assertFalse(jarApi.jarExists(SAMPLE_JAR_URI));
    }

    @Test
    public void testGetJarSize() {
        assertEquals(-1L, (long) jarApi.getJarSize(SAMPLE_JAR_URI));
        jarApi.putJar(SAMPLE_JAR_URI, SAMPLE_JAR);
        assertEquals(SAMPLE_JAR.length, (long) jarApi.getJarSize(SAMPLE_JAR_URI));
    }

    @Test
    public void testGetJarMetaData() throws ExecutionException {
        assertTrue(jarApi.getJarMetaData(SAMPLE_JAR_URI).isEmpty());
        jarApi.putJar(SAMPLE_JAR_URI, SAMPLE_JAR);
        Map<String, String> metaData = jarApi.getJarMetaData(SAMPLE_JAR_URI);
        assertEquals(new Integer(SAMPLE_JAR.length).toString(), metaData.get("Content-Length"));
        assertEquals(JarApiImpl.CONTENT_TYPE, metaData.get("Content-Type"));
        assertTrue(JarCache.getInstance().get(ks, SAMPLE_JAR_URI).isEmpty());
    }

    @Test
    public void testListJarsByUriPrefix() {

        Map<String, RaptureFolderInfo> res = jarApi.listJarsByUriPrefix("jar://bogus", -1);
        assertNotNull(res);
        assertTrue(res.isEmpty());

        jarApi.putJar("jar://folder1/jar-1.jar", SAMPLE_JAR);
        jarApi.putJar("jar://folder1/jar-2.jar", SAMPLE_JAR);
        jarApi.putJar("jar://folder1/folder2/jar-3.jar", SAMPLE_JAR);
        jarApi.putJar("jar://folder1/folder2/jar-4.jar", SAMPLE_JAR);

        Map<String, RaptureFolderInfo> folderMap = jarApi.listJarsByUriPrefix("jar://folder1", -1);
        assertEquals(5, folderMap.size());

        // Make sure hyphens are properly decoded for FILE repos (there are other special chars,
        // but they have their own test and hyphens are the most important for jars)
        assertTrue(folderMap.containsKey("jar://folder1/jar-1.jar"));
        assertTrue(folderMap.containsKey("jar://folder1/jar-2.jar"));
        assertTrue(folderMap.containsKey("jar://folder1/folder2/jar-3.jar"));
        assertTrue(folderMap.containsKey("jar://folder1/folder2/jar-4.jar"));

        folderMap = jarApi.listJarsByUriPrefix("jar://folder1/folder2", -1);
        assertEquals(2, folderMap.size());

        folderMap = jarApi.listJarsByUriPrefix("jar://folder1", 1);
        assertEquals(3, folderMap.size());
    }

    private byte[] createTestJar() {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream target = new JarOutputStream(baos, manifest)) {
            File testJar = new File(TEST_JAR_PATH);
            if (!testJar.exists() || !testJar.canRead()) {
                return null;
            }
            add(testJar, target);
        } catch (IOException e) {
            log.error(e);
        }
        return baos.toByteArray();
    }

    private void add(File source, JarOutputStream target) throws IOException {
        BufferedInputStream in = null;
        try {
            if (source.isDirectory()) {
                String name = source.getPath().replace("\\", "/");
                if (!name.isEmpty()) {
                    if (!name.endsWith("/")) {
                        name += "/";
                    }
                    JarEntry entry = new JarEntry(name);
                    entry.setTime(source.lastModified());
                    target.putNextEntry(entry);
                    target.closeEntry();
                }
                for (File nestedFile : source.listFiles()) {
                    add(nestedFile, target);
                }
                return;
            }

            JarEntry entry = new JarEntry(source.getPath().replace("\\", "/"));
            entry.setTime(source.lastModified());
            target.putNextEntry(entry);
            in = new BufferedInputStream(new FileInputStream(source));

            byte[] buffer = new byte[1024];
            while (true) {
                int count = in.read(buffer);
                if (count == -1) {
                    break;
                }
                target.write(buffer, 0, count);
            }
            target.closeEntry();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
