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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import rapture.common.exception.ExceptionToString;

public class YamlConfigReader extends ConfigFileReader<RaptureConfig> {
    public YamlConfigReader(String configFileName) {
        super(configFileName);
    }

    private static final Logger log = Logger.getLogger(YamlConfigReader.class);

    @Override
    protected RaptureConfig doOverlay(File overlaidFile, InputStream originalStream, InputStream... overridesStreams) {
        try {
            String currentString = null;
            if (originalStream != null) {
                try {
                    List<String> lines = IOUtils.readLines(originalStream);
                    currentString = StringUtils.join(lines, "\n");
                } catch (IOException e) {
                    log.error("Error reading original stream: " + ExceptionToString.format(e));
                }
            }
            for (InputStream overridesStream : overridesStreams) {
                String overridesString = null;
                if (overridesStream != null) {
                    try {
                        List<String> lines = IOUtils.readLines(overridesStream);
                        overridesString = StringUtils.join(lines, "\n");
                    } catch (IOException e) {
                        log.error("Error reading original stream: " + ExceptionToString.format(e));
                    }
                }
                currentString = overlayYaml(currentString, overridesString);
            }
            try {
                FileUtils.write(overlaidFile, currentString);
            } catch (IOException e1) {
                log.error(String.format("Error writing to overlay file: %s", ExceptionToString.format(e1)));
            }
            Constructor constructor = new Constructor(RaptureConfig.class);
            try {
                Yaml yaml = new Yaml(constructor);
                return (RaptureConfig) yaml.load(currentString);
            } catch (RuntimeException e) {
                log.fatal("Error overlaying yaml: " + ExceptionToString.format(e));
                return null;
            }

        } finally {
            if (originalStream != null) {
                try {
                    originalStream.close();
                } catch (IOException e) {
                    log.warn("Error closing stream\n" + ExceptionToString.format(e));
                }
            }
            for (InputStream overridesStream : overridesStreams) {
                if (overridesStream != null) {
                    try {
                        overridesStream.close();
                    } catch (IOException e) {
                        log.warn("Error closing stream\n" + ExceptionToString.format(e));
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static String overlayYaml(String originalYaml, String overridesYaml) {
        if (overridesYaml == null) {
            return originalYaml;
        } else {
            Yaml yaml = new Yaml();
            Map<String, Object> overlaid;
            if (originalYaml != null) {
                overlaid = yaml.loadAs(originalYaml, Map.class);
            } else {
                overlaid = new HashMap<String, Object>();
            }

            Map<String, Object> overrides;
            if (overridesYaml.length() > 0) {
                try {
                    overrides = yaml.loadAs(overridesYaml, Map.class);
                } catch (YAMLException e) {
                    log.error(String.format("Error decoding YAML:\n%s\n", ExceptionToString.format(e)));
                    overrides = new HashMap<String, Object>();
                }
            } else {
                overrides = new HashMap<String, Object>();
            }

            for (Entry<String, Object> entry : overrides.entrySet()) {
                overlaid.put(entry.getKey(), entry.getValue());
            }

            return yaml.dump(overlaid);
        }
    }

}
