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
package rapture.repo.cassandra;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import rapture.cassandra.CassandraConstants;

import com.google.common.base.Optional;

/**
 * @author bardhi
 * @since 3/10/15.
 */
public class ConfigParser {
    private static final Optional<String> ABSENT = Optional.absent();
    private final Map<String, String> config;
    private String keyspaceName;
    private String cfName;
    private Optional<String> pKeyPrefix;

    public ConfigParser(Map<String, String> config) {
        this.config = config;
    }

    public String getKeyspaceName() {
        return keyspaceName;
    }

    public void parse() {
        keyspaceName = config.get(CassandraConstants.KEYSPACECFG);

        cfName = config.get(CassandraConstants.CFCFG);

        if (StringUtils.isBlank(cfName)) {
            String authority = config.get(CassandraConstants.AUTHORITY);
            if (!StringUtils.isBlank(authority)) {
                int indexDot = authority.indexOf(".");
                if (indexDot != -1) {
                    cfName = authority.substring(0, indexDot);
                    if (authority.length() > indexDot + 1) {
                        pKeyPrefix = Optional.of(authority.substring(indexDot + 1, authority.length()));
                    } else {
                        pKeyPrefix = ABSENT;
                    }
                } else {
                    cfName = authority;
                    pKeyPrefix = ABSENT;
                }

            }
        } else {
            String tempPrefix = config.get(CassandraConstants.PARTITION_KEY_PREFIX);
            if (StringUtils.isBlank(tempPrefix)) {
                pKeyPrefix = ABSENT;
            } else {
                pKeyPrefix = Optional.of(tempPrefix);
            }
        }
    }

    public String getCfName() {
        return cfName;
    }

    public Optional<String> getpKeyPrefix() {
        return pKeyPrefix;
    }
}
