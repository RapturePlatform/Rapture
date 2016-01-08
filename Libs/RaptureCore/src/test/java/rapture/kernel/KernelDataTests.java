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
package rapture.kernel;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;

public class KernelDataTests {
    private static CallingContext ctx = ContextFactory.getKernelUser();

    private void dump(List<String> cusips) {
        for (String cusip : cusips) {
            System.out.println(cusip);
        }
    }

    @After
    public void finish() {
        // Kernel.getAdmin().dropType(ctx, "test", "bond");
        // Kernel.getAdmin().dropAuthority(ctx, "test", true);
    }

    private String getBString(List<Boolean> vals) {
        StringBuilder ret = new StringBuilder();
        for (Boolean b : vals) {
            if (b) {
                ret.append("T");
            } else {
                ret.append("F");
            }
        }
        return ret.toString();
    }

    @Before
    public void startup() {
        Kernel.initBootstrap(null, null, false);
        if (!Kernel.getDoc().docRepoExists(ctx, "//test.bond/")) {
            Kernel.getDoc().createDocRepo(ctx, "//test.bond/", "NREP {} USING MEMORY {}");
        }
    }

    @Test
    public void testExistence() {
        String doc = "{ \"test\" : 1}";
        Kernel.getDoc().putDoc(ctx, "//test.bond/cusip1/date1", doc);
        Kernel.getDoc().putDoc(ctx, "//test.bond/cusip2/date2", doc);
        Kernel.getDoc().putDoc(ctx, "//test.bond/cusip3/date1", doc);
        Kernel.getDoc().putDoc(ctx, "//test.bond/cusip3/date1", doc);
        Kernel.getDoc().putDoc(ctx, "//test.bond/cusip4/date1", doc);
        Kernel.getDoc().putDoc(ctx, "//test.bond/cusip5/date1", doc);
        Kernel.getDoc().putDoc(ctx, "//test.bond/cusip6/date1", doc);
        Kernel.getDoc().putDoc(ctx, "//test.bond/cusip7/date1", doc);
        Kernel.getDoc().putDoc(ctx, "//test.bond/cusip8/date1", doc);
        Kernel.getDoc().putDoc(ctx, "//test.bond/cusip8/date2", doc);
        Kernel.getDoc().putDoc(ctx, "//test.bond/cusip9/date1", doc);

        List<String> displayNames = new ArrayList<String>();
        displayNames.add("//test.bond/c");
        displayNames.add("//test.bond/cusip8/date2");
        List<Boolean> ret = Kernel.getDoc().docsExist(ctx, displayNames);
        String bString = getBString(ret);
        assertEquals("FT", bString);

        List<String> disp2 = new ArrayList<String>();
        disp2.add("//test.bond/cusip9/date1");
        disp2.add("//test.bond/cusip7/date1");
        Map<String, String> res = Kernel.getDoc().getDocs(ctx, disp2);
        for (String r : res.values()) {
            System.out.println(r);
        }
    }

}
