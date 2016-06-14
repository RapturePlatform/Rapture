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
package rapture.server;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.DispatchReturn;
import rapture.common.ErrorWrapper;
import rapture.common.ErrorWrapperFactory;
import rapture.common.RaptureEntitlementsContext;
import rapture.common.exception.RaptNotLoggedInException;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.BasePayload;
import rapture.common.model.GeneralResponse;
import rapture.kernel.Kernel;
import rapture.kernel.stat.StatHelper;

public abstract class BaseDispatcher {
    private static Logger log = Logger.getLogger(BaseDispatcher.class);

    public static String error(RaptureException raptException) {
        log.error("ERROR WHEN SERVICING API CALL");
        try {
            log.error(raptException.getFormattedMessage());
            ErrorWrapper ew = ErrorWrapperFactory.create(raptException);
            return JacksonUtil.jsonFromObject(new GeneralResponse(ew, true));
        } catch (Exception e1) {
            log.error("Fatal server error - ", e1);
            return String.format("{\"response\":{\"id\":\"null\",\"status\":500,\"message\":\"Fatal server error - %s\"},\"inError\":true,\"success\":false}",
                    e1.getMessage());
        }
    }

    public abstract DispatchReturn dispatch(String params, HttpServletRequest req, HttpServletResponse resp);

    public static String getContextIdFromRequest(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null || cookies.length == 0) {
            //log.info("No cookies sent");
        } else {
            for (Cookie c : cookies) {
                if (c.getName().equals("raptureContext")) {
                    log.debug("RaptureContext is " + c.getValue());
                    return c.getValue();
                }
            }
        }
        // Maybe it's in an id
        String header = req.getHeader("x-rapture");
        return header;
    }

    public static CallingContext validateSession(HttpServletRequest req, BasePayload payload) {
        CallingContext validatedContext = null;
        if (payload != null && payload.getContext() != null) {
            validatedContext = Kernel.getKernel().loadContext(payload.getContext().getContext());

            if (validatedContext == null) {
                log.warn("Bad Context ID: " + payload.getContext().getContext());
                CallingContext heldContext = validateSession(req);
                return heldContext;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("validateSession lookup returns " + validatedContext.debug());
                }
                return validatedContext;
            }
        } else {
            CallingContext heldContext = validateSession(req);
            log.debug("validateSession direct returns " + heldContext.debug());
            return heldContext;
        }
    }

    public static CallingContext validateSession(HttpServletRequest req) {
        // Will throw new RaptNotLoggedInException if the session isn't valid
        log.debug("Validating request");
        String context = getContextIdFromRequest(req);
        if (context == null) {
        	// FINAL CHECK - do with have an X-RAPTURE-APIKEY and X-RAPTURE-APPID
        	// If we do, attempt to load up an APIKeyDefinition from these, if correct,
        	// set up a calling context from that.
        	String appid = req.getHeader("x-rapture-appId");
        	String apikey = req.getHeader("x-rapture-apikey");
        	CallingContext ctx = Kernel.getKernel().loadContext(appid, apikey);
        	if (ctx == null) {
        		throw new RaptNotLoggedInException("Not logged in");
        	} else {
        		return ctx;
        	}
        }
        // Load context
        CallingContext heldContext = Kernel.getKernel().loadContext(context);
        if (heldContext == null) {
            throw new RaptNotLoggedInException("Invalid context");
        }
        return heldContext;
    }

    private RaptureEntitlementsContext getEntitlementsContext(BasePayload payload) {
        return new RaptureEntitlementsContext(payload);
    }

    protected void preHandlePayload(CallingContext context, BasePayload payload, String path) {
        validateContext(context, payload, path);
        recordUsageStats(payload);
    }

    protected String processResponse(GeneralResponse generalResponse) {
        String ret = JacksonUtil.jsonFromObject(generalResponse);
        try {
            StatHelper statApi = Kernel.getKernel().getStat();
            statApi.registerApiThroughput((long) ret.length());
        } catch (Exception e) {

        }
        return ret;
    }

    private static void recordUsageStats(BasePayload payload) {
        try {
            StatHelper statApi = Kernel.getKernel().getStat();
            statApi.registerUser(payload.getContext().getUser());
            statApi.registerApiCall();
        } catch (Exception e) {

        }
    }

    private void validateContext(CallingContext context, BasePayload payload, String path) {
        Kernel.getKernel().validateContext(context, path, getEntitlementsContext(payload));
    }
}
