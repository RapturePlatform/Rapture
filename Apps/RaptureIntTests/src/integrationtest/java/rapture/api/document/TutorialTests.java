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
package rapture.api.document;

import java.io.IOException;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.SimpleCredentialsProvider;
import rapture.common.exception.RaptureException;
import rapture.util.ResourceLoader;


/**
 * @author Jonathan Major <jonathan.major@incapturetechnologies.com>
 * @version 1.0
 * @since 2016-03-13
 *
 * This test class provides a skeleton to implement Testng integration tests against your Rapture environment
 * <p>
 * The setup method instantiates two Rapture objects:
 *    - raptureLogin : login api object
 *    - document     : document api object
 * <p>
 * See /RaptureIntTests/README.md on setup and running instructions.
 */

public class TutorialTests {

    private HttpLoginApi raptureLogin = null;
    private HttpDocApi document = null;

    /**
     * Setup TestNG method to create Rapture login object and objects.
     *
     * @param RaptureURL Passed in from <env>_testng.xml suite file
     * @param RaptureUser Passed in from <env>_testng.xml suite file
     * @param RapturePassword Passed in from <env>_testng.xml suite file
     *
     * @return none
     */
    @BeforeClass(groups={"document"})
    @Parameters({"RaptureURL","RaptureUser","RapturePassword"})
    public void setUp(@Optional("http://localhost:8665/rapture")String url,
                      @Optional("rapture")String username, @Optional("rapture")String password ) {

        ///If running from eclipse set environment variable -Penv=docker 
        //or use the following:
        //  url="http://localhost:8665/rapture";
        //  url="http://192.168.99.101:8665/rapture"; //docker
        
        Reporter.log("Using URL: " + url,true);
        raptureLogin = new HttpLoginApi(url, new SimpleCredentialsProvider(username, password));
        try{
            raptureLogin.login();
            document = new HttpDocApi(raptureLogin);
        } catch (RaptureException re) {
            Reporter.log(re.getFormattedMessage(),true);
        }
    }

    /**
     * TestNG method to cleanup.
     *
     * @param none
     * @return none
     */
    @AfterClass(groups={"document"})
    public void afterTest() {
        raptureLogin = null;
        document.deleteDocRepo("document://myfirstrepo");
        Reporter.log("AfterTest method run.",true);
    }

    /**
     * Skeleton test method for Tutorial referenced in /RaptureIntTests/README.md
     *
     * Test objective:
     * 1. create a versioned document repository
     * 2. write a document (json) to it
     * 3. update (i.e. write to the same )the document i.e. write to the same document as step 2.
     * 4. Assert the version number has been incremented
     * 5. Get the difference between document version using [jsonpatch](http://jsonpatch.com/)
     * 6. Assert on the actual and expected difference between documents (json)
     *
     * @param none
     * @return none
     */
    @Test(groups={"document"})
    public void myFirstTest(){
        //----------------------------------------------------------------------------------------------
        //Task: Define the Document repository with versioning
        //----------------------------------------------------------------------------------------------
        //Details:
        //  1. Document repositories use the following configuration string format:
        //      <Repo type> {} USING <Implementation Type> {prefix="prefix for collection or table name"}
        //  2. The configuration string is in fact a complex instruction written in a repository domain specific language (DSL)
        //     that is used to define the capabilities and underlying implementation of the repository
        //
        //In this case:
        //  1. NREP    : The Repository is a Document type with built-in versioning
        //  2. MONGODB : The Document repository is backed by MongoDB implementation
        //  3. prefix  : In most cases the configuration associated with the implementation has a prefix parameter that is used to
        //               define a table or a collection or a prefix for such entities in the underlying storage
        //               In this case the prefix used for underlying Mongo collection will start with "firstrepo"
        //----------------------------------------------------------------------------------------------
        String config = "NREP {} USING MONGODB {prefix=\"firstrepo\"}";

        //----------------------------------------------------------------------------------------------
        //Task: Define the URI (or address) of the document repository.
        //----------------------------------------------------------------------------------------------
        //Details:
        //  1. The URI is a key concept in Rapture; this is how data is uniquely addressed
        //  2. The URI does not have to match the prefix set in the repositories configuration string
        //  3. It is possible to use a shorthand URI without the schema prefix e.g. //myfirstrepo
        //----------------------------------------------------------------------------------------------
        String docRepoUri = "document://myfirstrepo";

        //----------------------------------------------------------------------------------------------
        //Task: Call createDocRepo(String docRepoUri, String config) method to create the repository
        //----------------------------------------------------------------------------------------------
        document.createDocRepo(docRepoUri, config);

        Reporter.log("Document repository config: " + config, true);
        Reporter.log("Document repository URI   : " + docRepoUri, true);
        
        //----------------------------------------------------------------------------------------------
        //Task: Check the repo exists and Assert on the Boolean response using docRepoExists(String docRepoUri)
        //----------------------------------------------------------------------------------------------
        Boolean docRepoExists = document.docRepoExists(docRepoUri);
        Assert.assertEquals(docRepoExists.booleanValue(), true,"check that repo exists.");

        //----------------------------------------------------------------------------------------------
        //Task: Load a json file (from /resources) from disk and create a Rapture document
        //----------------------------------------------------------------------------------------------
        //Details:
        //  1. Add the document name to end of the repo URI string e.g. //myfirstrepo/doc1
        //     - This is a string so use the String + operator
        //     - In this same manner a folder could be added to e.g.  //myfirstrepo/folder/doc1
        //  2. Load the json file using rapture.util.ResourceLoader.getResourceAsString(this, <filename>);
        //  3. Call method rapture.common.client.HttpDocApi.putDoc(String docUri, String content) to create the Rapture document
        //  4. The call to putDoc(String docUri, String content) is wrapped in a rapture.common.exception.RaptureException try/catch block
        //  5. Retrieve the version number of the document using rapture.common.client.HttpDocApi.getDocAndMeta(String docUri)
        //----------------------------------------------------------------------------------------------
        String docUri = docRepoUri + "/doc1";
        String jsonDoc1 = ResourceLoader.getResourceAsString(this, "/AccountDocumentSmall.json");

        try{
            document.putDoc(docUri, jsonDoc1);
        } catch (RaptureException re){
            Reporter.log(re.getFormattedMessage(),true);
            //could fail the test here..
            Assert.assertFalse(true);
        }

        Integer version = document.getDocAndMeta(docUri).getMetaData().getVersion();
        Assert.assertEquals(version.intValue(),1,"Check athat document has version == 1");

        //----------------------------------------------------------------------------------------------
        //Task: Load a slightly different json and write to same document URI and check the version was updated
        //----------------------------------------------------------------------------------------------

        String jsonDoc2 = ResourceLoader.getResourceAsString(this, "/AccountDocumentSmallModified.json");
        document.putDoc(docUri, jsonDoc2);
        Integer version2 = document.getDocAndMeta(docUri).getMetaData().getVersion();
        Assert.assertEquals(version2.intValue(),2,"Check athat document has version == 2");

        //----------------------------------------------------------------------------------------------
        //Task: use JsonPatch to get the difference between the two documents
        //----------------------------------------------------------------------------------------------
        // Details:
        // 1. Get the document versions from repo using rapture.common.client.HttpDocApi.getDoc(String docUri)
        // - Note the use of the @ symbol to retrieve the version, which is incremented with each call to putDoc()
        // 2. JsonPatch (https://github.com/fge/json-patch) is an implementation of https://tools.ietf.org/html/rfc6902
        //----------------------------------------------------------------------------------------------
        String docVersion1 = document.getDoc(docUri + "@1");
        String docVersion2 = document.getDoc(docUri + "@2"); // also use the uri without @2 to retrieve latest version
        JsonPatch patch = JsonDiff.asJsonPatch(getNodeFromString(docVersion1), getNodeFromString(docVersion2));
        Reporter.log("Difference is " + patch.toString(),true);
        String expectedPatch = "[op: replace; path: \"/glossary/GlossDiv/GlossList/GlossEntry/GlossSee\"; value: \"markup1\", op: replace; path: \"/glossary/GlossDiv/title\"; value: \"X\"]";
        Assert.assertEquals(patch.toString(), expectedPatch);
    }

    private JsonNode getNodeFromString(String s) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode ns = null;
        try {
            ns = mapper.readValue(s, JsonNode.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ns;
    }

}
