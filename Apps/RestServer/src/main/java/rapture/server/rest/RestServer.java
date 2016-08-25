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
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.secure;
import static spark.Spark.webSocket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableMap;

import rapture.app.RaptureAppService;
import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.config.ConfigLoader;
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
        setupHttps();
        setupWebSocket();
        setupRoutes();
        exception(Exception.class, (e, req, res) -> {
            log.error(String.format("Exception for request: %s [%s]%n[%s]", req.requestMethod(), req.pathInfo(), req.body()), e);
        });
    }

    private void setupHttps() {
        String keyStoreLocation = "keystore.jks";
        secure(keyStoreLocation, keyStoreLocation, null, null);

    }

    private void setupWebSocket() {
        webSocket("/websocket", GenericWebSocket.class);
    }

    @SuppressWarnings("unchecked")
    private void setupRoutes() {

        before((req, res) -> {
            log.info("Path is: " + req.pathInfo());
            if (!req.pathInfo().startsWith("/login")) {
                CallingContext ctx = ctxs.get(req.session().id());
                if (ctx == null) {
                    log.warn("CallingContext not found, rejecting...");
                    halt(401, "Please login first to /login");
                }
            }
        });

        post("/login", (req, res) -> {
            log.info("Logging in...");
            CallingContext ctx = null;
            Map<String, Object> data = JacksonUtil.getMapFromJson(req.body());
            String apiKey = (String) data.get("apiKey");
            if (StringUtils.isNotBlank(apiKey)) {
                String appKey = (String) data.get("appKey");
                ctx = Kernel.INSTANCE.loadContext(appKey, apiKey);
                if (ctx == null) {
                    String msg = String.format("Invalid apiKey [%s] and appKey [%s]", apiKey, appKey);
                    log.warn(msg);
                    halt(401, msg);
                }
            } else {
                String username = (String) data.get("username");
                String password = (String) data.get("password");
                try {
                    ctx = Kernel.getLogin().login(username, password, null);
                } catch (RaptureException re) {
                    String msg = "Invalid login: " + re.getMessage();
                    log.warn(msg);
                    halt(401, msg);
                }
            }
            String id = req.session(true).id();
            ctxs.put(id, ctx);
            return id;
        });

        post("/doc/:authority", (req, res) -> {
            log.info(req.body());
            Map<String, Object> data = JacksonUtil.getMapFromJson(req.body());
            String authority = req.params(":authority");
            String config = (String) data.get("config");
            CallingContext ctx = getContext(req);
            if (Kernel.getDoc().docRepoExists(ctx, authority)) {
                halt(409, String.format("Repo [%s] already exists", authority));
            }
            Kernel.getDoc().createDocRepo(ctx, authority, config);
            return new RaptureURI(authority, Scheme.DOCUMENT).toString();
        });

        get("/doc/*", (req, res) -> {
            String meta = req.queryParams("meta");
            if (StringUtils.isNotBlank(meta)) {
                return Kernel.getDoc().getDocAndMeta(getContext(req), getDocUriParam(req));
            }
            return Kernel.getDoc().getDoc(getContext(req), getDocUriParam(req));
        });

        put("/doc/*", (req, res) -> {
            return Kernel.getDoc().putDoc(getContext(req), getDocUriParam(req), req.body());
        });

        delete("/doc/:authority", (req, res) -> {
            Kernel.getDoc().deleteDocRepo(getContext(req), req.params(":authority"));
            return true;
        });

        delete("/doc/*", (req, res) -> {
            return Kernel.getDoc().deleteDoc(getContext(req), getDocUriParam(req));
        });

        post("/blob/:authority", (req, res) -> {
            log.info(req.body());
            Map<String, Object> data = JacksonUtil.getMapFromJson(req.body());
            String authority = req.params(":authority");
            String config = (String) data.get("config");
            String metaConfig = (String) data.get("metaConfig");
            CallingContext ctx = getContext(req);
            if (Kernel.getBlob().blobRepoExists(ctx, authority)) {
                halt(409, String.format("Repo [%s] already exists", authority));
            }
            Kernel.getBlob().createBlobRepo(ctx, authority, config, metaConfig);
            return new RaptureURI(authority, Scheme.BLOB).toString();
        });

        get("/blob/*", (req, res) -> {
            return Kernel.getBlob().getBlob(getContext(req), getBlobUriParam(req));
        }, json());

        put("/blob/*", (req, res) -> {
            String uri = getBlobUriParam(req);
            Kernel.getBlob().putBlob(getContext(req), uri, req.bodyAsBytes(), req.contentType());
            return uri;
        });

        delete("/blob/:authority", (req, res) -> {
            Kernel.getBlob().deleteBlobRepo(getContext(req), req.params(":authority"));
            return true;
        });

        delete("/blob/*", (req, res) -> {
            Kernel.getBlob().deleteBlob(getContext(req), getBlobUriParam(req));
            return true;
        });

        post("/series/:authority", (req, res) -> {
            log.info(req.body());
            Map<String, Object> data = JacksonUtil.getMapFromJson(req.body());
            String authority = req.params(":authority");
            String config = (String) data.get("config");
            CallingContext ctx = getContext(req);
            if (Kernel.getSeries().seriesRepoExists(ctx, authority)) {
                halt(409, String.format("Repo [%s] already exists", authority));
            }
            Kernel.getSeries().createSeriesRepo(ctx, authority, config);
            return new RaptureURI(authority, Scheme.SERIES).toString();
        });

        get("/series/*", (req, res) -> {
            return Kernel.getSeries().getPoints(getContext(req), getSeriesUriParam(req));
        }, json());

        put("/series/*", (req, res) -> {
            String uri = getSeriesUriParam(req);
            Map<String, Object> data = JacksonUtil.getMapFromJson(req.body());
            List<String> keys = (List<String>) data.get("keys");
            List<Object> values = (List<Object>) data.get("values");
            if (!values.isEmpty()) {
                Object obj = values.get(0);
                if (obj instanceof Long) {
                    Kernel.getSeries().addLongsToSeries(getContext(req), uri, keys, (List<Long>) (List<?>) values);
                } else if (obj instanceof String) {
                    Kernel.getSeries().addStringsToSeries(getContext(req), uri, keys, (List<String>) (List<?>) values);
                } else if (obj instanceof Double) {
                    Kernel.getSeries().addDoublesToSeries(getContext(req), uri, keys, (List<Double>) (List<?>) values);
                } else {
                    halt(400, "Unknown type in values parameter");
                }
            }
            return uri;
        });

        delete("/series/:authority", (req, res) -> {
            Kernel.getSeries().deleteSeriesRepo(getContext(req), req.params(":authority"));
            return true;
        });

        delete("/series/*", (req, res) -> {
            Kernel.getSeries().deleteSeries(getContext(req), getSeriesUriParam(req));
            return true;
        });

        post("/workorder/*", (req, res) -> {
            Map<String, String> params = new HashMap<>();
            String body = req.body();
            if (StringUtils.isNotBlank(body)) {
                Map<String, Object> data = JacksonUtil.getMapFromJson(body);
                params = (Map<String, String>) data.get("params");
            }
            return Kernel.getDecision().createWorkOrder(getContext(req), getWorkorderUriParam(req), params);
        });

    }

    private String getDocUriParam(Request req) {
        return getUriParam(req, "/doc/");
    }

    private String getBlobUriParam(Request req) {
        return getUriParam(req, "/blob/");
    }

    private String getSeriesUriParam(Request req) {
        return getUriParam(req, "/series/");
    }

    private String getWorkorderUriParam(Request req) {
        return getUriParam(req, "/workorder/");
    }

    private String getUriParam(Request req, String prefix) {
        String ret = req.pathInfo().substring(prefix.length());
        int index = ret.indexOf("?");
        if (index != -1) {
            ret = ret.substring(0, index);
        }
        return ret;
    }

    private CallingContext getContext(Request req) {
        return ctxs.get(req.session().id());
    }

}