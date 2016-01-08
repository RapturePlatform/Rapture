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

import org.joda.time.Period;

/**
 * @author bardhi
 * @since 1/5/15.
 */
public class NoOpMetricsService implements MetricsService {
    @Override
    public void startMonitoring(String metricClass, String id) {

    }

    @Override
    public void recordTimeDifference(String metricClass, String id, String parameterName) {

    }

    @Override
    public void recordTimeDifference(String parameterName, Long delta) {

    }

    @Override
    public void recordGaugeValue(String parameterName, Long value) {

    }

    @Override
    public void recordGaugeValue(String parameterName, Double value) {

    }

    @Override
    public void recordCount(String parameterName, Long count){

    }

    @Override
    public void stop() {

    }

    @Override
    public Double getMetricAverage(String parameterName, Period period) {
        throw new UnsupportedOperationException("No-op metrics service, not connected to Graphite");
    }

    @Override
    public Long getMetricCount(String parameterName, Period period) {
        throw new UnsupportedOperationException("No-op metrics service, not connected to Graphite");
    }

}
