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
package rapture.kernel.user;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class BatchExistTest {
    // Test the batch exist
    private static final CallingContext ctx = ContextFactory.getKernelUser();
    private static final String authority = "batchExist";

    @Test
    public void runBatchExistTest() {
        System.out.println("TEST STARTUP");
        List<String> displayNames = new ArrayList<String>();
        displayNames.add("//" + authority  + "/1");
        displayNames.add("//" + authority  + "/1");
        displayNames.add("//" + authority  + "/1");
        displayNames.add("//" + authority  + "/1");

        List<Boolean> ret = Kernel.getDoc().docsExist(ctx, displayNames);
        for (Boolean b : ret) {
            System.out.println("Res = " + b);
            assertTrue(b);
        }
        System.out.println("TEST END");
    }

    @After
    public void cleanUp() {
        Kernel.getDoc().deleteDocRepo(ctx, "//" + authority);
    }
    
    @Before
    public void setup() {
        System.out.println("BATCHEXIST STARTUP");
        Kernel.initBootstrap();
        if (Kernel.getDoc().docRepoExists(ctx, "//" + authority)) cleanUp();
        Kernel.getDoc().createDocRepo(ctx, "//" + authority, "NREP {} USING MEMORY {}");

        // Now put some data in there
        Kernel.getDoc().putDoc(ctx, "//" + authority + "/1", "{ \"alan\" : 1 }");
        System.out.println("BATCHEXIST END");
    }
}
