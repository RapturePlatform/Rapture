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
package rapture.repo;

import rapture.common.*;
import rapture.sheet.SheetStore;

import java.util.List;

/**
 * Created by seanchen on 7/2/15.
 */
public class SheetRepo {

    private SheetStore store;

    public SheetRepo(SheetStore store) {
        this.store = store;
    }

    public void setCell(String sheetName, int row, int column, String string, int tabId) {
        // TODO RAP-3141 Put graphite code here/figure out how to measure data size
        store.setCell(sheetName, row, column, string, tabId);
    }

    public String getCell(String sheetName, int row, int column, int tabId) {
        return store.getCell(sheetName, row, column, tabId);
    }

    public RaptureSheet createSheet(String sheetName) {
        // TODO RAP-3141 Does this count as a write
        return store.createSheet(sheetName);
    }

    public RaptureSheet deleteSheet(String sheetName) {
        return store.deleteSheet(sheetName);
    }

    public List<RaptureFolderInfo> listSheetsByUriPrefix(String sheetUriPrefix) {
        return store.listSheetsByUriPrefix(sheetUriPrefix);
    }

    public List<RaptureSheetCell> findCellsByEpoch(String sheetName, int tabId, long epoch) {
        return store.findCellsByEpoch(sheetName, tabId, epoch);
    }

    public List<RaptureSheetStyle> getSheetStyles(String sheetName) {
        return store.getSheetStyles(sheetName);
    }

    public Boolean deleteSheetStyle(String sheetName, String styleName) {
        return store.deleteSheetStyle(sheetName, styleName);
    }

    public RaptureSheetStyle putSheetStyle(String sheetName, RaptureSheetStyle style) {
        // TODO RAP-3141 Does this count as a write
        return store.putSheetStyle(sheetName, style);
    }

    public List<RaptureSheetScript> getSheetScripts(String sheetName) {
        return store.getSheetScripts(sheetName);
    }

    public Boolean deleteSheetScript(String sheetName, String scriptName) {
        return store.deleteSheetScript(sheetName, scriptName);
    }

    public RaptureSheetScript putSheetScript(String sheetName, String scriptName, RaptureSheetScript script) {
        // TODO RAP-3141 Does this count as a write
        return store.putSheetScript(sheetName, scriptName, script);
    }

    public List<RaptureSheetRange> getSheetNamedSelections(String sheetName) {
        return store.getSheetNamedSelections(sheetName);
    }

    public Boolean deleteSheetNamedSelection(String sheetName, String rangeName) {
        return store.deleteSheetNamedSelection(sheetName, rangeName);
    }

    public RaptureSheetRange putSheetNamedSelection(String sheetName, String rangeName, RaptureSheetRange range) {
        return store.putSheetNamedSelection(sheetName, rangeName, range);
    }

    public RaptureSheetScript getSheetScript(String sheetName, String scriptName) {
        return store.getSheetScript(sheetName, scriptName);
    }

    public void cloneSheet(String srcName, String targetName) {
        // TODO RAP-3141 Does this count as a write
        store.cloneSheet(srcName, targetName);
    }

    public RaptureSheetRange getSheetNamedSelection(String sheetName, String rangeName) {
        return store.getSheetNamedSelection(sheetName, rangeName);
    }

    public List<RaptureSheetNote> getSheetNotes(String sheetName) {
        return store.getSheetNotes(sheetName);
    }

    public Boolean deleteSheetNote(String sheetName, String noteId) {
        return store.deleteSheetNote(sheetName, noteId);
    }

    public RaptureSheetNote putSheetNote(String sheetName, RaptureSheetNote note) {
        // TODO RAP-3141 Does this count as a write
        return store.putSheetNote(sheetName, note);
    }

    public Boolean deleteSheetColumn(String sheetUri, int column) {
        return store.deleteSheetColumn(sheetUri, column);
    }

    public Boolean deleteSheetRow(String sheetUri, int row) {
        return store.deleteSheetRow(sheetUri, row);
    }

    public Boolean deleteSheetCell(String sheetUri, int row, int column, int tabId) {
        return store.deleteSheetCell(sheetUri, row, column, tabId);
    }

    public Boolean setBlock(String sheetUri, int startRow, int startColumn, List<String> values, int height, int width,
                            int tabId) {
        // TODO RAP-3141 Put graphite code here/figure out how to measure data size
        return store.setBlock(sheetUri, startRow, startColumn, values, height, width, tabId);
    }

    public Boolean sheetExists(String sheetUri) {
        return store.sheetExists(sheetUri);
    }

    public void drop() {
        store.drop();
    }
}
