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
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import rapture.common.DispatchReturn;
import rapture.common.shared.question.DispatchQuestionFunction;

@WebServlet(urlPatterns={"/question","/question/*"})
@MultipartConfig
public class QuestionServlet extends BaseServlet {
    
    private static final long serialVersionUID = -654951209810427391L;
    @SuppressWarnings("unused")
    private static final String QUESTION_URI_PREFIX = "question:/";
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(QuestionServlet.class);

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        StandardCallInfo call = processFunctionalRequest(req);

        DispatchQuestionFunction questionDispatch = DispatchQuestionFunction.valueOf(call.getFunctionName());
        DispatchReturn response;
        try {
            response = questionDispatch.executeDispatch(call.getContent(), req, resp);
        } catch (Exception e) {
            response = handleUnexpectedException(e);
        }
        sendResponseAppropriately(response.getContext(), req, resp, response.getResponse());

    }

    /*
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        
        String questionPath = req.getPathInfo();
        
        CallingContext callingContext = BaseDispatcher.validateSession(req);
        
        if(!callingContext.getValid()) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Must be logged in");
        }
        
        QuestionApiImplWrapper question = Kernel.getQuestion();
        QuestionContainer questionContainer = question.(callingContext, QUESTION_URI_PREFIX + questionPath);
        if (questionContainer.getHeaders() != null) {
        	String contentType = questionContainer.getHeaders().get(ContentEnvelope.CONTENT_TYPE_HEADER);
        	if (contentType != null) {
        		resp.setContentType(contentType);
        	}
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getOutputStream().write(questionContainer.getContent());
        
    }
    */

}
