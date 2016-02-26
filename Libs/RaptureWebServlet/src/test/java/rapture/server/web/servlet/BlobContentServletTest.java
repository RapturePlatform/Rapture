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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
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
import javax.servlet.http.Part;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.BlobContainer;
import rapture.common.CallingContext;
import rapture.common.EntitlementSet;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.EntitlementApi;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.MD5Utils;
import rapture.common.model.BlobRepoConfig;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.kernel.BlobApiImpl;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class BlobContentServletTest {

    static String saveRaptureRepo;
    static String saveInitSysConfig;
    static CallingContext rootContext;
    static CallingContext userContext;
    static CallingContext user2Context;
    static String user = "user";
    static String user2 = "user2";
    
    private static BlobApiImpl blobImpl;
    private static final String BLOB_USING_MEMORY = "BLOB {} USING MEMORY {}";
    private static final String REPO_USING_MEMORY = "REP {} USING MEMORY {}";

    private static final byte[] SAMPLE_BLOB = "This is a blob".getBytes();
    
    private static final String auth = "test" + System.currentTimeMillis();
    static String blobAuthorityURI = "blob://"+auth+"/";
    static String blobName = "ItCameFromOuterSpace";
    static String blobURI = blobAuthorityURI + blobName;
    static EntitlementApi entApi = null;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        RaptureConfig.setLoadYaml(false);
        RaptureConfig config = ConfigLoader.getConf();

        saveRaptureRepo = config.RaptureRepo;
        saveInitSysConfig = config.InitSysConfig;

        System.setProperty("LOGSTASH-ISENABLED", "false");

        Kernel.INSTANCE.restart();
        Kernel.initBootstrap();
        rootContext = ContextFactory.getKernelUser();

        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");

        if (!Kernel.getAdmin().doesUserExist(rootContext, user)) {
            Kernel.getAdmin().addUser(rootContext, user, "User User", MD5Utils.hash16(user), "user@user.com");
        }
        userContext = Kernel.getLogin().login(user, user, null);
        
        if (!Kernel.getAdmin().doesUserExist(rootContext, user2)) {
            Kernel.getAdmin().addUser(rootContext, user2, "User2 User2", MD5Utils.hash16(user2), "user2@user2.com");
        }
        user2Context = Kernel.getLogin().login(user2, user2, null);
        
        blobImpl = new BlobApiImpl(Kernel.INSTANCE);
        
        if (blobImpl.blobRepoExists(rootContext, blobAuthorityURI)) 
            blobImpl.deleteBlobRepo(rootContext, blobAuthorityURI);
        blobImpl.createBlobRepo(rootContext, blobAuthorityURI, BLOB_USING_MEMORY, REPO_USING_MEMORY);

        BlobRepoConfig blobRepoConfig = blobImpl.getBlobRepoConfig(rootContext, blobAuthorityURI);
        assertNotNull(blobRepoConfig);
        assertEquals(BLOB_USING_MEMORY, blobRepoConfig.getConfig());
        assertEquals(REPO_USING_MEMORY, blobRepoConfig.getMetaConfig());
        assertEquals(auth, blobRepoConfig.getAuthority());
        
        blobImpl.putBlob(rootContext, blobURI, SAMPLE_BLOB, "application/text");
        BlobContainer blob = blobImpl.getBlob(rootContext, blobURI);
        assertArrayEquals(SAMPLE_BLOB, blob.getContent());
        entApi = Kernel.getEntitlement();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        ConfigLoader.getConf().InitSysConfig = saveInitSysConfig;
        ConfigLoader.getConf().RaptureRepo = saveRaptureRepo;
    }

    BlobContentServlet bcs;
    Cookie[] cookie;

    
    @Before
    public void setUp() throws Exception {
        resp.setContentType("");
        baos = new ByteArrayOutputStream();
        bcs = new BlobContentServlet();

        map.put("repo", auth);
        cookie = new Cookie[] { new Cookie("raptureContext", userContext.getContext()) };
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testNotLoggedI() throws IOException, ServletException {
        map.put("check", Boolean.FALSE.toString());
        map.put("entitlements", Boolean.FALSE.toString());

        bcs.init(servletConfig);
        bcs.init();
        
        bcs.doGet(req, resp);
        assertNotEquals(HttpServletResponse.SC_NOT_FOUND, resp.getStatus());
        assertEquals("application/text", resp.getContentType());
    }


    @Test
    public void testLoggedIn() throws IOException, ServletException {
        map.put("check", Boolean.TRUE.toString());
        map.put("entitlements", Boolean.FALSE.toString());

        bcs.init(servletConfig);
        bcs.init();

        bcs.doGet(req, resp);
        assertNotEquals(HttpServletResponse.SC_NOT_FOUND, resp.getStatus());
        assertEquals("application/text", resp.getContentType());
    }

    @Test
    public void testEntitlements() throws IOException, ServletException {
        map.put("check", Boolean.TRUE.toString());
        map.put("entitlements", Boolean.TRUE.toString());
        
        bcs.init(servletConfig);
        bcs.init();
        
        String allowed = "Allowed";
        entApi.addEntitlement(rootContext, "data/read/"+blobName, allowed);
        entApi.addEntitlementGroup(rootContext, allowed);
        entApi.addUserToEntitlementGroup(rootContext, allowed, userContext.getUser());

        bcs.doGet(req, resp);
        assertNotEquals(HttpServletResponse.SC_NOT_FOUND, resp.getStatus());
        assertEquals("application/text", resp.getContentType());

        cookie = new Cookie[] { new Cookie("raptureContext", user2Context.getContext()) };

        try {
            bcs.doGet(req, resp);
            fail("Not Entitled");
        } catch (RaptureException e) {
            assertEquals("User user2 not authorized for that operation", e.getMessage());
        }
        
        // So give him permission
        
        entApi.addUserToEntitlementGroup(rootContext, allowed, user2Context.getUser());
        bcs.doGet(req, resp);
        assertNotEquals(HttpServletResponse.SC_NOT_FOUND, resp.getStatus());
        assertEquals("application/text", resp.getContentType());
    }


    static Map<String, String> map = new HashMap<>();
    String requestUri = "/"+blobName;
    
    String contentType = null;
    int contentLength = 0;

    ServletConfig servletConfig = new ServletConfig() {
        @Override
        public String getServletName() {
            return "Dummy";
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public String getInitParameter(String name) {
            return map.get(name);
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return new Vector<String>(map.keySet()).elements();
        }
    };

    HttpServletRequest req = new HttpServletRequest() {

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
            return contentLength;
        }

        @Override
        public String getContentType() {
            return contentType;
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
            return cookie;
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
            return requestUri;
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
        
    };
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    HttpServletResponse resp = new HttpServletResponse() {

        @Override
        public String getCharacterEncoding() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return new ServletOutputStream() {
                @Override
                public void write(int b) throws IOException {
                    baos.write(b);
                }
            };
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
            contentLength = len;
        }

        @Override
        public void setContentType(String type) {
            contentType = type;
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

        int status = Integer.MIN_VALUE;
        
        @Override
        public void setStatus(int sc) {
            status = sc;
        }

        @Override
        public void setStatus(int sc, String sm) {
            status = sc;
        }

        @Override
        public int getStatus() {
            return status;
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
        
    };
}
