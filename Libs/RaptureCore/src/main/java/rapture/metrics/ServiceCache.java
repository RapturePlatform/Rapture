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

import rapture.common.exception.ExceptionToString;
import rapture.common.metrics.TimerStartRecord;
import rapture.common.metrics.TimerStartRecordStorage;
import rapture.kernel.ContextFactory;
import rapture.metrics.cache.Count;
import rapture.metrics.cache.Gauge;
import rapture.metrics.cache.Metric;
import rapture.metrics.cache.Timer;
import rapture.metrics.store.MetricsStore;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * @author bardhi
 * @since 1/23/15.
 */
public class ServiceCache {
    private static final int BATCH_SIZE = 50;
    final long MAX_CACHE_SIZE; //max cache size allowed before it is flushed
    final long CACHE_FLUSH_TO; //timeout after which the cache is flushed
    final long EVENT_EXPIRATION_TO; //timeout after which we discard an "end" event

    private static final Logger log = Logger.getLogger(ServiceCache.class);
    private final ScheduledExecutorService cacheExecutor;
    private final Map<String, TimerStartRecord> recordIdToStart;
    private final List<TimerEndRecord> timerEndRecords;

    private final List<Gauge> gauges;
    private final List<Timer> timers;
    private final List<Count> counts;

    private final MetricsStore metricsStore;
    private long recordsLastFlushTime;
    private int runNum;
    private boolean isScheduled;

    public ServiceCache(long maxCacheSize, long cacheFlushTO, long endExpirationTO, MetricsStore metricsStore) {
        MAX_CACHE_SIZE = maxCacheSize;
        CACHE_FLUSH_TO = cacheFlushTO;
        EVENT_EXPIRATION_TO = endExpirationTO;

        isScheduled = false;
        recordIdToStart = new HashMap<>();
        gauges = new LinkedList<>();
        timers = new LinkedList<>();
        counts = new LinkedList<>();

        timerEndRecords = new LinkedList<>();
        recordsLastFlushTime = System.currentTimeMillis();
        runNum = 0;

        this.metricsStore = metricsStore;

        cacheExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("MetricsCacheThread-%d").build());
        cacheExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (isScheduled) {
                    isScheduled = false;
                    flushIfNeeded();

                    if (gauges.size() > 0 || timers.size() > 0 || counts.size() > 0 || timerEndRecords.size() > 0
                            || recordIdToStart.keySet().size() > 0) { //we need to schedule it again soon
                        isScheduled = true;
                    }
                }
            }
        }, 10, 5, TimeUnit.SECONDS);
    }

    /**
     * Flushes any metrics that are currently in memory
     */
    @VisibleForTesting
    void storeAll() {
        List<Timer> calculatedTimers = new LinkedList<>();
        List<TimerEndRecord> toRemove = new LinkedList<>();
        for (TimerEndRecord timerEndRecord : timerEndRecords) {
            String id = timerEndRecord.getId();
            TimerStartRecord startRecord = loadStartRecord(timerEndRecord);
            if (startRecord != null) {
                Long delta = timerEndRecord.getTimestamp() - startRecord.getTimestamp();
                Timer timer = new Timer();
                timer.setDelta(delta);
                timer.setParameterName(timerEndRecord.getParameterName());
                calculatedTimers.add(timer);
                toRemove.add(timerEndRecord);
            } else if (isExpired(timerEndRecord)) {
                log.warn(String.format("Timer end record expired: metricName=%s, id=%s", timerEndRecord.getParameterName(), id));
                toRemove.add(timerEndRecord);
            }
        }

        timerEndRecords.removeAll(toRemove);

        storeMetrics(calculatedTimers);

        storeMetrics(timers);
        timers.clear();

        storeMetrics(gauges);
        gauges.clear();

        storeMetrics(counts);
        counts.clear();

    }

    private <T extends Metric> void storeMetrics(List<T> metrics) {
        if (metrics.size() > 0) {
            printTraceInfo(metrics);
            int i = 0;
            try {
                for (T metric : metrics) {
                    i++;
                    if (i % BATCH_SIZE == 0) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Recorded %s statistics, will sleep then send next batch...", i));
                        }
                        TimeUnit.MILLISECONDS.sleep(100);
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Done sleeping, sending next batch of statistics now..."));
                        }
                    }
                    metric.storeMe(metricsStore);
                }
            } catch (InterruptedException e) {
                log.error("Interrupted while sending statistics: " + ExceptionToString.format(e));
            } catch (Exception e) {
                log.error(ExceptionToString.format(e));
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("Done sending %s metrics of type '%s' to the server", metrics.size(), metrics.get(0).getClass().getSimpleName()));
            }
        }
    }

    private <T extends Metric> void printTraceInfo(List<T> timerList) {
        if (log.isTraceEnabled()) {
            Map<String, Integer> nameToCount = new HashMap<>();
            for (T metric : timerList) {
                String name = metric.getParameterName();
                Integer count = nameToCount.get(name);
                if (count == null) {
                    count = 0;
                }
                count++;
                nameToCount.put(name, count);
            }
            for (Map.Entry<String, Integer> entry : nameToCount.entrySet()) {
                log.trace(String.format("About to record timer metrics: metricName=[%s], count=[%s]", entry.getKey(), entry.getValue()));
            }
            log.trace(String.format("About to send %s timers to the metrics store...", timerList.size()));
        }
    }

    /**
     * Removes from in-memory cache if this marks the end of the monitoring, and returns. If not found there, looks it up in storage. Never deletes from
     * storage as that's expensive and the storage has a TTL so it will get cleaned up periodically
     *
     * @param timerEndRecord
     * @return
     */
    private TimerStartRecord loadStartRecord(TimerEndRecord timerEndRecord) {
        String id = timerEndRecord.getId();
        String className = timerEndRecord.getMetricClass();
        TimerStartRecord record;
        String combinedId = MetricIdFactory.createCombinedId(className, id);
        record = recordIdToStart.get(combinedId);
        if (record != null) {
            return record;
        } else {
            return TimerStartRecordStorage.readByFields(className, id);
        }
    }

    public void addStartMonitoringRecord(final String combinedId, final TimerStartRecord record) {
        cacheExecutor.submit(new Runnable() {
            @Override
            public void run() {
                recordIdToStart.put(combinedId, record);
            }
        });
    }

    public void ensureScheduled() {
        cacheExecutor.submit(new Runnable() {
            @Override
            public void run() {
                isScheduled = true;
            }
        });
    }

    public void addEndRecord(final TimerEndRecord endRecord) {
        cacheExecutor.submit(new Runnable() {
            @Override
            public void run() {
                timerEndRecords.add(endRecord);
            }
        });
    }

    @VisibleForTesting
    void flushIfNeeded() {
        if (runNum % 4 == 0) {
            runNum = 0;
            storeAll();
        }

        runNum++;

        boolean startNeedsFlushing = checkStartNeedsFlushing();
        if (startNeedsFlushing) {
            flushStartRecordsToRapture();
        }
    }

    protected void flushStartRecordsToRapture() {
        long now = System.currentTimeMillis();
        int size = recordIdToStart.keySet().size();
        if (size > 0) {
            log.info(String.format("About to flush %s start records to disk...", size));
            for (TimerStartRecord record : recordIdToStart.values()) {
                TimerStartRecordStorage.add(record, ContextFactory.getKernelUser().getUser(), "Writing from " + getClass().getName());
            }
            recordIdToStart.clear();
            recordsLastFlushTime = now;
        }
        log.info(String.format("Done flushing %s start records to disk...", size));
    }

    protected boolean checkStartNeedsFlushing() {
        int size = recordIdToStart.keySet().size();
        boolean needsFlushing = size > MAX_CACHE_SIZE;

        if (!needsFlushing && size > 0) {
            long now = System.currentTimeMillis();
            long diffMillis = now - recordsLastFlushTime;
            if (diffMillis > CACHE_FLUSH_TO) {
                needsFlushing = true;
            }
        }
        return needsFlushing;
    }

    private boolean isExpired(TimerEndRecord monitoringEvent) {
        return monitoringEvent.getTimestamp() < System.currentTimeMillis() - EVENT_EXPIRATION_TO;
    }

    public void shutdownExecutor() {
        cacheExecutor.shutdown();
    }

    @VisibleForTesting
    public ScheduledExecutorService getCacheExecutor() {
        return cacheExecutor;
    }

    public void addTimer(final String parameterName, final Long delta) {
        cacheExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Timer timer = new Timer();
                timer.setParameterName(parameterName);
                timer.setDelta(delta);
                timers.add(timer);
            }
        });
    }

    public void addGaugeValue(final String parameterName, final Long value) {
        cacheExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Gauge gauge = new Gauge();
                gauge.setParameterName(parameterName);
                gauge.setLongValue(value);
                gauges.add(gauge);

            }
        });
    }

    public void addGaugeValue(final String parameterName, final Double value) {
        cacheExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Gauge gauge = new Gauge();
                gauge.setParameterName(parameterName);
                gauge.setDoubleValue(value);
                gauges.add(gauge);

            }
        });
    }

    public void addCount(final String parameterName, final Long value){
        cacheExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Count count = new Count();
                count.setParameterName(parameterName);
                count.setCount(value);
                counts.add(count);
            }
        });
    }
}
