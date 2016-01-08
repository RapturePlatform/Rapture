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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class StringUtilTest {
    @Test
    public void badInputFormats() {
        Map<String, Object> ret = StringUtil.getMapFromString("x=");
        assertTrue(ret.isEmpty());
    }

    @Test
    public void badInputFormats2() {
        Map<String, Object> ret = StringUtil.getMapFromString("=x");
        assertTrue(ret.isEmpty());
    }

    @Test
    public void badInputFormats3() {
        Map<String, Object> ret = StringUtil.getMapFromString(",,,,,,.....sjhkjhfjkfhjk");
        assertTrue(ret.isEmpty());
    }

    @Test
    public void badInputTest() {
        Map<String, Object> ret = StringUtil.getMapFromString(null);
        assertTrue(ret.isEmpty());

        ret = StringUtil.getMapFromString("");
        assertTrue(ret.isEmpty());
    }

    @Test
    public void badListInput() {
        List<String> ret = StringUtil.list(null);
        assertTrue(ret.isEmpty());
    }

    @Test
    public void badListInput2() {
        List<String> ret = StringUtil.list("");
        assertTrue(ret.isEmpty());
    }

    @Test
    public void goodInputTest() {

        Map<String, Object> ret = StringUtil.getMapFromString("x=y");
        assertTrue(ret.containsKey("x"));
        assertEquals(ret.get("x"), "y");
    }

    @Test
    public void goodInputTest2() {

        Map<String, Object> ret = StringUtil.getMapFromString("x=y,z=2");
        assertTrue(ret.containsKey("x"));
        assertEquals(ret.get("x"), "y");
        assertTrue(ret.containsKey("z"));
        assertEquals(ret.get("z"), "2");
    }

    @Test
    public void goodListInput() {
        List<String> ret = StringUtil.list("1,2,3,4");
        assertEquals(ret.size(), 4);
    }
    
    @Test
    public void testBase64Encoding() {
        String input = "Hello this is Alan\nFred was here and some more";
        String base64 = StringUtil.base64Compress(input);
        String output = StringUtil.base64Decompress(base64);
        System.out.println("Input = " + input);
        System.out.println("========");
        System.out.println("Base64 = " + base64);
        System.out.println("========");
        System.out.println("Output = " + output);
        System.out.println("========");
    }
}
