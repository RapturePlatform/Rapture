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
package rapture.jmx;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.log4j.Logger;

import rapture.config.MultiValueConfigLoader;
import rapture.common.exception.ExceptionToString;
import rapture.common.impl.jackson.JacksonUtil;

public class RaptureMBeanServer {

    private RaptureMBeanServer() {
    }

    private static Logger log = Logger.getLogger(RaptureMBeanServer.class);
    private static Map<String, Integer> defaultJMXPorts;

    /**
     * Try to find the JMX port number. - Look for environmental variable com.sun.management.jmxremote.port - Pick a sensible default
     *
     * @param appName
     *            The name of the app. It will be matched against a list of recognized names which map to ports
     */
    public static void initialise(String appName) {
        int port = 0;

        for (String s : new String[] { "com.sun.management.jmxremote.port", "JMX-" + appName+".port", "JMX-default.port", "JMX-" + appName, "JMX-default" }) {
            String portStr = MultiValueConfigLoader.getConfig(s);
            if (portStr != null) try {
                port = Integer.parseInt(portStr);
                break;
            } catch (NumberFormatException e) {
                log.debug(String.format("Error reading JMX port from system property '%s': %s", s, ExceptionToString.format(e)));
            }
        }
        if (port == 0) {
            port = 10500;
            log.warn("No JMX port found for "+appName+" - using "+port);
        }
        
        Map<String, Object> env = null;

        for (String s : new String[] { "JMX-" + appName+".args", "JMX-default.args" }) {
            String jmxargs = MultiValueConfigLoader.getConfig(s);
            if (jmxargs != null) try {
                env = JacksonUtil.getMapFromJson(jmxargs);
                break;
            } catch (Exception e) {
                log.debug("Unable to parse "+s+"="+jmxargs);
            }
        }

        if (env == null) {
            env = new HashMap<String, Object>();
            env.put("com.sun.management.jmxremote.authenticate", "false");
            env.put("com.sun.management.jmxremote.local.only", "false");
            env.put("com.sun.management.jmxremote.ssl", "false");
        }

        String jmxurl = null;
        for (String s : new String[] { "JMX-" + appName+".url", "JMX-default.url" }) {
            jmxurl = MultiValueConfigLoader.getConfig(s);
            if (jmxurl != null) break;
        }
        if (jmxurl == null) jmxurl = "service:jmx:rmi:///jndi/rmi://:%d/jmxrmi";

        // Get the platform MBeanServer
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        /**
         * Create a JMXConnectorServer to access the MBean Server. This works even if you didn't specify any JMX parameters on the command line
         */

        try {
            LocateRegistry.createRegistry(port);
            JMXServiceURL url = new JMXServiceURL(String.format(jmxurl, port));
            JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);

            log.info("Listening for JMX requests on port " + port);
            // Start the RMI connector server.
            cs.start();
        } catch (IOException e) {
            log.error("Error during JMX initialization: " + ExceptionToString.format(e));
        }

    }

}