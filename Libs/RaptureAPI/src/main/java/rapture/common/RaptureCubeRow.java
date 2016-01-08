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
package rapture.common;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * One row in a RaptureCube
 *
 * @author alan
 */
public class RaptureCubeRow implements RaptureTransferObject {
    @JsonIgnore
    private List<RaptureField> colFieldInfo;

    private List<RaptureCubeCell> cells;

    private String groupName;

    public RaptureCubeRow() {

    }

    public RaptureCubeRow(List<RaptureField> colFieldInfo) {
        this.colFieldInfo = colFieldInfo;
    }

    public void addEntries(List<String> colResults) {
        if (cells == null) {
            // Create a set of blanks, based on colFieldInfo
            cells = new ArrayList<RaptureCubeCell>();
            for (RaptureField f : colFieldInfo) {
                RaptureCubeCell cell = new RaptureCubeCell();
                cell.setEntryCount(0);
                cell.setUnits(f.getUnits());
                cell.setValue(null);
                cells.add(cell);
            }
        }
        // Add the entries to the cells in this row
        int pos = 0;
        for (String colResult : colResults) {
            RaptureField f = colFieldInfo.get(pos);
            RaptureCubeCell cell = cells.get(pos);
            cell.addEntry(colResult, f);
            pos++;
        }
    }

    public List<RaptureCubeCell> getCells() {
        return cells;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setCells(List<RaptureCubeCell> cells) {
        this.cells = cells;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

}
