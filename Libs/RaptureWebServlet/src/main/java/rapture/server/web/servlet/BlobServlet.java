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

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import rapture.common.BlobContainer;
import rapture.common.CallingContext;
import rapture.common.ContentEnvelope;
import rapture.common.DispatchReturn;
import rapture.common.exception.RaptNotLoggedInException;
import rapture.common.shared.blob.DispatchBlobFunction;
import rapture.kernel.BlobApiImplWrapper;
import rapture.kernel.Kernel;
import rapture.server.BaseDispatcher;

@WebServlet(urlPatterns = { "/blob", "/blob/*" })
@MultipartConfig
public class BlobServlet extends BaseServlet {

    private static final long serialVersionUID = -654951209810427391L;
    private static final String BLOB_URI_PREFIX = "blob:/";
    private static Logger log = Logger.getLogger(BlobServlet.class);

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        StandardCallInfo call = processFunctionalRequest(req);
        DispatchBlobFunction blobDispatch = DispatchBlobFunction.valueOf(call.getFunctionName());
        DispatchReturn response;
        try {
            response = blobDispatch.executeDispatch(call.getContent(), req, resp);
        } catch (Exception e) {
            response = handleUnexpectedException(e);
        }
       sendResponseAppropriately(response.getContext(), req, resp, response.getResponse());

    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String blobPath = req.getPathInfo();

        CallingContext callingContext = null;
        try {
            callingContext = BaseDispatcher.validateSession(req);
        } catch (RaptNotLoggedInException re) {
            log.error(re.getMessage());
            resp.sendError(re.getStatus(), re.getMessage());
        }

        if (!callingContext.getValid()) {
            log.error("Calling Context is not valid");
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Must be logged in");
        }

        BlobApiImplWrapper blob = Kernel.getBlob();
        BlobContainer blobContainer = blob.getBlob(callingContext, BLOB_URI_PREFIX + blobPath);

        if (blobContainer != null) {
            if (blobContainer.getHeaders() != null) {
                String contentType = blobContainer.getHeaders().get(ContentEnvelope.CONTENT_TYPE_HEADER);
                if (contentType != null) {
                    resp.setContentType(contentType);
                }
            }
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getOutputStream().write(blobContainer.getContent());
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

}
