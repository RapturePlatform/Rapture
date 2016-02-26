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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

import rapture.common.exception.RaptureException;
import rapture.kernel.Kernel;
import rapture.util.IDGenerator;
import rapture.config.ConfigLoader;

@MultipartConfig
public class RaptureInitServlet extends HttpServlet {
    /**
	 * 
	 */
    private static final long serialVersionUID = 8634699628525509460L;
    private static Logger log = Logger.getLogger(RaptureInitServlet.class);

    @Override
    public void init(ServletConfig config) {
        log.info("Starting RaptureServer through Servlet");
        log.info("======================================");
        // Need to start the bootstrap repo and launch it into the kernel
        // RaptureKernel.runAdditional(null);

        try {
            Map<String, String> templates = new HashMap<String, String>();
            templates.put("STD", ConfigLoader.getConf().StandardTemplate);
            Kernel.initBootstrap(templates, RaptureInitServlet.class, false);
            Kernel.getKernel().setAppStyle("webapp");
            Kernel.getKernel().setAppId(IDGenerator.getUUID());
        } catch (RaptureException e1) {
            log.error("Failed to start Rapture with " + e1.getMessage());
        }
    }
}
