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
package rapture.server;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableMap;

import rapture.app.RaptureAppService;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.config.ConfigLoader;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

/**
 * WatchServer monitors directories and takes actions as described in the
 * associated Rapture system configuration (//sys.config/watchserver/config)
 * document.
 * 
 * This document details directories and maps to an action (e.g. script,
 * workflow) in the Rapture system.
 * 
 * @author jonathan-major
 */
public final class WatchServer {
    private static Logger log = Logger.getLogger(WatchServer.class);
    private static final String CONFIG_URI = "watchserver/config";
    private static Map<String, Object> watchConfig;
    private static HashMap<String, List<Map>> config = new HashMap<String, List<Map>>();

    public static void main(String[] args) {
        try {
            Kernel.initBootstrap(ImmutableMap.of("STD", ConfigLoader.getConf().StandardTemplate), WatchServer.class, false);
            RaptureAppService.setupApp("WatchServer");
        } catch (RaptureException e1) {
            log.error("Failed to start WatchServer with " + ExceptionToString.format(e1));
            System.exit(-1);
        }

        // load configuration: specifies what action to take for an event
        // TODO: check that action uris exist.
        try {
            String retrieveSystemConfig = Kernel.getSys().retrieveSystemConfig(ContextFactory.getKernelUser(), "CONFIG", CONFIG_URI);
            watchConfig = JacksonUtil.getMapFromJson(retrieveSystemConfig);
            for (String key : watchConfig.keySet()) {
                List<Map> list = (List<Map>) watchConfig.get(key);
                if (Files.isDirectory(Paths.get(key))) {
                    config.put(key, list);
                    log.info("Path=" + key + ":Config=" + list.toString());
                } else {
                    log.error("Directory " + key + " does not exist! Check the config.");
                }
            }

        } catch (Exception e2) {
            log.error("Failed to load configuration from " + CONFIG_URI + ". Exception:" + ExceptionToString.format(e2));
            System.exit(-2);
        }

        for (String path : config.keySet()) {
            log.info("Setting up " + path);
            new WatchRunner(path, config.get(path)).startThread();
        }
    }

}
