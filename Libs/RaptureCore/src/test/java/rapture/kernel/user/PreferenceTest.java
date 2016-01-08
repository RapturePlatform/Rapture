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

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class PreferenceTest {
    // Test the preferences
    private static final CallingContext ctx = ContextFactory.getKernelUser();

    @Test
    public void runPreferenceTest() {
        Kernel.getUser().storePreference(ctx, "Test", "Pref1", "Hello");
        // The default memkey store doesn't handle children so this won't return
        // the above
        // Fix that first, then revist this test.
        List<String> categories = Kernel.getUser().getPreferenceCategories(ctx);
        for (String cat : categories) {
            System.out.println("Found category - " + cat);
            List<String> docs = Kernel.getUser().getPreferencesInCategory(ctx, cat);
            for (String d : docs) {
                System.out.println("Found doc - " + d);
                String val = Kernel.getUser().getPreference(ctx, cat, d);
                System.out.println("Value is " + val);
            }
        }
    }

    @Before
    public void setup() {
        Kernel.initBootstrap();
    }
}
