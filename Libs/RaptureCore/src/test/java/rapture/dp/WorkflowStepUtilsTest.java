package rapture.dp;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;

public class WorkflowStepUtilsTest {

    @SuppressWarnings({ "unchecked" })
    @Test
    public void testGetLogger() {
        final String logMsg = "this is a log msg";
        WorkflowStepUtils b1 = new DummyBrokerStepUtils("w1", "s1");
        Logger log1 = b1.getLogger();
        Enumeration<Appender> e1 = log1.getAllAppenders();
        List<Appender> appenders = new ArrayList<>();
        while (e1.hasMoreElements()) {
            appenders.add(e1.nextElement());
        }
        assertEquals(1, appenders.size());
        assertEquals("w1/s1", log1.getName());

        DummyBrokerStepUtils b2 = new DummyBrokerStepUtils("w2", "s2");
        Logger log2 = b2.getLogger();
        Enumeration<Appender> e2 = log2.getAllAppenders();
        appenders = new ArrayList<>();
        while (e2.hasMoreElements()) {
            appenders.add(e2.nextElement());
        }
        assertEquals(1, appenders.size());
        assertEquals("w2/s2", log2.getName());
        log2.info(logMsg);
        assertEquals(logMsg, b2.getMessage());
        assertEquals("w2.s2", ((AuditAppender) appenders.get(0)).getAuditLogUri());
    }

    private class DummyAuditAppender extends AuditAppender {

        public DummyAuditAppender(String auditLogUri) {
            super(auditLogUri);
        }

        private String msg;

        @Override
        protected void append(LoggingEvent event) {
            this.msg = (String) event.getMessage();
        }

        public String getMessage() {
            return msg;
        }
    }

    private class DummyBrokerStepUtils extends WorkflowStepUtils {

        private DummyAuditAppender appender = new DummyAuditAppender(getAuditLogUri());

        public DummyBrokerStepUtils(String workerUri, String stepName) {
            super(workerUri, stepName);
        }

        @Override
        String getAuditLogUri() {
            return workerUri + "." + stepName;
        }

        @Override
        Appender newAppender() {
            return appender;
        }

        public String getMessage() {
            return appender.getMessage();
        }
    }

}
