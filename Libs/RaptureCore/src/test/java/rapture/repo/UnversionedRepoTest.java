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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.exception.RaptureException;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentWithMeta;
import rapture.kernel.Kernel;
import rapture.repo.meta.handler.UnversionedMetaHandler;
import rapture.repo.meta.handler.UnversionedTestHelper;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author bardhi
 * @since 11/12/14.
 */
public class UnversionedRepoTest extends RepoTestContract {
    private static final String TEST_REPO_URI1 = "//unversionedDeleteNoMetaTest";
    private static final String TEST_REPO_URI2 = "//unversionedDeleteTest";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        for (String repoUri : new String[] { TEST_REPO_URI1, TEST_REPO_URI2 }) {
            if (!Kernel.getDoc().docRepoExists(ctx, repoUri)) {
                Kernel.getDoc().createDocRepo(ctx, repoUri, getConfig(repoUri));
            }
        }
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        for (String repoUri : new String[] { TEST_REPO_URI1, TEST_REPO_URI2 }) {
            if (Kernel.getDoc().docRepoExists(ctx, repoUri)) {
                Kernel.getDoc().deleteDocRepo(ctx, repoUri);
            }
        }
    }

    @Override
    protected boolean getUsesCleanupService() {
        return false;
    }

    @Override
    protected String getConfig(String authority) {
        return "REP {} USING MEMORY {}";
    }

    @Override
    protected String batchExpectedContent() {
        return defContent2;
    }

    @Override
    protected int batchExpectedV2() {
        return UnversionedMetaHandler.DEFAULT_VERSION;
    }

    @Override
    protected int batchExpectedV1() {
        return UnversionedMetaHandler.DEFAULT_VERSION;
    }

    @Test
    public void testVersionMeta() throws InterruptedException {
        Long before1 = beforeAndSleep();
        Kernel.getDoc().putDoc(ctx, REPO_URI2 + "/1", defContent);
        Long after1 = afterAndSleep();
        DocumentMetadata md1 = Kernel.getDoc().getDocMeta(ctx, REPO_URI2 + "/1");

        Kernel.getDoc().putDoc(ctx, REPO_URI2 + "/1", defContent);
        DocumentMetadata mdDupe = Kernel.getDoc().getDocMeta(ctx, REPO_URI2 + "/1");
        assertEquals(md1, mdDupe);

        Long before2 = beforeAndSleep();
        Kernel.getDoc().putDoc(ctx, REPO_URI2 + "/1", defContent3); //make this different
        Long after2 = afterAndSleep();

        assertEquals(UnversionedMetaHandler.DEFAULT_VERSION, md1.getVersion().intValue());
        assertTrue(String.format("before1=[%s], md1 created=[%s]", new DateTime(before1), new DateTime(md1.getCreatedTimestamp())),
                before1 < md1.getCreatedTimestamp());
        assertTrue(String.format("after1=[%s], md1 created=[%s]", new DateTime(after1), new DateTime(md1.getCreatedTimestamp())),
                after1 > md1.getCreatedTimestamp());
        assertEquals(md1.getModifiedTimestamp(), md1.getCreatedTimestamp());

        DocumentMetadata md2 = Kernel.getDoc().getDocMeta(ctx, REPO_URI2 + "/1");
        assertEquals(UnversionedMetaHandler.DEFAULT_VERSION, md2.getVersion().intValue());
        assertEquals(md1.getCreatedTimestamp(), md2.getCreatedTimestamp());
        assertTrue(String.format("before2=[%s], md2 modified=[%s]", new DateTime(before2), new DateTime(md2.getModifiedTimestamp())),
                before2 < md2.getModifiedTimestamp());
        assertTrue(String.format("after2=[%s], md2 modified=[%s]", new DateTime(before1), new DateTime(md1.getModifiedTimestamp())),
                after2 > md2.getModifiedTimestamp());
        assertTrue(md2.getModifiedTimestamp() > md2.getCreatedTimestamp());
    }

    protected Long afterAndSleep() throws InterruptedException {
        Thread.sleep(1);
        return System.currentTimeMillis();
    }

    protected Long beforeAndSleep() throws InterruptedException {
        Long before2 = System.currentTimeMillis();
        Thread.sleep(1);
        return before2;
    }

    @Test
    public void testVersionRequest() {
        try {
            String content = Kernel.getDoc().getDoc(ctx, REPO_URI1 + "/1@1");
            fail("Should have gotten exception requesting specific version of unversioned repository.");
        }
        catch (RaptureException e) {}
    }

    @Test
    public void testDelete() throws InterruptedException {
        String docURI = TEST_REPO_URI2 + "/toDelete.json";
        Long before = beforeAndSleep();
        Kernel.getDoc().putDoc(ctx, docURI, defContent);
        Integer firstVersion = Kernel.getDoc().getDocMeta(ctx, docURI).getVersion();
        Kernel.getDoc().putDoc(ctx, docURI, defContent2);
        Thread.sleep(1);
        Long beforeDelete = beforeAndSleep();
        Kernel.getDoc().deleteDoc(ctx, docURI);
        DocumentWithMeta dm = Kernel.getDoc().getDocAndMeta(ctx, docURI);
        assertNull(dm.getContent());
        assertTrue(before < dm.getMetaData().getCreatedTimestamp());
        assertTrue(String.format("beforeDelete %s, createdTimestamp %s", beforeDelete, dm.getMetaData().getCreatedTimestamp()),
                beforeDelete > dm.getMetaData().getCreatedTimestamp());
        assertTrue(beforeDelete < dm.getMetaData().getModifiedTimestamp());
        assertTrue(dm.getMetaData().getDeleted());
        assertEquals(firstVersion, dm.getMetaData().getVersion());
    }

    @Test
    public void testNoMetaDelete() throws InterruptedException {
        String docURI = TEST_REPO_URI1 + "/toDelete2.json";

        UnversionedRepo repo = (UnversionedRepo) Kernel.INSTANCE.getRepo(TEST_REPO_URI1.substring(2));
        KeyStore docStore = UnversionedTestHelper.getDocStore(repo.getMetaHandler());
        String docPath = new RaptureURI(docURI, Scheme.DOCUMENT).getDocPath();
        docStore.put(docPath, defContent);

        DocumentWithMeta dm = Kernel.getDoc().getDocAndMeta(ctx, docURI);
        assertEquals(StringUtils.deleteWhitespace(defContent), StringUtils.deleteWhitespace(dm.getContent()));
        assertEquals(docPath, dm.getDisplayName());
        assertNull(dm.getMetaData());

        Long beforeDelete = beforeAndSleep();
        Kernel.getDoc().deleteDoc(ctx, docURI);
        Long afterDelete = afterAndSleep();

        dm = Kernel.getDoc().getDocAndMeta(ctx, docURI);
        assertNull(dm.getContent());
        assertEquals(docPath, dm.getDisplayName());
        assertNotNull(dm.getMetaData());

        assertTrue(beforeDelete < dm.getMetaData().getCreatedTimestamp());
        assertTrue(afterDelete > dm.getMetaData().getCreatedTimestamp());
        assertEquals(dm.getMetaData().getModifiedTimestamp(), dm.getMetaData().getCreatedTimestamp());
        assertTrue(dm.getMetaData().getDeleted());
        assertEquals(-1, dm.getMetaData().getVersion().intValue());

    }
}
