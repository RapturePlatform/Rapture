/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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
package rapture.kernel.user;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class RenameTest {
    // Test the batch exist
    private static final CallingContext ctx = ContextFactory.getKernelUser();
    private static final String authority = "renameTest";

    @Test
    public void runRenameTest() {
        System.out.println("TEST STARTUP");
        String originalContent = Kernel.getDoc().getDoc(ctx, "//" + authority  + "/1");
        Kernel.getDoc().renameDoc(ctx, "//" + authority  + "/1", "//" + authority  + "/alan");
        String content = Kernel.getDoc().getDoc(ctx, "//" + authority  + "/alan");
        assertTrue(content.equals(originalContent));

        String oldContent = Kernel.getDoc().getDoc(ctx, "//" + authority + "/1");
        assertTrue(oldContent == null);
        System.out.println("TEST END");
    }
    
    @After
    public void cleanUp() {
        Kernel.getDoc().deleteDocRepo(ctx, "//" + authority);
    }

    @Before
    public void setup() {
        System.out.println("RENAME STARTUP");
        Kernel.initBootstrap();
        if (Kernel.getDoc().docRepoExists(ctx, "//" + authority)) cleanUp();

        Kernel.getDoc().createDocRepo(ctx, "//" + authority, "REP {} USING MEMORY {}");

        // Now put some data in there
        Kernel.getDoc().putDoc(ctx, "//" + authority + "/1", "{ \"alan\" : 1 }");
        System.out.println("BATCHEXIST END");
    }
}
