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

import rapture.common.storable.helpers.RaptureFieldHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This is the return value from a cube view filter
 * Note that this class is referenced in types.api - any changes to this file should be reflected there.
 *
 * @author alan
 */
public class RaptureCubeResult implements RaptureTransferObject {

    private List<String> groupNames;

    private List<String> columnNames;

    private List<RaptureCubeRow> rows;

    @JsonIgnore
    private Map<String, RaptureCubeRow> rowsByGroupName;

    @JsonIgnore
    private List<RaptureField> colFieldInfo;

    public RaptureCubeResult() {
        rows = new ArrayList<RaptureCubeRow>();
        rowsByGroupName = new HashMap<String, RaptureCubeRow>();
    }

    /**
     * Add an entry to this cube
     *
     * @param entryName
     * @param grpResults
     * @param colResults
     * @param colFieldInfo
     */
    public void addEntry(String entryName, List<String> grpResults, List<String> colResults) {

        // We basically create a dotted entry using the grpResults, building it
        // up one at a time
        // For each entry we see if we already have an entry. If we do, we apply
        // the "colFieldInfo grouping function" to each value in ColResults
        // And repeat as necessary. Each cell we need to store how many records
        // have been added (so we can do averaging correctly)
        StringBuilder entry = new StringBuilder();
        for (String grpName : grpResults) {
            if (entry.length() != 0) {
                entry.append(".");
            }
            entry.append(grpName);
            // Now find the entry
            RaptureCubeRow rowToWorkWith = null;
            if (rowsByGroupName.containsKey(entry.toString())) {
                rowToWorkWith = rowsByGroupName.get(entry.toString());
            } else {
                rowToWorkWith = new RaptureCubeRow(colFieldInfo);
                rowToWorkWith.setGroupName(entry.toString());
                rows.add(rowToWorkWith);
                rowsByGroupName.put(entry.toString(), rowToWorkWith);
            }
            rowToWorkWith.addEntries(colResults);
        }
    }

    public void finish() {
        // Sort the rows...
        Collections.sort(rows, new Comparator<RaptureCubeRow>() {

            @Override
            public int compare(RaptureCubeRow arg0, RaptureCubeRow arg1) {
                return arg0.getGroupName().compareTo(arg1.getGroupName());
            }

        });

    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public List<String> getGroupNames() {
        return groupNames;
    }

    public List<RaptureCubeRow> getRows() {
        return rows;
    }

    public void setColFieldInfo(List<RaptureField> colFieldInfo) {
        this.colFieldInfo = colFieldInfo;
        // Setup column names from this
        List<String> cNames = new ArrayList<String>();
        for (RaptureField f : colFieldInfo) {
            RaptureFieldHelper.addColumnNamesToSet(f, cNames);
        }
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public void setGroupNames(List<String> groupNames) {
        this.groupNames = groupNames;
    }

    public void setRows(List<RaptureCubeRow> rows) {
        this.rows = rows;
    }

}
