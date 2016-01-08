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
package rapture.metrics;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;

import rapture.common.exception.ExceptionToString;
import rapture.metrics.reader.GraphiteMetricsReader;
import rapture.metrics.reader.MetricsConfigReader;
import rapture.metrics.reader.MetricsReader;
import rapture.metrics.reader.NoOpMetricsReader;
import rapture.metrics.store.DummyMetricsStore;
import rapture.metrics.store.StatsdStore;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author bardhi
 * @since 12/30/14.
 */
public class MetricsFactory {

    public static MetricsService createDefaultService() {
        if (MetricsConfigReader.isEnabled()) {
            return new NonBlockingMetricsService(
                    MetricsConfigReader.getMaxCacheSize(), MetricsConfigReader.getCacheFlushTO(),
                    MetricsConfigReader.getMaxTimerSize(),
                    createDefaultStore(), createDefaultReader());
        } else {
            log.warn(String.format("No statsd host could be found, metrics recording is no-op"));
            return new NoOpMetricsService();
        }
    }

    private static final Logger log = Logger.getLogger(MetricsFactory.class);

    private static MetricsReader createDefaultReader() {
        String scheme = MetricsConfigReader.getGraphiteUrlScheme();
        String host = MetricsConfigReader.getGraphiteHost();
        int graphitePort = MetricsConfigReader.getGraphitePort();

        if (scheme == null || host == null) {
            log.error(String.format("Unable to initialize graphite reader. Cannot determine graphite url. scheme=%s, host=%s", scheme, host));
        } else {
            try {
                URL url = new URL(scheme, host, graphitePort, "");
                return new GraphiteMetricsReader(url);
            } catch (MalformedURLException e) {
                log.error(ExceptionToString.format(e));
            }
        }
        return new NoOpMetricsReader();
    }

    protected static StatsdStore createDefaultStore() {
        return new StatsdStore(MetricsConfigReader.getStatsdHost(), MetricsConfigReader.getStatsdPort());
    }

    @VisibleForTesting
    public static MetricsService createDummyService() {
        return new NonBlockingMetricsService(10, 10000, 10000, new DummyMetricsStore(), new NoOpMetricsReader());
    }

}
