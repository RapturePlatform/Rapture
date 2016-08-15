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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import rapture.common.CallingContext;
import rapture.common.ErrorWrapper;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.exception.RaptureExceptionFormatter;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.BasePayload;
import rapture.common.model.GeneralResponse;

/**
 * All Java Client connectivity to the Rapture API is ultimately derived from this class. It provides general means to connect to Rapture, execute a command and
 * return the results. Derived classes handle the specific plugins of each API.
 * 
 * When being setup, we can get given an array (list) of hostUrls. These correspond to alternate endpoints for accessing the API. We can eventually also
 * retrieve this from Rapture itself, but not for now.
 * 
 * Given this array of hostUrls this is the way we handle connections.
 * 
 * 1. If we already have a connection, use that. 2. If we don't have a connection, pick a hostUrl at random and use that to create a connection. 3. If we have
 * an error on a connection, mark the hostUrl as "down" and pick a new url at random from those that are "up". If there are no other up connections, fail. 4.
 * Periodically look at the "down" connections and mark them as "up", so that they are returned to the pool.
 */

public class BaseHttpApi {
    private static final Logger log = Logger.getLogger(BaseHttpApi.class);
    private String keyholeUrl;
    protected String currentUrl;
    protected String fullUrl;
    private URLStateManager stateManager;

    protected HttpClient httpclient;
    private CallingContext ctx;

    public BaseHttpApi(HttpLoginApi login, String keyholePart) {
        this.ctx = login.getContext();
        this.httpclient = login.httpclient;
        this.keyholeUrl = keyholePart;
        this.currentUrl = login.currentUrl;
        this.fullUrl = login.fullUrl;

        this.setStateManager(login.getStateManager());
        setup();
    }

    public BaseHttpApi(String url, String keyholePart) {
        this.keyholeUrl = keyholePart;
        this.stateManager = new URLStateManager(url);
        currentUrl = stateManager.getURL();
        if (currentUrl == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "No available Rapture end points");
        }
        setup();
    }

    private static final int MAX_RETRIES = 3;

    protected <T> T doRequest(BasePayload payload, String requestCode, TypeReference<T> reference) {
        int numTries = 0;
        while (numTries < MAX_RETRIES - 1) {
            numTries++;
            try {
                return innerDoRequest(payload, requestCode, reference);
            } catch (Exception e) {
                log.error(String.format("%s -- exception during API call, attempt %s/%s. Will retry", e, numTries, MAX_RETRIES));
            }
        }
        // if we got here, it means that we never returned above
        String errorMessage;
        Throwable exception;
        try {
            return innerDoRequest(payload, requestCode, reference);
        } catch (ClientProtocolException e) {
            exception = e;
            errorMessage = "Error connecting to server after 3 retries";
        } catch (IOException e) {
            exception = e;
            errorMessage = "Error connecting to server after 3 retries";
        }

        RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, errorMessage, exception);
        log.error(String.format("%s -- exception during API call, attempt %s/%s. Stack trace: ", exception, MAX_RETRIES, MAX_RETRIES));
        log.error(RaptureExceptionFormatter.getExceptionMessage(raptException, exception));
        throw raptException;
    }

    private <T> T innerDoRequest(BasePayload payload, String requestCode, TypeReference<T> reference) throws ClientProtocolException, IOException {
        String responseObjectJson;
        responseObjectJson = makeRequest(requestCode, JacksonUtil.jsonFromObject(payload));
        GeneralResponse resp = responseFromJson(responseObjectJson);
        if (resp == null || resp.getResponse() == null || resp.getResponse().getContent() == null) {
            return null;
        }
        if (reference != null) {
            return JacksonUtil.objectFromJson(resp.getResponse().getContent(), reference);
        } else {
            return null;
        }
    }

    public CallingContext getContext() {
        return ctx;
    }

    private String inputStreamToString(InputStream is) throws IOException {
        String line = "";
        StringBuilder total = new StringBuilder();

        // Wrap a BufferedReader around the InputStream
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));

        // Read response until the end
        while ((line = rd.readLine()) != null) {
            total.append(line);
        }

        // Return full string
        rd.close();
        return total.toString();
    }

    private byte[] getCompressedParams(String request) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzos = null;
        try {
            gzos = new GZIPOutputStream(baos);
            gzos.write(request.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return null;
        } catch (IOException e) {
            return null;
        } finally {
            if (gzos != null) {
                try {
                    gzos.close();
                } catch (IOException ignore) {

                }
            }
        }
        return baos.toByteArray();
    }

    protected String makeRequest(String fn, String request) throws ClientProtocolException, IOException {
        boolean notInError = true;
        log.trace("Entering makeRequest (" + fn + "," + request + ")");
        while (notInError) {
            try {
                HttpPost httppost = new HttpPost(fullUrl);
                MultipartEntity entity = new MultipartEntity();
                entity.addPart("FUNCTION", new StringBody(fn));
                final byte[] compressedParams = getCompressedParams(request);
                if (compressedParams != null) {
                    InputStreamBody isb = new InputStreamBody(new ByteArrayInputStream(compressedParams), "params.txt") {
                        @Override
                        public long getContentLength() {
                            return compressedParams.length;
                        }
                    };
                    entity.addPart("PARAMS", isb);
                } else {
                    entity.addPart("PARAMS", new StringBody(request));
                }
                long length = entity.getContentLength(); // will force it to
                // recalculate length

                if (log.isDebugEnabled()) log.trace("Content Length is " + length);

                httppost.setEntity(entity);
                HttpResponse response = httpclient.execute(httppost);
                String responseObjectJson = inputStreamToString(response.getEntity().getContent()).toString();
                return responseObjectJson;
            } catch (IOException e) {
                log.error(String.format("Got exception during makeRequest. Will try to recover."));
                String oldUrl = currentUrl;
                stateManager.markURLBad(currentUrl);
                currentUrl = stateManager.getURL();
                if (currentUrl == null) {
                    String message = String.format("Got exception accessing %s, and there are no available Rapture end points", oldUrl);
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, message, e);

                }
                httpclient = null;
                setup();
            }
        }
        return "";
    }

    protected GeneralResponse responseFromJson(String responseObjectJson) {
        // if the servlet throws an exception - which happens if a Reflex assertion fails - then extract the info
        if (responseObjectJson.startsWith("<!DOCTYPE html>")) {
            log.warn(responseObjectJson);
            int rootCause = responseObjectJson.indexOf("root cause");
            if (rootCause > 0) {
                int skipPre = responseObjectJson.indexOf("<pre>", rootCause);
                String cause = responseObjectJson.substring(skipPre + 5, responseObjectJson.indexOf("</pre>", skipPre)).replaceAll("&quot;", "\"");
                throw new RaptureException("", 500, cause);
            }
        }
        GeneralResponse resp = JacksonUtil.objectFromJson(responseObjectJson, GeneralResponse.class);
        if (resp.isInError()) {
            throwError(resp);
        }
        return resp;
    }

    public void setContext(CallingContext context) {
        this.ctx = context;
    }

    private static HttpClient getHttpClient() {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        schemeRegistry.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));

        PoolingClientConnectionManager cm = new PoolingClientConnectionManager(schemeRegistry);
        // Increase max total connection to 200
        cm.setMaxTotal(200);
        // Increase default max connection per route to 20
        cm.setDefaultMaxPerRoute(20);
        // Increase max connections for localhost:80 to 50
        HttpHost localhost = new HttpHost("locahost", 80);
        cm.setMaxPerRoute(new HttpRoute(localhost), 50);

        DefaultHttpClient httpClient = new DefaultHttpClient(cm);
        // Use a proxy if it is defined - we need to pass this on to the
        // HttpClient
        if (System.getProperties().containsKey("http.proxyHost")) {
            String host = System.getProperty("http.proxyHost");
            String port = System.getProperty("http.proxyPort", "8080");
            HttpHost proxy = new HttpHost(host, Integer.parseInt(port));
            httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }
        httpClient.addRequestInterceptor(new HttpRequestInterceptor() {

            @Override
            public void process(HttpRequest request, HttpContext arg1) throws HttpException, IOException {
                if (!request.containsHeader("Accept-Encoding")) {
                    request.addHeader("Accept-Encoding", "gzip");
                }
            }

        });

        httpClient.addResponseInterceptor(new HttpResponseInterceptor() {

            @Override
            public void process(final HttpResponse response, final HttpContext context) throws HttpException, IOException {
                if (log.isTraceEnabled()) {
                    log.trace("Response Headers:");
                    for (Header h : response.getAllHeaders()) {
                        log.trace(h.getName() + " : " + h.getValue());
                    }
                }
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    Header ceheader = entity.getContentEncoding();
                    if (ceheader != null) {
                        HeaderElement[] codecs = ceheader.getElements();
                        for (int i = 0; i < codecs.length; i++) {
                            if (codecs[i].getName().equalsIgnoreCase("gzip")) {
                                response.setEntity(new GzipDecompressingEntity(response.getEntity()));
                                return;
                            }
                        }
                    }
                }
            }

        });

        return httpClient;
    }

    public HttpClient getUnderlyingConnection() {
        return httpclient;
    }

    private void setup() {
        this.fullUrl = this.currentUrl + "/" + this.keyholeUrl;
        if (httpclient == null) {
            httpclient = getHttpClient();
        }
    }

    protected void throwError(GeneralResponse resp) {
        ErrorWrapper wrapper = JacksonUtil.objectFromJson(resp.getResponse().getContent(), ErrorWrapper.class);
        log.debug("Received error with message " + wrapper.getMessage());
        String exceptionMessage = wrapper.getMessage();
        if (exceptionMessage == null || exceptionMessage.length() == 0) {
            exceptionMessage = "Received an unknown error from the API";
        }
        RaptureException raptException = new RaptureException(wrapper.getId(), wrapper.getStatus(), wrapper.getStackTrace());
        throw raptException;
    }

    public URLStateManager getStateManager() {
        return stateManager;
    }

    public void setStateManager(URLStateManager stateManager) {
        this.stateManager = stateManager;
    }
}
