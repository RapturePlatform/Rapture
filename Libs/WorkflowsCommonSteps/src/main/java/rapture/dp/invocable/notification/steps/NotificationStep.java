package rapture.dp.invocable.notification.steps;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.common.net.MediaType;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.api.AdminApi;
import rapture.common.api.DecisionApi;
import rapture.common.dp.AbstractInvocable;
import rapture.common.exception.ExceptionToString;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.Kernel;
import rapture.kernel.dp.ExecutionContextUtil;
import rapture.mail.EmailTemplate;
import rapture.mail.Mailer;
import rapture.mail.SMTPConfig;

// Should maybe subclass NotificationStep to provide other notification mechanisms?

// A step to notify the user
// * By email
// * By instant message app - Slack/What'sApp/Pidgin
// * text message?

public class NotificationStep extends AbstractInvocable {
    private static Logger log = Logger.getLogger(NotificationStep.class);
    DecisionApi decision;

    public NotificationStep(String workerUri, String stepName) {
        super(workerUri, stepName);
        decision = Kernel.getDecision();
    }

    @Override
    public String invoke(CallingContext ctx) {
	// Don't set STEPNAME here because we want the name of the preceding step
        // Can read config from a documemnt or pass as args
        AdminApi admin = Kernel.getAdmin();
        String types = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "NOTIFY_TYPE"));

        if (types == null) {
            log.error("Cannot determine NOTIFY_TYPE value");
            return getErrorTransition();
        }
        String retval = getNextTransition();
        for (String type : types.split("[, ]+")) {
            try {
                if (type.equalsIgnoreCase("SLACK") && !sendSlack(ctx)) retval = getErrorTransition();
            } catch (Exception e) {
                log.error("Slack Notification failed: " + e.getMessage());
                log.debug(ExceptionToString.format(e));
                retval = getErrorTransition();
            }
            try {
                if (type.equalsIgnoreCase("EMAIL") && !sendEmail(ctx)) retval = getErrorTransition();
            } catch (Exception e) {
                log.error("Email Notification failed: " + e.getMessage());
                log.debug(ExceptionToString.format(e));
                retval = getErrorTransition();
            }
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
        String workOrder = new RaptureURI(getWorkerURI()).toShortString();
        return ExecutionContextUtil.evalTemplateECF(ctx, workOrder, template, new HashMap<String, String>());
    }

    private boolean sendSlack(CallingContext ctx) {
        String message = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "MESSAGE_BODY"));
        // Legacy: use template if values are not set
        String templateName = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "MESSAGE_TEMPLATE"));
        String webhook = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "SLACK_WEBHOOK"));
        if (webhook == null) {
            log.error("No webhook specified");
            return false;
        }

        try {
            if (message == null) {
                if (templateName == null) {
                    log.error("No message specified");
                    return false;
                }
                EmailTemplate template = Mailer.getEmailTemplate(ctx, templateName);
                message = template.getMsgBody();
            }
            URL url = new URL(webhook);
            Map<String, String> slackNotification = new HashMap<>();
            slackNotification.put("text", renderTemplate(ctx, message));
            int response = doPost(url, JacksonUtil.bytesJsonFromObject(slackNotification));
            if (response == 200) return true;
        } catch (Exception e) {
            log.debug(ExceptionToString.format(e));
        }
        return false;
    }

    // Provided as an alternative to using admin.emailUser
    private boolean sendEmail(CallingContext ctx) {

        final SMTPConfig config = Mailer.getSMTPConfig();
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

        Properties props = System.getProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.host", config.getHost());
        props.put("mail.smtp.port", config.getPort());
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getFrom(), config.getPassword());
            }
        });

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(config.getFrom()));
            String[] allRecipients = renderTemplate(ctx, recipientList).split("[, ]+");

            InternetAddress[] address = new InternetAddress[allRecipients.length];
            for (int i = 0; i < allRecipients.length; i++)
                address[i] = new InternetAddress(allRecipients[i]);

            msg.setRecipients(Message.RecipientType.TO, address);
            msg.setSubject(renderTemplate(ctx, subject));
            msg.setContent(renderTemplate(ctx, message), MediaType.PLAIN_TEXT_UTF_8.toString());
            msg.setSentDate(new Date());
            Transport.send(msg);
        } catch (MessagingException e) {
            log.debug(ExceptionToString.format(e));
            return false;
        }
        return true;
    }
}
