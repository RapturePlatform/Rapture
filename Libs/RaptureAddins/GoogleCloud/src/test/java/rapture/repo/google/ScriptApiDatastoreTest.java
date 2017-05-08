/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Incapture Technologies LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation GCP_DATASTOREs (the "Software"), to deal
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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import rapture.kernel.AbstractFileTest;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.ScriptApiImpl;
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

public class ScriptApiDatastoreTest extends AbstractFileTest {

    private static final String auth = "test" + System.currentTimeMillis();
    private static final String REPO_USING_GCP_DATASTORE = "REP {} USING GCP_DATASTORE {projectid=\"todo3-incap\", prefix=\"/tmp/" + auth + "\"}";
    private static final File temp = new File("/tmp/" + auth);
    private static final String scriptAuthorityURI = "script://" + auth;
    private static final String scriptURI = scriptAuthorityURI + "/For/A/Jesters/Tear";

    private static ScriptApiImpl scriptImpl;

    String saveRaptureRepo;
    String saveInitSysConfig;

    @BeforeClass
    static public void setUp() {
        AbstractFileTest.setUp();
        config.RaptureRepo = REPO_USING_GCP_DATASTORE;
        config.InitSysConfig = "NREP {} USING GCP_DATASTORE { projectid=\"todo3-incap\", prefix=\"/tmp/" + auth + "/sys.config\"}";

        Kernel.initBootstrap();
        callingContext = ContextFactory.getKernelUser();

        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {projectid=\"todo3-incap\", prefix=\"/tmp/" + auth + "\"}");
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
        @SuppressWarnings("unused")
        RaptureSnippet snip = scriptImpl.createSnippet(ContextFactory.getKernelUser(), snipUri, script);
        RaptureSnippet snipRead = scriptImpl.getSnippet(ContextFactory.getKernelUser(), snipUri);
        assertEquals(script, snipRead.getSnippet());

        script = "// Compliments on unnatural size";
        String snipUri2 = UUID.randomUUID() + "/the/web";
        @SuppressWarnings("unused")
        RaptureSnippet snip2 = scriptImpl.createSnippet(ContextFactory.getKernelUser(), snipUri2, script);
        RaptureSnippet snip2Read = scriptImpl.getSnippet(ContextFactory.getKernelUser(), snipUri2);
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
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, "script://thiswontexist", -1);
        assertTrue(resultsMap.isEmpty());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, "script://thiswontexist/so/returnempty", -1);
        assertTrue(resultsMap.isEmpty());
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

        List<String> removed = scriptImpl.deleteScriptsByUriPrefix(callingContext, uriPrefix + "/folder1/folder2");
        assertEquals(2, removed.size());
        resultsMap = scriptImpl.listScriptsByUriPrefix(callingContext, uriPrefix, 0);
        int size = resultsMap.size();
        Set<String> uris = resultsMap.keySet();
        assertEquals(5, size);
        removed = scriptImpl.deleteScriptsByUriPrefix(callingContext, uriPrefix);
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

        ReflexNode returned = walker.walk();
        InstrumentDebugger instrument = new InstrumentDebugger();
        instrument.setProgram(program);
        @SuppressWarnings("unused")
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
        String ret = Kernel.getScript().runScript(ctx, script.getAddressURI().toString(), parameters);
        assertEquals("ABCD", ret);
        scriptImpl.deleteScript(ctx, script.getAddressURI().toString());
    }

    @Test
    public void testScriptExtendedWithParams() {
        RaptureScript script = new RaptureScript();
        CallingContext ctx = ContextFactory.getKernelUser();
        script.setAuthority(auth);
        script.setLanguage(RaptureScriptLanguage.REFLEX);
        script.setName("Candy");
        script.setPurpose(RaptureScriptPurpose.PROGRAM);
        String scriptWrite = "meta do \n" + "return string,'Just the parameters put together'; \n" + "param 'a',map,'The a parameter'; \n"
                + "param 'b',number,'The b parameter'; \n" + "param 'c',boolean,'The c parameter'; \n" + "param 'd',list,'The d parameter'; \n"
                + "param 'i',integer,'The i parameter'; \n" + "property 'color','blue'; \n" + "end \n" + "println(a); \n" + "println(b); \n" + "println(c); \n"
                + "println(d); \n" + "aa=${a}; aa.W='X'; \n aa['Y']='Z'; \n println(\"a is ${a}\"); \n" + "println(\"b is ${b}\"); \n"
                + "println(\"c is ${c}\"); \n" + "println(\"d is ${d}\"); \n" + "e=2 * ${b}; \n" + "f = !${c}; \n"
                + "return \"${a} ${aa} ${b} ${c} ${d} ${e} ${f} ${i}\";\n";
        script.setScript(scriptWrite);
        scriptImpl.putScript(ctx, script.getAddressURI().toString(), script);
        RaptureScript scriptRead = scriptImpl.getScript(ctx, script.getAddressURI().toString());
        assertEquals(scriptWrite, scriptRead.getScript());

        Map<String, String> parameters = new HashMap<>();
        parameters.put("a", "{\"A\":\"B\",\"C\":\"D\"}");
        parameters.put("b", "-6e08");
        parameters.put("i", "2.112e03");
        parameters.put("c", Boolean.TRUE.toString());
        parameters.put("d", "[1,2,3,4,5]");
        ScriptResult ret = Kernel.getScript().runScriptExtended(ctx, script.getAddressURI().toString(), parameters);
        assertEquals("{A=B, C=D} {A=B, C=D, W=X, Y=Z} -6E+8 true [1, 2, 3, 4, 5] -1.2E+9 false 2112", ret.getReturnValue());
        scriptImpl.deleteScript(ctx, script.getAddressURI().toString());
    }

    @Test
    public void testScriptReturnsInteger() {
        RaptureScript script = new RaptureScript();
        CallingContext ctx = ContextFactory.getKernelUser();
        script.setAuthority(auth);
        script.setLanguage(RaptureScriptLanguage.REFLEX);
        script.setName("Candy");
        script.setPurpose(RaptureScriptPurpose.PROGRAM);
        String scriptWrite = "meta do \n return integer,'Half the supplied value'; \n param 'a',number,'A number'; \n end \n return ${a} / 2;\n";
        script.setScript(scriptWrite);
        scriptImpl.putScript(ctx, script.getAddressURI().toString(), script);
        RaptureScript scriptRead = scriptImpl.getScript(ctx, script.getAddressURI().toString());
        assertEquals(scriptWrite, scriptRead.getScript());

        Map<String, String> parameters = new HashMap<>();
        parameters.put("a", "2.112e03");
        ScriptResult ret = Kernel.getScript().runScriptExtended(ctx, script.getAddressURI().toString(), parameters);
        assertEquals(new Integer(1056), ret.getReturnValue());
        scriptImpl.deleteScript(ctx, script.getAddressURI().toString());
    }

    @Test
    public void testScriptReturnsBoolean() {
        RaptureScript script = new RaptureScript();
        CallingContext ctx = ContextFactory.getKernelUser();
        script.setAuthority(auth);
        script.setLanguage(RaptureScriptLanguage.REFLEX);
        script.setName("Candy");
        script.setPurpose(RaptureScriptPurpose.PROGRAM);
        String scriptWrite = "meta do \n return boolean,'True if a is 2112'; \n param 'a',number,'A number'; \n end \n return ${a} == 2112;\n";
        script.setScript(scriptWrite);
        scriptImpl.putScript(ctx, script.getAddressURI().toString(), script);
        RaptureScript scriptRead = scriptImpl.getScript(ctx, script.getAddressURI().toString());
        assertEquals(scriptWrite, scriptRead.getScript());

        Map<String, String> parameters = new HashMap<>();
        parameters.put("a", "2.112e03");
        ScriptResult ret = Kernel.getScript().runScriptExtended(ctx, script.getAddressURI().toString(), parameters);
        assertEquals(Boolean.TRUE, ret.getReturnValue());
        scriptImpl.deleteScript(ctx, script.getAddressURI().toString());
    }

    @Test
    public void testScriptReturnsMap() {
        RaptureScript script = new RaptureScript();
        CallingContext ctx = ContextFactory.getKernelUser();
        script.setAuthority(auth);
        script.setLanguage(RaptureScriptLanguage.REFLEX);
        script.setName("Candy");
        script.setPurpose(RaptureScriptPurpose.PROGRAM);
        String scriptWrite = "meta do \n return map,'map[b:a]'; \n param 'a',number,'A number'; \n param 'b',string,'A string'; \n end \n c = {}; \n c[b]=a; \n return ${c};\n";
        script.setScript(scriptWrite);
        scriptImpl.putScript(ctx, script.getAddressURI().toString(), script);
        RaptureScript scriptRead = scriptImpl.getScript(ctx, script.getAddressURI().toString());
        assertEquals(scriptWrite, scriptRead.getScript());

        Map<String, String> parameters = new HashMap<>();
        parameters.put("a", "2112");
        parameters.put("b", "Rush");
        ScriptResult ret = Kernel.getScript().runScriptExtended(ctx, script.getAddressURI().toString(), parameters);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("Rush", BigDecimal.valueOf(2112));
        Map retmap = (Map) ret.getReturnValue();
        assertEquals(map.size(), retmap.size());
        for (String key : map.keySet()) {
            assertEquals(map.get(key), retmap.get(key));
        }
        scriptImpl.deleteScript(ctx, script.getAddressURI().toString());
    }

    @Test
    public void testScriptReturnsList() {
        RaptureScript script = new RaptureScript();
        CallingContext ctx = ContextFactory.getKernelUser();
        script.setAuthority(auth);
        script.setLanguage(RaptureScriptLanguage.REFLEX);
        script.setName("Candy");
        script.setPurpose(RaptureScriptPurpose.PROGRAM);
        String scriptWrite = "meta do \n return list,'list[b,a]'; \n param 'a',number,'A number'; \n param 'b',string,'A string'; \n end \n c = []; \n c+=a; \n c+=b; \n return ${c};\n";
        script.setScript(scriptWrite);
        scriptImpl.putScript(ctx, script.getAddressURI().toString(), script);
        RaptureScript scriptRead = scriptImpl.getScript(ctx, script.getAddressURI().toString());
        assertEquals(scriptWrite, scriptRead.getScript());

        Map<String, String> parameters = new HashMap<>();
        parameters.put("a", "2112");
        parameters.put("b", "Rush");
        ScriptResult ret = Kernel.getScript().runScriptExtended(ctx, script.getAddressURI().toString(), parameters);
        List<Object> list = new ArrayList<>();
        list.add("Rush");
        list.add(BigDecimal.valueOf(2112));
        List retlist = (List) ret.getReturnValue();
        assertEquals(list.size(), retlist.size());
        scriptImpl.deleteScript(ctx, script.getAddressURI().toString());
    }

    @Test
    public void testScriptParamsCannotOverwrite() {
        RaptureScript script = new RaptureScript();
        CallingContext ctx = ContextFactory.getKernelUser();
        script.setAuthority(auth);
        script.setLanguage(RaptureScriptLanguage.REFLEX);
        script.setName("Candy");
        script.setPurpose(RaptureScriptPurpose.PROGRAM);
        String scriptWrite = "meta do \n param 'a',number,'A number'; \n param 'b',string,'A string'; \n end \n a=1984;\n b='Van Halen';\n return \"${a} ${b}\";\n";
        script.setScript(scriptWrite);
        scriptImpl.putScript(ctx, script.getAddressURI().toString(), script);
        RaptureScript scriptRead = scriptImpl.getScript(ctx, script.getAddressURI().toString());
        assertEquals(scriptWrite, scriptRead.getScript());

        Map<String, String> parameters = new HashMap<>();
        parameters.put("a", "2112");
        parameters.put("b", "Rush");
        ScriptResult ret = Kernel.getScript().runScriptExtended(ctx, script.getAddressURI().toString(), parameters);
        assertEquals("1984 Van Halen", ret.getReturnValue().toString());

        scriptImpl.deleteScript(ctx, script.getAddressURI().toString());
    }

}
