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
package rapture.metrics;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.joda.time.Period;

import rapture.common.metrics.TimerStartRecord;
import rapture.metrics.reader.MetricsReader;
import rapture.metrics.store.MetricsStore;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author bardhi
 * @since 1/5/15.
 */
public class NonBlockingMetricsService implements MetricsService {
    private static final Logger log = Logger.getLogger(MetricsService.class);

    private final ServiceCache cache;
    private final MetricsStore metricsStore;
    private final MetricsReader metricsReader;

    @VisibleForTesting
    public NonBlockingMetricsService(long maxCacheSize, long cacheFlushTO, long endExpirationTO, MetricsStore metricsStoreIn, MetricsReader metricsReaderIn) {
        this.metricsStore = metricsStoreIn;
        this.metricsReader = metricsReaderIn;
        cache = new ServiceCache(maxCacheSize, cacheFlushTO, endExpirationTO, metricsStoreIn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startMonitoring(String metricClass, String id) {
        Long timestamp = System.currentTimeMillis();
        addStartMonitoringRecord(metricClass, id, timestamp);
    }

    private void addStartMonitoringRecord(final String metricName, final String id, Long timestamp) {
        final TimerStartRecord record = new TimerStartRecord();
        record.setId(id);
        record.setMetricName(metricName);
        record.setTimestamp(timestamp);
        cache.addStartMonitoringRecord(MetricIdFactory.createCombinedId(metricName, id), record);
        cache.ensureScheduled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordTimeDifference(String metricClass, String id, String parameterName) {
        Long timestamp = System.currentTimeMillis();
        TimerEndRecord timerEndRecord = new TimerEndRecord();
        timerEndRecord.setMetricClass(metricClass);
        timerEndRecord.setParameterName(parameterName);
        timerEndRecord.setId(id);
        timerEndRecord.setTimestamp(timestamp);
        cache.addEndRecord(timerEndRecord);
        cache.ensureScheduled();
    }

    @Override
    public void recordTimeDifference(final String parameterName, final Long delta) {
        cache.addTimer(parameterName, delta);
        cache.ensureScheduled();
    }

    @Override
    public void recordGaugeValue(String parameterName, Long value) {
        cache.addGaugeValue(parameterName, value);
        cache.ensureScheduled();
    }

    @Override
    public void recordGaugeValue(String parameterName, Double value) {
        cache.addGaugeValue(parameterName, value);
        cache.ensureScheduled();
    }

    @Override
    public void recordCount(String parameterName, Long count) {
        cache.addCount(parameterName, count);
        cache.ensureScheduled();
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
        metricsStore.stop();
        cache.shutdownExecutor();
    }

    @Override
    public Double getMetricAverage(String parameterName, Period period) throws IOException {
        return metricsReader.getMetricAverage(parameterName, period);
    }

    @Override
    public Long getMetricCount(String parameterName, Period period) throws IOException {
        return metricsReader.getMetricCount(parameterName, period);
    }

    @VisibleForTesting
    ServiceCache getCache() {
        return cache;
    }
}
