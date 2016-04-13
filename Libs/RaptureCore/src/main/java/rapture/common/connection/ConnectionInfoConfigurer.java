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

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang.StringUtils;
import rapture.common.CallingContext;
import rapture.common.ConnectionInfo;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.config.MultiValueConfigLoader;
import rapture.kernel.Kernel;
import rapture.kernel.sys.SysArea;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by yanwang on 12/3/15.
 */
public abstract class ConnectionInfoConfigurer {

    protected static String SYS_PATH_PREFIX = "connection/";

    public abstract ConnectionType getType();
    public abstract int getDefaultPort();

    protected TypeReference<Map<String, ConnectionInfo>> typeReference = new TypeReference<Map<String, ConnectionInfo>>(){};

    public Map<String, ConnectionInfo> getConnectionInfo(CallingContext context) {
        // first, try to get from sys.config
        Map<String, ConnectionInfo> map = getConnectionInfoFromSysConfig(context);
        if(map != null && !map.isEmpty()) {
            return map;
        }
        // otherwise, get from local config file
        map = new HashMap<>();
        for(String instanceName : getInstancesFromLocal()) {
            map.put(instanceName, getConnectionInfoFromLocal(instanceName));
        }
        if(!map.isEmpty()) {
            writeConnectionInfoToSysConfig(context, map);
        }
        return map;
    }

    public void putConnectionInfo(CallingContext context, ConnectionInfo info) {
        validateConnectionInfo(info);
        // check connection is new
        Map<String, ConnectionInfo> map = getConnectionInfo(context);
        if(map.containsKey(info.getInstanceName())) {
        	//
            //throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR,
            //            "Will not add. Connection already exists: " + info);
        }
        // add connection info to sys.config
        map.put(info.getInstanceName(), info);
        writeConnectionInfoToSysConfig(context, map);
    }

    public void setConnectionInfo(CallingContext context, ConnectionInfo info) {
        validateConnectionInfo(info);
        // check connection exists
        Map<String, ConnectionInfo> map = getConnectionInfo(context);
        if(!map.containsKey(info.getInstanceName())) {
            throw RaptureExceptionFactory.create("Instance " + info.getInstanceName() + " does not exist");
        }
        map.put(info.getInstanceName(), info);
        writeConnectionInfoToSysConfig(context, map);
    }

    protected Map<String, ConnectionInfo> getConnectionInfoFromSysConfig(CallingContext context) {
        String config = Kernel.getSys().retrieveSystemConfig(context, SysArea.CONFIG.toString(),
                getSysConfigPath());
        if(!StringUtils.isBlank(config)) {
            return JacksonUtil.objectFromJson(config, typeReference);
        }
        return null;
    }

    protected void writeConnectionInfoToSysConfig(CallingContext context, Map<String, ConnectionInfo> map) {
        Kernel.getSys().writeSystemConfig(context, SysArea.CONFIG.toString(), getSysConfigPath(),
                JacksonUtil.jsonFromObject(map));
    }

    protected String getSysConfigPath() {
        return SYS_PATH_PREFIX + getType().toString();
    }

    protected Set<String> getInstancesFromLocal() {
        return MultiValueConfigLoader.getConfigKeys(getConfigPrimary());
    }

    protected ConnectionInfo getConnectionInfoFromLocal(String instanceName) {
        try {
            String url = getConnectionUrl(instanceName);
            URI uri = new URI(url);
            String host = StringUtils.isBlank(uri.getHost()) ? "localhost" : uri.getHost();
            int port = uri.getPort() < 0 ? getDefaultPort() : uri.getPort();
            String username = "";
            String password = "";
            if(!StringUtils.isBlank(uri.getUserInfo())) {
                String[] userInfo = uri.getUserInfo().split(":");
                username = userInfo[0];
                password = userInfo[1];
            }
            String dbName = uri.getPath().substring(1);
            return new ConnectionInfo(host, port,
                    username, password, dbName, instanceName);
        } catch (URISyntaxException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Invalid URI", e);
        }
    }

    protected void validateConnectionInfo(ConnectionInfo info) {
        checkFieldPresent("host", info.getHost());
        checkPort(info);
        checkFieldPresent("username", info.getUsername());
        checkFieldPresent("password", info.getPassword());
        checkFieldPresent("database name", info.getDbName());
        checkFieldPresent("instance name", info.getInstanceName());
    }

    protected String getConfigPrimary() {
        return getType().toString();
    }

    protected String getConnectionUrl(String instanceName) {
        return getConfig(instanceName);
    }

    protected String getConfig(String... secondary) {
        String configName = getConfigName(secondary);
        return MultiValueConfigLoader.getConfig(configName);
    }

    protected String getConfigName(String... secondary) {
        return String.format("%s-%s", getConfigPrimary(), StringUtils.join(secondary, "."));
    }

    protected void checkFieldPresent(String fieldName, String fieldValue) {
        if(StringUtils.isBlank(fieldValue)) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR,
                    fieldName + " can not be empty");
        }
    }

    protected void checkPort(ConnectionInfo info) {
        if(info.getPort() <= 0) {
            info.setPort(getDefaultPort());
        }
    }

}
