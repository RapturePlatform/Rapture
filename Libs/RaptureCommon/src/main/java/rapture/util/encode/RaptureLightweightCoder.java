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
package rapture.util.encode;

import rapture.util.RaptureURLCoder;

/**
 * This can be used to convert a string that could not (may not) be stored as a RaptureURI into something that could be stored as a RaptureURI, along with a
 * reverse function that can undo such a change.
 * <p>
 * Unlike RaptureURLCoder, this class does not encode forward slashes
 * 
 * @author bardhi
 * @since 6/23/14.
 */
public class RaptureLightweightCoder extends RaptureURLCoder {
    
    private static final RaptureEncodeFilter FILTER = new RaptureURLCoderFilter("/");

    public static String encode(String value) {
        return RaptureURLCoder.encode(value, FILTER);
    }
}
