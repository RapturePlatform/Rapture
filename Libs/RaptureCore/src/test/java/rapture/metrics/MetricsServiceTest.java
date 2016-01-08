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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import rapture.common.metrics.TimerStartRecordStorage;
import rapture.metrics.reader.MetricsReader;
import rapture.metrics.reader.NoOpMetricsReader;
import rapture.metrics.store.DummyMetricsStore;

public class MetricsServiceTest {

    private static final long HUGE = 100000L;
    private MetricsReader metricsReader = new NoOpMetricsReader();

    @Test
    public void testEventsCache() throws Exception {
        long maxCacheSize = 2;
        final List<String> metricNames = new LinkedList<>();
        final List<Long> deltas = new LinkedList<>();

        final NonBlockingMetricsService service = new NonBlockingMetricsService(maxCacheSize, HUGE, HUGE, new DummyMetricsStore() {
            @Override
            public void recordDelta(String parameterName, Long delta) {
                metricNames.add(parameterName);
                deltas.add(delta);
            }
        }, metricsReader);

        String metricClass = "a.b";
        String id1 = "id1";
        String id2 = "id2";

        service.startMonitoring(metricClass, id1);
        service.startMonitoring(metricClass, id2);
        MetricsTestHelper.flushIfNeeded(service);
        assertEquals(0, metricNames.size());
        assertEquals(0, deltas.size());

        service.recordTimeDifference(metricClass, id1, "metric.1");
        service.recordTimeDifference(metricClass, id2, "metric.2");
        MetricsTestHelper.flushIfNeeded(service);
        assertEquals(2, metricNames.size());
        assertEquals(2, deltas.size());

        assertNull("id1 null", TimerStartRecordStorage.readByFields("a.b.", id1));
        assertNull("id2 null", TimerStartRecordStorage.readByFields("a.b.", id2));
    }

    @Test
    public void testEventsCacheOverflow() throws InterruptedException {
        long maxCacheSize = 2;
        final List<String> aspects = new LinkedList<>();
        final List<Long> deltas = new LinkedList<>();

        final NonBlockingMetricsService service = new NonBlockingMetricsService(maxCacheSize, HUGE, HUGE, new DummyMetricsStore() {
            @Override
            public void recordDelta(String parameterName, Long delta) {
                aspects.add(parameterName);
                deltas.add(delta);
            }
        }, metricsReader);

        String metricClass = "a.b";
        String idPrefix = "idOverflow";
        for (int i = 0; i < maxCacheSize + 1; i++) {
            service.startMonitoring(metricClass, idPrefix + i);
        }
        MetricsTestHelper.flushIfNeeded(service);
        for (int i = 0; i < maxCacheSize + 1; i++) {
            service.recordTimeDifference(metricClass, idPrefix + i, "metric.name");
        }
        MetricsTestHelper.flushIfNeeded(service);
        assertEquals(maxCacheSize + 1, aspects.size());
        assertEquals(maxCacheSize + 1, deltas.size());
        for (int i = 0; i < maxCacheSize + 1; i++) {
            String id = idPrefix + i;
            assertNotNull(String.format("%s %s not null", metricClass, id), TimerStartRecordStorage.readByFields(metricClass, id));

        }
    }

    @Test
    public void testMultiNode() throws InterruptedException {
        long maxCacheSize = 2;
        final List<String> metricNames = new LinkedList<>();
        final List<Long> deltas = new LinkedList<>();

        NonBlockingMetricsService startService = new NonBlockingMetricsService(maxCacheSize, HUGE, HUGE, new DummyMetricsStore(), metricsReader);

        NonBlockingMetricsService recordService = new NonBlockingMetricsService(maxCacheSize, HUGE, HUGE, new DummyMetricsStore() {
            @Override
            public void recordDelta(String parameterName, Long delta) {
                metricNames.add(parameterName);
                deltas.add(delta);
            }
        }, metricsReader);
        /**
         * First try running without flushing cache. The stop executor has no way of knowing about the start of the events so it won't pick them up
         */
        String metricClass = "a.b";
        for (int i = 0; i < maxCacheSize; i++) {
            startService.startMonitoring(metricClass, "idMultiNode" + i);
        }
        MetricsTestHelper.flushIfNeeded(startService);

        for (int i = 0; i < maxCacheSize; i++) {
            recordService.recordTimeDifference(metricClass, "idMultiNode" + i, "my.metric.name");
        }
        MetricsTestHelper.flushIfNeeded(startService);
        MetricsTestHelper.flushIfNeeded(recordService);
        assertEquals(0, metricNames.size());
        assertEquals(0, deltas.size());

        //now make it flush the cache, and then the other executor should be able to pick it up
        startService.startMonitoring(metricClass, "idMultiNode" + maxCacheSize);
        MetricsTestHelper.flushIfNeeded(startService);
        MetricsTestHelper.flushIfNeeded(recordService);
        assertEquals(maxCacheSize, metricNames.size());
        assertEquals(maxCacheSize, deltas.size());

    }

    @Test
    public void testStopEvent() throws Exception {

    }

    @Test
    public void testRunPeriodically() throws Exception {

    }

    @Test
    public void testFlushStartRecords() throws Exception {

    }

    @Test
    public void testFlushSize() throws Exception {
        long maxCacheSize = 10;
        final NonBlockingMetricsService service = new NonBlockingMetricsService(maxCacheSize, HUGE, HUGE, new DummyMetricsStore(), metricsReader);
        String metricName = "a.b";
        String eventId = "c";
        for (int i = 0; i < maxCacheSize + 100; i++) {
            service.startMonitoring(metricName, eventId); //same event 100 times, should not fill cache
        }
        MetricsTestHelper.flushIfNeeded(service);
        assertNull("not flushed", TimerStartRecordStorage.readByFields("a.b", "c"));

        for (int i = 0; i < maxCacheSize - 1; i++) {
            service.startMonitoring(metricName, eventId + i);
        }
        MetricsTestHelper.flushIfNeeded(service);
        assertNull("not flushed", TimerStartRecordStorage.readByFields("a.b", "c"));

        service.startMonitoring(metricName, eventId + maxCacheSize); //now full!
        MetricsTestHelper.flushIfNeeded(service);
        assertNotNull("c is finally flushed", TimerStartRecordStorage.readByFields("a.b", "c"));
    }

    @Test
    public void testFlushTime() throws Exception {
        long maxCacheSize = 10;
        NonBlockingMetricsService service = new NonBlockingMetricsService(maxCacheSize, 1L, 1L, new DummyMetricsStore(), metricsReader);
        MetricsTestHelper.flushIfNeeded(service);
        assertFalse(service.getCache().checkStartNeedsFlushing());
        String metricName = "a.b";
        String eventId = "c";
        service.startMonitoring(metricName, eventId);
        Thread.sleep(10);
        MetricsTestHelper.flushIfNeeded(service);
        assertNotNull("is flushed", TimerStartRecordStorage.readByFields("a.b", "c"));

    }

    @Test
    public void testNonStoppingMetrics() throws Exception {
        long maxCacheSize = 2;
        final Set<String> metricNames = new HashSet<>();
        final List<Long> deltas = new LinkedList<>();

        final NonBlockingMetricsService service = new NonBlockingMetricsService(maxCacheSize, HUGE, HUGE, new DummyMetricsStore() {
            @Override
            public void recordDelta(String parameterName, Long delta) {
                metricNames.add(parameterName);
                deltas.add(delta);
            }
        }, metricsReader);

        String metricClass = "non.stopping.a.b";
        String id = "id";

        service.startMonitoring(metricClass, id);
        MetricsTestHelper.flushIfNeeded(service);
        assertEquals(0, metricNames.size());
        assertEquals(0, deltas.size());

        service.recordTimeDifference(metricClass, id, "non.stopping.r1");
        service.recordTimeDifference(metricClass, id, "non.stopping.r2");
        MetricsTestHelper.flushIfNeeded(service);
        assertEquals(2, metricNames.size());
        assertEquals(2, deltas.size());

        service.recordTimeDifference(metricClass, id, "name3");
        MetricsTestHelper.flushIfNeeded(service);
        assertEquals(3, metricNames.size());

        service.recordTimeDifference(metricClass, id, "name3");
        MetricsTestHelper.flushIfNeeded(service);
        assertEquals(3, metricNames.size());
    }

    @Test
    public void testTimers() throws Exception {
        long maxCacheSize = 2;
        final List<String> parameterNames = new LinkedList<>();
        final List<Long> deltas = new LinkedList<>();
        final List<String> gauges = new LinkedList<>();

        final NonBlockingMetricsService service = new NonBlockingMetricsService(maxCacheSize, HUGE, HUGE, new DummyMetricsStore() {
            @Override
            public void recordDelta(String parameterName, Long delta) {
                parameterNames.add(parameterName);
                deltas.add(delta);
            }

            @Override
            public void recordGaugeValue(String parameterName, Long value) {
                parameterNames.add(parameterName);
                gauges.add("" + value);
            }

            @Override
            public void recordGaugeValue(String parameterName, Double value) {
                parameterNames.add(parameterName);
                gauges.add("" + value);
            }
        }, metricsReader);

        long delta1 = 1L;
        long delta2 = 3411L;
        String parameterName1 = "metric.1";
        service.recordTimeDifference(parameterName1, delta1);
        String parameterName2 = "metric.2";
        service.recordTimeDifference(parameterName2, delta2);
        MetricsTestHelper.flushIfNeeded(service);

        assertEquals(2, parameterNames.size());
        assertEquals(2, deltas.size());
        assertEquals(0, gauges.size());
        assertTrue("deltas are " + deltas, deltas.contains(delta1));
        assertTrue("deltas are " + deltas, deltas.contains(delta2));

        assertTrue("parameters contain " + parameterNames, parameterNames.contains(parameterName1));
        assertTrue("parameters contain " + parameterNames, parameterNames.contains(parameterName2));

    }

    @Test
    public void testTimersMixed() throws Exception {
        long maxCacheSize = 2;
        final List<String> parameterNames = new LinkedList<>();
        final List<Long> deltas = new LinkedList<>();

        final List<String> gauges = new LinkedList<>();

        final NonBlockingMetricsService service = new NonBlockingMetricsService(maxCacheSize, HUGE, HUGE, new DummyMetricsStore() {
            @Override
            public void recordDelta(String parameterName, Long delta) {
                parameterNames.add(parameterName);
                deltas.add(delta);
            }

            @Override
            public void recordGaugeValue(String parameterName, Long value) {
                parameterNames.add(parameterName);
                gauges.add("" + value);
            }

            @Override
            public void recordGaugeValue(String parameterName, Double value) {
                parameterNames.add(parameterName);
                gauges.add("" + value);
            }
        }, metricsReader);

        String metricClass = "a.b";
        String id1 = "id1";
        String id2 = "id2";

        service.startMonitoring(metricClass, id1);
        service.startMonitoring(metricClass, id2);
        MetricsTestHelper.flushIfNeeded(service);
        assertEquals(0, parameterNames.size());
        assertEquals(0, deltas.size());

        service.recordTimeDifference(metricClass, id1, "metric.1");
        service.recordTimeDifference(metricClass, id2, "metric.2");

        long delta1 = 1L;
        long delta2 = 3411L;
        String parameterName1 = "metric.3";
        service.recordTimeDifference(parameterName1, delta1);
        String parameterName2 = "metric.4";
        service.recordTimeDifference(parameterName2, delta2);

        MetricsTestHelper.flushIfNeeded(service);

        assertEquals(4, parameterNames.size());
        assertEquals(4, deltas.size());
        assertEquals(0, gauges.size());
        assertTrue("deltas are " + deltas, deltas.contains(delta1));
        assertTrue("deltas are " + deltas, deltas.contains(delta2));

        assertTrue("parameters contain " + parameterNames, parameterNames.contains(parameterName1));
        assertTrue("parameters contain " + parameterNames, parameterNames.contains(parameterName2));

        assertNull("id1 null", TimerStartRecordStorage.readByFields("a.b.", id1));
        assertNull("id2 null", TimerStartRecordStorage.readByFields("a.b.", id2));
    }

    @Test
     public void testGauges() throws Exception {
        long maxCacheSize = 2;
        final List<String> parameterNames = new LinkedList<>();
        final List<Long> deltas = new LinkedList<>();
        final List<String> gauges = new LinkedList<>();

        final NonBlockingMetricsService service = new NonBlockingMetricsService(maxCacheSize, HUGE, HUGE, new DummyMetricsStore() {
            @Override
            public void recordDelta(String parameterName, Long delta) {
                parameterNames.add(parameterName);
                deltas.add(delta);
            }

            @Override
            public void recordGaugeValue(String parameterName, Long value) {
                parameterNames.add(parameterName);
                gauges.add("" + value);
            }

            @Override
            public void recordGaugeValue(String parameterName, Double value) {
                parameterNames.add(parameterName);
                gauges.add("" + value);
            }
        }, metricsReader);

        long gauge1 = 1L;
        String p1 = "p1";
        service.recordGaugeValue(p1, gauge1);
        double gauge2 = 2.3;
        String p2 = "p2";
        service.recordGaugeValue(p2, gauge2);

        MetricsTestHelper.flushIfNeeded(service);
        assertEquals(2, parameterNames.size());
        assertEquals(0, deltas.size());
        assertEquals(2, gauges.size());
        assertTrue("gauges are " + gauges, gauges.contains("" + gauge1));
        assertTrue("gauges are " + gauges, gauges.contains("" + gauge2));

        assertTrue("parameters contain " + parameterNames, parameterNames.contains(p1));
        assertTrue("parameters contain " + parameterNames, parameterNames.contains(p2));

    }

    @Test
    public void testCounts() throws Exception {
        long maxCacheSize = 2;
        final List<String> parameterNames = new LinkedList<>();
        final List<Long> deltas = new LinkedList<>();
        final List<String> gauges = new LinkedList<>();
        final List<Long> counts = new LinkedList<>();

        final NonBlockingMetricsService service = new NonBlockingMetricsService(maxCacheSize, HUGE, HUGE, new DummyMetricsStore() {
            @Override
            public void recordDelta(String parameterName, Long delta) {
                parameterNames.add(parameterName);
                deltas.add(delta);
            }

            @Override
            public void recordGaugeValue(String parameterName, Long value) {
                parameterNames.add(parameterName);
                gauges.add("" + value);
            }

            @Override
            public void recordGaugeValue(String parameterName, Double value) {
                parameterNames.add(parameterName);
                gauges.add("" + value);
            }

            @Override
            public void recordCount(String parameterName, Long count){
                parameterNames.add(parameterName);
                counts.add(count);
            }
        }, metricsReader);

        long count1 = 1L;
        String p1 = "p1";
        service.recordCount(p1, count1);
        long count2 = 2l;
        String p2 = "p2";
        service.recordCount(p2, count2);

        MetricsTestHelper.flushIfNeeded(service);
        assertEquals(2, parameterNames.size());
        assertEquals(0, deltas.size());
        assertEquals(0, gauges.size());
        assertEquals(2, counts.size());
        assertTrue("counts are " + counts, counts.contains(count1));
        assertTrue("counts are " + counts, counts.contains(count2));

        assertTrue("parameters contain " + parameterNames, parameterNames.contains(p1));
        assertTrue("parameters contain " + parameterNames, parameterNames.contains(p2));

    }

    @Test
    public void testGaugesAndTimers() throws Exception {
        long maxCacheSize = 2;
        final List<String> parameterNames = new LinkedList<>();
        final List<Long> deltas = new LinkedList<>();
        final List<String> gauges = new LinkedList<>();

        final NonBlockingMetricsService service = new NonBlockingMetricsService(maxCacheSize, HUGE, HUGE, new DummyMetricsStore() {
            @Override
            public void recordDelta(String parameterName, Long delta) {
                parameterNames.add(parameterName);
                deltas.add(delta);
            }

            @Override
            public void recordGaugeValue(String parameterName, Long value) {
                parameterNames.add(parameterName);
                gauges.add("" + value);
            }

            @Override
            public void recordGaugeValue(String parameterName, Double value) {
                parameterNames.add(parameterName);
                gauges.add("" + value);
            }
        }, metricsReader);

        long gauge1 = 1L;
        String p1 = "p1";
        service.recordGaugeValue(p1, gauge1);
        double gauge2 = 2.3;
        String p2 = "p2";
        service.recordGaugeValue(p2, gauge2);

        long delta1 = 1L;
        long delta2 = 3411L;
        String p3 = "metric.3";
        service.recordTimeDifference(p3, delta1);
        String p4 = "metric.4";
        service.recordTimeDifference(p4, delta2);

        MetricsTestHelper.flushIfNeeded(service);
        assertEquals("parameters are " + parameterNames, 4, parameterNames.size());
        assertEquals(2, deltas.size());
        assertEquals(2, gauges.size());

        assertTrue("gauges are " + gauges, gauges.contains("" + gauge1));
        assertTrue("gauges are " + gauges, gauges.contains("" + gauge2));

        assertTrue("deltas are " + deltas, deltas.contains(delta1));
        assertTrue("deltas are " + deltas, deltas.contains(delta2));

        assertTrue("parameters contain " + parameterNames, parameterNames.contains(p1));
        assertTrue("parameters contain " + parameterNames, parameterNames.contains(p2));
        assertTrue("parameters contain " + parameterNames, parameterNames.contains(p3));
        assertTrue("parameters contain " + parameterNames, parameterNames.contains(p4));

    }

}