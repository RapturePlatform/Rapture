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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.client.HttpBlobApi;
import rapture.common.exception.RaptureException;
import rapture.helper.IntegrationTestHelper;
import rapture.util.ResourceLoader;

public class BlobApiTests {

    private HttpBlobApi blobApi = null;
    IntegrationTestHelper helper = null;

    /**
     * Creates an instance of HttpAdmin API that will be used in test methods
     */
    @BeforeClass(groups = { "blob", "mongo", "nightly" })
    @Parameters({ "RaptureURL", "RaptureUser", "RapturePassword" })
    public void beforeTest(@Optional("http://localhost:8665/rapture") String url, @Optional("rapture") String user, @Optional("rapture") String password) {

        helper = new IntegrationTestHelper(url, user, password);
        blobApi = helper.getBlobApi();
    }

    /**
     * Creates blog using parameter input pointing to a test file. Stores and retrieves blob data and verifies. Delete afterwards
     * 
     * @throws IOException
     */
    @Test(groups = { "blob", "nightly",
            "mongo" }, description = "Test basic operations on blob: creation, fetch content, get size, get meta data, and deletion", dataProvider = "blobFileScenarios", enabled = true)
    public void testBlobFromFile(String fileName, String contentType) throws IOException {

        RaptureURI repo = helper.getRandomAuthority(Scheme.BLOB);
        helper.configureTestRepo(repo, "MONGODB");
        String repoName = new RaptureURI.Builder(repo).docPath("").build().toString();
        String blobUri = repoName + "testblob" + System.nanoTime();
        byte[] bytes = null;
        String fullPath = System.getProperty("user.dir") + File.separator + "build" + File.separator + "resources" + File.separator + "test" + File.separator
                + fileName;
        Assert.assertTrue(new File(fullPath).exists(), "Cannot find file: " + fullPath);
        bytes = Files.readAllBytes(new File(fullPath).toPath());
        blobApi.putBlob(blobUri, bytes, contentType);
        Assert.assertEquals(new String(blobApi.getBlob(blobUri).getContent()), new String(bytes));
        Assert.assertEquals(blobApi.getBlobSize(blobUri), new Long(bytes.length));
        Map<String, String> metaDataMap = blobApi.getBlobMetaData(blobUri);
        Assert.assertTrue(metaDataMap.containsKey("Content-Type"));
        Assert.assertEquals(metaDataMap.get("Content-Length"), (new Long(bytes.length)).toString());
        Assert.assertTrue(metaDataMap.containsKey("createdTimestamp"));
        Assert.assertTrue(metaDataMap.containsKey("writeTime"));
        Assert.assertTrue(metaDataMap.containsKey("modifiedTimestamp"));
    }

    @Test(groups = { "blob", "mongo", "nightly" }, enabled = true, expectedExceptions = RaptureException.class)
    public void testNullBlobContents() {
        RaptureURI repo = helper.getRandomAuthority(Scheme.BLOB);
        helper.configureTestRepo(repo, "MONGODB");
        String repoName = new RaptureURI.Builder(repo).docPath("").build().toString();
        String blobUri = repoName + "testblob" + System.nanoTime();

        blobApi.putBlob(blobUri, null, "application/text");

    }

    @Test(groups = { "blob", "mongo", "nightly" })
    public void testBlobPut() {
        long maxContentSize = 10000L;

        Random rand = new Random();
        RaptureURI repo = helper.getRandomAuthority(Scheme.BLOB);
        helper.configureTestRepo(repo, "MONGODB");
        String repoName = new RaptureURI.Builder(repo).docPath("").build().toString();
        String blobUri = repoName + "testblob" + System.nanoTime();

        long content_size = Math.abs(rand.nextLong() % maxContentSize);
        String currBlobURI = blobUri + Thread.currentThread().getId() + "_" + content_size + "_" + System.nanoTime();
        Reporter.log("Creating URI " + currBlobURI + " with content size= " + content_size, true);
        String currContent = "";
        for (int i = 0; i < content_size; i++)
            currContent = currContent + "a";
        Reporter.log("Storing to blob: " + currBlobURI, true);
        try {
            blobApi.putBlob(currBlobURI, currContent.getBytes(), "application/text");
        } catch (Exception e) {
            Reporter.log("Exception thrown: " + e, true);
        }
        Assert.assertEquals(blobApi.getBlobSize(currBlobURI).longValue(), content_size);
        Assert.assertEquals(blobApi.getBlobMetaData(currBlobURI).get("Content-Type"), "application/text");
    }

    @Test(groups = { "blob", "mongo", "nightly" })
    public void testBlobAppend() {

        RaptureURI repo = helper.getRandomAuthority(Scheme.BLOB);
        helper.configureTestRepo(repo, "MONGODB");
        String repoName = new RaptureURI.Builder(repo).docPath("").build().toString();
        String blobUri = repoName + "testblob" + System.nanoTime();
        String currBlobURI = blobUri + Thread.currentThread().getId() + "_" + System.nanoTime();

        String currContent = "INITIAL CONTENT";
        Reporter.log("Creating URI " + currBlobURI + " with content size= " + currContent.length(), true);
        try {
            blobApi.putBlob(currBlobURI, currContent.getBytes(), "application/text");
        } catch (Exception e) {
            Reporter.log("Exception thrown: " + e, true);
        }
        Reporter.log("Appending to blob: " + currBlobURI, true);
        blobApi.addBlobContent(currBlobURI, "MORE CONTENT".getBytes());
        Assert.assertEquals(new String(blobApi.getBlob(currBlobURI).getContent()), currContent + "MORE CONTENT");

        Reporter.log("Appending to blob: " + currBlobURI, true);
        blobApi.addBlobContent(currBlobURI, "EVEN MORE CONTENT".getBytes());
        Assert.assertEquals(new String(blobApi.getBlob(currBlobURI).getContent()), currContent + "MORE CONTENT" + "EVEN MORE CONTENT");
    }

    @Test(groups = { "blob", "mongo", "nightly" })
    public void testBlobDelete() {
        RaptureURI repo = helper.getRandomAuthority(Scheme.BLOB);
        helper.configureTestRepo(repo, "MONGODB");
        String repoName = new RaptureURI.Builder(repo).docPath("").build().toString();

        // test that blob does not exists and is null before putting content
        String currBlobURI = repoName + Thread.currentThread().getId() + "_delete_" + System.nanoTime();
        Assert.assertFalse(blobApi.blobExists(currBlobURI));
        Assert.assertNull(blobApi.getBlob(currBlobURI));

        // Check that deleting non-exisitng blob returns false
        try {
            blobApi.deleteBlob(currBlobURI);
        } catch (Exception e) {
            Reporter.log("Exception thrown: " + e, true);

        }

        // test that put then delete content nullifies blob and makes it not exist
        blobApi.putBlob(currBlobURI, "TEST".getBytes(), "application/text");
        blobApi.deleteBlob(currBlobURI);
        Assert.assertFalse(blobApi.blobExists(currBlobURI));
        Assert.assertNull(blobApi.getBlob(currBlobURI));

        // test that put then appened then delete content nullifies blob and makes it not exist
        blobApi.putBlob(currBlobURI, "TEST".getBytes(), "application/text");
        blobApi.addBlobContent(currBlobURI, "MORE CONTENT".getBytes());
        blobApi.deleteBlob(currBlobURI);
        Assert.assertFalse(blobApi.blobExists(currBlobURI));
        Assert.assertNull(blobApi.getBlob(currBlobURI));
    }

    @Test(groups = { "blob", "mongo", "nightly" })
    public void testBlobDeletePut() {
        RaptureURI repo = helper.getRandomAuthority(Scheme.BLOB);
        helper.configureTestRepo(repo, "MONGODB");
        String repoName = new RaptureURI.Builder(repo).docPath("").build().toString();

        // test that blob does not exists and is null before putting content
        String currBlobURI = repoName + Thread.currentThread().getId() + "_delete_put_" + System.nanoTime();
        Assert.assertFalse(blobApi.blobExists(currBlobURI));
        Assert.assertNull(blobApi.getBlob(currBlobURI));

        // test that put then delete content nullifies blob and makes it not exist
        blobApi.putBlob(currBlobURI, "TEST".getBytes(), "application/text");
        blobApi.deleteBlob(currBlobURI);
        Assert.assertFalse(blobApi.blobExists(currBlobURI));
        Assert.assertNull(blobApi.getBlob(currBlobURI));

        blobApi.putBlob(currBlobURI, "TESTTEST".getBytes(), "application/text");
        Assert.assertTrue(blobApi.blobExists(currBlobURI));
        Assert.assertEquals(blobApi.getBlob(currBlobURI).getContent(), "TESTTEST".getBytes());
    }

    @Test(groups = { "blob", "mongo",
            "nightly" }, description = "overwrite an application/text blob with an application/text blob of same size.", dataProvider = "blobOverwriteScenarios", enabled = true)
    public void overwriteExistingTextBlobTest(int originalContentSize, int newContentSize) {

        RaptureURI repo = helper.getRandomAuthority(Scheme.BLOB);
        helper.configureTestRepo(repo, "MONGODB");
        String repoName = new RaptureURI.Builder(repo).docPath("").build().toString();

        // write original blob to blob store
        String orgContent = "";
        for (long j = 0; j < originalContentSize; j++) {
            orgContent = orgContent + "a";
        }
        // write the blob
        String blobURI = repoName + "/testoverwrite";
        try {
            blobApi.putBlob(blobURI, orgContent.getBytes(), "application/text");
        } catch (Exception e) {
            Reporter.log("Exception thrown: " + e, true);

        }

        // get the blob from store
        String retrievedOrgContent = new String(blobApi.getBlob(blobURI).getContent());
        Reporter.log("Original blob contents: " + retrievedOrgContent, true);

        Assert.assertEquals(retrievedOrgContent, orgContent, "Compare retrieved blob data to original blob data written to same repo.");

        // overwrite the original blob with a new one
        String newContent = "";
        for (long j = 0; j < newContentSize; j++) {
            newContent = newContent + "b";
        }
        Reporter.log("Overwriting original blob with: " + newContent, true);

        try {
            blobApi.putBlob(blobURI, newContent.getBytes(), "application/text");
        } catch (Exception e) {
            Reporter.log("Exception thrown: " + e, true);

        }
        String retrievedNewContent = new String(blobApi.getBlob(blobURI).getContent());
        Reporter.log("Overwritten blob contents: " + retrievedNewContent, true);

        Assert.assertEquals(retrievedNewContent, newContent, "Blob should be overwritten by newContent bx100");
    }

    @Test(groups = { "blob", "mongo", "nightly" }, description = "overwrite an application/pdf blob with a different blob type.", enabled = true)
    public void overwriteExistingPDFBlobWithTextBlobTest() throws FileNotFoundException {

        RaptureURI repo = helper.getRandomAuthority(Scheme.BLOB);
        helper.configureTestRepo(repo, "MONGODB");
        String repoName = new RaptureURI.Builder(repo).docPath("").build().toString();

        // load file1 and store in blob store
        String path = getFilePath(this, "/blob/small-pdf-file.pdf");
        Reporter.log("Loading pdf: " + path, true);
        byte[] putOrgData = getFileAsBytes(path);
        String blobURI = repoName + "/over_write_test";

        try {
            blobApi.putBlob(blobURI, putOrgData, "application/pdf");
        } catch (Exception e) {
            Reporter.log("Exception thrown: " + e, true);
        }

        byte[] retrievedOrgData = blobApi.getBlob(blobURI).getContent();
        Assert.assertEquals(retrievedOrgData, putOrgData, "Compare retrieved blob data to original blob data written to same repo.");

        // load a test file2 and store in same blob store
        String putNewData = ResourceLoader.getResourceAsString(this, "/blob/simple_blob_test.txt");

        try {
            blobApi.putBlob(blobURI, putNewData.getBytes(), "application/text");
        } catch (Exception e) {
            Reporter.log("Exception thrown: " + e, true);
        }

        byte[] retrievedNewData = blobApi.getBlob(blobURI).getContent();

        Assert.assertEquals(retrievedNewData, putNewData.getBytes(), "Compare retrieved blob data to original blob data written to same repo.");

    }

    @Test(groups = { "blob", "mongo", "nightly" }, enabled = true)
    public void testBlobRepositoryCreation() {
        RaptureURI repo = helper.getRandomAuthority(Scheme.BLOB);
        helper.configureTestRepo(repo, "MONGODB");
        String repoName = new RaptureURI.Builder(repo).docPath("").build().toString();

        Assert.assertTrue(blobApi.blobRepoExists(repoName));

    }

    private static String getFilePath(Object context, String resourcePath) {

        String realPath = context.getClass().getResource(resourcePath).getPath();

        return new File(realPath).getPath();
    }

    private static byte[] getFileAsBytes(String filePath) {
        byte[] returnVal = null;
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(filePath);
            returnVal = IOUtils.toByteArray(fis);
        } catch (Exception e) {
            throw new RuntimeException("We had problems with " + filePath + " --- " + e);
        }

        return returnVal;
    }

    @AfterClass(groups = { "blob", "mongo", "nightly" })
    public void AfterTest() {
        helper.cleanAllAssets();
    }

    @DataProvider
    public Object[][] blobFileScenarios() {
        return new Object[][] {
                new Object[] { "blob" + File.separator + "simple_blob_test.txt", "text/plain" },
                new Object[] { "blob" + File.separator + "small-pdf-file.pdf", "application/pdf" },
                new Object[] { "blob" + File.separator + "small_csv_file.csv", "text/csv" },
                new Object[] { "blob" + File.separator + "small-jpg-file.jpg", "image/jpeg" },
        };
    }

    @DataProvider
    public Object[][] blobOverwriteScenarios() {
        return new Object[][] {
                new Object[] { 100, 100 },
                new Object[] { 50, 100 },
                new Object[] { 100, 50 },
                new Object[] { 50, 1 },
                new Object[] { 1, 50 },
        };
    }

}
