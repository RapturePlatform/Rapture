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
import org.junit.Assert;
import org.junit.Test;

public class CastTest extends ResourceBasedTest {

    @Test
    public void testCast() throws RecognitionException {
        runTestFor("/cast.rfx");
    }

    @Test
    public void testCast1() throws RecognitionException {
        String result = runTestFor("/cast1.rfx");
        Assert.assertEquals(
                "number = 1.0\nnumber = 1.00\nnumber = 0.0000001\n---\n"
                        + "double number = 2.0\nfloat number = 2.0\nnumber = 2.0\ninteger = 2\nstring = 2\n---\n"
                        + "double number = 3.0\nfloat number = 3.0\nnumber = 3.0\ninteger = 3\nstring = 3.0\n---\n"
                        + "double number = 4.0\nfloat number = 4.0\nnumber = 4.0\ninteger = 4\nstring = 4\n---\n"
                        + "double number = 5.0\nfloat number = 5.0\nnumber = 5.0\ninteger = 5\nstring = 5.0\n---\n"
                        + "double number = 0.0000006\nfloat number = 0.0000006\nnumber = 0.0000006\ninteger = 0\nstring = 0.0000006\n---\n"
                        + "double number = 70000000.0\nfloat number = 70000000.0\nnumber = 70000000.0\ninteger = 70000000\nstring = 70000000\n"
                        + "--RETURNS--true",
                result);
    }

    @Test
    public void testCast2() throws RecognitionException {
        String result = runTestFor("/cast2.rfx");
        Assert.assertEquals("{\n  \"J\" : 1.0\n}\n--RETURNS--true", result);
    }
}
