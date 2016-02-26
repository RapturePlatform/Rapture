package rapture.repo.cassandra.key;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotEquals;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentRepoConfig;
import rapture.common.model.DocumentWithMeta;
import rapture.config.RaptureConfig;
import rapture.kernel.AbstractFileTest;
import rapture.kernel.ContextFactory;
import rapture.kernel.DocApiFileTest;
import rapture.kernel.DocApiImpl;
import rapture.kernel.Kernel;

public class DocApiCassandraTest {
    static String saveInitSysConfig;
    static String saveRaptureRepo;
    static final String auth = "test" + System.currentTimeMillis();
    static String[] suffixes = new String[] { "", "_meta", "_attribute", "-2d1", "-2d1_meta", "-2d1_attribute", "_blob", "_sheet", "_sheetmeta", "_series"} ;
    static RaptureConfig config = new RaptureConfig();
    
    private static final Logger log = Logger.getLogger(DocApiFileTest.class);
    private static final String ks = "testkeyspace";
    private static final String colFamilyName = "testcolfam";
    private static final String REPO_USING_CASSANDRA = "NREP {} USING CASSANDRA {keyspace=\"" + ks + "\", cf=\"" + colFamilyName + "\"}";
    private static final String GET_ALL_BASE = "document://getAll";
    private static final String docContent = "{\"content\":\"Cold and misty morning I had heard a warning borne in the air\"}";
    private static final String docAuthorityURI = "document://" + auth;
    private static final String docURI = docAuthorityURI + "/brain/salad/surgery";

    private static CallingContext callingContext;
    private static DocApiImpl docImpl;

    @BeforeClass
    static public void setUp() {
        AbstractFileTest.setUp();
        config.RaptureRepo = REPO_USING_CASSANDRA;
        config.InitSysConfig = "NREP {} USING MEMORY { prefix=\"/tmp/" + auth + "/sys.config\"}";
        // config.DefaultPipelineTaskStatus = "TABLE {} USING FILE {prefix=\"/tmp/" + auth + "\"}";
        callingContext = new CallingContext();
        callingContext.setUser("dummy");
                
        Kernel.initBootstrap();
        callingContext = ContextFactory.getKernelUser();

        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {prefix=\"/tmp/" + auth + "\"}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");
        docImpl = new DocApiImpl(Kernel.INSTANCE);
    }

    @After
    public void teardown() {
        docImpl.deleteDocRepo(callingContext, docAuthorityURI);
    }
    
    @Test
    public void testCreateAndGetRepo() {
        docImpl.createDocRepo(callingContext, docAuthorityURI, REPO_USING_CASSANDRA);
        DocumentRepoConfig docRepoConfig = docImpl.getDocRepoConfig(callingContext, docAuthorityURI);
        assertNotNull(docRepoConfig);
        assertEquals(REPO_USING_CASSANDRA, docRepoConfig.getDocumentRepo().getConfig());
        assertEquals(auth, docRepoConfig.getAuthority());
    }
    
    @Test
    public void testGetDocumentRepositories() {
        testCreateAndGetRepo();
        List<DocumentRepoConfig> docRepositories = docImpl.getDocRepoConfigs(callingContext);
        docImpl.createDocRepo(callingContext, "document://somewhereelse/", REPO_USING_CASSANDRA);
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
    public void testGetDocRepoStatus() {
        testPutAndGetDoc();
        Map<String, String> ret = docImpl.getDocRepoStatus(callingContext, docAuthorityURI);
        assertNotEquals(0, ret.size());
    }

    @Test
    public void testDeleteDocRepo() {
        testPutAndGetDoc();
        docImpl.deleteDocRepo(callingContext, docAuthorityURI);
        String doc = docImpl.getDoc(callingContext, docURI);
        assertNull("Having dropped this repo, its data should be gone even if the repo is recreated.", doc);
    }

    private long[] putVersionsOfDocInRepo(String docUri, int numVersions, int microsecondsBetween) throws InterruptedException {
        long[] versionTimes = new long[numVersions];
        for (int i = 0; i < numVersions; i++) {
            String jsonContent = "{ \"version\" : \"" + (i + 1) + "\" }";
            docImpl.putDoc(ContextFactory.getKernelUser(), docUri, jsonContent);
            DocumentMetadata meta = docImpl.getDocMeta(ContextFactory.getKernelUser(), docUri);
            versionTimes[i] = meta.getModifiedTimestamp();
            Thread.sleep(microsecondsBetween);
        }
        return versionTimes;
    }

    @Test
    public void testGetDocWithVersion() throws InterruptedException {
        docImpl.createDocRepo(callingContext, docAuthorityURI, REPO_USING_CASSANDRA);
        int numVersions = 3;
        int microsecondsBetween = 2;

        putVersionsOfDocInRepo(docURI, numVersions, microsecondsBetween);

        for (int i = 0; i < numVersions; i++) {
            String versionedDocUri = docURI + "@" + (i+1);
            DocumentWithMeta documentWithMeta = docImpl.getDocAndMeta(callingContext, versionedDocUri);
            String docContent = documentWithMeta.getContent();
            DocumentMetadata docMeta = documentWithMeta.getMetaData();

            assertTrue("Doc content should contain expected data for version.", docContent.matches(".*" + (i + 1) + ".*"));
            assertEquals("Meta data should reflect expected version.", i+1, (long) docMeta.getVersion());

            assertEquals("Doc content should be the same from getDocAndMeta and getDoc.", docContent, docImpl.getDoc(callingContext, versionedDocUri));
            assertEquals("Meta data should be the same from getDocAndMeta and getDocMeta.", docMeta, docImpl.getDocMeta(callingContext, versionedDocUri));
        }
    }

    @Test
    public void testGetDocWithAsOfTime() throws InterruptedException {
        docImpl.createDocRepo(callingContext, docAuthorityURI, REPO_USING_CASSANDRA);
        int numVersions = 5;
        int microsecondsBetween = 2;

        long[] versionTimes = putVersionsOfDocInRepo(docURI, numVersions, microsecondsBetween);
        for (int i = 0; i < numVersions; i++) {
            doTestGetDocWithAsOfTime(docURI, versionTimes[i], i + 1, "Exact time version was submitted should return that version");
        }

        doTestGetDocWithAsOfTime(docURI, versionTimes[numVersions - 1] + 1, numVersions, "With AsOfTime later than last version, get last version");
        doTestGetDocWithAsOfTime(docURI, versionTimes[numVersions - 1] - 1, numVersions - 1, "With AsOfTime between two versions, get earlier version");

        try {
            doTestGetDocWithAsOfTime(docURI, versionTimes[0] - 1, 0, "");
            fail("Should have gotten exception trying to get version from before doc existed.");
        }
        catch (RaptureException e) {
            if (!e.getMessage().equals("!InvalidAsOfTime!")) {
                fail("Should have gotten a different exception.");
            }
        }

        // Archiving
        int cutoffVersion = 3;
        long archiveCutoffTimestamp = versionTimes[cutoffVersion - 1];
        docImpl.archiveRepoDocs(callingContext, docURI, 1, archiveCutoffTimestamp, true);

        try {
            doTestGetDocWithAsOfTime(docURI, archiveCutoffTimestamp, 0, "");
            fail("Should have gotten exception trying to get version that has been deleted.");
        }
        catch (RaptureException e) {
            if (!e.getMessage().equals("!InvalidAsOfTime!")) {
                fail("Should have gotten a different exception.");
            }
        }

        doTestGetDocWithAsOfTime(docURI, versionTimes[cutoffVersion], cutoffVersion + 1, "Next version up from archived should still be there.");
    }

    protected void doTestGetDocWithAsOfTime(String docUri, long timestamp, int expectedVersion, String testMessage) {
        String timestampUri  = docUri + "@t" + timestamp;
        DocumentWithMeta documentWithMeta = docImpl.getDocAndMeta(callingContext, timestampUri);

        assertEquals(testMessage, expectedVersion, (long) documentWithMeta.getMetaData().getVersion());
        assertTrue("Doc content should contain expected data for version.", documentWithMeta.getContent().matches(".*" + expectedVersion + ".*"));
    }

}
