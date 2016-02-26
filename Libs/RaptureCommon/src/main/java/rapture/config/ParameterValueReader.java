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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import rapture.common.exception.ExceptionToString;

public class ParameterValueReader implements ValueReader {
    private static Logger log = Logger.getLogger(ParameterValueReader.class);

    Map<String, String> envMap;
    Map<String, String> propMap;
    String[] args;
    CommandLine line = null;

    EnvironmentValueReader envReader = new EnvironmentValueReader();
    SystemPropertyValueReader propReader = new SystemPropertyValueReader();
    Options options = new Options();

    public ParameterValueReader(String[] args) {
        super();
        this.args = args;
        envMap = new HashMap<>();
        propMap = new HashMap<>();
    }

    public void addMapping(String opt, boolean hasArg, String description, String envVar, String property) {
        if (envVar != null) {
            envMap.put(opt, envVar);
        }
        if (propMap != null) {
            propMap.put(opt, property);
        }
        options.addOption(opt, hasArg, description);
        line = null;

    };

    @Override
    public String getValue(String parameter) {
        String value = null;
        try {
            if (line == null) {
                line = new PosixParser().parse(options, args);
            }
            if (line.hasOption(parameter)) {
                value = line.getOptionValue(parameter);
            }
        } catch (ParseException e) {
            log.debug("cannot parse command line options " + e.getMessage());
            log.trace(ExceptionToString.format(e));
            e.printStackTrace();
        }

        // If cmd line option not present try environment
        if (value == null) {
            String envName = envMap.get(parameter);
            if (envName != null) value = envReader.getValue(envName);
        }
        
        // If environmental variable not defined try property reader
        if (value == null) {
            String propName = propMap.get(parameter);
            if (propName != null) value = envReader.getValue(propName);
        }
        return value;
    }
}
