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
package rapture.dp.invocable.ftp.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.AuditApiImpl;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.dp.ExecutionContextUtil;

public class CheckFileExistsStepTest {

    AuditApiImpl audit;
    File tempDir = new File("/tmp/etc/foo/");

    @Before
    public void doBefore() {
        System.setProperty("LOGSTASH-ISENABLED", "false");

        try {
            tempDir.mkdirs();
            tempDir.deleteOnExit();
            File.createTempFile("alias", "foo", tempDir);
            File.createTempFile("alias", "bar", tempDir);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        audit = Kernel.getAudit().getTrusted();
        CallingContext context = ContextFactory.getKernelUser();
        if (audit.doesAuditLogExist(context, RaptureConstants.DEFAULT_AUDIT_URI)) {
            audit.deleteAuditLog(context, RaptureConstants.DEFAULT_AUDIT_URI);

        }
        if (!audit.doesAuditLogExist(context, RaptureConstants.DEFAULT_AUDIT_URI)) {
            audit.createAuditLog(context, new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(), "LOG {} using MEMORY {}");
        }

        if (!audit.doesAuditLogExist(context, "workflow")) {
            audit.createAuditLog(context, "workflow", "LOG {} using MEMORY {}");
        }

    }

    @SuppressWarnings({ "unused", "static-access" })
    @Test
    public void checkMessageTest() {
        CallingContext ctx = ContextFactory.getAnonymousUser();
        String workerUri = "workorder://x/y#0";
        String workOrderUri = workerUri.substring(0, workerUri.length() - 2);
        CheckFileExistsStep cfes = new CheckFileExistsStep(workerUri, "foo");
        Kernel.getDecision().setContextLiteral(ctx, workerUri, "TODAY", "19780606");
        String today = Kernel.getDecision().getContextValue(ctx, workerUri, "TODAY");
        String val = ExecutionContextUtil.getValueECF(ctx, workerUri, "TODAY", null);
        assertEquals(today, val);
        String filename = "/tmp/${TODAY}";
        String expand = ExecutionContextUtil.evalTemplateECF(ctx, workerUri, filename, null);
        Kernel.getDecision().setContextLiteral(ctx, workerUri, "EXIST_FILENAMES", JacksonUtil.jsonFromObject(ImmutableMap.of(filename, true)));
        assertEquals(cfes.getFailTransition(), cfes.invoke(ctx));
        assertEquals("/tmp/19780606 was not found but was expected", Kernel.getDecision().getContextValue(ctx, workerUri, "fooError"));
    }

    @Test
    public void checkWildcardTest1() {
        CallingContext ctx = ContextFactory.getAnonymousUser();
        String workerUri = "workorder://x/y#0";
        CheckFileExistsStep cfes = new CheckFileExistsStep(workerUri, "foo1");
        Kernel.getDecision().setContextLiteral(ctx, workerUri, "EXIST_FILENAMES", JacksonUtil.jsonFromObject(ImmutableMap.of("/bin/c.", Boolean.TRUE)));
        assertEquals(cfes.getNextTransition(), cfes.invoke(ctx));
        assertEquals("", Kernel.getDecision().getContextValue(ctx, workerUri, "foo1Error"));
    }

    @Test
    public void checkWildcardTest2() {
        CallingContext ctx = ContextFactory.getAnonymousUser();
        String workerUri = "workorder://x/y#0";
        CheckFileExistsStep cfes = new CheckFileExistsStep(workerUri, "foo2");
        Kernel.getDecision().setContextLiteral(ctx, workerUri, "EXIST_FILENAMES", JacksonUtil.jsonFromObject(ImmutableMap.of("/bin/c.", Boolean.FALSE)));
        assertEquals(cfes.getFailTransition(), cfes.invoke(ctx));
        assertEquals("/bin/cp was found but was not expected", Kernel.getDecision().getContextValue(ctx, workerUri, "foo2Error"));
    }

    @Test
    public void checkWildcardTest3() {
        CallingContext ctx = ContextFactory.getAnonymousUser();
        String workerUri = "workorder://x/y#0";
        CheckFileExistsStep cfes = new CheckFileExistsStep(workerUri, "foo3");
        Kernel.getDecision().setContextLiteral(ctx, workerUri, "EXIST_FILENAMES", JacksonUtil.jsonFromObject(ImmutableMap.of("/bin/c.*", Boolean.TRUE)));
        assertEquals(cfes.getNextTransition(), cfes.invoke(ctx));
        assertEquals("", Kernel.getDecision().getContextValue(ctx, workerUri, "foo3Error"));
    }

    @Test
    public void checkWildcardTest4() {
        CallingContext ctx = ContextFactory.getAnonymousUser();
        String workerUri = "workorder://x/y#0";
        String workOrderUri = workerUri.substring(0, workerUri.length() - 2);
        CheckFileExistsStep cfes = new CheckFileExistsStep(workerUri, "foo4");
        Kernel.getDecision().setContextLiteral(ctx, workerUri, "EXIST_FILENAMES", JacksonUtil.jsonFromObject(ImmutableMap.of("/bin/c.*", Boolean.FALSE)));
        assertEquals(cfes.getFailTransition(), cfes.invoke(ctx));
        String foo4Error = Kernel.getDecision().getContextValue(ctx, workerUri, "foo4Error");
        foo4Error = foo4Error.substring(foo4Error.indexOf(' '));
        assertEquals(" files or directories matching /bin/c.* were found but were not expected", foo4Error);
    }

    @Test
    public void checkWildcardTest5() {
        CallingContext ctx = ContextFactory.getAnonymousUser();
        String workerUri = "workorder://x/y#0";
        CheckFileExistsStep cfes = new CheckFileExistsStep(workerUri, "foo5");
        Kernel.getDecision().setContextLiteral(ctx, workerUri, "EXIST_FILENAMES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("/usr/sh.re/.{8}/I[a-z]+/Cocos", Boolean.TRUE)));
        String trans = cfes.invoke(ctx);
        assertEquals("", Kernel.getDecision().getContextValue(ctx, workerUri, "foo5Error"));
        assertEquals(cfes.getNextTransition(), trans);
    }

    @Test
    public void checkWildcardTest6() {
        CallingContext ctx = ContextFactory.getAnonymousUser();
        String workerUri = "workorder://x/y#0";
        CheckFileExistsStep cfes = new CheckFileExistsStep(workerUri, "foo6");
        Kernel.getDecision().setContextLiteral(ctx, workerUri, "EXIST_FILENAMES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("/usr/sh.re/.{8}/I[a-z]+/Cocos", Boolean.FALSE)));
        String trans = cfes.invoke(ctx);
        assertEquals("/usr/share/zoneinfo/Indian/Cocos was found but was not expected", Kernel.getDecision().getContextValue(ctx, workerUri, "foo6Error"));
        assertEquals(cfes.getFailTransition(), trans);
    }

    @Test
    public void checkWildcardTest7() {
        CallingContext ctx = ContextFactory.getAnonymousUser();
        String workerUri = "workorder://x/y#0";
        CheckFileExistsStep cfes = new CheckFileExistsStep(workerUri, "foo7");
        long time = System.currentTimeMillis();
        Kernel.getDecision().setContextLiteral(ctx, workerUri, "EXIST_FILENAMES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("file://DoesNotExist", Boolean.FALSE)));
        String trans = cfes.invoke(ctx);
        long time2 = System.currentTimeMillis();
        Kernel.getDecision().setContextLiteral(ctx, workerUri, "EXIST_FILENAMES", JacksonUtil.jsonFromObject(ImmutableMap.of("file://usr", Boolean.TRUE)));
        String trans2 = cfes.invoke(ctx);
        long time3 = System.currentTimeMillis();
        System.out.println("Exists case took " + (time3 - time2) + "ms");
        System.out.println("Non-Exists case took " + (time2 - time) + "ms");
        assertTrue((time2 - time) < 1000); // Should take much less than 1 second
        assertEquals("", Kernel.getDecision().getContextValue(ctx, workerUri, "foo7Error"));
        assertEquals(cfes.getNextTransition(), trans);
    }

    @Test
    public void checkWildcardTest8() {
        CallingContext ctx = ContextFactory.getAnonymousUser();
        String workerUri = "workorder://x/y#0";
        CheckFileExistsStep cfes = new CheckFileExistsStep(workerUri, "foo8");
        Kernel.getDecision().setContextLiteral(ctx, workerUri, "EXIST_FILENAMES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("file://tmp/etc/foo/alia.*", Boolean.TRUE)));
        String trans = cfes.invoke(ctx);
        assertEquals("", Kernel.getDecision().getContextValue(ctx, workerUri, "foo8Error"));
        assertEquals(cfes.getNextTransition(), trans);
    }

    @Test
    public void checkWildcardTest9() {
        CallingContext ctx = ContextFactory.getAnonymousUser();
        String workerUri = "workorder://x/y#0";
        CheckFileExistsStep cfes = new CheckFileExistsStep(workerUri, "foo9");
        Kernel.getDecision().setContextLiteral(ctx, workerUri, "EXIST_FILENAMES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("file://tmp/etc/foo/alia.*", Boolean.FALSE)));
        String trans = cfes.invoke(ctx);
        String foo9Error = Kernel.getDecision().getContextValue(ctx, workerUri, "foo9Error");
        foo9Error = foo9Error.substring(foo9Error.indexOf(' '));

        assertEquals(" files or directories matching file://tmp/etc/foo/alia.* were found but were not expected", foo9Error);
        assertEquals(cfes.getFailTransition(), trans);
    }

    @Test
    public void checkWildcardTest10() {
        CallingContext ctx = ContextFactory.getAnonymousUser();
        String workerUri = "workorder://x/y#0";
        CheckFileExistsStep cfes = new CheckFileExistsStep(workerUri, "foo10");
        Kernel.getDecision().setContextLiteral(ctx, workerUri, "EXIST_FILENAMES",
                JacksonUtil.jsonFromObject(
                        ImmutableMap.of("/tmp/etc/foo/a.*", Boolean.FALSE, "/foo", Boolean.TRUE, "file://bar", Boolean.TRUE, "/etc/.osts", Boolean.FALSE)));
        String trans = cfes.invoke(ctx);
        String foo10Error = Kernel.getDecision().getContextValue(ctx, workerUri, "foo10Error");
        foo10Error = foo10Error.substring(foo10Error.indexOf(' '));
        assertEquals(
                " files or directories matching /tmp/etc/foo/a.* were found but were not expected\n" + "/foo was not found but was expected\n"
                        + "file://bar was not found but was expected\n" + "/etc/hosts was found but was not expected",
                foo10Error);
        assertEquals(cfes.getFailTransition(), trans);
    }
}
