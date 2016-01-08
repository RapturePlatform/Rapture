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
package rapture.config;

import org.apache.log4j.Logger;

/**
 * This class is used to load the config for Rapture
 *
 * @author amkimian
 */
public class ConfigLoader {

    /*
     * default name of the rapture config file
     */
    public static final String RAPTURE_CFG = "rapture.cfg";
    private static Logger log = Logger.getLogger(ConfigLoader.class);

    private static RaptureConfig conf;
    private static String globalConfigDir;

    static {
        log.info("Loading Rapture Config");
        if (RaptureConfig.getLoadYaml()) {
            loadYaml();
        } else {
            conf = new RaptureConfig();
        }
    }

    static void loadYaml() {
        String configFileName = System.getProperty("RAPTURE_CONFIG_FILENAME");
        if (configFileName == null) {
            configFileName = RAPTURE_CFG;
        }

        ConfigFileReader<RaptureConfig> configFileReader = new YamlConfigReader(configFileName);
        conf = configFileReader.findAndRead();
        if (conf == null) {
            log.fatal("Unable to load rapture config from any sources.");
            conf = new RaptureConfig();
            conf.applyOverrides();
        } else {
            conf.applyOverrides();
            globalConfigDir = configFileReader.getGlobalConfigDir();
            log.info("Successfully loaded config file");
            if (!System.getProperty("os.arch").contains("64")) log
                    .info("32bit JVM detected.  It is recommended to run Rapture on a 64bit JVM for better performance.");
        }
    }

    public static RaptureConfig getConf() {
        return conf;
    }

    public static String getGlobalConfigDir() {
        return globalConfigDir;
    }
}
