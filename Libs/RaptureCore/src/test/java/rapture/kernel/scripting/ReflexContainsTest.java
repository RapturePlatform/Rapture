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
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class ReflexContainsTest {

    private static CallingContext ctx = ContextFactory.getKernelUser();

    private String uri = "//containsTest";
    private String scr = "contains";

    @After
    public void cleanUp() {
        Kernel.getDoc().deleteDocRepo(ctx, uri);
    }
    
    @Before
    public void setup() {
        Kernel.initBootstrap();
        if (!Kernel.getDoc().docRepoExists(ctx, uri)) {
            Kernel.getDoc().createDocRepo(ctx, uri, "NREP {} USING MEMORY {}");
        }
        
        if (Kernel.getScript().doesScriptExist(ctx, uri + "/" + scr))
            Kernel.getScript().deleteScript(ctx, uri + "/" + scr);
        
        Kernel.getScript().createScript(
                ctx,
                uri + "/" + scr,
                RaptureScriptLanguage.REFLEX,
                RaptureScriptPurpose.PROGRAM,
                "foo = ['1', '2', '3'];\n" + "return foo contains '1';\n");
    }

    @Test 
    public void testContains() {
        String retval = Kernel.getScript().runScript(ctx, uri + "/" + scr, new HashMap<String, String>());
        assertEquals("true", retval);
    }
}
