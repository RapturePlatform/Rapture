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
package rapture.operation;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.HttpOperationApi;
import rapture.helper.IntegrationTestHelper;

/**
 * Tests to exercise Full Text Search. Note: We should be able to use any repo type; Memory, Mongo, Postgres etc. as the purpose of the test is to verify that
 * the API methods successfully interact with ElasticSearch;
 */

public class OperationApiIntegrationTest {

    private HttpLoginApi raptureLogin = null;
    private HttpOperationApi operationApi = null;
    private HttpDocApi docApi = null;
    CallingContext callingContext = null;
    boolean cleanUpPrevious = true;
    IntegrationTestHelper helper;
    RaptureURI repo;

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
    @BeforeClass(groups = { "nightly" })
    @Parameters({ "RaptureURL", "RaptureUser", "RapturePassword" })
    public void setUp(@Optional("http://localhost:8665/rapture") String url, @Optional("rapture") String username, @Optional("rapture") String password) {

        // If running from eclipse set env var -Penv=docker or use the following
        // url variable settings:
        // url="http://192.168.99.101:8665/rapture"; //docker
        // url="http://localhost:8665/rapture";

        helper = new IntegrationTestHelper(url, username, password);
        raptureLogin = helper.getRaptureLogin();
        docApi = helper.getDocApi();
        operationApi = helper.getOperationApi();
        callingContext = raptureLogin.getContext();

        repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, "MEMORY");

    }

    /**
     * TestNG method to cleanup.
     *
     * @param none
     * @return none
     */
    @AfterClass(groups = { "nightly" })
    public void afterTest() {
        helper.cleanAllAssets();
        raptureLogin = null;
    }

    @Test(groups = { "nightly" })
    public void testInvoke() throws IOException {
        String docu = RaptureURI.builder(repo).docPath("/document").asString();
        docApi.putDoc(docu, "{\"a\":1,\"b\":2,\"c\":3,\"incr\":\"this[\'b\'] = cast(this.b, \'integer\') + params.b; return this;\"}");
        Map<String, Object> m = new HashMap<>();
        m.put(new String("b"), new Integer(3));
        operationApi.invokeSave(docu, "incr", m);
        m.put(new String("b"), new Integer(4));
        operationApi.invokeSave(docu, "incr", m);
        String content = docApi.getDoc(docu);
        assertEquals(content, "{\"a\":1,\"b\":9,\"c\":3,\"incr\":\"this[\'b\'] = cast(this.b, \'integer\') + params.b; return this;\"}");
    }

    @Test(groups = { "nightly" })
    public void testInvokeAlt() throws IOException {
        String docu = RaptureURI.builder(repo).docPath("/document").asString();
        String alt = RaptureURI.builder(repo).docPath("/alternate").asString();
        docApi.putDoc(docu, "{\"alpha\":1,\"bravo\":2,\"charlie\":3}");
        docApi.putDoc(alt,
                "{\"setVal\" : \"this[params[\'fieldToChange\']] = params[\'newFieldValue\']; return this;\", "
                        + "\"increment\" : \"this[params[\'fieldToIncrement\']] = cast(this[params[\'fieldToIncrement\']], \'integer\') + 1; println(this); return this;\" }");
        Map<String, Object> m = new HashMap<>();
        m.put(new String("fieldToChange"), new String("bravo"));
        m.put(new String("newFieldValue"), new String("Five"));
        m.put(new String("fieldToIncrement"), new String("charlie"));
        operationApi.invokeSaveAlt(docu, "setVal", m, alt);
        Map<String, Object> content = operationApi.invokeAlt(docu, "increment", m, alt);
        assertEquals(content.get("bravo"), "Five");
        assertEquals(content.get("charlie"), 4);
    }

    @Test(groups = { "nightly" })
    public void testInterface() throws IOException {
        String docu = RaptureURI.builder(repo).docPath("/document").asString();
        String iface = RaptureURI.builder(repo).docPath("/interface").asString();
        docApi.putDoc(docu, "{\"alpha\":1,\"bravo\":2,\"charlie\":3, \"$interface\":\"" + iface + "\"}");
        docApi.putDoc(iface, "{\"setVal\" : \"this[params[\'fieldToChange\']] = params[\'newFieldValue\']; return this;\", "
                + "\"inc\" : \"this[params[\'fieldToIncrement\']] = cast(this[params[\'fieldToIncrement\']], \'integer\') + 1; println(this); return this;\" }");
        Map<String, Object> m = new HashMap<>();
        m.put(new String("fieldToChange"), new String("bravo"));
        m.put(new String("newFieldValue"), new String("Six"));
        m.put(new String("fieldToIncrement"), new String("alpha"));
        operationApi.invokeSave(docu, "setVal", m);
        Map<String, Object> content = operationApi.invoke(docu, "inc", m);
        assertEquals(content.get("bravo"), "Six");
        assertEquals(content.get("alpha"), 2);
    }

    @Test(groups = { "nightly" })
    public void testParent() throws IOException {
        String docu = RaptureURI.builder(repo).docPath("/document").asString();
        String parent1 = RaptureURI.builder(repo).docPath("/parent1").asString();
        String parent2 = RaptureURI.builder(repo).docPath("/parent2").asString();
        String parent3 = RaptureURI.builder(repo).docPath("/parent3").asString();
        docApi.putDoc(docu, "{\"alpha\":1,\"bravo\":2,\"charlie\":3, \"$parent\":\"" + parent1 + "\"}");
        docApi.putDoc(parent1, "{\"setVal\" : \"this['alpha'] = params['newFieldValue']; return this;\", \"$parent\" : \"" + parent2 + "\" }");
        docApi.putDoc(parent2, "{\"setVal\" : \"this['bravo'] = params['newFieldValue']; return this;\", \"$parent\" : \"" + parent3 + "\" }");
        docApi.putDoc(parent3,
                "{\"increment\" : \"this[params[\'fieldToIncrement\']] = cast(this[params[\'fieldToIncrement\']], \'integer\') + 1; println(this); return this;\" }");

        Map<String, Object> m = new HashMap<>();
        m.put(new String("newFieldValue"), new String("Seven"));
        m.put(new String("fieldToIncrement"), new String("charlie"));
        operationApi.invokeSave(docu, "setVal", m);
        Map<String, Object> content = operationApi.invoke(docu, "increment", m);
        assertEquals(content.get("alpha"), "Seven");
        assertEquals(content.get("bravo"), 2);
        assertEquals(content.get("charlie"), 4);

        docApi.putDoc(docu, "{\"alpha\":1,\"bravo\":2,\"charlie\":3, \"$parent\":\"" + parent1 + "\", \"$interface\":\"" + parent2 + "\"}");

        operationApi.invokeSave(docu, "setVal", m);
        content = operationApi.invoke(docu, "increment", m);
        assertEquals(content.get("alpha"), 1);
        assertEquals(content.get("bravo"), "Seven");
        assertEquals(content.get("charlie"), 4);

    }
}
