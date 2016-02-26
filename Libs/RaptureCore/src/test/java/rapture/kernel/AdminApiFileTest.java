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
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.model.DocumentRepoConfig;
import rapture.config.RaptureConfig;

/**
 * 
 * @author qqqq
 *
 */
public class AdminApiFileTest {

    private static final Logger log = Logger.getLogger(AdminApiFileTest.class);
    private static final String auth = "muse-" + System.currentTimeMillis();
    private static final String REPO_USING_FILE1 = "REP {} USING FILE {prefix=\"/tmp/" + auth + "\"}";
    private static final String REPO_USING_FILE2 = "REP {} USING FILE {prefix=\"/tmp/new_" + auth + "\"}";
//    private static final String REPO_USING_MEMORY = "REP {} USING MEMORY {}";
    private static final File temp = new File("/tmp/" + auth);
    private static final String docContent = "{\"content\":\"This is the last time I will abandon you. I wish I could.\"}";
    private static final String docAuthorityURI = "document://" + auth;
    private static final String docURI = docAuthorityURI + "/stockholm/syndrome";

    private CallingContext callingContext;
    private DocApiImpl docImpl;
    private AdminApiImpl adminImpl;

    @Before
    public void before() {
        temp.deleteOnExit();
        RaptureConfig.setLoadYaml(false);
        System.setProperty("LOGSTASH-ISENABLED", "false");
        this.callingContext = new CallingContext();
        this.callingContext.setUser("dummy");

        Kernel.initBootstrap();
        callingContext = ContextFactory.getKernelUser();

        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");
        docImpl = new DocApiImpl(Kernel.INSTANCE);
        adminImpl = new AdminApiImpl(Kernel.INSTANCE);
    }

    @After
    public void after() throws Exception {
        FileUtils.deleteDirectory(temp);
    }

    @Ignore // initiateTypeConversion does not work.
    @Test
    public void testInitiateTypeConversion() {
        // START BY CREATING A MEMORY REPO
        
        String authURI = "//auth";
        String doc1URI = authURI+"/dir1/doc1";
        String doc2URI = authURI+"/dir2/doc2";
        
        docImpl.createDocRepo(callingContext, authURI, REPO_USING_FILE1);
        DocumentRepoConfig docRepoConfig = docImpl.getDocRepoConfig(callingContext, authURI);
        assertNotNull(docRepoConfig);
        assertEquals(REPO_USING_FILE1, docRepoConfig.getDocumentRepo().getConfig());
        assertEquals("auth", docRepoConfig.getAuthority());

        // Now write some documents to that repo
        
        docImpl.putDoc(callingContext, doc1URI, docContent);
        String doc1 = docImpl.getDoc(callingContext, doc1URI);
        assertEquals(docContent, doc1);

        // Now the fun part.
        adminImpl.initiateTypeConversion(callingContext, "document:"+authURI, REPO_USING_FILE2, 0);
        try {
            Thread.sleep(600000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        doc1 = docImpl.getDoc(callingContext, doc1URI);
        assertEquals(docContent, doc1);

        docRepoConfig = docImpl.getDocRepoConfig(callingContext, authURI);
        assertEquals(REPO_USING_FILE2, docRepoConfig.getDocumentRepo().getConfig());
        
        docImpl.putDoc(callingContext, doc2URI, docContent);
        String doc2 = docImpl.getDoc(callingContext, doc2URI);
        assertEquals(docContent, doc2);

    }
}
