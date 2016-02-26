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

import java.net.HttpURLConnection;

import rapture.common.exception.RaptureExceptionFactory;

public class StringComplianceCheck {
    private StringComplianceCheck() {
        
    }
    static char[] INVALIDSET = { '/', '"', '\'', '?', '#', '@','$' };
    /**
     * Throws an exception if value contains chars that are not allowed.
     * Not allowed include slashes, hashes, question marks etc.
     * @param value
     */
    public static void checkNoInvalidChars(String value) {
        // Use a loop as it is quicker than a regex....?
        if (value == null) {
            return;
        }
        for(int i=0; i< value.length(); i++) {
            char v = value.charAt(i);
            for(int j=0; j< INVALIDSET.length; j++) {
                if (v == INVALIDSET[j]) {
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, String.format("Invalid character %c in string %s", INVALIDSET[j], value));
                }
            }
        }
    }
}
