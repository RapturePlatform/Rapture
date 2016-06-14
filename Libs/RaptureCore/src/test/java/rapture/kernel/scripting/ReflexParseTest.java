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
package rapture.kernel.scripting;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureScript;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.api.ScriptApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.HttpScriptApi;
import rapture.common.client.SimpleCredentialsProvider;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.script.IRaptureScript;
import rapture.script.ScriptFactory;

public class ReflexParseTest {
    @Before
    public void setup() {
        ScriptFactory.init();
    }

    private String testScript(String scriptContent) {
        RaptureScript script = new RaptureScript();
        script.setLanguage(RaptureScriptLanguage.REFLEX);
        script.setName("A test program");
        script.setScript(scriptContent);
        IRaptureScript scr = ScriptFactory.getScript(script);
        String res = scr.validateProgram(ContextFactory.getKernelUser(), script);
        System.out.println(String.format("Return from parse check is '%s'", res));
        return res;
    }

    @Test
    public void testSimpleProgram() {
        assertTrue(testScript("println('hello');println(_params['hello']);return 5;\n").isEmpty());
    }

    @Test
    public void testErrorProgram() {
        assertFalse(testScript("x=4 println(test);").isEmpty());
    }

    @Test
    public void RAP3753() {
        ScriptApi scriptApi = Kernel.getScript();
        CallingContext context = ContextFactory.getKernelUser();
        String currScript = "script://" + UUID.randomUUID();

        scriptApi.createScript(context, currScript + "1", RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, "junk");
        String r1 = scriptApi.checkScript(context, currScript + "1");
        assertFalse(r1, StringUtils.isEmpty(r1));

        scriptApi.createScript(context, currScript + "2", RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, "// junk");
        String r2 = scriptApi.checkScript(context, currScript + "2");
        assertTrue(r2, StringUtils.isEmpty(r2));

        try {
            HttpLoginApi loginApi = new HttpLoginApi("http://localhost:8665/rapture", new SimpleCredentialsProvider("rapture", "rapture"));
            loginApi.login();
            HttpScriptApi httpScriptApi = new HttpScriptApi(loginApi);

            httpScriptApi.createScript(currScript + "3", RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, "junk");
            String r3 = httpScriptApi.checkScript(currScript + "3");
            assertFalse(r3, StringUtils.isEmpty(r3));

            httpScriptApi.createScript(currScript + "4", RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, "// junk");
            String r4 = httpScriptApi.checkScript(currScript + "4");
            assertTrue(r4, StringUtils.isEmpty(r4));
        } catch (Exception e) {
            // This will fail if RaptureAPI is not available.
            // It's simply to confirm that it works remotely too.
        }
    }
}
