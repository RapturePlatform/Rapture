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
package rapture.kernel.sheet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureSheetCell;
import rapture.common.RaptureSheetNote;
import rapture.common.RaptureSheetRange;
import rapture.common.RaptureSheetScript;
import rapture.common.RaptureSheetStatus;
import rapture.common.RaptureSheetStyle;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.sheet.RaptureSheetStyleAlignment;
import rapture.common.sheet.RaptureSheetStyleColor;
import rapture.common.sheet.RaptureSheetStyleNumberFormat;
import rapture.common.sheet.RaptureSheetStyleSize;
import rapture.common.sheet.RaptureSheetStyleWeight;
import rapture.repo.SheetRepo;
import rapture.sheet.SheetStore;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public abstract class SheetContract {

    private static final String AUTHORITY = "sheetauth1";
    private static final String SHEET_NAME = String.format("%s/riskspan/mortgage", AUTHORITY);

    private static final Logger log = Logger.getLogger(SheetContract.class);

    private static final int MAX_ROW = 10;
    private static final int MAX_COL = 20;

    private static final List<Integer> DIMENSIONS = Arrays.asList(1, 3);

    Map<Integer, Table<Integer, Integer, String>> dimToCells;

    protected abstract SheetRepo getSheetRepo();

    private SheetRepo sheetRepo;

    @Before
    public void setup() {
        sheetRepo = getSheetRepo();
        sheetRepo.createSheet(SHEET_NAME);
        sheetRepo.deleteSheet(SHEET_NAME);
        sheetRepo.createSheet(SHEET_NAME);
        dimToCells = new HashMap<Integer, Table<Integer, Integer, String>>();

        log.info("Going to insert");
        for (int dim : DIMENSIONS) {
            Table<Integer, Integer, String> table = HashBasedTable.create();
            dimToCells.put(dim, table);
            for (int row = 0; row < MAX_ROW; row++) {
                for (int col = 0; col < MAX_COL; col++) {
                    String val = createCellVal(row, col);
                    sheetRepo.setCell(SHEET_NAME, row, col, val, dim);
                    table.put(row, col, val);
                }
            }
        }

        log.info("Done inserting");

    }

    @Test
    public void testGetCell() {
        testGetCell(SHEET_NAME);
    }

    private void testGetCell(String sheetName) {
        for (int dim : DIMENSIONS) {
            for (int row = 0; row < MAX_ROW; row++) {
                for (int col = 0; col < MAX_COL; col++) {
                    String currCellValue = sheetRepo.getCell(sheetName, row, col, dim);
                    assertEquals(dimToCells.get(dim).get(row, col), currCellValue);
                }
            }
        }
    }

    @Test
    public void testAllCells() {
        testAllCells(SHEET_NAME);
    }

    private void testAllCells(String sheetName) {

        for (int dimension : DIMENSIONS) {
            List<RaptureSheetCell> cells = sheetRepo.findCellsByEpoch(sheetName, dimension, new Long(0));
            Table<Integer, Integer, String> expectedCells = dimToCells.get(dimension);
            int foundCount = 0;
            for (RaptureSheetCell rapCell : cells) {
                int row = rapCell.getRow();
                int col = rapCell.getColumn();
                assertEquals(expectedCells.get(row, col), rapCell.getData());
                foundCount++;
            }
            assertEquals(expectedCells.size(), foundCount);
        }

    }

    private String createCellVal(int row, int col) {
        return String.format("xyz(%s:%s)", row, col);
    }

    @Test
    public void testDeleteSheet() {
        for (int dimension : DIMENSIONS) {
            sheetRepo.deleteSheet(SHEET_NAME);
            assertNull(sheetRepo.findCellsByEpoch(SHEET_NAME, dimension, 0L));
        }
    }

    @Test
    public void testDeleteSheetCells() {
        int row = 1;
        int column = 3;
        String value = "afe";
        int dimension = 1;
        sheetRepo.setCell(SHEET_NAME, row, column, value, dimension);
        String cell = sheetRepo.getCell(SHEET_NAME, row, column, dimension);
        assertEquals(value, cell);
        sheetRepo.deleteSheetCell(SHEET_NAME, row, column, dimension);
        cell = sheetRepo.getCell(SHEET_NAME, row, column, dimension);
        assertNull(cell);
    }

    @Test
    public void testDeleteSheetColumn() {
        testDeleteSheetColumn(SHEET_NAME);
    }

    private void testDeleteSheetColumn(String sheetName) {
        int dimension = 100;

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                sheetRepo.setCell(sheetName, row, col, createCellVal(row, col), dimension);
            }
        }

        sheetRepo.deleteSheetColumn(sheetName, 0);
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 4; col++) {
                assertEquals(String.format("Failed at %s:%s", row, col), createCellVal(row, col + 1), sheetRepo.getCell(sheetName, row, col, dimension));
            }
            assertNull(sheetRepo.getCell(sheetName, row, 4, dimension));
        }

    }

    @Test
    public void testDeleteSheetRow() {
        int dimension = 100;

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                sheetRepo.setCell(SHEET_NAME, row, col, createCellVal(row, col), dimension);
            }
        }

        sheetRepo.deleteSheetRow(SHEET_NAME, 0);
        for (int col = 0; col < 5; col++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(createCellVal(row + 1, col), sheetRepo.getCell(SHEET_NAME, row, col, dimension));
            }
            assertNull(sheetRepo.getCell(SHEET_NAME, 4, col, dimension));
        }
    }

    @Test
    public void testScript() {
        String scriptName = "scriptName";
        RaptureSheetScript script = new RaptureSheetScript();
        script.setAutoRun(true);
        script.setDescription("desc");
        script.setScript("script body");
        sheetRepo.putSheetScript(SHEET_NAME, scriptName, script);

        RaptureSheetScript retVal = sheetRepo.getSheetScript(SHEET_NAME, scriptName);
        assertEquals(script.isAutoRun(), retVal.isAutoRun());
        assertEquals(script.getDescription(), retVal.getDescription());
        assertEquals(script.getScript(), retVal.getScript());

        assertNull(sheetRepo.getSheetScript(SHEET_NAME, scriptName + "a"));
    }

    @Test
    public void testScript2() {
        String script1 = "scriptName1";
        String script2 = "scriptName2";
        RaptureSheetScript script = new RaptureSheetScript();
        script.setAutoRun(true);
        script.setDescription("desc");
        script.setScript("script body");
        sheetRepo.putSheetScript(SHEET_NAME, script1, script);
        sheetRepo.putSheetScript(SHEET_NAME, script2, script);

        RaptureSheetScript retVal = sheetRepo.getSheetScript(SHEET_NAME, script1);
        assertEquals(script.isAutoRun(), retVal.isAutoRun());
        assertEquals(script.getDescription(), retVal.getDescription());
        assertEquals(script.getScript(), retVal.getScript());

        assertNotNull(sheetRepo.getSheetScript(SHEET_NAME, script2));
    }

    @Test
    public void testScript3() {
        String script1 = "scriptName1";
        String script2 = "scriptName2";
        RaptureSheetScript script = new RaptureSheetScript();
        script.setAutoRun(true);
        script.setName(script1);
        script.setDescription("desc");
        script.setScript("script body");
        sheetRepo.putSheetScript(SHEET_NAME, script1, script);
        script.setName(script2);
        sheetRepo.putSheetScript(SHEET_NAME, script2, script);

        List<RaptureSheetScript> scripts = sheetRepo.getSheetScripts(SHEET_NAME);
        assertEquals(2, scripts.size());
        boolean found2 = false;
        boolean found1 = false;
        for (RaptureSheetScript temp : scripts) {
            if (temp.getName().equals(script1)) {
                found1 = true;
            } else if (temp.getName().equals(script2)) {
                found2 = true;
            }
        }
        assertTrue(found1);
        assertTrue(found2);
    }

    @Test
    public void testScript4() {
        String script1 = "scriptName1";
        String script2 = "scriptName2";
        RaptureSheetScript script = new RaptureSheetScript();
        script.setAutoRun(true);
        script.setDescription("desc");
        script.setScript("script body");
        script.setName(script1);
        sheetRepo.putSheetScript(SHEET_NAME, script1, script);
        script.setName(script2);
        sheetRepo.putSheetScript(SHEET_NAME, script2, script);

        sheetRepo.deleteSheetScript(SHEET_NAME, script2);
        List<RaptureSheetScript> scripts = sheetRepo.getSheetScripts(SHEET_NAME);
        assertEquals(1, scripts.size());
        assertEquals(script1, scripts.get(0).getName());
    }

    @Test
    public void testNote() {
        RaptureSheetNote note = new RaptureSheetNote();
        note.setNote("this is the note");
        note.setWhen(new Date());
        note.setWho("who?");

        note.setId("1");
        sheetRepo.putSheetNote(SHEET_NAME, note);
        note.setId("2");
        sheetRepo.putSheetNote(SHEET_NAME, note);

        List<RaptureSheetNote> notes = sheetRepo.getSheetNotes(SHEET_NAME);
        assertEquals(2, notes.size());
        boolean found2 = false;
        boolean found1 = false;
        for (RaptureSheetNote temp : notes) {
            if (temp.getId().equals("1")) {
                found1 = true;
            } else if (temp.getId().equals("2")) {
                found2 = true;
            }
        }
        assertTrue(found1);
        assertTrue(found2);

        sheetRepo.deleteSheetNote(SHEET_NAME, "1");
        notes = sheetRepo.getSheetNotes(SHEET_NAME);
        assertEquals(1, notes.size());
        assertEquals("2", notes.get(0).getId());
    }

    @Test
    public void testScriptGen() {
    }
    @Test
    public void testRange() {
        RaptureSheetRange range1 = new RaptureSheetRange();
        range1.setStartColumn(0);
        range1.setEndColumn(1);
        range1.setName("range1");
        range1.setStartRow(5);
        range1.setEndRow(3);

        RaptureSheetRange range2 = new RaptureSheetRange();
        range2.setStartColumn(01);
        range2.setEndColumn(12);
        range2.setName("range2");
        range2.setStartRow(51);
        range2.setEndRow(31);

        sheetRepo.putSheetNamedSelection(SHEET_NAME, "range1", range1);
        sheetRepo.putSheetNamedSelection(SHEET_NAME, "range2", range2);

        List<RaptureSheetRange> retVal = sheetRepo.getSheetNamedSelections(SHEET_NAME);
        assertEquals(2, retVal.size());

        boolean found2 = false;
        boolean found1 = false;
        for (RaptureSheetRange temp : retVal) {
            if (temp.getName().equals("range1")) {
                found1 = true;
                assertEquals(JacksonUtil.jsonFromObject(range1), JacksonUtil.jsonFromObject(temp));
            } else if (temp.getName().equals("range2")) {
                found2 = true;
                assertEquals(JacksonUtil.jsonFromObject(range2), JacksonUtil.jsonFromObject(temp));
            }
        }
        assertTrue(found1);
        assertTrue(found2);

        sheetRepo.deleteSheetNamedSelection(SHEET_NAME, "range1");
        List<RaptureSheetRange> ranges = sheetRepo.getSheetNamedSelections(SHEET_NAME);
        assertEquals(1, ranges.size());
        assertEquals("range2", ranges.get(0).getName());
    }

    @Test
    public void testStyle() {
        RaptureSheetStyle style1 = new RaptureSheetStyle();
        style1.setAlignment(RaptureSheetStyleAlignment.CENTER);
        style1.setColor(RaptureSheetStyleColor.BLACK);
        style1.setName("style1");
        style1.setNumberFormat(RaptureSheetStyleNumberFormat.DECIMAL2);
        style1.setSize(RaptureSheetStyleSize.LARGE);
        style1.setWeight(RaptureSheetStyleWeight.BOLD);

        RaptureSheetStyle style2 = new RaptureSheetStyle();
        style2.setAlignment(RaptureSheetStyleAlignment.CENTER);
        style2.setColor(RaptureSheetStyleColor.BLACK);
        style2.setName("style2");
        style2.setNumberFormat(RaptureSheetStyleNumberFormat.DECIMAL2);
        style2.setSize(RaptureSheetStyleSize.LARGE);
        style2.setWeight(RaptureSheetStyleWeight.BOLD);

        sheetRepo.putSheetStyle(SHEET_NAME, style1);
        sheetRepo.putSheetStyle(SHEET_NAME, style2);

        List<RaptureSheetStyle> retVal = sheetRepo.getSheetStyles(SHEET_NAME);
        assertEquals(2, retVal.size());

        boolean found2 = false;
        boolean found1 = false;
        for (RaptureSheetStyle temp : retVal) {
            if (temp.getName().equals(style1.getName())) {
                found1 = true;
                assertEquals(JacksonUtil.jsonFromObject(style1), JacksonUtil.jsonFromObject(temp));
            } else if (temp.getName().equals(style2.getName())) {
                found2 = true;
                assertEquals(JacksonUtil.jsonFromObject(style2), JacksonUtil.jsonFromObject(temp));
            }
        }
        assertTrue(found1);
        assertTrue(found2);

        sheetRepo.deleteSheetStyle(SHEET_NAME, style2.getName());
        retVal = sheetRepo.getSheetStyles(SHEET_NAME);
        assertEquals(1, retVal.size());
        assertEquals(style1.getName(), retVal.get(0).getName());
    }

    @Test
    public void testClone() {
        // first, set up some stuff
        testStyle();
        testRange();
        testNote();
        testScript();
        String targetName = "targetTestSheet";
        sheetRepo.cloneSheet(SHEET_NAME, targetName);

        testAllCells(targetName);
        testGetCell(targetName);
        testDeleteSheetColumn(targetName);
        testAllCells(SHEET_NAME); // make sure this did not get affected

        checkMetadataEquals(sheetRepo.getSheetScripts(SHEET_NAME), sheetRepo.getSheetScripts(targetName));
        checkMetadataEquals(sheetRepo.getSheetNamedSelections(SHEET_NAME), sheetRepo.getSheetNamedSelections(targetName));
        checkMetadataEquals(sheetRepo.getSheetNotes(SHEET_NAME), sheetRepo.getSheetNotes(targetName));
        checkMetadataEquals(sheetRepo.getSheetStyles(SHEET_NAME), sheetRepo.getSheetStyles(targetName));
    }

    private <T> void checkMetadataEquals(List<T> expectedList, List<T> foundList) {
        assertTrue(String.format("Size of list is %s", expectedList.size()), expectedList.size() > 0);
        assertEquals(expectedList.size(), foundList.size());
        for (T expected : expectedList) {
            boolean isFound = true;
            String json = JacksonUtil.jsonFromObject(expected);
            for (T found : foundList) {
                String foundJson = JacksonUtil.jsonFromObject(found);
                if (json.equals(foundJson)) {
                    isFound = true;
                    continue;
                }
            }
            assertTrue("Found " + json, isFound);
        }
    }
    
    @Test
    public void testFindSheetByUriPrefix() {
        sheetRepo.createSheet("a/b/1");
        sheetRepo.createSheet("a/b/2");
        sheetRepo.createSheet("a/c/1");
        List<RaptureFolderInfo> level1 = sheetRepo.listSheetByUriPrefix("");
        assertEquals(2,  level1.size());
        
        List<RaptureFolderInfo> level2 = sheetRepo.listSheetByUriPrefix("a/");
        assertEquals(2, level2.size());
        
        List<RaptureFolderInfo> level3 = sheetRepo.listSheetByUriPrefix("a/b/");
        assertEquals(2, level3.size());
        
        List<RaptureFolderInfo> level3c = sheetRepo.listSheetByUriPrefix("a/c/");
        assertEquals(1, level3c.size());
        assertEquals("1", level3c.get(0).getName());
        assertEquals(false, level3c.get(0).isFolder());


        sheetRepo.createSheet("b/c/1");
        List<RaptureFolderInfo> b = sheetRepo.listSheetByUriPrefix("b/");
        assertEquals(1, b.size());
        assertEquals("c", b.get(0).getName());
        assertEquals(true, b.get(0).isFolder());

        List<RaptureFolderInfo> bc = sheetRepo.listSheetByUriPrefix("b/c/");
        assertEquals(1, bc.size());
        assertEquals("1", bc.get(0).getName());
        assertEquals(false, bc.get(0).isFolder());
    }
}

