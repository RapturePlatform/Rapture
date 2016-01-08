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

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rapture.common.*;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.repo.KeyStore;
import rapture.repo.mem.MemKeyStore;
import rapture.sheet.SheetStore;

public class MemorySheet implements SheetStore {
    private Map<String, InMemorySheet> sheets = new HashMap<String, InMemorySheet>();
    private String authority;

    KeyStore sheetStore = new MemKeyStore();

    public MemorySheet() {
        Map<String, String> config = new HashMap<String, String>();
        sheetStore.setConfig(config);
    }

    @Override
    public void setCell(String sheetName, int row, int column, String string, int dimension) {
        if (!sheets.containsKey(sheetName)) {
            createSheet(sheetName);
        }
        InMemorySheet sheet = sheets.get(sheetName);
        sheet.setCell(dimension, row, column, string);
    }

    @Override
    public Boolean setBlock(String sheetName, int startRow, int startColumn, List<String> values, int width,
            int height, int dimension) {
        InMemorySheet sheet = sheets.get(sheetName);
        int currentRow = startRow;
        int currentColumn = startColumn;
        int columnCount = 0;
        for (String val : values) {
            sheet.setCell(dimension, currentRow, currentColumn, val);
            currentColumn++;
            columnCount++;
            if (columnCount >= width) {
                currentRow++;
                currentColumn = startColumn;
                columnCount = 0;
            }
        }
        return true;
    }

    @Override
    public RaptureSheet createSheet(String name) {
        sheets.put(name, new InMemorySheet());
        sheetStore.put(name, name);
        RaptureSheet ret = new RaptureSheet();
        ret.setName(name);
        ret.setAuthority(authority);

        return ret;
    }

    @Override
    public RaptureSheet deleteSheet(String name) {
        sheets.remove(name);
        sheetStore.delete(name);
        RaptureSheet ret = new RaptureSheet();
        ret.setName(name);
        ret.setAuthority(authority);
        return ret;
    }

    @Override
    public List<RaptureFolderInfo> listSheetByUriPrefix(String prefix) {
        return sheetStore.getSubKeys(prefix);
    }

    @Override
    public void setConfig(String authority, Map<String, String> config) {
        this.authority = authority;
    }

    @Override
    public String getCell(String sheetName, int row, int column, int dimension) {
        InMemorySheet sheet = sheets.get(sheetName);
        return sheet.getCell(dimension, row, column);
    }

    @Override
    public List<RaptureSheetCell> findCellsByEpoch(String sheetName, int dimension, long epoch) {
        InMemorySheet sheet = sheets.get(sheetName);
        if (sheet != null) {
            return sheet.getCells(dimension, epoch).getCells();
        } else {
            return null;
        }
    }

    @Override
    public List<RaptureSheetStyle> getSheetStyles(String name) {
        InMemorySheet sheet = sheets.get(name);
        return sheet.getSheetStyles();
    }

    @Override
    public Boolean deleteSheetStyle(String name, String styleName) {
        InMemorySheet sheet = sheets.get(name);
        return sheet.deleteSheetStyle(styleName);
    }

    @Override
    public RaptureSheetStyle putSheetStyle(String name, RaptureSheetStyle style) {
        InMemorySheet sheet = sheets.get(name);
        return sheet.putSheetStyle(style);
    }

    @Override
    public RaptureSheetScript getSheetScript(String name, String scriptName) {
        InMemorySheet sheet = sheets.get(name);
        return sheet.getSheetScript(scriptName);
    }

    @Override
    public List<RaptureSheetScript> getSheetScripts(String name) {
        InMemorySheet sheet = sheets.get(name);
        return sheet.getSheetScripts();
    }

    @Override
    public Boolean deleteSheetScript(String name, String scriptName) {
        InMemorySheet sheet = sheets.get(name);
        return sheet.deleteSheetScript(scriptName);
    }

    @Override
    public RaptureSheetScript putSheetScript(String name, String scriptName, RaptureSheetScript script) {
        InMemorySheet sheet = sheets.get(name);
        return sheet.putSheetScript(scriptName, script);
    }

    @Override
    public List<RaptureSheetRange> getSheetNamedSelections(String name) {
        InMemorySheet sheet = sheets.get(name);
        return sheet.getSheetNamedSelections();
    }

    @Override
    public RaptureSheetRange getSheetNamedSelection(String name, String rangeName) {
        InMemorySheet sheet = sheets.get(name);
        return sheet.getSheetNamedSelection(rangeName);
    }

    @Override
    public Boolean deleteSheetNamedSelection(String name, String rangeName) {
        InMemorySheet sheet = sheets.get(name);
        return sheet.deleteSheetNamedSelection(rangeName);
    }

    @Override
    public RaptureSheetRange putSheetNamedSelection(String name, String rangeName, RaptureSheetRange range) {
        InMemorySheet sheet = sheets.get(name);
        return sheet.putSheetNamedSelection(rangeName, range);
    }

    @Override
    public void cloneSheet(String srcName, String targetName) {
        InMemorySheet source = sheets.get(srcName);
        if (source == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    String.format("Source sheet %s does not exist", srcName));
        }

        createSheet(targetName);
        InMemorySheet target = new InMemorySheet(source);
        sheets.put(targetName, target);

    }

    @Override
    public List<RaptureSheetNote> getSheetNotes(String name) {
        InMemorySheet sheet = sheets.get(name);
        return sheet.getSheetNotes();
    }

    @Override
    public Boolean deleteSheetNote(String name, String noteId) {
        InMemorySheet sheet = sheets.get(name);
        return sheet.deleteSheetNote(noteId);
    }

    @Override
    public RaptureSheetNote putSheetNote(String name, RaptureSheetNote note) {
        InMemorySheet sheet = sheets.get(name);
        return sheet.putSheetNote(note);
    }

    @Override
    public Boolean deleteSheetColumn(String sheetName, int column) {
        /**
         * Two steps: 1. Delete contents in this column 2. Shift all other
         * columns greater than this one over
         * 
         */
        InMemorySheet sheet = sheets.get(sheetName);
        if (sheet != null) {
            for (int row = 0; row <= sheet.getMaxRow(); row++) {
                sheet.delete(row, column);
                for (int dimension : sheet.getDimensions()) {
                    for (int tempCol = column + 1; tempCol <= sheet.getMaxColumn(); tempCol++) {
                        String data = sheet.getCell(dimension, row, tempCol);
                        sheet.setCell(dimension, row, tempCol - 1, data);
                        sheet.delete(row, tempCol, dimension);
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Boolean deleteSheetRow(String sheetName, int row) {
        /**
         * Two steps 1. Delete all contents in this row 2. Shift all rows
         * greater than this one up
         */
        InMemorySheet sheet = sheets.get(sheetName);
        if (sheet != null) {
            for (int column = 0; column <= sheet.getMaxColumn(); column++) {
                sheet.delete(row, column);
                for (int dimension : sheet.getDimensions()) {
                    for (int tempRow = row + 1; tempRow <= sheet.getMaxRow(); tempRow++) {
                        String data = sheet.getCell(dimension, tempRow, column);
                        sheet.setCell(dimension, tempRow - 1, column, data);
                        sheet.delete(tempRow, column, dimension);
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Boolean deleteSheetCell(String sheetName, int row, int column, int dimension) {
        InMemorySheet sheet = sheets.get(sheetName);
        if (sheet != null) {
            return sheet.delete(row, column, dimension);
        } else {
            return false;
        }
    }

    @Override
    public Boolean sheetExists(String docPath) {
        return sheets.containsKey(docPath);
    }

    @Override
    public void drop() {
        //do nothing
    }
}
