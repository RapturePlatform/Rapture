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
package rapture.event.generator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import rapture.common.TimeProcessorStatus;
import rapture.common.TimeProcessorStatusStorage;
import rapture.common.model.RaptureNetwork;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

/**
 * The time processor runs in the context of a Rapture kernel. It does these
 * things:
 * 
 * 1. Manages the concept of a "last time processed" - a document keyed to the
 * RaptureNetwork id that has a date in it. 2. When it starts up it loads that
 * date (if it exists) and optionally runs itself for every second between that
 * date and "now" 3. Each tick it stores the new date (so this status is
 * ephemeral). 4. For each tick it uses the eventgenerator to get strings of
 * events. 4.1 For each one, it looks for a script with the same path (but with
 * the "system/timeprocessor" prefixed). If that is present, it runs that script
 * with the calendar object passed in. 4.2 It also fires an event with the
 * calendar object passed in.
 * 
 * @author alanmoore
 * @deprecated Use ScheduleServer
 * 
 */
public class TimeProcessor {
    private static Logger log = Logger.getLogger(TimeProcessor.class);
    private DateTime currentTick;
    private TimeProcessorStatus status;
    private ExecutorService executor = Executors.newFixedThreadPool(10);

    @Deprecated
    public void initialize() {
        // Load the current tick point. If it isn't present, initialize the
        // currentTick to "now" and run once
        log.info("Initializing processor");
        RaptureNetwork network = Kernel.getEnvironment().getNetworkInfo(ContextFactory.getKernelUser());
        try {
            status = TimeProcessorStatusStorage.readByFields(network.getNetworkId());
        } catch (Exception e) {
            log.error("Got error when retrieving status, assuming we can move to now " + e.getMessage());
            status = null;
        }
        if (status == null) {
            log.info("No saved processor state, starting from current time");
            status = new TimeProcessorStatus();
            status.setNetwork(network.getNetworkId());
            status.setProcessingServer(Kernel.getEnvironment().getThisServer(ContextFactory.getKernelUser()).getServerId());
            status.setWhen(new DateTime().getMillis());
        }

        currentTick = new DateTime(status.getWhen());

        maybeTick(false);
    }

    private void saveCurrentStatus() {
        status.setWhen(currentTick.getMillis());
        TimeProcessorStatusStorage.add(status, "$admin", "Advance tick");
    }

    @Deprecated
    public void runForCurrentTick(boolean periodic) {
        List<String> events = EventGenerator.generateEventNames(currentTick, periodic);
        if (!events.isEmpty()) {
            // log.info("Would process " + events);
            for (String event : events) {
                process(event);
            }
        }
        saveCurrentStatus();
    }

    private Cache<String, Boolean> knowledgeCache = CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(5, TimeUnit.MINUTES).build();

    private Optional<Boolean> isKnown(String ref) {
        Boolean knowledge = knowledgeCache.getIfPresent(ref);
        if (knowledge == null) {
            return Optional.absent();
        } else {
            return Optional.of(knowledge);
        }
    }

    private void putInKnowledge(String ref, Boolean present) {
        knowledgeCache.put(ref, present);
    }

    private void process(final String event) {
        // What does process mean?
        // 1. if a script exists with the name /system/timeprocessor + event,
        // run that script
        // 2. if an event exists with the name event, fire that event
        try {
            log.debug("Looking at " + event);
            final String scriptName = "script://system/timeprocessor" + event;
            final String tick = "" + currentTick.getMillis();
            boolean runScript = false;
            if (isKnown(scriptName).or(false)) {
                runScript = true;
            } else if (Kernel.getScript().doesScriptExist(ContextFactory.getKernelUser(), scriptName)) {
                runScript = true;
                putInKnowledge(scriptName, true);
            } else {
                putInKnowledge(scriptName, false);
            }
            
            if (runScript) {
                executor.execute(new Runnable() {

                    @Override
                    public void run() {
                        // log.info("Script for event exists, running");
                        Map<String, String> parameters = new HashMap<String, String>();
                        parameters.put("tick", tick);
                        Kernel.getScript().runScript(ContextFactory.getKernelUser(), scriptName, parameters);
                    }

                });

            } else {              
                if (isKnown(event).or(true)) {
                    executor.execute(new Runnable() {

                        @Override
                        public void run() {
                            if (Kernel.getEvent().runEvent(ContextFactory.getKernelUser(), event, "", tick)) {
                                putInKnowledge(event, true);
                            } else {
                                putInKnowledge(event, false);
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error("Could not process event " + event + " with error " + e.getMessage() + " but am continuing to look for other events");
        }
    }

    @Deprecated
    public void maybeTick(boolean periodic) {
        boolean caughtUp = false;

        // If periodic is false, catch up mode is usually happening. Cache the
        // script tree?

        while (!caughtUp) {
            DateTime now = new DateTime();
            if (now.isAfter(currentTick)) {
                runForCurrentTick(periodic);
                currentTick = currentTick.plusMinutes(1);
            } else {
                caughtUp = true;
                break;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
