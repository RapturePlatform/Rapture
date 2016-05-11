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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jolokia.jvmagent.JolokiaServer;
import org.jolokia.jvmagent.JolokiaServerConfig;

import rapture.jmx.beans.JmxAppCache;
import rapture.jmx.beans.RaptureLogging;
import rapture.util.NetworkUtil;

/**
 * Represents a JmxServer for this rapture kernel. There should only be 1 per Rapture kernel to ensure discovery works properly.
 * 
 * @author dukenguyen
 *
 */
public enum JmxServer {
    INSTANCE;

    private static final Logger log = Logger.getLogger(JmxServer.class);

    private JmxApp jmxApp;
    private boolean started;

    public static JmxServer getInstance() {
        return INSTANCE;
    }

    public void start(String appName) {
        if (isStarted()) {
            log.warn(String.format("Rejecting an attempt to start more than one JmxServer.  AppName passed is [%s]", appName));
            return;
        }
        registerBeans();
        Map<String, String> config = new HashMap<>();
        config.put("host", "*");
        config.put("port", "0");
        config.put("agentContext", "/" + appName);
        JolokiaServer server;
        try {
            server = new JolokiaServer(new JolokiaServerConfig(config), false);
        } catch (IOException e) {
            log.error("Unable to start JmxServer", e);
            return;
        }
        server.start();
        setStarted(true);
        jmxApp = new JmxApp(appName, NetworkUtil.getSiteLocalServerIP(), server.getAddress().getPort());
        log.info(String.format("JmxServer can be accessed at [http://%s:%d/%s]", jmxApp.getHost(), jmxApp.getPort(), jmxApp.getName()));
    }

    public JmxApp getJmxApp() {
        return jmxApp;
    }

    /**
     * This is where we should register our Rapture JMX beans
     */
    private void registerBeans() {
        registerBean(new RaptureLogging(), "rapture.jmx.beans:type=RaptureLogging");
        registerBean(new JmxAppCache(), "rapture.jmx.beans:type=JmxAppCache");
    }

    private void registerBean(Object object, String objNameStr) {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName = new ObjectName(objNameStr);
            if (!mbs.isRegistered(objectName)) {
                mbs.registerMBean(object, objectName);
            }
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException | MalformedObjectNameException e) {
            log.warn(String.format("Registering a bean with name [%s] caused an exception", objNameStr), e);
        }
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public void restart() {
        setStarted(false);
        if (jmxApp != null && StringUtils.isNotBlank(jmxApp.getName())) {
            start(jmxApp.getName());
        }
    }
}