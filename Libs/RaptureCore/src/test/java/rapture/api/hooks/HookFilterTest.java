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
package rapture.api.hooks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.api.hooks.impl.AbstractApiHook;
import rapture.api.hooks.impl.AuditApiHook;
import rapture.common.CallingContext;
import rapture.common.hooks.CallName;
import rapture.common.hooks.HookType;
import rapture.common.hooks.HooksConfigTest;
import rapture.common.hooks.SingleHookConfig;
import rapture.common.storable.helpers.HooksConfigHelper;
import rapture.kernel.ContextFactory;

public class HookFilterTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    private List<AbstractApiHook> hooks;
    private HookFilter hookFilter;
    private Collection<SingleHookConfig> allHooksConfig;
    private CallingContext context;

    @Before
    public void setUp() throws Exception {
        hookFilter = new HookFilter();
        allHooksConfig = HooksConfigHelper.getHooks(HooksConfigTest.createHooksConfig());
        context = ContextFactory.getKernelUser();
    }

    @Test
    public void testPrePost() {
        hooks = hookFilter.filter(allHooksConfig, HookType.POST, context, CallName.Series_addDoublesToSeries);
        assertEquals(1, hooks.size());
        assertTrue(hooks.get(0) instanceof AuditApiHook);

        hooks = hookFilter.filter(allHooksConfig, HookType.PRE, context, CallName.Series_addDoublesToSeries);
        assertEquals(2, hooks.size());
    }

    @Test
    public void testAdmin() {
        hooks = hookFilter.filter(allHooksConfig, HookType.PRE, context, CallName.Admin_addIPToWhiteList);
        assertEquals(1, hooks.size());
        assertTrue(hooks.get(0) instanceof AuditApiHook);

        hooks = hookFilter.filter(allHooksConfig, HookType.PRE, context, CallName.Admin_addTemplate);
        assertEquals(1, hooks.size());
        assertTrue(hooks.get(0) instanceof AuditApiHook);
    }

    @Test
    public void testUser() {
        hooks = hookFilter.filter(allHooksConfig, HookType.PRE, context, CallName.User_getWhoAmI);
        assertEquals(1, hooks.size());
        assertTrue(hooks.get(0) instanceof AuditApiHook);

        hooks = hookFilter.filter(allHooksConfig, HookType.PRE, context, CallName.Doc_setDocAttribute);
        assertEquals(2, hooks.size());
    }
    
}
