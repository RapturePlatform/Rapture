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
package reflex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.antlr.runtime.RecognitionException;
import org.junit.Test;

import reflex.value.ReflexDateValue;

public class DateTest extends ResourceBasedTest {
    @Test
    public void testDateAdding() throws RecognitionException {
        String ret = runTestFor("/date/adding.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    // See RAP-3540
    @Test
    public void testDate() {
        ReflexDateValue v = new ReflexDateValue("20120804", "BDF");
        String s = v.toString();
        assertEquals("20120804", s);
    }

    @Test
    public void testDateCreation() throws RecognitionException {
        String ret = runTestFor("/date/creation.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testTime() throws RecognitionException {
        String ret = runTestFor("/time/creation.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testComparison() throws RecognitionException {
        String ret = runTestFor("/date/comparison.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testTimeComparison() throws RecognitionException {
        String ret = runTestFor("/date/timecomparison.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void yesterday() throws RecognitionException {
        String[] ret = runTestFor("/date/yesterday.rfx").split("\n");

        ZonedDateTime today = ZonedDateTime.now(ZoneId.of("America/Los_Angeles"));
        ZonedDateTime yesterday = today.minusDays(1);

        DateTimeFormatter df1 = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter df2 = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        assertEquals(ret[3], "TODAY2 " + today.format(df2));
        assertEquals(ret[2], "TODAY " + today.format(df1));
        assertEquals(ret[1], "YESTERDAY2 " + yesterday.format(df2));
        assertEquals(ret[0], "YESTERDAY " + yesterday.format(df1));
        assertTrue("Test case did not complete successfully", ret[ret.length - 1].endsWith("true"));
    }

    @Test
    public void testEpoch() throws RecognitionException {
        String ret = runTestFor("/date/epoch.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
        ret = runTestFor("/time/epoch.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testCalendar() throws RecognitionException {
        String ret = runTestFor("/date/calendar.rfx");
        System.out.println(ret);
        assertTrue("Test case did not complete successfully", ret.endsWith("TODAY"));
    }

}
