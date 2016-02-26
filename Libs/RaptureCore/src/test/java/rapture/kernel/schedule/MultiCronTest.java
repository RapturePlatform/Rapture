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
package rapture.kernel.schedule;

import org.joda.time.DateTime;
import org.junit.Test;
import rapture.common.exception.RaptureException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

// MINUTES HOURS DAYINMONTH MONTH DAYOFWEEK
public class MultiCronTest extends CronTest{
    private void testString(String d1, String d2) {
        assertEquals(d1, d2);
    }

    @Test(expected = RaptureException.class)
    public void testMultiEmpty() {
        CronParser parser = MultiCronParser.create("; ;  ;");
        DateTime fromPoint = new DateTime(2019, 1, 1, 0, 0, 1); // 2019-1-1 00:00:00
        DateTime toPoint = parser.nextRunDate(fromPoint);
        assertNull(toPoint);
    }

    @Test
    public void testSimpleCron() {
        CronParser parser = MultiCronParser.create("5 10 2,4 6 *");
        DateTime fromPoint = new DateTime(2013, 1, 1, 0, 0, 1); // 2019-1-1 00:00:00
        DateTime toPoint = parser.nextRunDate(fromPoint);
        testString(toPoint.toString("M/d/yy hh:mm a"), "6/2/13 10:05 AM");
    }

    @Test
    public void testDoubleCron() {
        CronParser parser = MultiCronParser.create("8 10 2,4 6 *;5 10 2,4 6 *");
        DateTime fromPoint = new DateTime(2013, 1, 1, 0, 0, 1); // 2019-1-1 00:00:00
        DateTime toPoint = parser.nextRunDate(fromPoint);
        testString(toPoint.toString("M/d/yy hh:mm a"), "6/2/13 10:05 AM");
    }

}
