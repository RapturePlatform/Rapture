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
package rapture.kernel;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import rapture.common.CallingContext;
import rapture.common.api.EnvironmentApi;
import rapture.common.model.RaptureServerInfo;
import rapture.common.model.RaptureServerInfoStorage;
import rapture.common.model.RaptureServerStatus;
import rapture.common.model.RaptureServerStatusStorage;
import rapture.config.MultiValueConfigLoader;
import rapture.jmx.JmxApp;
import rapture.jmx.JmxAppCache;

public class EnvironmentApiImpl extends KernelBase implements EnvironmentApi {
    private static Logger log = Logger.getLogger(EnvironmentApiImpl.class);

    public EnvironmentApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
        Unirest.setTimeouts(2000, 2000);
    }

    @Override
    public RaptureServerInfo getThisServer(CallingContext context) {
        String serverId = MultiValueConfigLoader.getConfig("ENVIRONMENT-id");
        if (serverId == null) {
            log.info("No server id found!");
            return null;
        } else {
            log.info("Loading information for server " + serverId);
            return RaptureServerInfoStorage.readByFields(serverId);
        }
    }

    @Override
    public List<RaptureServerInfo> getServers(CallingContext context) {
        return RaptureServerInfoStorage.readAll();
    }

    @Override
    public RaptureServerInfo setThisServer(CallingContext context, RaptureServerInfo info) {
        log.info("Writing server information out id is " + info.getServerId());
        MultiValueConfigLoader.writeConfig("ENVIRONMENT-id", info.getServerId());
        log.info("Name is " + info.getName() + ", storing");
        RaptureServerInfoStorage.add(info, context.getUser(), "Set initial ID");
        return info;
    }

    @Override
    public void setApplianceMode(CallingContext context, Boolean mode) {
        MultiValueConfigLoader.writeConfig("ENVIRONMENT-appliance", mode.toString());
    }

    @Override
    public Boolean getApplianceMode(CallingContext context) {
        return Boolean.valueOf(MultiValueConfigLoader.getConfig("ENVIRONMENT-appliance", "false"));
    }

    @Override
    public List<RaptureServerStatus> getServerStatus(CallingContext context) {
        return RaptureServerStatusStorage.readAll();
    }

    @Override
    public Map<String, String> getMemoryInfo(CallingContext context, List<String> appNames) {
        return processGetOrPost(appNames, "read/java.lang:type=Memory", null);
    }

    @Override
    public Map<String, String> getOperatingSystemInfo(CallingContext context, List<String> appNames) {
        return processGetOrPost(appNames, "read/java.lang:type=OperatingSystem", null);
    }

    @Override
    public Map<String, String> getThreadInfo(CallingContext context, List<String> appNames) {
        return processGetOrPost(appNames, "read/java.lang:type=Threading", null);
    }

    @Override
    public Map<String, String> readByPath(CallingContext context, List<String> appNames, String path) {
        return processGetOrPost(appNames, "read/" + path, null);
    }

    @Override
    public Map<String, String> writeByPath(CallingContext context, List<String> appNames, String path) {
        return processGetOrPost(appNames, "write/" + path, null);
    }

    @Override
    public Map<String, String> execByPath(CallingContext context, List<String> appNames, String path) {
        return processGetOrPost(appNames, "exec/" + path, null);
    }

    @Override
    public Map<String, String> readByJson(CallingContext context, List<String> appNames, String json) {
        return processJson(appNames, json);
    }

    @Override
    public Map<String, String> writeByJson(CallingContext context, List<String> appNames, String json) {
        return processJson(appNames, json);
    }

    @Override
    public Map<String, String> execByJson(CallingContext context, List<String> appNames, String json) {
        return processJson(appNames, json);
    }

    private Map<String, String> processJson(List<String> appNames, String json) {
        return processGetOrPost(appNames, null, json);
    }

    private Map<String, String> processGetOrPost(List<String> appNames, String path, String json) {
        Map<String, String> ret = new HashMap<>();
        Map<String, JmxApp> apps;
        try {
            apps = JmxAppCache.getInstance().get();
        } catch (ExecutionException e) {
            log.error("Failed to update JmxApp cache", e);
            return ret;
        }
        log.debug("Apps are: " + apps.toString());
        ExecutorService executor = Executors.newFixedThreadPool(apps.size());
        for (Map.Entry<String, JmxApp> entry : apps.entrySet()) {
            final String appName = entry.getKey();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    log.debug(String.format("Executing for appName [%s]", appName));
                    if (checkApp(appName, appNames)) {
                        JmxApp app = entry.getValue();
                        HttpResponse<JsonNode> res;
                        try {
                            if (StringUtils.isNotBlank(json)) {
                                res = Unirest.post(app.getUrl()).body(json).asJson();
                            } else {
                                res = Unirest.get(String.format("%s/%s", app.getUrl(), path)).asJson();
                            }
                            ret.put(entry.getKey(), IOUtils.toString(res.getRawBody()));
                            res.getRawBody().close();
                        } catch (UnirestException | IOException e) {
                            log.error(String.format("Error accessing %s/read/%s", app.getUrl(), path), e);
                            JmxAppCache.getInstance().invalidate();
                        }
                    }
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(4, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Could not wait for executor shutdown", e);
        }
        log.debug("Returned result size is: " + ret.size());
        return ret;
    }

    private boolean checkApp(String app, List<String> appNames) {
        return CollectionUtils.isEmpty(appNames) || appNames.contains(app);
    }
}