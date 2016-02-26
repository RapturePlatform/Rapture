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
package rapture.dsl.dparse;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.fail;
import rapture.common.exception.RaptureException;

import static org.junit.Assert.assertEquals;

/**
 * Created by zanniealvarez on 12/23/15.
 */
public class AsOfTimeDirectiveParserTest {
    String dateString = "20150526T132613";
    String msString = ".025";
    String tzString = "-0400";

    @Test
    public void testFullySpecifiedDatetime() {
        AsOfTimeDirectiveParser fullySpecified = new AsOfTimeDirectiveParser(dateString + msString + tzString);
        assertEquals(1432661173025L, fullySpecified.getMillisTimestamp());
    }

    @Test
    @Ignore // Assumes San Francisco time zone on server, which is not always the case
    public void testDateTimeWithMs() {
        AsOfTimeDirectiveParser fullySpecified = new AsOfTimeDirectiveParser(dateString + msString);
        assertEquals(1432671973025L, fullySpecified.getMillisTimestamp());
    }

    @Test
    public void testDateTimeWithTz() {
        AsOfTimeDirectiveParser fullySpecified = new AsOfTimeDirectiveParser(dateString + tzString);
        assertEquals(1432661173000L, fullySpecified.getMillisTimestamp());
    }

    @Test
    @Ignore // Assumes San Francisco time zone on server, which is not always the case
    public void testDateTimeBare() {
        AsOfTimeDirectiveParser fullySpecified = new AsOfTimeDirectiveParser(dateString);
        assertEquals(1432671973000L, fullySpecified.getMillisTimestamp());
    }

    @Test
    public void testTimestamp() {
        AsOfTimeDirectiveParser fullySpecified = new AsOfTimeDirectiveParser("t1432671973025");
        assertEquals(1432671973025L, fullySpecified.getMillisTimestamp());
    }

    @Test
    public void testBadFormats() {
        doTestBadFormat("fff");
        doTestBadFormat("t1432671971");
        doTestBadFormat("m1432671971000");
        doTestBadFormat("t143267197100");
        doTestBadFormat("t14326719710000");
        doTestBadFormat("2015-05-26 13:26:13");
    }

    public void doTestBadFormat(String badlyFormattedDate) {
        try {
            new AsOfTimeDirectiveParser(badlyFormattedDate);
            fail("Should have failed to parse badly formatted date " + badlyFormattedDate);
        }
        catch (RaptureException exception) { }
    }
}
