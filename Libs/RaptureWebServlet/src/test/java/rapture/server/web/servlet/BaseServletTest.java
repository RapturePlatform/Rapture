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
package rapture.server.web.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ReadListener;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class BaseServletTest {

    @SuppressWarnings("rawtypes")
    @Test
    public void testGetParamsForJson() throws UnsupportedEncodingException, IOException, ServletException {
        BaseServlet b = new BaseServlet();
        Map<String, Object> props = b.getParams(new HttpServletRequest() {

            @Override
            public Object getAttribute(String name) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Enumeration<String> getAttributeNames() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getCharacterEncoding() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
                // TODO Auto-generated method stub

            }

            @Override
            public int getContentLength() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public String getContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public ServletInputStream getInputStream() throws IOException {
                String source = "{\n" +
                        "  \"id\": \"evt_1853r6HeTEznmvLm6D3dcJkQ\",\n" +
                        "  \"object\": \"event\",\n" +
                        "  \"api_version\": \"2016-03-07\",\n" +
                        "  \"created\": 1461806532,\n" +
                        "  \"data\": {\n" +
                        "    \"object\": {\n" +
                        "      \"id\": \"card_1853dOHeTEznmvLm2Ddb7dZS\",\n" +
                        "      \"object\": \"card\",\n" +
                        "      \"address_city\": null,\n" +
                        "      \"address_country\": null,\n" +
                        "      \"address_line1\": null,\n" +
                        "      \"address_line1_check\": null,\n" +
                        "      \"address_line2\": null,\n" +
                        "      \"address_state\": null,\n" +
                        "      \"address_zip\": null,\n" +
                        "      \"address_zip_check\": null,\n" +
                        "      \"brand\": \"Visa\",\n" +
                        "      \"country\": \"US\",\n" +
                        "      \"customer\": \"cus_8Ll2p6F1MgLu9j\",\n" +
                        "      \"cvc_check\": \"pass\",\n" +
                        "      \"dynamic_last4\": null,\n" +
                        "      \"exp_month\": 12,\n" +
                        "      \"exp_year\": 2017,\n" +
                        "      \"fingerprint\": \"Q8EtGncFBXfbxV9I\",\n" +
                        "      \"funding\": \"credit\",\n" +
                        "      \"last4\": \"4242\",\n" +
                        "      \"metadata\": {\n" +
                        "      },\n" +
                        "      \"name\": null,\n" +
                        "      \"tokenization_method\": null\n" +
                        "    }\n" +
                        "  },\n" +
                        "  \"livemode\": false,\n" +
                        "  \"pending_webhooks\": 2,\n" +
                        "  \"request\": \"req_8LlNQlFsRjJEwP\",\n" +
                        "  \"type\": \"customer.source.deleted\"\n" +
                        "}";
                final InputStream is = IOUtils.toInputStream(source, "UTF-8");
                return new ServletInputStream() {
                    @Override
                    public int read() throws IOException {
                        return is.read();
                    }

                    @Override
                    public boolean isFinished() {
                        // TODO Auto-generated method stub
                        return false;
                    }

                    @Override
                    public boolean isReady() {
                        // TODO Auto-generated method stub
                        return false;
                    }

                    @Override
                    public void setReadListener(ReadListener listener) {
                        // TODO Auto-generated method stub

                    }
                };
            }

            @Override
            public String getParameter(String name) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Enumeration<String> getParameterNames() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String[] getParameterValues(String name) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getProtocol() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getScheme() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getServerName() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int getServerPort() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public BufferedReader getReader() throws IOException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getRemoteAddr() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getRemoteHost() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void setAttribute(String name, Object o) {
                // TODO Auto-generated method stub

            }

            @Override
            public void removeAttribute(String name) {
                // TODO Auto-generated method stub

            }

            @Override
            public Locale getLocale() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Enumeration<Locale> getLocales() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public boolean isSecure() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public RequestDispatcher getRequestDispatcher(String path) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getRealPath(String path) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int getRemotePort() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public String getLocalName() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getLocalAddr() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int getLocalPort() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public ServletContext getServletContext() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public AsyncContext startAsync() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public boolean isAsyncStarted() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean isAsyncSupported() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public AsyncContext getAsyncContext() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public DispatcherType getDispatcherType() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getAuthType() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Cookie[] getCookies() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public long getDateHeader(String name) {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public String getHeader(String name) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int getIntHeader(String name) {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public String getMethod() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getPathInfo() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getPathTranslated() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getContextPath() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getQueryString() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getRemoteUser() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public boolean isUserInRole(String role) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public Principal getUserPrincipal() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getRequestedSessionId() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getRequestURI() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public StringBuffer getRequestURL() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getServletPath() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public HttpSession getSession(boolean create) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public HttpSession getSession() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public boolean isRequestedSessionIdValid() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean isRequestedSessionIdFromCookie() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean isRequestedSessionIdFromURL() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean isRequestedSessionIdFromUrl() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void login(String username, String password) throws ServletException {
                // TODO Auto-generated method stub

            }

            @Override
            public void logout() throws ServletException {
                // TODO Auto-generated method stub

            }

            @Override
            public Collection<Part> getParts() throws IOException, IllegalStateException, ServletException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Part getPart(String name) throws IOException, IllegalStateException, ServletException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public long getContentLengthLong() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public String changeSessionId() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public <T extends HttpUpgradeHandler> T upgrade(Class<T> httpUpgradeHandlerClass) throws IOException, ServletException {
                // TODO Auto-generated method stub
                return null;
            }

        });

        // test a few values for type and content
        assertTrue(props.get("object") instanceof String);
        assertEquals("event", props.get("object"));
        assertEquals("Visa", ((Map) ((Map) props.get("data")).get("object")).get("brand"));
        assertTrue(((Map) ((Map) props.get("data")).get("object")).get("exp_month") instanceof Integer);
        assertEquals(12, ((Map) ((Map) props.get("data")).get("object")).get("exp_month"));
        assertEquals(null, ((Map) ((Map) props.get("data")).get("object")).get("dynamic_last4"));
        assertEquals("cus_8Ll2p6F1MgLu9j", ((Map) ((Map) props.get("data")).get("object")).get("customer"));
    }
}