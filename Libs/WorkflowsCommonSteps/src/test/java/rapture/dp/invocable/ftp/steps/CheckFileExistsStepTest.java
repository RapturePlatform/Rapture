package rapture.dp.invocable.ftp.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        assertEquals("4 files or directories matching /bin/c.* were found but were not expected",
                Kernel.getDecision().getContextValue(ctx, workerUri, "foo4Error"));
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
                JacksonUtil.jsonFromObject(ImmutableMap.of("file://etc/alia.*", Boolean.TRUE)));
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
                JacksonUtil.jsonFromObject(ImmutableMap.of("file://etc/alia.*", Boolean.FALSE)));
        String trans = cfes.invoke(ctx);
        assertEquals("2 files or directories matching file://etc/alia.* were found but were not expected",
                Kernel.getDecision().getContextValue(ctx, workerUri, "foo9Error"));
        assertEquals(cfes.getFailTransition(), trans);
    }

    @Test
    public void checkWildcardTest10() {
        CallingContext ctx = ContextFactory.getAnonymousUser();
        String workerUri = "workorder://x/y#0";
        CheckFileExistsStep cfes = new CheckFileExistsStep(workerUri, "foo10");
        Kernel.getDecision().setContextLiteral(ctx, workerUri, "EXIST_FILENAMES",
                JacksonUtil.jsonFromObject(
                        ImmutableMap.of("/foo", Boolean.TRUE, "file://bar", Boolean.TRUE, "/etc/.osts", Boolean.FALSE, "/etc/a.*", Boolean.FALSE)));
        String trans = cfes.invoke(ctx);
        assertEquals(
                "/foo was not found but was expected\nfile://bar was not found but was expected\n/etc/hosts was found but was not expected\n11 files or directories matching /etc/a.* were found but were not expected",
                Kernel.getDecision().getContextValue(ctx, workerUri, "foo10Error"));
        assertEquals(cfes.getFailTransition(), trans);
    }
}
