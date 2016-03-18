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

import static rapture.common.Scheme.DOCUMENT;
import static rapture.common.Scheme.EVENT;
import static rapture.common.Scheme.SCRIPT;
import static rapture.common.Scheme.SERIES;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.Reporter;

import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.common.SeriesString;
import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpEventApi;
import rapture.common.client.HttpIdGenApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.HttpScriptApi;
import rapture.common.client.HttpSeriesApi;
import rapture.common.client.SimpleCredentialsProvider;
import rapture.common.model.DocumentRepoConfig;
import rapture.util.ResourceLoader;


/**
 * @author Jonathan Major <jonathan.major@incapturetechnologies.com>
 * @version 1.0
 * @since 2014-03-01
 *
 * Smoke tests are designed to yes the basic functionality and environment of a Rapture system, namely:
 * 1. Document repository on MongoDB implementation: create repository, add document, drop repository.
 * 2. Document versioned Repository
 * 3. Fire an event on calling Document.putDoc() to run a Reflex script
 * 4. Series Repository on Cassandra implementation: create repository, add series, drop repository
 * 5. Blob repository on MongoDB implementation: create repository, add a blob, drop repository
 * 6. Additionally tests underlying integration with MongoDB, Cassandra and RabbitMQ
 * <p>
 * See /RaptureIntTests/README.md on setup and running instructions.
 *
 */

public class SmokeTests {

    private HttpLoginApi raptureLogin = null;
    private HttpSeriesApi series = null;
    private HttpDocApi document = null;
    private HttpScriptApi script = null;
    private HttpEventApi event = null;
    private HttpIdGenApi fountain = null;

    /**
     * Setup TestNG method to create Rapture login object and objects.
     *
     * @param RaptureURL Passed in from <env>_testng.xml suite file
     * @param RaptureUser Passed in from <env>_testng.xml suite file
     * @param RapturePassword Passed in from <env>_testng.xml suite file
     * @return none
     */
    @BeforeClass(groups={"smoke"})
    @Parameters({"RaptureURL","RaptureUser","RapturePassword"})
    public void setUp(@Optional("http://localhost:8665/rapture")String url,
                      @Optional("rapture")String username, @Optional("rapture")String password ) {

        //If running from eclipse set env var -Penv=docker or use the following url variable settings:
        //url="http://192.168.99.101:8665/rapture"; //docker
        //url="http://localhost:8665/rapture";
        System.out.println("Using url " + url);
        raptureLogin = new HttpLoginApi(url, new SimpleCredentialsProvider(username, password));
        raptureLogin.login();
        series = new HttpSeriesApi(raptureLogin);
        document = new HttpDocApi(raptureLogin);
        script = new HttpScriptApi(raptureLogin);
        event = new HttpEventApi(raptureLogin);
        fountain = new HttpIdGenApi(raptureLogin);
    }

    /**
     * TestNG method to cleanup.
     *
     * @param none
     * @return none
     */
    @AfterClass(groups={"smoke"})
    public void afterTest() {
        raptureLogin = null;
    }

    /**
     * Test to create an unversioned document repository, put 2 documents and then return all documents in one call.
     * <p>
     * Relies on auto-repo creation when using document.putDoc()
     *
     * @param none
     * @return none
     */
    //
    @Test(groups={"smoke"})
    public void autoRepoCreationTest(){
        String s1 = "document://test123/docA";
        String s2 = "document://test123/docAB";

        document.putDoc(s1, "{\"key1\":\"val1\"}");
        document.putDoc(s2, "{\"key2\":\"val2\"}");

        Reporter.log(document.getDoc(s1),true);
        Reporter.log(document.getDoc(s2),true);

        Map<String, RaptureFolderInfo> allChildren = document.listDocsByUriPrefix("//test123",1);
        Reporter.log("Number of docs in list is " + allChildren.size(),true);
        Assert.assertEquals(allChildren.size(), 2,"Assert to check the number of children documents in //test123 path.");
    }

    /**
     * Setup a document repository to call a Reflex script whenever a document is written to it.
     * <p>
     * Uses the Rapture event mechanism to
     *  <p>
     * The Reflex script is in /resources directory
     * <p>
     * IDGEN is a mechanism to create a unique ID for each document insert. YOu can specify the base, length of ID and prefix.
     *
     * @param none
     * @return none
     */
    @Test(groups={"smoke"})
    public void eventsTest() throws InterruptedException{
        String auth = "test.doceventsrc";
        String authtgt = "test.doceventtgt";
        String srcRepo = "//" + auth;
        String tgtRepo = "//" + authtgt;
        String repoConfigTemplate = "NREP {} USING %s {prefix=\"%s\"}"; //Configuration for a versioned document repository
        //This IDGEN will produce unique IDs as follows: TST00001, TST00002 etc
        String fountainCfgTemplate = "IDGEN { base=\"10\",length=\"5\", prefix=\"TST\"} USING MONGODB {prefix=\"testfountain.%s\"}";
        String fountainCfg = String.format(fountainCfgTemplate, (long)System.nanoTime());

        if(document.docRepoExists(tgtRepo)){
            document.deleteDocRepo(tgtRepo);
        }

        if(document.docRepoExists(srcRepo)){
            String fountainUri = document.getDocRepoIdGenConfig(srcRepo).getAddressURI().toString();
            fountain.deleteIdGen(fountainUri);
            script.deleteScript("script://test.doceventsrc/copyfromsrctotgt");
            event.deleteEvent("event://test.doceventsrc/data/update");
            document.deleteDocRepo(srcRepo);
        }

        String tgtcfg = String.format(repoConfigTemplate, "MONGODB", authtgt);
        document.createDocRepo(tgtRepo, tgtcfg);
        Reporter.log("Target Repo " + tgtRepo + " does not exist so creating it with config: " + tgtcfg,true);

        //repo does not exist so create it
        String srccfg = String.format(repoConfigTemplate, "MONGODB", auth);
        document.createDocRepo(srcRepo, srccfg);
        Reporter.log("Repo " + srcRepo + " does not exist so creating it with config: " + srccfg,true);

        //attach fountain
        Reporter.log("Attaching fountain: " + fountainCfg,true);
        DocumentRepoConfig repoConfig = document.setDocRepoIdGenConfig(srcRepo, fountainCfg);
        Reporter.log("Repo config: " + repoConfig,true);

        //load script and create a script uri
        String scriptURI = RaptureURI.builder(SCRIPT,auth).docPath("copyfromsrctotgt").build().toString();
        Reporter.log("script uri: " + scriptURI,true);
        String reflexScript = ResourceLoader.getResourceAsString(this, "/copyDocumentSrcToTgtRepo.rfx");
        script.createScript(scriptURI, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM,reflexScript);

        String eventName = "data/update";
        String eventURI = RaptureURI.builder(EVENT, auth).docPath(eventName).build().toString();
        Reporter.log("event uri: " + eventURI,true);
        //associate event with script
        event.addEventScript(eventURI, scriptURI, false);

        String docToWrite = "{\"key1\":\"val1 written by event\"}";
        String docName =  "/doc" + System.nanoTime();
        String putContentSrcUri = document.putDoc(srcRepo + docName, docToWrite);
        Assert.assertEquals(putContentSrcUri, "document://test.doceventsrc" + docName);
        Thread.sleep(500);
        String getContentTgt = document.getDoc(tgtRepo + docName);
        String getContentSrc = document.getDoc(srcRepo + docName);
        Reporter.log("Source document: " + getContentSrc, true);
        Reporter.log("Target document: " + getContentTgt, true);

        //cleanup
        String fountainUri = document.getDocRepoIdGenConfig(srcRepo).getAddressURI().toString();
        fountain.deleteIdGen(fountainUri);
        script.deleteScript("script://test.doceventsrc/copyfromsrctotgt");
        event.deleteEvent("event://test.doceventsrc/data/update");
        document.deleteDocRepo(srcRepo);

        Assert.assertEquals(getContentSrc, getContentTgt);
    }

    /**
     * Create, write to and retrieve from a versioned Document repository using a MongoDB implementation.
     *
     * @param none
     * @return none
     */
    @Test(groups ={"smoke"}, enabled=true)
    public void documentVersionedTest() {
        String authorityName = "test.document";
        authorityName = authorityName + "_" + System.nanoTime();
        String putcontent = "{ \"testkey1\" : \"hello1\"}";

        String docRepoUri = RaptureURI.builder(DOCUMENT, authorityName).asString();
        String config = String.format("NREP {} USING MONGODB {prefix=\"test.%s\"}", System.nanoTime());
        try {
            document.createDocRepo(docRepoUri, config);
        }
        catch (Exception e) {
            Assert.fail("Checkout verification for creating a document repo.");
        }
        Reporter.log("Created document repo " + docRepoUri,true);

        String docUri = RaptureURI.builder(DOCUMENT, authorityName).docPath("folder1").asString();
        document.putDoc(docUri, putcontent);
        String getcontent = document.getDoc(docUri);
        //check document is good
        Assert.assertEquals(putcontent, getcontent, "Checkout verification to compare written == retrieved document.");

        //check version of document
        Integer version = document.getDocAndMeta(docUri).getMetaData().getVersion();
        Assert.assertEquals(version.intValue(),1,"Check a new document in version repository has a version == 1");

        //write to the same document againand check version is updated
        String putcontent2 = "{ \"testkey2\" : \"hello2\"}";
        document.putDoc(docUri, putcontent2);
        Integer version2 = document.getDocAndMeta(docUri).getMetaData().getVersion();
        Assert.assertEquals(version2.intValue(),2,"Check a new document in version repository has a version == 2");

        String getcontent2 = document.getDoc(docUri);
        Assert.assertEquals(putcontent2, getcontent2, "Checkout verification to compare written == retrieved document.");

        //cleanup
        document.deleteDocRepo(docRepoUri);
        Assert.assertFalse(document.docRepoExists(docRepoUri));
        Reporter.log("Dropped document repo " + docRepoUri,true);
    }

    @DataProvider(name = "fileNames")
    public Object[][] fileNameData() {
        return new Object[][] { {"/AccountDocumentSmall.json"},{"/AccountDocumentSmallModified.json"},{"/AccountPositionsDocumentSmall.json"}
        ,{"/TransactionDocumentSmall.json"}};
    }

    /**
     * Load json files from /resources directory and write to a versioned repository.
     *
     * @param none
     * @return none
     */
    @Test(groups ={"smoke"}, enabled=true, dataProvider = "fileNames")
    public void documentCheckoutLoadJsonFileTest(String filename) {
        String authorityName = "test.document" + System.nanoTime();
        authorityName = authorityName + "_" + System.nanoTime();
        String putcontent = ResourceLoader.getResourceAsString(this, filename);

        String docRepoUri = RaptureURI.builder(DOCUMENT, authorityName).asString();
        String config = String.format("NREP {} USING MONGODB {prefix=\"test.%s\"}", System.nanoTime());

        document.createDocRepo(docRepoUri, config);
        Reporter.log("Created document repo " + docRepoUri, true);

        String docUri = RaptureURI.builder(DOCUMENT, authorityName).docPath("folder1").asString();
        document.putDoc(docUri, putcontent);
        String getcontent = document.getDoc(docUri);
        Reporter.log(getcontent, true);
        Assert.assertEquals(putcontent, getcontent, "Checkout verification to compare written == retrieved document.");

        document.deleteDocRepo(docRepoUri);
        Assert.assertFalse(document.docRepoExists(docRepoUri));
        Reporter.log("Dropped document repo " + docRepoUri, true);
    }

    /**
     * Create, write to and retrieve from a Series repository on Cassandra implementation.
     *
     * @param none
     * @return none
     */
    @Test(groups ={"smoke"}, enabled=true, description="Create, write to and retrieve from a Series type on Cassandra implementation")
    public void seriesCheckoutTest() {
        Random r = new Random();
        int loopCount = 10;
        String folderStr = "folder1";
        //create the series configuration string
        String authorityName = "test.seriescheckout" + "_" + System.nanoTime();
        String colFamilyName = "colname" + "_" + System.nanoTime();
        String ks = "testkeyspace" + System.nanoTime();
        String config = "SREP {} USING CASSANDRA {keyspace=\"" + ks + "\", cf=\"" + colFamilyName + "\"}";
        Reporter.log("Series config string: " + config, true);

        //construct the URIs using RaptureURI class
        String seriesRepoURI = RaptureURI.builder(SERIES, authorityName).asString();
        String seriesURI = RaptureURI.builder(SERIES, authorityName).docPath(folderStr).asString();
        Reporter.log(" " + seriesRepoURI, true);
        series.createSeriesRepo(seriesRepoURI, config);
        Reporter.log("Created series " + seriesURI + " for checkout purposes.", true);

        //create series data
        for (int i = 0; i < loopCount; i++) {
            String colKey =  "col_" + System.nanoTime();
            double nextDouble = r.nextDouble();
            series.addDoubleToSeries(seriesURI, colKey, nextDouble);
            Reporter.log(colKey + " " + nextDouble, true);
        }
        //get series data
        List<SeriesString> sd = series.getPointsAsStrings(seriesURI);

        List<String> s =new LinkedList<String>();
        List<String> cols = new LinkedList<String>();

        for (SeriesString ss : sd) {
            s.add(ss.getValue());
            cols.add(ss.getKey());
        }

        int seriesValues = s.size();
        int SeriesCols = cols.size();
        //check written == retrieved
        Assert.assertEquals(seriesValues, loopCount, "Number of Series values should be " + loopCount + " but is " + seriesValues);
        Assert.assertEquals(SeriesCols, loopCount, "Number of Series columns should be " + loopCount + " but is " + SeriesCols);

        //drop the repo and check if it is deleted
        series.deleteSeriesRepo(seriesRepoURI);
        Boolean doesSeriesRepoExist = series.seriesRepoExists(seriesRepoURI);
        Assert.assertFalse(doesSeriesRepoExist.booleanValue(),"expected false but got " + doesSeriesRepoExist.toString() );
        Reporter.log("Deleted series repo " + seriesRepoURI, true);
    }
}
