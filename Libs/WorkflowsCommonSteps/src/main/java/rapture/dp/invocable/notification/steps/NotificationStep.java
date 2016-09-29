package rapture.dp.invocable.notification.steps;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.api.AdminApi;
import rapture.common.api.DecisionApi;
import rapture.common.dp.AbstractInvocable;
import rapture.common.dp.ExecutionContextField;
import rapture.common.dp.ExecutionContextFieldStorage;
import rapture.common.dp.Steps;
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

        // Can read config from a documemnt or pass as args
        AdminApi admin = Kernel.getAdmin();
        String types = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "NOTIFY_TYPE"));

        if (types == null) {
            log.error("Cannot determine NOTIFY_TYPE value");

            List<ExecutionContextField> foo1 = ExecutionContextFieldStorage.readAll();

            decision.setContextLiteral(ctx, getWorkerURI(), "FOO", "BAR");
            String bar = decision.getContextValue(ctx, getWorkerURI(), "FOO");
            String s = getWorkerURI().replace("workorder:/", "document://sys.RaptureEphemeral/dp/execontextfield");
            Map<String, RaptureFolderInfo> foo = Kernel.getDoc().listDocsByUriPrefix(ctx, s, 2);
            return Steps.ERROR.toString();
        }
        String retval = Steps.NEXT.toString();
        for (String type : types.split("[, ]+")) {
            if (type.equalsIgnoreCase("SLACK") && !sendSlack(ctx)) retval = Steps.ERROR.toString();
            if (type.equalsIgnoreCase("EMAIL") && !sendEmail(ctx)) retval = Steps.ERROR.toString();
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
        String templateName = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "MESSAGE_TEMPLATE"));
        String webhook = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "SLACK_WEBHOOK"));
        if ((webhook == null) || (templateName == null)) return false;

        try {
            EmailTemplate template = Mailer.getEmailTemplate(ctx, templateName);

            URL url = new URL(webhook);
            Map<String, String> slackNotification = new HashMap<>();
            slackNotification.put("text", renderTemplate(ctx, template.getMsgBody()));
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

        String templateName = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "MESSAGE_TEMPLATE"));
        if (templateName == null) return false;
        EmailTemplate template = Mailer.getEmailTemplate(ctx, templateName);
        
        String recipientList = renderTemplate(ctx, template.getEmailTo());

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
            String[] recipients = recipientList.split("[, ]+");

            InternetAddress[] address = new InternetAddress[recipients.length];
            for (int i = 0; i < recipients.length; i++)
                address[i] = new InternetAddress(recipients[i]);

            msg.setRecipients(Message.RecipientType.TO, address);
            msg.setSubject(renderTemplate(ctx, template.getSubject()));
            msg.setContent(renderTemplate(ctx, template.getMsgBody()), MediaType.ANY_TEXT_TYPE.toString());
            msg.setSentDate(new Date());
            Transport.send(msg);
        } catch (MessagingException e) {
            log.debug(ExceptionToString.format(e));
            return false;
        }
        return true;
    }
}
