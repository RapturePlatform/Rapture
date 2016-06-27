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

import static org.junit.Assert.assertTrue;

import org.antlr.runtime.RecognitionException;
import org.junit.Test;

public class FunctionalTest extends ResourceBasedTest {
    @Test
    public void testMap() throws RecognitionException {
        String ret = runTestFor("/functional/map.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testFilter() throws RecognitionException {
        String ret = runTestFor("/functional/filter.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testFold() throws RecognitionException {
        String ret = runTestFor("/functional/fold.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testAny() throws RecognitionException {
        String ret = runTestFor("/functional/any.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testAll() throws RecognitionException {
        String ret = runTestFor("/functional/all.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void takeWhile() throws RecognitionException {
        String ret = runTestFor("/functional/takeWhile.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void dropWhile() throws RecognitionException {
        String ret = runTestFor("/functional/dropWhile.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void splitWith() throws RecognitionException {
        String ret = runTestFor("/functional/splitWith.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }
}
