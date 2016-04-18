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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureScript;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureSnippet;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.ScriptResult;
import rapture.common.api.ScriptingApi;
import rapture.config.ConfigLoader;
import reflex.IReflexHandler;
import reflex.IReflexOutputHandler;
import reflex.ReflexLexer;
import reflex.ReflexParseException;
import reflex.ReflexParser;
import reflex.ReflexTreeWalker;
import reflex.node.ReflexNode;
import reflex.util.InstrumentDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

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
        Kernel.getLock().createLockManager(callingContext, "lock://kernel", "LOCKING USING DUMMY {}", "");
        scriptImpl = new ScriptApiImpl(Kernel.INSTANCE);
        
        Kernel.getIdGen().setupDefaultIdGens(callingContext, false);
        String systemBlobRepo = "//sys.blob";
        Kernel.getBlob().deleteBlobRepo(callingContext, systemBlobRepo);
        Kernel.getBlob().createBlobRepo(callingContext, systemBlobRepo, ConfigLoader.getConf().DefaultSystemBlobConfig,
                ConfigLoader.getConf().DefaultSystemBlobFoldersConfig);

    }

    @Test
    public void testPutScriptWithPath() {
        RaptureScript script = new RaptureScript();
        script.setLanguage(RaptureScriptLanguage.REFLEX);
        script.setAuthority(auth);
        script.setName("Three/Boats/Down/From/The/Candy");
        script.setPurpose(RaptureScriptPurpose.OPERATION);
        script.setParameters(Collections.EMPTY_LIST);
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
        script.setParameters(Collections.EMPTY_LIST);
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
        script.setParameters(Collections.EMPTY_LIST);
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
        script.setParameters(Collections.EMPTY_LIST);
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
        script.setParameters(Collections.EMPTY_LIST);
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

        List<String> removed = scriptImpl.deleteScriptsByUriPrefix(callingContext, uriPrefix + "/folder1/folder2", false);
        assertEquals(2, removed.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, uriPrefix, 0);
        int size = resultsMap.size();
        Set<String> uris = resultsMap.keySet();
        assertEquals(5, size);
        removed = scriptImpl.deleteScriptsByUriPrefix(callingContext, uriPrefix, false);
        assertEquals(4, removed.size());
    }

    public String runScript(String program, Map<String, Object> injectedVars) throws RecognitionException, ReflexParseException {
        final StringBuilder sb = new StringBuilder();

        ReflexLexer lexer = new ReflexLexer();
        lexer.setCharStream(new ANTLRStringStream(program));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ReflexParser parser = new ReflexParser(tokens);

        CommonTree tree = (CommonTree) parser.parse().getTree();

        CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
        ReflexTreeWalker walker = new ReflexTreeWalker(nodes, parser.languageRegistry);

        IReflexHandler handler = walker.getReflexHandler();
        handler.setOutputHandler(new IReflexOutputHandler() {

            @Override
            public boolean hasCapability() {
                return false;
            }

            @Override
            public void printLog(String text) {
                sb.append(text);
            }

            @Override
            public void printOutput(String text) {
                sb.append(text);
            }

            @Override
            public void setApi(ScriptingApi api) {
            }
        });

        if (injectedVars != null && !injectedVars.isEmpty()) {
            for (Map.Entry<String, Object> kv : injectedVars.entrySet()) {
                walker.currentScope.assign(kv.getKey(), kv.getValue() == null ? new ReflexNullValue() : new ReflexValue(kv.getValue()));
            }
        }

        @SuppressWarnings("unused")
        ReflexNode returned = walker.walk();
        InstrumentDebugger instrument = new InstrumentDebugger();
        instrument.setProgram(program);
        ReflexValue retVal = (returned == null) ? null : returned.evaluateWithoutScope(instrument);
        instrument.getInstrumenter().log();
        return sb.toString();
    }

    @Test
    public void params() throws RecognitionException {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "one");
        String program = "b='two';\n println(\"${a}${b}\"); \n";

        String output = runScript(program, map);
        assertEquals("onetwo", output.trim());
    }

    @Test
    public void testScriptWithParams() {
        RaptureScript script = new RaptureScript();
        CallingContext ctx = ContextFactory.getKernelUser();
        script.setAuthority(auth);
        script.setLanguage(RaptureScriptLanguage.REFLEX);
        script.setName("Candy");
        script.setPurpose(RaptureScriptPurpose.PROGRAM);
        script.setParameters(Collections.EMPTY_LIST);
        String scriptWrite = "meta do \n" + "return string,'Just the parameters put together'; \n" + "param 'a',string,'The a parameter'; \n"
                + "param 'b',string,'The b parameter'; \n" + "param 'c',string,'The c parameter'; \n" + "param 'd',string,'The d parameter'; \n"
                + "property 'color','blue'; \n" + "end \n" + "println(\"a is ${a}\"); \n" + "println(\"b is ${b}\"); \n" + "println(\"c is ${c}\"); \n"
                + "println(\"d is ${d}\"); \n" + "return \"${a}${b}${c}${d}\"; \n";
        script.setScript(scriptWrite);
        scriptImpl.putScript(ctx, script.getAddressURI().toString(), script);
        RaptureScript scriptRead = scriptImpl.getScript(ctx, script.getAddressURI().toString());
        assertEquals(scriptWrite, scriptRead.getScript());

        Map<String, String> parameters = new HashMap<>();
        parameters.put("a", "A");
        parameters.put("b", "B");
        parameters.put("c", "C");
        parameters.put("d", "D");
        String name = "key";
        String ret = Kernel.getScript().runScript(ctx, script.getAddressURI().toString(), parameters);
        assertEquals("ABCD", ret);
    }

    @Test
    public void testScriptExtendedWithParams() {
        RaptureScript script = new RaptureScript();
        CallingContext ctx = ContextFactory.getKernelUser();
        script.setAuthority(auth);
        script.setLanguage(RaptureScriptLanguage.REFLEX);
        script.setName("Candy");
        script.setPurpose(RaptureScriptPurpose.PROGRAM);
        script.setParameters(Collections.EMPTY_LIST);
        String scriptWrite = "meta do \n" + "return string,'Just the parameters put together'; \n" + "param 'a',string,'The a parameter'; \n"
                + "param 'b',string,'The b parameter'; \n" + "param 'c',string,'The c parameter'; \n" + "param 'd',string,'The d parameter'; \n"
                + "property 'color','blue'; \n" + "end \n" + "println(\"a is ${a}\"); \n" + "println(\"b is ${b}\"); \n" + "println(\"c is ${c}\"); \n"
                + "println(\"d is ${d}\"); \n" + "return \"${a}${b}${c}${d}\"; \n";
        script.setScript(scriptWrite);
        scriptImpl.putScript(ctx, script.getAddressURI().toString(), script);
        RaptureScript scriptRead = scriptImpl.getScript(ctx, script.getAddressURI().toString());
        assertEquals(scriptWrite, scriptRead.getScript());

        Map<String, String> parameters = new HashMap<>();
        parameters.put("a", "A");
        parameters.put("b", "B");
        parameters.put("c", "C");
        parameters.put("d", "D");
        String name = "key";
        ScriptResult ret = Kernel.getScript().runScriptExtended(ctx, script.getAddressURI().toString(), parameters);
        assertEquals("ABCD", ret.getReturnValue());
    }
}
