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
package rapture.util.encode;

import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;


/**
 * @author bardhi
 * @since 6/23/14.
 */
public class RaptureEncodeHelper {

    public static final char ESCAPE = '%';
    private static Logger log = Logger.getLogger(RaptureEncodeHelper.class);

    public static String decode(String value) {
        
        String encoded = (ESCAPE == '%') ? value : value.replace(ESCAPE, '%');
        try {
            return java.net.URLDecoder.decode(encoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Seems unlikely
            log.error(e.getMessage());
        }
        return value;
    }
}
