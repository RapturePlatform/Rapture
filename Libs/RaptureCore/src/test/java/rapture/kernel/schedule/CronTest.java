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
import org.joda.time.DateTimeZone;
import org.joda.time.Hours;
import org.junit.Test;
import rapture.common.exception.RaptureException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CronTest {
    public static final String DATE_FORMAT = "M/d/yy hh:mm a";

    @Test
    public void testCron() {
        CronParser parser = CronParser.create("5 10 2,4 6 *");
        checkNextRunDate(parser, new DateTime(2013, 1, 1, 0, 0), new DateTime(2013, 6, 2, 10, 5));
        checkNextRunDate(parser, new DateTime(2014, 6, 2, 10, 6), new DateTime(2014, 6, 4, 10, 5));
        checkNextRunDate(parser, new DateTime(2015, 6, 4, 10, 6), new DateTime(2016, 6, 2, 10, 5));
    }

    @Test
    public void testCronDaily() {
        CronParser parser = CronParser.create("0 23 * * * *");
        checkNextRunDate(parser, new DateTime(2013, 1, 1, 0, 0), new DateTime(2013, 1, 1, 23, 0));
        checkNextRunDate(parser, new DateTime(2014, 1, 1, 23, 1), new DateTime(2014, 1, 2, 23, 0));
    }

    @Test
    public void testCronDayOfWeek() {
        CronParser parser = CronParser.create("0 23 * * 1,3,5 *");
        // Jan 1 2013 is a Tuesday.
        checkNextRunDate(parser, new DateTime(2013, 1, 1, 23, 1), new DateTime(2013, 1, 2, 23, 0));
        checkNextRunDate(parser, new DateTime(2013, 1, 2, 23, 1), new DateTime(2013, 1, 4, 23, 0));
        checkNextRunDate(parser, new DateTime(2013, 1, 4, 23, 1), new DateTime(2013, 1, 7, 23, 0));
        // Dec 29 2013 is a Sunday
        checkNextRunDate(parser, new DateTime(2013, 12, 29, 23, 1), new DateTime(2013, 12, 30, 23, 0));
        checkNextRunDate(parser, new DateTime(2013, 12, 30, 23, 1), new DateTime(2014, 1, 1, 23, 0));
    }

    @Test
    public void testCronMonthly() {
        CronParser parser = CronParser.create("0 23 1 * * *");
        checkNextRunDate(parser, new DateTime(2013, 1, 1, 23, 1), new DateTime(2013, 2, 1, 23, 0));
        checkNextRunDate(parser, new DateTime(2013, 5, 6, 23, 1), new DateTime(2013, 6, 1, 23, 0));
        checkNextRunDate(parser, new DateTime(2013, 12, 31, 23, 1), new DateTime(2014, 1, 1, 23, 0));
    }

    @Test(expected = RaptureException.class)
    public void testInvalidCron() {
        // Parse error, 24 is an invalid hour.
        CronParser.create("0 24 * * * *");
    }

    @Test
    public void testRange() {
        CronParser parser = CronParser.create("1-10 23 * * * *");
        checkNextRunDate(parser, new DateTime(2013, 1, 1, 0, 0), new DateTime(2013, 1, 1, 23, 1));
        checkNextRunDate(parser, new DateTime(2014, 1, 1, 23, 2), new DateTime(2014, 1, 1, 23, 3));
        checkNextRunDate(parser, new DateTime(2015, 1, 1, 23, 11), new DateTime(2015, 1, 2, 23, 1));

    }

    @Test
    public void testSingleYear() {
        CronParser parser = CronParser.create("1-10 23 * * * 2014");
        checkNextRunDate(parser, new DateTime(2013, 1, 1, 0, 0), new DateTime(2014, 1, 1, 23, 1));
        checkNextRunDate(parser, new DateTime(2014, 1, 1, 0, 0), new DateTime(2014, 1, 1, 23, 1));
        checkNextRunDate(parser, new DateTime(2015, 1, 1, 0, 0), null);

    }

    @Test
    public void testRangeOfYears() {
        CronParser parser = CronParser.create("1-10 23 * * * 2014-2019");
        checkNextRunDate(parser, new DateTime(1999, 1, 1, 0, 0), new DateTime(2014, 1, 1, 23, 1));
        checkNextRunDate(parser, new DateTime(2014, 2, 3, 23, 2), new DateTime(2014, 2, 3, 23, 3));
        checkNextRunDate(parser, new DateTime(2019, 12, 31, 23, 9), new DateTime(2019, 12, 31, 23, 10));
        checkNextRunDate(parser, new DateTime(2021, 12, 31, 23, 10), null);
    }

    @Test
    public void testListOfYears() {
        CronParser parser = CronParser.create("1-10 23 * * * 2014,2015,2016,2020,2012,2021");
        checkNextRunDate(parser, new DateTime(1999, 1, 1, 0, 0), new DateTime(2012, 1, 1, 23, 1));
        checkNextRunDate(parser, new DateTime(2013, 2, 3, 23, 2), new DateTime(2014, 1, 1, 23, 1));
        checkNextRunDate(parser, new DateTime(2019, 12, 31, 23, 11), new DateTime(2020, 1, 1, 23, 1));
        checkNextRunDate(parser, new DateTime(2021, 12, 31, 23, 9), new DateTime(2021, 12, 31, 23, 10));
        checkNextRunDate(parser, new DateTime(2021, 12, 31, 23, 10), null);
    }

    @Test(expected = RaptureException.class)
    public void testEmpty() {
        CronParser.create("");
    }


    @Test
    public void testMinuteNumber() throws Exception {
        CronParser parser = CronParser.create("3 * * * *");
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 13, 1), new DateTime(2012, 4, 10, 13, 3));
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 13, 3), new DateTime(2012, 4, 10, 14, 3));
    }

    // helper test method.  given a CronParser and DateTime input, check the expected nextRunDate
    private void checkNextRunDate(CronParser parser, DateTime fromPoint, DateTime expected) {
        DateTime toPoint = parser.nextRunDate(fromPoint);
        assertEquals(expected, toPoint);
    }

    @Test
    public void testMinuteIncrement() throws Exception {
        CronParser parser = CronParser.create("0/15 * * * *");
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 13, 0), new DateTime(2012, 4, 10, 13, 15));
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 13, 15), new DateTime(2012, 4, 10, 13, 30));
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 13, 30), new DateTime(2012, 4, 10, 13, 45));
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 13, 45), new DateTime(2012, 4, 10, 14, 0));
    }

    @Test
    public void testMinuteList() throws Exception {
        CronParser parser = CronParser.create("7,19 * * * *");
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 13, 0), new DateTime(2012, 4, 10, 13, 7));
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 13, 7), new DateTime(2012, 4, 10, 13, 19));
    }

    @Test
    public void testHourNumber() throws Exception {
        CronParser parser = CronParser.create("* 3 * * *");
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 13, 1), new DateTime(2012, 4, 11, 3, 0));
        checkNextRunDate(parser, new DateTime(2012, 4, 11, 3, 0), new DateTime(2012, 4, 11, 3, 1));
        checkNextRunDate(parser, new DateTime(2012, 4, 11, 3, 59), new DateTime(2012, 4, 12, 3, 0));
    }

    @Test
    public void testHourIncrement() throws Exception {
        CronParser parser = CronParser.create("* 0/15 * * *");
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 13, 0), new DateTime(2012, 4, 10, 15, 0));
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 15, 0), new DateTime(2012, 4, 10, 15, 1));
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 15, 59), new DateTime(2012, 4, 11, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 4, 11, 0, 0), new DateTime(2012, 4, 11, 0, 1));
        checkNextRunDate(parser, new DateTime(2012, 4, 11, 15, 0), new DateTime(2012, 4, 11, 15, 1));
    }

    @Test
    public void testHourList() throws Exception {
        CronParser parser = CronParser.create("* 7,19 * * *");
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 13, 0), new DateTime(2012, 4, 10, 19, 0));
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 19, 0), new DateTime(2012, 4, 10, 19, 1));
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 19, 59), new DateTime(2012, 4, 11, 7, 0));
    }

    @Test
    public void testHourRuns25TimesInDSTChangeToWinter() throws Exception {
        CronParser cron = CronParser.create("1 * * * *");
        DateTimeZone timeZone = DateTimeZone.forID("America/New_York");
        DateTime start = new DateTime(2011, 11, 6, 0, 0, 0, timeZone);
        DateTime slutt = start.plusDays(1).withTimeAtStartOfDay();
        DateTime tid = start;
        assertEquals(25, Hours.hoursBetween(start, slutt).getHours());
        int count=0;
        DateTime lastTime = tid;
        while(tid.isBefore(slutt)){
            DateTime nextTime = cron.nextRunDate(tid);
            assertTrue(nextTime.isAfter(lastTime));
            lastTime = nextTime;
            tid = tid.plusHours(1);
            count++;
        }
        assertEquals(25, count);
    }

    @Test
    public void testHourRuns23TimesInDSTChangeToSummer() throws Exception {
        CronParser cron = CronParser.create("1 * * * *");
        DateTimeZone timeZone = DateTimeZone.forID("America/New_York");
        DateTime start = new DateTime(2011, 3, 13, 0, 0, 0, timeZone);
        DateTime slutt = start.plusDays(1).withTimeAtStartOfDay();
        DateTime tid = start;
        assertEquals(23, Hours.hoursBetween(start, slutt).getHours());
        int count=0;
        DateTime lastTime = tid;
        while(tid.isBefore(slutt)){
            DateTime nextTime = cron.nextRunDate(tid);
            assertTrue(nextTime.isAfter(lastTime));
            lastTime = nextTime;
            tid = tid.plusHours(1);
            count++;
        }
        assertEquals(23, count);
    }

    @Test
    public void testDayOfMonthNumber() throws Exception {
        CronParser parser = CronParser.create("* * 3 * *");
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 13, 0), new DateTime(2012, 5, 3, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 5, 3, 0, 0), new DateTime(2012, 5, 3, 0, 1));
        checkNextRunDate(parser, new DateTime(2012, 5, 3, 0, 59), new DateTime(2012, 5, 3, 1, 0));
        checkNextRunDate(parser, new DateTime(2012, 5, 3, 23, 59), new DateTime(2012, 6, 3, 0, 0));
    }

    @Test
    public void testDayOfMonthIncrement() throws Exception {
        CronParser parser = CronParser.create("* * 1/15 * *");
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 13, 0), new DateTime(2012, 4, 16, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 4, 16, 23, 59), new DateTime(2012, 5, 1, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 4, 30, 23, 59), new DateTime(2012, 5, 1, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 5, 1, 23, 59), new DateTime(2012, 5, 16, 0, 0));
    }

    @Test
    public void testDayOfMonthList() throws Exception {
        CronParser parser = CronParser.create("* * 7,19 * *");
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 13, 0), new DateTime(2012, 4, 19, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 4, 19, 23, 59), new DateTime(2012, 5, 7, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 5, 7, 23, 59), new DateTime(2012, 5, 19, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 5, 30, 23, 59), new DateTime(2012, 6, 7, 0, 0));
    }

    @Test
    public void testDayOfMonthLast() throws Exception {
        CronParser parser = CronParser.create("* * L * *");
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 13, 0), new DateTime(2012, 4, 30, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 2, 12, 0, 0), new DateTime(2012, 2, 29, 0, 0));
    }

    @Test
    public void testDayOfMonthNumberLast() throws Exception {
        CronParser parser = CronParser.create("* * 3L * *");
        checkNextRunDate(parser, new DateTime(2012, 4, 10, 13, 0), new DateTime(2012, 4, 30 - 3, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 2, 12, 0, 0), new DateTime(2012, 2, 29 - 3, 0, 0));
    }

    @Test
    public void testDayOfMonthClosestWeekday() throws Exception {
        CronParser parser = CronParser.create("* * 9W * *");
        // 9 - is weekday in may
        checkNextRunDate(parser, new DateTime(2012, 5, 2, 0, 0), new DateTime(2012, 5, 9, 0, 0));
        // 9 - is weekday in may
        checkNextRunDate(parser, new DateTime(2012, 5, 8, 0, 0), new DateTime(2012, 5, 9, 0, 0));
        // 9 - saturday, friday closest weekday in june
        checkNextRunDate(parser, new DateTime(2012, 5, 9, 23, 59), new DateTime(2012, 6, 8, 0, 0));
        // 9 - sunday, monday closest weekday in september
        checkNextRunDate(parser, new DateTime(2012, 9, 1, 0, 0), new DateTime(2012, 9, 10, 0, 0));
    }

    @Test(expected = RaptureException.class)
    public void testDayOfMonthInvalidModifier() throws Exception {
        CronParser parser = CronParser.create("* * 9X * *");
    }

    @Test(expected = RaptureException.class)
    public void testDayOfMonthInvalidIncrementModifier() throws Exception {
        CronParser parser = CronParser.create("* * 9#2 * *");
    }
    @Test
    public void testMonthNumber() throws Exception {
        CronParser parser = CronParser.create("0 0 1 5 *");
        checkNextRunDate(parser, new DateTime(2012, 2, 12, 0, 0), new DateTime(2012, 5, 1, 0, 0));
    }

    @Test
    public void testMonthIncrement() throws Exception {
        CronParser parser = CronParser.create("0 0 1 5/2 *");
        checkNextRunDate(parser, new DateTime(2012, 2, 12, 0, 0), new DateTime(2012, 5, 1, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 5, 1, 0, 0), new DateTime(2012, 7, 1, 0, 0));
//         if rolling over year then reset month field (cron rules - increments only affect own field)
        parser = CronParser.create("0 0 1 5/10 *");
        checkNextRunDate(parser, new DateTime(2012, 5, 1, 0, 0), new DateTime(2013, 5, 1, 0, 0));
    }

    @Test
    public void testMonthList() throws Exception {
        CronParser parser = CronParser.create("0 0 1 3,7,12 *");
        checkNextRunDate(parser, new DateTime(2012, 2, 12, 0, 0), new DateTime(2012, 3, 1, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 3, 1, 0, 0), new DateTime(2012, 7, 1, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 7, 1, 0, 0), new DateTime(2012, 12, 1, 0, 0));
    }

    @Test
    public void testMonthListByName() throws Exception {
        CronParser parser = CronParser.create("0 0 1 MAR,JUL,DEC *");
        checkNextRunDate(parser, new DateTime(2012, 2, 12, 0, 0), new DateTime(2012, 3, 1, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 3, 1, 0, 0), new DateTime(2012, 7, 1, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 7, 1, 0, 0), new DateTime(2012, 12, 1, 0, 0));
    }

    @Test(expected = RaptureException.class)
    public void testInvalidMonthModifier() throws Exception {
        CronParser parser = CronParser.create("0 0 0 1 ? *");
    }

    @Test
    public void testDayOfWeekNumber() throws Exception {
        CronParser parser = CronParser.create("0 0 * * 3");
        checkNextRunDate(parser, new DateTime(2012, 4, 1, 0, 0), new DateTime(2012, 4, 4, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 4, 4, 0, 0), new DateTime(2012, 4, 11, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 4, 12, 0, 0), new DateTime(2012, 4, 18, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 4, 18, 0, 0), new DateTime(2012, 4, 25, 0, 0));
    }

    @Test
    public void testDayOfWeekIncrement() throws Exception {
        CronParser parser = CronParser.create("0 0 * * 3/2");
        checkNextRunDate(parser, new DateTime(2012, 4, 1, 0, 0), new DateTime(2012, 4, 4, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 4, 4, 0, 0), new DateTime(2012, 4, 6, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 4, 6, 0, 0), new DateTime(2012, 4, 8, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 4, 8, 0, 0), new DateTime(2012, 4, 11, 0, 0));
    }

    @Test
    public void testDayOfWeekList() throws Exception {
        CronParser parser = CronParser.create("0 0 * * 1,5,7");
        checkNextRunDate(parser, new DateTime(2012, 4, 1, 0, 0), new DateTime(2012, 4, 2, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 4, 2, 0, 0), new DateTime(2012, 4, 6, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 4, 6, 0, 0), new DateTime(2012, 4, 8, 0, 0));
    }

    @Test
    public void testDayOfWeekListByName() throws Exception {
        CronParser parser = CronParser.create("0 0 * * MON,FRI,SUN");
        checkNextRunDate(parser, new DateTime(2012, 4, 1, 0, 0), new DateTime(2012, 4, 2, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 4, 2, 0, 0), new DateTime(2012, 4, 6, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 4, 6, 0, 0), new DateTime(2012, 4, 8, 0, 0));
    }

    @Test
    public void testDayOfWeekLastFridayInMonth() throws Exception {
        CronParser parser = CronParser.create("0 0 * * 5L");
        checkNextRunDate(parser, new DateTime(2012, 4, 1, 0, 0), new DateTime(2012, 4, 27, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 4, 27, 0, 0), new DateTime(2012, 5, 25, 0, 0));
        checkNextRunDate(parser, new DateTime(2012, 2, 6, 0, 0), new DateTime(2012, 2, 24, 0, 0));
        parser = CronParser.create("0 0 * * FRIL");
        checkNextRunDate(parser, new DateTime(2012, 2, 6, 0, 0), new DateTime(2012, 2, 24, 0, 0));
    }

    @Test(expected = RaptureException.class)
    public void testDayOfWeekInvalidModifier() throws Exception {
        CronParser parser = CronParser.create("* * * * 5W");
    }


    @Test(expected = RaptureException.class)
    public void testDayOfWeekInvalidIncrementModifier() throws Exception {
        CronParser parser = CronParser.create("* * * * 5?3");
    }

    @Test
    public void testDayOfWeek0AsSunday() throws Exception {
        CronParser parser = CronParser.create("0 0 * * 0");
        checkNextRunDate(parser, new DateTime(2012, 4, 1, 0, 0), new DateTime(2012, 4, 8, 0, 0));
        parser = CronParser.create("0 0 * * 0L");
        checkNextRunDate(parser, new DateTime(2012, 4, 1, 0, 0), new DateTime(2012, 4, 29, 0, 0));
        parser = CronParser.create("0 0 * * 0#2");
        checkNextRunDate(parser, new DateTime(2012, 4, 1, 0, 0), new DateTime(2012, 4, 8, 0, 0));
    }

    @Test
    public void testDayOfWeek7AsSunday() throws Exception {
        CronParser parser = CronParser.create("0 0 * * 7");
        checkNextRunDate(parser, new DateTime(2012, 4, 1, 0, 0), new DateTime(2012, 4, 8, 0, 0));
        parser = CronParser.create("0 0 * * 7L");
        checkNextRunDate(parser, new DateTime(2012, 4, 1, 0, 0), new DateTime(2012, 4, 29, 0, 0));
        parser = CronParser.create("0 0 * * 7#2");
        checkNextRunDate(parser, new DateTime(2012, 4, 1, 0, 0), new DateTime(2012, 4, 8, 0, 0));
    }

    @Test
    public void testDayOfWeekNthFridayInMonth() throws Exception {
        CronParser parser = CronParser.create("0 0 * * 5#3");
        checkNextRunDate(parser, new DateTime(2012, 4, 1, 0, 0), new DateTime(2012, 4, 20, 0, 0));
        parser = CronParser.create("0 0 * * 5#3");
        checkNextRunDate(parser, new DateTime(2012, 4, 20, 0, 0), new DateTime(2012, 5, 18, 0, 0));
        parser = CronParser.create("0 0 * * 7#1");
        checkNextRunDate(parser, new DateTime(2012, 3, 30, 0, 0), new DateTime(2012, 4, 1, 0, 0));
        parser = CronParser.create("0 0 * * 7#1");
        checkNextRunDate(parser, new DateTime(2012, 4, 1, 0, 0), new DateTime(2012, 5, 6, 0, 0));
        parser = CronParser.create("0 0 * * 3#5");
        checkNextRunDate(parser, new DateTime(2013, 2, 6, 0, 0), new DateTime(2013, 5, 29, 0, 0));
        parser = CronParser.create("0 0 * * WED#5");
        checkNextRunDate(parser, new DateTime(2013, 4, 1, 0, 0), new DateTime(2013, 5, 29, 0, 0));
    }

    @Test(expected = RaptureException.class)
    public void testRollingPeriod() throws Exception {
        CronParser parser = CronParser.create("* * 5-1 * *");
    }

    @Test(expected = RaptureException.class)
    public void testNonExistingDate() throws Exception {
        CronParser parser = CronParser.create("* 30 2 * *");
    }

    @Test
    public void testLeapYear() throws Exception {
        // the default barrier is 4 years - so leap years are considered.
        CronParser parser = CronParser.create("* * 29 2 *");
        checkNextRunDate(parser, new DateTime(2012, 3, 1, 00, 00), new DateTime(2016, 2, 29, 00, 00));
    }
}
