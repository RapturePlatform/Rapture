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
package rapture.sheet.memory;

import rapture.common.RaptureSheetCell;
import rapture.common.RaptureSheetNote;
import rapture.common.RaptureSheetRange;
import rapture.common.RaptureSheetScript;
import rapture.common.RaptureSheetStatus;
import rapture.common.RaptureSheetStyle;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.util.IDGenerator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

/**
 * The implementation of an in memory Rapture sheet.
 *
 * @author amkimian
 */
public class InMemorySheet {
    @JsonProperty
    private Map<Integer, Table<Integer, Integer, RaptureSheetCell>> dimToData;
    private int maxColumn = 0;
    private int maxRow = 0;
    private Map<String, RaptureSheetScript> scriptMap;
    private Map<String, RaptureSheetNote> noteMap;
    private Map<String, RaptureSheetRange> rangeMap;
    private Map<String, RaptureSheetStyle> styleMap;

    public InMemorySheet() {
        dimToData = new HashMap<Integer, Table<Integer, Integer, RaptureSheetCell>>();
        scriptMap = new HashMap<String, RaptureSheetScript>();
        noteMap = new HashMap<String, RaptureSheetNote>();
        rangeMap = new HashMap<String, RaptureSheetRange>();
        styleMap = new HashMap<String, RaptureSheetStyle>();
    }

    @SuppressWarnings("unchecked")
    public InMemorySheet(InMemorySheet source) {
        this.dimToData = new HashMap<Integer, Table<Integer, Integer, RaptureSheetCell>>();
        for (Integer dim : source.dimToData.keySet()) {
            Table<Integer, Integer, RaptureSheetCell> table = source.dimToData.get(dim);
            dimToData.put(dim, HashBasedTable.create(table));
        }

        this.scriptMap = ((Map<String, RaptureSheetScript>) ((Object) JacksonUtil.getMapFromJson(JacksonUtil.jsonFromObject(source.scriptMap))));
        this.noteMap = (Map<String, RaptureSheetNote>) (Object) JacksonUtil.getMapFromJson(JacksonUtil.jsonFromObject(source.noteMap));
        this.rangeMap = (Map<String, RaptureSheetRange>) (Object) JacksonUtil.getMapFromJson(JacksonUtil.jsonFromObject(source.rangeMap));
        this.styleMap = (Map<String, RaptureSheetStyle>) (Object) JacksonUtil.getMapFromJson(JacksonUtil.jsonFromObject(source.styleMap));
    }

    public void setCell(int dim, int row, int col, String val) {
        Table<Integer, Integer, RaptureSheetCell> table = dimToData.get(dim);
        if (table == null) {
            table = HashBasedTable.create();
            dimToData.put(dim, table);
        }

        setCell(table, row, col, val);

    }

    private void setCell(Table<Integer, Integer, RaptureSheetCell> table, int row, int column, String value) {
        RaptureSheetCell cell = new RaptureSheetCell();
        cell.setRow(row);
        cell.setColumn(column);
        cell.setData(value);
        Long epoch = System.currentTimeMillis() / 1000;
        cell.setEpoch(epoch);
        table.put(row, column, cell);
        if (row > maxRow) {
            maxRow = row;
        }
        if (column > maxColumn) {
            maxColumn = column;
        }
    }

    public int getMaxRow() {
        return maxRow;
    }

    public int getMaxColumn() {
        return maxColumn;
    }

    public String getCell(int dimension, int row, int col) {
        Table<Integer, Integer, RaptureSheetCell> table = dimToData.get(dimension);
        if (table != null) {
            RaptureSheetCell cell = table.get(row, col);
            if (cell != null) {
                return cell.getData();
            } else {
                return null;
            }
        } else {
            return null;
        }

    }

    public RaptureSheetStatus getCells(Integer dimension, Long minEpochFilter) {
        Table<Integer, Integer, RaptureSheetCell> table = dimToData.get(dimension);
        Set<Cell<Integer, Integer, RaptureSheetCell>> cellSet = table.cellSet();
        List<RaptureSheetCell> cells = new LinkedList<RaptureSheetCell>();
        Long maxEpoch = 0L;
        for (Cell<Integer, Integer, RaptureSheetCell> cell : cellSet) {
            RaptureSheetCell sheetCell = cell.getValue();
            if (sheetCell.getEpoch() > minEpochFilter) {
                cells.add(sheetCell);
                if (maxEpoch < sheetCell.getEpoch()) {
                    maxEpoch = sheetCell.getEpoch();
                }
            }
        }

        RaptureSheetStatus ret = new RaptureSheetStatus();
        ret.setCells(cells);
        ret.setEpoch(maxEpoch);
        return ret;

    }

    public Boolean delete(int row, int column, int dimension) {
        Table<Integer, Integer, RaptureSheetCell> table = dimToData.get(dimension);
        if (table == null) {
            return false;
        } else {
            return table.remove(row, column) != null;
        }
    }

    public Boolean delete(int row, int col) {
        for (Integer dimension : dimToData.keySet()) {
            delete(row, col, dimension);
        }
        return true;
    }

    public Set<Integer> getDimensions() {
        return dimToData.keySet();
    }

    public RaptureSheetScript getSheetScript(String scriptName) {
        return scriptMap.get(scriptName);
    }

    public List<RaptureSheetScript> getSheetScripts() {
        return ImmutableList.<RaptureSheetScript>builder().addAll(scriptMap.values()).build();
    }

    public Boolean deleteSheetScript(String scriptName) {
        return scriptMap.remove(scriptName) != null;
    }

    public RaptureSheetScript putSheetScript(String scriptName, RaptureSheetScript script) {
        RaptureSheetScript storedScript = JacksonUtil.objectFromJson(JacksonUtil.jsonFromObject(script), RaptureSheetScript.class);
        return scriptMap.put(scriptName, storedScript);
    }

    public List<RaptureSheetNote> getSheetNotes() {
        return ImmutableList.<RaptureSheetNote>builder().addAll(noteMap.values()).build();
    }

    public Boolean deleteSheetNote(String noteId) {
        return noteMap.remove(noteId) != null;
    }

    public RaptureSheetNote putSheetNote(RaptureSheetNote note) {
        if (note.getId() == null || note.getId().isEmpty()) {
            note.setId(IDGenerator.getUUID());
        }
        RaptureSheetNote storableNote = JacksonUtil.objectFromJson(JacksonUtil.jsonFromObject(note), RaptureSheetNote.class);
        return noteMap.put(storableNote.getId(), storableNote);
    }

    public RaptureSheetRange putSheetNamedSelection(String rangeName, RaptureSheetRange range) {
        RaptureSheetRange storable = JacksonUtil.objectFromJson(JacksonUtil.jsonFromObject(range), RaptureSheetRange.class);
        return rangeMap.put(rangeName, storable);
    }

    public Boolean deleteSheetNamedSelection(String rangeName) {
        return rangeMap.remove(rangeName) != null;
    }

    public RaptureSheetRange getSheetNamedSelection(String rangeName) {
        return rangeMap.get(rangeName);
    }

    public List<RaptureSheetRange> getSheetNamedSelections() {
        return ImmutableList.<RaptureSheetRange>builder().addAll(rangeMap.values()).build();
    }

    public RaptureSheetStyle putSheetStyle(RaptureSheetStyle style) {
        RaptureSheetStyle storable = JacksonUtil.objectFromJson(JacksonUtil.jsonFromObject(style), RaptureSheetStyle.class);
        return styleMap.put(style.getName(), storable);
    }

    public Boolean deleteSheetStyle(String styleName) {
        return styleMap.remove(styleName) != null;
    }

    public List<RaptureSheetStyle> getSheetStyles() {
        return ImmutableList.<RaptureSheetStyle>builder().addAll(styleMap.values()).build();

    }
}
