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

import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureScript;
import rapture.common.TimedEventRecord;
import rapture.common.exception.ExceptionToString;
import rapture.common.model.RaptureEvent;
import rapture.common.model.RaptureEventScript;
import rapture.common.model.RaptureEventWorkflow;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.script.reflex.ReflexRaptureScript;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;

import reflex.ReflexParser;

/**
 * Generates a list of events that would be "fired" by TimeProcessor/TimeServer
 * between two dates Used to generate a UI
 *
 * @author alanmoore
 */
public class RangedEventGenerator {
    private static final Logger log = Logger.getLogger(RangedEventGenerator.class);

    public static List<TimedEventRecord> generateWeeklyEvents(DateTime start) {
        List<TimedEventRecord> ret = new ArrayList<TimedEventRecord>();
        ret.addAll(EventGenerator.generateWeeklyEventRecords(start, new EventHelper() {

            @Override
            public List<TimedEventRecord> filterEvent(String eventName, DateTime forWhen) {
                String scriptPrefix = "/system/timeprocessor" + eventName;
                List<TimedEventRecord> ret = new ArrayList<TimedEventRecord>();
                scriptWorkWith(eventName, scriptPrefix, forWhen, ret, 1);
                eventWorkWith(eventName, "/" + eventName, forWhen, ret, 1);
                return ret;
            }

            private ReflexRaptureScript rrs = new ReflexRaptureScript();

            private void eventWorkWith(String eventName, String prefix, DateTime forWhen, List<TimedEventRecord> ret,
                    int depth) {
                List<RaptureFolderInfo> fInfo = Kernel.getEvent().listEventsByUriPrefix(ContextFactory.getKernelUser(), prefix);
                for (RaptureFolderInfo f : fInfo) {
                    String next = prefix + "/" + f.getName();
                    if (f.isFolder()) {
                        depth++;
                        if (depth != 4) {
                            eventWorkWith(eventName, next, forWhen, ret, depth);
                        }
                    } else {
                        RaptureEvent event = Kernel.getEvent().getEvent(ContextFactory.getKernelUser(), next);
                        // Now for each of the event items, add a timedRecord
                        if (event.getScripts() != null) {
                            for (RaptureEventScript script : event.getScripts()) {
                                ret.add(getEventRecordForScript(eventName, script.getScriptURI(), forWhen));
                            }
                        }
                        if (event.getWorkflows() != null) {
                            for (RaptureEventWorkflow workflow : event.getWorkflows()) {
                                ret.add(getEventRecordForWorkflow(next, workflow.getWorkflow(), forWhen));
                            }
                        }
                    }
                }
            }

            private TimedEventRecord getEventRecordForWorkflow(String eventName, String workflowId, DateTime forWhen) {
                // The record will have a modified timezone string first, then the hour, then the minute, we need
                // to convert that hour/minute/timezone into a local date (or actually a datetime in a given timezone usually passed in)

                String[] parts = eventName.split("/");
                String timeZoneString = parts[parts.length - 3];
                String hourString = parts[parts.length - 2];
                String minuteString = parts[parts.length - 1];
                DateTimeZone tz = EventGenerator.getRealDateTimeZone(timeZoneString);

                MutableDateTime dt = new MutableDateTime(tz);
                dt.setHourOfDay(Integer.parseInt(hourString));
                dt.setMinuteOfHour(Integer.parseInt(minuteString));

                DateTime inLocalTimeZone = dt.toDateTime(forWhen.getZone());

                TimedEventRecord rec = new TimedEventRecord();
                // Workflow w =
                // Kernel.getDecision().getWorkflow(ContextFactory.getKernelUser(),
                // workflowId);
                rec.setEventName(workflowId);
                rec.setEventContext(workflowId);
                rec.setWhen(inLocalTimeZone.toDate());
                rec.setInfoContext("green");

                DateTime endPoint = inLocalTimeZone.plusMinutes(30);
                rec.setEnd(endPoint.toDate());
                return rec;
            }

            private TimedEventRecord getEventRecordForScript(String eventName, String scriptId, DateTime forWhen) {
                String[] parts = eventName.split("/");
                String timeZoneString = parts[parts.length - 3];
                String hourString = parts[parts.length - 2];
                String minuteString = parts[parts.length - 1];
                DateTimeZone tz = EventGenerator.getRealDateTimeZone(timeZoneString);

                MutableDateTime dt = new MutableDateTime(tz);
                dt.setDate(forWhen.getMillis());
                dt.setHourOfDay(Integer.parseInt(hourString));
                dt.setMinuteOfHour(Integer.parseInt(minuteString));

                DateTime inLocalTimeZone = dt.toDateTime(forWhen.getZone());

                TimedEventRecord rec = new TimedEventRecord();
                // Put the name from the meta property "name" on the
                // script if it exists
                RaptureScript theScript = Kernel.getScript().getScript(ContextFactory.getKernelUser(), scriptId);
                String name = null;
                String color = "blue";
                if (theScript != null) {
                    try {
                        ReflexParser parser = rrs.getParser(ContextFactory.getKernelUser(), theScript.getScript());
                        name = parser.scriptInfo.getProperty("name");
                        if (parser.scriptInfo.getProperty("color") != null) {
                            color = parser.scriptInfo.getProperty("color");
                        }
                    } catch (Exception e) {
                        log.error("Unexpected error: " + ExceptionToString.format(e));
                    }
                }
                rec.setEventName(name == null ? eventName : name);
                rec.setEventContext(scriptId);
                rec.setInfoContext(color);
                rec.setWhen(inLocalTimeZone.toDate());
                DateTime endPoint = inLocalTimeZone.plusMinutes(30);
                rec.setEnd(endPoint.toDate());
                return rec;
            }

            private void scriptWorkWith(String eventName, String prefix, DateTime forWhen, List<TimedEventRecord> ret,
                    int depth) {
                Map<String,RaptureFolderInfo> fInfo = Kernel.getScript().listScriptsByUriPrefix(ContextFactory.getKernelUser(), prefix, -1);
                for (RaptureFolderInfo f : fInfo.values()) {
                    String next = prefix + "/" + f.getName();
                    if (f.isFolder()) {
                        if (depth != 4) {
                            scriptWorkWith(eventName, next, forWhen, ret, depth + 1);
                        } else {
                        }
                    } else {
                        // This script is set to run at x/y/z.../02/00
                        // So run
                        ret.add(getEventRecordForScript(next, next, forWhen));
                    }
                }
            }

        }));

        return ret;
    }
}
