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
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.DispatchReturn;
import rapture.common.RaptureScript;
import rapture.common.RaptureURI;
import rapture.common.exception.RaptNotLoggedInException;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.server.BaseDispatcher;

/**
 * Created by yanwang on 6/3/15.
 */
public class JavaScriptPageServlet extends BaseServlet {

    private static final long serialVersionUID = 8271972998410468347L;

    private static Logger logger = Logger.getLogger(JavaScriptPageServlet.class);

    private String scriptPrefix;

    @Override
    public void init() throws ServletException {
        logger.info("INITIALIZING....");
        scriptPrefix = getServletConfig().getInitParameter("prefix");
        if (scriptPrefix == null) {
            throw new ServletException("No prefix parameter");
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> parameterMap = new HashMap<>();
        Enumeration<String> e = req.getParameterNames();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            String val = req.getParameter(key);
            parameterMap.put(key, val);
        }
        process(parameterMap, req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Map<String, String> parameterMap = new HashMap<>();
        Map<String, Object> props = getParams(req);
        for (String key : props.keySet()) {
            Object val = props.get(key);
            String valStr = null;
            if (val instanceof String) {
                valStr = URLDecoder.decode((String) val, "UTF-8");
            } else if (val != null) {
                valStr = val.toString();
            }
            parameterMap.put(key, valStr);
        }
        process(parameterMap, req, resp);
    }

    private void process(Map<String, String> parameterMap, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.debug("req is " + req);
        logger.debug("resp is " + resp);
        logger.debug("parameterMap is " + parameterMap);

        // check script exists
        RaptureURI scriptURI = getScriptURI(req);
        logger.info(String.format("Running script for uri %s", scriptURI.toString()));
        RaptureScript script = Kernel.getScript().getScript(ContextFactory.ADMIN, scriptURI.toString());
        if (script == null || StringUtils.isBlank(script.getScript())) {
            logger.warn("Could not locate script for uri - " + scriptURI.toString());
            resp.setStatus(HttpStatus.SC_NOT_FOUND);
            return;
        }
        // run JavaScript
        DispatchReturn response;
        try {
            CallingContext context = BaseDispatcher.validateSession(req);
            if (context != null) {
                logger.trace("Got session context " + context.debug());
                String result = Kernel.getScript().runScript(context, scriptURI.getFullPath(), parameterMap);
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().append(result);
                resp.setContentType("text/plain");
            } else {
                String err = "Cannot execute script " + script + " : cannot get session context for authorization";
                logger.error(err);
                resp.sendError(HttpURLConnection.HTTP_UNAUTHORIZED, err);
            }
        } catch (RaptNotLoggedInException re) {
            logger.error("Cannot execute script " + script + " : " + re.getMessage());
            resp.sendError(re.getStatus(), re.getMessage());
        } catch (Exception e) {
            response = handleUnexpectedException(e);
            sendResponseAppropriately(response.getContext(), req, resp, response.getResponse());
        }
    }

    private RaptureURI getScriptURI(HttpServletRequest req) {
        String scriptName = req.getPathInfo();
        return new RaptureURI(scriptPrefix + scriptName);
    }
}
