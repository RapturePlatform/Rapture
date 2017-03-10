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
package rapture.repo.google;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.joda.time.Duration;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.testing.RemoteStorageHelper;
import com.google.cloud.storage.testing.RemoteStorageHelper.StorageHelperException;
import com.google.common.net.MediaType;

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
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.kernel.BlobApiImpl;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class BlobApiGoogleTest {

    public static String saveInitSysConfig;
    public static String saveRaptureRepo;
    public static final String auth = "test" + Integer.toHexString((int) System.currentTimeMillis());
    public static RaptureConfig config;
    public static CallingContext callingContext;

    private static BlobApiImpl blobImpl;
    private static final String BLOB_USING_GOOGLE = "BLOB {} USING GCP_STORAGE {prefix=\"B" + auth + "\"}";
    private static final String REPO_USING_GOOGLE = "REP {} USING GCP_DATASTORE {prefix=\"" + auth + "\"}";
    private static final String META_USING_GOOGLE = "REP {} USING GCP_DATASTORE {prefix=\"M" + auth + "\"}";
    private static final byte[] SAMPLE_BLOB = "This is a blob".getBytes();

    static String blobAuthorityURI = "blob://" + auth;
    static String blobURI = blobAuthorityURI + "/SwampThing";
    private static LocalDatastoreHelper helper = LocalDatastoreHelper.create();

    @AfterClass
    public static void tidyUp() throws IOException, InterruptedException, TimeoutException {
        helper.stop(new Duration(6000));
    }

    @BeforeClass
    static public void setUp() {
        GoogleDatastoreKeyStore.setDatastoreOptionsForTesting(helper.getOptions());
        try {
            helper.start();
        } catch (IOException | InterruptedException e) {
            Assert.fail(e.getMessage());
        } // Starts the local Datastore emulator in a separate process
        
        
        try {
            File key = new File("src/test/resources/key.json");
            Assume.assumeTrue("Cannot read " + key.getAbsolutePath(), key.canRead());
            RemoteStorageHelper helper = RemoteStorageHelper.create("todo3-incap", new FileInputStream(key));
            GoogleBlobStore.setStorageForTesting(helper.getOptions().getService());
        } catch (StorageHelperException | FileNotFoundException e) {
            Assume.assumeNoException("Cannot create storage helper", e);
        }

        RaptureConfig.setLoadYaml(false);
        config = ConfigLoader.getConf();
        saveRaptureRepo = config.RaptureRepo;
        saveInitSysConfig = config.InitSysConfig;
        System.setProperty("LOGSTASH-ISENABLED", "false");
        callingContext = new CallingContext();
        callingContext.setUser("dummy");

        config.RaptureRepo = REPO_USING_GOOGLE;
        config.InitSysConfig = "NREP {} USING GCP_DATASTORE { prefix=\"" + auth + ".sys.config\"}";

        callingContext = new CallingContext();
        callingContext.setUser("dummy");
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {}");

        Kernel.initBootstrap();
        callingContext = ContextFactory.getKernelUser();

        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");
        blobImpl = new BlobApiImpl(Kernel.INSTANCE);
    }

    @AfterClass
    static public void cleanUp() throws IOException, InterruptedException, TimeoutException {
        ConfigLoader.getConf().InitSysConfig = saveInitSysConfig;
        ConfigLoader.getConf().RaptureRepo = saveRaptureRepo;

        try {
            if ((blobImpl != null) && blobImpl.blobRepoExists(callingContext, "blob://dummy")) blobImpl.deleteBlobRepo(callingContext, "blob://dummy");
        } catch (Exception e) {
            System.err.println("Warning: exception in clean up: " + e.getMessage());
        }

        try {
            if ((blobImpl != null) && blobImpl.blobRepoExists(callingContext, blobAuthorityURI)) blobImpl.deleteBlobRepo(callingContext, blobAuthorityURI);
        } catch (Exception e) {
            System.err.println("Warning: exception in clean up: " + e.getMessage());
        }

        try {
            helper.stop(new Duration(6000L));
        } catch (Exception e) {
            System.err.println("Warning: exception in clean up: " + e.getMessage());
        }
    }

    static boolean firstTime = true;

    @Test
    public void testCreateAndGetRepo() {
        if (!firstTime && blobImpl.blobRepoExists(callingContext, blobAuthorityURI)) return;
        firstTime = false;
        blobImpl.createBlobRepo(callingContext, blobAuthorityURI, BLOB_USING_GOOGLE, META_USING_GOOGLE);
        BlobRepoConfig blobRepoConfig = blobImpl.getBlobRepoConfig(callingContext, blobAuthorityURI);
        assertNotNull(blobRepoConfig);
        assertEquals(BLOB_USING_GOOGLE, blobRepoConfig.getConfig());
        assertEquals(META_USING_GOOGLE, blobRepoConfig.getMetaConfig());
        assertEquals(auth, blobRepoConfig.getAuthority());
    }

    @Test
    public void testGetChildren() {

        testPutAndGetBlob();
        assertTrue(blobImpl.blobExists(callingContext, blobURI));
        Map<String, RaptureFolderInfo> children = blobImpl.listBlobsByUriPrefix(callingContext, blobAuthorityURI, -1);
        assertEquals(1, children.size());
        children = blobImpl.listBlobsByUriPrefix(callingContext, "blob://thiswontexist", -1);
        assertTrue(children.isEmpty());
        children = blobImpl.listBlobsByUriPrefix(callingContext, "blob://thiswontexist/so/returnempty", -1);
        assertTrue(children.isEmpty());
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
    public void testValidDocStore() {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("prefix", "foo");
        blobImpl.createBlobRepo(callingContext, "blob://dummy2", "BLOB {} USING GCP_STORAGE { prefix=\"foo\" }", "REP {} USING GCP_DATASTORE { prefix=\"foo\" }");

    }

    @Test
    public void testGetBlobRepositories() {
        try {
            testCreateAndGetRepo();
            List<BlobRepoConfig> before = blobImpl.getBlobRepoConfigs(callingContext);

            blobImpl.createBlobRepo(callingContext, "blob://somewhere_else/", "BLOB {} USING GCP_STORAGE {prefix=\"somewhere_else\"}",
                    "REP {} USING GCP_DATASTORE {prefix=\"somewhere_else\"}");

            List<BlobRepoConfig> after = blobImpl.getBlobRepoConfigs(callingContext);
            // And then there were three
            assertEquals(JacksonUtil.jsonFromObject(after), before.size() + 1, after.size());
        } finally {
            blobImpl.deleteBlobRepo(callingContext, "blob://somewhere_else/");
        }
    }

    @Test
    public void testPutAndGetBlob() {
        testCreateAndGetRepo();
        try {
            blobImpl.putBlob(callingContext, blobURI, SAMPLE_BLOB, MediaType.CSS_UTF_8.toString());
        } catch (StorageException e) {
            Assume.assumeFalse(e.getMessage().startsWith("503 Service Unavailable"));
            Assert.fail(e.getMessage());
        }
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
            assertEquals(Messages.getMessage("Api", "NoSuchRepo", new String[] { blobAuthorityURI }, null).format(), e.getMessage());
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
            assertEquals("Folder " + blobURI5 + " does not exist", e.getMessage());
        }
        blob = blobImpl.getBlob(callingContext, blobURI1);
        assertNotNull(blob);
        assertNotNull(blob.getContent());
        assertArrayEquals(SAMPLE_BLOB, blob.getContent());
    }
}
