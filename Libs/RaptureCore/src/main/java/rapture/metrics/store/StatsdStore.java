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
package rapture.metrics.store;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import rapture.common.exception.ExceptionToString;

import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.StatsDClientErrorHandler;
import com.timgroup.statsd.StatsDClientException;

/**
 * Try to not use this directly, use {@link rapture.metrics.MetricsService} instead
 *
 * @author bardhi
 * @since 12/30/14.
 */
public class StatsdStore implements MetricsStore {

    private static final Logger log = Logger.getLogger(StatsdStore.class);

    private final StatsDClient statsd;

    public StatsdStore(String statsdHost, int port) {
        statsd = createClient(statsdHost, port);
    }

    private StatsDClient createClient(String statsdHost, int port) {
        StatsDClientErrorHandler errorHandler = new StatsDClientErrorHandler() {
            @Override
            public void handle(Exception exception) {
                log.error("Error sending to statsd: " + ExceptionToString.format(exception));
            }
        };
        if (statsdHost == null) {
            log.fatal("Error, statsd host not set. It needs to be set in RaptureMETRICS.cfg");
            return new NoOpStatsDClient();
        } else {
            try {
                InetAddress address = InetAddress.getByName(statsdHost);
                String ip = address.getHostAddress();
                log.info(String.format("statsd host=%s, ip=%s", statsdHost, ip));
                return createClient(ip, port, errorHandler);
            } catch (UnknownHostException e) {
                log.error(String.format("Error resolving statsd host ip. hostname=%s - %s", statsdHost, ExceptionToString.format(e)));
                return createClient(statsdHost, port, errorHandler);
            }
        }
    }

    private StatsDClient createClient(String address, int port, StatsDClientErrorHandler errorHandler) {
        try {
            return new NonBlockingStatsDClient("rapture", address, port, errorHandler);
        } catch (StatsDClientException e1) {
            log.error(String.format("Error initializing statsd client with host %s - %s", address, ExceptionToString.format(e1)));
            return new NoOpStatsDClient();
        }
    }

    public void recordDelta(String parameterName, Long delta) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Recording time. parameterName: %s; delta: %s", parameterName, delta));
        }
        statsd.recordExecutionTime(parameterName, delta);
    }

    @Override
    public void recordGaugeValue(String parameterName, Long value) {
        statsd.recordGaugeValue(parameterName, value);
    }

    @Override
    public void recordGaugeValue(String parameterName, Double value) {
        statsd.recordGaugeValue(parameterName, value);
    }

    @Override
    public void recordCount(String parameterName, Long value){
        statsd.count(parameterName, value);
    }

    @Override
    public void stop() {
        statsd.stop();
    }

}
