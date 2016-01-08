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
import javax.servlet.http.Part;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.exception.RaptNotLoggedInException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.kernel.Kernel;
import rapture.server.BaseDispatcher;
import rapture.util.MimeTypeResolver;

@WebServlet("/blobupload")
@MultipartConfig
public class BlobUploadServlet extends BaseServlet {
    private static final long serialVersionUID = 7598856450379496544L;
    private static Logger log = Logger.getLogger(BlobUploadServlet.class);
    private static MimeTypeResolver resolver = new MimeTypeResolver();
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        CallingContext context = null;
        try {
            context = BaseDispatcher.validateSession(request);
        } catch (RaptNotLoggedInException re) {
            log.error(re.getMessage());
            response.sendError(re.getStatus(), re.getMessage());
        }

        if (!context.getValid()) {
            log.error("Calling Context is not valid");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Must be logged in");
        }

        String description = request.getParameter("description"); // Retrieves <input type="text" name="description">
        // So the description is a uri. Parse it as such
        RaptureURI uri = new RaptureURI(description, Scheme.BLOB);
        Part filePart = request.getPart("file"); // Retrieves <input type="file" name="file">
        switch(uri.getScheme()) {
        case BLOB:
            storeBlob(context, filePart, uri, description);
            break;
        case SCRIPT:
            storeScript(context, filePart, uri, description);
            break;
        case DOCUMENT:
            storeDocument(context, filePart, uri, description);
            break;
        default:
            throw RaptureExceptionFactory.create("Could not work with that scheme");
        }
 
        log.info("URI is " + description);
        response.sendRedirect(request.getHeader("referer"));
    }

    private void storeDocument(CallingContext context, Part filePart, RaptureURI uri, String description) throws IOException {
        String shortPath = uri.getShortPath();
        byte[] data = getByteContentFromPart(filePart);
        String content = new String(data);
        Kernel.getDoc().putDoc(context, shortPath, content);
    }

    private void storeScript(CallingContext context, Part filePart, RaptureURI uri, String description) throws IOException {
        String shortPath = uri.getShortPath();
        log.info("Attempting to store script at " + shortPath);
        byte[] data = getByteContentFromPart(filePart);
        String script = new String(data);
        Kernel.getScript().createScript(context, shortPath, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, script);
    }

    private void storeBlob(CallingContext context, Part filePart, RaptureURI uri, String description) throws IOException {
       String shortPath = uri.getShortPath();
       byte[] data = getByteContentFromPart(filePart);
       log.info("Size of data is " + data.length);
       String fileName = getFilename(filePart);
       String mimeType = resolver.getMimeType(resolver.getExtensionFromPath(fileName));
       Kernel.getBlob().putBlob(context, shortPath, data, mimeType);      
    }
   
    private static String getFilename(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                return filename.substring(filename.lastIndexOf('/') + 1).substring(filename.lastIndexOf('\\') + 1); // MSIE fix.
            }
        }
        return null;
    }
}
