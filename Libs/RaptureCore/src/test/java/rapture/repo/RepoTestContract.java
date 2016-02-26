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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import rapture.common.CallingContext;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.model.DocumentWithMeta;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.series.children.cleanup.CleanupServiceWithListeners;
import rapture.series.children.cleanup.FolderCleanupService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class RepoTestContract {

    private static final int FOLDER_TIMEOUT = 30;
    protected CallingContext ctx;
    protected static final String defContent = "{ \"alan\" : 1 }";
    protected static final String defContent2 = "{ \"alan\" : 2 }";
    protected static final String defContent3 = "{ \"alan\" : 3 }";
    private LatchCleanupListener cleanupListener;

    protected static final String REPO_URI1 = "//testRepo1";
    protected static final String REPO_URI2 = "//testRepo2";
    private static final String REPO_URI3 = "//folder.test.repo";

    protected RepoTestContract() {
    }

    @Before
    public void setUp() throws Exception {
        Kernel.INSTANCE.restart();
        Kernel.initBootstrap();
        ctx = ContextFactory.getKernelUser();
        if (!Kernel.getDoc().docRepoExists(ctx, REPO_URI1)) {
            Kernel.getDoc().createDocRepo(ctx, REPO_URI1, getConfig(authorityForUri(REPO_URI1)));
        }
        if (!Kernel.getDoc().docRepoExists(ctx, REPO_URI2)) {
            Kernel.getDoc().createDocRepo(ctx, REPO_URI2, getConfig(authorityForUri(REPO_URI2)));
        }
        if (!Kernel.getDoc().docRepoExists(ctx, REPO_URI3)) {
            Kernel.getDoc().createDocRepo(ctx, REPO_URI3, getConfig(authorityForUri(REPO_URI3)));
        }

        // Now put some data in there
        Kernel.getDoc().putDoc(ctx, REPO_URI1 + "/1", defContent);
        Kernel.getDoc().putDoc(ctx, REPO_URI1 + "/1", defContent2);

        FolderCleanupService cleanupService = FolderCleanupService.getInstance();
        cleanupListener = new LatchCleanupListener(authorityForUri(REPO_URI3), getUsesCleanupService());
        if (cleanupService instanceof CleanupServiceWithListeners) 
            ((CleanupServiceWithListeners) cleanupService).addListener(cleanupListener);
    }

    @After
    public void tearDown() throws Exception {
        for (String repoUri : new String[] { REPO_URI1, REPO_URI2, REPO_URI3 }) {
            if (Kernel.getDoc().docRepoExists(ctx, repoUri)) {
                Kernel.getDoc().deleteDocRepo(ctx, repoUri);
            }
        }
        FolderCleanupService cleanupService = FolderCleanupService.getInstance();
        if (cleanupService instanceof CleanupServiceWithListeners) 
            ((CleanupServiceWithListeners) cleanupService).removeListener(cleanupListener);
    }

    private String authorityForUri(String uri) {
        return new RaptureURI(uri, Scheme.DOCUMENT).getAuthority();
    }

    protected abstract boolean getUsesCleanupService();

    protected abstract String getConfig(String authority);

    @Test
    public void testDeleteDocsByUriPrefixMiddle() {
        Kernel.getDoc().putDoc(ctx, REPO_URI1 + "/top/middle1/b1", defContent);
        Kernel.getDoc().putDoc(ctx, REPO_URI1 + "/top/middle1/b2", defContent);
        Kernel.getDoc().putDoc(ctx, REPO_URI1 + "/top/middle1/b3", defContent);
        Kernel.getDoc().putDoc(ctx, REPO_URI1 + "/top/middle2/b1", defContent);

        String middle = REPO_URI1 + "/top/middle1";
        Map<String, RaptureFolderInfo> map = Kernel.getDoc().listDocsByUriPrefix(ctx, middle, 0);
        assertEquals(3, map.size());
        String top = REPO_URI1 + "/top";
        map = Kernel.getDoc().listDocsByUriPrefix(ctx, top, 0);
        assertEquals(6, map.size());

        Kernel.getDoc().deleteDocsByUriPrefix(ctx, middle);

        try {
            map = Kernel.getDoc().listDocsByUriPrefix(ctx, middle, 0);
            assertEquals(0, map.size());
        } catch (Exception e) {
            // exception is OK because it no longer exists
        }
        map = Kernel.getDoc().listDocsByUriPrefix(ctx, top, 0);
        assertEquals(2, map.size());
    }

    @Test
    public void testDeleteDocsByUriPrefixTop() {
        Kernel.getDoc().putDoc(ctx, REPO_URI1 + "/top/middle1/b1", defContent);
        Kernel.getDoc().putDoc(ctx, REPO_URI1 + "/top/middle2/b2", defContent);
        Kernel.getDoc().putDoc(ctx, REPO_URI1 + "/top/middle3/b3", defContent);

        String top = REPO_URI1 + "/top";
        Map<String, RaptureFolderInfo> map = Kernel.getDoc().listDocsByUriPrefix(ctx, top, 0);
        assertEquals(6, map.size());

        Kernel.getDoc().deleteDocsByUriPrefix(ctx, top);

        try {
            map = Kernel.getDoc().listDocsByUriPrefix(ctx, top, 0);
            assertEquals(0, map.size());
        } catch (Exception e) {
            // exception is OK because it's been deleted
        }
    }

    @Test
    public void testVersionInfo() {
        String content = Kernel.getDoc().getDoc(ctx, REPO_URI1 + "/1");
        assertEquals(StringUtils.deleteWhitespace(defContent2), StringUtils.deleteWhitespace(content));
    }

    @Test
    public void testGetDocAndMetas() {
        String[] uris = { REPO_URI1 + "/xx1", REPO_URI1 + "/xx2", REPO_URI1 + "/xx3", REPO_URI1 + "/xx4" };
        for (String uri : uris) {
            Kernel.getDoc().putDoc(ctx, uri, defContent);
        }

        List<DocumentWithMeta> dwms = Kernel.getDoc().getDocAndMetas(ctx, Arrays.asList(uris));
        assertEquals(4, dwms.size());
        int i = 1;
        for (DocumentWithMeta dwm : dwms) {
            assertEquals(StringUtils.deleteWhitespace(defContent), StringUtils.deleteWhitespace(dwm.getContent()));
            assertEquals(batchExpectedV1(), dwm.getMetaData().getVersion().intValue());
            assertEquals("xx" + i++, dwm.getDisplayName());
        }

        for (String uri : uris) {
            Kernel.getDoc().putDoc(ctx, uri, defContent2);
        }

        dwms = Kernel.getDoc().getDocAndMetas(ctx, Arrays.asList(uris));
        assertEquals(4, dwms.size());
        i = 1;
        for (DocumentWithMeta dwm : dwms) {
            assertEquals(StringUtils.deleteWhitespace(defContent2), StringUtils.deleteWhitespace(dwm.getContent()));
            assertEquals(batchExpectedV2(), dwm.getMetaData().getVersion().intValue());
            assertEquals("xx" + i++, dwm.getDisplayName());
        }

        for (int j = 0; j < uris.length; j++) {
            uris[j] = uris[j] + "@1";
        }

        dwms = Kernel.getDoc().getDocAndMetas(ctx, Arrays.asList(uris));
        assertEquals(4, dwms.size());
        i = 1;
        for (DocumentWithMeta dwm : dwms) {
            assertEquals(StringUtils.deleteWhitespace(batchExpectedContent()), StringUtils.deleteWhitespace(dwm.getContent()));
            assertEquals(batchExpectedV1(), dwm.getMetaData().getVersion().intValue());
            assertEquals("xx" + i++, dwm.getDisplayName());
        }

    }

    @Test
    public void testFolderRemovedOnDelete1() throws InterruptedException, NoSuchFieldException, IllegalAccessException {
        List<String> docURIs = Arrays.asList(createDocName("file1"), createDocName("file2"));

        Map<String, RaptureFolderInfo> children = Kernel.getDoc().listDocsByUriPrefix(ctx, REPO_URI3, 0);
        assertEquals(0, children.size());

        for (String docURI : docURIs) {
            Kernel.getDoc().putDoc(ctx, docURI, "{}");
        }

        children = Kernel.getDoc().listDocsByUriPrefix(ctx, REPO_URI3, 0);
        assertEquals(3, children.size());

        cleanupListener.resetLatches(1, 0);
        for (String docURI : docURIs) {
            Kernel.getDoc().deleteDoc(ctx, docURI);
        }
        if (!cleanupListener.getDeletedLatch().await(FOLDER_TIMEOUT, TimeUnit.SECONDS)) {
            fail(String.format("deleted: %s; ignored: %s", cleanupListener.getDeleted(), cleanupListener.getDeleted()));
        }

        sleepWhileCleanup();
        children = Kernel.getDoc().listDocsByUriPrefix(ctx, REPO_URI3, 0);
        assertEquals(0, children.size());
        if (getUsesCleanupService()) {
            assertEquals("top", cleanupListener.getDeleted().get(0));
            assertEquals("deleted are " + cleanupListener.getDeleted(), 1, cleanupListener.getDeleted().size());
            assertTrue("ignored are " + cleanupListener.getIgnored(), cleanupListener.getIgnored().size() <= 1);
        }
    }

    @Test
    public void testFolderRemovedOnDelete2() throws InterruptedException, NoSuchFieldException, IllegalAccessException {
        String doc1 = createDocName("middle1/file1");
        String doc2 = createDocName("middle2/file2");
        String doc3 = createDocName("middle2/file3");

        Map<String, RaptureFolderInfo> children = Kernel.getDoc().listDocsByUriPrefix(ctx, REPO_URI3, 0);
        assertEquals(0, children.size());

        Kernel.getDoc().putDoc(ctx, doc1, "{}");
        Kernel.getDoc().putDoc(ctx, doc2, "{}");
        Kernel.getDoc().putDoc(ctx, doc3, "{}");

        children = Kernel.getDoc().listDocsByUriPrefix(ctx, REPO_URI3, 0);
        assertEquals(6, children.size());

        cleanupListener.resetLatches(1, 1);
        Kernel.getDoc().deleteDoc(ctx, doc1);
        if (!cleanupListener.getDeletedLatch().await(FOLDER_TIMEOUT, TimeUnit.SECONDS) || !cleanupListener.getIgnoredLatch()
                .await(FOLDER_TIMEOUT, TimeUnit.SECONDS)) {
            fail(String.format("deleted: %s; ignored: %s", cleanupListener.getDeleted(), cleanupListener.getDeleted()));
        }
        sleepWhileCleanup();
        children = Kernel.getDoc().listDocsByUriPrefix(ctx, REPO_URI3, 0);
        assertEquals(4, children.size());
        if (getUsesCleanupService()) {
            assertEquals("top/middle1", cleanupListener.getDeleted().get(0));
            assertEquals(1, cleanupListener.getDeleted().size());
            assertEquals("top", cleanupListener.getIgnored().get(0));
            assertEquals(1, cleanupListener.getIgnored().size());
        }

        cleanupListener.resetLatches(2, 0);
        Kernel.getDoc().deleteDoc(ctx, doc2);
        Kernel.getDoc().deleteDoc(ctx, doc3);
        if (!cleanupListener.getDeletedLatch().await(FOLDER_TIMEOUT, TimeUnit.SECONDS) || !cleanupListener.getIgnoredLatch()
                .await(FOLDER_TIMEOUT, TimeUnit.MINUTES)) {
            fail(String.format("deleted: %s; ignored: %s", cleanupListener.getDeleted(), cleanupListener.getDeleted()));
        }

        sleepWhileCleanup();
        children = Kernel.getDoc().listDocsByUriPrefix(ctx, REPO_URI3, 0);
        assertEquals("children are " + children, 0, children.size());
        if (getUsesCleanupService()) {
            assertEquals("top/middle1", cleanupListener.getDeleted().get(0));
            assertEquals("top/middle2", cleanupListener.getDeleted().get(1));
            assertEquals("top", cleanupListener.getDeleted().get(2));
            assertEquals(3, cleanupListener.getDeleted().size());
            assertTrue(String.format("We must have ignored at least 1 by now, list is: %s", cleanupListener.getIgnored()),
                    cleanupListener.getIgnored().size() > 0);
        }
    }

    private void sleepWhileCleanup() throws InterruptedException {
        if (getUsesCleanupService()) {
            Thread.sleep(1000);
        }
    }

    private String createDocName(String name) {
        return REPO_URI3 + "/top/" + name;
    }

    protected abstract String batchExpectedContent();

    protected abstract int batchExpectedV2();

    protected abstract int batchExpectedV1();

}
