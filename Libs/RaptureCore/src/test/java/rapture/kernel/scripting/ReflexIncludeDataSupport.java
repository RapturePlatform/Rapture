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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class ReflexIncludeDataSupport {
    private static CallingContext ctx = ContextFactory.getKernelUser();

    String auth = "//reflexIncludeDataAuthority";
    
    @After
    public void cleanUp() {
        Kernel.getDoc().deleteDocRepo(ctx, auth);
    }
        
    @Before
    public void setup() {
        Kernel.initBootstrap();
        // Setup test authority, and types
        if (Kernel.getDoc().docRepoExists(ctx, auth)) cleanUp();
        
        Kernel.getDoc().createDocRepo(ctx, auth, "VREP {} USING MEMORY {}");
        
        if (Kernel.getScript().doesScriptExist(ctx, auth + "/createDoc"))
            Kernel.getScript().deleteScript(ctx, auth + "/createDoc");
        if (Kernel.getScript().doesScriptExist(ctx, auth + "/runTest"))
            Kernel.getScript().deleteScript(ctx, auth + "/runTest");

        // Create the scripts

        String scriptToInclude = "def createDoc(displayName, content)\nprintln('Will be creating ' + displayName);\ncontent --> displayName;\nend\n";
        Kernel.getScript().createScript(ctx, auth + "/createDoc", RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, scriptToInclude);
        String scriptToUse = "include 'reflexIncludeDataAuthority/createDoc';\n\ncontent = {};\ncontent['test']=true;\n\ncreateDoc('document://reflexIncludeDataAuthority/1', content);\n";
        Kernel.getScript().createScript(ctx, auth + "/runTest", RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, scriptToUse);
    }

    @Test
    public void testIncludeData() {
        Kernel.getScript().runScript(ctx, auth + "/runTest", new HashMap<String, String>());

        // And test the output

        String output = Kernel.getDoc().getDoc(ctx, auth + "/1");
        assertEquals("{\"test\":true}", output.replaceAll("[ \n]*", ""));

    }
}
