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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

import rapture.common.ErrorWrapper;
import rapture.common.RaptureScript;
import rapture.common.RaptureURI;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.script.KernelScript;
import reflex.ReflexException;
import reflex.ReflexExecutor;

/**
 * The session control servlet is used to manage login, logout and redirection. Basically the main work is done in a Reflex script (provided externally) which
 * should be manipulating an object that is injected into the Reflex scope. We look at that scope afterwards and take appropriate action - usually setting
 * cookies and redirecting. If we don't redirect we output the println output of the script (like ReflexScriptPageServlet)
 * 
 * Note that scripts run from this servlet are usually run without context checking, so care needs to be taken to set both the script context (to a place where
 * scripts are very readonly) and on what the script can actually do. Putting a bad script in the system could easily be used to login. In this way the scripts
 * cannot be nested, and the api is limited to the login api.
 * 
 * @author amkimian
 *
 */
public class SessionControlServlet extends BaseServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(SessionControlServlet.class);

    private String scriptPrefix;

    @Override
    public void init() throws ServletException {
        log.info("INITIALIZING....");

        scriptPrefix = getServletConfig().getInitParameter("prefix");
        if (scriptPrefix == null) {
            throw new ServletException("No prefix parameter");
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String path = req.getPathInfo();
        log.info("Path is " + path);

        String[] parts = path.split("/");
        String scriptName = parts[parts.length - 1];

        String scriptURL = scriptPrefix + scriptName;

        log.info("Script URL is " + scriptURL);

        // We look at the last part of the request.getPath(), that is the "root" name of the script
        // We then prepend that with the "real source" of the scripts
        // (so e.g. you may come in as /login/doLogin, but that gets translated to script://secure/doLogin )

        String script = getReflexScript(scriptURL);

        if (script == null || script.isEmpty()) {
            resp.setStatus(HttpStatus.SC_NOT_FOUND);
            return;
        }

        Map<String, Object> parameterMap = new HashMap<String, Object>();

        Enumeration<String> e = req.getParameterNames();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            String val = req.getParameter(key);
            parameterMap.put(key, val);
        }
        
        ResponseControl control = new ResponseControl();
        control = runScriptWithResponseControl(script, parameterMap, control, resp);

        // Run script with controlObject = variable "control"

        // Retrieve object back from context/scope, turn back into control object

        // Handle cookies first
        log.info("Returned from run script");
        ControlResponse r = new ControlResponse();
        if (control != null) {
            log.info("Control is not null");
            for (Map.Entry<String, ResponseCookie> entry : control.getCookieMap().entrySet()) {
                if (entry.getValue().isDelete()) {
                    log.info("Remove cookie");
                    Cookie killMyCookie = new Cookie(entry.getKey(), null);
                    killMyCookie.setMaxAge(0);
                    killMyCookie.setPath(entry.getValue().getPath());
                    resp.addCookie(killMyCookie);
                } else {
                    log.info("Add cookie");
                    Cookie cookie = new Cookie(entry.getKey(), entry.getValue().getValue());
                    cookie.setPath(entry.getValue().getPath());
                    resp.addCookie(cookie);
                }
            }

            log.info("Response code is " + control.getResponseCode());
            //resp.setStatus(control.getResponseCode());
            if (control.getPageRedirection() != null && !control.getPageRedirection().isEmpty()) {
                log.info("Sending redirect");
                r.setRedirect(control.getPageRedirection());
                //resp.sendRedirect(control.getPageRedirection());
            } else if (!StringUtils.isBlank(control.getMessage())) {
                r.setMessage(control.getMessage());
            }
            String content = JacksonUtil.jsonFromObject(r);
            log.info("Sending back " + content);
            resp.getWriter().append(content);
        }        
        resp.flushBuffer();
    }

    protected ResponseControl runScriptWithResponseControl(String script, Map<String, Object> parameterMap, ResponseControl control, HttpServletResponse resp) throws IOException {
        // Inject ResponseControl as an object (called "control")
        // Run script

        Map<String, Object> controlMap = JacksonUtil.getHashFromObject(control);

        Map<String, Object> masterParameterMap = new HashMap<String, Object>();
        masterParameterMap.put("control", controlMap);
        masterParameterMap.put("web", parameterMap);
                // Now run script
        ReflexScriptPageHandler handler = new ReflexScriptPageHandler();
        KernelScript kScript = new KernelScript();

        try {
            kScript.setCallingContext(ContextFactory.getKernelUser());// TODO: Make this the anon user
            handler.setScriptApi(kScript);

            ReflexExecutor.runReflexProgram(script, handler, masterParameterMap);

            System.out.println(handler.getOutput());
            String controlContent = JacksonUtil.jsonFromObject(masterParameterMap.get("control"));
            log.info("Control content is " + controlContent);
            ResponseControl ret = JacksonUtil.objectFromJson(controlContent, ResponseControl.class);
            return ret;

        } catch (ReflexException re) {
            
            System.out.println(handler.getOutput());
            log.error(ExceptionToString.format(re));
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

            ErrorWrapper ew = new ErrorWrapper();
            ew.setStatus(rapEx.getStatus());
            ew.setMessage(rapEx.getMessage());
            ew.setId(rapEx.getId());

            sendVerboseError(resp, ew);
        }
        return null;
    }

    private void sendVerboseError(HttpServletResponse resp, ErrorWrapper ew) throws IOException {
        Map<String, Object> map = JacksonUtil.getHashFromObject(ew);
        map.put("error", ew.getMessage());
        String r = JacksonUtil.jsonFromObject(map);
        //resp.setStatus(HttpURLConnection.HTTP_INTERNAL_ERROR);
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().append(r);
        resp.setContentType("text/plain");
    }

    protected String getReflexScript(String uri) {
        RaptureURI scriptURI = new RaptureURI(uri);
        log.debug(String.format("Running script for uri %s", scriptURI.toString()));
        RaptureScript script = Kernel.getScript().getScript(ContextFactory.ADMIN, scriptURI.toString());
        if (script != null) {
            return script.getScript();
        } else {
            log.warn("Could not locate script for uri - " + scriptURI.toString());
            return null;
        }
    }
}
