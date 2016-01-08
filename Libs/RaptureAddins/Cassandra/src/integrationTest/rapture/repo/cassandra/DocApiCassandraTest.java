package rapture.repo.cassandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.text.rtf.RTFEditorKit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;

import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentRepoConfig;
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
    private static final String ks = "testKeySpace";
    private static final String colFamilyName = "testColFam";
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
    
    
    static boolean firstTime = true;

    @Test
    public void testCreateAndGetRepo() {
        if (!firstTime && docImpl.docRepoExists(callingContext, docAuthorityURI)) return;
        firstTime = false;
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
        testCreateAndGetRepo();
        testPutAndGetDoc();
        Map<String, String> ret = docImpl.getDocRepoStatus(callingContext, docAuthorityURI);
        assertNotEquals(0, ret.size());
    }
}
