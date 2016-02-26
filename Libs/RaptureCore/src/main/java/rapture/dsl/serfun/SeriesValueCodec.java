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
package rapture.dsl.serfun;

import rapture.common.SeriesValue;
import rapture.common.StructureSeriesValue;
import rapture.common.exception.RaptureExceptionFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;

/**
 * This codec helps prepare a SeriesValue for encoding to a binary repo like CassSeriesConnection.
 *
 * The idea is to start with a correct-but-slow implementation and defer the performance work
 * until we have data on what we would be best served to implement.
 *
 * @author mel
 */
public class SeriesValueCodec {
    // poor-man's enum since I need a single byte
    public static final byte DECIMAL = 'd';
    public static final byte LONG = 'l';
    public static final byte STRING = 's';
    public static final byte BOOLEAN = 'b';
    public static final byte STRUCTURE = 'j';

    /**
     * Strip the column away from a SeriesValue and encode what remains
     *
     * @throws UnsupportedEncodingException
     */
    public static byte[] encodeValue(SeriesValue v) throws UnsupportedEncodingException {
        // primitive types: double, long, string, complex type: structure
        // function type cannot be serialized (yet)
        if (v.isDouble()) {
            return makeArray(v.asDouble());
        } else if (v.isLong()) {
            return makeArray(v.asLong());
        } else if (v.isString()) {
            return makeArray(v.asString());
        } else if (v.isBoolean()) {
            return makeArray(v.asBoolean());
        } else if (v.isStructure()) {
            return makeArray(v.asStructure());
        } else {
            throw new IllegalArgumentException("Function type cannot encoded as value");
        }
    }

    /**
     * Reconstitutes a series value
     *
     * @throws IOException
     */
    public static SeriesValue decode(String column, byte[] bytes) throws IOException {
        String raw = new String(bytes, "UTF-8").substring(1);
        switch (bytes[0]) {
            case DECIMAL:
                return new DecimalSeriesValue(Double.parseDouble(raw), column);
            case LONG:
                return new LongSeriesValue(Long.parseLong(raw), column);
            case STRING:
                return new StringSeriesValue(raw, column);
            case STRUCTURE:
                return StructureSeriesValueImpl.unmarshal(raw, column);
            case BOOLEAN:
                return new BooleanSeriesValue(raw, column);
        }
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Undecodable data type: " + bytes[0]);
    }

    private static byte[] makeArray(double value) throws UnsupportedEncodingException {
        return ("d" + value).getBytes("UTF-8");
    }

    private static byte[] makeArray(long value) throws UnsupportedEncodingException {
        return ("l" + value).getBytes("UTF-8");
    }

    private static byte[] makeArray(String value) throws UnsupportedEncodingException {
        return ("s" + value).getBytes("UTF-8");
    }

    private static byte[] makeArray(boolean value) throws UnsupportedEncodingException {
        return ("b" + value).getBytes("UTF-8");
    }

    private static byte[] makeArray(StructureSeriesValue value) throws UnsupportedEncodingException {
        String json = value.asString();
        return ("j" + json).getBytes("UTF-8");
    }
}
