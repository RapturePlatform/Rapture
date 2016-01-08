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
package rapture.sheet.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureSheetNote;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.SheetApiImpl;

/**
 * Created by zanniealvarez on 6/25/15.
 */
public class FileSheetStoreTest {

    CallingContext context;
    String repoURIString;
    String folderURI;
    Integer numFiles = 3;
    Integer numFolders = 3;

    private static final Logger log = Logger.getLogger(FileSheetStoreTest.class);
    private static final String authority = "snuffy";
    private static final String REPO_USING_FILE = "REP {} USING FILE {prefix=\"/tmp/" + authority + "\"}";
    private static final File temp = new File("/tmp/" + authority);
    private static final String sheetAuthorityURI = "sheet://" + authority;
    private static final String sheetURI = sheetAuthorityURI + "/brain/salad/surgery";

    String saveInitSysConfig;
    String saveRaptureRepo;

    private CallingContext callingContext;
    private SheetApiImpl sheetImpl;

    @Before
    public void setUp() throws Exception {

        temp.mkdir();
        temp.deleteOnExit();
        RaptureConfig.setLoadYaml(false);
        RaptureConfig config = ConfigLoader.getConf();
        saveRaptureRepo = config.RaptureRepo;
        saveInitSysConfig = config.InitSysConfig;

        config.RaptureRepo = REPO_USING_FILE;
        config.InitSysConfig = "NREP {} USING FILE { prefix=\"/tmp/" + authority + "/sys.config\"}";

        System.setProperty("LOGSTASH-ISENABLED", "false");
        this.callingContext = new CallingContext();
        this.callingContext.setUser("dummy");

        Kernel.initBootstrap();
        callingContext = ContextFactory.getKernelUser();

        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using FILE {prefix=\"/tmp/" + authority + "\"}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");
        sheetImpl = new SheetApiImpl(Kernel.INSTANCE);

        context = ContextFactory.getKernelUser();
        repoURIString = RaptureURI.builder(Scheme.SHEET, authority).build().toString();
        folderURI = repoURIString;

        for (int i = 0; i < numFolders; i++) {
            folderURI += "folder" + i + "/";
        }

        String sheetconfig = "SHEET {} USING FILE {prefix=\"test." + authority + ".FILE\"}";

        sheetImpl.createSheetRepo(context, repoURIString, sheetconfig);

        for (int i = 0; i < numFiles; i++) {
            String fileUri = folderURI + "/asdf" + i;

            sheetImpl.createSheet(context, fileUri);

            int sheetRows = 10;
            int sheetCols = 10;
            int sheetDims = 2;

            for (int currDim = 0; currDim <= sheetDims; currDim++) {
                for (int currColumn = 0; currColumn < sheetCols; currColumn++) {
                    for (int currRow = 0; currRow < sheetRows; currRow++) {
                        sheetImpl.setSheetCell(context, fileUri, currRow, currColumn, "VALUE1=" + currRow + "," + currColumn, currDim);
                    }
                }
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        Map<String, RaptureFolderInfo> allChildrenMap = sheetImpl.listSheetsByUriPrefix(context, folderURI, 0);
        for (Map.Entry<String, RaptureFolderInfo> entry : allChildrenMap.entrySet()) {
            sheetImpl.deleteSheet(context, entry.getKey());
        }
        sheetImpl.deleteSheetRepo(context, repoURIString);
        ConfigLoader.getConf().InitSysConfig = saveInitSysConfig;
        ConfigLoader.getConf().RaptureRepo = saveRaptureRepo;
    }

    @Test
    public void testFindSheetsByUriPrefixOneLevel() throws Exception {
        Map<String, RaptureFolderInfo> children = sheetImpl.listSheetsByUriPrefix(context, folderURI, 1);
        assertEquals(numFiles.intValue(), children.values().size());
    }

    @Test
    public void testFindSheetsByUriPrefix() throws Exception {
        Map<String, RaptureFolderInfo> allChildrenMap = sheetImpl.listSheetsByUriPrefix(context, repoURIString, 0);
        assertEquals(numFiles.intValue() + numFolders.intValue(), allChildrenMap.size());
    }

    @Test
    public void testGetSheetNotes() throws Exception {
        for (int i = 0; i < 10; i++) {
            RaptureSheetNote not = new RaptureSheetNote();
            not.setId("12345" + i);
            not.setNote("this is a dummy note");
            Date sup = new Date();
            sup.setTime(System.currentTimeMillis());
            not.setWhen(sup);
            sheetImpl.createSheetNote(context, folderURI + "/asdf0", not);
        }

        List<RaptureSheetNote> lst2 = sheetImpl.getSheetNotes(context, folderURI + "/asdf0");
        for (RaptureSheetNote elem : lst2) {
            System.out.println(elem.toString());
        }
        assertEquals(10, lst2.size());
        assertEquals("123459", lst2.get(9).getId());

    }

    @Test
    public void testGetSheetNotesWithoutID() throws Exception {
        for (int i = 0; i < 10; i++) {
            RaptureSheetNote not = new RaptureSheetNote();
            not.setNote("this is a dummy note");
            Date sup = new Date();
            sup.setTime(System.currentTimeMillis());
            not.setWhen(sup);
            sheetImpl.createSheetNote(context, folderURI + "/asdf1", not);
        }
        List<RaptureSheetNote> lst = sheetImpl.getSheetNotes(context, folderURI + "/asdf1");
        for (RaptureSheetNote note : lst) {
            System.out.println(note.toString());
        }
        assertEquals(10, lst.size());
    }

    @Test
    public void testGetCell() throws Exception {
        assertEquals("VALUE1=0,0", sheetImpl.getSheetCell(context, folderURI + "/asdf0", 0, 0, 0));
        sheetImpl.deleteSheetCell(context, folderURI + "/asdf0", 0, 0, 0);
        assertNull(sheetImpl.getSheetCell(context, folderURI + "/asdf0", 0, 0, 0));
    }
}
