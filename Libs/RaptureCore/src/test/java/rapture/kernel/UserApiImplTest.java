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
package rapture.kernel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.api.DocApi;
import rapture.common.api.EntitlementApi;
import rapture.common.api.UserApi;
import rapture.common.impl.jackson.MD5Utils;

public class UserApiImplTest {

    CallingContext rootContext = null;
    CallingContext ozzyContext = null;
    UserApi api = null;
    EntitlementApi entApi = null;
    DocApi docApi = null;
    private String user = "ozzy";

    @Before
    public void setup() {
    }

    
    @Before
    public void setUp() throws Exception {
        Kernel.initBootstrap();
        rootContext = ContextFactory.getKernelUser();
        api = Kernel.getUser();
        if (!Kernel.getAdmin().doesUserExist(rootContext, user)) {
            Kernel.getAdmin().addUser(rootContext, user, "Ozzy", MD5Utils.hash16("ozzy"), "ozzy@sabbath.com", "ignored");
        }
        if (!Kernel.getDoc().docRepoExists(rootContext, "//privateAuthority")) {
            Kernel.getDoc().createDocRepo(rootContext, "//privateAuthority", "NREP {} USING MEMORY {}");
        }
        ozzyContext = Kernel.getLogin().login(user, "ozzy", null);
        entApi = Kernel.getEntitlement();
        docApi = Kernel.getDoc();
    }

    @Test
    public void testIsPermitted() {
        assertTrue(api.isPermitted(rootContext, "user.isPermitted", null));
        assertFalse(api.isPermitted(rootContext, "user.isNotPermitted", null));
        assertTrue(api.isPermitted(rootContext, "doc.getDoc", "doc://foo/bar"));
        assertTrue(api.isPermitted(rootContext, "doc.putDoc", "doc://foo/bar"));
        
        assertTrue(api.isPermitted(rootContext, "doc.getDoc", "/foo/bar"));
        assertTrue(api.isPermitted(rootContext, "doc.putDoc", "/foo/bar"));
        
        assertTrue(api.isPermitted(rootContext, "/data/read/$uri", "doc://foo/bar"));
        assertTrue(api.isPermitted(rootContext, "/data/read/$uri", "doc://foo/bar"));
        
        assertTrue(api.isPermitted(rootContext, "/data/read/$uri", "/foo/bar"));
        assertTrue(api.isPermitted(rootContext, "/data/read/$uri", "/foo/bar"));

        assertTrue(api.isPermitted(rootContext, "/data/read/foo/bar", null));
        assertTrue(api.isPermitted(rootContext, "/data/read/foo/bar", null));

        assertTrue(api.isPermitted(ozzyContext, "user.isPermitted", null));
        assertFalse(api.isPermitted(ozzyContext, "user.isNotPermitted", null));
        assertTrue(api.isPermitted(ozzyContext, "doc.getDoc", "doc://foo/bar"));
        assertTrue(api.isPermitted(ozzyContext, "doc.putDoc", "doc://foo/bar"));
    }
    // overlaps with EntitlementApiImplTest
}
