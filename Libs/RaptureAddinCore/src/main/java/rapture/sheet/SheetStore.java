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
package rapture.sheet;

import java.util.List;
import java.util.Map;

import rapture.common.*;

public interface SheetStore {

    void setConfig(String authority, Map<String, String> config);

    void setCell(String sheetName, int row, int column, String string, int dimension);

    String getCell(String sheetName, int row, int column, int dimension);

    RaptureSheet createSheet(String name);

    RaptureSheet deleteSheet(String name);

    List<RaptureFolderInfo> listSheetByUriPrefix(String displayNamePart);

    List<RaptureSheetCell> findCellsByEpoch(String name, int dimension, long epoch);

    List<RaptureSheetStyle> getSheetStyles(String name);

    Boolean deleteSheetStyle(String name, String styleName);

    RaptureSheetStyle putSheetStyle(String name, RaptureSheetStyle style);

    List<RaptureSheetScript> getSheetScripts(String name);

    Boolean deleteSheetScript(String name, String scriptName);

    RaptureSheetScript putSheetScript(String name, String scriptName, RaptureSheetScript script);

    List<RaptureSheetRange> getSheetNamedSelections(String name);

    Boolean deleteSheetNamedSelection(String name, String rangeName);

    RaptureSheetRange putSheetNamedSelection(String name, String rangeName, RaptureSheetRange range);

    RaptureSheetScript getSheetScript(String name, String scriptName);

    void cloneSheet(String srcName, String targetName);

    RaptureSheetRange getSheetNamedSelection(String name, String rangeName);

    List<RaptureSheetNote> getSheetNotes(String name);

    Boolean deleteSheetNote(String name, String noteId);

    RaptureSheetNote putSheetNote(String name, RaptureSheetNote note);

    Boolean deleteSheetColumn(String docPath, int column);

    Boolean deleteSheetRow(String docPath, int row);

    Boolean deleteSheetCell(String docPath, int row, int column, int dimension);

    Boolean sheetExists(String docPath);

    Boolean setBlock(String docPath, int startRow, int startColumn, List<String> values, int height, int width,
            int dimension);

    // Drop the sheet store
    void drop();

}
