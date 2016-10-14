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
package rapture.server.web.servlet;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import rapture.common.DispatchReturn;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.kernel.Kernel;
import rapture.server.BaseDispatcher;
import rapture.server.login.LoginDispatchEnum;

/**
 * The login servlet is used to issue RaptureContexts and to validate RaptureContexts.
 * 
 * The request is always posted, and contains the following fields:
 * 
 * 1. METHOD - "RequestContext", "Login", "ValidateContext" 2. CONTENT - the content of the request
 * 
 * We then call the appropriate method (doRequestContext(params) ) and the response is passed back to the caller as a JSON string, which either contains an
 * exception or a valid response.
 * 
 * Login is a two-phase process. First a caller requests a context - this is a random uuid, but we store that as data, but blank apart from the username the
 * context is for. A set of salt is also computed.
 * 
 * Then the caller performs a login by sending a hash(hash(password), salt) where password is the password for that user.
 * 
 * The server can also compute hash(hash(password), salt) as it has hash(password) and salt. If the strings are equal the user is logged in and the context id
 * is associated with that user for a certain amount of time.
 * 
 * @author alan
 * 
 */
@WebServlet("/login")
@MultipartConfig
public class LoginServlet extends BaseServlet {

    private static final long serialVersionUID = -4771514093502324701L;

    private boolean checkIPAddressValid(String remoteAddr) {
        return Kernel.getKernel().checkIPAddress(remoteAddr);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        DispatchReturn response;
        if (checkIPAddressValid(req.getRemoteAddr())) {
            StandardCallInfo call = processFunctionalRequest(req);

            LoginDispatchEnum loginDispatch = LoginDispatchEnum.valueOf(call.getFunctionName());
            try {
                response = loginDispatch.executeDispatch(call.getContent(), req, resp);
            } catch (Exception e) {
                response = handleUnexpectedException(e);
            }
        } else {
            RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_UNAUTHORIZED,
                    String.format("IP Address not approved for access", req.getRemoteAddr()));
            response = new DispatchReturn(BaseDispatcher.error(raptException));
        }
        // Send the response and we're done

        sendResponseAppropriately(response.getContext(), req, resp, response.getResponse());

    }
}
