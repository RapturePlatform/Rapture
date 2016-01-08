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
package rapture.util;

import org.junit.Assert;
import org.junit.Test;


public class DisplayNameParserTest {
    private final String TEST_NAME1="FIRST/LAST";
    private final String TEST_NAME2="FIRST/MIDDLE/LAST";
    private final String TEST_NAME3="FIRST";

    @Test
    public void testGetPartsFirstLastPass() {
        String [] retVal=DisplayNameParser.getParts(TEST_NAME1);
        Assert.assertTrue(retVal[0].compareTo("FIRST")==0 && retVal[1].compareTo("LAST")==0);
    }

    @Test
    public void testGetFirstLastFail() {
        String [] retVal=DisplayNameParser.getParts(TEST_NAME1);
        Assert.assertFalse(retVal[0].compareTo("LAST")==0 && retVal[1].compareTo("FIRST")==0);
    }

    @Test
    public void testGetFirstLastArraySize() {
        String [] retVal=DisplayNameParser.getParts(TEST_NAME1);
        Assert.assertEquals(retVal.length,2);
    }
   
    @Test
    public void testGetPartsFirstMiddleLastPass() {
        String [] retVal=DisplayNameParser.getParts(TEST_NAME2);
        Assert.assertTrue(retVal[0].compareTo("FIRST")==0 && retVal[1].compareTo("MIDDLE/LAST")==0);
    }


    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetOneName() {
        DisplayNameParser.getParts(TEST_NAME3);
    }
}
