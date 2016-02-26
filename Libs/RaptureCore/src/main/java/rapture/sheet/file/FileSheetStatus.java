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
package rapture.sheet.file;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rapture.common.RaptureSheetCell;
import rapture.common.RaptureSheetStatus;

public class FileSheetStatus extends RaptureSheetStatus {
    private Map<Integer, Map<Integer, RaptureSheetCell>> rowColMap = new HashMap<>();

    public void setCells(Collection<RaptureSheetCell> newCells) {
        for (RaptureSheetCell cell : newCells) {
            addCell(cell);
        }
    }
    
    public void addCell(RaptureSheetCell cell) {
        List<RaptureSheetCell> cells = getCells();
        if (cells == null) {
            cells = new ArrayList<>();
            setCells(cells);
        }
        Integer rowNum = new Integer(cell.getRow());
        Integer colNum = new Integer(cell.getColumn());
        Map<Integer, RaptureSheetCell> row = rowColMap.get(rowNum);
        if (row == null) {
            row = new HashMap<>();
            rowColMap.put(rowNum, row);
        }
        RaptureSheetCell col = row.remove(colNum);
        if (col != null) cells.remove(col);
        if (cell.getData() != null) {
            cells.add(cell);
            row.put(colNum, cell);
        }
        
        if ((cell.getEpoch()) > getEpoch()) setEpoch(cell.getEpoch());
    }

    @Override
    public Long getEpoch() {
        Long epoch = super.getEpoch();
        return (epoch != null) ? epoch : Long.MIN_VALUE;
    }
}
