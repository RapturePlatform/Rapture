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

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;

import org.junit.Ignore;
import org.junit.Test;

import rapture.util.encode.RaptureEncodeHelper;
import rapture.util.encode.RaptureLightweightCoder;


public class RaptureURLCoderTest {

    public void testReversible(String v) {
        String encoded = RaptureURLCoder.encode(v);
        String decoded = RaptureURLCoder.decode(encoded);
        System.out.println("From " + v);
        System.out.println("To " + encoded);
        System.out.println("Back " + decoded);

        String encoded2 = RaptureLightweightCoder.encode(v);
        String decoded2 = RaptureLightweightCoder.decode(encoded2);
        System.out.println("From " + v);
        System.out.println("To " + encoded2);
        System.out.println("Back " + decoded2);
        
        try {
            String official = java.net.URLEncoder.encode(v, "UTF-8");
            System.out.println("Official " + official);
            if (!encoded.equals(official)) System.out.println("Official encoding "+official+" is different from ours "+encoded);
            
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        assertEquals("RaptureURLCoder", v, decoded);
        assertEquals("RaptureLightweightCoder", v, decoded2);
    }

    // Should not change when encoded
    
    public void testNotEncoded(String v) {
        String encoded = RaptureURLCoder.encode(v);
        String decoded = RaptureURLCoder.decode(encoded);

        String encoded2 = RaptureLightweightCoder.encode(v);
        String decoded2 = RaptureLightweightCoder.decode(encoded2);
        
        assertEquals("RaptureURLCoder", v, encoded);
        assertEquals("RaptureLightweightCoder", v, encoded2);
        
        assertEquals("RaptureURLCoder", v, decoded);
        assertEquals("RaptureLightweightCoder", v, decoded2);
    }
    
    @Test
    public void testEasy() {
        //testString("Simple");
        testReversible("Simple with spaces");
        testReversible("Simple?");
        testReversible("Simple_and_how and that was a dash");
        testReversible("Hello & three ^ four ~ 5");
        testReversible("USD/CAD R 06/18/14");
        testReversible("abc Q1 XYZ");
        testReversible("q!d$g^j*");
        testReversible("foo/bar");
        testReversible("this_year");
        testReversible("EFI_AF_I");
    }
    
    @Test
    public void testInternationalCharacters() {
    	// If you don't see Cyrillic and Chinese characters your default character encoding is not UTF-8
        testNotEncoded("ДэвидТонг");
        testNotEncoded("大衛通");
    }
    
    @Test
    public void testEightBitSymbols() {
    	// These are all single UTF-8 characters: Paragraph, GBP Currency, Section, +/-, JPY currency
        testReversible("¶");
        testReversible("£");
        testReversible("§");
        testReversible("±");
        testReversible("¥");
    }

    @Test
    public void testEmoji() throws UnsupportedEncodingException {
    	// This is a 'smiley face' emoji
        testReversible("😊");
    }
    
    /**
     * The old RaptureURLCoder fails on the Euro character. It encodes it as %20AC which isn't correct.
     */
    @Test
    public void testSixteenBitSymbols() {
    	// This is the Euro symbol
        testReversible("€");
    }

    @Test
    public void testPathological() {
        RaptureEncodeHelper.decode("-------");
    }
}
