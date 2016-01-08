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
package rapture.kernel;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import rapture.common.BlobContainer;
import rapture.common.CallingContext;
import rapture.common.ContentEnvelope;
import rapture.common.Messages;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.BlobApi;
import rapture.common.exception.RaptureException;
import rapture.common.model.BlobRepoConfig;
import rapture.kernel.schemes.RepoSchemeContractTest;

public class BlobApiImplTest extends RepoSchemeContractTest {

    private static CallingContext callingContext;
    private static BlobApiImpl blobImpl;
    private static final String BLOB_USING_MEMORY = "BLOB {} USING MEMORY {}";
    private static final String REPO_USING_MEMORY = "REP {} USING MEMORY {}";

    private static final byte[] SAMPLE_BLOB = "This is a blob".getBytes();
    
    private static final String auth = "test" + System.currentTimeMillis();
    static String blobAuthorityURI = "blob://"+auth+"/";
    static String blobName = "ItCameFromOuterSpace";
    static String blobURI = blobAuthorityURI + blobName;

    @Before
    public void setUp() {
        System.setProperty("LOGSTASH-ISENABLED", "false");
        super.before();
        callingContext = ContextFactory.getKernelUser();

        Kernel.INSTANCE.clearRepoCache(false);
        blobImpl = new BlobApiImpl(Kernel.INSTANCE);
    }

    @After
    public void after() {
        super.after();
    }
    
    @AfterClass
    public static void cleanUp() {
        blobImpl.deleteBlobRepo(callingContext, blobAuthorityURI);
        blobImpl.deleteBlobRepo(callingContext, "blob://somewhereelse/");
    }

    static boolean firstTime = true;

    @Test 
    public void testIllegalRepoPaths() {
        String repo = "blob://";
        String docPath = repo+"x/x";
        try {
            blobImpl.createBlobRepo(ContextFactory.getKernelUser(), repo, "BLOB {} using MEMORY {}", "REP {} using MEMORY {}");
            fail("Cannot create a repository without an authority");
        } catch (RaptureException e) {
            if (!e.getMessage().startsWith("Cant find resource for bundle"))
                assertEquals("Cannot create a repository without an authority", e.getMessage());
        }
        try {
            blobImpl.createBlobRepo(ContextFactory.getKernelUser(), "", "BLOB {} using MEMORY {}", "REP {} using MEMORY {}");
            fail("Repository URI cannot be null or empty");
        } catch (RaptureException e) {
            if (!e.getMessage().startsWith("Cant find resource for bundle"))
                assertEquals("Argument Repository URI cannot be null or empty", e.getMessage());
        }
        try {
            blobImpl.createBlobRepo(ContextFactory.getKernelUser(), null, "BLOB {} using MEMORY {}", "REP {} using MEMORY {}");
            fail("Repository URI cannot be null or empty");
        } catch (RaptureException e) {
            if (!e.getMessage().startsWith("Cant find resource for bundle"))
                assertEquals("Argument Repository URI cannot be null or empty", e.getMessage());
        }
        try {
            blobImpl.createBlobRepo(ContextFactory.getKernelUser(), docPath, "BLOB {} using MEMORY {}", "REP {} using MEMORY {}");
            fail("Repository URI can't have a document path component");
        } catch (RaptureException e) {
            if (!e.getMessage().startsWith("Cant find resource for bundle"))
                assertEquals("A Repository URI may not have a document path component", e.getMessage());
        }
    }    

    
    @Test
    public void testCreateAndGetRepo() {
        if (!firstTime && blobImpl.blobRepoExists(callingContext, blobAuthorityURI)) return;
        blobImpl.createBlobRepo(callingContext, blobAuthorityURI, BLOB_USING_MEMORY, REPO_USING_MEMORY);
        firstTime = false;
        BlobRepoConfig blobRepoConfig = blobImpl.getBlobRepoConfig(callingContext, blobAuthorityURI);
        assertNotNull(blobRepoConfig);
        assertEquals(BLOB_USING_MEMORY, blobRepoConfig.getConfig());
        assertEquals(REPO_USING_MEMORY, blobRepoConfig.getMetaConfig());
        assertEquals(auth, blobRepoConfig.getAuthority());
    }

    @Test
    public void testGetBlobRepositories() {
        testCreateAndGetRepo();
        List<BlobRepoConfig> blobRepositories = blobImpl.getBlobRepoConfigs(callingContext);
//        assertEquals(1, blobRepositories.size());
        blobImpl.createBlobRepo(callingContext, "blob://somewhereelse/", BLOB_USING_MEMORY, REPO_USING_MEMORY);
        List<BlobRepoConfig> newBlobRepositories = blobImpl.getBlobRepoConfigs(callingContext);
        assertEquals(blobRepositories.size()+1, newBlobRepositories.size());
    }

    @Test
    public void testPutAndGetBlob() {
        testCreateAndGetRepo();
        blobImpl.putBlob(callingContext, blobURI, SAMPLE_BLOB, "application/text");
        BlobContainer blob = blobImpl.getBlob(callingContext, blobURI);
        assertArrayEquals(SAMPLE_BLOB, blob.getContent());
    }

    // See RAP-2797
    @Ignore
    @Test
    public void testPutAndGetBlobWithAttribute() {
        testCreateAndGetRepo();
        RaptureURI blobURIWithAttribute = new RaptureURI.Builder(Scheme.BLOB, auth).docPath("Foo").attribute("Bar").build();
        blobImpl.putBlob(callingContext, blobURIWithAttribute.toString(), SAMPLE_BLOB, "application/text");
        BlobContainer blob = blobImpl.getBlob(callingContext, blobURIWithAttribute.toString());
        assertArrayEquals(SAMPLE_BLOB, blob.getContent());
    }

    @Test
    public void testGetWithNoScheme() {
        testPutAndGetBlob();
        BlobContainer blob = blobImpl.getBlob(callingContext, "//"+auth+"/"+blobName);
        assertArrayEquals(SAMPLE_BLOB, blob.getContent());
    }

    @Test
    public void testDeleteBlob() {
        testCreateAndGetRepo();
        blobImpl.deleteBlob(callingContext, blobURI);
        BlobContainer blob = blobImpl.getBlob(callingContext, blobURI);
        assertNull(blob);
    }

    @Override
    public void testPutAndGetContent() {
        testCreateAndGetRepo();
        super.testPutAndGetContent();
    }
    
    @Test()
    public void testDeleteBlobRepo() {
        testCreateAndGetRepo();
        blobImpl.deleteBlobRepo(callingContext, blobAuthorityURI);
        try {
            blobImpl.getBlob(callingContext, blobURI);
            fail("Repository should have been deleted");
        } catch (RaptureException e) {
            assertEquals(Messages.getMessage("Api", "NoSuchRepo", new String[] { new RaptureURI(blobAuthorityURI).toAuthString() } , null).format(), e.getMessage());
        }
    }

    @Test
    public void testGetBlobMetaData() {
        testPutAndGetBlob();
        Map<String, String> metaData = blobImpl.getBlobMetaData(callingContext, blobURI);
        assertEquals(metaData.get("Content-Type"), "application/text");
    }

    @Test
    public void testGetSize() {
        testPutAndGetBlob();
        Long blobSize = blobImpl.getBlobSize(callingContext, blobURI);
        assertEquals(14L, blobSize.longValue());
    }

    @Test
    public void testListBlobsByUriPrefix() {
        testCreateAndGetRepo();

        String uriPrefix = blobAuthorityURI + "uriFragment/";
        blobImpl.putBlob(callingContext, uriPrefix + "blob1", SAMPLE_BLOB, "application/text");
        blobImpl.putBlob(callingContext, uriPrefix + "blob2", SAMPLE_BLOB, "application/text");
        blobImpl.putBlob(callingContext, uriPrefix + "folder1/blob3", SAMPLE_BLOB, "application/text");
        blobImpl.putBlob(callingContext, uriPrefix + "folder1/blob4", SAMPLE_BLOB, "application/text");
        blobImpl.putBlob(callingContext, uriPrefix + "folder1/folder2/blob5", SAMPLE_BLOB, "application/text");
        blobImpl.putBlob(callingContext, uriPrefix + "folder1/folder2/blob6", SAMPLE_BLOB, "application/text");

        Map<String, RaptureFolderInfo> resultsMap;
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext, uriPrefix, -1);
        assertEquals(8, resultsMap.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext, uriPrefix, 0);
        assertEquals(8, resultsMap.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext, uriPrefix, 4);
        assertEquals(8, resultsMap.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext, uriPrefix, 3);
        assertEquals(8, resultsMap.size());

        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext, uriPrefix, 2);
        assertEquals(6, resultsMap.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext, uriPrefix, 1);
        assertEquals(3, resultsMap.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext, uriPrefix+"/folder1", 1);
        assertEquals(3, resultsMap.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext, uriPrefix+"/folder1", 2);
        assertEquals(5, resultsMap.size());
        
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext, blobAuthorityURI, -1);
        assertEquals(9, resultsMap.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext, blobAuthorityURI, 0);
        assertEquals(9, resultsMap.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext, blobAuthorityURI, 4);
        assertEquals(9, resultsMap.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext, blobAuthorityURI, 3);
        assertEquals(7, resultsMap.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext, blobAuthorityURI, 2);
        assertEquals(4, resultsMap.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext, blobAuthorityURI, 1);
        assertEquals(1, resultsMap.size());
        String str = resultsMap.keySet().toArray(new String[1])[0];
        assertEquals(blobAuthorityURI+"uriFragment/", str);
        
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext,  "blob://", 1);
        assertEquals(1, resultsMap.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext,  "blob://", 2);
        assertEquals(2, resultsMap.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext,  "blob://", 3);
        assertEquals(5, resultsMap.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext,  "blob://", 4);
        assertEquals(8, resultsMap.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext,  "blob://", 5);
        assertEquals(10, resultsMap.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext,  "blob://", 6);
        assertEquals(10, resultsMap.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext,  "blob://", 0);
        assertEquals(10, resultsMap.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext,  "blob://", -1);
        assertEquals(10, resultsMap.size());

    }

    @Override
    public Object getDocument() {

        BlobContainer bc = new BlobContainer();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(ContentEnvelope.CONTENT_SIZE, "14");
        headers.put(ContentEnvelope.CONTENT_TYPE_HEADER, "whateva");
        bc.setContent(SAMPLE_BLOB);
        bc.setHeaders(headers);
        return bc;
    }

    @Override
    public String getDocumentURI() {
        return blobURI;
    }

    @Override
    public String getBookmarkedDocumentURI() {
        return null;
    }

    @Override
    public Object getBookmarkedDocument() {
        return null;
    }
    
    @Test 
    public void testListByPrefix() {
        String fileRepo = "//fileBasedBlobRepo";
        String memRepo = "//memoryBasedBlobRepo";
        BlobApi api = Kernel.getBlob();

        if (api.blobRepoExists(callingContext, fileRepo))
            api.deleteBlobRepo(callingContext, fileRepo);
        if (api.blobRepoExists(callingContext, memRepo))
            api.deleteBlobRepo(callingContext, memRepo);
        
        api.createBlobRepo(callingContext, memRepo, "REP {} USING MEMORY { }", "REP {} USING MEMORY { }");
        api.createBlobRepo(callingContext, fileRepo, "REP {} USING FILE { prefix=\"fileBasedBlobRepo\" }", "REP {} USING MEMORY { }");
                
        blobImpl.putBlob(callingContext, memRepo + "/foo/bar/baz", SAMPLE_BLOB, "application/text");
        blobImpl.putBlob(callingContext, fileRepo + "/foo/bar/baz", SAMPLE_BLOB, "application/text");
        
        Map<String, RaptureFolderInfo> memList = api.listBlobsByUriPrefix(callingContext, memRepo+"/foo", -1);
        Map<String, RaptureFolderInfo> fileList = api.listBlobsByUriPrefix(callingContext, fileRepo+"/foo", -1);
        
        assertEquals(memList.size(), fileList.size());
        
        for (String mem : memList.keySet()) {
            String file = mem.replaceAll(memRepo, fileRepo);
            assertEquals(memList.get(mem).getName(), fileList.get(file).getName());
            assertEquals(memList.get(mem).isFolder(), fileList.get(file).isFolder());
        }

        if (api.blobRepoExists(callingContext, fileRepo))
            api.deleteBlobRepo(callingContext, fileRepo);
        if (api.blobRepoExists(callingContext, memRepo))
            api.deleteBlobRepo(callingContext, memRepo);
    }

    @Test
    public void testDeleteBlobsByUriPrefix() {
        testCreateAndGetRepo();

        String uriPrefix = blobAuthorityURI + "uriFragment/";
        blobImpl.putBlob(callingContext, uriPrefix + "blob1", SAMPLE_BLOB, "application/text");
        blobImpl.putBlob(callingContext, uriPrefix + "blob2", SAMPLE_BLOB, "application/text");
        blobImpl.putBlob(callingContext, uriPrefix + "folder1/blob3", SAMPLE_BLOB, "application/text");
        blobImpl.putBlob(callingContext, uriPrefix + "folder1/blob4", SAMPLE_BLOB, "application/text");
        blobImpl.putBlob(callingContext, uriPrefix + "folder1/folder2/blob5", SAMPLE_BLOB, "application/text");
        blobImpl.putBlob(callingContext, uriPrefix + "folder1/folder2/blob6", SAMPLE_BLOB, "application/text");

        Map<String, RaptureFolderInfo> resultsMap;
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext, uriPrefix, -1);
        assertEquals(8, resultsMap.size());

        List<String> removed;
        removed = blobImpl.deleteBlobsByUriPrefix(callingContext, uriPrefix+"folder1/folder2/");
        assertEquals(2, removed.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext, uriPrefix, -1);
        assertEquals(5, resultsMap.size());
        removed = blobImpl.deleteBlobsByUriPrefix(callingContext, uriPrefix);
        assertEquals(5, removed.size());
        resultsMap = blobImpl.listBlobsByUriPrefix(callingContext, uriPrefix, -1);
        assertEquals(0, resultsMap.size());
    }

}
