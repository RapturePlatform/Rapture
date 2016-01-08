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
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.ErrorWrapper;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptNotLoggedInException;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.script.KernelScript;
import rapture.server.BaseDispatcher;
import reflex.ReflexException;
import reflex.ReflexExecutor;

/**
 * The Reflex script page servlet is kind of like jsps but for Reflex scripts
 * (Files with extension rfx)
 *
 * @author alan
 */

public abstract class BaseReflexScriptPageServlet extends BaseServlet {

    /**
     *
     */
    private static final long serialVersionUID = 2930792109818985861L;

    // private static Logger log = Logger
    // .getLogger(BaseReflexScriptPageServlet.class);

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.debug("req is " + req);
        logger.debug("resp is " + resp);
        logger.debug("request variables " + getRequestVariables(req));

        String script = getReflexScript(req);
        if (script == null || script.isEmpty()) {
            resp.setStatus(HttpStatus.SC_NOT_FOUND);
            return;
        }
        Map<String, Object> parameterMap = new HashMap<String, Object>();

        Map<String, Map<String, Object>> paramSet = new HashMap<>();
        Enumeration<String> e = req.getParameterNames();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            String[] val = req.getParameterValues(key);
            Object arg;

            if (key.endsWith("[]")) {
                arg = val;
                key = key.substring(0, key.length() - 2);
            } else
                arg = val[0];
            
            parameterMap.put(key, arg);

            int idx = key.lastIndexOf("[");
            while (idx > 0) {
                String v = key.substring(idx+1, key.length()-1);
                String k = key.substring(0, idx);
                Map<String, Object> map = paramSet.get(k);
                if (map == null) {
                    map = new HashMap<String, Object>();
                    paramSet.put(k, map);
                }
                map.put(v, arg);
                arg = map;
                parameterMap.put(k, map);
                
                key = k;
                idx = key.lastIndexOf("[");
            }
        }
        process(script, parameterMap, req, resp);
    }

    /**
     * Returns a printable version of the script. E.g. the script has a URI, that's what this returns. Otherwise, it
     * returns a subset of the script body.
     *
     * @param req
     * @return
     */
    protected abstract String getPrintableScript(HttpServletRequest req);

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        logger.debug("req is " + req);
        logger.debug("resp is " + resp);
        logger.debug("request variables " + getRequestVariables(req));

        String script = getReflexScript(req);
        if (script == null || script.isEmpty()) {
            resp.setStatus(HttpStatus.SC_NOT_FOUND);
            return;
        }
        Properties props = getParams(req);
        Map<String, Object> parameterMap = new HashMap<String, Object>();

        Set<Object> keys = props.keySet();
        for (Object k : keys) {
            String key = k.toString();
            String val = URLDecoder.decode(props.getProperty(key), "UTF-8");
            parameterMap.put(key, val);
        }
        process(script, parameterMap, req, resp);
    }

    protected abstract String getReflexScript(HttpServletRequest req);

    private static final Logger logger = Logger.getLogger(BaseReflexScriptPageServlet.class);

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    void process(String script, Map<String, Object> parameterMap, HttpServletRequest req, HttpServletResponse resp) throws IOException {

        logger.debug("script is " + script);
        logger.debug("parameterMap is " + parameterMap);

        Map<String, Object> masterParameterMap = new HashMap<String, Object>();
        masterParameterMap.put("web", parameterMap);
        masterParameterMap.put("SERVER", getRequestVariables(req));
        // Now run script
        ReflexScriptPageHandler handler = new ReflexScriptPageHandler();
        KernelScript kScript = new KernelScript();

        CallingContext context;
        try {
            context = BaseDispatcher.validateSession(req);
            if (context != null) {
                logger.trace("Got session context " + context.debug());
                kScript.setCallingContext(context);
                handler.setScriptApi(kScript);

                ReflexExecutor.runReflexProgram(script, handler, masterParameterMap);
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().append(handler.getOutput());
                resp.setContentType("text/plain");
            } else {
                String err = "Cannot execute script " + script + " : cannot get session context for authorization";
                logger.error(err);
                resp.sendError(HttpURLConnection.HTTP_UNAUTHORIZED, err);
            }
        } catch (RaptNotLoggedInException re) {
            logger.error("Cannot execute script " + script + " : " + re.getMessage());
            resp.sendError(re.getStatus(), re.getMessage());
        } catch (ReflexException re) {
            logger.debug(ExceptionToString.format(re));
            Throwable cause = re.getCause();
            if (cause == null) {
                cause = re;
            }
            while (cause.getCause() != null && !(cause instanceof RaptureException)) {
                cause = cause.getCause();
            }

            RaptureException rapEx;
            if (cause instanceof RaptureException) {
                rapEx = (RaptureException) cause;
            } else {
                if (re.getMessage() != null) {
                    rapEx = RaptureExceptionFactory.create("Error calling Reflex script: " + re.getMessage(), re);
                } else {
                    rapEx = RaptureExceptionFactory.create("Error calling Reflex script", re);
                }
            }
            logger.error("Cannot execute script " + getPrintableScript(req) + " : " + rapEx.getFormattedMessage());

            ErrorWrapper ew = new ErrorWrapper();
            ew.setStatus(rapEx.getStatus());
            ew.setMessage(rapEx.getMessage());
            ew.setId(rapEx.getId());

            sendVerboseError(resp, ew);
        }
        resp.flushBuffer();
    }

    private void sendVerboseError(HttpServletResponse resp, ErrorWrapper ew) throws IOException {
        Map<String, Object> map = JacksonUtil.getHashFromObject(ew);
        map.put("error", ew.getMessage());
        String r = JacksonUtil.jsonFromObject(map);
        resp.setStatus(HttpURLConnection.HTTP_INTERNAL_ERROR);
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().append(r);
        resp.setContentType("text/plain");
    }

    private Map<String, Object> getRequestVariables(HttpServletRequest req) {
        Map<String, Object> serverMap = new HashMap<String, Object>();
        serverMap.put("ContentType", req.getContentType());
        serverMap.put("ContextPath", req.getContextPath());
        serverMap.put("Method", req.getMethod());
        serverMap.put("PathInfo", req.getPathInfo());
        serverMap.put("RemoteAddr", req.getRemoteAddr());
        serverMap.put("", req.getLocalAddr());
        serverMap.put("Headers", getHeaderVariables(req));
        serverMap.put("Attributes", getHeaderAttributes(req));
        return serverMap;
    }

    private Map<String, Object> getHeaderVariables(HttpServletRequest req) {
        Map<String, Object> headerMap = new HashMap<String, Object>();
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headerMap.put(headerName, req.getHeader(headerName));
        }
        return headerMap;
    }

    private Map<String, Object> getHeaderAttributes(HttpServletRequest req) {
        Map<String, Object> variables = new HashMap<String, Object>();
        Enumeration<String> vars = req.getAttributeNames();
        while (vars.hasMoreElements()) {
            String attrName = vars.nextElement();
            variables.put(attrName, req.getAttribute(attrName));
        }
        return variables;
    }
}
