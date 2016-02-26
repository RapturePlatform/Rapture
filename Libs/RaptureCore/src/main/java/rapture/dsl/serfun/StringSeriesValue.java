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

import rapture.common.Hose;
import rapture.common.SeriesValue;
import rapture.common.StructureSeriesValue;

import com.google.common.collect.Lists;

public class StringSeriesValue implements SeriesValue, Comparable<SeriesValue> {
    private final String val;
    private final String col;

    public StringSeriesValue(String val, String col) {
        this.val = val;
        this.col = col;
    }
    
    public StringSeriesValue(String val) {
        this.val = val;
        this.col = NULL_COLUMN;
    }
    
    @Override
    public String getColumn() {
        return col;
    }

    @Override
    public String asString() {
        return val;
    }

    @Override
    public long asLong() {
        throw new UnsupportedOperationException("Attempt to use string as integer");
    }

    @Override
    public double asDouble() {
        return Double.NaN;
    }

    @Override
    public boolean asBoolean() {
        return !val.isEmpty();
    }

    @Override
    public Hose asStream() {
        throw new UnsupportedOperationException("Attempt to use string as function");
    }
    
    @Override
    public StructureSeriesValue asStructure() {
        throw new UnsupportedOperationException("Attempt to use string as structure");
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public boolean isLong() {
        return false;
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
        return true;
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
    
    public static List<SeriesValue> zip(List<String> columns, List<String> values) {
        Iterator<String> iterC = columns.iterator();
        Iterator<String> iterD = values.iterator();
        List<SeriesValue> result = Lists.newArrayList();
        while(iterC.hasNext()) {
            result.add(new StringSeriesValue(iterD.next(), iterC.next()));
        }
        return result;
    }

    @Override
    public int compareTo(SeriesValue o) {
        return col.compareTo(o.getColumn());
    }
}
