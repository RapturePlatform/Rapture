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
package rapture.mail;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.stringtemplate.v4.ST;
import rapture.common.CallingContext;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

/**
 * Created by yanwang on 11/18/15.
 */
public class Mailer {
    private static Logger log = Logger.getLogger(Mailer.class);

    public static String SMTP_CONFIG_URL = "email/config";
    public static String EMAIL_TEMPLATE_DIR = "email/template/";

    public static void email(CallingContext context, String templateName, Map<String, ? extends Object> templateValues) {
        final SMTPConfig config = getSMTPConfig();
        Session session = getSession(config);
        EmailTemplate template = getEmailTemplate(context, templateName);
        try {
            // Instantiate a new MimeMessage and fill it with the required information.
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(config.getFrom()));
            String to = renderTemplate(template.getEmailTo(), templateValues);
            if(StringUtils.isBlank(to)) {
                throw RaptureExceptionFactory.create("No emailTo field");
            }
            InternetAddress[] address = { new InternetAddress(to) };
            msg.setRecipients(Message.RecipientType.TO, address);
            msg.setSubject(renderTemplate(template.getSubject(), templateValues));
            msg.setSentDate(new Date());
            msg.setContent(renderTemplate(template.getMsgBody(), templateValues), "text/html; charset=utf-8");
            // Hand the message to the default transport service for delivery.
            Transport.send(msg);
        } catch (MessagingException e) {
            log.error("Failed to send email", e);
        }
    }

    public static Session getSession(final SMTPConfig config) {
        // Create some properties and get the default Session.
        Properties props = System.getProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.host", config.getHost());
        props.put("mail.smtp.port", config.getPort());
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getUsername(), config.getPassword());
            }
        });
        return session;
    }

    public static SMTPConfig getSMTPConfig() {
        String configString = Kernel.getSys().retrieveSystemConfig(
                ContextFactory.getKernelUser(), "CONFIG", SMTP_CONFIG_URL);
        if(StringUtils.isBlank(configString)) {
            throw RaptureExceptionFactory.create("No SMTP configured");
        }
        return JacksonUtil.objectFromJson(configString, SMTPConfig.class);
    }

    public static EmailTemplate getEmailTemplate(CallingContext context, String templateName) {
        String templateJson = Kernel.getSys().retrieveSystemConfig(
                context, "CONFIG", EMAIL_TEMPLATE_DIR + templateName);
        if(StringUtils.isBlank(templateJson)) {
            throw RaptureExceptionFactory.create("Email template " + templateName + " does not exist");
        }
        return JacksonUtil.objectFromJson(templateJson, EmailTemplate.class);
    }

    public static String renderTemplate(String templateStr, Map<String, ? extends Object> templateValues) {
        ST template = new ST(templateStr, '$', '$');
        if(templateValues != null) {
            for(String key : templateValues.keySet()) {
                template.add(key, templateValues.get(key));
            }
        }
        return template.render();
    }

}
