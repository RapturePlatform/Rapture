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

import java.io.UnsupportedEncodingException;

import rapture.util.encode.RaptureEncodeFilter;
import rapture.util.encode.RaptureEncodeHelper;
import rapture.util.encode.RaptureURLCoderFilter;

/**
 * This can be used to convert a string that could not (may not) be stored as a RaptureURI into something that
 * could be stored as a RaptureURI, along with a reverse function that can undo such a change.
 *
 * @author amkimian
 */
public class RaptureURLCoder extends RaptureEncodeHelper {

    private static final RaptureEncodeFilter FILTER = new RaptureURLCoderFilter();

    public static String encode(String value) {
        return encode(value, FILTER);
    }
    
    public static String encode(String value, RaptureEncodeFilter filter) {
        StringBuilder ret = new StringBuilder();
        int cpc = value.codePointCount(0, value.length());
        for (int i = 0; i < cpc; i++) {
            int codePoint = value.codePointAt(i);
            if ((codePoint == ESCAPE) || filter.isNeedsEncode(codePoint)) {
                // Don't use + for space
                if (codePoint == ' ') {
                    ret.append("%20");
                } else try {
                    // Use the Java encoder. It works better for characters > 127 
                    ret.append(java.net.URLEncoder.encode(new String(Character.toChars(codePoint)), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    // Can't encode it? This shouldn't happen
                    ret.append(Character.toChars(codePoint));
                }
            } else {
                ret.append(Character.toChars(codePoint));
            }
        }
        String retval = ret.toString();
        return (ESCAPE == '%') ? retval : retval.replace('%',  ESCAPE);
    }
}
