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
package rapture.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import com.google.common.collect.ImmutableMap;

import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.AdminApi;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.MD5Utils;
import rapture.common.model.RaptureUser;
import rapture.common.model.RaptureUserStorage;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.mail.Mailer;
import rapture.mail.SMTPConfig;

/**
 * @author bardhi
 * @since 4/27/15.
 */
public class AdminApiImplTest {

    static final Wiser wiser = new Wiser();
    static final CallingContext context = ContextFactory.getKernelUser();
    static final AdminApi admin = Kernel.getAdmin();
    static final String configStr = "CONFIG";
    static final String geezer = "geezer";

    static String saveRaptureRepo;
    static String saveInitSysConfig;
    static CallingContext rootContext;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        RaptureConfig.setLoadYaml(false);
        RaptureConfig config = ConfigLoader.getConf();

        saveRaptureRepo = config.RaptureRepo;
        saveInitSysConfig = config.InitSysConfig;

        System.setProperty("LOGSTASH-ISENABLED", "false");

        Kernel.INSTANCE.restart();
        Kernel.initBootstrap();
        rootContext = ContextFactory.getKernelUser();

        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");

        wiser.setPort(2525);
        wiser.start();

        SMTPConfig emailCfg = new SMTPConfig().setHost("localhost").setPort(2525).setUsername("").setPassword("")
                .setFrom("Incapture <support@incapturetechnologies.com>").setAuthentication(false).setTlsenable(false).setTlsrequired(false);
        Kernel.getSys().writeSystemConfig(context, configStr, Mailer.SMTP_CONFIG_URL, JacksonUtil.jsonFromObject(emailCfg));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        wiser.stop();
    }

    @Before
    public void setUp() throws Exception {
        Kernel.initBootstrap();
    }

    @After
    public void tearDown() throws Exception {
        Kernel.INSTANCE.restart();
    }

    @Test
    public void testGetUserBad() throws Exception {
        RaptureUser user = Kernel.getAdmin().getUser(ContextFactory.getKernelUser(), "*we*34as??ds//");
        assertNull(user);
    }

    @Test
    public void testEmailUser() throws Exception {
        CallingContext context = ContextFactory.getKernelUser();
        AdminApi admin = Kernel.getAdmin();

        Map<String, String> map = ImmutableMap.of("msgBody", "Hi", "emailTo", "$user.emailAddress$", "subject", "Test Rapture Password Reset");
        Kernel.getSys().writeSystemConfig(context, "CONFIG", "email/template/CREATE_PASSWORD_RESET_TOKEN", JacksonUtil.jsonFromObject(map));

        if (!admin.doesUserExist(context, geezer)) {
            admin.addUser(context, geezer, "Geezer Butler", MD5Utils.hash16(geezer), "GEEZER@SABBATH.COM");
        }

        Map<String, Object> tmap = ImmutableMap.of("username", geezer);
        Kernel.getAdmin().emailUser(context, geezer, "CREATE_PASSWORD_RESET_TOKEN", tmap);

        boolean found = false;
        for (WiserMessage message : wiser.getMessages()) {
            String envelopeSender = message.getEnvelopeSender();
            String envelopeReceiver = message.getEnvelopeReceiver();
            MimeMessage mess = message.getMimeMessage();
            if (mess.getSubject().equals(map.get("subject"))) {
                assertEquals("support@incapturetechnologies.com", envelopeSender);
                assertEquals("GEEZER@SABBATH.COM", envelopeReceiver);
                assertEquals("Hi\r\n", mess.getContent().toString());
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testVerifyResetTolkein() {
        if (!admin.doesUserExist(context, geezer)) {
            admin.addUser(context, geezer, "Geezer Butler", MD5Utils.hash16(geezer), "GEEZER@SABBATH.COM");
        }

        String token = admin.createPasswordResetToken(context, geezer);
        assertFalse(admin.verifyPasswordResetToken(context, geezer, "_" + token));
        assertTrue(admin.verifyPasswordResetToken(context, geezer, token));
        RaptureUser user = admin.getUser(context, geezer);
        Long expire = user.getTokenExpirationTime();
        user.setTokenExpirationTime(0L);
        RaptureUserStorage.add(user, user.getUsername(), "testing");
        assertFalse(admin.verifyPasswordResetToken(context, geezer, token));

        user.setTokenExpirationTime(expire);
        RaptureUserStorage.add(user, user.getUsername(), "testing");
        assertTrue(admin.verifyPasswordResetToken(context, geezer, token));

        admin.destroyUser(context, geezer);
        assertFalse(admin.verifyPasswordResetToken(context, geezer, token));

    }

    @Test
    public void testOrigin() throws MessagingException, IOException {
        if (!admin.doesUserExist(context, geezer)) {
            admin.addUser(context, geezer, "Geezer Butler", MD5Utils.hash16(geezer), "geezer@sabbath.com");
        }

        SMTPConfig emailCfg = new SMTPConfig().setHost("localhost").setPort(2525).setUsername("").setPassword("")
                .setFrom("Incapture <support@incapturetechnologies.com>").setAuthentication(false).setTlsenable(false).setTlsrequired(false).setDebug("INFO");
        Kernel.getSys().writeSystemConfig(context, configStr, Mailer.SMTP_CONFIG_URL, JacksonUtil.jsonFromObject(emailCfg));

        Map<String, String> reset = ImmutableMap.of("emailTo", "$user.emailAddress$", "subject", "Rapture Password Reset", "msgBody",
                "<!DOCTYPE html><html style='margin:0; padding:0; font-size: 16px; font-weight: normal;'><head><link href='http://fonts.googleapis.com/css?family=Open+Sans' rel='stylesheet' "
                        + "type='text/css'><meta http-equiv='Content-Type' content='text/html; charset=UTF-8' /><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                        + "<title>Rapture Password Reset</title><link href='https://fonts.googleapis.com/css?family=Montserrat|Open+Sans' rel='stylesheet'></head>"
                        + "<body style='margin: 0; padding: 0;'><table align='center' border='0' cellpadding='0' cellspacing='0' height='100%' width='100%' id='emailContainer' style='width: 100%;'>"
                        + "<tr><td align='center' valign='top' style='width: 100%; max-width: 620px; padding: calc(1.75% + 8px) 0; position: relative; background-image: url(https://s28.postimg.org/eld392gb1/rui_email_bg.png);background-position: center center;'><!-- <center> --><table align='center' border='0' cellpadding='0' cellspacing='0' height='100%' width='100%' id='emailContainer' style='width: 100%;'><tr>"
                        + "<div style='text-align: center;'><a href='$origin$' target='_blank' style='text-decoration: none'><img src='https://s28.postimg.org/8vwuora4t/rui_logo.png' alt='Rapture UI Logo' style='text-decoration:none; width: calc(10% + 32px); opacity: .92;' /></a></div></tr><tr>"
                        + "<div style='padding-top: 16px; text-align: center;'><a href='$origin$' target='_blank' style='text-decoration: none; color: #a7abb2; line-height: 14px; font-size: 20px; letter-spacing: 8px; margin-left: 8px; width: 100%; font-family: \"Montserrat\", Arial, sans-serif;'>RAPTURE</a></div></tr></table></td></tr><tr><td align='center' valign='top' style='word-break: break-all; background-color: #5d7cae; padding: 32px;'>"
                        + "<div style='width: 100%; max-width: 620px; margin:0 auto; text-align: left; font-size: 15px; color: #FFF; font-family: \"Open Sans\", Arial, sans-serif;'>Hello $userFullName$,<br>"
                        + "<br>You recently requested your password to be reset. Please click the button below to be linked to our secured server to complete this process. Note that this link is only valid for 24 hours.<br><br><br>"
                        + "<div style='text-align: center; padding: 16px 0 18px 0;'><a href='$origin$/signin/update_password/$user.username$/$user.passwordResetToken$' target='_blank' style='text-decoration:none; color: #fff; cursor: pointer; background: #94aedb; position: relative; padding: 16px 24px; border-bottom: 3px solid #7f9ccc; font-family: \"Montserrat\", Arial, sans-serif; font-size: 14px; letter-spacing: 2px; text-indent: 2px;'>"
                        + "<span>UPDATE MY PASSWORD</span></a></div><br><br>If you encounter any issues or have questions or believe this email was sent to you by mistake, please contact us at:<br><a style='color: #FFF' href='mailto:support@incapturetechnologies.com'>support@incapturetechnologies.com</a><br><br><br>Thank you for choosing Rapture,<br>The Incapture Technologies Team</div></td>"
                        + "</tr><tr><td align='center' valign='top' style='background-color: #4d6593; padding: 32px; color: #FFF; font-size: 12px; font-family: \"Open Sans\", Arial, sans-serif;'>"
                        + "<div style='width: 100%; max-width: 620px; position: relative; text-align: left;'>Incapture Technologies<br>600 Montgomery Street<br>Third Floor<br>San Francisco CA 94111</div><br><br>"
                        + "<div style='width: 100%; max-width: 620px; position: relative; text-align: left; height: 60px; line-height: 60px;'>"
                        + "<span style='float: right; height: 100%; line-height: 60px;'><img style='width: 140px' src='https://s28.postimg.org/b313coff1/incapture_logo_white.png'/></span>"
                        + "<span style='opacity: .75; margin-right: 12px;'><a href='//www.facebook.com/incapture' rel='external' target='_blank'><img style='width: 28px;' src='https://s28.postimg.org/g6sd0vqj1/facebook.png'/></a></span>"
                        + "<span style='opacity: .75; margin-right: 12px;'>"
                        + "<a href='//www.linkedin.com/company/2748426' rel='external' target='_blank'><img style='width: 28px;' src='https://s28.postimg.org/92ajruj9p/linkedin.png'/></a></span>"
                        + "<span style='opacity: .75; margin-right: 12px;'><a href='http://plus.google.com/117139879784498155144' rel='external' target='_blank'><img style='width: 28px;' src='https://s28.postimg.org/w2h746h3h/google_plus.png'/></a></span>"
                        + "<span style='opacity: .75; margin-right: 12px;'><a href='https://github.com/RapturePlatform/' rel='external' target='_blank'>"
                        + "<img style='width: 28px;' src='https://s28.postimg.org/53dc90uml/github_circle.png'/></a></span></div></td></tr></table></body></html>");
        String content = JacksonUtil.jsonFromObject(reset);
        Kernel.getSys().writeSystemConfig(context, configStr, "email/template/CREATE_PASSWORD_RESET_TOKEN", content);

        Map<String, Object> map = ImmutableMap.of("origin", "'", "userFullName", geezer);
        admin.emailUser(context, geezer, "CREATE_PASSWORD_RESET_TOKEN", map);

        boolean found = false;
        for (WiserMessage message : wiser.getMessages()) {
            String envelopeSender = message.getEnvelopeSender();
            String envelopeReceiver = message.getEnvelopeReceiver();
            MimeMessage mess = message.getMimeMessage();
            if (mess.getSubject().equals(reset.get("subject"))) {
                assertEquals("support@incapturetechnologies.com", envelopeSender);
                assertEquals("geezer@sabbath.com", envelopeReceiver);
                assertEquals(reset.get("msgBody").replaceAll(".origin.", map.get("origin").toString()).replaceAll(".userFullName.", geezer)
                        .replaceAll(".user.username.", geezer).replaceAll(".user.passwordResetToken.", "") + "\r\n",
                        mess.getContent().toString());
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testError() throws MessagingException, IOException {
        if (!admin.doesUserExist(context, geezer)) {
            admin.addUser(context, geezer, "Geezer Butler", MD5Utils.hash16(geezer), "geezer@sabbath.com");
        }

        wiser.getServer().setRequireTLS(true);

        SMTPConfig emailCfg = new SMTPConfig().setHost("localhost").setPort(2525).setUsername("").setPassword("")
                .setFrom("Incapture <support@incapturetechnologies.com>").setAuthentication(false).setTlsenable(false).setTlsrequired(false).setDebug("INFO");
        Kernel.getSys().writeSystemConfig(context, configStr, Mailer.SMTP_CONFIG_URL, JacksonUtil.jsonFromObject(emailCfg));
        Map<String, String> reset = ImmutableMap.of("emailTo", "$user.emailAddress$", "subject", "Rapture Password Reset", "msgBody", "This won't get sent");
        String content = JacksonUtil.jsonFromObject(reset);
        Kernel.getSys().writeSystemConfig(context, configStr, "email/template/CREATE_PASSWORD_RESET_TOKEN", content);

        Map<String, Object> map = ImmutableMap.of("origin", "'", "userFullName", geezer);
        try {
            admin.emailUser(context, geezer, "CREATE_PASSWORD_RESET_TOKEN", map);
        } catch (Exception e) {
            assertEquals("Cannot email user geezer at address geezer@sabbath.com : error is 530 Must issue a STARTTLS command first\n", e.getMessage());
        }
        wiser.getServer().setRequireTLS(false);

    }
}
