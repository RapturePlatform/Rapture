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

import org.joda.time.Period;

/**
 * @author bardhi
 * @since 12/2/14.
 */
public interface MetricsService {

    /**
     * Use this to measure how long certain events take. Call this function to indicate that it should start monitoring a unique metric class + id
     * combination. This tells the system to start monitoring this combination. To record a time between the call to this function and after something is
     * complete, call {@link #recordTimeDifference(String, String, String)}. The event must be relatively short, ranging from milliseconds to a few hours
     * <p>
     *
     * @param metricClass The metric class is a string that together with the unique id, can be used to record metrics for a particular area we are measuring.
     * @param id          A unique id for this occurrence of the event that we're measuring, e.g. the work order id (WO00001)
     */
    void startMonitoring(String metricClass, String id);

    /**
     * Call this to record the time difference between now and the time we started monitoring a metric class + id combination. The metric class and id must
     * match the metric class and id already being monitored, which means that {@link #startMonitoring(String, String)} must have already been called.
     * This method can be called multiple times for the same start event-unique id combination.
     * <p>
     *
     * @param metricClass   The metric class we are monitoring
     * @param id            The unique id for this metric class
     * @param parameterName The name of the parameter to record.
     *                      <p>
     *                      Use this convention for parameterName: <namespace>.<instrumented section>.<target (noun)>.<action
     *                      (past tense verb)>
     *                      E.g. poms.bag.order.written
     */
    void recordTimeDifference(String metricClass, String id, String parameterName);

    /**
     * Record a time difference, if you know the difference. This is different from {@link #recordTimeDifference(String, String, String)}, as it requires you
     * knowing the exact delta to enter. If you do not want to calculate the delta yourself, you should use {@link #startMonitoring(String, String)} and
     * {@link #recordTimeDifference(String, String, String)} instead
     *
     * @param parameterName
     * @param delta
     */
    void recordTimeDifference(String parameterName, Long delta);

    /**
     * Call this to record a gauge value. A gauge is an arbitrary numeric value. Gauges are typically used for measured values like temperatures or current
     * memory usage, but also "counts" that can go up and down, like the number of database connections currently in use.
     *
     * @param parameterName
     * @param value
     */
    void recordGaugeValue(String parameterName, Long value);

    /**
     * Call this to record a gauge value. A gauge is an arbitrary numeric value. Gauges are typically used for measured values like temperatures or current
     * memory usage, but also "counts" that can go up and down, like the number of database connections currently in use.
     *
     * @param parameterName
     * @param value
     */
    void recordGaugeValue(String parameterName, Double value);

    /**
     * Call this to record a count value. A count is a simple counter that can be increased or decreased. Counts are typically used to measure values such
     * as running totals, like the number of total calls to a method over a certain period of time.
     *
     * @param parameterName
     * @param count
     */
    void recordCount(String parameterName, Long count);

    /**
     * Stop this service and shut down any possible connections or thread executors
     */
    void stop();

    /**
     * Return the average value for this metric over the specified amount of time
     *
     * @param parameterName
     * @param period
     * @return
     */
    Double getMetricAverage(String parameterName, Period period) throws IOException;

    /**
     * Get the count of how many times this metric has been hit in the specified time period
     *
     * @param parameterName
     * @param period
     * @return
     */
    Long getMetricCount(String parameterName, Period period) throws IOException;
}