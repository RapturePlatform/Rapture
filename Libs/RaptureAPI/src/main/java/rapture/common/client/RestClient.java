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
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import rapture.common.client.retry.RetryFunction;
import rapture.common.client.retry.RetryHandler;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.RestFolderInfo;
import rapture.common.model.StandardRestFileResponse;
import rapture.common.model.StandardRestFolderResponse;

/**
 * The REST client is used to interact with Rapture's REST interface
 * 
 * @author amkimian
 * 
 */
public class RestClient extends BaseRestClient {
    public RestClient(HttpLoginApi apiClient, String url) {
        super(apiClient, url + "/r/");
    }

    public List<RestFolderInfo> getFolderInfo(RestFolderInfo r) {
        return getFolderInfoFromUrl(r.getUrl());

    }

    /**
     * The REST interface works by starting at a root node, and returning back
     * the top level folders and documents
     * 
     * Then, given one of those top level folders you can continue down the
     * tree.
     * 
     * Retrieving a document can also be performed.
     * 
     */

    public List<RestFolderInfo> getFolderInfoFromUrl(String url) {
        return getRawFolderInfo(getBaseUrl() + url.substring(1));
    }

    private List<RestFolderInfo> getRawFolderInfo(final String url) {
        RetryFunction<List<RestFolderInfo>> function = new RetryFunction<List<RestFolderInfo>>() {
            @Override
            public List<RestFolderInfo> execute() throws ClientProtocolException, URISyntaxException, IOException {
                return lowLevel_getRawFolderInfo(url);
            }

            @Override
            public void recover() {
                login();
            }
        };
        return RetryHandler.execute(function, 2);
    }

    private List<RestFolderInfo> lowLevel_getRawFolderInfo(String url) throws ClientProtocolException, IOException, URISyntaxException {
        HttpClient httpclient = getUnderlyingConnection();

        HttpGet get = new HttpGet(new URI(url));
        HttpResponse response = httpclient.execute(get);
        String jsonResponse = inputStreamToString(response.getEntity().getContent());
        StandardRestFolderResponse resp = JacksonUtil.objectFromJson(jsonResponse, StandardRestFolderResponse.class);
        return resp.getContent();
    }

    private RestFolderInfo lowLevel_getTopFolderInfo(String url) throws ClientProtocolException, IOException, URISyntaxException {
        HttpClient httpclient = getUnderlyingConnection();
        HttpGet get = new HttpGet(new URI(url));
        HttpResponse response = httpclient.execute(get);
        String jsonResponse = inputStreamToString(response.getEntity().getContent());
        StandardRestFolderResponse resp = JacksonUtil.objectFromJson(jsonResponse, StandardRestFolderResponse.class);
        return resp.getCurrent();
    }

    public RestFolderInfo getTopFolderInfo(final String url) {
        RetryFunction<RestFolderInfo> function = new RetryFunction<RestFolderInfo>() {
            @Override
            public RestFolderInfo execute() throws ClientProtocolException, URISyntaxException, IOException {
                return lowLevel_getTopFolderInfo(url);
            }

            @Override
            public void recover() {
                login();
            }
        };
        return RetryHandler.execute(function, 2);
    }

    public String getRestContent(final RestFolderInfo info) {
        RetryFunction<String> function = new RetryFunction<String>() {
            @Override
            public String execute() throws ClientProtocolException, URISyntaxException, IOException {
                return lowLevel_getRestContent(info);
            }

            @Override
            public void recover() {
                login();
            }
        };
        return RetryHandler.execute(function, 2);
    }

    public String lowLevel_getRestContent(RestFolderInfo info) throws URISyntaxException, ClientProtocolException, IOException {
        HttpClient httpclient = getUnderlyingConnection();
        HttpGet get = new HttpGet(new URI(getBaseUrl() + info.getUrl().substring(1)));
        HttpResponse response = httpclient.execute(get);
        String jsonResponse = inputStreamToString(response.getEntity().getContent());
        System.out.println("Response was " + jsonResponse);
        StandardRestFileResponse resp = JacksonUtil.objectFromJson(jsonResponse, StandardRestFileResponse.class);
        return resp.getContent();
    }

    public List<RestFolderInfo> getRootFolderInfo() {
        return getRawFolderInfo(getBaseUrl());
    }
}
