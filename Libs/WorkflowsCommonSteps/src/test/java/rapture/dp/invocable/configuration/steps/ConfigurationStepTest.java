package rapture.dp.invocable.configuration.steps;

import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.kernel.AuditApiImpl;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class ConfigurationStepTest {

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
        ConfigurationStep conf = new ConfigurationStep(workerUri, "foo");
        Kernel.getDecision().setContextLiteral(ctx, workerUri, "CONFIGURATION", "{\"document://matrix/Emailconfig\":\"document://matrix/CMIconfig\"}");
        conf.invoke(ctx);
    }

}
