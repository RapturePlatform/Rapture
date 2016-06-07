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

import org.antlr.runtime.RecognitionException;
import org.junit.Test;

public class ReflexTest extends ResourceBasedTest {

    @Test
    public void testAssert() throws RecognitionException {
        runTestFor("/reflexAssert.rfx");
    }

    @Test
    public void testChain() throws RecognitionException {
        runTestFor("/chain.rfx");
    }

    @Test
    public void testConstants() throws RecognitionException {
        runTestFor("/constants.rfx");
    }

    @Test
    public void testDateTime() throws RecognitionException {
        runTestFor("/datetime.rfx");
    }

    @Test
    public void testInclude() throws RecognitionException {
        runTestFor("/testInclude.rfx");
    }

    @Test
    public void testLexer() throws RecognitionException {
        runTestFor("/reflex.rfx");
    }

    @Test
    public void testMaps() throws RecognitionException {
        runTestFor("/maps.rfx");
    }

    @Test
    public void testNative() throws RecognitionException {
        runTestFor("/native.rfx");
    }

    @Test
    public void testTypeOf() throws RecognitionException {
        runTestFor("/typeof.rfx");
    }

    @Test
    public void testReserved() throws RecognitionException {
        runTestFor("/reserved.rfx");
    }

    @Test
    public void testConst() throws RecognitionException {
        runTestFor("/const.rfx");
    }

    @Test
    public void testDate() throws RecognitionException {
        runTestFor("/date.rfx");
    }

    @Test
    public void testNumbers() throws RecognitionException {
        runTestFor("/numbers.rfx");
    }
}
