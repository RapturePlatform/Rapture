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

import java.net.HttpURLConnection;
import java.util.List;

import rapture.common.Hose;
import rapture.common.SeriesValue;
import rapture.common.StructureSeriesValue;
import rapture.common.exception.RaptureExceptionFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * This glue class applies range, dereferencing, and output selection, then
 * delegates everything else.
 * 
 * @author mel
 * 
 */
public class HoseArg extends SimpleHose {
    @Override
    public String getName() {
        return "#ARG";
    }

    @Override
    public String getColumn() {
        return upstream.getColumn();
    }

    @Override
    public String asString() {
        return upstream.asString();
    }

    @Override
    public long asLong() {
        return upstream.asLong();
    }

    @Override
    public double asDouble() {
        return upstream.asDouble();
    }

    @Override
    public Hose asStream() {
        return upstream.asStream();
    }

    @Override
    public boolean isNumber() {
        return upstream.isNumber();
    }

    @Override
    public boolean isLong() {
        return upstream.isLong();
    }

    @Override
    public boolean isDouble() {
        return upstream.isDouble();
    }

    @Override
    public boolean isStructure() {
        return upstream.isStructure();
    }

    @Override
    public boolean isSeries() {
        return upstream.isSeries();
    }

    // TODO MEL apply dereference logic here
    @Override
    public SeriesValue pullValue() {
        return upstream.pullValue(upstreamIndex);
    }

    // TODO MEL apply dereference logic here
    @Override
    public void pushValue(SeriesValue v) {
        downstream.pushValue(v, downstreamIndex);
    }

    /**
     * Convenience routine to extract integer parms.
     * 
     * @param msg
     *            Error message is parm is not an integer
     * @return the parameter
     */
    public int tryInt(String msg) {
        Preconditions.checkArgument(isLong(), msg);
        return (int) asLong();
    }

    public static HoseArg make(SeriesValue base, SeriesValue outkey, List<String> derefList) {
        HoseArg result = new HoseArg();
        result.reconfigure(base, outkey, derefList);
        return result;
    }

    public void reconfigure(SeriesValue base, SeriesValue outkey, List<String> derefList) {
        upstream = base;
        if (outkey == null) {
            upstreamIndex = 0;
        } else if (outkey.isLong()) {
            upstreamIndex = (int) outkey.asLong();
        } else {
            Hose baseHose;
            try {
                baseHose = base.asStream();
            } catch (UnsupportedOperationException ex) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "The expression " + base.asString()
                        + " does not support multiple outputs");
            }
            upstreamIndex = baseHose.getOutputIndex(outkey.asString());
            if (upstreamIndex == -1) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "The output name " + outkey.asString()
                        + " does not refer to an output of " + base.asString());
            }
        }
    }

    @Override
    public StructureSeriesValue asStructure() {
        return upstream.asStructure();
    }

    private static List<String> EMPTY_LIST = ImmutableList.of();

    public static HoseArg makeDecimal(String in) {
        return make(new DecimalSeriesValue(in), null, EMPTY_LIST);
    }

    public static HoseArg makeLong(String in) {
        return make(new LongSeriesValue(in), null, EMPTY_LIST);
    }

    public static HoseArg makeString(String in) {
        return make(new StringSeriesValue(in), null, EMPTY_LIST);
    }

    public static HoseArg makeStream(Hose h) {
        return make(h, null, EMPTY_LIST);
    }
}
