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
package rapture.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.log4j.Logger;

import rapture.common.exception.ExceptionToString;
import rapture.home.RaptureHomeRetriever;

/**
 * This is an abstract class used to find the Rapture config files, calculate an
 * overlay if there are any overrides in an override cfg file, and then return
 * the properties.
 * 
 * There are three levels searched in the discovery path, from lowest precedence
 * to highest precedence: 1. default (baked into classpath) 2. global config 3.
 * app-specific config. If the global config system property or env variable is
 * set, we look for global config and app config in that dir. Otherwise, look in
 * classpath.
 * 
 * 
 * @author bardhi
 * 
 */
public abstract class ConfigFileReader<T> {
    private static final Logger log = Logger.getLogger(ConfigFileReader.class);
    private String configFileName;
    private String globalConfigHome;
    private String appConfigHome;
    private String path;
    private String appName;
    private String globalConfigDir;
    private String dir;

    public ConfigFileReader(String configFileName) {
        this.configFileName = configFileName;
        this.globalConfigHome = RaptureHomeRetriever.getGlobalConfigHome();
        this.appConfigHome = RaptureHomeRetriever.getAppConfigHome();
        this.appName = RaptureHomeRetriever.getAppName();
    }

    /**
     */
    public T findAndRead() {
        T overlaidResult = null;

        InputStream defaultConfigStream;
        InputStream globalConfigStream;
        InputStream appConfigStream;

        /*
         * Default is always from classpath
         */
        defaultConfigStream = getStreamFromClasspath("defaults/");
        log.info(foundMessage("default", "classpath", path));

        /*
         * If RAPTURE_CONFIG_HOME if set, look for both global and app-specific
         * config in location where that property points. Else, look in
         * classpath for both
         */
        if (globalConfigHome != null) {
            log.info(String.format("%s is set to %s, retrieve config from there for %s", RaptureHomeRetriever.GLOBAL_CONFIG_HOME, globalConfigHome,
                    configFileName));
            globalConfigDir = globalConfigHome;
            globalConfigStream = getStreamFromConfigHome(globalConfigHome);
            log.info(foundMessage("global config", "RAPTURE_CONFIG_HOME", path));
            appConfigStream = getStreamFromConfigHome(appConfigHome);
            log.info(foundMessage("app-specific config", "RAPTURE_CONFIG_HOME", path));
        } else {
            globalConfigStream = getStreamFromClasspath("");
            globalConfigDir = dir;
            log.info(foundMessage("global config", "classpath", path));
            appConfigStream = getStreamFromClasspath(appName + "/");
            log.info(foundMessage("app-specific config", "classpath", path));
        }

        File overlaidFile = null;
        try {
            overlaidFile = File.createTempFile(configFileName, ".cfg");
        } catch (IOException e) {
            log.warn("Error creating overlaid file: " + ExceptionToString.format(e));
        }

        if (defaultConfigStream == null && globalConfigStream == null && appConfigStream == null) {
            log.warn("No config files found for " + configFileName);
        } else {
            overlaidResult = doOverlay(overlaidFile, defaultConfigStream, globalConfigStream, appConfigStream);
        }
        if (overlaidResult != null) {
            if (overlaidFile != null && overlaidFile.exists()) {
                log.debug("Overlaid file is available at " + overlaidFile.getAbsolutePath());
            }
        } else {
            log.info("Unable to find overlay file for " + configFileName);
        }
        return overlaidResult;
    }

    private InputStream getStreamFromConfigHome(String configHome) {
        path = null;
        dir = null;
        if (configHome != null) {
            File file = new File(configHome, configFileName);
            if (file.exists()) {
                InputStream stream;
                try {
                    stream = new FileInputStream(file);
                    path = file.getAbsolutePath();
                    return stream;
                } catch (FileNotFoundException e) {
                    log.error("Error reading file: " + ExceptionToString.format(e));
                }
            }
        }
        return null;
    }

    private String foundMessage(String type, String where, String path) {
        if (path == null) {
            return String.format("No %s found for %s.", type, configFileName);
        } else {
            return String.format("Found %s for %s in %s. URL: %s", type, configFileName, where, path);
        }
    }

    private InputStream getStreamFromClasspath(String dirName) {
        path = null;
        String defaultCfgPath = String.format("rapture/config/%s%s", dirName, configFileName);
        URL defaultURL = this.getClass().getClassLoader().getResource(defaultCfgPath);

        if (defaultURL != null) {
            InputStream stream = this.getClass().getClassLoader().getResourceAsStream(defaultCfgPath);
            path = defaultURL.toString();
            dir = new File(defaultURL.getFile()).getParent();
            return stream;
        }
        return null;
    }

    /**
     * The implementation of this should overlay the file into overlaidFile, and
     * return the properties map. It also must close the input streams.
     * 
     * @param overlaidFile
     * @param baseStream
     * @param overridesStreams
     * @return
     */
    protected abstract T doOverlay(File overlaidFile, InputStream baseStream, InputStream... overridesStreams);

    String getGlobalConfigDir() {
        return globalConfigDir;
    }

}
