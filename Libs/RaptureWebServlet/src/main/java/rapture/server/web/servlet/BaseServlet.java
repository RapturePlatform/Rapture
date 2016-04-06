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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.DispatchReturn;
import rapture.common.RaptureEntitlementsContext;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.kernel.Kernel;
import rapture.server.BaseDispatcher;
import external.multipart.BufferedServletInputStream;
import external.multipart.MultipartParser;
import external.multipart.ParamGetter;

/**
 * The base servlet contains the common utility classes for manipulating
 * requests and responses
 * 
 * @author alan
 * 
 */
@MultipartConfig
public class BaseServlet extends HttpServlet {
    /**
	 * 
	 */
    private static final long serialVersionUID = 488948490778665765L;

    private static final int MAXPARAMSIZE = 600000;

    private static Logger log = Logger.getLogger(BaseServlet.class);

    // Most utility code adapted from
    // http://www.javadocexamples.com/java_source/com/oreilly/servlet/multipart/MultipartParser.java.html

    /** default encoding */
    private static final String DEFAULT_ENCODING = "UTF-8";
    /** preferred encoding */
    private String encoding = DEFAULT_ENCODING;
    private byte[] buf = new byte[8 * 1024];

    protected CallingContext getSessionContext(HttpServletRequest req) {
        log.info("Retrieving context from session");
        HttpSession session = req.getSession(false);
        if (session == null) {
            log.info("No login context");
            return null;
        } else {
            log.debug("Returning session login context");
            return (CallingContext) session.getAttribute("context");
        }
    }

    protected String getFullContent(HttpServletRequest req) throws IOException {
        ServletInputStream in = req.getInputStream();
        in = new BufferedServletInputStream(in);
        StringBuilder ret = new StringBuilder();
        do {
            String line = readLine(in);
            if (line == null) {
                break;
            }
            ret.append(line);
        } while (true);
        return ret.toString();
    }

    protected Properties getParams(HttpServletRequest req) throws UnsupportedEncodingException, IOException, ServletException {
        String contentType = req.getContentType();
        Collection<Part> parts = null;
        try {
            parts = req.getParts();
        } catch (Exception e) {
            log.debug("Error parsing request parts " + e.getMessage());
        }

        if (parts != null && !parts.isEmpty()) {
            Properties props = new Properties();
            for (Part p : parts) {
                String realContent = getContentFromPart(p);
                props.put(p.getName(), realContent);
            }
            return props;
        } else {
            Properties props = null;
            if (contentType.indexOf("application/x-www-form-urlencoded") >= 0) {
                String result = URLDecoder.decode(getFullContent(req), encoding);
                props = ParamGetter.getProps(result);

            } else if (contentType.indexOf("multipart/form-data") >= 0) {
                MultipartParser parser = new MultipartParser(req, MAXPARAMSIZE);
                props = ParamGetter.getProps(parser);
            } else {
                log.error("Cannot parse contentType of " + contentType);
                throw new IOException("Bad content type of " + contentType);
            }
            return props;
        }
    }

    private String getStringFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        byte[] buffer = new byte[10240];
        for (int length = 0; (length = is.read(buffer)) > 0;) {
            bao.write(buffer, 0, length);
        }
        bao.close();
        return new String(bao.toByteArray());
    }

    protected byte[] getByteContentFromPart(Part p) throws IOException {
        InputStream inputStream = p.getInputStream();
        ByteArrayOutputStream bai = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        boolean done = false;
        while(!done) {
            int count = inputStream.read(buffer);
            if (count <= 0) {
                done = true;
            } else {
                bai.write(buffer, 0, count);
            }
        }
        bai.close();
        inputStream.close();
        byte[] ret = bai.toByteArray();
        return ret;
    }
    
    private String getContentFromPart(Part p) {
        String contentType = p.getContentType();
        InputStream inputStream = null;
        try {
            if (contentType == null || contentType.startsWith("text/plain")) {
                inputStream = p.getInputStream();
            } else if (contentType.equals("application/octet-stream")) {
                inputStream = new GZIPInputStream(p.getInputStream());
            } else {
                log.error(String.format("Unknown content type %s", contentType));
                return null;
            }
            return getStringFromInputStream(inputStream);
        } catch (IOException e) {
            log.error("Could not parse input " + ExceptionToString.format(e));
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("Error closing input stream " + ExceptionToString.format(e));
                }
            }
        }
    }

    protected StandardCallInfo processFunctionalRequest(HttpServletRequest req) throws UnsupportedEncodingException, IOException, ServletException {
        StandardCallInfo ret = new StandardCallInfo();
        Properties props = getParams(req);
        ret.setFunctionName(props.getProperty("FUNCTION"));
        ret.setContent(props.getProperty("PARAMS"));
        log.debug("Function is " + ret.getFunctionName());
        return ret;
    }

    private String readLine(ServletInputStream in) throws IOException {
        StringBuilder sbuf = new StringBuilder();
        int result;
        do {
            result = in.readLine(buf, 0, buf.length); // does +=
            if (result != -1) {
                sbuf.append(new String(buf, 0, result, encoding));
            }
        } while (result == buf.length); // loop only if the buffer was filled

        if (sbuf.length() == 0) {
            return null; // nothing read, must be at the end of stream
        }

        // Cut off the trailing \n or \r\n
        // It should always be \r\n but IE5 sometimes does just \n
        // Thanks to Luke Blaikie for helping make this work with \n
        int len = sbuf.length();
        if (len >= 2 && sbuf.charAt(len - 2) == '\r') {
            sbuf.setLength(len - 2); // cut \r\n
        } else if (len >= 1 && sbuf.charAt(len - 1) == '\n') {
            sbuf.setLength(len - 1); // cut \n
        }
        return sbuf.toString();
    }

    /**
     * Check this session is valid
     * 
     * @param context 
     * @param entPath
     * @param entCtx
     * 
     */
    protected void validateContext(CallingContext context, String entPath, RaptureEntitlementsContext entCtx) {
        // Does the context match what we know?
        Kernel.getKernel().validateContext(context, entPath, entCtx);
    }

    protected void sendResponseAppropriately(CallingContext context, HttpServletRequest req, HttpServletResponse resp, String response) throws IOException {
        String encoding = req.getHeader("Accept-Encoding");
        resp.setContentType("application/json");
        if (encoding != null && encoding.indexOf("gzip") >= 0) {
            resp.setHeader("Content-Encoding", "gzip");
            OutputStream o = null;
            GZIPOutputStream gz = null;
            try {
                o = resp.getOutputStream();
                gz = new GZIPOutputStream(o);
                gz.write(response.getBytes("UTF-8"));
            } finally {
                if (gz != null) {
                    gz.close();
                }
                if (o != null) {
                    o.close();
                }
            }
        } else {
            resp.getWriter().append(response);
            resp.flushBuffer();
        }
    }

    /**
     * This should be called when we catch an exception after calling a
     * dispatcher. (We should never get an exception in those circumstances
     * however....)
     * 
     * @param e
     * @return
     */
    protected DispatchReturn handleUnexpectedException(Exception e) {
        RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Unknown error", e);
        String response = BaseDispatcher.error(raptException);
        log.error(raptException.getFormattedMessage());
        Kernel.writeAuditEntry("exception", 2, e.getMessage());
        return new DispatchReturn(response);
    }
}
