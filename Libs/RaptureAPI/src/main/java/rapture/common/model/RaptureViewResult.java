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
package rapture.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import rapture.common.RaptureTransferObject;

/**
 * This is used to store the result of running a view
 * 
 * A view has a specific set of columns that are returned, and a series of rows.
 * 
 * The column names are set by the view definition, and then each row is an
 * array of arrays, where the values match the column names by index, with null
 * indicating blank information for that column.
 * Note that this class is referenced in types.api - any changes to this file should be reflected there.
 */

public class RaptureViewResult implements RaptureTransferObject {
    private List<String> columnNames;
    private List<List<Object>> rows;
    private List<Object> currentRow;

    public RaptureViewResult() {
        columnNames = new ArrayList<String>();
        rows = new ArrayList<List<Object>>();
    }

    public void addRowValue(Object columnValue) {
        currentRow.add(columnValue);
    }

    public void addValue(Object x) {
        if (x instanceof List) {
            // Add each member of this list, then start a new row
            @SuppressWarnings("rawtypes")
            List vals = (List) x;
            for (Object inner : vals) {
                addRowValue(inner);
            }
            startNewRow();
        } else {
            addRowValue(x);
        }
    }

    /**
     * Gets the columnNames for this instance.
     * 
     * @return The columnNames.
     */
    public List<String> getColumnNames() {
        return this.columnNames;
    }

    /**
     * Gets the rows for this instance.
     * 
     * @return The rows.
     */
    public List<List<Object>> getRows() {
        return this.rows;
    }

    /**
     * Sets the columnNames for this instance.
     * 
     * @param columnNames
     *            The columnNames.
     */
    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    /**
     * Sets the rows for this instance.
     * 
     * @param rows
     *            The rows.
     */
    public void setRows(List<List<Object>> rows) {
        this.rows = rows;
    }

    public void startNewRow() {
        currentRow = new Vector<Object>(columnNames.size());
        rows.add(currentRow);
    }
}
