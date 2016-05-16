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
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.ErrorWrapper;
import rapture.common.RaptureScript;
import rapture.common.RaptureURI;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.ContextResponseData;
import rapture.kernel.Kernel;
import rapture.kernel.script.KernelScript;
import reflex.ReflexException;
import reflex.ReflexExecutor;

/**
 * The User bounded servlet runs Reflex scripts with (or 'bound' to) a specified user.
 * 
 * This servlet take two web.xml params:
 * 1. user    An existing Rapture user setup with appropriate entitlements 
 * 2. prefix  A defined location where only this servlet can run scripts
 * 
 * @author jonathan-major
 *
 */
public class UserBoundedScriptPageServlet extends BaseServlet {

    private static CallingContext DEFINEDUSER = null;
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(UserBoundedScriptPageServlet.class);

    private String scriptPrefix;
    private String user;
     

    /**
     * Initializes the user bound servlet.
     * <p>
     * Loads config from web.xml and creates a CallingContext for user that the servlet is bound to.  
     *
     */
    @Override
    public void init() throws ServletException {
        log.debug("Starting UserBoundedScriptPageServlet");

        scriptPrefix = getServletConfig().getInitParameter("prefix");
        if (scriptPrefix == null) {
            throw new ServletException("No prefix parameter");
        }
        user = getServletConfig().getInitParameter("user");
        if (user == null) {
            throw new ServletException("No user parameter");
        }
        
        //Create a context for the existing user. If the user doesn't exist getContextForUser throws a RaptureException
        try{
            ContextResponseData contextForUser = Kernel.getLogin().getContextForUser(user);
            DEFINEDUSER = new CallingContext();
            DEFINEDUSER.setContext(contextForUser.getContextId());
            DEFINEDUSER.setUser(user);
            DEFINEDUSER.setValid(true);
        } catch (RaptureException re) {
            throw new ServletException("Error looking up user. " + re.getMessage());
        }
    }
    
    /**
     * POST method implementation.
     * <p>
     * Extract the Reflex URI and parameters out of request and run it using the Calling Context of user.  
     *
     * @param  req  The HttpServletRequest
     * @param  resp The HttpServletResponse      
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        
        Map<String, Object> requestVariables = getRequestVariables(req);
        
        log.debug("req is " + req);
        log.debug("resp is " + resp);
        log.debug("request variables " + requestVariables);
        
        //check if there are req variables
        if(Integer.valueOf(req.getHeader("content-length")) == 0){
            log.debug("Request has no parameters.");
            resp.sendError(HttpStatus.SC_BAD_REQUEST, "Request has no parameters");
            return;
        }
        
        String scriptURL = getScriptUrlFromHttpRequest(req);
        if (scriptURL == null) {
            resp.sendError(HttpStatus.SC_BAD_REQUEST, "Request is missing path information.");
            return;
        }
        
        String script = getReflexScript(scriptURL);
        if (script == null || script.isEmpty()) {
            resp.sendError(HttpStatus.SC_NOT_FOUND, "Service " + scriptURL + " has no endpoint");
            return;
        }
        
        Map<String, Object> props = getParams(req);
        Map<String, Object> parameterMap = new HashMap<>();
        for (String key : props.keySet()) {
            Object val = props.get(key);
            if (val instanceof String) {
                val = URLDecoder.decode((String) val, "UTF-8");
            }
            parameterMap.put(key, val);
        }
        
        runScriptWithUser(script, parameterMap, resp);
        
        log.debug("Finished running script: " + scriptURL);
        
        resp.flushBuffer();
    }
    

    /**
     * GET method implementation.
     * <p>
     * Extract the Reflex URI and parameters out of request and run it using the Calling Context of user.  
     *
     * @param  req  The HttpServletRequest
     * @param  resp The HttpServletResponse      
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        
        String scriptURL = getScriptUrlFromHttpRequest(req);
        if (scriptURL == null) {
            resp.sendError(HttpStatus.SC_BAD_REQUEST, "Request is missing path information.");
            return;
        }
        
        String script = getReflexScript(scriptURL);
        if (script == null || script.isEmpty()) {
            resp.sendError(HttpStatus.SC_NOT_FOUND, "Service " + scriptURL + " has no endpoint");
            return;
        }

        Map<String, Object> parameterMap = new HashMap<String, Object>();

        Enumeration<String> e = req.getParameterNames();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            String val = req.getParameter(key);
            parameterMap.put(key, val);
        }
        
        runScriptWithUser(script, parameterMap, resp);
       
        log.debug("Finished running script: " + scriptURL);
      
        resp.flushBuffer();
    }

    /**
     * Process the request that came either by POST or GET method
     * <p>
     * @param  script        The actual script/endpoint to execute  
     * @param  parameterMap  Parameters extracted from request and injected into the script 
     * @param  resp          The HttpServletResponse
     */
    protected void runScriptWithUser(String script, Map<String, Object> parameterMap, HttpServletResponse resp) throws IOException {

        Map<String, Object> masterParameterMap = new HashMap<String, Object>();
        masterParameterMap.put("web", parameterMap);
        // Now run script
        ReflexScriptPageHandler handler = new ReflexScriptPageHandler();
        KernelScript kScript = new KernelScript();

        try {
            kScript.setCallingContext(DEFINEDUSER);
            handler.setScriptApi(kScript);
            
            ReflexExecutor.runReflexProgram(script, handler, masterParameterMap);
            
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().append(handler.getOutput());
            resp.setContentType("text/plain");
            resp.setStatus(HttpStatus.SC_OK);
            
        } catch (ReflexException re) {
            
            log.error(handler.getOutput());
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
                    rapEx = RaptureExceptionFactory.create("Error calling endpoint: " + re.getMessage(), re);
                } else {
                    rapEx = RaptureExceptionFactory.create("Error calling endpoint", re);
                }
            }

            ErrorWrapper ew = new ErrorWrapper();
            ew.setStatus(rapEx.getStatus());
            ew.setMessage(rapEx.getMessage());
            ew.setId(rapEx.getId());

            sendVerboseError(resp, ew);
        }
        resp.flushBuffer();
    }

    /**
     * Helper method to extract the URI from HttpServletRequest
     * <p>
     * @param  request  The actual script/endpoint to execute  
     * @return          URI of script as a String e.g. script://<repo>/endpoint 
     */
    private String getScriptUrlFromHttpRequest(HttpServletRequest request){
        
        String path = request.getPathInfo();
        if(path == null){ //getPathInfo() returns null if there was no extra path information. 
            return null;
        }
        log.debug("Path is " + path);
        String[] parts = path.split("/");
        
        String scriptName = parts[parts.length - 1];
        log.debug(scriptName);
        
        String scriptURL = scriptPrefix + scriptName;
        log.debug("Script URL is " + scriptURL);
        
        return scriptURL;
    }
    
    
    /**
     * Helper method to add 
     * <p>
     * @param  resp  The HttpServletResponse response object
     * @param  ew    Error object 
     */
    private void sendVerboseError(HttpServletResponse resp, ErrorWrapper ew) throws IOException {
        Map<String, Object> map = JacksonUtil.getHashFromObject(ew);
        map.put("error", ew.getMessage());
        String r = JacksonUtil.jsonFromObject(map);
        resp.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().append(r);
        resp.setContentType("text/plain");
    }

    /**
     * Helper method to get the script/endpoint from a script:// repo
     * <p>
     * @param  uri  The actual script/endpoint to execute  
     * @return      Script file 
     */
    protected String getReflexScript(String uri) {
        RaptureURI scriptURI = new RaptureURI(uri);
        RaptureScript script = Kernel.getScript().getScript(DEFINEDUSER, scriptURI.toString());
        if (script != null) {
            return script.getScript();
        } else {
            log.warn("Could not locate script for uri - " + scriptURI.toString());
            return null;
        }
    }
    
    /**
     * Helper methods to get header info out of a HttpServletRequest request object
     * <p>
     * @param  req  The HttpServletRequest request object
     * @return      Map of HTTP header 
     */
    private Map<String, Object> getRequestVariables(HttpServletRequest req) {
        Map<String, Object> serverMap = new HashMap<String, Object>();
        serverMap.put("ContentType", req.getContentType());
        serverMap.put("ContextPath", req.getContextPath());
        serverMap.put("Method", req.getMethod());
        serverMap.put("PathInfo", req.getPathInfo());
        serverMap.put("RemoteAddr", req.getRemoteAddr());
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
