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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import rapture.common.BlobContainer;
import rapture.common.CallingContext;
import rapture.common.ContentEnvelope;
import rapture.common.exception.RaptNotLoggedInException;
import rapture.kernel.BlobApiImplWrapper;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.server.BaseDispatcher;

@MultipartConfig
public class BlobContentServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final String BLOB_URI_PREFIX = "blob://";
    private static Logger log = Logger.getLogger(BlobContentServlet.class);
    private String blobRepo;
    private boolean checkCredentials = false;
    private String redirect = null;
    private List<String> templateMimeTypes = new ArrayList<String>();
    private String pageTemplate = null;

    @Override
    public void init() throws ServletException {
        log.info("INITIALIZING....");

        blobRepo = getServletConfig().getInitParameter("repo");
        if (blobRepo == null) {
            throw new ServletException("No folder parameter");
        }
        String checkCredsString = getServletConfig().getInitParameter("check");
        if (checkCredsString != null) {
            checkCredentials = Boolean.valueOf(checkCredsString);
        }
        redirect = getServletConfig().getInitParameter("redirectOnAuthFail");
        String templateCheck = getServletConfig().getInitParameter("templates");
        if (templateCheck != null) {
            String[] parts = templateCheck.split(",");
            for (String p : parts) {
                templateMimeTypes.add(p);
            }
        }
        pageTemplate = getServletConfig().getInitParameter("pageTemplate");
    }

    /**
     * A get here is used to serve content back to the browser. The mime type is set from the blob A given install of this servlet has the blob to use as a
     * configuration option.
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String blobPath = req.getRequestURI();
        if (blobPath.equals("/")) {
            blobPath = "/index.html"; // Or "welcome page" init parameter
        }

        CallingContext callingContext = null;
        if (checkCredentials) {
            try {
                callingContext = BaseDispatcher.validateSession(req);
            } catch (RaptNotLoggedInException re) {
                log.error(re.getMessage());
                if (redirect != null) {
                    resp.sendRedirect(redirect);
                } else {
                    resp.sendError(re.getStatus(), re.getMessage());
                }
                return;
            }

            if (callingContext == null || !callingContext.getValid()) {
                log.error("Calling Context is not valid");
                if (redirect != null) {
                    resp.sendRedirect(redirect);
                } else {
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Must be logged in");
                }
                return;
            }
        } else {
            callingContext = ContextFactory.getKernelUser();
        }
        if(blobPath.startsWith("/t/app/")) {
            streamTemplatedBlob(resp, blobPath.substring(2), callingContext);
        } else {
            streamBlob(resp, blobPath, callingContext, true);
        }
    }

    private void streamBlob(HttpServletResponse resp, String blobPath, CallingContext callingContext, boolean outer) throws IOException {
        BlobApiImplWrapper blob = Kernel.getBlob();
        String uri = BLOB_URI_PREFIX + blobRepo + blobPath;
        BlobContainer blobContainer = blob.getBlob(callingContext, uri);
        if (blobContainer != null) {
            boolean performTemplating = false;
            if (blobContainer.getHeaders() != null) {
                String contentType = blobContainer.getHeaders().get(ContentEnvelope.CONTENT_TYPE_HEADER);
                if (contentType != null) {
                    if (outer) {
                        resp.setContentType(contentType);
                    }
                    if (templateMimeTypes.contains(contentType)) {
                        performTemplating = true;
                    }
                }
            }
            if (outer) {
                resp.setStatus(HttpServletResponse.SC_OK);
            }
            if (performTemplating) {
                templateParse(callingContext, resp, blobContainer.getContent());
            } else {
                resp.getOutputStream().write(blobContainer.getContent());
            }
        } else {
            log.error("No blob at " + uri);
            if (outer) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    private void streamTemplatedBlob(HttpServletResponse resp, String blobPath, CallingContext callingContext) throws IOException {
        String uri = BLOB_URI_PREFIX + blobRepo + pageTemplate;
        BlobContainer blobContainer = Kernel.getBlob().getBlob(callingContext, uri);
        if(blobContainer != null) {
            String content = new String(blobContainer.getContent());
            content = content.replace("/path/to/main/content", blobPath);
            resp.getOutputStream().write(content.getBytes());
        } else {
            log.error("No blob at " + uri);
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void templateParse(CallingContext callingContext, HttpServletResponse resp, byte[] content) throws IOException {
        // The content is something that may contain {{ [uri] }}
        // uri can either be script:// (output of executing script is inserted into content)
        // or just a path, which is rooted to the blobcontainer (BLOB_URI_PREFIX + blobRepo + thispath)
        // if this blob has the same mimetype, it is inserted.

        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content)));
        String line;
        while ((line = reader.readLine()) != null) {
            boolean noMore = false;
            int point = 0;
            while (!noMore) {
                int startPoint = line.indexOf("{{{", point);
                if (startPoint == -1) {
                    noMore = true;
                } else {
                    log.info("Template at position " + startPoint + " on line " + line.substring(point));
                    resp.getOutputStream().write(line.substring(point, startPoint).getBytes());
                    point = startPoint;
                    int endPoint = line.indexOf("}}}", point);
                    if (endPoint != 1) {
                        String inner = line.substring(point + 2, endPoint).trim();
                        log.info("Inner template is " + inner);
                        point = endPoint + 2;
                        streamBlob(resp, inner, callingContext, false);

                    } else {
                        log.info("No endpoint for startpoint");
                    }
                }
            }
            resp.getOutputStream().write(line.substring(point).getBytes());
        }
        reader.close();
    }

}
