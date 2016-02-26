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
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.DispatchReturn;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureScript;
import rapture.common.exception.RaptNotLoggedInException;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.Kernel;
import rapture.kernel.script.KernelScript;
import rapture.server.BaseDispatcher;
import rapture.server.web.servlet.micro.MicroService;
import rapture.server.web.servlet.micro.MicroServiceException;
import rapture.server.web.servlet.micro.MicroServiceParam;
import reflex.ReflexExecutor;

/**
 * The microservice servlet accepts a configuration parameter which defines a series of documents in a hierarchy that define microservices. We have the means to
 * discover services and to execute services through this servlet.
 *
 * @author alan
 */
public class MicroServiceServlet extends BaseServlet {

    /**
     *
     */
    private static final long serialVersionUID = -1275872776877337688L;
    private static Logger log = Logger.getLogger(MicroServiceServlet.class);

    private String serviceDefLocation;

    @Override
    public void init() throws ServletException {

        serviceDefLocation = getServletConfig().getInitParameter("service");
        if (serviceDefLocation == null) {
            throw new ServletException("No service parameter");
        } else {
            if (serviceDefLocation.endsWith("/")) {
                serviceDefLocation = serviceDefLocation.substring(0, serviceDefLocation.length() - 1);
            }
        }

    }


    private CallingContext getCallingContext(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CallingContext callingContext = null;
        try {
            callingContext = BaseDispatcher.validateSession(req);
        } catch (RaptNotLoggedInException re) {
            log.error(re.getMessage());
            resp.sendError(re.getStatus(), re.getMessage());
        }

        if (callingContext == null || !callingContext.getValid()) {
            log.error("Calling Context is not valid");
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Must be logged in");
            return null;
        }
        return callingContext;
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Properties props = getParams(req);
        Map<String, Object> parameterMap = new HashMap<String, Object>();

        Enumeration<Object> e = props.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement().toString();
            String val = props.getProperty(key);
            parameterMap.put(key, val);
        }

        DispatchReturn response;

        try {
            CallingContext context = getCallingContext(req, resp);
            if (context != null) {
                process(context, req, resp, parameterMap);
            }
        } catch (Exception ex) {
            response = handleUnexpectedException(ex);
            sendResponseAppropriately(response.getContext(), req, resp, response.getResponse());
        }
    }
    
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {

        Map<String, Object> parameterMap = new HashMap<String, Object>();

        Enumeration<String> e = req.getParameterNames();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            String val = req.getParameter(key);
            parameterMap.put(key, val);
        }

        DispatchReturn response;

        try {
            CallingContext context = getCallingContext(req, resp);
            if (context != null) {
                process(context, req, resp, parameterMap);
            }
            // Basically either we are in getChildren style mode, particularly if the request does not correspond to a document. If so - return
            // the children at this point
            // If we are at a document point we have two modes. One, with the parameter ?info set, implies that we should just return the service
            // information (the document)
            // Otherwise we are executing the service, so we need to get and validate (coerce) the parameters, call the script, and then process and return the
            // return value
        } catch (Exception ex){
            response = handleUnexpectedException(ex);
            sendResponseAppropriately(response.getContext(), req, resp, response.getResponse());
        }

    }

    private void process(CallingContext context, HttpServletRequest req, HttpServletResponse resp, Map<String, Object> parameterMap) throws IOException {
        try {
            if (parameterMap.containsKey("info")) {
                processInfoRequest(context, req, resp);
            } else {
                // Execute a service request if this maps to a service
                processExecRequest(context, req, resp, parameterMap);
            }
        } catch (MicroServiceException e) {
            e.sendResponse(resp);
        }
    }

    private void processExecRequest(CallingContext context, HttpServletRequest req, HttpServletResponse resp, Map<String, Object> parameterMap)
            throws IOException, MicroServiceException {
        String uri = serviceDefLocation + req.getPathInfo();
        String content = Kernel.getDoc().getDoc(context, uri);
        if (content == null) {
            throw new MicroServiceException(HttpServletResponse.SC_NOT_FOUND, "No micro service");
        }
        MicroService service = JacksonUtil.objectFromJson(content, MicroService.class);
        if (service == null) {
            throw new MicroServiceException(HttpServletResponse.SC_BAD_REQUEST, "Invalid micro service");
        }
        execService(service, context, req, resp, parameterMap);
    }

    private void execService(MicroService service, CallingContext context, HttpServletRequest req, HttpServletResponse resp, Map<String, Object> parameterMap)
            throws MicroServiceException, IOException {
        // The service has an implied definition of parameters that need to be coerced according to the service definition
        Map<String, Object> coercedParameters = getCoercedParameters(service, parameterMap);
        // Now run script
        ReflexScriptPageHandler handler = new ReflexScriptPageHandler();
        KernelScript kScript = new KernelScript();

        kScript.setCallingContext(context);
        handler.setScriptApi(kScript);

        RaptureScript s = Kernel.getScript().getScript(context, service.getService());
        if (s == null) {
            throw new MicroServiceException(HttpServletResponse.SC_BAD_REQUEST, "Script " + service.getService() + " not found");
        }
        Object ret = ReflexExecutor.runReflexProgram(s.getScript(), handler, coercedParameters);
        resp.setCharacterEncoding("UTF-8");
        switch (service.getReturns().getFrom()) {
            case "output":
                resp.getWriter().append(handler.getOutput());
                break;
            case "return":
                resp.getWriter().append(ret.toString());
                break;
            default:
                throw new MicroServiceException(HttpServletResponse.SC_BAD_REQUEST, "Unknown return type " + service.getReturns().getFrom());
        }
        resp.setContentType(service.getReturns().getType());

    }

    private Map<String, Object> getCoercedParameters(MicroService service, Map<String, Object> parameterMap) throws MicroServiceException {
        Map<String, Object> ret = new HashMap<String, Object>();
        for (MicroServiceParam p : service.getParams()) {
            if (parameterMap.containsKey(p.getName())) {
                ret.put(p.getName(), coerceValue(parameterMap.get(p.getName()), p.getType()));
            } else {
                throw new MicroServiceException(HttpServletResponse.SC_BAD_REQUEST, "Parameter " + p.getName() + " not found");
            }
        }
        return ret;
    }

    private Object coerceValue(Object object, String type) throws MicroServiceException {
        switch (type) {
            case "text":
                return object.toString();
            case "number":
                try {
                    Integer v = Integer.parseInt(object.toString());
                    return v;
                } catch (NumberFormatException e) {
                    try {
                        Double v = Double.parseDouble(object.toString());
                        return v;
                    } catch (NumberFormatException e2) {
                        throw new MicroServiceException(HttpServletResponse.SC_BAD_REQUEST, "Value " + object.toString() + " cannot be coerced to a number");
                    }
                }
            default:
                throw new MicroServiceException(HttpServletResponse.SC_BAD_REQUEST, "Unknown parameter type " + type);
        }
    }

    private void processInfoRequest(CallingContext context, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // This is a request for information - either a service list discovery or information about a service
        // PathInfo is the relative path of the request - prepend serviceDefLocation onto that
        String uri = serviceDefLocation + req.getPathInfo();
        String content = Kernel.getDoc().getDoc(context, uri);
        if (content != null) {
            resp.getOutputStream().write(content.getBytes());
        } else {
            Map<String, RaptureFolderInfo> infoMap = Kernel.getDoc().listDocsByUriPrefix(context, uri, 1);
            List<RaptureFolderInfo> info = new ArrayList<RaptureFolderInfo>();
            for(RaptureFolderInfo rfi : infoMap.values()){
                info.add(rfi);
            }
            resp.getOutputStream().write(JacksonUtil.jsonFromObject(info).getBytes());
        }
        resp.setContentType("application/json");
        resp.flushBuffer();
    }

}
