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
package rapture.kernel.schedule;

import java.net.HttpURLConnection;

import com.google.common.base.Joiner;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

import fc.cron.CronExpression;

import rapture.common.exception.RaptureExceptionFactory;

/**
 * Parses a cron tab spec, yielding an internal structure that you can ask questions of. Specifically, when is the next date from "date" (which could be now)
 * that we can run based on this cron spec. We use this date in the RaptureJobExec.
 * 
 * Cron spec is of the form "minutes hours daysinmonth months daysinweek (years)". Years is optional. If you specify a year in the past, nothing will run.
 * 
 * @author amkimian
 * 
 */
public class CronParser {
    private CronExpression cronExpression;

    /*
     * allowed year values according to http://en.wikipedia.org/wiki/Cron
     */
    private static final int YEARMIN = 1970;
    private static final int YEARMAX = 2099;

    private Set<Integer> years;

    public CronParser(String cronLine) {
        String[] params = cronLine.split(" ");
        String cronExp = cronLine;
        if (params.length > 5) {
            years = parseRangeParam(params[5], YEARMAX, YEARMIN, YEARMIN, YEARMAX);
            // Trim the year before passing it to CronExpression.
            cronExp = cronLine.substring(0, cronLine.lastIndexOf(" "));
        }
        cronExpression = new CronExpression(cronExp, false);
    }

    public CronParser() {
    }

    private static Set<Integer> parseRangeParam(String param, int timeLength, int minLength, int lowestVal, int highestVal) {
        String[] paramArray;
        Set<Integer> ret = new HashSet<Integer>();
        // Test for range
        if (param.indexOf('-') != -1) {
            paramArray = param.split("-");
            if (paramArray.length == 2) {
                ret = fillRange(Integer.parseInt(paramArray[0]), Integer.parseInt(paramArray[1]));
            } else {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "- range must have two values");
            }
        } else {
            if (param.indexOf(",") != -1) {
                paramArray = param.split(",");
            } else {
                paramArray = new String[] { param };
            }
            for (String p : paramArray) {
                if (p.indexOf("/") != -1) {
                    int secondary = Integer.parseInt(p.substring(p.indexOf("/") + 1));
                    for (int a = 1; a <= timeLength; a++) {
                        if (a % secondary == 0) {
                            if (a == timeLength) {
                                ret.add(minLength);
                            } else {
                                ret.add(a);
                            }
                        }
                    }
                } else {
                    if (p.equals("*")) {
                        ret.addAll(fillRange(minLength, timeLength));
                    } else {
                        ret.add(Integer.parseInt(p));
                    }
                }
            }
        }
        for (int x : ret) {
            if (x < lowestVal || x > highestVal) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error value is out of bounds for cron spec");
            }
        }
        return ret;
    }

    private static Set<Integer> fillRange(int start, int end) {
        if (start > end) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Range in reverse");
        }
        Set<Integer> ret = new HashSet<Integer>();
        for (int i = start; i <= end; i++) {
            ret.add(i);
        }
        return ret;
    }

    /**
     * Returns the next run date based on a fromPoint (starting date). Returns null if there is no more future runs.
     *
     * @param fromPoint
     * @return
     */
    public DateTime nextRunDate(DateTime fromPoint) {
        if (years != null) {
            int year = fromPoint.getYear();
            if (year > getHighestYear()) {
                // year specified in the cron spec is in the past, so no more runs
                return null;
            }
            while (!years.contains(year)) {
                fromPoint = fromPoint.plusYears(1).withDayOfYear(1).withTime(0, 0, 0, 0);
                year = fromPoint.getYear();
            }
            DateTime result = cronExpression.nextTimeAfter(fromPoint);
            year = result.getYear();
            if (year > getHighestYear()) {
                // year specified in the cron spec is in the past, so no more runs
                return null;
            } else {
                return result;
            }
        } else {
            return cronExpression.nextTimeAfter(fromPoint);
        }

    }

    /**
     * Used to figure out if the latest year specified has already past--at which point we don't run anything and return null
     * 
     * @return
     */
    private int getHighestYear() {
        int highYear = YEARMIN;
        for (int year : years) {
            highYear = Math.max(year, highYear);
        }
        return highYear;
    }

    public static CronParser create(String cronSpec) {
        try {
            return new CronParser(cronSpec);
        } catch (Exception ex) {
            throw RaptureExceptionFactory.create("Unsupported cron specification: " + cronSpec);
        }
    }
}
