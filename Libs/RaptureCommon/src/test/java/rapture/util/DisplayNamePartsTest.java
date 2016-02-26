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
package rapture.util;

import org.junit.Assert;
import org.junit.Test;


public class DisplayNamePartsTest {
    private final String TEST_NAME1="PART/PERS/DISP";
    private final String TEST_NAME2="PART/PERS";
    private final String TEST_NAME3="DISP";
    private final String TEST_NAME4="PART/PERS/DISP/1/2/3/4";
    
    @Test
    public void testGetPartsCorrectOrder() {
        DisplayNameParts dnp=new DisplayNameParts(TEST_NAME1);
        Assert.assertTrue(dnp.getDisplay().compareTo("DISP")==0 && dnp.getAuthority().compareTo("PART")==0 && dnp.getPerspective().compareTo("PERS")==0 ); 
    }

    @Test
    public void testGetPartsIncorrectOrder() {
        DisplayNameParts dnp=new DisplayNameParts(TEST_NAME1);
        Assert.assertFalse(dnp.getDisplay().compareTo("DISP")==0 && dnp.getAuthority().compareTo("PERS")==0 && dnp.getPerspective().compareTo("PARTS")==0 ); 
    }

    @Test
    public void testGetPartsIncorrectDisplay() {
        DisplayNameParts dnp=new DisplayNameParts(TEST_NAME1);
        Assert.assertFalse(dnp.getDisplay().compareTo("PART")==0 && dnp.getAuthority().compareTo("PART")==0 && dnp.getPerspective().compareTo("PERS")==0 ); 
    }
    
    @Test
    public void testGetPartsLongCorrectOrder() {
        DisplayNameParts dnp=new DisplayNameParts(TEST_NAME4);
        Assert.assertTrue(dnp.getDisplay().compareTo("DISP/1/2/3/4")==0 && dnp.getAuthority().compareTo("PART")==0 && dnp.getPerspective().compareTo("PERS")==0 ); 
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testMissingPerspective() {
        new DisplayNameParts(TEST_NAME2);
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testMissingPartsPerspective() {
        new DisplayNameParts(TEST_NAME3);
    }    
    
}
