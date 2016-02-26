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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureFolderInfo;
import rapture.common.annotations.SampleCode;
import rapture.common.api.DocApi;
import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.SimpleCredentialsProvider;
import rapture.common.exception.RaptureException;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentRepoConfig;
import rapture.common.model.DocumentWithMeta;
import rapture.common.version.ApiVersion;
import rapture.dsl.dparse.AsOfTimeDirectiveParser;
import rapture.kernel.schemes.RepoSchemeContractTest;

public class DocApiImplTest extends RepoSchemeContractTest {

    @Override
    public Object getDocument() {
        return "{\"content\":\"This is my content, here it roar\"}";
    }

    @Override
    public String getDocumentURI() {
        return "document://" + AUTHORITY + "/document";
    }

    @Override
    public String getBookmarkedDocumentURI() {
        return null;
    }

    @Override
    public Object getBookmarkedDocument() {
        return null;
    }

    private static final String GET_ALL_BASE = "document://getAll";
    DocApi api;

    @SampleCode(api = "doc")
    @Test
    public void testGetDoc() {
        CallingContext ctx = ContextFactory.getKernelUser();
        String docUri = "document://myrepo/mydoc";

        // setup our json content
        String jsonContent = "{ \"key\" : \"value\" }";

        // put a document first
        assertEquals(docUri, api.putDoc(ctx, docUri, jsonContent));
        // now get the same document out
        assertEquals(jsonContent, api.getDoc(ctx, docUri));

        // attempt to get a document that does not exist, should return null
        assertNull(api.getDoc(ctx, "document://unknown/doesntexist"));
    }

    private String setUpVersionedRepo(String authority) {
        String repoUri = "document://" + authority;
        String config = "NREP {} USING MEMORY {prefix=\"" + authority + "\"}";
        api.createDocRepo(ContextFactory.getKernelUser(), repoUri, config);

        return repoUri;
    }

    private long[] putVersionsOfDocInRepo(String docUri, int numVersions, int microsecondsBetween) throws InterruptedException {
        long[] versionTimes = new long[numVersions];
        for (int i = 0; i < numVersions; i++) {
            String jsonContent = "{ \"version\" : \"" + (i + 1) + "\" }";
            api.putDoc(ContextFactory.getKernelUser(), docUri, jsonContent);
            DocumentMetadata meta = api.getDocMeta(ContextFactory.getKernelUser(), docUri);
            versionTimes[i] = meta.getModifiedTimestamp();
            Thread.sleep(microsecondsBetween);
        }
        return versionTimes;
    }

    @Test
    public void testListNonexistentAuthority() {
        CallingContext ctx = ContextFactory.getKernelUser();
        String authority = "document://For_Testing_"+System.currentTimeMillis();
        try {
            api.listDocsByUriPrefix(ctx, authority, 0);
            fail("Exception expected");
        } catch (RaptureException re) {
            assertEquals("Repository "+authority+" does not exist", re.getMessage());
        }
    }
    
    @Test
    public void testNonExistentFolderListDocsByUriPrefix() {
        CallingContext ctx = ContextFactory.getKernelUser();
        String docUri = "document://test."+System.currentTimeMillis();
        
        for (DocumentRepoConfig drc : api.getDocRepoConfigs(ctx)) {
            api.deleteDocRepo(ctx, drc.getAddressURI().toString());
        }
        api.createDocRepo(ctx, docUri, "REP {} USING MEMORY {}");
        String uri = docUri+"/testFolder/fish/face";
        try{
            api.listDocsByUriPrefix(ctx, uri, -1);
            fail("Exception expected");
        } catch (RaptureException re) {
            assertEquals("Document or folder " +uri+ " does not exist", re.getMessage());
        }
        
        
    }

    @Test
    public void testGetDocWithVersion() throws InterruptedException {
        CallingContext ctx = ContextFactory.getKernelUser();
        String repoUri = setUpVersionedRepo("versionedRepo");
        String docUri = repoUri + "/mydoc";
        int numVersions = 3;
        int microsecondsBetween = 2;

        putVersionsOfDocInRepo(docUri, numVersions, microsecondsBetween);

        for (int i = 0; i < numVersions; i++) {
            String versionedDocUri = docUri + "@" + (i + 1);
            DocumentWithMeta documentWithMeta = api.getDocAndMeta(ctx, versionedDocUri);
            String docContent = documentWithMeta.getContent();
            DocumentMetadata docMeta = documentWithMeta.getMetaData();

            assertTrue("Doc content should contain expected data for version.", docContent.matches(".*" + (i + 1) + ".*"));
            assertEquals("Meta data should reflect expected version.", i + 1, (long) docMeta.getVersion());

            assertEquals("Doc content should be the same from getDocAndMeta and getDoc.", docContent, api.getDoc(ctx, versionedDocUri));
            assertEquals("Meta data should be the same from getDocAndMeta and getDocMeta.", docMeta, api.getDocMeta(ctx, versionedDocUri));
        }

        api.deleteDocRepo(ctx, repoUri);
    }

    @Test
    public void testGetDocWithAsOfTime() throws InterruptedException {
        CallingContext ctx = ContextFactory.getKernelUser();
        String repoUri = setUpVersionedRepo("versionedRepo");
        String docUri = repoUri + "/mydoc";
        int numVersions = 3;

        int microsecondsBetween = 2;

        long[] versionTimes = putVersionsOfDocInRepo(docUri, numVersions, microsecondsBetween);
        for (int i = 0; i < numVersions; i++) {
            doTestAllAsOfTimeURIVariants(docUri, versionTimes[i], i + 1, "Exact time version was submitted should return that version");
        }

        doTestAllAsOfTimeURIVariants(docUri, versionTimes[numVersions - 1] + 1, numVersions,
                "With AsOfTime later than last version, get last version");
        doTestAllAsOfTimeURIVariants(docUri, versionTimes[numVersions - 1] - 1, numVersions - 1,
                "With AsOfTime between two versions, get earlier version");

        try {
            doTestAllAsOfTimeURIVariants(docUri, versionTimes[0] - 1, 0, "");
            fail("Should have gotten exception trying to get version from before doc existed.");
        } catch (RaptureException e) {
        }

        api.archiveRepoDocs(ctx, docUri, 2, versionTimes[2], true);

        try {
            doTestAllAsOfTimeURIVariants(docUri, versionTimes[0] + 1, 0, "");
            fail("Should have gotten exception trying to get version that has been deleted.");
        } catch (RaptureException e) {
        }

        api.deleteDocRepo(ctx, repoUri);
    }

    protected void doTestAllAsOfTimeURIVariants(String docUri, long milliseconds, int expectedVersion, String testMessage) {
        // Test all three millisecond variants of an AsOfTime url for a given point in time.
        // (Testing variants with whole second precision is a bit problematic here, let's leave that to other tests.)
        SimpleDateFormat dateFormatWithTZ = new SimpleDateFormat(AsOfTimeDirectiveParser.AS_OF_TIME_FORMAT_MS_TZ);
        SimpleDateFormat dateFormatWithoutTZ = new SimpleDateFormat(AsOfTimeDirectiveParser.AS_OF_TIME_FORMAT_MS);

        Date versionDate = new Date(milliseconds);
        String datetimeUri = docUri + "@" + dateFormatWithoutTZ.format(versionDate);
        String datetimeUriTZ = docUri + "@" + dateFormatWithTZ.format(versionDate);
        String timestampUri = docUri + "@t" + milliseconds;

        assertEquals(testMessage, expectedVersion, (long) api.getDocMeta(ContextFactory.getKernelUser(), datetimeUri).getVersion());
        assertEquals(testMessage, expectedVersion, (long) api.getDocMeta(ContextFactory.getKernelUser(), datetimeUriTZ).getVersion());
        assertEquals(testMessage, expectedVersion, (long) api.getDocMeta(ContextFactory.getKernelUser(), timestampUri).getVersion());
    }

    @Test
    public void testIllegalRepoPaths() {
        String repo = "document://";
        String docPath = repo + "x/x";
        try {
            api.createDocRepo(ContextFactory.getKernelUser(), repo, "REP {} using MEMORY {}");
            fail("Cannot create a repository without an authority");
        } catch (RaptureException e) {
            assertEquals("Cannot create a repository without an authority", e.getMessage());
        }
        try {
            api.createDocRepo(ContextFactory.getKernelUser(), "", "REP {} using MEMORY {}");
            fail("URI cannot be null or empty");
        } catch (RaptureException e) {
            assertEquals("Argument Repository URI cannot be null or empty", e.getMessage());
        }
        try {
            api.createDocRepo(ContextFactory.getKernelUser(), null, "REP {} using MEMORY {}");
            fail("URI cannot be null or empty");
        } catch (RaptureException e) {
            assertEquals("Argument Repository URI cannot be null or empty", e.getMessage());
        }
        try {
            api.createDocRepo(ContextFactory.getKernelUser(), docPath, "REP {} using MEMORY {}");
            fail("Document repository Uri can't have a doc path component");
        } catch (RaptureException e) {
            assertEquals("A Repository URI may not have a document path component", e.getMessage());
        }
    }

    @Test
    public void testPutContentWithNoRepo() {
        String randomDoc = "document://" + UUID.randomUUID().toString() + "/JUNK/ForTestingOnly";
        String randomJson = "{\"Foo\" : \"Bar\"}";

        api.putDoc(ContextFactory.getKernelUser(), randomDoc, randomJson);
        assertEquals(randomJson, api.getDoc(ContextFactory.getKernelUser(), randomDoc));
    }

    HttpLoginApi loginApi;
    HttpDocApi httpDocApi;

    @Ignore
    // or move to Integration tests
    @Test
    public void testPutDocumentWithNoRepoOverHttp() { // Generate some test data

        ApiVersion clientApiVersion = new ApiVersion();
        clientApiVersion.setMajor(1);
        clientApiVersion.setMinor(9);

        loginApi = new HttpLoginApi("http://localhost:8665/rapture", new SimpleCredentialsProvider("raptureApi", "raptivating"));
        loginApi.login();

        httpDocApi = new HttpDocApi(loginApi);
        String randomDoc = "document://" + UUID.randomUUID().toString() + "/JUNK/ForTestingOnly";
        String randomJson = "{\"Foo\" : \"Bar\"}";

        httpDocApi.putDoc(randomDoc, randomJson);
        assertEquals(randomJson, httpDocApi.getDoc(randomDoc));
    }

    @Test
    public void testlistDocsByUriPrefix() {
        CallingContext callingContext = getCallingContext();

        for (DocumentRepoConfig drc : api.getDocRepoConfigs(callingContext)) {
            api.deleteDocRepo(callingContext, drc.getAddressURI().toString());
        }
        api.createDocRepo(callingContext, GET_ALL_BASE, "REP {} USING MEMORY {}");

        String uriPrefix = GET_ALL_BASE + "/uriFragment/";
        api.putDoc(callingContext, uriPrefix + "blob1", "{\"foo\":\"bar\"}");
        api.putDoc(callingContext, uriPrefix + "blob2", "{\"foo\":\"bar\"}");
        api.putDoc(callingContext, uriPrefix + "folder1/blob3", "{\"foo\":\"bar\"}");
        api.putDoc(callingContext, uriPrefix + "folder1/blob4", "{\"foo\":\"bar\"}");
        api.putDoc(callingContext, uriPrefix + "folder1/folder2/blob5", "{\"foo\":\"bar\"}");
        api.putDoc(callingContext, uriPrefix + "folder1/folder2/blob6", "{\"foo\":\"bar\"}");

        Map<String, RaptureFolderInfo> resultsMap;

        resultsMap = api.listDocsByUriPrefix(callingContext, uriPrefix, -1);
        assertEquals(8, resultsMap.size());
        resultsMap = api.listDocsByUriPrefix(callingContext, uriPrefix, 0);
        assertEquals(8, resultsMap.size());
        resultsMap = api.listDocsByUriPrefix(callingContext, uriPrefix, 4);
        assertEquals(8, resultsMap.size());
        resultsMap = api.listDocsByUriPrefix(callingContext, uriPrefix, 3);
        assertEquals(8, resultsMap.size());
        resultsMap = api.listDocsByUriPrefix(callingContext, uriPrefix, 2);
        assertEquals(6, resultsMap.size());
        resultsMap = api.listDocsByUriPrefix(callingContext, uriPrefix, 1);
        assertEquals(3, resultsMap.size());

        resultsMap = api.listDocsByUriPrefix(callingContext, uriPrefix + "/folder1/folder2", 1);
        assertEquals(2, resultsMap.size());
        resultsMap = api.listDocsByUriPrefix(callingContext, uriPrefix + "/folder1", 1);
        assertEquals(3, resultsMap.size());

        resultsMap = api.listDocsByUriPrefix(callingContext, uriPrefix + "/folder1", 2);
        assertEquals(5, resultsMap.size());

        resultsMap = api.listDocsByUriPrefix(callingContext, GET_ALL_BASE, -1);
        assertEquals(9, resultsMap.size());
        resultsMap = api.listDocsByUriPrefix(callingContext, GET_ALL_BASE, 0);
        assertEquals(9, resultsMap.size());
        resultsMap = api.listDocsByUriPrefix(callingContext, GET_ALL_BASE, 4);
        assertEquals(9, resultsMap.size());
        resultsMap = api.listDocsByUriPrefix(callingContext, GET_ALL_BASE, 3);
        assertEquals(7, resultsMap.size());
        resultsMap = api.listDocsByUriPrefix(callingContext, GET_ALL_BASE, 2);
        assertEquals(4, resultsMap.size());
        resultsMap = api.listDocsByUriPrefix(callingContext, GET_ALL_BASE, 1);
        assertEquals(1, resultsMap.size());
        String str = resultsMap.keySet().toArray(new String[1])[0];
        assertEquals(uriPrefix, str);

        resultsMap = api.listDocsByUriPrefix(callingContext, "document://", 1);
        assertEquals(1, resultsMap.size());
        resultsMap = api.listDocsByUriPrefix(callingContext, "document://", 2);
        assertEquals(2, resultsMap.size());
        resultsMap = api.listDocsByUriPrefix(callingContext, "document://", 3);
        assertEquals(5, resultsMap.size());
        resultsMap = api.listDocsByUriPrefix(callingContext, "document://", 4);
        assertEquals(8, resultsMap.size());
        resultsMap = api.listDocsByUriPrefix(callingContext, "document://", 5);
        assertEquals(10, resultsMap.size());
        resultsMap = api.listDocsByUriPrefix(callingContext, "document://", 6);
        assertEquals(10, resultsMap.size());
        resultsMap = api.listDocsByUriPrefix(callingContext, "document://", 0);
        assertEquals(10, resultsMap.size());
        resultsMap = api.listDocsByUriPrefix(callingContext, "document://", -1);
        assertEquals(10, resultsMap.size());

    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.kernel.schemes.RepoSchemeContractTest#before()
     */
    @Override
    public void before() {
        // TODO Auto-generated method stub
        super.before();
        api = new DocApiImpl(Kernel.INSTANCE);
    }

    // When using MEMORY storage empty folders automatically vanish.
    // But with File they have to be explicitly deleted. Problem?
    @Test
    public void whereDidMyFolderGo() {
        CallingContext callingContext = getCallingContext();

        if (api.docRepoExists(callingContext, GET_ALL_BASE)) {
            api.deleteDocRepo(callingContext, GET_ALL_BASE);
        }
        api.createDocRepo(callingContext, GET_ALL_BASE, "REP {} USING MEMORY {}");

        String parent = GET_ALL_BASE + "/parent/";
        String child = parent + "child.doc";

        api.putDoc(callingContext, child, "{\"daddy\": \"where do I come from\"}");

        assertTrue(api.docExists(callingContext, child));
        Map<String, RaptureFolderInfo> docs = api.listDocsByUriPrefix(callingContext, GET_ALL_BASE, 3);
        assertNotNull(docs.get(parent));
        api.deleteDoc(callingContext, child);
        assertFalse(api.docExists(callingContext, child));
        docs = api.listDocsByUriPrefix(callingContext, GET_ALL_BASE, 3);
        assertNull("folder is now gone because it was empty and we are using Memory", docs.get(parent));

        api.deleteDocRepo(callingContext, GET_ALL_BASE);
        api.createDocRepo(callingContext, GET_ALL_BASE, "REP {} USING FILE { prefix = \"foo\"}");

        api.putDoc(callingContext, child, "{\"daddy\": \"where do I come from\"}");

        assertTrue(api.docExists(callingContext, child));
        docs = api.listDocsByUriPrefix(callingContext, GET_ALL_BASE, 3);
        assertNotNull(docs.get(parent));
        api.deleteDoc(callingContext, child);
        assertFalse(api.docExists(callingContext, child));
        docs = api.listDocsByUriPrefix(callingContext, GET_ALL_BASE, 3);
        assertNotNull("folder is now empty but still exists because we are using File", docs.get(parent));
    }

}
