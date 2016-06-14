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
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import rapture.jmx.JmxApp;
import rapture.jmx.JmxAppCache;

/**
 * This servlet acts as a proxy to the individual JMX servers in the rapture cluster. All you need is the unique app identifier name@host:port, to hit that
 * app's direct JMX http server. Example URL is http://curtis:8080/jmx/CurtisWebServer@192.168.99.1:23455/read/java.lang:type=Memory
 * 
 * @author dukenguyen
 *
 */
@WebServlet(urlPatterns = "/jmx")
public class JmxServlet extends BaseServlet {

    private static final long serialVersionUID = -5651493694360781626L;

    private static final Logger log = Logger.getLogger(JmxServlet.class);

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (StringUtils.isNotBlank(path)) {
            path = stripLeadingSlash(path);
            int index = path.indexOf('/');
            if (index == -1) {
                error(res, "Invalid URL path provided");
                return;
            }
            String appName = path.substring(0, index);
            path = path.substring(index);
            JmxApp app;
            try {
                app = JmxAppCache.getInstance().get().get(appName);
            } catch (ExecutionException e) {
                error(res, e.getMessage());
                return;
            }
            if (app == null) {
                error(res, String.format("App [%s] not found", appName));
                return;
            }
            try (InputStream input = new URL(app.getUrl() + path).openStream();
                    OutputStream output = res.getOutputStream();) {
                IOUtils.copy(input, output);
            }
        } else {
            error(res, path + " is unavailable");
        }
    }

    private void error(HttpServletResponse res, String msg) throws IOException {
        log.error("Error forwarding jmx request: " + msg);
        res.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
    }

    private String stripLeadingSlash(String path) {
        return path.trim().substring(1);
    }

}