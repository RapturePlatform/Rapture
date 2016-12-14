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

import java.net.HttpURLConnection;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.exception.RaptureExceptionFormatter;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.MD5Utils;
import rapture.common.model.ContextResponseData;
import rapture.common.model.RaptureUser;
import rapture.common.model.RaptureUserStorage;
import rapture.common.version.ApiVersion;
import rapture.common.version.ApiVersionComparator;
import rapture.server.ServerApiVersion;
import rapture.util.IDGenerator;

public class Login extends KernelBase {

    class KernelInfo {
        private String version = "0.1";

        private String applicationName = "Rapture";

        public String getApplicationName() {
            return applicationName;
        }

        public String getVersion() {
            return version;
        }

        public void setApplicationName(String applicationName) {
            this.applicationName = applicationName;
        }

        public void setVersion(String version) {
            this.version = version;
        }

    }

    private static Logger log = Logger.getLogger(Login.class);

    public Login(Kernel raptureKernel) {
        super(raptureKernel);
    }

    public CallingContext checkLogin(String context, String username, String saltedPassword, ApiVersion clientApiVersion) {
        long functionStartTime = System.currentTimeMillis();

        String documentName = "session/" + context;
        String content;
        if (!ApiVersionComparator.INSTANCE.isCompatible(clientApiVersion)) {
            String message = String.format("Client API Version (%s) does not match server API Version (%s)", clientApiVersion,
                    ServerApiVersion.getApiVersion());
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, message);
        }

        content = getEphemeralRepo().getDocument(documentName);
        CallingContext savedContext = JacksonUtil.objectFromJson(content, CallingContext.class);
        RaptureUser userAccount = Kernel.getAdmin().getUser(ContextFactory.getKernelUser(), username);
        String userPassInvalid = String.format("username or password invalid (attempted username '%s')", username);
        if (userAccount == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_UNAUTHORIZED, userPassInvalid);
        }
        if (username.equals(savedContext.getUser())) {
            if (userAccount.getInactive()) {
                String message = "Cannot login as an inactive user";
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_UNAUTHORIZED, message);
            }
            if (!userAccount.getVerified()) {
                String message = "This account has not yet been verified. Please check your email at "+userAccount.getEmailAddress()+" for the verification link.-";
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_UNAUTHORIZED, message);
            }
            if (userAccount.getApiKey()) {
                savedContext.setValid(true);
            } else {
                String toHash = userAccount.getHashPassword() + ":" + savedContext.getSalt();
                String testHash = MD5Utils.hash16(toHash);
                if (testHash.equals(saltedPassword)) {
                    savedContext.setValid(true);
                    String msg = "User " + username + " logged in";
                    log.info(msg);
                    Kernel.writeComment(msg);
                } else {
                    RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_UNAUTHORIZED, userPassInvalid);
                    log.info(RaptureExceptionFormatter.getExceptionMessage(raptException, "Passwords do not match"));
                    throw raptException;

                }
            }
        }
        getEphemeralRepo().addToStage(RaptureConstants.OFFICIAL_STAGE, documentName, JacksonUtil.jsonFromObject(savedContext), false);
        getEphemeralRepo().commitStage(RaptureConstants.OFFICIAL_STAGE, "admin", "session validation");

        // user has successfully logged in, lets write it to the audit logs
        Kernel.getAudit().getTrusted().writeAuditEntry(savedContext, RaptureConstants.DEFAULT_AUDIT_URI, "login", 0,
                String.format("User [%s] has logged in", username));

        long endFunctionTime = System.currentTimeMillis();
        Kernel.getMetricsService().recordTimeDifference("apiMetrics.loginApi.checkLogin.fullFunctionTime.succeeded", (endFunctionTime - functionStartTime));

        return savedContext;
    }

    
    
    /**
     * Given a username, validate the user exists, create a context for this user
     *
     * @param user
     * @return
     * @throws Exception
     */
    public ContextResponseData getContextForUser(String user) {
        long functionStartTime = System.currentTimeMillis();

        log.info("Checking user " + user + " exists.");
        RaptureUser usr = Kernel.getAdmin().getUser(ContextFactory.getKernelUser(), user);
        if (usr != null) {
            log.info("Found user " + usr.getUsername());
            if (!usr.getInactive()) {
                log.info("User " + usr.getUsername() + " is active.");
            } else {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_UNAUTHORIZED, String.format("user: '%s' is inactive.", user));
            }
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_UNAUTHORIZED, String.format("No such user: '%s'", user));
        }

        String contextId = IDGenerator.getUUID();
        log.info("Context id is " + contextId);
        ContextResponseData resp = new ContextResponseData();
        resp.setContextId(contextId);
        resp.setSalt(IDGenerator.getUUID());

        CallingContext logContext = new CallingContext();
        logContext.setContext(contextId);
        logContext.setSalt(resp.getSalt());
        logContext.setUser(user);

        getEphemeralRepo().addToStage(RaptureConstants.OFFICIAL_STAGE, "session/" + contextId, JacksonUtil.jsonFromObject(logContext), false);
        getEphemeralRepo().commitStage(RaptureConstants.OFFICIAL_STAGE, "admin", "session creation");

        long endFunctionTime = System.currentTimeMillis();
        Kernel.getMetricsService().recordTimeDifference("apiMetrics.loginApi.getContextForUser.fullFunctionTime.succeeded",
                (endFunctionTime - functionStartTime));

        return resp;
    }

    public KernelInfo getInfo() {
        return new KernelInfo();
    }

    /**
     * @deprecated Call {@link #loginWithHash(String, String, ApiVersion)} instead
     */
    public CallingContext loginWithHash(String userName, String hashPassword) {
        return loginWithHash(userName, hashPassword, null);
    }

    public CallingContext loginWithHash(String userName, String hashPassword, ApiVersion clientApiVersion) {
        long functionStartTime = System.currentTimeMillis();

        if (hashPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        if (userName == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        ContextResponseData context = getContextForUser(userName);
        String hashedPassword = MD5Utils.hash16(hashPassword + ":" + context.getSalt());

        CallingContext returnContext = checkLogin(context.getContextId(), userName, hashedPassword, clientApiVersion);

        long endFunctionTime = System.currentTimeMillis();
        Kernel.getMetricsService().recordTimeDifference("apiMetrics.loginApi.loginWithHash.fullFunctionTime.succeeded", (endFunctionTime - functionStartTime));

        return returnContext;
    }

    /**
     * @deprecated Call {@link #login(String, String, ApiVersion)} instead
     */
    public CallingContext login(String userName, String password) {
        return login(userName, password, null);
    }

    public CallingContext login(String userName, String password, ApiVersion version) {
        long functionStartTime = System.currentTimeMillis();

        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        if (userName == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        ContextResponseData context = getContextForUser(userName);
        String hashedPassword = MD5Utils.hash16(MD5Utils.hash16(password) + ":" + context.getSalt());

        CallingContext returnContext = checkLogin(context.getContextId(), userName, hashedPassword, version);

        long endFunctionTime = System.currentTimeMillis();
        Kernel.getMetricsService().recordTimeDifference("apiMetrics.loginApi.login.fullFunctionTime.succeeded", (endFunctionTime - functionStartTime));

        return returnContext;
    }

    public String createRegistrationToken(String username) {
        long functionStartTime = System.currentTimeMillis();

        CallingContext context = ContextFactory.getKernelUser();
        String token = Kernel.getAdmin().createRegistrationToken(context, username);

        long endFunctionTime = System.currentTimeMillis();
        Kernel.getMetricsService().recordTimeDifference("apiMetrics.loginApi.login.fullFunctionTime.succeeded", (endFunctionTime - functionStartTime));
        return token;
    }

    public String createPasswordResetToken(String username) {
        long functionStartTime = System.currentTimeMillis();

        CallingContext context = ContextFactory.getKernelUser();
        String token = Kernel.getAdmin().createPasswordResetToken(context, username);

        long endFunctionTime = System.currentTimeMillis();
        Kernel.getMetricsService().recordTimeDifference("apiMetrics.loginApi.login.fullFunctionTime.succeeded", (endFunctionTime - functionStartTime));
        return token;
    }

    public void cancelPasswordResetToken(String username) {
        long functionStartTime = System.currentTimeMillis();

        CallingContext context = ContextFactory.getKernelUser();
        Kernel.getAdmin().cancelPasswordResetToken(context, username);

        long endFunctionTime = System.currentTimeMillis();
        Kernel.getMetricsService().recordTimeDifference("apiMetrics.loginApi.login.fullFunctionTime.succeeded", (endFunctionTime - functionStartTime));
    }

    public void resetPassword(String username, String hashPassword, String passwordResetToken) {
        long functionStartTime = System.currentTimeMillis();

        CallingContext context = ContextFactory.getKernelUser();
        RaptureUser user = Kernel.getAdmin().getUser(context, username);
        if (StringUtils.isBlank(user.getPasswordResetToken()) || !passwordResetToken.equals(user.getPasswordResetToken())) {
            throw RaptureExceptionFactory.create("Invalid password reset token");
        }
        if (user.getTokenExpirationTime() <= System.currentTimeMillis()) {
            throw RaptureExceptionFactory.create("Password reset token has expired");
        }
        user.setHashPassword(hashPassword);
        RaptureUserStorage.add(user, username, "Reset password for user " + username); //$NON-NLS-1$
        // cancel token
        Kernel.getAdmin().cancelPasswordResetToken(context, username);

        long endFunctionTime = System.currentTimeMillis();
        Kernel.getMetricsService().recordTimeDifference("apiMetrics.loginApi.login.fullFunctionTime.succeeded", (endFunctionTime - functionStartTime));
    }
}
