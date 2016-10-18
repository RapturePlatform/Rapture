package rapture.dp.invocable.ftp.steps;

import static org.junit.Assert.assertEquals;

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
        assertEquals("/tmp/19780606 was not found but was expected ", Kernel.getDecision().getContextValue(ctx, workerUri, "fooError"));
    }

    @Test
    public void checkWildcardTest() {
        CallingContext ctx = ContextFactory.getAnonymousUser();
        String workerUri = "workorder://x/y#0";
        String workOrderUri = workerUri.substring(0, workerUri.length() - 2);
        CheckFileExistsStep cfes = new CheckFileExistsStep(workerUri, "foo");
        String filename = "/etc/aliase.";
        String expand = ExecutionContextUtil.evalTemplateECF(ctx, workerUri, filename, null);
        Kernel.getDecision().setContextLiteral(ctx, workerUri, "EXIST_FILENAMES", JacksonUtil.jsonFromObject(ImmutableMap.of("/bin/c.", Boolean.TRUE,
                "file://bin/m[a-z]{4}", Boolean.TRUE, "file://.{3}/zsh", Boolean.TRUE, "file://x.*/y.*/z.*", Boolean.FALSE, "/b{7}/w.*/f?/c", Boolean.FALSE)));
        assertEquals(cfes.getNextTransition(), cfes.invoke(ctx));
    }
}
