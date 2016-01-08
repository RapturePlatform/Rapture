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
package rapture.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentWithMeta;
import rapture.kernel.Kernel;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VersionedRepoTest extends RepoTestContract {
    private static final String REPO_URI = "//versionedDeleteTest";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (!Kernel.getDoc().docRepoExists(ctx, REPO_URI)) {
            Kernel.getDoc().createDocRepo(ctx, REPO_URI, getConfig(REPO_URI));
        }
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    protected boolean getUsesCleanupService() {
        return false;
    }

    @Override
    protected String getConfig(String authority) {
        return "NREP {} USING MEMORY {}";
    }

    @Override
    protected String batchExpectedContent() {
        return defContent;
    }

    @Override
    protected int batchExpectedV2() {
        return 2;
    }

    @Override
    protected int batchExpectedV1() {
        return 1;
    }

    @Test
    public void testRevert() {
        Kernel.getDoc().revertDoc(ctx, REPO_URI1 + "/1");
        String content = Kernel.getDoc().getDoc(ctx, REPO_URI1 + "/1");
        assertEquals(StringUtils.deleteWhitespace(defContent), StringUtils.deleteWhitespace(content));
    }

    @Test
    public void testVersionsAndDates() throws InterruptedException {
        Long before1 = beforeAndSleep();
        Kernel.getDoc().putDoc(ctx, REPO_URI2 + "/1", defContent);
        Long after1 = afterAndSleep();
        DocumentMetadata md1 = Kernel.getDoc().getDocMeta(ctx, REPO_URI2 + "/1");

        Kernel.getDoc().putDoc(ctx, REPO_URI2 + "/1", defContent);
        DocumentMetadata mdDupe = Kernel.getDoc().getDocMeta(ctx, REPO_URI2 + "/1");
        assertEquals(md1, mdDupe);

        Long before2 = beforeAndSleep();
        Kernel.getDoc().putDoc(ctx, REPO_URI2 + "/1", defContent2);
        Long after2 = afterAndSleep();

        assertEquals(1, md1.getVersion().intValue());
        assertTrue(before1 < md1.getCreatedTimestamp());
        assertTrue(String.format("after=%s, time =%s", after1, md1.getCreatedTimestamp()), after1 > md1.getCreatedTimestamp());
        assertEquals(md1.getModifiedTimestamp(), md1.getCreatedTimestamp());

        DocumentMetadata md2 = Kernel.getDoc().getDocMeta(ctx, REPO_URI2 + "/1");
        assertEquals(2, md2.getVersion().intValue());
        assertTrue(before2 < md2.getModifiedTimestamp());
        assertTrue(after2 > md2.getModifiedTimestamp());
        assertEquals(md1.getCreatedTimestamp(), md2.getCreatedTimestamp());
    }

    protected Long beforeAndSleep() throws InterruptedException {
        Long time = System.currentTimeMillis();
        Thread.sleep(1);
        return time;
    }

    protected long afterAndSleep() throws InterruptedException {
        Thread.sleep(1);
        return System.currentTimeMillis();
    }

    @Test
    public void testOKOptimistic() {
        Kernel.getDoc().putDoc(ctx, REPO_URI1 + "/opt", defContent);
        boolean ret1 = Kernel.getDoc().putDocWithVersion(ctx, REPO_URI1 + "/opt", defContent2, 1);
        assertTrue(ret1);
        boolean ret2 = Kernel.getDoc().putDocWithVersion(ctx, REPO_URI1 + "/opt", defContent3, 1);
        assertFalse(ret2);
    }

    @Test
    public void testVersionRequest() {
        String content = Kernel.getDoc().getDoc(ctx, REPO_URI1 + "/1@1");
        assertEquals(StringUtils.deleteWhitespace(defContent), StringUtils.deleteWhitespace(content));
    }

    @Test
    public void testDelete() throws InterruptedException {
        String docURI = REPO_URI + "/toDelete.json";

        Kernel.getDoc().putDoc(ctx, docURI, defContent);
        DocumentMetadata md1 = Kernel.getDoc().getDocMeta(ctx, docURI);
        Integer firstVersion = md1.getVersion();
        Thread.sleep(1);
        Kernel.getDoc().putDoc(ctx, docURI, defContent2);
        DocumentMetadata md2 = Kernel.getDoc().getDocMeta(ctx, docURI);
        Long beforeDelete = beforeAndSleep();
        Kernel.getDoc().deleteDoc(ctx, docURI);
        DocumentWithMeta dm = Kernel.getDoc().getDocAndMeta(ctx, docURI);
        assertNull(dm.getContent());
        DocumentMetadata md3 = dm.getMetaData();
        assertTrue(String.format("beforeDelete %s, createdTimestamp %s", beforeDelete, md3.getCreatedTimestamp()),
                beforeDelete > md3.getCreatedTimestamp());
        assertTrue(beforeDelete < md3.getModifiedTimestamp());
        assertTrue(md3.getDeleted());
        assertEquals(firstVersion + 2, md3.getVersion().intValue());
        assertEquals(md2.getCreatedTimestamp(), md1.getCreatedTimestamp());
        assertEquals(md3.getCreatedTimestamp(), md1.getCreatedTimestamp());
    }
}
