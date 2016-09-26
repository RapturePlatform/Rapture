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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import rapture.common.client.HttpLoginApi;
import rapture.common.client.HttpUserApi;
import rapture.common.client.SimpleCredentialsProvider;
import rapture.server.rest.util.TestUtils;

public class ApiKeyTest {

    @Before
    public void setup() {
        TestUtils.setupHttpsUnirest();
    }

    @Test
    public void testCreateApiKey() {
        String host = "http://localhost:8665/rapture";
        SimpleCredentialsProvider creds = new SimpleCredentialsProvider("rapture", "rapture");
        HttpLoginApi login = new HttpLoginApi(host, creds);
        login.login();
        HttpUserApi user = new HttpUserApi(login);
        String apiKey = user.addApiKey("testApp");
        assertNotNull(apiKey);
        System.out.println(apiKey);
    }

    @Test
    public void testLoginWithApiKey() throws UnirestException {
        HttpResponse<String> result = Unirest.post("https://localhost:4567/login")
                .body("{\"appKey\":\"testApp\", \"apiKey\":\"98d73ac8-3e28-4d8b-97a9-298028ddd6cb\"}").asString();
        printResponse(result);
        if (result.getStatus() != 200) {
            fail("Failed to login");
        }
    }

    private void printResponse(HttpResponse<String> result) {
        System.out.println(result.getBody());
        System.out.println(result.getStatus() + ": " + result.getStatusText());
    }
}