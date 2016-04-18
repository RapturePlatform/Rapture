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

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rapture.common.CallingContext;
import rapture.common.RaptureFolderInfo;
import rapture.common.api.BlobApi;
import rapture.common.api.JarApi;

import java.util.Arrays;
import java.util.Map;

public class JarApiImplTest {
    private static final String SAMPLE_JAR_URI = "jar://testjar.jar";
    private static final byte[] SAMPLE_JAR = "Wesa got a grand army. That’s why you no liking us meesa thinks.".getBytes();

    JarApi jarApi = null;
    CallingContext rootContext;

    @Before
    public void setUp() {
        jarApi = Kernel.getJar();
        rootContext = ContextFactory.getKernelUser();
        tearDown();
    }

    @After
    public void tearDown() {
        BlobApi blobApi = Kernel.getBlob();

        // ----------------------------------
        // Temporary workaround to deleteBlobRepo not removing files from the filesystem
        // Implemented 11/2015 to allow test to pass in spite of RAP-3878
        if (!blobApi.blobRepoExists(rootContext, JarApiImpl.JAR_REPO_URI)) {
            // Has side effect of creating jar repo, making it exist officially and not just
            // as residual files on the filesystem.
            jarApi.putJar(rootContext, SAMPLE_JAR_URI, SAMPLE_JAR);
        }
        try {
            blobApi.deleteBlobsByUriPrefix(rootContext, JarApiImpl.JAR_REPO_URI, false);
        } catch (Exception e) {
            // OK if it's already gone
        }
        // End temporary workaround
        // ----------------------------------

        blobApi.deleteBlobRepo(rootContext, JarApiImpl.JAR_REPO_URI);
    }

    @Test
    public void testJarExists() {
        assertFalse(jarApi.jarExists(rootContext, SAMPLE_JAR_URI));
        jarApi.putJar(rootContext, SAMPLE_JAR_URI, SAMPLE_JAR);
        assertTrue(jarApi.jarExists(rootContext, SAMPLE_JAR_URI));
        assertFalse(jarApi.jarExists(rootContext, "jar://something/i/just/made/up.jar"));
    }

    @Test
    public void testPutAndGetJar() {
        assertNull(jarApi.getJar(rootContext, SAMPLE_JAR_URI));
        jarApi.putJar(rootContext, SAMPLE_JAR_URI, SAMPLE_JAR);
        assertTrue(Arrays.equals(SAMPLE_JAR, jarApi.getJar(rootContext, SAMPLE_JAR_URI).getContent()));
    }

    @Test
    public void testDeleteJar() {
        try {
            // Just doing this to establish we don't get exceptions when the repo doesn't exist yet.
            jarApi.deleteJar(rootContext, SAMPLE_JAR_URI);
        } catch (Exception e) {
        }
        assertFalse(jarApi.jarExists(rootContext, SAMPLE_JAR_URI));
        jarApi.putJar(rootContext, SAMPLE_JAR_URI, SAMPLE_JAR);
        assertTrue(jarApi.jarExists(rootContext, SAMPLE_JAR_URI));
        jarApi.deleteJar(rootContext, SAMPLE_JAR_URI);
        assertFalse(jarApi.jarExists(rootContext, SAMPLE_JAR_URI));
    }

    @Test
    public void testGetJarSize() {
        assertNull(jarApi.getJarSize(rootContext, SAMPLE_JAR_URI));
        jarApi.putJar(rootContext, SAMPLE_JAR_URI, SAMPLE_JAR);
        assertEquals(SAMPLE_JAR.length, (long)jarApi.getJarSize(rootContext, SAMPLE_JAR_URI));
    }

    @Test
    public void testGetJarMetaData() {
        assertNull(jarApi.getJarMetaData(rootContext, SAMPLE_JAR_URI));
        jarApi.putJar(rootContext, SAMPLE_JAR_URI, SAMPLE_JAR);

        Map<String, String> metaData = jarApi.getJarMetaData(rootContext, SAMPLE_JAR_URI);
        assertEquals(new Integer(SAMPLE_JAR.length).toString(), metaData.get("Content-Length"));
        assertEquals(JarApiImpl.CONTENT_TYPE, metaData.get("Content-Type"));
    }

    @Test
    public void testListJarsByUriPrefix() {
        assertNull(jarApi.listJarsByUriPrefix(rootContext, "jar://bogus", -1));

        jarApi.putJar(rootContext, "jar://folder1/jar-1.jar", SAMPLE_JAR);
        jarApi.putJar(rootContext, "jar://folder1/jar-2.jar", SAMPLE_JAR);
        jarApi.putJar(rootContext, "jar://folder1/folder2/jar-3.jar", SAMPLE_JAR);
        jarApi.putJar(rootContext, "jar://folder1/folder2/jar-4.jar", SAMPLE_JAR);

        Map<String, RaptureFolderInfo> folderMap = jarApi.listJarsByUriPrefix(rootContext, "jar://folder1", -1);
        assertEquals(5, folderMap.size());

        // Make sure hyphens are properly decoded for FILE repos (there are other special chars,
        // but they have their own test and hyphens are the most important for jars)
        assertTrue(folderMap.containsKey("jar://folder1/jar-1.jar"));
        assertTrue(folderMap.containsKey("jar://folder1/jar-2.jar"));
        assertTrue(folderMap.containsKey("jar://folder1/folder2/jar-3.jar"));
        assertTrue(folderMap.containsKey("jar://folder1/folder2/jar-4.jar"));

        folderMap = jarApi.listJarsByUriPrefix(rootContext, "jar://folder1/folder2", -1);
        assertEquals(2, folderMap.size());

        folderMap = jarApi.listJarsByUriPrefix(rootContext, "jar://folder1", 1);
        assertEquals(3, folderMap.size());
    }

    @Test
    public void testEnableDisableJar() {
        jarApi.putJar(rootContext, SAMPLE_JAR_URI, SAMPLE_JAR);
        assertFalse(jarApi.jarIsEnabled(rootContext, SAMPLE_JAR_URI));

        jarApi.enableJar(rootContext, SAMPLE_JAR_URI);
        assertTrue(jarApi.jarIsEnabled(rootContext, SAMPLE_JAR_URI));

        jarApi.disableJar(rootContext, SAMPLE_JAR_URI);
        assertFalse(jarApi.jarIsEnabled(rootContext, SAMPLE_JAR_URI));
    }
}
