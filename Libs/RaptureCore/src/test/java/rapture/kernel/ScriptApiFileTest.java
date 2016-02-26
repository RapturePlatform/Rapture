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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureParameter;
import rapture.common.RaptureScript;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureSnippet;
import rapture.common.RaptureURI;
import rapture.common.Scheme;

public class ScriptApiFileTest extends AbstractFileTest {

    private static final String auth = "test" + System.currentTimeMillis();
    private static final String REPO_USING_FILE = "REP {} USING FILE {prefix=\"/tmp/" + auth + "\"}";
    private static final File temp = new File("/tmp/" + auth);
    private static final String scriptAuthorityURI = "script://" + auth;
    private static final String scriptURI = scriptAuthorityURI + "/For/A/Jesters/Tear";

    private static ScriptApiImpl scriptImpl;

    String saveRaptureRepo;
    String saveInitSysConfig;

    @BeforeClass
    static public void setUp() {
        AbstractFileTest.setUp();
        config.RaptureRepo = REPO_USING_FILE;
        config.InitSysConfig = "NREP {} USING FILE { prefix=\"/tmp/" + auth + "/sys.config\"}";

        Kernel.initBootstrap();
        callingContext = ContextFactory.getKernelUser();

        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using FILE {prefix=\"/tmp/" + auth + "\"}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");
        scriptImpl = new ScriptApiImpl(Kernel.INSTANCE);
    }

    @Test
    public void testPutScriptWithPath() {
        RaptureScript script = new RaptureScript();
        script.setLanguage(RaptureScriptLanguage.REFLEX);
        script.setAuthority(auth);
        script.setName("Three/Boats/Down/From/The/Candy");
        script.setPurpose(RaptureScriptPurpose.OPERATION);
        script.setParameters(new ArrayList<RaptureParameter>());
        String scriptWrite = "// I'm a market square hero";
        script.setScript(scriptWrite);
        scriptImpl.putScript(ContextFactory.getKernelUser(), "//" + UUID.randomUUID(), script);
        RaptureScript scriptRead = scriptImpl.getScript(ContextFactory.getKernelUser(), script.getAddressURI().toString());
        assertEquals(scriptWrite, scriptRead.getScript());
    }

    @Test
    public void testPutScriptWithNoRepo() {
        RaptureScript script = new RaptureScript();
        script.setLanguage(RaptureScriptLanguage.REFLEX);
        script.setName("Grendel");
        script.setPurpose(RaptureScriptPurpose.PROGRAM);
        script.setParameters(new ArrayList<RaptureParameter>());
        String scriptWrite = "// do nothing";
        script.setScript(scriptWrite);
        scriptImpl.putScript(ContextFactory.getKernelUser(), "//" + UUID.randomUUID(), script);
        RaptureScript scriptRead = scriptImpl.getScript(ContextFactory.getKernelUser(), script.getAddressURI().toString());
        assertEquals(scriptWrite, scriptRead.getScript());
    }

    @Test
    public void testPutScript() {
        RaptureScript script = new RaptureScript();
        script.setAuthority(auth);
        script.setLanguage(RaptureScriptLanguage.REFLEX);
        script.setName("Candy");
        script.setPurpose(RaptureScriptPurpose.PROGRAM);
        script.setParameters(new ArrayList<RaptureParameter>());
        String scriptWrite = "// do nothing";
        script.setScript(scriptWrite);
        scriptImpl.putScript(ContextFactory.getKernelUser(), script.getAddressURI().toString(), script);
        RaptureScript scriptRead = scriptImpl.getScript(ContextFactory.getKernelUser(), script.getAddressURI().toString());
        assertEquals(scriptWrite, scriptRead.getScript());
    }

    @Test
    public void testPutSnippet() {
        String snipUri = auth + "/the/web";
        String script = "// And thus begins the web";
        RaptureSnippet snip = scriptImpl.createSnippet(ContextFactory.getKernelUser(), snipUri, script);
        RaptureSnippet snipRead = scriptImpl.getSnippet(ContextFactory.getKernelUser(), snipUri);
        assertEquals(script, snipRead.getSnippet());

        String snipUri2 = UUID.randomUUID() + "/the/web";
        RaptureSnippet snip2 = scriptImpl.createSnippet(ContextFactory.getKernelUser(), snipUri2, script);
        RaptureSnippet snip2Read = scriptImpl.getSnippet(ContextFactory.getKernelUser(), snipUri);
        assertEquals(script, snip2Read.getSnippet());
    }

    @Test
    public void testlistByUriPrefix() {
        CallingContext callingContext = getCallingContext();

        RaptureScript script = new RaptureScript();
        script.setLanguage(RaptureScriptLanguage.REFLEX);
        script.setAuthority(auth);
        script.setPurpose(RaptureScriptPurpose.OPERATION);
        script.setParameters(new ArrayList<RaptureParameter>());
        String scriptWrite = "// I'm a market square hero";
        script.setScript(scriptWrite);

        String uriPrefix = auth + "/uriFragment";
        script.setName("uriFragment/script1");
        scriptImpl.putScript(callingContext, uriPrefix + "/script1", script);
        script.setName("uriFragment/script2");
        scriptImpl.putScript(callingContext, uriPrefix + "/script2", script);
        script.setName("uriFragment/folder1/script3");
        scriptImpl.putScript(callingContext, uriPrefix + "/folder1/script3", script);
        script.setName("uriFragment/folder1/script4");
        scriptImpl.putScript(callingContext, uriPrefix + "/folder1/script4", script);
        script.setName("uriFragment/folder1/folder2/script5");
        scriptImpl.putScript(callingContext, uriPrefix + "/folder1/folder2/script5", script);
        script.setName("uriFragment/folder1/folder2/script6");
        scriptImpl.putScript(callingContext, uriPrefix + "/folder1/folder2/script6", script);

        Map<String, RaptureFolderInfo> resultsMap;
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, uriPrefix, -1);
        assertEquals(8, resultsMap.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, uriPrefix, 0);
        assertEquals(8, resultsMap.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, uriPrefix, 4);
        assertEquals(8, resultsMap.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, uriPrefix, 3);
        assertEquals(8, resultsMap.size());

        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, uriPrefix, 2);
        assertEquals(6, resultsMap.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, uriPrefix, 1);
        assertEquals(3, resultsMap.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, uriPrefix + "/folder1", 1);
        assertEquals(3, resultsMap.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, uriPrefix + "/folder1", 2);
        assertEquals(5, resultsMap.size());

        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, scriptAuthorityURI, -1);
        assertEquals(9, resultsMap.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, scriptAuthorityURI, 0);
        assertEquals(9, resultsMap.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, scriptAuthorityURI, 4);
        assertEquals(9, resultsMap.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, scriptAuthorityURI, 3);
        assertEquals(7, resultsMap.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, scriptAuthorityURI, 2);
        assertEquals(4, resultsMap.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, scriptAuthorityURI, 1);
        assertEquals(1, resultsMap.size());

        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, "script://", 1);
        assertEquals(1, resultsMap.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, "script://", 2);
        assertEquals(2, resultsMap.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, "script://", 3);
        assertEquals(5, resultsMap.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, "script://", 4);
        assertEquals(8, resultsMap.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, "script://", 5);
        assertEquals(10, resultsMap.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, "script://", 6);
        assertEquals(10, resultsMap.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, "script://", 0);
        assertEquals(10, resultsMap.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, "script://", -1);
        assertEquals(10, resultsMap.size());

    }

    @Test
    public void testDeleteScriptsByUriPrefix() throws InterruptedException {
        CallingContext callingContext = getCallingContext();

        RaptureScript script = new RaptureScript();
        script.setLanguage(RaptureScriptLanguage.REFLEX);
        script.setAuthority(auth);
        script.setPurpose(RaptureScriptPurpose.OPERATION);
        script.setParameters(new ArrayList<RaptureParameter>());
        String scriptWrite = "// I'm a market square hero";
        script.setScript(scriptWrite);

        String uriPrefix = auth + "/uriFragment";
        script.setName("uriFragment/script1");
        scriptImpl.putScript(callingContext, uriPrefix + "/script1", script);
        script.setName("uriFragment/script2");
        scriptImpl.putScript(callingContext, uriPrefix + "/script2", script);
        script.setName("uriFragment/folder1/script3");
        scriptImpl.putScript(callingContext, uriPrefix + "/folder1/script3", script);
        script.setName("uriFragment/folder1/script4");
        scriptImpl.putScript(callingContext, uriPrefix + "/folder1/script4", script);
        script.setName("uriFragment/folder1/folder2/script5");
        scriptImpl.putScript(callingContext, uriPrefix + "/folder1/folder2/script5", script);
        script.setName("uriFragment/folder1/folder2/script6");
        scriptImpl.putScript(callingContext, uriPrefix + "/folder1/folder2/script6", script);

        Map<String, RaptureFolderInfo> resultsMap;
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, uriPrefix, 0);
        assertEquals(8, resultsMap.size());

        List<String> removed = scriptImpl.deleteScriptsByUriPrefix(callingContext, uriPrefix + "/folder1/folder2");
        assertEquals(2, removed.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, uriPrefix, 0);
        int size = resultsMap.size();
        Set<String> uris = resultsMap.keySet();
        assertEquals(5, size);
        removed = scriptImpl.deleteScriptsByUriPrefix(callingContext, uriPrefix);
        assertEquals(4, removed.size());
    }
}
