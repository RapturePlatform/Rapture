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

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

import rapture.common.Hose;
import rapture.common.SeriesValue;
import rapture.common.StructureSeriesValue;

public class LongSeriesValue implements SeriesValue {
    private String str;
    private final long val;
    private final String col;
    
    public static final LongSeriesValue ZERO = new LongSeriesValue(0);

    public LongSeriesValue(String str, String col) {
        this.val = Long.parseLong(str);
        this.str = str;
        this.col = col;
    }
    
    public LongSeriesValue(long val, String col) {
        this.val = val;
        this.col = col;
    }
    
    public LongSeriesValue(String str) {
        this.val = Long.parseLong(str);
        this.str = str;
        this.col = NULL_COLUMN;
    }
    
    public LongSeriesValue(long l) {
        this.val = l;
        this.col = NULL_COLUMN;
    }

    @Override
    public String getColumn() {
        return col;
    }
    
    @Override
    public String asString() {
        prepStr();
        return str;
    }

    @Override
    public long asLong() {
        return val;
    }

    @Override
    public double asDouble() {
        return val;
    }

    @Override
    public boolean asBoolean() {
        return val != 0;
    }

    @Override
    public Hose asStream() {
        throw new UnsupportedOperationException("Attempt to use 64-bit integer as a function");
    }

    @Override
    public boolean isNumber() {
        return true;
    }

    @Override
    public boolean isLong() {
        return true;
    }

    @Override
    public boolean isDouble() {
        return false;
    }

    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public boolean isString() {
        return false;
    }
    
    @Override
    public boolean isStructure() {
        return false;
    }

    @Override
    public boolean isSeries() {
        return false;
    }

    @Override
    public SeriesValue pullValue() {
        return this;
    }

    @Override
    public SeriesValue pullValue(int index) {
        return this;
    }
    
    private void prepStr() {
        if (str == null) {
            str = "" + val;
        }
    }

    @Override
    public StructureSeriesValue asStructure() {
        throw new UnsupportedOperationException("Attempt to use 64-bit integer as a structure");
    }
    
    public static List<SeriesValue> zip(List<String> columns, List<Long> values) {
        Iterator<String> iterC = columns.iterator();
        Iterator<Long> iterD = values.iterator();
        List<SeriesValue> result = Lists.newArrayList();
        while(iterC.hasNext()) {
            result.add(new LongSeriesValue(iterD.next(), iterC.next()));
        }
        return result;
    }
}
