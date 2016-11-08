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
package rapture.server.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;

import rapture.common.impl.jackson.JacksonUtil;
import rapture.server.rest.util.TestUtils;

public class RestServerIntTest {
	private String apiKey = "18d84e26-e04c-4a5d-bcc9-637785922aab";
    @Before
    public void setup() throws UnirestException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        TestUtils.setupHttpsUnirest();
        //HttpResponse<String> result = Unirest.post("https://localhost:4567/login")
        //		.header("x-api-key", "18d84e26-e04c-4a5d-bcc9-637785922aab")
        //        .body("{\"username\":\"rapture\", \"password\":\"rapture\"}").asString();
        //printResponse(result);
        //if (result.getStatus() != 200) {
        //    fail("Failed to login");
        //}
    }
	
    @Test
    public void testDocCreateRepo() throws UnirestException {
        HttpResponse<String> result = Unirest.post("https://localhost:4567/doc/config")
        		.header("x-api-key",apiKey).body("{\"config\":\"NREP {} USING MONGODB {prefix=\\\"config\\\"}\"}").asString();
        printResponse(result);
        assertEquals(200, result.getStatus());
        result = Unirest.post("https://localhost:4567/doc/config")
        		.header("x-api-key",apiKey).body("{\"config\":\"NREP {} USING MONGODB {prefix=\\\"config\\\"}\"}").asString();
        printResponse(result);
        assertEquals(409, result.getStatus());
    }

    @Test
    public void testDocGet() throws UnirestException {
        HttpResponse<String> result = Unirest.get("https://localhost:4567/doc/config/mqSeriesConfig").header("x-api-key",apiKey).asString();
        printResponse(result);
        assertTrue(result.getBody().indexOf("connectionFactory") != -1);
    }

    @Test
    public void testDocGetWithVersion() throws UnirestException {
        HttpResponse<String> result = Unirest.get("https://localhost:4567/doc/config/mqSeriesConfig@2").header("x-api-key",apiKey).asString();
        printResponse(result);
        assertTrue(result.getBody().indexOf("connectionFactory") != -1);
    }

    @Test
    public void testDocGetMeta() throws UnirestException {
        HttpResponse<String> result = Unirest.get("https://localhost:4567/doc/config/mqSeriesConfig?meta=true").header("x-api-key",apiKey).asString();
        printResponse(result);
        assertTrue(result.getBody().indexOf("connectionFactory") != -1);
    }

    @Test
    public void testDocPut() throws UnirestException {
        int status = Unirest.put("https://localhost:4567/doc/config/mqSeriesConfig").header("x-api-key",apiKey)
                .body("{ \"environment\" : \"test\", \"connectionFactory\" : \"CF\", \"outputQueue\" : \"RAP_TO_TB\", \"writeEnabled\" : false }").asString()
                .getStatus();
        assertEquals(200, status);
    }

    @Test
    public void testDocDeleteRepo() throws UnirestException {
        HttpResponse<String> result = Unirest.delete("https://localhost:4567/doc/config").header("x-api-key",apiKey).asString();
        printResponse(result);
    }

    @Test
    public void testDocDelete() throws UnirestException {
        HttpResponse<String> result = Unirest.delete("https://localhost:4567/doc/config/mqSeriesConfig").header("x-api-key",apiKey).asString();
        printResponse(result);
        int status = Unirest.get("https://localhost:4567/doc/config/mqSeriesConfig").header("x-api-key",apiKey).asString().getStatus();
        assertEquals(404, status);
    }

    @Test
    public void testBlobCreateRepo() throws UnirestException {
        HttpResponse<String> result = Unirest.post("https://localhost:4567/blob/archive").header("x-api-key",apiKey)
                .body("{\"config\":\"BLOB {} USING MONGODB {prefix=\\\"archive\\\"}\","
                        + "\"metaConfig\":\"REP {} USING MONGODB {prefix=\\\"archive\\\"}\"}")
                .asString();
        printResponse(result);
        assertEquals(200, result.getStatus());
        result = Unirest.post("https://localhost:4567/blob/archive").header("x-api-key",apiKey)
                .body("{\"config\":\"BLOB {} USING MONGODB {prefix=\\\"archive\\\"}\","
                        + "\"metaConfig\":\"REP {} USING MONGODB {prefix=\\\"archive\\\"}\"}")
                .asString();
        printResponse(result);
        assertEquals(409, result.getStatus());
    }

    @Test
    public void testBlobGet() throws UnirestException {
        HttpResponse<String> result = Unirest.get("https://localhost:4567/blob/archive/FIX/2016/05/18/8668_input").header("x-api-key",apiKey).asString();
        printResponse(result);
    }

    @Test
    public void testBlobPut() throws UnirestException {
        int status = Unirest.put("https://localhost:4567/blob/archive/FIX/2016/05/18/8668_input")
        		.header("x-api-key",apiKey).header("Content-Type", "text/plain").body("a string value")
                .asString().getStatus();
        assertEquals(200, status);
    }

    @Test
    public void testBlobDeleteRepo() throws UnirestException {
        HttpResponse<String> result = Unirest.delete("https://localhost:4567/blob/archive").header("x-api-key",apiKey).asString();
        printResponse(result);
    }

    @Test
    public void testBlobDelete() throws UnirestException {
        HttpResponse<String> result = Unirest.delete("https://localhost:4567/blob/archive/FIX/2016/05/18/8668_input").header("x-api-key",apiKey).asString();
        printResponse(result);
        int status = Unirest.get("https://localhost:4567/blob/archive/FIX/2016/05/18/8668_input").header("x-api-key",apiKey).asString().getStatus();
        assertEquals(404, status);
    }

    @Test
    public void testSeriesCreateRepo() throws UnirestException {
        HttpResponse<String> result = Unirest.post("https://localhost:4567/series/srepo").header("x-api-key",apiKey)
                .body("{\"config\":\"SREP {} USING MONGODB {prefix=\\\"srepo\\\"}\"}").asString();
        printResponse(result);
        assertEquals(200, result.getStatus());
        result = Unirest.post("https://localhost:4567/series/srepo").header("x-api-key",apiKey)
                .body("{\"config\":\"SREP {} USING MONGODB {prefix=\\\"srepo\\\"}\"}").asString();
        printResponse(result);
        assertEquals(409, result.getStatus());
    }

    @Test
    public void testSeriesGet() throws UnirestException {
        HttpResponse<String> result = Unirest.get("https://localhost:4567/series/srepo/x").header("x-api-key",apiKey).asString();
        printResponse(result);
    }

    @Test
    public void testSeriesPut() throws UnirestException {
        int status = Unirest.put("https://localhost:4567/series/srepo/x").header("x-api-key",apiKey)
                .body("{ \"keys\":[\"k1\",\"k2\"], \"values\":[\"x\",\"y\"]}").asString()
                .getStatus();
        assertEquals(200, status);
    }

    @Test
    public void testSeriesDeleteRepo() throws UnirestException {
        HttpResponse<String> result = Unirest.delete("https://localhost:4567/series/srepo").header("x-api-key",apiKey).asString();
        printResponse(result);
    }

    @Test
    public void testSeriesDelete() throws UnirestException {
        HttpResponse<String> result = Unirest.delete("https://localhost:4567/series/srepo/x").header("x-api-key",apiKey).asString();
        printResponse(result);
        result = Unirest.get("https://localhost:4567/series/srepo/x").asString();
        printResponse(result);
    }

    @Test
    public void testWorkorderCreate() throws UnirestException {
        HttpResponse<String> result = Unirest.post("https://localhost:4567/workorder/workflows/recon").header("x-api-key",apiKey).asString();
        printResponse(result);
    }

    /**
     * Create a structured store repository
     * @throws UnirestException
     */
    
    @Test
    public void testSStoreRepoCreate() throws UnirestException {
    	String repoName = "mysstore" + System.currentTimeMillis();
    	
    	HttpResponse<String> result = Unirest.post("https://localhost:4567/sstore/" + repoName)
    		.header("x-api-key",apiKey)
    		.body("{\"config\":\"STRUCTURED {} USING POSTGRES {}\"}").asString();
    	printResponse(result);
    	assertEquals(200, result.getStatus());
    	
    }
    
    /**
     * Create a structured store repository and the delete it
     * @throws UnirestException
     */
    @Test
    public void testSStoreRepoDelete() throws UnirestException {
    	String repoName = "mysstore" + System.currentTimeMillis();
    	//create repo
    	HttpResponse<String> result = Unirest.post("https://localhost:4567/sstore/" + repoName)
        		.header("x-api-key",apiKey)
        		.body("{\"config\":\"STRUCTURED {} USING POSTGRES {}\"}").asString();
        printResponse(result);
    	//delete it
        HttpResponse<String> delresult = Unirest.delete("https://localhost:4567/sstore/" + repoName)
        		.header("x-api-key",apiKey).asString();
        printResponse(delresult);
        //assert that repo isnt there post-delete; if the repo wasn't deleted the post would fail with 409 
        HttpResponse<String> createresult = Unirest.post("https://localhost:4567/sstore/" + repoName)
        		.header("x-api-key",apiKey)
        		.body("{\"config\":\"STRUCTURED {} USING POSTGRES {}\"}").asString();
        printResponse(createresult);
        assertEquals(200, result.getStatus());
        	
    }
    
    /**
     * Create a structured store repository, add a table, add a row and get data 
     * @throws UnirestException
     */
    @Test
    public void testSStoreGetData() throws UnirestException {
    	String repoName = "mysstore" + System.currentTimeMillis();
    	//create repo
    	HttpResponse<String> result = Unirest.post("https://localhost:4567/sstore/" + repoName)
        		.header("x-api-key",apiKey)
        		.body("{\"config\":\"STRUCTURED {} USING POSTGRES {}\"}").asString();
        printResponse(result);
        //add table 
        String tableName = "mytable" + System.currentTimeMillis();
        HttpResponse<String> addtableresult = Unirest.post("https://localhost:4567/sstore/" + repoName + "/" + tableName)
        		.header("x-api-key",apiKey)
        		.body("{\"id\":\"int\",\"firstname\":\"varchar(30)\",\"lastname\":\"varchar(30)\",\"age\":\"int\"}").asString();
        printResponse(addtableresult);
        //add data
        HttpResponse<String> adddataresult = Unirest.put("https://localhost:4567/sstore/" + repoName + "/" + tableName)
        		.header("x-api-key",apiKey)
        		.body("{\"id\":3,\"firstname\":\"jim\",\"lastname\":\"brown\",\"age\":41}").asString();
        printResponse(adddataresult);
        //get data
        HttpResponse<String> getdataresult = Unirest.get("https://localhost:4567/sstore/" + repoName + "/" + tableName + "?where=age=41")
        		.header("x-api-key",apiKey).asString();
        printResponse(getdataresult);
        
        assertEquals("[{age=41, firstname=jim, id=3, lastname=brown}]",getdataresult.getBody());
    }
    
    
    private void printResponse(HttpResponse<String> result) {
        System.out.println(result.getBody());
        System.out.println(result.getStatus() + ": " + result.getStatusText());
    }
}