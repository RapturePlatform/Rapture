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

import org.joda.time.DateTime;
import java.util.ArrayList;
import java.util.List;

import rapture.common.exception.RaptureExceptionFactory;

/**
 * Parses a multiple cron tab spec, yielding an internal structure that you can ask questions of. Specifically, when is the next date from "date" (which could be now)
 * that we can run based on this cron spec. We use this date in the RaptureJobExec.
 * 
 * multiple cron tab spec is cron tabs, separated by ;
 * empty specs are ignored
 *
 * @author amkimian
 * 
 */
public class MultiCronParser extends CronParser {
    List<CronParser> cronList = new ArrayList<>();

    public MultiCronParser(String input) {
        String[] cronLines = input.split(";");
        for (String cronLine : cronLines) {
            cronLine = cronLine.trim();
            if (!"".equals(cronLine)) {
                cronList.add(new CronParser(cronLine));
            }
        }
    }

    public static CronParser create(String cronSpec) {
        try {
            MultiCronParser result = new MultiCronParser(cronSpec);
            if (result.cronList.isEmpty()) {
                throw new Exception("all empty");
            }
            return result;
        } catch (Exception ex) {
            throw RaptureExceptionFactory.create("Unsupported cron specification: " + cronSpec);
        }
    }
        /**
         * Returns the next run date based on a fromPoint (starting date). Returns null if there is no more future runs.
         *
         * @param fromPoint
         * @return
         */
    public DateTime nextRunDate(DateTime fromPoint) {
        DateTime result = null;
        // Take the earliest non-null nextRunDate for each CronParser, and return it.
        for (CronParser cp : cronList) {
            DateTime nextRun = cp.nextRunDate(fromPoint);
            if (result == null || nextRun.isBefore(result)) {
                result = nextRun;
            }
        }
        return result;
    }

}
