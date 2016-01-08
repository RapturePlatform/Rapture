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
package rapture.event.generator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import rapture.common.TimedEventRecord;

import com.google.common.collect.HashBiMap;

/**
 * Generate event names given a date/calendar object
 * 
 * @author alanmoore
 * 
 */
public class EventGenerator {
    /**
     * This must be called with a minute movement - or you'll get repeats
     * 
     * @param cal
     * @param periodic
     * @param periodic
     * @return
     */
    public static List<String> generateEventNames(DateTime cal, boolean periodic) {
        List<String> ret = new ArrayList<String>();
        generatePeriodic(ret, cal, periodic);
        return ret;
    }

    private static void checkAdd(int dayOfWeek, int checkValue, int[] valids, String name, String prefix, List<String> ret) {
        for (int v : valids) {
            if ((checkValue % v) == 0) {
                ret.add(String.format("%s/daily/%s/%02d", prefix, name, v));
                ret.add(String.format("%s/%s/%s/%02d", prefix, isWeekend(dayOfWeek) ? "weekend" : "weekday", name, v));
                ret.add(String.format("%s/%s/%s/%02d", prefix, getDOWName(dayOfWeek), name, v));
            }
        }
    }

    private static String getDOWName(int jodaTimeDay) {
        return dow[jodaTimeDay-1];
    }
    // JODA Chronology uses ISO standard where Monday = 1 and Sunday = 7
    // Does it hurt to have two Sundays? Should zero be an error?
    private static String[] dow = { "mon", "tue", "wed", "thu", "fri", "sat", "sun" };
    private static int[] minuteChecks = new int[] { 1, 10, 15, 30 };
    private static int[] hourChecks = new int[] { 1, 12 };

    private static String[] supportedTimeZones = { "America/New_York", "America/Los_Angeles", "Europe/London", "UTC" };

    public static String[] getSupportedZones() {
        return supportedTimeZones;
    }
    
    private static HashBiMap<String, String> timeZoneBiMap = HashBiMap.create();
    private static Map<String, DateTimeZone> timeZones = new HashMap<String, DateTimeZone>();
    
    public static String getConvertedTimeZone(String realTimeZone) {
        return timeZoneBiMap.get(realTimeZone);
    }
    
    public static String getRealTimeZone(String convertedTimeZone) {
        return timeZoneBiMap.inverse().get(convertedTimeZone);
    }
    
    public static DateTimeZone getRealDateTimeZone(String timeZoneString) {
        return timeZones.get(getRealTimeZone(timeZoneString));
    }
    
    static {
        for (int i = 0; i < supportedTimeZones.length; i++) {
            timeZones.put(supportedTimeZones[i],DateTimeZone.forID(supportedTimeZones[i]));
            String alternate = supportedTimeZones[i].replace('/', '-');
            timeZoneBiMap.put(supportedTimeZones[i], alternate);
        }

    }

    private static DateTimeFormatter hmFormat = DateTimeFormat.forPattern("HH/mm");

    private static boolean isWeekend(int dow) {
        return dow >=6;
    }
    private static void generatePeriodic(List<String> ret, DateTime dt, boolean periodic) {
        // We guarantee that each call to this function, cal will be advanced a
        // minute at least

        int dayOfWeek = dt.getDayOfWeek();
        if (periodic) {
            checkAdd(dayOfWeek, dt.getMinuteOfHour(), minuteChecks, "minutes", "/time", ret);
            if (dt.getMinuteOfHour() == 0) {
                checkAdd(dayOfWeek, dt.getHourOfDay(), hourChecks, "hours", "/time", ret);
                if (dt.getHourOfDay() == 0) {
                    ret.add("/time/days" + dt.getDayOfMonth());
                    if (dt.getDayOfMonth() == 1) {
                        ret.add("/time/months" + dt.getMonthOfYear());
                    }
                }
            }
        }

        // We construct, for each supported timezone,
        // timezone/daily/[timezone]/hour/minute
        // timezone/weekend/ (if a weekend)
        // timezone/weekeday/ (if a weekday)
        // timezone/day

        // So initially create the end point for each timezone, then push it out
        // We assume dt is in UT (but does this matter?)

        for (Map.Entry<String, DateTimeZone> zoneEntry : timeZones.entrySet()) {
            DateTime dtLocal = dt.withZone(zoneEntry.getValue());
            String convertedZone = getConvertedTimeZone(zoneEntry.getKey());
            int localDow = dtLocal.getDayOfWeek();
            String endField = hmFormat.print(dtLocal);
            ret.add(String.format("/timezone/daily/%s/%s", convertedZone, endField));
            ret.add(String.format("/timezone/%s/%s/%s", isWeekend(localDow) ? "weekend" : "weekday", convertedZone, endField));
            ret.add(String.format("/timezone/%s/%s/%s", getDOWName(localDow), convertedZone, endField));
        }
    }

    public static Collection<? extends TimedEventRecord> generateWeeklyEventRecords(DateTime startDt, EventHelper eventHelper) {
        // Given a start date, look forward one week (7 days), showing events
        // that are at most one minute granular. Don't show periodic events
        // Do some attempt to order them correctly
        List<TimedEventRecord> records = new ArrayList<TimedEventRecord>();
        int currentDow = startDt.getDayOfWeek();
        DateTime endPoint = startDt.plusDays(7);
        DateTime current = startDt.plus(0);
        int endDow = endPoint.getDayOfWeek();
        boolean first = true;
        while (currentDow != endDow || first) {
            first = false;
            String startPoint = String.format("/timezone/%s", getDOWName(currentDow));
            List<TimedEventRecord> recs = eventHelper.filterEvent(startPoint, current);
            if (recs != null) {
                records.addAll(recs);
            }
            startPoint = "/timezone/daily";
            recs = eventHelper.filterEvent(startPoint, current);
            if (recs != null) {
                records.addAll(recs);
            }
            if (isWeekend(currentDow)) {
                startPoint = "/timezone/weekend";
                recs = eventHelper.filterEvent(startPoint, current);
            } else {
                startPoint = "/timezone/weekday";
                recs = eventHelper.filterEvent(startPoint, current);
            }
            if (recs != null) {
                records.addAll(recs);
            }
            current = current.plusDays(1);
            currentDow = current.getDayOfWeek();
        }
        return records;
    }

 
}
