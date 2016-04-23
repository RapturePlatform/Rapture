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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import rapture.common.BlobContainer;
import rapture.common.CallingContext;
import rapture.common.Messages;
import rapture.common.RaptureConstants;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.BlobRepoConfig;

import com.google.common.net.MediaType;

public class BlobApiFileTest extends AbstractFileTest {

    private static CallingContext callingContext;
    private static BlobApiImpl blobImpl;
    private static final String BLOB_USING_FILE = "BLOB {} USING FILE {prefix=\"/tmp/B" + auth + "\"}";
    private static final String REPO_USING_FILE = "REP {} USING FILE {prefix=\"/tmp/" + auth + "\"}";
    private static final String META_USING_FILE = "REP {} USING FILE {prefix=\"/tmp/M" + auth + "\"}";
    private static final byte[] SAMPLE_BLOB = "This is a blob".getBytes();

    static String blobAuthorityURI = "blob://" + auth;
    static String blobURI = blobAuthorityURI + "/SwampThing";

    @BeforeClass
    static public void setUp() {        
        AbstractFileTest.setUp();
        
        config.RaptureRepo = REPO_USING_FILE;
        config.InitSysConfig = "NREP {} USING FILE { prefix=\"/tmp/" + auth + ".sys.config\"}";
        // config.DefaultPipelineTaskStatus = "TABLE {} USING FILE {prefix=\"/tmp/" + auth + "\"}";

        callingContext = new CallingContext();
        callingContext.setUser("dummy");

        Kernel.initBootstrap();
        callingContext = ContextFactory.getKernelUser();

        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");
        blobImpl = new BlobApiImpl(Kernel.INSTANCE);
    }

    @AfterClass
    static public void cleanUp() {
        if (blobImpl.blobRepoExists(callingContext, "blob://dummy"))
            blobImpl.deleteBlobRepo(callingContext, "blob://dummy");
    }
    
    static boolean firstTime = true;

    @Test
    public void testCreateAndGetRepo() {
        if (!firstTime && blobImpl.blobRepoExists(callingContext, blobAuthorityURI)) return;
        firstTime = false;
        blobImpl.createBlobRepo(callingContext, blobAuthorityURI, BLOB_USING_FILE, META_USING_FILE);
        BlobRepoConfig blobRepoConfig = blobImpl.getBlobRepoConfig(callingContext, blobAuthorityURI);
        assertNotNull(blobRepoConfig);
        assertEquals(BLOB_USING_FILE, blobRepoConfig.getConfig());
        assertEquals(META_USING_FILE, blobRepoConfig.getMetaConfig());
        assertEquals(auth, blobRepoConfig.getAuthority());
    }
    
    @Test
    public void testGetChildren() {
        testPutAndGetBlob();
        assertTrue(blobImpl.blobExists(callingContext, blobURI));

        Map<String, RaptureFolderInfo> children = blobImpl.listBlobsByUriPrefix(callingContext, blobAuthorityURI, -1);
        int size = children.size();
        assertEquals(1, children.size());
        
    }

    @Test
    public void testDeleteRepo() {
        testPutAndGetBlob();
        assertTrue(blobImpl.blobExists(callingContext, blobURI));
        BlobContainer bc = blobImpl.getBlob(callingContext, blobURI);
        blobImpl.deleteBlobRepo(callingContext, blobAuthorityURI);
        assertFalse(blobImpl.blobRepoExists(callingContext, blobAuthorityURI));
        assertFalse(blobImpl.blobExists(callingContext, blobURI));
        testCreateAndGetRepo();
        assertTrue(blobImpl.blobRepoExists(callingContext, blobAuthorityURI));
        assertFalse(blobImpl.blobExists(callingContext, blobURI));
        testPutAndGetBlob();
        assertTrue(blobImpl.blobExists(callingContext, blobURI));
    }

    @Test
    public void testThatWhichShouldNotBe() {
        String dummyAuthorityURI = "blob://dummy";
        String dummyURI = dummyAuthorityURI + "/dummy";
        try {
            blobImpl.createBlobRepo(callingContext, dummyAuthorityURI, "BLOB {} USING FILE { }", META_USING_FILE);
            BlobContainer blob = blobImpl.getBlob(callingContext, dummyURI);
            fail("You can't create a repo without a valid prefix");
        } catch (Exception e) {
            assertTrue(e.getMessage(), e.getMessage().contains("prefix"));
        }
        
        try {
            // because the config gets stored even though it's not valid
            blobImpl.deleteBlobRepo(callingContext, dummyAuthorityURI);
        } catch (Exception e) {
            assertTrue(e.getMessage(), e.getMessage().contains("prefix"));
        }
        
        try {
            blobImpl.createBlobRepo(callingContext, dummyAuthorityURI, "BLOB {} USING FILE { prefix=\"\" }", META_USING_FILE);
            BlobContainer blob = blobImpl.getBlob(callingContext, dummyURI);
            fail("You can't create a repo without a valid prefix");
        } catch (Exception e) {
            assertTrue(e.getMessage(), e.getMessage().contains("prefix"));
        }

        // because the config gets stored even though it's not valid
        blobImpl.deleteBlobRepo(callingContext, dummyAuthorityURI);
        
        Map<String, String> hashMap = new HashMap<>();
        try {
            // Don't pass NULL - that's bad.
            blobImpl.createBlobRepo(callingContext, dummyAuthorityURI, "BLOB {} USING FILE {}", "REP {} USING FILE {}");
            BlobContainer blob = blobImpl.getBlob(callingContext, dummyURI);
            fail("You can't create a repo without a prefix");
        } catch (Exception e) {
            assertTrue(e.getMessage(), e.getMessage().contains("prefix"));
        }

        // because the config gets stored even though it's not valid
        blobImpl.deleteBlobRepo(callingContext, dummyAuthorityURI);
        
        hashMap.put("prefix", "  ");
        try {
            blobImpl.createBlobRepo(callingContext, dummyAuthorityURI, "BLOB {} USING FILE { prefix=\"  \" }", "REP {} USING FILE { prefix=\"  \" }");

            BlobContainer blob = blobImpl.getBlob(callingContext, dummyURI);
            fail("You can't create a repo without a valid prefix");
        } catch (Exception e) {
            assertTrue(e.getMessage(), e.getMessage().contains("prefix"));
        }
        blobImpl.deleteBlobRepo(callingContext, dummyAuthorityURI);
    }

    @Test
    public void testValidDocStore() {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("prefix", "/tmp/foo");
        blobImpl.createBlobRepo(callingContext, "blob://dummy2", "BLOB {} USING FILE { prefix=\"/tmp/foo\" }", "REP {} USING FILE { prefix=\"/tmp/foo\" }");

    }
    
    @Test
    public void testGetBlobRepositories() {
        testCreateAndGetRepo();
        List<BlobRepoConfig> before = blobImpl.getBlobRepoConfigs(callingContext);
        
        blobImpl.createBlobRepo(callingContext, "blob://somewhereelse/",
                "BLOB {} USING FILE {prefix=/tmp/somewhereelse\"}",
                "REP {} USING FILE {prefix=/tmp/somewhereelse\"}");

        List<BlobRepoConfig> after = blobImpl.getBlobRepoConfigs(callingContext);
        // And then there were three
        assertEquals(JacksonUtil.jsonFromObject(after), before.size()+1, after.size());
    }

    @Test
    public void testPutAndGetBlob() {
        testCreateAndGetRepo();
        blobImpl.putBlob(callingContext, blobURI, SAMPLE_BLOB, MediaType.CSS_UTF_8.toString());
        BlobContainer blob = blobImpl.getBlob(callingContext, blobURI);
        assertArrayEquals(SAMPLE_BLOB, blob.getContent());
    }

    // Can't store blobs with attributes - See RAP-2797
    @Ignore
    @Test
    public void testPutAndGetBlobWithAttribute() {
        testCreateAndGetRepo();
        RaptureURI blobURIWithAttribute = new RaptureURI.Builder(Scheme.BLOB, auth).docPath("Foo").attribute("Bar").build();
        blobImpl.putBlob(callingContext, blobURIWithAttribute.toString(), SAMPLE_BLOB, MediaType.CSS_UTF_8.toString());
        BlobContainer blob = blobImpl.getBlob(callingContext, blobURIWithAttribute.toString());
        assertArrayEquals(SAMPLE_BLOB, blob.getContent());
    }

    @Test
    public void testGetWithNoScheme() {
        testPutAndGetBlob();
        BlobContainer blob = blobImpl.getBlob(callingContext, blobURI.substring(blobURI.indexOf(':') + 1));
        assertArrayEquals(SAMPLE_BLOB, blob.getContent());
        assertEquals(MediaType.CSS_UTF_8.toString(), blob.getHeaders().get("Content-Type"));
    }

    @Test
    public void testDeleteBlob() {
        testCreateAndGetRepo();
        blobImpl.deleteBlob(callingContext, blobURI);
        BlobContainer blob = blobImpl.getBlob(callingContext, blobURI);
        assertNull(blob);
    }

    @Test()
    public void testDeleteBlobRepo() {
        testCreateAndGetRepo();
        blobImpl.deleteBlobRepo(callingContext, blobAuthorityURI);
        try {
            blobImpl.getBlob(callingContext, blobURI);
            fail("Repository should have been deleted");
        } catch (RaptureException e) {
            assertEquals(Messages.getMessage("Api", "NoSuchRepo", new String[] { blobAuthorityURI } , null).format(), e.getMessage());
        }
    }

    @Test
    public void testGetBlobMetaData() {
        testPutAndGetBlob();
        Map<String, String> metaData = blobImpl.getBlobMetaData(callingContext, blobURI);
        assertEquals(metaData.get("Content-Type"), MediaType.CSS_UTF_8.toString());
    }

    @Test
    public void testGetSize() {
        testPutAndGetBlob();
        Long blobSize = blobImpl.getBlobSize(callingContext, blobURI);
        assertEquals(14L, blobSize.longValue());
    }
    
    // deleteBlobsByUriPrefix called on a non-existent blob deletes all existing blobs in folder
    @Test
    public void testRap3945() {
        testCreateAndGetRepo();
        String blobURI1 = blobAuthorityURI + "/PacMan/Inky";
        String blobURI2 = blobAuthorityURI + "/PacMan/Pinky";
        String blobURI3 = blobAuthorityURI + "/PacMan/Blnky";
        String blobURI4 = blobAuthorityURI + "/PacMan/Clyde";
        String blobURI5 = blobAuthorityURI + "/PacMan/Sue";

        blobImpl.putBlob(callingContext, blobURI1, SAMPLE_BLOB, MediaType.CSS_UTF_8.toString());
        blobImpl.putBlob(callingContext, blobURI2, SAMPLE_BLOB, MediaType.CSS_UTF_8.toString());
        blobImpl.putBlob(callingContext, blobURI3, SAMPLE_BLOB, MediaType.CSS_UTF_8.toString());
        blobImpl.putBlob(callingContext, blobURI4, SAMPLE_BLOB, MediaType.CSS_UTF_8.toString());
        
        BlobContainer blob;
        
        blob = blobImpl.getBlob(callingContext, blobURI1);
        assertNotNull(blob);
        assertNotNull(blob.getContent());
        assertArrayEquals(SAMPLE_BLOB, blob.getContent());
        
        blob = blobImpl.getBlob(callingContext, blobURI4);
        assertNotNull(blob);
        assertNotNull(blob.getContent());
        assertArrayEquals(SAMPLE_BLOB, blob.getContent());
        
        assertNull(blobImpl.getBlob(callingContext, blobURI5));
                
        try {
            blobImpl.deleteBlobsByUriPrefix(callingContext, blobURI5);
            // SHOULD FAIL OR DO NOTHING
        } catch (Exception e) {
            assertTrue(e.getMessage().equals("Folder "+blobURI5+" does not exist"));
        }
        blob = blobImpl.getBlob(callingContext, blobURI1);
        assertNotNull(blob);
        assertNotNull(blob.getContent());
        assertArrayEquals(SAMPLE_BLOB, blob.getContent());        
    }
}
