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

import java.net.HttpURLConnection;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import rapture.common.RaptureScript;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

/**
 * The Reflex script page servlet is kind of like jsps but for Reflex scripts
 * Here the sources of the scripts come from Rapture - with the path after the
 * servlet information simply being
 * <p/>
 * authority/scriptName
 *
 * @author alan
 */
public class ReflexRefScriptPageServlet extends BaseReflexScriptPageServlet {

    /**
     *
     */
    private static final long serialVersionUID = -1275872776877337688L;
    private static Logger log = Logger.getLogger(ReflexRefScriptPageServlet.class);

    @Override protected String getPrintableScript(HttpServletRequest req) {
        RaptureURI uri = getScriptURI(req);
        return uri.toString();
    }

    private RaptureURI getScriptURI(HttpServletRequest req) {
        String scriptName = req.getServletPath().substring(1);
        int firstPartPoint = scriptName.indexOf("/");
        int lastDecPoint = scriptName.lastIndexOf('.');
        String authority = scriptName.substring(0, firstPartPoint);
        String name = scriptName.substring(firstPartPoint + 1, lastDecPoint);
        return RaptureURI.builder(Scheme.SCRIPT, authority).docPath(name).build();
    }

    @Override
    protected String getReflexScript(HttpServletRequest req) {
        RaptureURI scriptURI = getScriptURI(req);
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
