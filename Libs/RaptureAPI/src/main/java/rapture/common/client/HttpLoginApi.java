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
package rapture.common.client;

import rapture.client.ClientApiVersion;
import rapture.common.CallingContext;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.MD5Utils;
import rapture.common.model.ContextResponseData;
import rapture.common.model.GeneralResponse;
import rapture.common.shared.login.LoginPayload;
import rapture.common.shared.login.RequestSessionPayload;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

public class HttpLoginApi extends BaseHttpApi {

    static final Logger log = Logger.getLogger(HttpLoginApi.class);
    private CredentialsProvider credentialsProvider;

    public HttpLoginApi(String hostUrl, CredentialsProvider provider) {
        super(hostUrl, "login");
        this.credentialsProvider = provider;
    }

    private CallingContext doLogin(String user, String hashPassword, String session) {
        LoginPayload requestObj = new LoginPayload();
        requestObj.setUser(user);
        requestObj.setDigest(hashPassword);
        requestObj.setContext(session);
        requestObj.setClientApiVersion(ClientApiVersion.getApiVersion());
        try {
            String responseObjectJson = makeRequest("LOGIN", JacksonUtil.jsonFromObject(requestObj));
            GeneralResponse resp = JacksonUtil.objectFromJson(responseObjectJson, GeneralResponse.class);
            if (resp.isInError()) {
                throwError(resp);
            }
            return JacksonUtil.objectFromJson(resp.getResponse().getContent(), new TypeReference<CallingContext>() {
            });
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error making HTTP request", e);
        }
    }

    public void login() {
        String user = credentialsProvider.getUserName();
        String password = credentialsProvider.getPassword();
        // Login, get session id, use that session id in future context calls
        ContextResponseData context = requestSession(user);
        String hashedPassword = MD5Utils.hash16(MD5Utils.hash16(password) + ":" + context.getSalt());
        CallingContext context2 = doLogin(user, hashedPassword, context.getContextId());
        setContext(context2);
    }

    public ContextResponseData requestSession(String user) {
        RequestSessionPayload requestObj = new RequestSessionPayload();
        requestObj.setUser(user);
        String responseObjectJson;

        if (log.isTraceEnabled()) log.trace("User is " + user);
        try {
            responseObjectJson = makeRequest("CONTEXT", JacksonUtil.jsonFromObject(requestObj));
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error making HTTP request", e);
        }
        if (log.isTraceEnabled()) log.trace("response is " + responseObjectJson);

        if (responseObjectJson != null && responseObjectJson.length() > 0) {
            GeneralResponse resp = JacksonUtil.objectFromJson(responseObjectJson, GeneralResponse.class);
            if (resp.isInError()) {
                throwError(resp);
            }
            return JacksonUtil.objectFromJson(resp.getResponse().getContent(), new TypeReference<ContextResponseData>() {
            });
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    "Received Zero Length JSON response! Make sure the Rapture URL is correct. (Current URL is: " + this.fullUrl + ")");
        }
    }

}
