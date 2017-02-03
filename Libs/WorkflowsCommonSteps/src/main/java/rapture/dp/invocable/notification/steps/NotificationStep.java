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
package rapture.dp.invocable.notification.steps;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.common.net.MediaType;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.api.AdminApi;
import rapture.common.api.DecisionApi;
import rapture.common.dp.Worker;
import rapture.common.dp.WorkerStorage;
import rapture.common.exception.ExceptionToString;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.dp.AbstractStep;
import rapture.dp.InvocableUtils;
import rapture.kernel.Kernel;
import rapture.kernel.dp.ExecutionContextUtil;
import rapture.mail.EmailTemplate;
import rapture.mail.Mailer;

// Should maybe subclass NotificationStep to provide other notification mechanisms?

// A step to notify the user
// * By email
// * By instant message app - Slack/What'sApp/Pidgin
// * text message?

public class NotificationStep extends AbstractStep {
    private static Logger log = Logger.getLogger(NotificationStep.class);
    DecisionApi decision;

    public NotificationStep(String workerUri, String stepName) {
        super(workerUri, stepName);
        decision = Kernel.getDecision();
    }

    private String previousStepName = "UNDEFINED";

    @Override
    public void preInvoke(CallingContext ctx) {
        String psn = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "STEPNAME"));
        if (psn != null) previousStepName = psn;
    }

    @Override
    public String invoke(CallingContext ctx) {
	// Don't set STEPNAME here because we want the name of the preceding step
        // Can read config from a documemnt or pass as args
        AdminApi admin = Kernel.getAdmin();
        String types = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "NOTIFY_TYPE"));

        if (types == null) {
            decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), "Problem in " + previousStepName + ": parameter NOTIFY_TYPE is not set", true);
            return getErrorTransition();
        }
        StringBuffer error = new StringBuffer();
        String retval = getNextTransition();
        for (String type : types.split("[, ]+")) {
            try {
                if (type.equalsIgnoreCase("SLACK")) {
                    if (!sendSlack(ctx)) {
                        decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), "Slack notification failed", true);
                        retval = getErrorTransition();
                    }
                } else if (type.equalsIgnoreCase("EMAIL")) {
                    if (!sendEmail(ctx)) {
                        decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), "Email notification failed", true);
                        retval = getErrorTransition();
                    }
                } else if (type.equalsIgnoreCase("WORKFLOW")) {
                    if (!sendEmail(ctx) && !sendSlack(ctx)) {
                        decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), "Notification failed", true);
                        retval = getErrorTransition();
                    }
                } else {
                    String unsupported = "Unsupported notification type: " + type;
                    error.append(unsupported).append("\n");
                    decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), unsupported, true);
                    retval = getErrorTransition();
                }
            } catch (Exception e) {
                Throwable cause = ExceptionToString.getRootCause(e);
                error.append("Cannot send ").append(type).append(" notification : ").append(cause.getLocalizedMessage()).append("\n");
                decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), "Problem in NotificationStep " + previousStepName + ": notification failed", true);
                decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), ExceptionToString.summary(cause), true);
                log.error(ExceptionToString.format(ExceptionToString.getRootCause(e)));
                retval = getErrorTransition();
            }
        }

        String errMsg = error.toString();
        if (!StringUtils.isEmpty(errMsg)) {
            log.error(errMsg);
            decision.setContextLiteral(ctx, getWorkerURI(), previousStepName, "Notification failure");
            decision.setContextLiteral(ctx, getWorkerURI(), getErrName(), errMsg);
        }
        return retval;
    }

    private int doPost(URL url, byte[] body) throws IOException {
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setFixedLengthStreamingMode(body.length);
        http.setRequestProperty("Content-Type", MediaType.JSON_UTF_8.toString());
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        http.connect();
        try (OutputStream stream = http.getOutputStream()) {
            stream.write(body);
        }
        int response = http.getResponseCode();
        http.disconnect();
        return response;
    }

    public String renderTemplate(CallingContext ctx, String template) {
        RaptureURI workUri = new RaptureURI(getWorkerURI());
        String workOrder = workUri.toShortString();
        Worker worker = WorkerStorage.readByFields(workOrder, workUri.getElement());
        return ExecutionContextUtil.evalTemplateECF(ctx, workOrder, template, InvocableUtils.getLocalViewOverlay(worker));
    }

    private boolean sendSlack(CallingContext ctx) throws IOException {
        String message = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "MESSAGE_BODY"));
        // Legacy: use template if values are not set
        String templateName = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "MESSAGE_TEMPLATE"));
        String webhook = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "SLACK_WEBHOOK"));
        if (webhook == null) {
            decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), "Problem in " + previousStepName + ": No webhook specified", true);
            return false;
        }

        if (message == null) {
            if (templateName == null) {
                decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), "Problem in " + previousStepName + ": No message specified", true);
                return false;
            }
            EmailTemplate template = Mailer.getEmailTemplate(ctx, templateName);
            message = template.getMsgBody();
        }
        URL url = new URL(webhook);
        Map<String, String> slackNotification = new HashMap<>();
        slackNotification.put("text", renderTemplate(ctx, message));
        int response = doPost(url, JacksonUtil.bytesJsonFromObject(slackNotification));
        if (response == 200) {
            decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), previousStepName + ": slack notification sent successfully", false);
            return true;
        } else {
            decision.writeWorkflowAuditEntry(ctx, getWorkerURI(),
                    "Problem in " + previousStepName + ": slack notification failed with HTTP error code " + response, true);
            return false;
        }
    }

    private boolean sendEmail(CallingContext ctx) throws AddressException, MessagingException {
        String message = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "MESSAGE_BODY"));
        String subject = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "MESSAGE_SUBJECT"));
        String recipientList = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "EMAIL_RECIPIENTS"));
        // Legacy: use template if values are not set
        String templateName = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "MESSAGE_TEMPLATE"));

        if (templateName != null) {
            EmailTemplate template = Mailer.getEmailTemplate(ctx, templateName);
            if (template != null) {
                if (message == null) message = template.getMsgBody();
                if (subject == null) subject = template.getSubject();
                if (recipientList == null) recipientList = template.getEmailTo();
            }
        }

        if (message == null) {
            decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), "Problem in " + previousStepName + ": No message specified", true);
            return false;
        }

        if (recipientList == null) {
            decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), "Problem in " + previousStepName + ": No recipient specified", true);
            return false;
        }

        try {
            Mailer.email(renderTemplate(ctx, recipientList).split("[, ]+"), renderTemplate(ctx, subject), renderTemplate(ctx, message));
            decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), previousStepName + ": email notification sent successfully", false);
            return true;
        } catch (MessagingException e) {
            log.warn("Unable to send email", e);
            return false;
        }
    }
}
