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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.client.ClientApiVersion;
import rapture.common.CallingContext;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.MD5Utils;
import rapture.common.model.RaptureUser;
import rapture.common.version.ApiVersion;
import rapture.mail.Mailer;
import rapture.server.ServerApiVersion;

public class LoginTest {

    private static String username = "raptureApi";
    private static String password = "new_password";
    private static String hashedPassword = MD5Utils.hash16(password);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testApiVersionBad() {
        ApiVersion clientApiVersion = new ApiVersion();
        clientApiVersion.setMajor(1);
        clientApiVersion.setMinor(0);
        boolean gotException = false;
        try {
            Kernel.getLogin().login("rapture", "rapture", clientApiVersion);
        } catch (RaptureException e) {
            gotException = true;
        }
        assertTrue(gotException);
    }

    @Test
    public void testApiVersionNoVersion() {
        boolean gotException = false;
        try {
            Kernel.getLogin().login("rapture", "rapture", null);
        } catch (RaptureException e) {
            gotException = true;
        }
        assertFalse(gotException);
    }

    @Test
    public void testApiVersionGood() {
        ApiVersion clientApiVersion = new ApiVersion();
        clientApiVersion.setMajor(2);
        clientApiVersion.setMinor(0);
        boolean gotException = false;
        try {
            Kernel.getLogin().login("rapture", "rapture", clientApiVersion);
        } catch (RaptureException e) {
            gotException = true;
        }
        assertFalse(gotException);
    }

    @Test
    public void testApiVersionServer() {
        boolean gotException = false;
        // should work, server api version is same
        try {
            Kernel.getLogin().login("rapture", "rapture", ServerApiVersion.getApiVersion());
        } catch (RaptureException e) {
            gotException = true;
        }
        assertFalse(gotException);
    }

    @Test
    public void testApiVersionClient() {
        boolean gotException = false;
        // should work, client api version is same as server since we're in the
        // same vm
        try {
            Kernel.getLogin().login("rapture", "rapture", ClientApiVersion.getApiVersion());
        } catch (RaptureException e) {
            gotException = true;
        }
        assertFalse(gotException);
    }

    private void setupMailer() {
        // create smtp config
        CallingContext context = ContextFactory.getKernelUser();
        String area = "CONFIG";
        String smtpConfig = "{\"host\":\"email-smtp.us-west-2.amazonaws.com\",\"port\":25,\"username\":\"AKIAITJH4OMD772SGJEA\",\"password\":\"AsrWwMMyGHLJbJMEidXPH7b0d/s8/K7b41udMFDZXRlF\",\"from\":\"support@incapturetechnologies.com\"}";
        Kernel.getSys().writeSystemConfig(context, area, Mailer.SMTP_CONFIG_URL, smtpConfig);

        // create dummy email template
        String templateName = "CREATE_PASSWORD_RESET_TOKEN";
        String template = "{\"emailTo\":\"emailAddress\",\"subject\":\"Ignore - generated from test\",\"msgBody\":\"This email is generated from test\"}";
        String url = Mailer.EMAIL_TEMPLATE_DIR + templateName;
        Kernel.getSys().writeSystemConfig(context, area, url, template);

        // update user email
        Kernel.getAdmin().updateUserEmail(context, username, "support@incapturetechnologies.com");
    }

    @Test
    public void testResetPasswordWithInvalidToken() {
        // reset password with invalid token should fail
        try {
            Kernel.getLogin().resetPassword(username, hashedPassword, "somerandomtoken");
        } catch (RaptureException e) {
            assertTrue(e.getMessage().contains("Invalid password reset token"));
        }
    }

    @Test
    public void testResetPasswordWithExpiredToken() {
        setupMailer();
        String token = Kernel.getLogin().createPasswordResetToken(username);
        RaptureUser user = Kernel.getAdmin().getUser(ContextFactory.getKernelUser(), username);
        assertNotNull(user.getPasswordResetToken());
        Kernel.getLogin().cancelPasswordResetToken(username);
        try {
            Kernel.getLogin().resetPassword(username, hashedPassword, token);
        } catch (RaptureException e ) {
            assertTrue(e.getMessage().contains("Password reset token has expired"));
        }
    }

    @Test
    public void testResetPasswordWithValidToken() {
        setupMailer();
        CallingContext context = ContextFactory.getKernelUser();

        String oldHashedPassword = Kernel.getAdmin().getUser(context, username).getHashPassword();
        String token = Kernel.getLogin().createPasswordResetToken(username);

        // reset password should succeed the first time
        Kernel.getLogin().resetPassword(username, hashedPassword, token);
        // reset password should fail after token is used
        try {
            Kernel.getLogin().resetPassword(username, hashedPassword, token);
        } catch (RaptureException e) {
            assertTrue(e.getMessage().contains("Password reset token has expired"));
        }

        // login with old password should fail
        try {
            Kernel.getLogin().loginWithHash(username, oldHashedPassword, getGoodVersion());
        } catch (RaptureException e) {
            assertTrue(e.getMessage().contains("username or password invalid"));
        }

        // login with new password should succeed
        Kernel.getLogin().login(username, password, getGoodVersion());
        Kernel.getLogin().loginWithHash(username, hashedPassword, getGoodVersion());
    }


    private ApiVersion getGoodVersion() {
        ApiVersion clientApiVersion = new ApiVersion();
        clientApiVersion.setMajor(2);
        clientApiVersion.setMinor(0);
        return clientApiVersion;
    }

}
