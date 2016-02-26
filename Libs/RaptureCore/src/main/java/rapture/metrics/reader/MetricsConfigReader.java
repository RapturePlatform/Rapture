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
package rapture.metrics.reader;

import rapture.config.MultiValueConfigLoader;

/**
 * @author bardhi
 * @since 2/3/15.
 */
public class MetricsConfigReader {
    public static String getGraphiteUrlScheme() {
        return MultiValueConfigLoader.getConfig("METRICS-graphiteUrlScheme");
    }

    public static int getGraphitePort() {
        return Integer.parseInt(MultiValueConfigLoader.getConfig("METRICS-graphitePort"));
    }

    public static String getGraphiteHost() {
        return MultiValueConfigLoader.getConfig("METRICS-graphiteHost");
    }

    public static String getGrafanaUrlScheme() {
        return MultiValueConfigLoader.getConfig("METRICS-grafanaUrlScheme");
    }

    public static int getGrafanaPort() {
        return Integer.parseInt(MultiValueConfigLoader.getConfig("METRICS-grafanaPort"));
    }

    public static boolean isEnabled() {
        return Boolean.valueOf(MultiValueConfigLoader.getConfig("METRICS-isEnabled"));
    }

    public static Long getMaxTimerSize() {
        String val = MultiValueConfigLoader.getConfig("METRICS-maxTimerSize");
        return Long.parseLong(val) * 3600 * 1000;
    }

    public static Long getCacheFlushTO() {
        return Long.parseLong(MultiValueConfigLoader.getConfig("METRICS-cacheFlushTO"));
    }

    public static Long getMaxCacheSize() {
        return Long.parseLong(MultiValueConfigLoader.getConfig("METRICS-cacheSize"));
    }

    public static String getStatsdHost() {
        return MultiValueConfigLoader.getConfig("METRICS-statsdHost");
    }

    public static int getStatsdPort() {
        return Integer.parseInt(MultiValueConfigLoader.getConfig("METRICS-statsdPort"));
    }
}
