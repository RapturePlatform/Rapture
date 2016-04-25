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
package rapture.api.checkout;

import static org.testng.AssertJUnit.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.common.net.MediaType;

import rapture.common.CallingContext;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureScript;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.BlobApi;
import rapture.common.api.DocApi;
import rapture.common.api.EventApi;
import rapture.common.api.IdGenApi;
import rapture.common.api.ScriptApi;
import rapture.common.api.SeriesApi;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.Login;

/**
 * Tests to exercise the Mongo repo to check for breakages in migrating to Mongo
 * 3.0
 */

public class ConsistencyTest {

    private Login raptureLogin = null;
    private SeriesApi seriesApi = null;
    private DocApi docApi = null;
    private ScriptApi scriptApi = null;
    private EventApi eventApi = null;
    private IdGenApi fountainApi = null;
    private BlobApi blobApi = null;
    CallingContext callingContext = null;

    /**
     * Setup TestNG method to create Rapture login object and objects.
     *
     * @param RaptureURL
     *            Passed in from <env>_testng.xml suite file
     * @param RaptureUser
     *            Passed in from <env>_testng.xml suite file
     * @param RapturePassword
     *            Passed in from <env>_testng.xml suite file
     * @return none
     */
    @BeforeMethod
    @BeforeClass(groups = { "mongo" })
    @Parameters({ "RaptureURL", "RaptureUser", "RapturePassword" })
    public void setUp(@Optional("http://localhost:8665/rapture") String url, @Optional("rapture") String username, @Optional("rapture") String password) {

        // If running from eclipse set env var -Penv=docker or use the following
        // url variable settings:
        // url="http://192.168.99.101:8665/rapture"; //docker
        // url="http://localhost:8665/rapture";
        
        
//        System.out.println("Using url " + url);
//        raptureLogin = new HttpLoginApi(url, new SimpleCredentialsProvider(username, password));
//        raptureLogin.login();
//        seriesApi = new HttpSeriesApi(raptureLogin);
//        docApi = new HttpDocApi(raptureLogin);
//        scriptApi = new HttpScriptApi(raptureLogin);
//        eventApi = new HttpEventApi(raptureLogin);
//        fountainApi = new HttpIdGenApi(raptureLogin);
//        blobApi = new HttpBlobApi(raptureLogin);
//        callingContext = raptureLogin.getContext();
//        
        
    }

    /**
     * TestNG method to cleanup.
     *
     * @param none
     * @return none
     */
    @AfterClass(groups = { "mongo" })
    public void afterTest() {
        raptureLogin = null;
    }


    boolean compare(StringBuilder sb, List<String> l1, List<String> l2) {
        boolean ret = true;
        
        // Assume not null. Assume at least one has values.
        
        Collections.sort(l1);
        Collections.sort(l2);
        String any = (!l1.isEmpty()) ? l1.get(0) : l2.get(0);
        int skipColon = any.indexOf(':');
        int skipAuth = l1.get(0).indexOf('/', skipColon+3);

        if (l1.size() == l2.size()) {
            sb.append("Sizes are equal\n");
            for (int index = 0; index < l1.size(); index++) {
                String s1 = l1.get(index);
                String s2 = l2.get(index);
                if (!s1.substring(skipAuth).equals(s2.substring(skipAuth))) {
                    sb.append("At index "+index+" ").append(s1).append(" != ").append(s2).append("\n");
                    ret = false;
                }
            }
        } else {
            ret = false;
            sb.append("First has length ").append(l1.size()).append("\n");
            for (int index = 0; index < l1.size(); index++) {
                sb.append("At index "+index+" ").append(l1.get(index)).append("\n");
            }
            sb.append("\nSecond has length ").append(l2.size()).append("\n");
            for (int index = 0; index < l2.size(); index++) {
                sb.append("At index "+index+" ").append(l2.get(index)).append("\n");
            }
        }
        return ret;
    }
    
    String content = "{\"Nothing\":\"much\"}";
    String FOOBAR = "foo/bar";
    String FOOBARBAZ = FOOBAR+"/baz";
    String WIBBLE = "/wibble";

    void createBlobs(String path) {
        blobApi.putBlob(callingContext, path, content.getBytes(), MediaType.ANY_TEXT_TYPE.toString());
        assertTrue(blobApi.blobExists(callingContext, path));
        blobApi.putBlob(callingContext, path+WIBBLE, content.getBytes(), MediaType.ANY_TEXT_TYPE.toString());
        assertTrue(blobApi.blobExists(callingContext, path+WIBBLE));
    }
    
    void createDocs(String path) {
        docApi.putDoc(callingContext, path, content);
        assertTrue(docApi.docExists(callingContext, path));
        docApi.putDoc(callingContext, path+WIBBLE, content);
        assertTrue(docApi.docExists(callingContext, path+WIBBLE));
    }
    
    void createSeries(String path) {
        seriesApi.addStringToSeries(callingContext, path, "Sierra", "Nevada");
        assertEquals("'Nevada", seriesApi.getLastPoint(callingContext, path).getValue());
        seriesApi.addStringToSeries(callingContext, path+WIBBLE, "Anchor", "Steam");
        assertEquals("'Steam", seriesApi.getLastPoint(callingContext, path+WIBBLE).getValue());
    }
    
    void createScripts(String path) {
    	RaptureURI uri = new RaptureURI(path);
    	putScript(uri.getAuthority(), uri.getDocPath());
    	putScript(uri.getAuthority(), uri.getDocPath()+WIBBLE);
    }
    
    void putScript(String auth, String name) {
        RaptureScript script = new RaptureScript();
        script.setAuthority(auth);
        script.setLanguage(RaptureScriptLanguage.REFLEX);
        script.setName(name);
        script.setPurpose(RaptureScriptPurpose.PROGRAM);
        script.setParameters(Collections.emptyList());
        String scriptWrite = "// do nothing";
        script.setScript(scriptWrite);
        scriptApi.putScript(callingContext, script.getAddressURI().toString(), script);
        RaptureScript scriptRead = scriptApi.getScript(callingContext, script.getAddressURI().toString());
        assertEquals(scriptWrite, scriptRead.getScript());
    }

    @Test
    public void testBlobConsistency() {
        Kernel.initBootstrap();
        String uuid = UUID.randomUUID().toString();
        
        // Use local not remote - remove this to connect to API server
        //
        blobApi = Kernel.getBlob();   
        callingContext = ContextFactory.getKernelUser();
        //
        // Use local not remote - remove this to connect to API server

        List<String> removeMong;
        List<String> removeFile;
        List<String> removeMem;
        
        Map<String, RaptureFolderInfo> listMong;
        Map<String, RaptureFolderInfo> listFile;
        Map<String, RaptureFolderInfo> listMem;

        // /foo and /foo/bar are FOLDERS
        // /foo/bar/baz is BOTH a FOLDER and a NODE
        // /foo/bar/baz/wibble is a NODE
        
        {
            String authFile = RaptureURI.builder(Scheme.BLOB, uuid+"-file").asString();
            String authMongo = RaptureURI.builder(Scheme.BLOB, uuid+"-mongo").asString();
            String authMem = RaptureURI.builder(Scheme.BLOB, uuid+"-memory").asString();
            
            blobApi.createBlobRepo(callingContext, authMongo, "BLOB {} USING MONGODB {prefix=\"" + uuid + "\"}", "NREP {} USING MONGODB {prefix=\"Meta" + uuid + "\"}");
            blobApi.createBlobRepo(callingContext, authFile, "BLOB {} USING FILE {prefix=\"" + uuid + "\"}", "NREP {} USING FILE {prefix=\"Meta" + uuid + "\"}");
            blobApi.createBlobRepo(callingContext, authMem, "BLOB {} USING MEMORY {prefix=\"" + uuid + "\"}", "NREP {} USING MEMORY {prefix=\"Meta" + uuid + "\"}");
            
            assertNotNull(blobApi.getBlobRepoConfig(callingContext, authMongo));
            assertNotNull(blobApi.getBlobRepoConfig(callingContext, authMem));
            assertNotNull(blobApi.getBlobRepoConfig(callingContext, authFile));
                        
            createBlobs(authMongo+FOOBARBAZ);
            createBlobs(authFile+FOOBARBAZ);
            createBlobs(authMem+FOOBARBAZ);
            
            assertTrue(blobApi.blobExists(callingContext, authMongo+FOOBARBAZ));
            assertTrue(blobApi.blobExists(callingContext, authFile+FOOBARBAZ));
            assertTrue(blobApi.blobExists(callingContext, authMem+FOOBARBAZ));
            
            assertTrue(blobApi.blobExists(callingContext, authMongo+FOOBARBAZ+WIBBLE));
            assertTrue(blobApi.blobExists(callingContext, authFile+FOOBARBAZ+WIBBLE));
            assertTrue(blobApi.blobExists(callingContext, authMem+FOOBARBAZ+WIBBLE));

            // only returns deleted blobs?
            removeMong = blobApi.deleteBlobsByUriPrefix(callingContext, authMongo+FOOBARBAZ);
            removeFile = blobApi.deleteBlobsByUriPrefix(callingContext, authFile+FOOBARBAZ);
            removeMem = blobApi.deleteBlobsByUriPrefix(callingContext, authMem+FOOBARBAZ);
            
            // The node FOOBARBAZ/WIBBLE should have been deleted 
            // (along with the empty folder FOOBARBAZ if recursion applies)
            // but the node FOOBARBAZ should still be intact.
            
            assertEquals(JacksonUtil.jsonFromObject(removeMong), 1, removeMong.size());
            assertEquals(JacksonUtil.jsonFromObject(removeFile), 1, removeFile.size());
            assertEquals(JacksonUtil.jsonFromObject(removeMem), 1, removeMem.size());
            
            assertTrue(blobApi.blobExists(callingContext, authFile+FOOBARBAZ));
            assertTrue(blobApi.blobExists(callingContext, authMem+FOOBARBAZ));
            assertTrue(blobApi.blobExists(callingContext, authMongo+FOOBARBAZ));
            
            listMem = blobApi.listBlobsByUriPrefix(callingContext, authMem+FOOBAR, 2);
            listFile = blobApi.listBlobsByUriPrefix(callingContext, authFile+FOOBAR, 2);
            listMong = blobApi.listBlobsByUriPrefix(callingContext, authMongo+FOOBAR, 2);

            assertEquals(JacksonUtil.jsonFromObject(listMong), 1, listMong.size());
            assertEquals(JacksonUtil.jsonFromObject(listMem), 1, listMem.size());
            assertEquals(JacksonUtil.jsonFromObject(listFile), 1, listFile.size());
            
            removeMong = blobApi.deleteBlobsByUriPrefix(callingContext, authMongo+FOOBAR);
            removeFile = blobApi.deleteBlobsByUriPrefix(callingContext, authFile+FOOBAR);
            removeMem = blobApi.deleteBlobsByUriPrefix(callingContext, authMem+FOOBAR);
            
            assertEquals(JacksonUtil.jsonFromObject(removeMong), 1, removeMong.size());
            assertEquals(JacksonUtil.jsonFromObject(removeFile), 1, removeFile.size());
            assertEquals(JacksonUtil.jsonFromObject(removeMem), 1, removeMem.size());
            
            assertEquals(authMongo+FOOBARBAZ, removeMong.get(0));
            assertEquals(authFile+FOOBARBAZ, removeFile.get(0));
            assertEquals(authMem+FOOBARBAZ, removeMem.get(0));
        }
    }
    
    
    @Test
    public void testDocsConsistency_NREP() {
        Kernel.initBootstrap();
        String uuid = UUID.randomUUID().toString();
        
        // Use local not remote - remove this to connect to API server
        //
        docApi = Kernel.getDoc();   
        callingContext = ContextFactory.getKernelUser();
        //
        // Use local not remote - remove this to connect to API server

        List<String> removeMong;
        List<String> removeFile;
        List<String> removeMem;
        
        Map<String, RaptureFolderInfo> listMong;
        Map<String, RaptureFolderInfo> listFile;
        Map<String, RaptureFolderInfo> listMem;

        // /foo and /foo/bar are FOLDERS
        // /foo/bar/baz is BOTH a FOLDER and a NODE
        // /foo/bar/baz/wibble is a NODE
        
        {
            String authFile = RaptureURI.builder(Scheme.DOCUMENT, uuid+"-file").asString();
            String authMongo = RaptureURI.builder(Scheme.DOCUMENT, uuid+"-mongo").asString();
            String authMem = RaptureURI.builder(Scheme.DOCUMENT, uuid+"-memory").asString();
            
            docApi.createDocRepo(callingContext, authMongo, "NREP {} USING MONGODB {prefix=\"" + uuid + "\"}");
            docApi.createDocRepo(callingContext, authFile, "NREP {} USING FILE {prefix=\"" + uuid + "\"}");
            docApi.createDocRepo(callingContext, authMem, "NREP {} USING MEMORY {prefix=\"" + uuid + "\"}");
            
            assertNotNull(docApi.getDocRepoConfig(callingContext, authMongo));
            assertNotNull(docApi.getDocRepoConfig(callingContext, authMem));
            assertNotNull(docApi.getDocRepoConfig(callingContext, authFile));
                        
            createDocs(authMongo+FOOBARBAZ);
            createDocs(authFile+FOOBARBAZ);
            createDocs(authMem+FOOBARBAZ);
            
            assertTrue(docApi.docExists(callingContext, authMongo+FOOBARBAZ));
            assertTrue(docApi.docExists(callingContext, authFile+FOOBARBAZ));
            assertTrue(docApi.docExists(callingContext, authMem+FOOBARBAZ));
            
            assertTrue(docApi.docExists(callingContext, authMongo+FOOBARBAZ+WIBBLE));
            assertTrue(docApi.docExists(callingContext, authFile+FOOBARBAZ+WIBBLE));
            assertTrue(docApi.docExists(callingContext, authMem+FOOBARBAZ+WIBBLE));

            // only returns deleted Docs?
            removeMong = docApi.deleteDocsByUriPrefix(callingContext, authMongo+FOOBARBAZ);
            removeFile = docApi.deleteDocsByUriPrefix(callingContext, authFile+FOOBARBAZ);
            removeMem = docApi.deleteDocsByUriPrefix(callingContext, authMem+FOOBARBAZ);
            
            // The node FOOBARBAZ/WIBBLE should have been deleted 
            // (along with the empty folder FOOBARBAZ if recursion applies)
            // but the node FOOBARBAZ should still be intact.
            
            assertEquals(JacksonUtil.jsonFromObject(removeMong), 1, removeMong.size());
            assertEquals(JacksonUtil.jsonFromObject(removeFile), 1, removeFile.size());
            assertEquals(JacksonUtil.jsonFromObject(removeMem), 1, removeMem.size());
            
            assertTrue(docApi.docExists(callingContext, authFile+FOOBARBAZ));
            assertTrue(docApi.docExists(callingContext, authMem+FOOBARBAZ));
            assertTrue(docApi.docExists(callingContext, authMongo+FOOBARBAZ));
            
            listMem = docApi.listDocsByUriPrefix(callingContext, authMem+FOOBAR, 2);
            listFile = docApi.listDocsByUriPrefix(callingContext, authFile+FOOBAR, 2);
            listMong = docApi.listDocsByUriPrefix(callingContext, authMongo+FOOBAR, 2);

            assertEquals(JacksonUtil.jsonFromObject(listMong), 1, listMong.size());
            assertEquals(JacksonUtil.jsonFromObject(listMem), 1, listMem.size());
            assertEquals(JacksonUtil.jsonFromObject(listFile), 1, listFile.size());
            
            removeMong = docApi.deleteDocsByUriPrefix(callingContext, authMongo+FOOBAR);
            removeFile = docApi.deleteDocsByUriPrefix(callingContext, authFile+FOOBAR);
            removeMem = docApi.deleteDocsByUriPrefix(callingContext, authMem+FOOBAR);
            
            assertEquals(JacksonUtil.jsonFromObject(removeMong), 1, removeMong.size());
            assertEquals(JacksonUtil.jsonFromObject(removeFile), 1, removeFile.size());
            assertEquals(JacksonUtil.jsonFromObject(removeMem), 1, removeMem.size());
            
            assertEquals(authMongo+FOOBARBAZ, removeMong.get(0));
            assertEquals(authFile+FOOBARBAZ, removeFile.get(0));
            assertEquals(authMem+FOOBARBAZ, removeMem.get(0));
        }
    }
    
    
    @Test
    public void testDocsConsistency_REP() {
        Kernel.initBootstrap();
        String uuid = UUID.randomUUID().toString();
        
        // Use local not remote - remove this to connect to API server
        //
        docApi = Kernel.getDoc();   
        callingContext = ContextFactory.getKernelUser();
        //
        // Use local not remote - remove this to connect to API server

        List<String> removeMong;
        List<String> removeFile;
        List<String> removeMem;
        
        Map<String, RaptureFolderInfo> listMong;
        Map<String, RaptureFolderInfo> listFile;
        Map<String, RaptureFolderInfo> listMem;

        // /foo and /foo/bar are FOLDERS
        // /foo/bar/baz is BOTH a FOLDER and a NODE
        // /foo/bar/baz/wibble is a NODE
        
        {
            String authFile = RaptureURI.builder(Scheme.DOCUMENT, uuid+"-file").asString();
            String authMongo = RaptureURI.builder(Scheme.DOCUMENT, uuid+"-mongo").asString();
            String authMem = RaptureURI.builder(Scheme.DOCUMENT, uuid+"-memory").asString();
            
            docApi.createDocRepo(callingContext, authMongo, "REP {} USING MONGODB {prefix=\"" + uuid + "\"}");
            docApi.createDocRepo(callingContext, authFile, "REP {} USING FILE {prefix=\"" + uuid + "\"}");
            docApi.createDocRepo(callingContext, authMem, "REP {} USING MEMORY {prefix=\"" + uuid + "\"}");
            
            assertNotNull(docApi.getDocRepoConfig(callingContext, authMongo));
            assertNotNull(docApi.getDocRepoConfig(callingContext, authMem));
            assertNotNull(docApi.getDocRepoConfig(callingContext, authFile));
                        
            createDocs(authMongo+FOOBARBAZ);
            createDocs(authFile+FOOBARBAZ);
            createDocs(authMem+FOOBARBAZ);
            
            assertTrue(docApi.docExists(callingContext, authMongo+FOOBARBAZ));
            assertTrue(docApi.docExists(callingContext, authFile+FOOBARBAZ));
            assertTrue(docApi.docExists(callingContext, authMem+FOOBARBAZ));
            
            assertTrue(docApi.docExists(callingContext, authMongo+FOOBARBAZ+WIBBLE));
            assertTrue(docApi.docExists(callingContext, authFile+FOOBARBAZ+WIBBLE));
            assertTrue(docApi.docExists(callingContext, authMem+FOOBARBAZ+WIBBLE));

            // only returns deleted Docs?
            removeMong = docApi.deleteDocsByUriPrefix(callingContext, authMongo+FOOBARBAZ);
            removeFile = docApi.deleteDocsByUriPrefix(callingContext, authFile+FOOBARBAZ);
            removeMem = docApi.deleteDocsByUriPrefix(callingContext, authMem+FOOBARBAZ);
            
            // The node FOOBARBAZ/WIBBLE should have been deleted 
            // (along with the empty folder FOOBARBAZ if recursion applies)
            // but the node FOOBARBAZ should still be intact.
            
            assertEquals(JacksonUtil.jsonFromObject(removeMong), 1, removeMong.size());
            assertEquals(JacksonUtil.jsonFromObject(removeFile), 1, removeFile.size());
            assertEquals(JacksonUtil.jsonFromObject(removeMem), 1, removeMem.size());
            
            assertTrue(docApi.docExists(callingContext, authFile+FOOBARBAZ));
            assertTrue(docApi.docExists(callingContext, authMem+FOOBARBAZ));
            assertTrue(docApi.docExists(callingContext, authMongo+FOOBARBAZ));
            
            assertFalse(docApi.docExists(callingContext, authFile+FOOBARBAZ+WIBBLE));
            assertFalse(docApi.docExists(callingContext, authMem+FOOBARBAZ+WIBBLE));
            assertFalse(docApi.docExists(callingContext, authMongo+FOOBARBAZ+WIBBLE));
            
            listMem = docApi.listDocsByUriPrefix(callingContext, authMem+FOOBAR, 2);
            listFile = docApi.listDocsByUriPrefix(callingContext, authFile+FOOBAR, 2);
            listMong = docApi.listDocsByUriPrefix(callingContext, authMongo+FOOBAR, 2);

            assertEquals(JacksonUtil.jsonFromObject(listMong), 1, listMong.size());
            assertEquals(JacksonUtil.jsonFromObject(listMem), 1, listMem.size());
            assertEquals(JacksonUtil.jsonFromObject(listFile), 1, listFile.size());
            
            removeMong = docApi.deleteDocsByUriPrefix(callingContext, authMongo+FOOBAR);
            removeFile = docApi.deleteDocsByUriPrefix(callingContext, authFile+FOOBAR);
            removeMem = docApi.deleteDocsByUriPrefix(callingContext, authMem+FOOBAR);
            
            assertEquals(JacksonUtil.jsonFromObject(removeMong), 1, removeMong.size());
            assertEquals(JacksonUtil.jsonFromObject(removeFile), 1, removeFile.size());
            assertEquals(JacksonUtil.jsonFromObject(removeMem), 1, removeMem.size());
            
            assertEquals(authMongo+FOOBARBAZ, removeMong.get(0));
            assertEquals(authFile+FOOBARBAZ, removeFile.get(0));
            assertEquals(authMem+FOOBARBAZ, removeMem.get(0));
        }
    }
    
    @Test
    public void testSeriesConsistency() {
        Kernel.initBootstrap();
        String uuid = UUID.randomUUID().toString();
        
        // Use local not remote - remove this to connect to API server
        //
        seriesApi = Kernel.getSeries();   
        callingContext = ContextFactory.getKernelUser();
        //
        // Use local not remote - remove this to connect to API server

        List<String> removeMong;
        List<String> removeFile;
        List<String> removeMem;
        
        Map<String, RaptureFolderInfo> listMong;
        Map<String, RaptureFolderInfo> listFile;
        Map<String, RaptureFolderInfo> listMem;

        // /foo and /foo/bar are FOLDERS
        // /foo/bar/baz is BOTH a FOLDER and a NODE
        // /foo/bar/baz/wibble is a NODE
        
        {
            String authFile = RaptureURI.builder(Scheme.SERIES, uuid+"-file").asString();
            String authMongo = RaptureURI.builder(Scheme.SERIES, uuid+"-mongo").asString();
            String authMem = RaptureURI.builder(Scheme.SERIES, uuid+"-memory").asString();
            String authCass = RaptureURI.builder(Scheme.SERIES, uuid+"-cassandra").asString();
            
            seriesApi.createSeriesRepo(callingContext, authMongo, "SREP {} USING MONGODB {prefix=\"" + uuid + "\"}");
            seriesApi.createSeriesRepo(callingContext, authFile, "SREP {} USING FILE {prefix=\"" + uuid + "\"}");
            seriesApi.createSeriesRepo(callingContext, authMem, "SREP {} USING MEMORY {prefix=\"" + uuid + "\"}");
            seriesApi.createSeriesRepo(callingContext, authCass, "SREP {} USING MEMORY {prefix=\"" + uuid + "\"}");
            
            assertNotNull(seriesApi.getSeriesRepoConfig(callingContext, authMongo));
            assertNotNull(seriesApi.getSeriesRepoConfig(callingContext, authMem));
            assertNotNull(seriesApi.getSeriesRepoConfig(callingContext, authFile));
                        
            createSeries(authMongo+FOOBARBAZ);
            createSeries(authFile+FOOBARBAZ);
            createSeries(authMem+FOOBARBAZ);
            
            assertTrue(seriesApi.seriesExists(callingContext, authMongo+FOOBARBAZ));
            assertTrue(seriesApi.seriesExists(callingContext, authFile+FOOBARBAZ));
            assertTrue(seriesApi.seriesExists(callingContext, authMem+FOOBARBAZ));
            
            assertTrue(seriesApi.seriesExists(callingContext, authMongo+FOOBARBAZ+WIBBLE));
            assertTrue(seriesApi.seriesExists(callingContext, authFile+FOOBARBAZ+WIBBLE));
            assertTrue(seriesApi.seriesExists(callingContext, authMem+FOOBARBAZ+WIBBLE));

            removeMem = seriesApi.deleteSeriesByUriPrefix(callingContext, authMem+FOOBARBAZ);
            removeFile = seriesApi.deleteSeriesByUriPrefix(callingContext, authFile+FOOBARBAZ);
            removeMong = seriesApi.deleteSeriesByUriPrefix(callingContext, authMongo+FOOBARBAZ);
            
            // The node FOOBARBAZ/WIBBLE should have been deleted 
            // (along with the empty folder FOOBARBAZ if recursion applies)
            // but the node FOOBARBAZ should still be intact.
            
            assertEquals(JacksonUtil.jsonFromObject(removeMong), 1, removeMong.size());
            assertEquals(JacksonUtil.jsonFromObject(removeFile), 1, removeFile.size());
            assertEquals(JacksonUtil.jsonFromObject(removeMem), 1, removeMem.size());
            
            assertTrue(seriesApi.seriesExists(callingContext, authFile+FOOBARBAZ));
            assertTrue(seriesApi.seriesExists(callingContext, authMem+FOOBARBAZ));
            assertTrue(seriesApi.seriesExists(callingContext, authMongo+FOOBARBAZ));
            
            assertFalse(seriesApi.seriesExists(callingContext, authFile+FOOBARBAZ+WIBBLE));
            assertFalse(seriesApi.seriesExists(callingContext, authMem+FOOBARBAZ+WIBBLE));
            assertFalse(seriesApi.seriesExists(callingContext, authMongo+FOOBARBAZ+WIBBLE));
            
            listMem = seriesApi.listSeriesByUriPrefix(callingContext, authMem+FOOBAR, 2);
            listFile = seriesApi.listSeriesByUriPrefix(callingContext, authFile+FOOBAR, 2);
            listMong = seriesApi.listSeriesByUriPrefix(callingContext, authMongo+FOOBAR, 2);

            assertEquals(JacksonUtil.jsonFromObject(listMong), 1, listMong.size());
            assertEquals(JacksonUtil.jsonFromObject(listMem), 1, listMem.size());
            assertEquals(JacksonUtil.jsonFromObject(listFile), 1, listFile.size());
            
            removeMong = seriesApi.deleteSeriesByUriPrefix(callingContext, authMongo+FOOBAR);
            removeFile = seriesApi.deleteSeriesByUriPrefix(callingContext, authFile+FOOBAR);
            removeMem = seriesApi.deleteSeriesByUriPrefix(callingContext, authMem+FOOBAR);
            
            assertEquals(JacksonUtil.jsonFromObject(removeMong), 1, removeMong.size());
            assertEquals(JacksonUtil.jsonFromObject(removeFile), 1, removeFile.size());
            assertEquals(JacksonUtil.jsonFromObject(removeMem), 1, removeMem.size());
            
            assertEquals(authMongo+FOOBARBAZ, removeMong.get(0));
            assertEquals(authFile+FOOBARBAZ, removeFile.get(0));
            assertEquals(authMem+FOOBARBAZ, removeMem.get(0));
        }
    }
    
    @Test
    public void testScriptsConsistency() {
        Kernel.initBootstrap();
        String uuid = UUID.randomUUID().toString();
        
        // Use local not remote - remove this to connect to API server
        //
        scriptApi = Kernel.getScript();   
        callingContext = ContextFactory.getKernelUser();
        //
        // Use local not remote - remove this to connect to API server

        List<String> removeMong;
        List<String> removeFile;
        List<String> removeMem;
        
        Map<String, RaptureFolderInfo> listMong;
        Map<String, RaptureFolderInfo> listFile;
        Map<String, RaptureFolderInfo> listMem;

        // /foo and /foo/bar are FOLDERS
        // /foo/bar/baz is BOTH a FOLDER and a NODE
        // /foo/bar/baz/wibble is a NODE
        
        {
            String authFile = RaptureURI.builder(Scheme.SCRIPT, uuid+"-file").asString();
            String authMongo = RaptureURI.builder(Scheme.SCRIPT, uuid+"-mongo").asString();
            String authMem = RaptureURI.builder(Scheme.SCRIPT, uuid+"-memory").asString();
            
            // Repo already exists - no need to create
            
            createScripts(authMongo+FOOBARBAZ);
            createScripts(authFile+FOOBARBAZ);
            createScripts(authMem+FOOBARBAZ);
            
            assertTrue(scriptApi.doesScriptExist(callingContext, authMongo+FOOBARBAZ));
            assertTrue(scriptApi.doesScriptExist(callingContext, authFile+FOOBARBAZ));
            assertTrue(scriptApi.doesScriptExist(callingContext, authMem+FOOBARBAZ));
            
            assertTrue(scriptApi.doesScriptExist(callingContext, authMongo+FOOBARBAZ+WIBBLE));
            assertTrue(scriptApi.doesScriptExist(callingContext, authFile+FOOBARBAZ+WIBBLE));
            assertTrue(scriptApi.doesScriptExist(callingContext, authMem+FOOBARBAZ+WIBBLE));

            removeMong = scriptApi.deleteScriptsByUriPrefix(callingContext, authMongo+FOOBARBAZ);
            removeFile = scriptApi.deleteScriptsByUriPrefix(callingContext, authFile+FOOBARBAZ);
            removeMem = scriptApi.deleteScriptsByUriPrefix(callingContext, authMem+FOOBARBAZ);
            
            // The node FOOBARBAZ/WIBBLE should have been deleted 
            // (along with the empty folder FOOBARBAZ if recursion applies)
            // but the node FOOBARBAZ should still be intact.
            
            assertEquals(JacksonUtil.jsonFromObject(removeMong), 1, removeMong.size());
            assertEquals(JacksonUtil.jsonFromObject(removeFile), 1, removeFile.size());
            assertEquals(JacksonUtil.jsonFromObject(removeMem), 1, removeMem.size());
            
            assertTrue(scriptApi.doesScriptExist(callingContext, authFile+FOOBARBAZ));
            assertTrue(scriptApi.doesScriptExist(callingContext, authMem+FOOBARBAZ));
            assertTrue(scriptApi.doesScriptExist(callingContext, authMongo+FOOBARBAZ));
            
            listMem = scriptApi.listScriptsByUriPrefix(callingContext, authMem+FOOBAR, 2);
            listFile = scriptApi.listScriptsByUriPrefix(callingContext, authFile+FOOBAR, 2);
            listMong = scriptApi.listScriptsByUriPrefix(callingContext, authMongo+FOOBAR, 2);

            assertEquals(JacksonUtil.jsonFromObject(listMong), 1, listMong.size());
            assertEquals(JacksonUtil.jsonFromObject(listMem), 1, listMem.size());
            assertEquals(JacksonUtil.jsonFromObject(listFile), 1, listFile.size());
            
            removeMong = scriptApi.deleteScriptsByUriPrefix(callingContext, authMongo+FOOBAR);
            removeFile = scriptApi.deleteScriptsByUriPrefix(callingContext, authFile+FOOBAR);
            removeMem = scriptApi.deleteScriptsByUriPrefix(callingContext, authMem+FOOBAR);
            
            assertEquals(JacksonUtil.jsonFromObject(removeMong), 1, removeMong.size());
            assertEquals(JacksonUtil.jsonFromObject(removeFile), 1, removeFile.size());
            assertEquals(JacksonUtil.jsonFromObject(removeMem), 1, removeMem.size());
            
            assertEquals(authMongo+FOOBARBAZ, removeMong.get(0));
            assertEquals(authFile+FOOBARBAZ, removeFile.get(0));
            assertEquals(authMem+FOOBARBAZ, removeMem.get(0));
        }
    }


//        {
//            SeriesApi impl = Kernel.getSeries();
//            String auth = Scheme.SERIES.toString() + "://" + uuid;
//            impl.createSeriesRepo(callingContext, auth, "SREP {} USING MONGODB {prefix=\"" + uuid + "\"}");
//            assertNotNull(impl.getSeriesRepoConfig(callingContext, auth));
//            List<String> deleted = impl.deleteSeriesByUriPrefix(callingContext, auth + "/spurious", false);
//            assertNotNull(deleted);
//            assertTrue(deleted.isEmpty());
//        }
//
//        {
//            ScriptApi impl = Kernel.getScript();
//            String auth = Scheme.SCRIPT.toString() + "://" + uuid;
//            List<String> deleted = impl.deleteScriptsByUriPrefix(callingContext, auth + "/spurious", false);
//            assertNotNull(deleted);
//            assertTrue(deleted.isEmpty());
//        }
//        
//        {
//            EventApi impl = Kernel.getEvent();
//            String auth = Scheme.EVENT.toString() + "://" + uuid;
//            impl.addEventMessage(callingContext, auth+"/derek", "Jeff", "Brian", new HashMap<>());
//            List<RaptureFolderInfo> rfi = impl.listEventsByUriPrefix(callingContext, auth);
//            
//            Assert.assertNotNull(rfi);
//            Assert.assertFalse(rfi.isEmpty());
//            Assert.assertEquals(rfi.get(0).getName(), "derek");
//            
//            // Should we delete events?
//        }
//        
//        // Fields don't work. See RAP-4050
//        
//        {
//            JarApi impl = Kernel.getJar();
//            String auth = Scheme.JAR.toString() + "://" + uuid;
//            try {
//                impl.putJar(callingContext, auth+"/marmalade", "Marmalade".getBytes());
//                Assert.assertNotNull(impl.getJar(callingContext, auth+"/marmalade"));
//                impl.deleteJar(callingContext, auth + "/spurious");
//                Assert.fail("Exception expected");
//            } catch (Exception e) {
//                assertEquals("Blob or folder " + auth + "/spurious does not exist", e.getMessage());
//            }
//        }
}
