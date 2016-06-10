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

public class ReflexTest extends ResourceBasedTest {

    @Test
    public void testAssert() throws RecognitionException {
        String ret = runTestFor("/reflexAssert.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testChain() throws RecognitionException {
        String ret = runTestFor("/chain.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testConstants() throws RecognitionException {
        String ret = runTestFor("/constants.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testDateTime() throws RecognitionException {
        String ret = runTestFor("/datetime.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testInclude() throws RecognitionException {
        String ret = runTestFor("/testInclude.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testLexer() throws RecognitionException {
        String ret = runTestFor("/reflex.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testMaps() throws RecognitionException {
        String ret = runTestFor("/maps.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testNative() throws RecognitionException {
        String ret = runTestFor("/native.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testTypeOf() throws RecognitionException {
        String ret = runTestFor("/typeof.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testConst() throws RecognitionException {
        String ret = runTestFor("/const.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testDate() throws RecognitionException {
        String ret = runTestFor("/date.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testNumbers() throws RecognitionException {
        String ret = runTestFor("/numbers.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }

    @Test
    public void testContains() throws RecognitionException {
        String ret = runTestFor("/contains.rfx");
        assertTrue("Test case did not complete successfully", ret.endsWith("true"));
    }
}
