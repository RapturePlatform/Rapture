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
package rapture.series.cassandra.config;

import java.util.Map;

import rapture.cassandra.CassandraConstants;
import rapture.common.Messages;
import rapture.series.config.ConfigValidator;
import rapture.series.config.InvalidConfigException;

public class CassandraConfigValidator implements ConfigValidator {
    public static final String ALPHA_NUM_UNDERSCORE = "^[A-Za-z]([A-Za-z]|_|[0-9]){0,31}$";
    Messages messages = new Messages("Cassandra");

    @Override
    public void validate(Map<String, String> configMap) throws InvalidConfigException {
        String keyspace = configMap.get(CassandraConstants.KEYSPACECFG);
        if ((keyspace == null) || (keyspace.length() == 0) || !keyspace.matches(ALPHA_NUM_UNDERSCORE)) {
            throw new InvalidConfigException(messages.getMessage("BadFormat", new String[] { CassandraConstants.KEYSPACECFG, keyspace }).format());
        }
        
        String cf = configMap.get(CassandraConstants.CFCFG);
        if ((cf == null) || (cf.length() == 0) || !cf.matches(ALPHA_NUM_UNDERSCORE)) {
            throw new InvalidConfigException(messages.getMessage("BadFormat", new String[] { CassandraConstants.CFCFG, cf }).format());
        }
        
        String limit = configMap.get(CassandraConstants.OVERFLOWLIMITCFG);
        if (limit != null) {
            try {
                Integer.valueOf(limit);
            } catch(NumberFormatException ex) {
                throw new InvalidConfigException(messages.getMessage("BadInteger", new String[] { CassandraConstants.OVERFLOWLIMITCFG, limit }).format());
            }
        }
    }
}
  