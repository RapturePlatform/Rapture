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
package rapture.common;


public interface SeriesValue {
    /**
     * This is the default column identifier to be used for values not associated with a column.
     */
    public static final String NULL_COLUMN = "";
    
    /**
     * get the column (e.g. time in a time series) for this value
     * @return the column, or NULL_COLUMN if this value is not associated with a column
     */
    String getColumn();
    
    String asString();
    long asLong();
    double asDouble();
    boolean asBoolean();
    StructureSeriesValue asStructure();
    Hose asStream();
    
    boolean isNumber();
    boolean isLong();
    boolean isDouble();
    boolean isBoolean();
    boolean isString();
    boolean isStructure();
    boolean isSeries();
    
    SeriesValue pullValue();
    SeriesValue pullValue(int index);

}
