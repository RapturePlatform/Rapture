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
package rapture.kernel;

import java.io.File;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import rapture.common.CallingContext;
import rapture.config.ConfigLoader;

/**
 * Applications (particularly the kernel) use this class instance (accessible
 * from the Kernel) to update their status.
 * 
 * The core works out what the capabilities are, and a lot of this information
 * comes from the config file, with the properties overridden (hopefully) on the
 * command line by something like RaptureRunner.
 * 
 * The goal is to be able to call: public Boolean
 * recordRunnerStatus(CallingContext context, String server, String serverGroup,
 * String appInstance, String appName, String status, Map<String, Object>
 * capabilities) { on the runner api
 * 
 * @author amkimian
 * 
 */
public class StatusManager {
    private String serverName;
    private String serverGroup; // From config
    private String appInstance; // From config
    private String appName; // From config
    private String status; // Updated by application that is running
    private Map<String, Object> capabilities; // Initialized by this class,
                                              // updateable (extended) by
                                              // applications

    public StatusManager(Class<?> classFile) {
        serverGroup = ConfigLoader.getConf().ServerGroup;
        appInstance = ConfigLoader.getConf().AppInstance;
        if (appInstance == "localRun") {
            try {
                File x = new File(classFile.getProtectionDomain().getCodeSource().getLocation().getPath());
                appInstance = x.getName();
            } catch (Exception e) {

            }
        }
        appName = ConfigLoader.getConf().AppName;
        status = "Unknown";
        capabilities = new HashMap<String, Object>();
        try {
            serverName = java.net.InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            serverName = "unknown";
        }
    }

    public void setCapability(String name, Object value) {
        capabilities.put(name, value);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void updateThroughKernel() {
        CallingContext context = ContextFactory.getKernelUser();
        Kernel.getRunner().recordRunnerStatus(context, serverName, serverGroup, appInstance, appName, status);
        if (capabilities != null && capabilities.keySet().size() > 0) {
            Kernel.getRunner().recordInstanceCapabilities(context, serverName, appInstance, capabilities);
        }
    }
}
