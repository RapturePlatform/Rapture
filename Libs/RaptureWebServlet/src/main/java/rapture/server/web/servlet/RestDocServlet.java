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
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.RaptureFolderInfo;
import rapture.common.exception.RaptNotLoggedInException;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentRepoConfig;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.server.BaseDispatcher;
import rapture.server.web.servlet.rest.RestCollectionResponse;
import rapture.server.web.servlet.rest.RestFile;
import rapture.server.web.servlet.rest.RestFolder;

public class RestDocServlet extends BaseServlet {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static Logger log = Logger
            .getLogger(RestDocServlet.class);

    /**
     * A GET request basically starts with /r2/ and then routes through the
     * magic kernel folder browser. We either resolve folders or "files"
     *
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String response;
        CallingContext context = ContextFactory.getKernelUser();
        try {
            log.info("Validing context");
            context = BaseDispatcher.validateSession(req);
            if (context == null) {
                throw new RaptNotLoggedInException("Not logged in");
            }
            log.info("Context is for " + context.getUser());
            String path = req.getPathInfo();
            // We have three possible things here
            // (1) A root request - returns a list of document repositories in
            // folder form
            // (2) A folder request within a document repository - returns a
            // list of folders/file names
            // (3) A document request - returns the content of that document

            if (path == null || (path.length() == 1 && path.startsWith("/"))) {
                response = processRootRequest(context);
            } else {
                response = processNonRootRequest(context, path);
            }

        } catch (RaptureException e) {
            response = BaseDispatcher.error(e);
        } catch (Exception e) {
            response = BaseDispatcher.error(RaptureExceptionFactory.create(
                    HttpURLConnection.HTTP_INTERNAL_ERROR,
                    "Could not handle rest request.", e));
        }
        sendResponseAppropriately(context, req, resp, response);
    }

    private String processRootRequest(CallingContext context) {
        RestCollectionResponse response = new RestCollectionResponse();
        response.setPath("/");
        List<DocumentRepoConfig> repos = Kernel.getDoc()
                .getDocRepoConfigs(context);
        for (DocumentRepoConfig repo : repos) {
            RestFolder f = new RestFolder();
            f.setPath("/" + repo.getAuthority());
            f.setName(repo.getAuthority());
            response.getFolders().add(f);
        }
        return JacksonUtil.jsonFromObject(response);
    }

    private String processNonRootRequest(CallingContext context, String path) {
        // Should be a doc uri really
        DateFormat df = DateFormat.getDateTimeInstance();
        if (path.charAt(0) == '/' && path.charAt(1) != '/') {
            path = "/" + path;
        }
        if (path.endsWith("/")) {
            RestCollectionResponse response = handleFolder(context, path, df);
            return JacksonUtil.jsonFromObject(response);
        } else {

            String content = Kernel.getDoc().getDoc(context, path);
            if (content == null) {
                RestCollectionResponse response = handleFolder(context, path,
                        df);
                return JacksonUtil.jsonFromObject(response);

            } else {
                return content;
            }

        }
    }

    private RestCollectionResponse handleFolder(CallingContext context,
                                                String path, DateFormat df) {

        Map<String, RaptureFolderInfo> contentMap = Kernel.getDoc().listDocsByUriPrefix(context, path, 1);
        RestCollectionResponse response = new RestCollectionResponse();
        response.setPath(path);
        for (RaptureFolderInfo f : contentMap.values()) {
            if (f.isFolder()) {
                RestFolder fold = new RestFolder();
                fold.setPath(path + (path.endsWith("/") ? "" : "/") + f.getName() + "/");
                fold.setName(f.getName());
                response.getFolders().add(fold);
            } else {
                RestFile file = new RestFile();
                file.setPath(path + (path.endsWith("/") ? "" : "/") + f.getName());
                file.setName(f.getName());
                DocumentMetadata meta = Kernel.getDoc().getDocMeta(context,
                        file.getPath());
                if (meta != null) {
                    file.setLastUser(meta.getUser());
                    file.setLastVersion(meta.getVersion());
                    file.setLastWriteTime(df.format(new Date(meta.getModifiedTimestamp())));

                }
                response.getFiles().add(file);
            }

        }
        return response;
    }
}
