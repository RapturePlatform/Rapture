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
package rapture.common.hooks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.storable.helpers.HooksConfigHelper;

public class HooksConfigRepoTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        // make sure no extra hooks stored before each test starts
        HooksConfig originalConfig = new HooksConfig();
        originalConfig.setIdToHook(new HashMap<String, SingleHookConfig>());
        HooksConfigRepo.INSTANCE.storeHooksConfig(originalConfig, "fakeuser", "comment");
    }

    @Test
    public void testStoreAndLoad() {
        String user = "fakeUser";
        String comment = "this is a comment";
        HooksConfig originalConfig = HooksConfigTest.createHooksConfig();
        HooksConfigRepo.INSTANCE.storeHooksConfig(originalConfig, user, comment);

        HooksConfig hooksConfigNew = HooksConfigRepo.INSTANCE.loadHooksConfig();
        SingleHookConfig hookConfig1 = HooksConfigHelper.getHooks(originalConfig).iterator().next();
        SingleHookConfig hookConfig1New = HooksConfigHelper.getHooks(hooksConfigNew).iterator().next();
        assertEquals(2, HooksConfigHelper.getHooks(hooksConfigNew).size());
        assertEquals(hookConfig1.getIncludes().size(), hookConfig1New.getIncludes().size());
        assertEquals(hookConfig1.getExcludes().size(), hookConfig1New.getExcludes().size());
        assertEquals(hookConfig1.getIncludes().get(0), hookConfig1New.getIncludes().get(0));

    }

    @Test
    public void testInitialLoad() {
        HooksConfig fileConfig = HooksConfigRepo.INSTANCE.readFromFile();
        HooksConfig totalConfig = HooksConfigRepo.INSTANCE.loadHooksConfig();

        assertTrue(HooksConfigHelper.getHooks(fileConfig).size() > 0);
        assertEquals(HooksConfigHelper.getHooks(fileConfig).size(), HooksConfigHelper.getHooks(totalConfig).size());
    }

}
