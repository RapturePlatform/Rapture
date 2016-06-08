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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentRepoConfig;
import rapture.common.model.DocumentWithMeta;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;

public class DocApiMemoryTest {

    private static final Logger log = Logger.getLogger(DocApiMemoryTest.class);
    private static final String auth = "test" + System.currentTimeMillis();
    private static final String REPO_USING_MEMORY = "REP {} USING MEMORY {prefix=\"/tmp/" + auth + "\"}";
    private static final String GET_ALL_BASE = "document://getAll";
    private static final File temp = new File("/tmp/" + auth);
    private static final String docContent = "{\"content\":\"Cold and misty morning I had heard a warning borne in the air\"}";
    private static final String docAuthorityURI = "document://" + auth;
    private static final String docURI = docAuthorityURI + "/brain/salad/surgery";

    String saveInitSysConfig;
    String saveRaptureRepo;

    private CallingContext callingContext;
    private DocApiImpl docImpl;

    @Before
    public void setUp() {
        temp.mkdir();
        temp.deleteOnExit();
        RaptureConfig.setLoadYaml(false);
        RaptureConfig config = ConfigLoader.getConf();
        saveRaptureRepo = config.RaptureRepo;
        saveInitSysConfig = config.InitSysConfig;

        config.RaptureRepo = REPO_USING_MEMORY;
        config.InitSysConfig = "NREP {} USING MEMORY { prefix=\"/tmp/" + auth + "/sys.config\"}";
        // config.DefaultPipelineTaskStatus = "TABLE {} USING FILE {prefix=\"/tmp/" + auth + "\"}";

        System.setProperty("LOGSTASH-ISENABLED", "false");
        this.callingContext = new CallingContext();
        this.callingContext.setUser("dummy");

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
        docImpl.createDocRepo(callingContext, docAuthorityURI, REPO_USING_MEMORY);
        DocumentRepoConfig docRepoConfig = docImpl.getDocRepoConfig(callingContext, docAuthorityURI);
        assertNotNull(docRepoConfig);
        assertEquals(REPO_USING_MEMORY, docRepoConfig.getDocumentRepo().getConfig());
        assertEquals(auth, docRepoConfig.getAuthority());
    }
    
    @Test
    public void testGetDocRepoStatus() {
        testCreateAndGetRepo();
        Map<String, String> ret = docImpl.getDocRepoStatus(callingContext, docAuthorityURI);
        assertNotEquals(0, ret.size());
    }
    
    @Test
    public void testMetadata() {
        testCreateAndGetRepo();
        String docUri = RaptureURI.builder(new RaptureURI(docAuthorityURI)).docPath("cha/cha/slide").asString();
        docImpl.putDoc(callingContext, docUri, "{\"Everybody\":\"clap your hands\"}");
        DocumentMetadata met1 = docImpl.getDocMeta(callingContext, docUri);
        DocumentWithMeta met2 = docImpl.getDocAndMeta(callingContext, docUri);
        List<DocumentWithMeta> met3 = docImpl.getDocAndMetas(callingContext, ImmutableList.of(docUri));

        assertNotNull(met1);
        assertNotNull(met2);
        assertNotNull(met3);
        assertEquals(1, met3.size());
        System.out.println(met1.toString());
        System.out.println(met2.toString());
        System.out.println(met3.get(0).toString());
        assertEquals(met2, met3.get(0));
        assertEquals(met1, met2.getMetaData());
    }

    @After
    public void after() throws IOException {
        FileUtils.deleteDirectory(temp);
        ConfigLoader.getConf().InitSysConfig = saveInitSysConfig;
        ConfigLoader.getConf().RaptureRepo = saveRaptureRepo;
    }
}