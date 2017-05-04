package rapture.dp.pubsub;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.QueueSubscriber;
import rapture.common.model.AuditLogEntry;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.dp.AuditAppender;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class AuditAppenderTest {

    private CallingContext ctx = ContextFactory.getKernelUser();

    private Logger log;
    private static final String AUTH = "log://workflow";
    private static final String LOG_URI = AUTH + "/workflowauth/20160411/docpath/step1";
    private static QueueSubscriber subscriber = null;

    @Before
    public void setup() {
        RaptureConfig config = ConfigLoader.getConf();
        config.DefaultExchange = "PIPELINE {} USING GCP_PUBSUB { threads=\"5\", projectid=\"todo3-incap\"}";

        Kernel.INSTANCE.restart();
        // subscriber = Kernel.INSTANCE.createAndSubscribe(CATEGORY, config.DefaultExchange);

        Kernel.getAudit().createAuditLog(ctx, AUTH, "LOG {} USING MEMORY {}");
        log = Logger.getLogger(AuditAppenderTest.class);
        AuditAppender appender = new AuditAppender(LOG_URI);
        log.removeAppender(appender);
        log.addAppender(appender);
    }

    @After
    public void after() {
        if (subscriber != null) Kernel.getPipeline2().unsubscribeQueue(ctx, subscriber);
    }

    @Test
    public void testAppender() {
        final String testMsg = "this is a test log msg, we will assert it shows up in the audit repo";
        log.info(testMsg);
        List<AuditLogEntry> ret = Kernel.getAudit().getRecentLogEntries(ctx, LOG_URI, 1);
        assertEquals(1, ret.size());
        assertEquals(testMsg, ret.get(0).getMessage());
    }

    @Test
    public void testAppenderWithThrowable() {
        final String errMsg = "error message with exception";
        Exception e = new RuntimeException("test exception");
        log.error(errMsg, e);
        List<AuditLogEntry> ret = Kernel.getAudit().getRecentLogEntries(ctx, LOG_URI, 2);
        assertEquals(2, ret.size());
        assertEquals(ExceptionUtils.getStackTrace(e), ret.get(0).getMessage());
        assertEquals(errMsg, ret.get(1).getMessage());
    }
}
