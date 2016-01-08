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
package rapture.dsl.serfun;

import rapture.common.Hose;
import rapture.common.SeriesValue;
import rapture.common.StructureSeriesValue;

/**
 * Created by mel on 6/17/14.
 */
public class BooleanSeriesValue implements SeriesValue {
    private final String col;
    private final boolean val;

    public BooleanSeriesValue(boolean val, String col) {
        this.col = col;
        this.val = val;
    }

    public BooleanSeriesValue(String val, String col) {
        this.col = col;
        this.val = "true".equals(val.toLowerCase());
    }

    @Override
    public String getColumn() {
        return col;
    }

    @Override
    public String asString() {
        return val ? "true" : "false";
    }

    @Override
    public long asLong() {
        return val ? 1 : 0;
    }

    @Override
    public double asDouble() {
        return val ? 1.0 : 0.0;
    }

    @Override
    public boolean asBoolean() {
        return val;
    }

    @Override
    public StructureSeriesValue asStructure() {
        throw new UnsupportedOperationException("attempt to use boolean as a structure");
    }

    @Override
    public Hose asStream() {
        throw new UnsupportedOperationException("attempt to use boolean as a function");
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
        return false;
    }

    @Override
    public boolean isBoolean() {
        return true;
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
}
