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
package rapture.common;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * Created by yanwang on 11/30/15.
 */
public class ConnectionInfo implements RaptureTransferObject {
    private String host;
    private int port;
    private String username;
    private String password;
    private String dbName;
    private String instanceName;
    private String url;
    private Map<String, Object> options = new HashMap<>();

    public ConnectionInfo() {
    }

    public ConnectionInfo(String host, int port,
            String username, String password,
            String dbName, String instanceName) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.dbName = dbName;
        this.instanceName = instanceName;
    }

    @Override
    public String toString() {
        return String.format("{instanceName=%s, host=%s, port=%d, dbName=%s, " +
                "username=%s, password=%s, options=%s, url=%s}",
                instanceName, host, port, dbName,
                username, password, options, url);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ConnectionInfo) {
            ConnectionInfo info = (ConnectionInfo) object;
            return StringUtils.equals(host, info.getHost())
                    && port == info.getPort()
                    && StringUtils.equals(dbName, info.getDbName());
        }
        return false;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    public Object getOption(String key) {
        return options.get(key);
    }

    public void setOption(String key, Object value) {
        options.put(key, value);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
