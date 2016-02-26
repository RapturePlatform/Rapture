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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;

import rapture.common.exception.ExceptionToString;

public class JavaPropertiesConfigReader extends ConfigFileReader<Map<Object, Object>> {
    private static final Logger log = Logger.getLogger(JavaPropertiesConfigReader.class);

    public JavaPropertiesConfigReader(String configFileName) {
        super(configFileName);
    }

    @Override
    protected Map<Object, Object> doOverlay(File overlaidFile, InputStream originalStream, InputStream... overridesStreams) {
        try {
            Map<Object, Object> extraProperties = new HashMap<Object, Object>();
            boolean isOverride = false;
            for (InputStream overridesStream : overridesStreams) {
                if (overridesStream != null) {
                    Properties overrides = new Properties();
                    try {
                        overrides.load(overridesStream);
                        for (Entry<Object, Object> override : overrides.entrySet()) {
                            extraProperties.put(override.getKey(), override.getValue());
                        }
                        isOverride = true;
                    } catch (IOException e) {
                        log.error(String.format("Error reading from override file:\n%s", ExceptionToString.format(e)));
                    }
                }
            }

            // Copy original into destination, add any extra properties
            Properties configProperties = new Properties();
            if (originalStream != null) {
                try {
                    configProperties.load(originalStream);
                } catch (IOException e) {
                    log.error(String.format("Error reading from original file:\n%s", ExceptionToString.format(e)));
                }
            }

            FileWriter out = null;
            try {
                out = new FileWriter(overlaidFile);
                String comment;
                if (isOverride) {
                    comment = "There were overrides";
                } else {
                    comment = "No overrides found.";
                }
                configProperties.putAll(extraProperties);
                configProperties.store(out, comment);
                return configProperties;
            } catch (IOException e) {
                log.error(String.format("Error creating properties file\n%s", ExceptionToString.format(e)));
                return null;
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        log.warn("Error closing stream\n" + ExceptionToString.format(e));
                    }
                }
            }

        } finally {
            for (InputStream overridesStream : overridesStreams) {
                if (overridesStream != null) {
                    try {
                        overridesStream.close();
                    } catch (IOException e) {
                        log.warn("Error closing stream\n" + ExceptionToString.format(e));
                    }
                }
            }
            
            if (originalStream != null) {
                try {
                    originalStream.close();
                } catch (IOException e) {
                    log.warn("Error closing stream\n" + ExceptionToString.format(e));
                }
            }

        }

    }
}
