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
package rapture.common.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import rapture.common.client.retry.RetryFunction;
import rapture.common.client.retry.RetryHandler;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.Rest2Response;

/**
 * The REST2 client is used to interact with Rapture's REST2 interface
 * 
 * @author amkimian
 * 
 */
public class Rest2Client extends BaseRestClient {
    public Rest2Client(HttpLoginApi apiClient, String url) {
        super(apiClient, url + "/r2/");
    }

    public Rest2Response getInformation(String url) {
        return getRawInformation(getBaseUrl() + url);
    }

    private Rest2Response getRawInformation(final String url) {
        RetryFunction<Rest2Response> function = new RetryFunction<Rest2Response>() {

            @Override
            public Rest2Response execute() throws ClientProtocolException, URISyntaxException, IOException {
                return lowLevel_getRawInformation(url);
            }

            @Override
            public void recover() {
                login();
            }
        };
        return RetryHandler.execute(function, 2);

    }

    private Rest2Response lowLevel_getRawInformation(String url) throws URISyntaxException, ClientProtocolException, IOException {
        HttpClient httpclient = getUnderlyingConnection();
        HttpGet get = new HttpGet(new URI(url));
        HttpResponse response = httpclient.execute(get);
        String jsonResponse = inputStreamToString(response.getEntity().getContent());
        Rest2Response resp = JacksonUtil.objectFromJson(jsonResponse, Rest2Response.class);
        return resp;
    }
}
