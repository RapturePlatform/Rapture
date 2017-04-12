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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.joda.time.Duration;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.cloud.datastore.testing.LocalDatastoreHelper;

import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentRepoConfig;
import rapture.kernel.AbstractFileTest;
import rapture.kernel.ContextFactory;
import rapture.kernel.DocApiImpl;
import rapture.kernel.Kernel;
import rapture.pipeline2.gcp.PubsubPipeline2Handler;

public class DocApiGoogleTest extends AbstractFileTest {

    private static final Logger log = Logger.getLogger(DocApiGoogleTest.class);
    private static final String REPO_USING_GCP_DATASTORE = "REP {} USING GCP_DATASTORE {prefix =\"" + auth + "\"}";
    private static final String GET_ALL_BASE = "document://getAll";
    private static final String docContent = "{\"content\":\"Cold and misty morning I had heard a warning borne in the air\"}";
    private static final String docAuthorityURI = "document://" + auth;
    private static final String docURI = docAuthorityURI + "/brain/salad/surgery";

    private static CallingContext callingContext;
    private static DocApiImpl docImpl;

    static LocalDatastoreHelper helper = null;

    @BeforeClass
    public static void setupLocalDatastore() throws IOException, InterruptedException {
        helper = LocalDatastoreHelper.create(1.0);
        helper.start(); // Starts the local Datastore emulator in a separate process
        GoogleDatastoreKeyStore.setDatastoreOptionsForTesting(helper.getOptions());
        GoogleIndexHandler.setDatastoreOptionsForTesting(helper.getOptions());
    }

    @AfterClass
    public static void cleanupLocalDatastore() throws IOException, InterruptedException, TimeoutException {
        Kernel.shutdown();
        PubsubPipeline2Handler.cleanUp();
        try {
            helper.stop(new Duration(60000L));
            helper.reset();
            helper = null;
        } catch (Exception e) {
            System.out.println("Exception shutting down LocalDatastoreHelper: " + e.getMessage());
        }
    }

    @BeforeClass
    static public void setUp() {
        AbstractFileTest.setUp();
        config.RaptureRepo = REPO_USING_GCP_DATASTORE;
        config.InitSysConfig = "NREP {} USING GCP_DATASTORE { prefix =\"" + auth + "/sys.config\"}";
        config.DefaultPipelineTaskStatus = "TABLE {} USING MEMORY {prefix =\"" + auth + "\"}";
        config.DefaultExchange = "PIPELINE {} USING GCP_PUBSUB { projectid=\"todo3-incap\"}";
        config.DefaultWorkflowAuditLog = "LOG {} USING MEMORY {maxEntries=\"100\"}";
        System.setProperty("LOGSTASH-ISENABLED", "false");

        callingContext = new CallingContext();
        callingContext.setUser("dummy");

        Kernel.initBootstrap();
        callingContext = ContextFactory.getKernelUser();
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {prefix=\"/tmp/" + UUID.randomUUID() + "\"}");

        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {prefix =\"" + auth + "\"}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");
        docImpl = new DocApiImpl(Kernel.INSTANCE);
    }

    @Test
    public void testValidDocStore() {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("prefix", "/tmp/foo");
        docImpl.createDocRepo(callingContext, "document://dummy2", "NREP {} USING GCP_DATASTORE { prefix = foo\"");
    }

    @Test
    public void testPutContentWithNoRepo() {
        String randomDoc = "document://" + UUID.randomUUID().toString() + "/JUNK/ForTestingOnly";
        String randomJson = "{\"Foo\" : \"Bar\"}";

        docImpl.putDoc(ContextFactory.getKernelUser(), randomDoc, randomJson);
        assertEquals(randomJson, docImpl.getDoc(ContextFactory.getKernelUser(), randomDoc));
    }


    @Test
    public void testDeleteRepo() {
        testPutAndGetDoc();
        assertTrue(docImpl.docExists(callingContext, docURI));
        String doc = docImpl.getDoc(callingContext, docURI);
        docImpl.deleteDocRepo(callingContext, docAuthorityURI);
        assertFalse(docImpl.docRepoExists(callingContext, docAuthorityURI));
        assertFalse(docImpl.docExists(callingContext, docURI));
        testCreateAndGetRepo();
        assertTrue(docImpl.docRepoExists(callingContext, docAuthorityURI));
        // assertFalse("The repository was deleted so why is the file still here", docImpl.docExists(callingContext, docURI));
        testPutAndGetDoc();
        assertTrue(docImpl.docExists(callingContext, docURI));
    }


    @Test
    public void testlistDocsByUriPrefix() {
        if (docImpl.docRepoExists(callingContext, GET_ALL_BASE)) {
            docImpl.deleteDocRepo(callingContext, GET_ALL_BASE);

        } else {
            docImpl.createDocRepo(callingContext, GET_ALL_BASE, "REP {} USING GCP_DATASTORE {prefix =\"" + auth + "-1\"}");
        }
        docImpl.putDoc(callingContext, GET_ALL_BASE + "/uncle", "{\"magic\": \"Drunk Uncle\"}");
        docImpl.putDoc(callingContext, GET_ALL_BASE + "/dad/kid1", "{\"magic\": \"Awesome Child\"}");
        docImpl.putDoc(callingContext, GET_ALL_BASE + "/dad/kid2", "{\"magic\": \"Stellar Child\"}");
        docImpl.putDoc(callingContext, GET_ALL_BASE + "/daddywarbucks/fakeKid", "{\"magic\": \"Fake Child\"}");
        Map<String, RaptureFolderInfo> allDocs = docImpl.listDocsByUriPrefix(callingContext, GET_ALL_BASE, 0);
        // listDocsByUriPrefix also returns both the folders /dad and /daddywarbucks

        for (String s : allDocs.keySet()) {
            System.out.println("Got key : " + s);
        }
        assertEquals(6, allDocs.size());
        Assert.assertFalse(allDocs.values().toArray(new RaptureFolderInfo[6])[0].getName().startsWith(GET_ALL_BASE + "//"));
        Map<String, RaptureFolderInfo> dadDocs = docImpl.listDocsByUriPrefix(callingContext, GET_ALL_BASE + "/dad", 0);
        for (String s : dadDocs.keySet()) {
            System.out.println("Key : " + s);
        }
        assertEquals(2, dadDocs.size());
        // ordering is not guaranteed, we could get either kid1 or kid2, but should not get fakeKid nor uncle
        String s = dadDocs.keySet().toArray(new String[2])[0];
        assertEquals(GET_ALL_BASE + "/dad/kidX != " + s, (GET_ALL_BASE + "/dad/kidX").length(), s.length());
    }

    static boolean firstTime = true;

    @Test
    public void testCreateAndGetRepo() {
        if (!firstTime && docImpl.docRepoExists(callingContext, docAuthorityURI)) return;
        firstTime = false;
        docImpl.createDocRepo(callingContext, docAuthorityURI, REPO_USING_GCP_DATASTORE);
        DocumentRepoConfig docRepoConfig = docImpl.getDocRepoConfig(callingContext, docAuthorityURI);
        assertNotNull(docRepoConfig);
        assertEquals(REPO_USING_GCP_DATASTORE, docRepoConfig.getDocumentRepo().getConfig());
        assertEquals(auth, docRepoConfig.getAuthority());
    }

    @Test
    public void testGetDocumentRepositories() {
        testCreateAndGetRepo();
        List<DocumentRepoConfig> docRepositories = docImpl.getDocRepoConfigs(callingContext);
        docImpl.createDocRepo(callingContext, "document://somewhereelse/", REPO_USING_GCP_DATASTORE);
        List<DocumentRepoConfig> docRepositoriesNow = docImpl.getDocRepoConfigs(callingContext);
        assertEquals(JacksonUtil.jsonFromObject(docRepositoriesNow), docRepositories.size() + 1, docRepositoriesNow.size());
    }

    @Test
    public void testPutAndGetDoc() {
        testCreateAndGetRepo();
        docImpl.putDoc(callingContext, docURI, docContent);
        String doc = docImpl.getDoc(callingContext, docURI);
        assertEquals(docContent, doc);
    }

    @Test
    public void testPutAndGetDocWithAttribute() {
        testCreateAndGetRepo();
        // Hmm if you use Bar not meta/Bar then it causes problems later - shouldn't that get caught by URI builder?
        RaptureURI docURIWithAttribute = new RaptureURI.Builder(Scheme.DOCUMENT, auth).docPath("Foo").attribute("meta/Bar").build();
        docImpl.putDoc(callingContext, docURIWithAttribute.toString(), docContent);
        String doc = docImpl.getDoc(callingContext, docURIWithAttribute.toString());
        assertEquals(docContent, doc);
    }

    @Test
    public void testDeleteDoc() {
        testPutAndGetDoc();
        assertTrue("Doc should exist", docImpl.docExists(callingContext, docURI));
        String doc = docImpl.getDoc(callingContext, docURI);
        assertNotNull("Document should exist but data not present", doc);

        docImpl.deleteDoc(callingContext, docURI);
        assertFalse("Doc should have been deleted", docImpl.docExists(callingContext, docURI));
        doc = docImpl.getDoc(callingContext, docURI);
        assertNull("Document should have been deleted but data still present", doc);
    }

    @Test()
    public void testDeleteDocRepo() {
        testPutAndGetDoc();
        String data = docImpl.getDoc(callingContext, docURI);
        assertNotNull("Document not found", data);
        List<DocumentRepoConfig> docRepositories = docImpl.getDocRepoConfigs(callingContext);
        assertTrue("Doc should exist", docImpl.docExists(callingContext, docURI));

        Assert.assertTrue(docImpl.docRepoExists(callingContext, docAuthorityURI));
        docImpl.deleteDocRepo(callingContext, docAuthorityURI);
        Assert.assertFalse(docImpl.docRepoExists(callingContext, docAuthorityURI));
        DocumentRepoConfig docRepoConfig = docImpl.getDocRepoConfig(callingContext, docAuthorityURI);
        assertNull("Repository should have been deleted", docRepoConfig);

        // Don't do this; it quietly creates a REP USING MEMEORY config (should it?)
        //        String zombie = docImpl.getDoc(callingContext, docURI);
        //        assertNull("Repository should have been deleted but data still present", zombie);

        List<DocumentRepoConfig> docRepositoriesAfter = docImpl.getDocRepoConfigs(callingContext);

        assertEquals("Should be one less now", docRepositories.size() - 1, docRepositoriesAfter.size());

        assertFalse("Doc should have been deleted", docImpl.docExists(callingContext, docURI));
        try {
            String doc = docImpl.getDoc(callingContext, docURI);
            assertNull("Document should have been deleted but data still present", doc);
        } catch (Exception e) {
            // Exception is OK here - we are asking for a doc in a repo that isn't there
        }
    }

    @Test

    public void deleteDocsByUriPrefix() {
        String json = "{\"key123\":\"value123\"}";
        testCreateAndGetRepo();

        /* DocumentRepoConfig dr = */
        docImpl.getDocRepoConfig(callingContext, docAuthorityURI);
        assertTrue(docImpl.docRepoExists(callingContext, docAuthorityURI));

        docImpl.putDoc(callingContext, docAuthorityURI + "/folder1/folder2/file3", json);
        assertTrue(docImpl.docExists(callingContext, docAuthorityURI + "/folder1/folder2/file3"));
        String content = docImpl.getDoc(callingContext, docAuthorityURI + "/folder1/folder2/file3");
        assertEquals(json, content);

        docImpl.putDoc(callingContext, docAuthorityURI + "/folder1/file2", json);
        List<String> removedUris = docImpl.deleteDocsByUriPrefix(callingContext, docAuthorityURI + "/folder1/folder2");
        assertFalse(docImpl.docExists(callingContext, docAuthorityURI + "/folder1/folder2/file3"));

        File meta = new File("/tmp" + docAuthorityURI + "_meta/folder1/folder2/file3-3f-2d1.txt");
        String data = null;
        if (meta.exists()) {
            log.warn("Although the file has been deleted its metadata still exists");
            try {
                data = FileUtils.readFileToString(meta);
                log.warn(data);
                Map<String, Object> map = JacksonUtil.getMapFromJson(data);
                String deleted = map.get("deleted").toString();
                if (deleted.equals("false")) log.warn("and it's marked as not deleted");
            } catch (IOException e) {
            }
        }

        docImpl.putDoc(callingContext, docAuthorityURI + "/folder1/folder2", json);
        assertTrue(docImpl.docExists(callingContext, docAuthorityURI + "/folder1/folder2"));

        docImpl.putDoc(callingContext, docAuthorityURI + "/folder1/folder2/file3", json);
        assertTrue(docImpl.docExists(callingContext, docAuthorityURI + "/folder1/folder2/file3"));

        try {
            String data2 = FileUtils.readFileToString(meta);
            System.out.println("Was " + data + " now " + data2);
            Map<String, Object> map = JacksonUtil.getMapFromJson(data);
            String deleted = map.get("deleted").toString();
            if (deleted.equals("false")) log.warn("and it's marked as not deleted");
        } catch (IOException e) {
        }

        // But you can't do this
        //        docImpl.putDoc(callingContext, repoUri + "/folder1/folder2.txt/file3", json);
        //        assertTrue(docImpl.docExists(callingContext, repoUri + "/folder1/folder2.txt/file3"));

    }

    @Test
    public void removeVREPFolder() {
        String repoUri = "//testRepoUri_" + System.currentTimeMillis();
        File f1 = new File("/tmp"+repoUri.substring(1));
        f1.mkdir();
        f1.deleteOnExit();
        File f2 = new File("/tmp"+repoUri.substring(1)+"_cache");
        f2.mkdir();
        f2.deleteOnExit();
        
        String json = "{\"key123\":\"value123\"}";
        docImpl.createDocRepo(callingContext, repoUri, "VREP {} USING GCP_DATASTORE {prefix =\"" + repoUri + "\"}");

        /* DocumentRepoConfig dr = */
        docImpl.getDocRepoConfig(callingContext, repoUri);
        assertTrue(docImpl.docRepoExists(callingContext, repoUri));

        docImpl.putDoc(callingContext, repoUri + "/folder1/folder2/file3", json);
        assertTrue(docImpl.docExists(callingContext, repoUri + "/folder1/folder2/file3"));
        String content = docImpl.getDoc(callingContext, repoUri + "/folder1/folder2/file3");
        assertEquals(json, content);

        docImpl.putDoc(callingContext, repoUri + "/folder1/file2", json);

        // If you delete a folder from a versioned repo the file still remains
        assertTrue(docImpl.docExists(callingContext, repoUri + "/folder1/folder2/file3"));

        File meta = new File("/tmp" + repoUri + "_meta/folder1/folder2/file3-3f-2d1.txt");
        String data = null;
        if (meta.exists()) {
            log.warn("Although the file has been deleted its metadata still exists");
            try {
                data = FileUtils.readFileToString(meta);
                log.warn(data);
                Map<String, Object> map = JacksonUtil.getMapFromJson(data);
                String deleted = map.get("deleted").toString();
                if (deleted.equals("false")) log.warn("and it's marked as not deleted");
            } catch (IOException e) {
            }
        }

        docImpl.putDoc(callingContext, repoUri + "/folder1/folder2", json);
        assertTrue(docImpl.docExists(callingContext, repoUri + "/folder1/folder2"));

        docImpl.putDoc(callingContext, repoUri + "/folder1/folder2/file3", json);
        assertTrue(docImpl.docExists(callingContext, repoUri + "/folder1/folder2/file3"));

        try {
            String data2 = FileUtils.readFileToString(meta);
            System.out.println("Was " + data + " now " + data2);
            Map<String, Object> map = JacksonUtil.getMapFromJson(data);
            String deleted = map.get("deleted").toString();
            if (deleted.equals("false")) log.warn("and it's marked as not deleted");
        } catch (IOException e) {
        }

        // But you can't do this
        //        docImpl.putDoc(callingContext, repoUri + "/folder1/folder2.txt/file3", json);
        //        assertTrue(docImpl.docExists(callingContext, repoUri + "/folder1/folder2.txt/file3"));
        
        try {
            FileUtils.deleteDirectory(f1);
        } catch (IOException e) {
        }
        try {
            FileUtils.deleteDirectory(f2);
        } catch (IOException e) {
        }
    }

    @Test
    public void testRap3532() {
        //create document repo
        long t = System.currentTimeMillis();
        testCreateAndGetRepo();
        DocumentRepoConfig dr = docImpl.getDocRepoConfig(callingContext, docAuthorityURI);
        //create document and putDoc
        String uri = docAuthorityURI + "/file1";
        docImpl.putDoc(callingContext, uri, "{\"key\":\"value\"}");
        //get the docImpl
        Assert.assertTrue("does doc exist should return true.", docImpl.docExists(callingContext, uri));
        String content = docImpl.getDoc(callingContext, uri);
        Assert.assertEquals("Document written to file is not same as expected.", content, "{\"key\":\"value\"}");
        //delete the document
        Assert.assertTrue(docImpl.deleteDoc(callingContext, uri));
        Assert.assertFalse("does doc exist should return false.", docImpl.docExists(callingContext, uri));
        Assert.assertNull("no doc so getDoc should return null.", docImpl.getDoc(callingContext, uri));

        Assert.assertTrue(docImpl.docRepoExists(callingContext, docAuthorityURI));
        docImpl.deleteDocRepo(callingContext, docAuthorityURI);
        Assert.assertFalse(docImpl.docRepoExists(callingContext, docAuthorityURI));
    }
}
