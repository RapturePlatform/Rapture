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
package rapture.server.rest;

import static rapture.server.rest.JsonUtil.json;
import static spark.Spark.before;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.put;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableMap;

import rapture.app.RaptureAppService;
import rapture.common.CallingContext;
import rapture.common.exception.RaptureException;
import rapture.config.ConfigLoader;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import spark.Request;

public class RestServer {

    private static final Logger log = Logger.getLogger(RestServer.class);

    private Map<String, CallingContext> ctxs = new HashMap<>();

    public static void main(String[] args) {
        try {
            Kernel.initBootstrap(ImmutableMap.of("STD", ConfigLoader.getConf().StandardTemplate), RestServer.class, false);
            RaptureAppService.setupApp("RestServer");
        } catch (RaptureException e) {
            log.error("Failed to start RestServer", e);
            return;
        }
        new RestServer().run();
    }

    private void run() {
        setupRoutes();
    }

    private void setupRoutes() {

        before((req, res) -> {
            String authentication = req.headers("Authentication");
            log.info("checking auth...");
            // TODO: check auth
            CallingContext ctx = ContextFactory.getKernelUser();
            ctxs.put(req.session().id(), ctx);
        });

        get("/doc/*", (req, res) -> {
            return Kernel.getDoc().getDoc(getContext(req), getDocUriParam(req));
        });

        put("/doc/*", (req, res) -> {
            return Kernel.getDoc().putDoc(getContext(req), getDocUriParam(req), req.body());
        });

        delete("/doc/*", (req, res) -> {
            return Kernel.getDoc().deleteDoc(getContext(req), getDocUriParam(req));
        });

        get("/blob/*", (req, res) -> {
            return Kernel.getBlob().getBlob(getContext(req), getBlobUriParam(req));
        }, json());

        put("/blob/*", (req, res) -> {
            String uri = getBlobUriParam(req);
            Kernel.getBlob().putBlob(getContext(req), uri, req.bodyAsBytes(), req.contentType());
            return uri;
        });

        delete("/blob/*", (req, res) -> {
            Kernel.getBlob().deleteBlob(getContext(req), getBlobUriParam(req));
            return null;
        });
    }

    private String getDocUriParam(Request req) {
        return req.pathInfo().substring("/doc/".length());
    }

    private String getBlobUriParam(Request req) {
        return req.pathInfo().substring("/blob/".length());
    }

    private CallingContext getContext(Request req) {
        return ctxs.get(req.session().id());
    }

}