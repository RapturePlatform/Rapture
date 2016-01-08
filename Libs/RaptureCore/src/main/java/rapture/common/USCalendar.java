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
package rapture.common;

import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;


public class USCalendar {

    private static Map<String, String> HOLIDAYS = initHolidays();

    //TODO read from config
    private static Map<String, String> initHolidays() {
        Map<String, String> holidays = new HashMap<String, String>();
        holidays.put("1/1", "New Year's Day");
        holidays.put("2/17", "President's Day");
        holidays.put("5/26", "Memorial Day");
        holidays.put("7/4", "Independence Day");
        holidays.put("9/1", "Labor Day");
        holidays.put("11/27", "Thanksgiving Day");
        holidays.put("12/25", "Christmas Day");
        return holidays;
    }

    public static DateTime minusBusinessDays(final DateTime date, int days) {
        DateTime prevBday = date;
        while(days-- > 0) {
            prevBday = getPreviousBusinessDay(prevBday);
        }
        return prevBday;
    }

    public static DateTime getPreviousBusinessDay(final DateTime date) {
        DateTime prevDay = date;
        do {
            prevDay = prevDay.minusDays(1);
        } while(!isBusinessDay(prevDay));
        return prevDay;
    }

    public static boolean isBusinessDay(DateTime date) {
        return !isWeekend(date) && !isHoliday(date);
    }

    public static boolean isWeekend(DateTime date) {
        return date.getDayOfWeek() > 5; // Sat=6
    }

    public static boolean isHoliday(DateTime date) {
        String key = String.format("%d/%d", date.getMonthOfYear(), date.getDayOfMonth());
        return HOLIDAYS.containsKey(key);
    }
}
