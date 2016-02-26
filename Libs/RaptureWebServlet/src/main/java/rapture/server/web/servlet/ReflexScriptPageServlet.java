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
import java.io.InputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.exception.ExceptionToString;
import rapture.util.ResourceLoader;

/**
 * The Reflex script page servlet is kind of like jsps but for Reflex scripts (Files with extension rfx)
 * 
 * @author alan
 */
public class ReflexScriptPageServlet extends BaseReflexScriptPageServlet {
    private static final long serialVersionUID = 1713902342313464871L;

    private static Logger log = Logger.getLogger(ReflexScriptPageServlet.class);

    private static final int MAX_PRINTABLE_SIZE = 50;

    @Override
    protected String getPrintableScript(HttpServletRequest req) {
        String scriptBody = getReflexScript(req);
        if (StringUtils.isEmpty(scriptBody)) {
            return "<script not found>";
        } else {
            if (scriptBody.length() > MAX_PRINTABLE_SIZE) {
                return scriptBody.substring(0, MAX_PRINTABLE_SIZE);
            } else {
                return scriptBody;
            }
        }
    }

    @Override
    protected String getReflexScript(HttpServletRequest req) {
        ServletContext context = getServletContext();
        String result = null;
        try (InputStream is = context.getResourceAsStream(req.getServletPath())) {
            if (is != null) {
                result = ResourceLoader.getResourceFromInputStream(is);
            }
        } catch (IOException e) {
            log.error(ExceptionToString.format(e));
        }
        return result;
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }
}
