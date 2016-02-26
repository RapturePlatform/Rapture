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
package rapture.log.management;

import rapture.config.MultiValueConfigLoader;

/**
 * @author bardhi
 * @since 2/17/15.
 */
public class LogManagementConfigReader {

    /**
     * Get the name of the log management system for the configured implementation
     * @return
     */
    public static String getSystemName() {
        return "Logstash";
    }

    public static String getLogHostFqdn() {
        return MultiValueConfigLoader.getConfig("LOGSTASH-logHostFqdn");
    }

    public static String getWebAppScheme() {
        return MultiValueConfigLoader.getConfig("LOGSTASH-webAppScheme");
    }

    public static int getWebAppPort() {
        String val = MultiValueConfigLoader.getConfig("LOGSTASH-webAppPort");
        return Integer.parseInt(val);
    }

    public static boolean isEnabled() {
        return Boolean.valueOf(MultiValueConfigLoader.getConfig("LOGSTASH-isEnabled"));
    }

    /**
     * Find out how many days worth of log data is available in logstash
     *
     * @return
     */
    public static int getRetentionPeriod() {
        String val = MultiValueConfigLoader.getConfig("LOGSTASH-retentionPeriod");
        return Integer.parseInt(val);
    }

    public static int getApiPort() {
        return Integer.parseInt(MultiValueConfigLoader.getConfig("LOGSTASH-apiPort"));
    }
}
