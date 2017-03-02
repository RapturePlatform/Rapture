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
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.junit.Before;
import org.junit.Test;

public class BaseReflexScriptPageServletTest {

    @Before
    public void setUp() throws Exception {
    }

    Map<String, Object> globalParameterMap = null;

    @SuppressWarnings("unchecked")
    @Test
    public void testDoGetParameters() {
        BaseReflexScriptPageServlet brsps = new BaseReflexScriptPageServlet() {

            private static final long serialVersionUID = 5289673110762074930L;

            @Override
            protected String getPrintableScript(HttpServletRequest req) {
                return "//foo";
            }

            @Override
            protected String getReflexScript(HttpServletRequest req) {
                return "//foo";
            }

            @Override
            void process(String script, Map<String, Object> parameterMap, HttpServletRequest req, HttpServletResponse resp) throws IOException {
                globalParameterMap = parameterMap;
            }
        };

        HttpServletRequest fakeReq = new HttpServletRequest() {

            @Override
            public Object getAttribute(String name) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Enumeration<String> getAttributeNames() {
                return Collections.enumeration(new HashSet<String>());
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
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public ServletInputStream getInputStream() throws IOException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getParameter(String name) {
                // TODO Auto-generated method stub
                return null;
            }

            Map<String, String[]> parameters = null;

            void parseParameters() {
                if (parameters == null) {
                    parameters = new HashMap<>();
                    parameters.put("Foo[0][xxx]", new String[] { "X0" });
                    parameters.put("Foo[0][yyy]", new String[] { "Y0" });
                    parameters.put("Foo[1][xxx]", new String[] { "X1" });
                    parameters.put("Foo[1][yyy]", new String[] { "Y1" });
                }
            }

            @Override
            public Enumeration<String> getParameterNames() {
                parseParameters();
                return Collections.enumeration(parameters.keySet());
            }

            @Override
            public String[] getParameterValues(String name) {
                return parameters.get(name);
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return parameters;
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
                return Collections.enumeration(new HashSet<Locale>());
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
                return Collections.enumeration(new HashSet<String>());
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                return Collections.enumeration(new HashSet<String>());
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

        };
        HttpServletResponse fakeResp = new HttpServletResponse() {

            @Override
            public String getCharacterEncoding() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getContentType() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public ServletOutputStream getOutputStream() throws IOException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public PrintWriter getWriter() throws IOException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void setCharacterEncoding(String charset) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setContentLength(int len) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setContentType(String type) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setBufferSize(int size) {
                // TODO Auto-generated method stub

            }

            @Override
            public int getBufferSize() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public void flushBuffer() throws IOException {
                // TODO Auto-generated method stub

            }

            @Override
            public void resetBuffer() {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean isCommitted() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void reset() {
                // TODO Auto-generated method stub

            }

            @Override
            public void setLocale(Locale loc) {
                // TODO Auto-generated method stub

            }

            @Override
            public Locale getLocale() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void addCookie(Cookie cookie) {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean containsHeader(String name) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public String encodeURL(String url) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String encodeRedirectURL(String url) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String encodeUrl(String url) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String encodeRedirectUrl(String url) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void sendError(int sc, String msg) throws IOException {
                // TODO Auto-generated method stub

            }

            @Override
            public void sendError(int sc) throws IOException {
                // TODO Auto-generated method stub

            }

            @Override
            public void sendRedirect(String location) throws IOException {
                // TODO Auto-generated method stub

            }

            @Override
            public void setDateHeader(String name, long date) {
                // TODO Auto-generated method stub

            }

            @Override
            public void addDateHeader(String name, long date) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setHeader(String name, String value) {
                // TODO Auto-generated method stub

            }

            @Override
            public void addHeader(String name, String value) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setIntHeader(String name, int value) {
                // TODO Auto-generated method stub

            }

            @Override
            public void addIntHeader(String name, int value) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setStatus(int sc) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setStatus(int sc, String sm) {
                // TODO Auto-generated method stub

            }

            @Override
            public int getStatus() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public String getHeader(String name) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Collection<String> getHeaders(String name) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Collection<String> getHeaderNames() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void setContentLengthLong(long length) {
                // TODO Auto-generated method stub

            }

        };

        try {
            brsps.doGet(fakeReq, fakeResp);
            Enumeration<String> e = fakeReq.getParameterNames();
            while (e.hasMoreElements()) {
                String key = e.nextElement();
                assertTrue(globalParameterMap.containsKey(key));
            }

            assertTrue(globalParameterMap.containsKey("Foo"));
            assertTrue(globalParameterMap.containsKey("Foo[0]"));
            assertTrue(globalParameterMap.containsKey("Foo[1]"));

            Object o = globalParameterMap.get("Foo[0]");
            assertTrue(o instanceof Map);
            Map<String, Object> map = (Map<String, Object>) o;
            assertEquals("X0", map.get("xxx").toString());
            assertEquals("Y0", map.get("yyy").toString());

            o = globalParameterMap.get("Foo[1]");
            assertTrue(o instanceof Map);
            map = (Map<String, Object>) o;
            assertEquals("X1", map.get("xxx").toString());
            assertEquals("Y1", map.get("yyy").toString());

            o = globalParameterMap.get("Foo");
            assertTrue(o instanceof Map);
            map = (Map<String, Object>) o;
            o = map.get("0");
            assertTrue(o instanceof Map);
            Map<String, Object> map1 = (Map<String, Object>) o;
            assertEquals("X0", map1.get("xxx").toString());
            assertEquals("Y0", map1.get("yyy").toString());

            o = map.get("1");
            assertTrue(o instanceof Map);
            map1 = (Map<String, Object>) o;
            assertEquals("X1", map1.get("xxx").toString());
            assertEquals("Y1", map1.get("yyy").toString());

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
