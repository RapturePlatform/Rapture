package rapture.document;

import rapture.common.client.HttpScriptApi;
import rapture.common.model.DocumentMetadata;
import rapture.helper.IntegrationTestHelper;

import static rapture.common.Scheme.DOCUMENT;
import static rapture.common.Scheme.EVENT;
import static rapture.common.Scheme.SCRIPT;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpEventApi;

public class DocumentApiTest {
    private HttpDocApi docApi=null;
    private HttpEventApi eventApi=null;
    private HttpScriptApi scriptApi=null;
    IntegrationTestHelper helper=null;
    
    @BeforeClass(groups={"document","mongo", "nightly"})
    @Parameters({"RaptureURL","RaptureUser","RapturePassword"})
    public void beforeTest(@Optional("http://localhost:8665/rapture")String url, @Optional("rapture")String user, @Optional("rapture")String password)  { 
        helper = new IntegrationTestHelper(url, user, password);

        docApi = helper.getDocApi();
        scriptApi =  helper.getScriptApi();
        eventApi =  helper.getEventApi();
 
    }
    
    @Test(groups={"document","mongo", "nightly"})
    public void testDocumentPut () {
    	RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, "MONGODB");
        String docPath= new RaptureURI.Builder(repo).docPath("doc"+System.nanoTime()).build().toString();
        String content="{\"key1\":\"value1\"}";
        Reporter.log("Creating and checking document "+docPath, true);
        docApi.putDoc(docPath, content);
        Assert.assertTrue(docApi.docExists(docPath));
        Assert.assertEquals(docApi.getDoc(docPath),content);
    }
    
    @Test(groups={"document","mongo", "nightly"})
    public void testDocumentUpdate () {
    	RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, "MONGODB");
        String docPath= new RaptureURI.Builder(repo).docPath("doc"+System.nanoTime()).build().toString();
        String content="{\"key1\":\"value1\"}";
        Reporter.log("Creating and checking document "+docPath, true);
        docApi.putDoc(docPath, content);
        
        for (int i = 0;i<5;i++) {
	        Reporter.log("Updating and checking document "+docPath, true);
	        String newContent="{\"key"+i+"\":\"value"+i+"\"}";
	        docApi.putDoc(docPath, newContent);
	        Assert.assertEquals(docApi.getDoc(docPath),newContent);
        }
    }
    
    @Test(groups={"document","mongo", "nightly"})
    public void testDocumentDelete () {
    	RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, "MONGODB");
        String docPath= new RaptureURI.Builder(repo).docPath("doc"+System.nanoTime()).build().toString();
        String content="{\"key1\":\"value1\"}";
        Reporter.log("Creating and checking document "+docPath, true);
        docApi.putDoc(docPath, content);
        Assert.assertTrue(docApi.docExists(docPath));
        Reporter.log("Deleting and checking document "+docPath, true);
        docApi.deleteDoc(docPath);
        Assert.assertFalse(docApi.docExists(docPath));
    }
    
    @Test(groups={"document","mongo", "nightly"})
    public void testDocumentWithAutoID(){
    	String idgenPrefix="TST";
    	int NUM_DOCS=5;
    	RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, "MONGODB");

        String docPath= new RaptureURI.Builder(repo).docPath("folder/#id").build().toString();
        String idgenCfg = "IDGEN { base=\"10\",length=\"5\", prefix=\""+idgenPrefix+"\"} USING MONGODB {prefix=\"testfountain."+System.nanoTime()+"\"}";
        docApi.setDocRepoIdGenConfig(repo.toString(), idgenCfg);
        
        String putData = "{\"key1\":\"#id\"}";
        
        Reporter.log("Creating " + NUM_DOCS +" documents", true);
        for (int i=1;i<NUM_DOCS;i++) {
	        String putContentUri = docApi.putDoc(docPath, putData);
	        Reporter.log("Creating document "+putContentUri, true);
	        String getContent = docApi.getDoc(putContentUri);
	        Reporter.log("Checking URI and content of document "+putContentUri, true);
	        Assert.assertTrue(putContentUri.contains(idgenPrefix+"0000"+i));
	        Assert.assertTrue(getContent.contains(idgenPrefix+"0000"+i));
        }

    }
    
    @Test(groups={"document","mongo", "nightly"})
    public void testEventFromRepoAfterPutTest() throws InterruptedException{
    	RaptureURI repo1 = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo1, "MONGODB");
        
        RaptureURI repo2 = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo2, "MONGODB");
        
        String testEventTargetDoc = RaptureURI.builder(DOCUMENT, repo2.getAuthority()).docPath("testdoc").build().toString();
        String docToWrite = "{\\\"key1\\\":\\\"val1 written by event\\\"}";
        String scriptText = "#doc.putDoc('" + testEventTargetDoc + "',\'" + docToWrite + "\');";
        Reporter.log("Creating script with text: "+scriptText,true);

        String eventName = "data/update"; //this is a special keyword/fixed uri which fires an event when data is written to repo 
        String scriptURI=helper.getRandomAuthority(SCRIPT)+"testScript"+System.nanoTime();
        
        scriptApi.createScript(scriptURI, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM,scriptText);
        
        String eventURI = RaptureURI.builder(EVENT, repo1.getAuthority()).docPath(eventName).build().toString();
        Reporter.log("Created event"+eventURI,true);
        //associate event with script to run test once only 
        eventApi.addEventScript(eventURI, scriptURI, true);
        //put content to repo and which should fire an event and write into testEventTargetDoc
        String docURI = RaptureURI.builder(DOCUMENT, repo1.getAuthority()).docPath("folder1/documenttest").build().toString();
        Reporter.log("Calling putDoc to "+docURI,true);
        docApi.putDoc(docURI, "{\"key1\":\"value1\"}");

        String s2 = docApi.getDoc(docURI);
        Assert.assertEquals(s2, "{\"key1\":\"value1\"}");
        Thread.sleep(1000);
        //check if the doc was written from the reflex script
        String s3 = docApi.getDoc(testEventTargetDoc);
        Reporter.log("Checking data: " + s3,true);
        Assert.assertEquals(s3, "{\"key1\":\"val1 written by event\"}");
        
    }
    
    @Test(groups={"document","mongo", "nightly"})
    public void testPutContentFireEventFromRepoTest() throws InterruptedException{
        //create the repo
    	RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, "MONGODB");
        
        String docURI = RaptureURI.builder(DOCUMENT, repo.getAuthority()).docPath("testdoc").build().toString();
        docApi.putDoc(docURI, "{\"key\":\"value\"}");
        //create script and attach it to an event
        String docRepoURI = RaptureURI.builder(DOCUMENT, repo.getAuthority()).build().toString();

        //create script and event uris
        String testDocUri = docRepoURI + "testDoc";
        String docVal="{\"keytest\":\"valuetest\"}";
        String scriptText = "#doc.putDoc('" + testDocUri + "'" + ",'" + docVal + "');";

        Reporter.log("Creating script with text: "+scriptText,true);
        String scriptName = "script"+System.nanoTime();
        String eventName = "data/update";
    
        String scriptURI=helper.getRandomAuthority(SCRIPT)+scriptName;
        scriptApi.createScript(scriptURI, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM,scriptText);
        String eventURI = RaptureURI.builder(EVENT, repo.getAuthority()).docPath(eventName).build().toString();
        //associate event with script to run test once only 
        eventApi.addEventScript(eventURI, scriptURI, true);
        Reporter.log ("Added event "+eventURI + " to script " + scriptURI);
        Reporter.log("Checking putContent works as expected after firing the event",true);  
        String docURI2 = RaptureURI.builder(DOCUMENT, repo.getAuthority()).docPath("folder1/documenttest2").build().toString();
        docApi.putDoc(docURI2, "{\"key1\":\"value1\"}");
        String s2 = docApi.getDoc(docURI2);
        Assert.assertEquals(s2, "{\"key1\":\"value1\"}");
        Thread.sleep(1000);

        Reporter.log("Checking "+testDocUri+" was written to after event firing",true);
        String getTestDoc = docApi.getDoc(testDocUri);
        Assert.assertEquals(getTestDoc, docVal,"test the document written to repo by fired event");
    }
    
    
    @Test(groups={"document","mongo", "nightly"})
    public void testDocumentListByUriPrefix(){
    	RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, "MONGODB");
        
        Reporter.log("Create some test documents",true);
        String docURIf1d1 = RaptureURI.builder(DOCUMENT, repo.getAuthority()).docPath("folder1/doc1").build().toString();
        String docURIf1d2 = RaptureURI.builder(DOCUMENT, repo.getAuthority()).docPath("folder1/doc2").build().toString();
        String docURIf1d3 = RaptureURI.builder(DOCUMENT, repo.getAuthority()).docPath("folder1/doc3").build().toString();
        String docURIf2f21d1 = RaptureURI.builder(DOCUMENT, repo.getAuthority()).docPath("folder2/folder21/doc1").build().toString();
        String docURIf2f21d2 = RaptureURI.builder(DOCUMENT, repo.getAuthority()).docPath("folder2/folder21/doc2").build().toString();
        String docURIf3d1 = RaptureURI.builder(DOCUMENT, repo.getAuthority()).docPath("folder3/doc1").build().toString();
        String content="{\"key\":\"value\"}";
        docApi.putDoc(docURIf1d1, content);
        docApi.putDoc(docURIf1d2, content);
        docApi.putDoc(docURIf1d3, content);
        docApi.putDoc(docURIf2f21d1, content);
        docApi.putDoc(docURIf2f21d2, content);
        docApi.putDoc(docURIf3d1, content);
        
        Reporter.log("Check folder contents using different depths",true);
        Assert.assertEquals(docApi.listDocsByUriPrefix(RaptureURI.builder(DOCUMENT, repo.getAuthority()).docPath("folder1").build().toString(), 2).size(),3);
        Assert.assertEquals(docApi.listDocsByUriPrefix(RaptureURI.builder(DOCUMENT, repo.getAuthority()).docPath("folder1").build().toString(), 1).size(),3);
        Assert.assertEquals(docApi.listDocsByUriPrefix(RaptureURI.builder(DOCUMENT, repo.getAuthority()).docPath("folder2").build().toString(), 2).size(),3);
        Assert.assertEquals(docApi.listDocsByUriPrefix(RaptureURI.builder(DOCUMENT, repo.getAuthority()).docPath("folder2").build().toString(), 1).size(),1);
        Assert.assertEquals(docApi.listDocsByUriPrefix(RaptureURI.builder(DOCUMENT, repo.getAuthority()).docPath("folder2").build().toString(), 0).size(),3);
        Assert.assertEquals(docApi.listDocsByUriPrefix(RaptureURI.builder(DOCUMENT, repo.getAuthority()).docPath("folder3").build().toString(), 0).size(),1);
       
        Reporter.log("Delete some docs and check folder contents",true);
        docApi.deleteDoc(docURIf1d1);
        docApi.deleteDoc(docURIf3d1);
        Assert.assertEquals(docApi.listDocsByUriPrefix(RaptureURI.builder(DOCUMENT, repo.getAuthority()).docPath("folder1").build().toString(), 2).size(),2);
        Assert.assertEquals(docApi.listDocsByUriPrefix(RaptureURI.builder(DOCUMENT, repo.getAuthority()).docPath("folder1").build().toString(), 1).size(),2);
        try {
        	docApi.listDocsByUriPrefix(RaptureURI.builder(DOCUMENT, repo.getAuthority()).docPath("folder3").build().toString(), 0).size();
        	Assert.fail();
        } catch (Exception e) {
        }
        
        Reporter.log("Recreated some doc and check folder contents",true);
        docApi.putDoc(docURIf3d1, content);
        Assert.assertEquals(docApi.listDocsByUriPrefix(RaptureURI.builder(DOCUMENT, repo.getAuthority()).docPath("folder3").build().toString(), 1).size(),1);
    }
    
    @Test(groups={"document","mongo", "nightly"})
    public void testDocumentWithVersionedRepo(){

    	RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, "MONGODB",true);
        
        String uniqueString = String.valueOf(System.nanoTime());

        String docURI = RaptureURI.builder(DOCUMENT, repo.getAuthority()).docPath("VersionTesting" + uniqueString).asString();
        
        String content1 = "{\"key1\":\"value1\"}";
        String content2 = "{\"key2\":\"value2\"}";
        String content3 = "{\"key3\":\"value3\"}";
        String content4 = "{\"key4\":\"value4\"}";
        String content5 = "{\"key5\":\"value5\", \"key2\":\"value1\"}";
        
        docApi.putDoc(docURI, content1);
        docApi.putDoc(docURI, content2);
        docApi.putDoc(docURI, content3);
        docApi.putDoc(docURI, content4);
        docApi.putDoc(docURI, content5);
        
        //get number of versions
        DocumentMetadata docMeta = docApi.getDocMeta(docURI);
        Assert.assertEquals(docMeta.getVersion().intValue(), 5, "Got wrong number of versions for document");
        
        Assert.assertNotNull(docApi.getDoc(docURI + "@1"), "Version 1 is Null");
        Assert.assertNotNull(docApi.getDoc(docURI + "@2"), "Version 2 is Null");
        Assert.assertNotNull(docApi.getDoc(docURI + "@3"), "Version 3 is Null");
        Assert.assertNotNull(docApi.getDoc(docURI + "@4"), "Version 4 is Null");
        Assert.assertNotNull(docApi.getDoc(docURI + "@5"), "Version 5 is Null");

        Assert.assertEquals(docApi.getDoc(docURI + "@1"), content1, "Content for version 1 does not match");
        Assert.assertEquals(docApi.getDoc(docURI + "@2"), content2, "Content for version 2 does not match");
        Assert.assertEquals(docApi.getDoc(docURI + "@3"), content3, "Content for version 3 does not match");
        Assert.assertEquals(docApi.getDoc(docURI + "@4"), content4, "Content for version 4 does not match");
        Assert.assertEquals(docApi.getDoc(docURI + "@5"), content5, "Content for version 5 does not match");
    }
    
    @AfterClass(groups={"document","mongo", "nightly"})
    public void AfterTest(){
    	helper.cleanAllAssets(); 
    }
    
}
