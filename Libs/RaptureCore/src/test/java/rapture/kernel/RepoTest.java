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

import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.ContentEnvelope;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentRepoConfig;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.repo.Repository;

public class RepoTest {

    private RepoApiImpl repo;
    private CallingContext callingContext;
    private static String config = "REP {} USING MEMORY {}";
    private String testTypeURI = "document://test/";
    private String repoUri = "document://test" + UUID.randomUUID();

    @Before
    public void setUp() {
        RaptureConfig.setLoadYaml(false);
        Kernel.initBootstrap();
        this.repo = new RepoApiImpl(Kernel.INSTANCE);
        this.callingContext = new CallingContext();
        this.callingContext.setUser("dummy");
        Kernel.INSTANCE.clearRepoCache(false);
    }

    @After
    public void tearDown() {
        Kernel.INSTANCE.clearRepoCache(false);
    }

    private void maybeCreateDocRepo() {
        if (!Kernel.getDoc().docRepoExists(callingContext, testTypeURI)) {
            Kernel.getDoc().createDocRepo(callingContext, testTypeURI, config);
        }

    }

    @SuppressWarnings("deprecation")
    @Test
    public void testPutContent() {
        maybeCreateDocRepo();
        String content = "{\"content\":\"This is my content, here it roar\"}";
        repo.putContent(callingContext, "document://test/testtype/document", content, null);

        ContentEnvelope document = repo.getContent(callingContext, "document://test/testtype/document");
        assertEquals(content, document.getContent());
    }

    @Test
    public void testGetRepositories() {
        List<DocumentRepoConfig> documentRepositories = Kernel.getDoc().getDocRepoConfigs(callingContext);
        int preexisting = documentRepositories.size();
        if (!Kernel.getDoc().docRepoExists(callingContext, repoUri)) {
            Kernel.getDoc().createDocRepo(callingContext, repoUri, config);
        }
        documentRepositories = Kernel.getDoc().getDocRepoConfigs(callingContext);
        assertEquals(JacksonUtil.jsonFromObject(documentRepositories), preexisting + 1, documentRepositories.size());
    }

    @Test
    public void testStandardTemplate() {
        ConfigLoader.getConf().StandardTemplate = "NREP {} USING MEMORY { prefix=\"${partition}.${type}\"}";
        Repository repo = Kernel.getKernel().getRepo("repoNotYetCreated");
        assertNotNull(repo);
    }
}
