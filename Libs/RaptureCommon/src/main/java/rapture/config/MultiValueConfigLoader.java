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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import rapture.common.exception.ExceptionToString;
import rapture.home.RaptureHomeRetriever;

/**
 * Given a concept like "What is the URL for MongoDB", this class provides a concrete implementation on how to get at that value.
 * <p>
 * Basically a request comes in for a value like "MONGODB-default" or "MONGODB-primary"
 * <p>
 * We resolve this like the following (and in this order)
 * <p>
 * 1. Look for a -D property of the same name, all caps (i.e. toUpper)
 * <p>
 * 2. Look for an environment variable of the same name, all caps (i.e. toUpper)
 * <p>
 * 3. Look for a file using the discovery path in {@link ConfigFileReader}
 * 
 * @author amkimian
 */
public enum MultiValueConfigLoader {
    INSTANCE;

    private static Logger log = Logger.getLogger(MultiValueConfigLoader.class);
    private Map<String, Map<String, String>> knownAssignments = new HashMap<>();
    private Map<String, String> knownGlobalDirs = new HashMap<>();
    private Map<String, Boolean> isSeenBeforeMap = new HashMap<>();
    private ValueReader envReader = new EnvironmentValueReader();
    private ValueReader sysPropReader = new SystemPropertyValueReader();

    private MultiValueConfigLoader() {
    }

    // Really used for mock testing
    public static void setEnvReader(ValueReader reader) {
        INSTANCE.envReader = reader;
    }

    public static void setSysPropReader(ValueReader reader) {
        INSTANCE.sysPropReader = reader;
    }

    public static String getConfig(String configName, String defaultValue) {
        String retVal = getConfig(configName);
        if (retVal == null) {
            return defaultValue;
        } else {
            return retVal;
        }
    }

    /**
     * By default attempt to store this in the FILE implementation, as that is the only one known to be persistent.
     * <p>
     * Write this into the first file found, or create a new file at the last search point.
     * 
     * @param configName
     * @param value
     */
    public static void writeConfig(String configName, String value) {
        log.debug("Storing config for " + configName);
        String[] parts = configName.split("-");
        String primary = parts[0];
        String secondary = "";
        if (parts.length > 1) {
            secondary = parts[1];
        }
        INSTANCE.saveUnderlyingConfig(primary, secondary, value);
    }

    public static String getConfig(String configName) {
        log.trace("Retrieving config for " + configName);
        String[] parts = configName.split("-");
        String primary = parts[0];
        String secondary = "";
        if (parts.length > 1) {
            secondary = parts[1];
        }
        return INSTANCE.getUnderlyingConfig(primary, secondary);
    }

    public static Set<String> getConfigKeys(String primary) {
        if(!INSTANCE.knownAssignments.containsKey(primary)) {
            INSTANCE.getUnderlyingConfig(primary, "");
        }
        if(INSTANCE.knownAssignments.containsKey(primary)) {
            return INSTANCE.knownAssignments.get(primary).keySet();
        } else {
            log.info("No config found for " + primary);
            return Collections.emptySet();
        }
    }

    private void saveUnderlyingConfig(String primary, String secondary, String value) {
        removeFromKnownAssignments(primary, secondary);
        String configFileName = getConfigFileName(primary);
        String appConfigHome = RaptureHomeRetriever.getAppConfigHome();
        File configFile = null;
        if (appConfigHome != null) {
            configFile = new File(appConfigHome, configFileName);
        } else {
            String globalConfigHome = null;
            if(knownGlobalDirs.containsKey(configFileName)) {
                globalConfigHome = knownGlobalDirs.get(configFileName);
            } else {
                // find the global config home
                ConfigFileReader<Map<Object, Object>> configFileReader = new JavaPropertiesConfigReader(configFileName);
                configFileReader.findAndRead();
                globalConfigHome = configFileReader.getGlobalConfigDir();
                if(globalConfigHome != null) {
                    knownGlobalDirs.put(configFileName, globalConfigHome);
                }
            }
            if (globalConfigHome != null) {
                configFile = new File(globalConfigHome, configFileName);
            }
        }

        if (configFile != null) {
            if (configFile.isAbsolute()) {
                log.info(String.format("About to save config for %s-%s into file %s", primary, secondary, configFile.getAbsolutePath()));
                saveToFile(configFile, secondary, value, true);
            } else {
                log.info(String.format("Will not store config, not an absolute path: %s", configFile.getAbsolutePath()));
            }
        } else {
            log.error(String.format("App config and global config home not set, saving config not supported for for %s-%s ", primary, secondary));
        }
        saveReturn(primary, secondary, value);
    }

    private boolean saveToFile(File file, String propertyName, String propertyValue, boolean createIfNotExist) {
        try {
            if (!file.exists()) {
                if (!createIfNotExist) {
                    return false;
                } else {
                    // noinspection ResultOfMethodCallIgnored
                    file.createNewFile();
                }
            }
            // Now read the file looking for the value
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder newFile = new StringBuilder();
            String line;
            boolean changed = false;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.isEmpty() || line.startsWith("[")) {
                    newFile.append(line);
                } else {
                    int indexPoint = line.indexOf('=');
                    String left = line.substring(0, indexPoint);
                    if (left.equals(propertyName)) {
                        newFile.append(left);
                        newFile.append("=");
                        newFile.append(propertyValue);
                        changed = true;
                    } else {
                        newFile.append(line);
                    }
                }
                newFile.append("\n");
            }
            reader.close();
            if (!changed) {
                newFile.append(propertyName);
                newFile.append("=");
                newFile.append(propertyValue);
                newFile.append("\n");
            }
            // And now rewrite the file
            FileOutputStream newOutput = new FileOutputStream(file, false);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(newOutput));
            writer.append(newFile.toString());
            writer.close();
        } catch (IOException e) {
            log.error("Problem when saving value in config file: " + ExceptionToString.format(e));
        }
        return false;
    }

    private String getUnderlyingConfig(String primary, String secondary) {
        // Have we seen this before? If so, return it
        String potentialReturn = getFromKnownAssignments(primary, secondary);
        if (potentialReturn != null) {
            return potentialReturn;
        }

        String foundString = String.format("Found %s-%s in ", primary, secondary);

        // 1. try to get from system property
        log.trace("Not currently known, looking for " + primary + " - " + secondary);
        potentialReturn = getFromSystemProperty(primary, secondary);
        if (potentialReturn != null) {
            log.info(foundString + " defined property");
            return saveReturn(primary, secondary, potentialReturn);
        }

        // 2. try to get from env variable
        potentialReturn = getFromEnvironment(primary, secondary);
        if (potentialReturn != null) {
            log.info(foundString + " environment");
            return saveReturn(primary, secondary, potentialReturn);
        }

        // 3. try to get from a file

        // first, some optimization -- don't attempt to read file hundreds of
        // times if we've already tried and it's not there
        if (!isReadFromFile(primary)) {

            String configFileName = getConfigFileName(primary);
            ConfigFileReader<Map<Object, Object>> configFileReader = new JavaPropertiesConfigReader(configFileName);
            Map<Object, Object> map = configFileReader.findAndRead();
            if (map != null) {
                for (Entry<Object, Object> entry : map.entrySet()) {
                    saveReturn(primary, entry.getKey().toString(), entry.getValue().toString());
                }
                knownGlobalDirs.put(configFileName, configFileReader.getGlobalConfigDir());
            }
            setReadFromFile(primary);
        }
        return getFromKnownAssignments(primary, secondary);
    }

    private void setReadFromFile(String primary) {
        isSeenBeforeMap.put(primary, true);
    }

    private boolean isReadFromFile(String primary) {
        return isSeenBeforeMap.containsKey(primary);
    }

    private String getConfigFileName(String primary) {
        return "Rapture" + primary.toUpperCase() + ".cfg";
    }

    private void removeFromKnownAssignments(String primary, String secondary) {
        if (knownAssignments.containsKey(primary)) {
            knownAssignments.get(primary).remove(secondary);
        }
    }

    private String getFromKnownAssignments(String primary, String secondary) {
        if (knownAssignments.containsKey(primary)) {
            if (knownAssignments.get(primary).containsKey(secondary)) {
                return knownAssignments.get(primary).get(secondary);
            }
        }
        return null;
    }

    private String getFromEnvironment(String primary, String secondary) {
        String checkName = getStandardName(primary, secondary);
        log.trace("Looking for " + checkName + " through env reader");
        return envReader.getValue(checkName);
    }

    private String saveReturn(String primary, String secondary, String potentialReturn) {
        if (!knownAssignments.containsKey(primary)) {
            knownAssignments.put(primary, new HashMap<String, String>());
        }
        knownAssignments.get(primary).put(secondary, potentialReturn);
        return potentialReturn;
    }

    private String getFromSystemProperty(String primary, String secondary) {
        String checkProperty = getStandardName(primary, secondary);
        log.trace("Looking for " + checkProperty + " through property reader");
        return sysPropReader.getValue(checkProperty);
    }

    private String getStandardName(String primary, String secondary) {
        return primary.toUpperCase() + "-" + secondary.toUpperCase();
    }
}
