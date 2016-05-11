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
package rapture.jmx;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

/**
 * Caches all the JMX apps and automatically expires entries after 1 minute. It issues a multicast to discover the Rapture nodes listening over http
 * 
 * @author dukenguyen
 *
 */
public enum JmxAppCache {
    INSTANCE;

    private static final Logger log = Logger.getLogger(JmxAppCache.class);

    /*
     * expire 1 minute after writing and refresh on the next get() call
     */
    private Cache<String, Map<String, JmxApp>> cache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

    public static JmxAppCache getInstance() {
        return INSTANCE;
    }

    public Map<String, JmxApp> get() throws ExecutionException {
        return cache.get("apps", new Callable<Map<String, JmxApp>>() {
            @Override
            public Map<String, JmxApp> call() throws ExecutionException {
                Map<String, JmxApp> ret = new HashMap<>();
                HttpResponse<JsonNode> response;
                try {
                    response = Unirest.get(String.format("%s/exec/jolokia:type=Discovery/lookupAgents", JmxServer.getInstance().getJmxApp().getUrl())).asJson();
                } catch (UnirestException e) {
                    throw new ExecutionException("Could not discover jolokia jmx agents using REST", e);
                }
                JSONArray nodes = response.getBody().getObject().getJSONArray("value");
                if (nodes.length() <= 0) {
                    // nothing came back, not even this server, so let's restart this server
                    log.error("Discovery produced 0 nodes, not even this server.  Attempting to restart");
                    JmxServer.getInstance().restart();
                }
                for (int i = 0; i < nodes.length(); i++) {
                    JmxApp app = new JmxApp(nodes.getJSONObject(i).getString("url"));
                    ret.put(app.toString(), app);
                }
                log.info(String.format("JmxAppCache refreshed with [%d] entries", ret.size()));
                return ret;
            }
        });
    }

    public void invalidate() {
        cache.invalidateAll();
    }
}