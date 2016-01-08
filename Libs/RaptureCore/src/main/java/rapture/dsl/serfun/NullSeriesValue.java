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

import java.util.Collection;

import com.google.common.collect.ImmutableList;

import rapture.common.Hose;
import rapture.common.SeriesValue;
import rapture.common.StructureSeriesValue;

public class NullSeriesValue implements StructureSeriesValue {

    @Override
    public String getColumn() { 
        return NULL_COLUMN;
    }

    @Override
    public String asString() {
        return "";
    }

    @Override
    public long asLong() {
        return 0;
    }

    @Override
    public double asDouble() {
        return 0.0;
    }

    @Override
    public boolean asBoolean() {
        return false;
    }

    @Override
    public StructureSeriesValue asStructure() {
        return this;
    }

    @Override
    public Hose asStream() {
        throw new UnsupportedOperationException();
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

    @Override
    public SeriesValue getField(String name) {
        return this;
    }

    @Override
    public Collection<String> getFields() {
        return ImmutableList.of();
    }

}
