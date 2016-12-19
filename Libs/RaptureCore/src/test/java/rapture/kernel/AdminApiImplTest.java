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
import static org.junit.Assert.assertNull;

import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import com.google.common.collect.ImmutableMap;

import rapture.common.CallingContext;
import rapture.common.api.AdminApi;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.MD5Utils;
import rapture.common.model.RaptureUser;
import rapture.mail.Mailer;
import rapture.mail.SMTPConfig;

/**
 * @author bardhi
 * @since 4/27/15.
 */
public class AdminApiImplTest {

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
        String geezer = "geezer";
        AdminApi admin = Kernel.getAdmin();

        // SMTPConfig emailCfg = new SMTPConfig().setHost("email-smtp.us-west-2.amazonaws.com").setPort(587).setUsername("AKIAITJH4OMD772SGJEA")
        // .setPassword("AsrWwMMyGHLJbJMEidXPH7b0d/s8/K7b41udMFDZXRlF").setFrom("Incapture <support@incapturetechnologies.com>").setAuthentication(true)
        // .setTlsenable(true).setTlsrequired(true);
        Wiser wiser = new Wiser();
        try {
            wiser.setPort(2525); // Default is 25
            wiser.start();

            SMTPConfig emailCfg = new SMTPConfig().setHost("localhost").setPort(2525).setUsername("").setPassword("")
                    .setFrom("Incapture <support@incapturetechnologies.com>").setAuthentication(false).setTlsenable(false).setTlsrequired(false);

            Kernel.getSys().writeSystemConfig(context, "CONFIG", Mailer.SMTP_CONFIG_URL, JacksonUtil.jsonFromObject(emailCfg));
            Map<String, String> map = ImmutableMap.of("msgBody", "Hi", "emailTo", "$user.emailAddress$", "subject", "Rapture Password Reset");
            Kernel.getSys().writeSystemConfig(context, "CONFIG", "email/template/CREATE_PASSWORD_RESET_TOKEN", JacksonUtil.jsonFromObject(map));

            if (!admin.doesUserExist(context, geezer)) {
                admin.addUser(context, geezer, "Geezer Butler", MD5Utils.hash16(geezer), "GEEZER@SABBATH.COM");
            }

            Map<String, Object> tmap = ImmutableMap.of("username", geezer);
            Kernel.getAdmin().emailUser(context, geezer, "CREATE_PASSWORD_RESET_TOKEN", tmap);

            for (WiserMessage message : wiser.getMessages()) {
                String envelopeSender = message.getEnvelopeSender();
                String envelopeReceiver = message.getEnvelopeReceiver();
                MimeMessage mess = message.getMimeMessage();
                System.out.println("From " + envelopeSender + " To " + envelopeReceiver + " Body " + JacksonUtil.jsonFromObject(mess.getContent().toString()));
                assertEquals("support@incapturetechnologies.com", envelopeSender);
                assertEquals("GEEZER@SABBATH.COM", envelopeReceiver);
                assertEquals("Hi\r\n", mess.getContent().toString());
                break;
            }
        } finally {
            wiser.stop();
        }
    }
}
