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
package rapture.script.js;

import static org.junit.Assert.assertEquals;

import rapture.common.CallingContext;
import rapture.common.RaptureScript;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.ScriptResult;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.ScriptApiImplWrapper;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class JavaScriptTest {

    private static final String JAVASCRIPT_SCRIPT_URI = "//longhorn/JStest.js";
    private static final String REFLEX_SCRIPT_URI = "//reflex/reflexTest.rfx";
    private CallingContext context;
    private ScriptApiImplWrapper scriptAPI;

    @Before
    public void setUp() {
        Kernel.initBootstrap();
        context = ContextFactory.getKernelUser();
        scriptAPI = Kernel.getScript();
    }

    @After
    public void tearDown() {
        scriptAPI.deleteScript(context, JAVASCRIPT_SCRIPT_URI);
        scriptAPI.deleteScript(context, REFLEX_SCRIPT_URI);
    }

    @Test
    public void testPutAndRunJSScript() {
        scriptAPI.createScript(context, JAVASCRIPT_SCRIPT_URI, RaptureScriptLanguage.JAVASCRIPT, RaptureScriptPurpose.PROGRAM,
                "var reflexScript = scriptAPI.getScript(\"//reflex/reflexTest.rfx\");\n print(\"This is JavaScript \\n\");\n reflexScript;\n");
        scriptAPI.createScript(context, REFLEX_SCRIPT_URI, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, "print(\"This is reflex\");");
        String runScriptOut = scriptAPI.runScript(context, JAVASCRIPT_SCRIPT_URI, new HashMap<String, String>());
        RaptureScript script = scriptAPI.getScript(context, "//reflex/reflexTest.rfx");
        assertEquals(JacksonUtil.jsonFromObject(script), runScriptOut);
    }

    @Test
    public void testPutAndRunJSScriptWithParams() {
        scriptAPI.createScript(context, JAVASCRIPT_SCRIPT_URI, RaptureScriptLanguage.JAVASCRIPT, RaptureScriptPurpose.PROGRAM, "_params.myparam");
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("myparam", "myParamValue");
        String runScriptOut = scriptAPI.runScript(context, JAVASCRIPT_SCRIPT_URI, params);
        assertEquals("\"myParamValue\"", runScriptOut);
    }

    @Test
    public void testPutAndRunJSScriptExtended() {
        scriptAPI.createScript(context, JAVASCRIPT_SCRIPT_URI, RaptureScriptLanguage.JAVASCRIPT, RaptureScriptPurpose.PROGRAM,
                "var reflexScript = scriptAPI.getScript(\"//reflex/reflexTest.rfx\");\n print(\"This is JavaScript \\n\");\n reflexScript;\n");
        scriptAPI.createScript(context, REFLEX_SCRIPT_URI, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, "print(\"This is reflex\");");
        ScriptResult runScriptOut = scriptAPI.runScriptExtended(context, JAVASCRIPT_SCRIPT_URI, new HashMap<String, String>());
        RaptureScript script = scriptAPI.getScript(context, "//reflex/reflexTest.rfx");
        assertEquals(
                JacksonUtil.jsonFromObject(script),
                runScriptOut.getReturnValue());
    }

    @Ignore
    @Test
    public void testValidateFail() {
        scriptAPI.createScript(context, JAVASCRIPT_SCRIPT_URI, RaptureScriptLanguage.JAVASCRIPT, RaptureScriptPurpose.PROGRAM,
                "print(\"test\");\n rubbish !#ASFD; \n");
        String checkScript = scriptAPI.checkScript(context, JAVASCRIPT_SCRIPT_URI);
        // this test is not valid because it assumes a particular version of the compiler and a particular locale.
        assertEquals("sun.org.mozilla.javascript.internal.EvaluatorException: missing ; before statement (<Unknown Source>#2)", checkScript);

    }

    @Test
    public void testValidate() {
        scriptAPI.createScript(context, JAVASCRIPT_SCRIPT_URI, RaptureScriptLanguage.JAVASCRIPT, RaptureScriptPurpose.PROGRAM, "print(\"test\");\n");
        String checkScript = scriptAPI.checkScript(context, JAVASCRIPT_SCRIPT_URI);
        assertEquals("", checkScript);
    }
}
