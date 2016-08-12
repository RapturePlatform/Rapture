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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.junit.Before;
import org.junit.Test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class RestServerIntTest {

    @Before
    public void setup() throws UnirestException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        setupHttpsUnirest();
        HttpResponse<String> result = Unirest.post("https://localhost:4567/login")
                .body("{\"username\":\"rapture\", \"password\":\"rapture\"}").asString();
        printResponse(result);
        if (result.getStatus() != 200) {
            fail("Failed to login");
        }
    }

    @Test
    public void testBadLogin() throws UnirestException {
        HttpResponse<String> result = Unirest.post("https://localhost:4567/login")
                .body("{\"username\":\"rapture\", \"password\":\"sadfasdf\"}").asString();
        printResponse(result);
        assertEquals(401, result.getStatus());
    }

    @Test
    public void testDocCreateRepo() throws UnirestException {
        HttpResponse<String> result = Unirest.post("https://localhost:4567/doc/newrepo")
                .body("{\"config\":\"NREP {} USING MONGODB {prefix=\\\"newrepo\\\"}\"}").asString();
        printResponse(result);
        assertEquals(200, result.getStatus());
        result = Unirest.post("https://localhost:4567/doc/newrepo")
                .body("{\"config\":\"NREP {} USING MONGODB {prefix=\\\"newrepo\\\"}\"}").asString();
        printResponse(result);
        assertEquals(409, result.getStatus());
    }

    @Test
    public void testDocGet() throws UnirestException {
        HttpResponse<String> result = Unirest.get("https://localhost:4567/doc/config/mqSeriesConfig").asString();
        printResponse(result);
        assertTrue(result.getBody().indexOf("connectionFactory") != -1);
    }

    @Test
    public void testDocPut() throws UnirestException {
        int status = Unirest.put("https://localhost:4567/doc/config/mqSeriesConfig")
                .body("{ \"environment\" : \"test\", \"connectionFactory\" : \"CF\", \"outputQueue\" : \"RAP_TO_TB\", \"writeEnabled\" : false }").asString()
                .getStatus();
        assertEquals(200, status);
    }

    @Test
    public void testDocDelete() throws UnirestException {
        HttpResponse<String> result = Unirest.delete("https://localhost:4567/doc/config/mqSeriesConfig").asString();
        printResponse(result);
        int status = Unirest.get("https://localhost:4567/doc/config/mqSeriesConfig").asString().getStatus();
        assertEquals(404, status);
    }

    @Test
    public void testBlobCreateRepo() throws UnirestException {
        HttpResponse<String> result = Unirest.post("https://localhost:4567/blob/newrepo")
                .body("{\"config\":\"BLOB {} USING MONGODB {prefix=\\\"newrepo\\\"}\","
                        + "\"metaConfig\":\"REP {} USING MONGODB {prefix=\\\"newrepo\\\"}\"}")
                .asString();
        printResponse(result);
        assertEquals(200, result.getStatus());
        result = Unirest.post("https://localhost:4567/blob/newrepo")
                .body("{\"config\":\"BLOB {} USING MONGODB {prefix=\\\"newrepo\\\"}\","
                        + "\"metaConfig\":\"REP {} USING MONGODB {prefix=\\\"newrepo\\\"}\"}")
                .asString();
        printResponse(result);
        assertEquals(409, result.getStatus());
    }

    @Test
    public void testBlobGet() throws UnirestException {
        HttpResponse<String> result = Unirest.get("https://localhost:4567/blob/archive/FIX/2016/05/18/8668_input").asString();
        printResponse(result);
    }

    @Test
    public void testBlobPut() throws UnirestException {
        int status = Unirest.put("https://localhost:4567/blob/archive/FIX/2016/05/18/8668_input").header("Content-Type", "text/plain").body("a string value")
                .asString().getStatus();
        assertEquals(200, status);
    }

    @Test
    public void testBlobDelete() throws UnirestException {
        HttpResponse<String> result = Unirest.delete("https://localhost:4567/blob/archive/FIX/2016/05/18/8668_input").asString();
        printResponse(result);
        int status = Unirest.get("https://localhost:4567/blob/archive/FIX/2016/05/18/8668_input").asString().getStatus();
        assertEquals(404, status);
    }

    private void printResponse(HttpResponse<String> result) {
        System.out.println(result.getBody());
        System.out.println(result.getStatus() + ": " + result.getStatusText());
    }

    private void setupHttpsUnirest() {
        SSLContext sslcontext = null;
        try {
            sslcontext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            fail(e.toString());
        }

        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
        Unirest.setHttpClient(httpclient);
    }

}
