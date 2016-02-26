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
package rapture.blob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

/**
 * Contract test for the BlobStore
 *
 * @author ben
 */
public abstract class BlobStoreContractTest {

    private static final String TEST_BLOB_CONTENT = "Test blob";

    private static final String TEST_DISPLAY_NAME = "test/testObj";

    private static final RaptureURI TEST_URI = new RaptureURI("blob://" + TEST_DISPLAY_NAME);

    public abstract BlobStore getBlobStore();

    private static CallingContext CONTEXT;

    @BeforeClass
    public static void beforeClass() {
        Kernel.initBootstrap();
        CONTEXT = ContextFactory.getKernelUser();
    }

    @After
    public void after() {
        long before = System.currentTimeMillis();
        getBlobStore().deleteBlob(CONTEXT, TEST_URI);
        long after = System.currentTimeMillis();
        printDiff("delete in after", before, after);
    }

    protected void printDiff(String title, long before, long after) {
        System.out.println(String.format("%s: delta=%s", title, after - before));
    }

    @Test
    public void testCreateAndRetrieveBlob() throws IOException {
        long before = System.currentTimeMillis();
        boolean result = getBlobStore().storeBlob(CONTEXT, TEST_URI, false, IOUtils.toInputStream(TEST_BLOB_CONTENT));
        long after = System.currentTimeMillis();
        printDiff("testCreateAndRetrieveBlob: storeBlob", before, after);
        assertTrue(result);
        before = System.currentTimeMillis();
        String blob = IOUtils.toString(getBlobStore().getBlob(CONTEXT, TEST_URI));
        after = System.currentTimeMillis();
        printDiff("testCreateAndRetrieveBlob: getBlob", before, after);
        assertEquals(TEST_BLOB_CONTENT, blob);
    }

    @Test
    public void testCreateAndRetrieveSymlink() throws IOException {
        RaptureURI SYMLINK_URI = new RaptureURI("blob://" + TEST_DISPLAY_NAME);
        String fileDirName = FileUtils.getTempDirectoryPath() + "/testFSRepo";
        new File(fileDirName).mkdirs();
        File foo = new File(fileDirName, "foo");
        foo.createNewFile();

        long before = System.currentTimeMillis();
        boolean result = getBlobStore().storeBlob(CONTEXT, SYMLINK_URI, false, IOUtils.toInputStream(TEST_BLOB_CONTENT));
        long after = System.currentTimeMillis();
        printDiff("testCreateAndRetrieveBlob: testCreateAndRetrieveSymlink", before, after);
        assertTrue(result);
        before = System.currentTimeMillis();
        String blob = IOUtils.toString(getBlobStore().getBlob(CONTEXT, SYMLINK_URI));
        after = System.currentTimeMillis();
        printDiff("testCreateAndRetrieveSymlink: getBlob", before, after);
        assertEquals(TEST_BLOB_CONTENT, blob);
    }

    @Test
    public void testCreateAndRetrieveEmptyBlob() throws IOException {
        long before = System.currentTimeMillis();
        boolean result = getBlobStore().storeBlob(CONTEXT, TEST_URI, false, IOUtils.toInputStream(""));
        long after = System.currentTimeMillis();
        printDiff("testCreateAndRetrieveEmptyBlob: storeBlob", before, after);
        assertTrue(result);
        before = System.currentTimeMillis();
        String blob = IOUtils.toString(getBlobStore().getBlob(CONTEXT, TEST_URI));
        after = System.currentTimeMillis();
        printDiff("testCreateAndRetrieveEmptyBlob: getBlob", before, after);
        assertEquals("", blob);
    }

    @Test
    public void testCreateAndRetrieveBlobAppend() throws IOException {
        long before = System.currentTimeMillis();
        getBlobStore().storeBlob(CONTEXT, TEST_URI, false, IOUtils.toInputStream(TEST_BLOB_CONTENT));
        long after = System.currentTimeMillis();
        printDiff("testCreateAndRetrieveBlobAppend: storeBlob1", before, after);
        before = System.currentTimeMillis();
        boolean result = getBlobStore().storeBlob(CONTEXT, TEST_URI, true, IOUtils.toInputStream(TEST_BLOB_CONTENT));
        after = System.currentTimeMillis();
        printDiff("testCreateAndRetrieveBlobAppend: storeBlob2", before, after);
        assertTrue(result);
        before = System.currentTimeMillis();
        String blob = IOUtils.toString(getBlobStore().getBlob(CONTEXT, TEST_URI));
        after = System.currentTimeMillis();
        printDiff("testCreateAndRetrieveBlobAppend: getBlob", before, after);
        assertEquals(TEST_BLOB_CONTENT + TEST_BLOB_CONTENT, blob);
    }

    @Test
    public void testCreateAndDelete() {
        long before = System.currentTimeMillis();
        boolean result = getBlobStore().storeBlob(CONTEXT, TEST_URI, false, IOUtils.toInputStream(TEST_BLOB_CONTENT));
        long after = System.currentTimeMillis();
        printDiff("testCreateAndDelete: storeBlob", before, after);
        assertTrue(result);
        before = System.currentTimeMillis();
        result = getBlobStore().deleteBlob(CONTEXT, TEST_URI);
        after = System.currentTimeMillis();
        printDiff("testCreateAndDelete: deleteBlob", before, after);
        assertTrue(result);
        before = System.currentTimeMillis();
        InputStream is = getBlobStore().getBlob(CONTEXT, TEST_URI);
        after = System.currentTimeMillis();
        printDiff("testCreateAndDelete: getBlob", before, after);
        assertNull(is);
    }

    @Test
    public void testGetBlobPart() throws IOException {
        long before = System.currentTimeMillis();
        boolean result = getBlobStore().storeBlob(CONTEXT, TEST_URI, false, IOUtils.toInputStream(TEST_BLOB_CONTENT));
        long after = System.currentTimeMillis();
        printDiff("testGetBlobPart: storeBlob", before, after);
        assertTrue(result);

        before = System.currentTimeMillis();
        String blobPart1 = IOUtils.toString(getBlobStore().getBlobPart(CONTEXT, TEST_URI, 0l, 2l), "UTF-8");
        after = System.currentTimeMillis();
        printDiff("testGetBlobPart: getBlob1", before, after);
        assertEquals(TEST_BLOB_CONTENT.substring(0, 2), blobPart1);

        before = System.currentTimeMillis();
        String blobPart2 = IOUtils.toString(getBlobStore().getBlobPart(CONTEXT, TEST_URI, 2l, 4l), "UTF-8");
        after = System.currentTimeMillis();
        printDiff("testGetBlobPart: getBlob2", before, after);
        assertEquals(TEST_BLOB_CONTENT.substring(2, 6), blobPart2);

    }

}
