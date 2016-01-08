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
package rapture.series.config;

import java.net.HttpURLConnection;
import java.util.Map;

import org.antlr.runtime.RecognitionException;

import rapture.common.exception.RaptureExceptionFactory;
import rapture.generated.SRapGenLexer;
import rapture.generated.SRapGenParser;
import rapture.series.SeriesFactory;

import com.google.common.collect.ImmutableMap;

public class ConfigValidatorService {
    private static final Map<Integer, String> typeToValidatorClass = ImmutableMap.of(
	    SRapGenLexer.MEMORY, "rapture.series.config.MemoryConfigValidator",
            SRapGenLexer.CASSANDRA, "rapture.series.cassandra.config.CassandraConfigValidator",
	    SRapGenLexer.CSV, "rapture.series.config.CSVConfigValidator",
            SRapGenLexer.MONGO, "rapture.series.mongo.config.MongoConfigValidator",
	    SRapGenLexer.FILE, "rapture.series.config.FileConfigValidator");

    public static void validateConfig(String config) throws InvalidConfigException, RecognitionException {
        validateConfig(SeriesFactory.parseConfig(config));
    }
        
    public static void validateConfig(SRapGenParser parser) throws InvalidConfigException, RecognitionException {
        int storeType = parser.getStoreType();
        ConfigValidator validator = getValidator(storeType);
        if (validator == null) {
            throw new InvalidConfigException("Unable to validate config for this type of series repo");
        }
        Map<String, String> configMap = parser.getConfig().getConfig();
        validator.validate(configMap);
    }

    private static ConfigValidator getValidator(int storeType) {
        try {
            String className = typeToValidatorClass.get(storeType);
            if (className != null) {
                Class<?> clazz = Class.forName(className);
                Object configObject = clazz.newInstance();
                if (configObject instanceof ConfigValidator) {
                    return (ConfigValidator) configObject;
                }
            }
        } catch (ClassNotFoundException| InstantiationException |IllegalAccessException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error reading series config", e);
        }
        return null;
    }
}
