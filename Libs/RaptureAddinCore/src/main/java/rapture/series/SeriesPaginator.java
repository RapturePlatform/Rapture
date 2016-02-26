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
package rapture.series;

import java.util.Iterator;
import java.util.List;

import rapture.common.SeriesValue;

public class SeriesPaginator implements Iterable<SeriesValue>, Iterator<SeriesValue> {
    private static final int NO_DROP = 0;
    private static final int DROP = 1;
    final String key;
    final String endCol;
    final int pageSize;
    final SeriesStore store;
    String lastCol;
    List<SeriesValue> buffer;
    int readIndex;
    
    public SeriesPaginator(String key, String startCol, String endCol, int pageSize, SeriesStore store) {
        this.key = key;
        this.endCol = endCol;
        this.pageSize = pageSize;
        this.store = store;
        lastCol = startCol;
        bufferPage(NO_DROP);
    }

    @Override
    public boolean hasNext() {
        return buffer != null;
    }

    @Override
    public SeriesValue next() {
        SeriesValue result = buffer.get(readIndex);
        readIndex++;
        if (readIndex == buffer.size()) {
            bufferPage(DROP);
        }
        return result;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
    private void bufferPage(int drop) {
        buffer = store.getPointsAfter(key, lastCol, endCol, pageSize + drop);
        readIndex = drop;
        if (buffer == null || buffer.size() <= drop) {
            buffer = null;
        } else {
            lastCol = buffer.get(buffer.size() - 1).getColumn();
        }
    }

    @Override
    public Iterator<SeriesValue> iterator() {
        return this;
    }
}
