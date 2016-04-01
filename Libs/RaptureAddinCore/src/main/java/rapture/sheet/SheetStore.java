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
package rapture.sheet;

import java.util.List;
import java.util.Map;

import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureSheet;
import rapture.common.RaptureSheetCell;
import rapture.common.RaptureSheetNote;
import rapture.common.RaptureSheetRange;
import rapture.common.RaptureSheetScript;
import rapture.common.RaptureSheetStyle;

@Deprecated
public interface SheetStore {

    @Deprecated
    void setConfig(String authority, Map<String, String> config);

    @Deprecated
    void setCell(String sheetName, int row, int column, String string, int dimension);

    @Deprecated
    String getCell(String sheetName, int row, int column, int dimension);

    @Deprecated
    RaptureSheet createSheet(String name);

    @Deprecated
    RaptureSheet deleteSheet(String name);

    @Deprecated
    List<RaptureFolderInfo> listSheetsByUriPrefix(String displayNamePart);

    @Deprecated
    List<RaptureSheetCell> findCellsByEpoch(String name, int dimension, long epoch);

    @Deprecated
    List<RaptureSheetStyle> getSheetStyles(String name);

    @Deprecated
    Boolean deleteSheetStyle(String name, String styleName);

    @Deprecated
    RaptureSheetStyle putSheetStyle(String name, RaptureSheetStyle style);

    @Deprecated
    List<RaptureSheetScript> getSheetScripts(String name);

    @Deprecated
    Boolean deleteSheetScript(String name, String scriptName);

    @Deprecated
    RaptureSheetScript putSheetScript(String name, String scriptName, RaptureSheetScript script);

    @Deprecated
    List<RaptureSheetRange> getSheetNamedSelections(String name);

    @Deprecated
    Boolean deleteSheetNamedSelection(String name, String rangeName);

    @Deprecated
    RaptureSheetRange putSheetNamedSelection(String name, String rangeName, RaptureSheetRange range);

    @Deprecated
    RaptureSheetScript getSheetScript(String name, String scriptName);

    @Deprecated
    void cloneSheet(String srcName, String targetName);

    @Deprecated
    RaptureSheetRange getSheetNamedSelection(String name, String rangeName);

    @Deprecated
    List<RaptureSheetNote> getSheetNotes(String name);

    @Deprecated
    Boolean deleteSheetNote(String name, String noteId);

    @Deprecated
    RaptureSheetNote putSheetNote(String name, RaptureSheetNote note);

    @Deprecated
    Boolean deleteSheetColumn(String docPath, int column);

    @Deprecated
    Boolean deleteSheetRow(String docPath, int row);

    @Deprecated
    Boolean deleteSheetCell(String docPath, int row, int column, int dimension);

    @Deprecated
    Boolean sheetExists(String docPath);

    @Deprecated
    Boolean setBlock(String docPath, int startRow, int startColumn, List<String> values, int height, int width, int dimension);

    // Drop the sheet store
    @Deprecated
    void drop();

}
