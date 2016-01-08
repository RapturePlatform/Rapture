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
package rapture.api.hooks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.hooks.CallName;
import rapture.common.hooks.HookFactory;
import rapture.common.hooks.HookType;
import rapture.common.hooks.HooksConfig;
import rapture.common.hooks.HooksConfigTest;
import rapture.common.hooks.SingleHookConfig;
import rapture.common.model.AuditLogEntry;
import rapture.kernel.AuditApiImpl;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class ApiHooksServiceTest {
    AuditApiImpl audit;

    @Before
    public void doBefore() {
        System.setProperty("LOGSTASH-ISENABLED", "false");
        audit = Kernel.getAudit().getTrusted();
        CallingContext context = ContextFactory.getKernelUser();
        if (audit.doesAuditLogExist(context, RaptureConstants.DEFAULT_AUDIT_URI)) {
            audit.deleteAuditLog(context, RaptureConstants.DEFAULT_AUDIT_URI);

        }
        if (!audit.doesAuditLogExist(context, RaptureConstants.DEFAULT_AUDIT_URI)) {
            audit.createAuditLog(context, new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(), "LOG {} using MEMORY {}");
        }
    }

    @Test
    public void testAudit() {

        List<AuditLogEntry> entries = null;
        List<AuditLogEntry> entries2 = null;

        HooksConfig hooksConfig = HooksConfigTest.createHooksConfig();
        ApiHooksService apiHooksService = new ApiHooksService();
        apiHooksService.configure(hooksConfig);
        CallingContext context = ContextFactory.getKernelUser();
        CallName callName = CallName.Admin_addIPToWhiteList;
        apiHooksService.pre(context, callName);
        entries = audit.getRecentLogEntries(context, RaptureConstants.DEFAULT_AUDIT_URI, 10);
        assertTrue(entries.size() > 0);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        hooksConfig = HooksConfigTest.createHooksConfig();
        apiHooksService = new ApiHooksService();
        apiHooksService.configure(hooksConfig);
        context = ContextFactory.getKernelUser();
        callName = CallName.Admin_addRemote;
        apiHooksService.pre(context, callName);

        entries2 = audit.getRecentLogEntries(context, RaptureConstants.DEFAULT_AUDIT_URI, 10);
        assertTrue(entries2.size() >= entries.size());
    }

    @Test
    public void testAddingAndRemovingRuntimeHooks() {
        TestHook hook = new TestHook();
        HooksConfig hooksConfig = HooksConfigTest.createHooksConfig();
        ApiHooksService apiHooksService = new ApiHooksService();
        apiHooksService.configure(hooksConfig);
        apiHooksService.pre(ContextFactory.getKernelUser(), CallName.Doc_getDoc);
        assertFalse(TestHook.hasRun());
        String id = "testHook";
        ArrayList<HookType> hookTypes = Lists.newArrayList(HookType.PRE);
        ArrayList<String> includes = Lists.newArrayList();
        ArrayList<String> excludes = Lists.newArrayList();
        SingleHookConfig singleHookConfig = HookFactory.INSTANCE.createConfig(hook, id, hookTypes, includes, excludes);
        apiHooksService.addSingleHookConfig(singleHookConfig);
        apiHooksService.pre(ContextFactory.getKernelUser(), CallName.Doc_getDoc);
        assertTrue(TestHook.hasRun());
        TestHook.reset();
        assertFalse(TestHook.hasRun());
        apiHooksService.removeSingleHookConfig(id);
        apiHooksService.pre(ContextFactory.getKernelUser(), CallName.Doc_getDoc);
        assertFalse(TestHook.hasRun());
    }
}
