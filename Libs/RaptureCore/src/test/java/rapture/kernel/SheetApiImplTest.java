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
package rapture.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureSheetNote;
import rapture.common.RaptureSheetRange;
import rapture.common.RaptureSheetRow;
import rapture.common.RaptureSheetScript;
import rapture.common.RaptureSheetStyle;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.SheetRepoConfig;
import rapture.common.api.SheetApi;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.sheet.RaptureSheetStyleAlignment;
import rapture.common.sheet.RaptureSheetStyleColor;
import rapture.common.sheet.RaptureSheetStyleNumberFormat;
import rapture.common.sheet.RaptureSheetStyleSize;
import rapture.common.sheet.RaptureSheetStyleWeight;

public class SheetApiImplTest extends AbstractFileTest {

    private static CallingContext callingContext;
    private static SheetApiImpl sheetImpl;
    private static final String auth = "test" + System.currentTimeMillis();
    private static final String SHEET_USING_FILE = "SHEET {} USING FILE {prefix=\"/tmp/" + auth + "\"}";
    private static final String REPO_USING_FILE = "REP {} USING FILE {prefix=\"/tmp/" + auth + "\"}";
    private static final String SAMENAME = "Use the same name in different places?";
    static String sheetAuthorityURI = "sheet://" + auth;
    static String sheetURI = sheetAuthorityURI + "/SwampThing";

    @BeforeClass
    static public void setUp() {
        AbstractFileTest.setUp();

        config.RaptureRepo = REPO_USING_FILE;
        config.InitSysConfig = "NREP {} USING FILE { prefix=\"/tmp/" + auth + "/sys.config\"}";
        // config.DefaultPipelineTaskStatus = "TABLE {} USING FILE
        // {prefix=\"/tmp/" + auth + "\"}";

        System.setProperty("LOGSTASH-ISENABLED", "false");
        callingContext = new CallingContext();
        callingContext.setUser("dummy");

        Kernel.initBootstrap();
        callingContext = ContextFactory.getKernelUser();

        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");
        sheetImpl = new SheetApiImpl(Kernel.INSTANCE);
    }

    @Test
    public void testDeleteRepo() {
        testCreateSheet();
        assertTrue(sheetImpl.sheetExists(callingContext, sheetURI));
        sheetImpl.deleteSheetRepo(callingContext, sheetAuthorityURI);
        assertFalse(sheetImpl.sheetRepoExists(callingContext, sheetAuthorityURI));
        assertFalse(sheetImpl.sheetExists(callingContext, sheetURI));
        testCreateAndGetRepo();
        assertTrue(sheetImpl.sheetRepoExists(callingContext, sheetAuthorityURI));
        assertFalse(sheetImpl.sheetExists(callingContext, sheetURI));
        testCreateSheet();
        assertTrue(sheetImpl.sheetExists(callingContext, sheetURI));
    }


    @Test
    public void testIllegalRepoPaths() {
        String repo = "sheet://";
        String docPath = repo + "x/x";
        try {
            sheetImpl.createSheetRepo(ContextFactory.getKernelUser(), repo, "SHEET {} using MEMORY {}");
            fail("Sheet repository Uri can't have a doc path component");
        } catch (RaptureException e) {
            assertEquals("Cannot create a repository without an authority", e.getMessage());
        }
        try {
            sheetImpl.createSheetRepo(ContextFactory.getKernelUser(), "", "SHEET {} using MEMORY {}");
            fail("URI cannot be null or empty");
        } catch (RaptureException e) {
            assertEquals("Argument Repository URI cannot be null or empty", e.getMessage());
        }
        try {
            sheetImpl.createSheetRepo(ContextFactory.getKernelUser(), null, "SHEET {} using MEMORY {}");
            fail("URI cannot be null or empty");
        } catch (RaptureException e) {
            assertEquals("Argument Repository URI cannot be null or empty", e.getMessage());
        }
        try {
            sheetImpl.createSheetRepo(ContextFactory.getKernelUser(), docPath, "SHEET {} using MEMORY {}");
            fail("Sheet repository Uri can't have a doc path component");
        } catch (RaptureException e) {
            assertEquals("A Repository URI may not have a document path component", e.getMessage());
        }
    }

    @Test
    public void testThatWhichShouldNotBe() {
        String dummyAuthorityURI = "sheet://dummy";
        String dummyURI = dummyAuthorityURI + "/dummy";
        try {
            sheetImpl.createSheetRepo(callingContext, dummyAuthorityURI, "SHEET {} USING FILE { }");
            sheetImpl.setSheetCell(callingContext, dummyURI, 6, 6, "Half a dozen", 6);
            fail("You can't create a repo without a prefix");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("prefix"));
        }

        // because the config gets stored even though it's not valid
        sheetImpl.deleteSheetRepo(callingContext, dummyAuthorityURI);

        try {
            sheetImpl.createSheetRepo(callingContext, dummyAuthorityURI, "SHEET {} USING FILE { prefix=\"\" }");
            sheetImpl.setSheetCell(callingContext, dummyURI, 6, 6, "Half a dozen", 6);
            fail("You can't create a repo without a valid prefix");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("prefix"));
        }

        // because the config gets stored even though it's not valid
        sheetImpl.deleteSheetRepo(callingContext, dummyAuthorityURI);

        Map<String, String> hashMap = new HashMap<>();
        try {
            sheetImpl.createSheetRepo(callingContext, dummyAuthorityURI, "SHEET {} USING FILE { }");
            sheetImpl.setSheetCell(callingContext, dummyURI, 6, 6, "Half a dozen", 6);
            fail("You can't create a repo without a prefix");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("prefix"));
        }

        // because the config gets stored even though it's not valid
        sheetImpl.deleteSheetRepo(callingContext, dummyAuthorityURI);

        hashMap.put("prefix", "");
        try {
            sheetImpl.createSheetRepo(callingContext, dummyAuthorityURI, "SHEET {} USING FILE { prefix=\"\" }");
            sheetImpl.setSheetCell(callingContext, dummyURI, 6, 6, "Half a dozen", 6);
            fail("You can't create a repo without a valid prefix");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("prefix"));
        }
        sheetImpl.deleteSheetRepo(callingContext, dummyAuthorityURI);
    }

    @Test
    public void testValidDocStore() {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("prefix", "/tmp/foo");
        sheetImpl.createSheetRepo(callingContext, "sheet://dummy2", "SHEET {} USING FILE { prefix=\"/tmp/foo\" }");
    }

    @Test
    public void testCreateAndGetRepo() {
        if (sheetImpl.sheetRepoExists(callingContext, sheetAuthorityURI)) 
            sheetImpl.deleteSheetRepo(callingContext, sheetAuthorityURI);
        sheetImpl.createSheetRepo(callingContext, sheetAuthorityURI, SHEET_USING_FILE);
        SheetRepoConfig repoConfig = sheetImpl.getSheetRepoConfig(callingContext, sheetAuthorityURI);
        assertNotNull(repoConfig);
        assertEquals(SHEET_USING_FILE, repoConfig.getConfig());
        assertEquals(auth, repoConfig.getAuthority());
    }

    @Test
    public void testGetSheetRepositories() {
        ensureRepo(sheetAuthorityURI);
        List<SheetRepoConfig> sheetRepositories = sheetImpl.getSheetRepoConfigs(callingContext);
        assertEquals(1, sheetRepositories.size());
        sheetImpl.createSheetRepo(callingContext, "sheet://somewhereelse/", SHEET_USING_FILE);
        sheetRepositories = sheetImpl.getSheetRepoConfigs(callingContext);
        assertEquals(2, sheetRepositories.size());
    }

    @Test
    public void testCreateSheet() {
        ensureRepo(sheetAuthorityURI);
        sheetImpl.createSheet(callingContext, sheetURI);
        assertTrue(sheetImpl.sheetExists(callingContext, sheetURI));
    }

    private void ensureSheet(String repo, String name) {
        sheetImpl.createSheet(callingContext, name);
    }

    private void ensureRepo(String repo) {
        if (sheetImpl.sheetRepoExists(callingContext, repo)) 
            sheetImpl.deleteSheetRepo(callingContext, repo);
        sheetImpl.createSheetRepo(callingContext, repo, SHEET_USING_FILE);
    }

    @Test
    public void createExistTest() {
        ensureRepo(sheetAuthorityURI);
        String name1 = "/Cheers/foo";
        String name2 = "/Cheers/bar";
        String sheet1 = sheetAuthorityURI + name1;
        String sheet2 = sheetAuthorityURI + name2;
        SheetApi api = sheetImpl;

        ensureSheet(sheetAuthorityURI, sheet1);
        ensureSheet(sheetAuthorityURI, sheet2);

        assertTrue(sheet1 + " doesn't seem to exist", api.sheetExists(callingContext, sheet1));
        assertNotNull("deleteSheet failed", api.deleteSheet(callingContext, sheet1));
        assertTrue(sheet2 + " should still exist", api.sheetExists(callingContext, sheet2));
        assertFalse(sheet1 + " won't go away", api.sheetExists(callingContext, sheet1));
        assertNotNull("Can't recreate sheet I deleted", api.createSheet(callingContext, sheet1));
        assertTrue(sheet1 + " doesn't seem to exist", api.sheetExists(callingContext, sheet1));
        assertNotNull("deleteSheet failed", api.deleteSheet(callingContext, sheet1));
        assertTrue(sheet2 + " should still exist", api.sheetExists(callingContext, sheet2));
        assertFalse(sheet1 + " won't go away", api.sheetExists(callingContext, sheet1));
    }

    @Test
    public void testDeleteSheetRepo() {
        String sheet = auth + "/TheGuardian";
        ensureRepo(sheetAuthorityURI);
        ensureSheet(sheetAuthorityURI, sheet);
        sheetImpl.deleteSheetRepo(callingContext, sheetAuthorityURI);
        assertFalse(sheetImpl.sheetExists(callingContext, sheet));
    }

    @Test
    public void writeToSheet() {
        String sheet = auth + "/TheGuardian";
        ensureRepo(sheetAuthorityURI);
        ensureSheet(sheetAuthorityURI, sheet);
        sheetImpl.setSheetCell(callingContext, sheet, 6, 6, "Six", 6);
        sheetImpl.setSheetCell(callingContext, sheet, 123, 999, " a!@£$%^&*bc ", 6);
        // Can row and column numbers be negative? Should that be an error?
        sheetImpl.setSheetCell(callingContext, sheet, -1, -7, "даве тонг", 6);
        assertEquals("Six", sheetImpl.getSheetCell(callingContext, sheet, 6, 6, 6));
        assertNull(sheetImpl.getSheetCell(callingContext, sheet, 5, 5, 5));

        // Writing this should append an entry to the Sheet.
        // It won't remove the previous entry.
        // When the sheet is read in the first entry will be overwritten by the
        // second.
        sheetImpl.setSheetCell(callingContext, sheet, 6, 6, "Half a dozen", 6);
        assertEquals("Half a dozen", sheetImpl.getSheetCell(callingContext, sheet, 6, 6, 6));

        sheetImpl.cloneSheet(callingContext, sheet, sheet + "_clone");
        assertEquals("Half a dozen", sheetImpl.getSheetCell(callingContext, sheet + "_clone", 6, 6, 6));
        // If you look at the files now you'll see that the clone doesn't have
        // the first entry but the original does
    }

    @Test
    public void deleteSheetRowColumn() {
        String sheet = auth + "/deleteSheetRowCol";
        ensureRepo(sheetAuthorityURI);
        ensureSheet(sheetAuthorityURI, sheet);
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 5; j++) {
                sheetImpl.setSheetCell(callingContext, sheet, i, j, i + "," + j, 1);
            }
        }
        assertNotNull(sheetImpl.getSheetCell(callingContext, sheet, 3, 4, 1));
        sheetImpl.deleteSheetRow(callingContext, sheet, 3);
        assertNull(sheetImpl.getSheetCell(callingContext, sheet, 3, 4, 1));
        assertNotNull(sheetImpl.getSheetCell(callingContext, sheet, 2, 4, 1));
        sheetImpl.deleteSheetColumn(callingContext, sheet, 4);
        assertNull(sheetImpl.getSheetCell(callingContext, sheet, 2, 4, 1));
        assertNotNull(sheetImpl.getSheetCell(callingContext, sheet, 4, 3, 1));
    }

    @Test
    public void testMetaDataNote() {
        String sheet = auth + "/metadata";
        ensureRepo(sheetAuthorityURI);
        ensureSheet(sheetAuthorityURI, sheet);
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 5; j++) {
                sheetImpl.setSheetCell(callingContext, sheet, i, j, i + "," + j, 1);
            }
        }

        RaptureSheetNote note = new RaptureSheetNote();
        note.setNote("Semibreve");
        note.setWho("Roger Daltry");
        note.setId("ego");
        note.setWhen(new Date());

        // Note (ha!) that we don't pass the ID twice
        sheetImpl.createSheetNote(callingContext, sheet, note);
        List<RaptureSheetNote> notes = sheetImpl.getSheetNotes(callingContext, sheet);
        assertEquals(1, notes.size());
        assertEquals(JacksonUtil.jsonFromObject(note), JacksonUtil.jsonFromObject(notes.get(0)));

        note.setNote("quaver");
        note.setWho("Pete Townsend");
        note.setId("superego");
        note.setWhen(new Date());

        sheetImpl.deleteSheetNote(callingContext, sheet, "NOT THERE");
        sheetImpl.deleteSheetNamedSelection(callingContext, sheet, "NOT THERE");
        sheetImpl.removeStyle(callingContext, sheet, "NOT THERE");
        sheetImpl.removeScript(callingContext, sheet, "NOT THERE");

        // Note (ha!) that we don't pass the ID twice
        sheetImpl.createSheetNote(callingContext, sheet, note);
        notes = sheetImpl.getSheetNotes(callingContext, sheet);
        assertEquals(2, notes.size());

        sheetImpl.deleteSheetNote(callingContext, sheet, "ego");
        notes = sheetImpl.getSheetNotes(callingContext, sheet);
        assertEquals(1, notes.size());
        assertEquals(JacksonUtil.jsonFromObject(note), JacksonUtil.jsonFromObject(notes.get(0)));
    }

    @Test
    public void testMetaDataRange() {
        String sheet = auth + "/metadata";
        ensureRepo(sheetAuthorityURI);
        ensureSheet(sheetAuthorityURI, sheet);
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 5; j++) {
                sheetImpl.setSheetCell(callingContext, sheet, i, j, i + "," + j, 1);
            }
        }

        RaptureSheetRange homeOnThe = new RaptureSheetRange();
        homeOnThe.setName(SAMENAME);
        homeOnThe.setStartColumn(2);
        homeOnThe.setEndColumn(4);
        homeOnThe.setStartRow(6);
        homeOnThe.setEndRow(8);
        String json = JacksonUtil.jsonFromObject(homeOnThe);

        // Again - Why pass the name twice?
        sheetImpl.createSheetNamedSelection(callingContext, sheet, SAMENAME, homeOnThe);
        List<RaptureSheetRange> ranges = sheetImpl.getSheetNamedSelections(callingContext, sheet);
        assertEquals(1, ranges.size());
        assertEquals(json, JacksonUtil.jsonFromObject(ranges.get(0)));

        List<RaptureSheetRow> copy = sheetImpl.getSheetNamedSelection(callingContext, sheet, homeOnThe.getName());
        assertNotNull(copy);

        sheetImpl.deleteSheetNote(callingContext, sheet, "NOT THERE");
        sheetImpl.deleteSheetNamedSelection(callingContext, sheet, "NOT THERE");
        sheetImpl.removeStyle(callingContext, sheet, "NOT THERE");
        sheetImpl.removeScript(callingContext, sheet, "NOT THERE");

        sheetImpl.createSheetNamedSelection(callingContext, sheet, "NewName", homeOnThe);
        ranges = sheetImpl.getSheetNamedSelections(callingContext, sheet);
        assertEquals(2, ranges.size());

        sheetImpl.deleteSheetNamedSelection(callingContext, sheet, SAMENAME);
        ranges = sheetImpl.getSheetNamedSelections(callingContext, sheet);
        assertEquals(1, ranges.size());
        assertEquals(JacksonUtil.jsonFromObject(homeOnThe), JacksonUtil.jsonFromObject(ranges.get(0)));
    }

    @Test
    public void testMetaDataScript() {
        String sheet = auth + "/metadata";
        ensureRepo(sheetAuthorityURI);
        ensureSheet(sheetAuthorityURI, sheet);
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 5; j++) {
                sheetImpl.setSheetCell(callingContext, sheet, i, j, i + "," + j, 1);
            }
        }

        RaptureSheetScript jestersTear = new RaptureSheetScript();
        jestersTear.setName(SAMENAME);
        jestersTear.setAutoRun(false);
        jestersTear.setDescription("first album");
        jestersTear.setIntervalInSeconds(17); // OK so that was The Cure
        jestersTear.setLastRun(new Date());
        jestersTear.setScript("// So here I am once more in the playground of the broken hearts");
        String json = JacksonUtil.jsonFromObject(jestersTear);

        // Script already has a name. Passing in a different name changes it? Is
        // make no sense.
        sheetImpl.createScript(callingContext, sheet, SAMENAME, jestersTear);
        List<RaptureSheetScript> scripts = sheetImpl.getAllScripts(callingContext, sheet);
        assertEquals(1, scripts.size());
        assertEquals(json, JacksonUtil.jsonFromObject(scripts.get(0)));

        RaptureSheetScript copy = sheetImpl.getSheetScript(callingContext, sheet, jestersTear.getName());
        assertEquals(json, JacksonUtil.jsonFromObject(copy));

        sheetImpl.deleteSheetNote(callingContext, sheet, "NOT THERE");
        sheetImpl.deleteSheetNamedSelection(callingContext, sheet, "NOT THERE");
        sheetImpl.removeStyle(callingContext, sheet, "NOT THERE");
        sheetImpl.removeScript(callingContext, sheet, "NOT THERE");

        sheetImpl.createScript(callingContext, sheet, "NewName", jestersTear);
        scripts = sheetImpl.getAllScripts(callingContext, sheet);
        assertEquals(2, scripts.size());

        sheetImpl.removeScript(callingContext, sheet, SAMENAME);
        scripts = sheetImpl.getAllScripts(callingContext, sheet);
        assertEquals(1, scripts.size());
        assertEquals(JacksonUtil.jsonFromObject(jestersTear), JacksonUtil.jsonFromObject(scripts.get(0)));

    }

    @Test
    public void testMetaDataStyle() {
        String sheet = auth + "/metadata";
        ensureRepo(sheetAuthorityURI);
        ensureSheet(sheetAuthorityURI, sheet);
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 5; j++) {
                sheetImpl.setSheetCell(callingContext, sheet, i, j, i + "," + j, 1);
            }
        }

        RaptureSheetStyle gangnam = new RaptureSheetStyle();
        gangnam.setName(SAMENAME);
        gangnam.setAlignment(RaptureSheetStyleAlignment.CENTER);
        gangnam.setColor(RaptureSheetStyleColor.BLACK);
        gangnam.setNumberFormat(RaptureSheetStyleNumberFormat.NORMAL);
        gangnam.setSize(RaptureSheetStyleSize.MEDIUM);
        gangnam.setWeight(RaptureSheetStyleWeight.NORMAL);
        String json = JacksonUtil.jsonFromObject(gangnam);

        // Passing the name twice again.
        sheetImpl.createStyle(callingContext, sheet, gangnam);
        List<RaptureSheetStyle> styles = sheetImpl.getAllStyles(callingContext, sheet);
        assertEquals(1, styles.size());
        assertEquals(json, JacksonUtil.jsonFromObject(styles.get(0)));

        sheetImpl.deleteSheetNote(callingContext, sheet, "NOT THERE");
        sheetImpl.deleteSheetNamedSelection(callingContext, sheet, "NOT THERE");
        sheetImpl.removeStyle(callingContext, sheet, "NOT THERE");
        sheetImpl.removeScript(callingContext, sheet, "NOT THERE");

        gangnam.setName("NewName");
        sheetImpl.createStyle(callingContext, sheet, gangnam);
        styles = sheetImpl.getAllStyles(callingContext, sheet);
        assertEquals(2, styles.size());

        sheetImpl.removeStyle(callingContext, sheet, SAMENAME);
        styles = sheetImpl.getAllStyles(callingContext, sheet);
        assertEquals(1, styles.size());
        assertEquals(JacksonUtil.jsonFromObject(gangnam), JacksonUtil.jsonFromObject(styles.get(0)));

    }

    @Test
    public void testlistByUriPrefix() {
        CallingContext callingContext = getCallingContext();
        ensureRepo(sheetAuthorityURI);

        String uriPrefix = sheetAuthorityURI + "/uriFragment/";
        ensureSheet(sheetAuthorityURI, uriPrefix + "sheet1");
        ensureSheet(sheetAuthorityURI, uriPrefix + "sheet2");
        ensureSheet(sheetAuthorityURI, uriPrefix + "folder1/sheet3");
        ensureSheet(sheetAuthorityURI, uriPrefix + "folder1/sheet4");
        ensureSheet(sheetAuthorityURI, uriPrefix + "folder1/folder2/sheet5");
        ensureSheet(sheetAuthorityURI, uriPrefix + "folder1/folder2/sheet6");

        Map<String, RaptureFolderInfo> resultsMap;
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, uriPrefix, -1);
        assertEquals(8, resultsMap.size());
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, uriPrefix, 0);
        assertEquals(8, resultsMap.size());
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, uriPrefix, 4);
        assertEquals(8, resultsMap.size());
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, uriPrefix, 3);
        assertEquals(8, resultsMap.size());

        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, uriPrefix, 2);
        assertEquals(6, resultsMap.size());
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, uriPrefix, 1);
        assertEquals(3, resultsMap.size());
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, uriPrefix + "/folder1", 1);
        assertEquals(3, resultsMap.size());
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, uriPrefix + "/folder1", 2);
        assertEquals(5, resultsMap.size());

        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, sheetAuthorityURI, -1);
        assertEquals(9, resultsMap.size());
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, sheetAuthorityURI, 0);
        assertEquals(9, resultsMap.size());
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, sheetAuthorityURI, 4);
        assertEquals(9, resultsMap.size());
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, sheetAuthorityURI, 3);
        assertEquals(7, resultsMap.size());
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, sheetAuthorityURI, 2);
        assertEquals(4, resultsMap.size());
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, sheetAuthorityURI, 1);
        assertEquals(1, resultsMap.size());

        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, "sheet://", 1);
        assertEquals(1, resultsMap.size());
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, "sheet://", 2);
        assertEquals(2, resultsMap.size());
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, "sheet://", 3);
        assertEquals(5, resultsMap.size());
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, "sheet://", 4);
        assertEquals(8, resultsMap.size());
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, "sheet://", 5);
        assertEquals(10, resultsMap.size());
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, "sheet://", 6);
        assertEquals(10, resultsMap.size());
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, "sheet://", 0);
        assertEquals(10, resultsMap.size());
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, "sheet://", -1);
        assertEquals(10, resultsMap.size());

    }

    private void sleepWhileCleanup() throws InterruptedException {
        Thread.sleep(1000);
    }

    @Test
    public void testDeleteByUriPrefix() throws InterruptedException {
        CallingContext callingContext = getCallingContext();
        ensureRepo(sheetAuthorityURI);

        String uriPrefix = sheetAuthorityURI + "/uriFragment/";
        ensureSheet(sheetAuthorityURI, uriPrefix + "sheet1");
        ensureSheet(sheetAuthorityURI, uriPrefix + "sheet2");
        ensureSheet(sheetAuthorityURI, uriPrefix + "folder1/sheet3");
        ensureSheet(sheetAuthorityURI, uriPrefix + "folder1/sheet4");
        ensureSheet(sheetAuthorityURI, uriPrefix + "folder1/folder2/sheet5");
        ensureSheet(sheetAuthorityURI, uriPrefix + "folder1/folder2/sheet6");

        Map<String, RaptureFolderInfo> resultsMap;
        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, uriPrefix, -1);
        assertEquals(8, resultsMap.size());

        List<String> removed;
        removed = sheetImpl.deleteSheetsByUriPrefix(callingContext, uriPrefix + "folder1/folder2");
        assertEquals(2, removed.size());

        resultsMap = sheetImpl.listSheetsByUriPrefix(callingContext, uriPrefix, -1);
        assertEquals(5, resultsMap.size());
    }

}
