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
package rapture.common.connection;

import org.apache.commons.lang.StringUtils;
import rapture.common.ConnectionInfo;
import rapture.common.exception.RaptureExceptionFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by yanwang on 12/3/15.
 */
public class PostgresConnectionInfoConfigurer extends ConnectionInfoConfigurer {
    public static int DEFAULT_PORT = 5432;
    public static String URL_PREFIX = "jdbc:";
    public static String URL = "url";
    public static String USER = "user";
    public static String PASSWORD = "password";
    public static Map<String, Class> OPTIONS = new HashMap<>();
    static {
        OPTIONS.put("initialPoolSize", Integer.class);
        OPTIONS.put("minPoolSize", Integer.class);
        OPTIONS.put("maxPoolSize", Integer.class);
        OPTIONS.put("maxIdleTimeExcessConnections", Integer.class);
        OPTIONS.put("maxStatements", Integer.class);
        OPTIONS.put("statementCacheNumDeferredCloseThreads", Integer.class);
        OPTIONS.put("idleConnectionTestPeriod", Integer.class);
        OPTIONS.put("testConnectionOnCheckin", Boolean.class);
    }

    @Override
    public ConnectionType getType() {
        return ConnectionType.POSTGRES;
    }

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    protected Set<String> getInstancesFromLocal() {
        Set<String> instances = new HashSet<>();
        for(String key : super.getInstancesFromLocal()) {
            int index = key.indexOf(".url");
            if(index > 0) {
                instances.add(key.substring(0, index));
            }
        }
        return instances;
    }

    @Override
    protected ConnectionInfo getConnectionInfoFromLocal(String instanceName) {
        ConnectionInfo info = super.getConnectionInfoFromLocal(instanceName);
        info.setUsername(getConfig(instanceName, USER));
        info.setPassword(getConfig(instanceName, PASSWORD));
        //read options
        for(String key : OPTIONS.keySet()) {
            String config = getConfig(instanceName, key);
            if(!StringUtils.isBlank(config)) {
                Class type = OPTIONS.get(key);
                if(type == Integer.class) {
                    info.setOption(key, Integer.valueOf(config));
                } else if(type == Boolean.class) {
                    info.setOption(key, Boolean.valueOf(config));
                } else {
                    info.setOption(key, config);
                }
            }
        }
        return info;
    }

    @Override
    protected String getConnectionUrl(String instanceName) {
        String fullUrl = getConfig(instanceName, URL);
        if(StringUtils.isBlank(fullUrl)) {
            return null;
        }
        if(fullUrl.startsWith(URL_PREFIX)) {
            return fullUrl.substring(URL_PREFIX.length());
        } else {
            throw RaptureExceptionFactory.create("Invalid Postgres connection url" + fullUrl);
        }
    }

    public static String getUrl(ConnectionInfo info) {
        //jdbc:postgresql://127.0.0.1:5432/integration
        return String.format("jdbc:postgresql://%s:%d/%s", info.getHost(), info.getPort(), info.getDbName());
    }
}
