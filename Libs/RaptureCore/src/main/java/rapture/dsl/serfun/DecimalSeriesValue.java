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

public class DecimalSeriesValue implements SeriesValue {
    private String str;
    private final double val;
    private final String col;

    public DecimalSeriesValue(String str, String col) {
        this.val = Double.parseDouble(str);
        this.str = str;
        this.col = col;
    }

    public DecimalSeriesValue(double d, String col) {
        this.val = d;
        this.col = col;
    }
    
    public DecimalSeriesValue(String str) {
        this.val = Double.parseDouble(str);
        this.str = str;
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
        return Math.round(val);
    }

    @Override
    public double asDouble() {
        return val;
    }

    @Override
    public boolean asBoolean() {
        return val != 0.0;
    }

    @Override
    public Hose asStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNumber() {
        return true;
    }

    @Override
    public boolean isLong() {
        return false;
    }

    @Override
    public boolean isDouble() {
        return true;
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
    
    public static SeriesValue make(String str) {
        if (str.contains(".")) {
            return new DecimalSeriesValue(str);
        } else {
            return new LongSeriesValue(str);
        }
    }

    private void prepStr() {
        if (str == null) {
            str = "" + val;
        }
    }

    @Override
    public StructureSeriesValue asStructure() {
        throw new UnsupportedOperationException("Attempt to use Decimal as Structure");
    }

    public static List<SeriesValue> zip(List<String> columns, List<Double> values) {
        Iterator<String> iterC = columns.iterator();
        Iterator<Double> iterD = values.iterator();
        List<SeriesValue> result = Lists.newArrayList();
        while(iterC.hasNext()) {
            result.add(new DecimalSeriesValue(iterD.next(), iterC.next()));
        }
        return result;
    }
}
