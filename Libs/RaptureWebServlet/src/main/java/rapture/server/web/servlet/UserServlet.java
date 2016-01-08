/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import rapture.common.DispatchReturn;
import rapture.common.shared.user.DispatchUserFunction;

@MultipartConfig
public class UserServlet extends BaseServlet {
    /**
	 * 
	 */
    private static final long serialVersionUID = 3495935250547858186L;
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(UserServlet.class);

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        StandardCallInfo call = processFunctionalRequest(req);

        DispatchUserFunction userDispatch = DispatchUserFunction.valueOf(call.getFunctionName());
        DispatchReturn response;
        try {
            response = userDispatch.executeDispatch(call.getContent(), req, resp);
        } catch (Exception e) {
            response = handleUnexpectedException(e);
        }

        // Send the response and we're done

        sendResponseAppropriately(response.getContext(), req, resp, response.getResponse());


    }
}
