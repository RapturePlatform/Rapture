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

import static org.junit.Assert.fail;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class ReflexMapTest {

    private static CallingContext ctx = ContextFactory.getKernelUser();

    private String uri = "//reflexMapTest";
    private String scr = "happyPath";
    private String scr2 = "illegalAssign";
    private String scr3 = "illegalAccess";

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
        if (Kernel.getScript().doesScriptExist(ctx, uri + "/" + scr2))
            Kernel.getScript().deleteScript(ctx, uri + "/" + scr2);
        if (Kernel.getScript().doesScriptExist(ctx, uri + "/" + scr3))
            Kernel.getScript().deleteScript(ctx, uri + "/" + scr3);
        
        // Valid. map.keyNotPresent or map[keyNotPresent] should return null.
        // NB: It is (currently) legal to mix dot and bracket syntax provided the bracket is the last element.
        // So a.b['c'] is legal but a['b'].c is not
        
        Kernel.getScript().createScript(
                ctx,
                uri + "/" + scr,
                RaptureScriptLanguage.REFLEX,
                RaptureScriptPurpose.PROGRAM,
                "a = {};\n" +
                "a['b'] = {};\n" + 
                "a.b.c = {};\n" + 
                "a.b.c.d = \"OK\";\n" + 
                "assert(a.b.c.d == \"OK\");\n" + 
                "assert(a.b.d == null);\n" + 
                "assert(a.b['d'] == null);\n" + 
                "assert(a.b.d == null);\n" + 
                "assert(a.d == null);\n");

        // Not valid because a.b is not initialised to a map
        Kernel.getScript().createScript(
                ctx,
                uri + "/" + scr2,
                RaptureScriptLanguage.REFLEX,
                RaptureScriptPurpose.PROGRAM,
                "a = {};\n" +
                "a.b.c = {};\n");
        
        // Not valid because x.y is not initialised to a map
        Kernel.getScript().createScript(
                ctx,
                uri + "/" + scr3,
                RaptureScriptLanguage.REFLEX,
                RaptureScriptPurpose.PROGRAM,
                "d = x.y.z;\n");

    }

    @Test 
    public void testValidAssign() {
        Kernel.getScript().runScript(ctx, uri + "/" + scr, new HashMap<String, String>());
    }
    
    @Test 
    public void testInvalidAssign() {
        try {
            Kernel.getScript().runScript(ctx, uri + "/" + scr2, new HashMap<String, String>());
            fail("Should not work");
        } catch (Exception e) {
            // Expected;
        }
    }
    
    @Test 
    public void testInvalidAccess() {
        try {
            Kernel.getScript().runScript(ctx, uri + "/" + scr3, new HashMap<String, String>());
            fail("Should not work");
        } catch (Exception e) {
            // Expected;
        }
    }
}
