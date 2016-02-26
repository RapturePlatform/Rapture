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

import org.antlr.runtime.RecognitionException;
import org.junit.Test;

import reflex.value.ReflexDateValue;

public class DateTest extends ResourceBasedTest {
    @Test
    public void testDateAdding() throws RecognitionException {
        runTestFor("/date/adding.rfx");
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
        runTestFor("/date/creation.rfx");
    }

    @Test
    public void testTime() throws RecognitionException {
        runTestFor("/time/creation.rfx");
    }

    @Test
    public void testComparison() throws RecognitionException {
        runTestFor("/date/comparison.rfx");
    }
}
