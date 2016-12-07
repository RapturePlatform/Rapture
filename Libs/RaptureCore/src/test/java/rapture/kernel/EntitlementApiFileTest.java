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
import rapture.common.EntitlementSet;
import rapture.common.api.DocApi;
import rapture.common.api.EntitlementApi;
import rapture.common.api.UserApi;
import rapture.common.impl.jackson.MD5Utils;

public class EntitlementApiFileTest {

    CallingContext rootContext;
    CallingContext ozzyContext;
    CallingContext tonyContext;
    CallingContext billContext;
    CallingContext geezerContext;
    String ozzy = "ozzy";
    String tony = "tony";
    String bill = "bill";
    String geezer = "geezer";
    UserApi api = null;
    EntitlementApi entApi = null;
    DocApi docApi = null;
    

    @Before
    public void setUp() throws Exception {
        rootContext = ContextFactory.getKernelUser();
        Kernel.initBootstrap();
        
        api = Kernel.getUser();
        if (!Kernel.getAdmin().doesUserExist(rootContext, ozzy)) {
            Kernel.getAdmin().addUser(rootContext, ozzy, "Ozzy Osbourne", MD5Utils.hash16(ozzy), "ozzy@sabbath.com");
        }
        if (!Kernel.getAdmin().doesUserExist(rootContext, geezer)) {
            Kernel.getAdmin().addUser(rootContext, geezer, "Geezer Butler", MD5Utils.hash16(geezer), "geezer@sabbath.com");
        }
        if (!Kernel.getAdmin().doesUserExist(rootContext, bill)) {
            Kernel.getAdmin().addUser(rootContext, bill, "Bill Ward", MD5Utils.hash16(bill), "bill@sabbath.com");
        }
        if (!Kernel.getAdmin().doesUserExist(rootContext, tony)) {
            Kernel.getAdmin().addUser(rootContext, tony, "Tony Iommi", MD5Utils.hash16(tony), "tony@sabbath.com");
        }
        
        if (!Kernel.getDoc().docRepoExists(rootContext, "//Black")) {
            Kernel.getDoc().createDocRepo(rootContext, "//Black", "NREP {} USING MEMORY {}");
        }

        ozzyContext = Kernel.getLogin().login(ozzy, ozzy, null);
        tonyContext = Kernel.getLogin().login(tony, tony, null);
        billContext = Kernel.getLogin().login(bill, bill, null);
        geezerContext = Kernel.getLogin().login(geezer, geezer, null);
        entApi = Kernel.getEntitlement();
        docApi = Kernel.getDoc();
    }

    @Test
    public void testMultipleEntitlementGroups() {
        String vocals = "Vocalist";
        String ent = EntitlementSet.Doc_putDoc.getPath().replaceAll("\\$.*", "Black/Sabbath");
        
        entApi.addEntitlement(rootContext, ent, vocals);
        entApi.addEntitlementGroup(rootContext, vocals);
        entApi.addGroupToEntitlement(rootContext, ent, vocals);
        assertTrue(entApi.getEntitlementGroup(rootContext, vocals).getUsers().isEmpty());
        assertTrue(entApi.getEntitlement(rootContext, ent).getGroups().contains(vocals));
        
        assertFalse("At this point nobody should have access", api.isPermitted(ozzyContext, "doc.putDoc", "doc://Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(ozzyContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(ozzyContext, "/data/write/$f(docURI)", "doc://Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(ozzyContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(ozzyContext, "/data/write/Black/Sabbath/Paranoid", null));
        
        assertFalse("At this point nobody should have access", api.isPermitted(tonyContext, "doc.putDoc", "doc://Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(tonyContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(tonyContext, "/data/write/$f(docURI)", "doc://Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(tonyContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(tonyContext, "/data/write/Black/Sabbath/Paranoid", null));
        
        assertFalse("At this point nobody should have access", api.isPermitted(billContext, "doc.putDoc", "doc://Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(billContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(billContext, "/data/write/$f(docURI)", "doc://Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(billContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(billContext, "/data/write/Black/Sabbath/Paranoid", null));
        
        assertFalse("At this point nobody should have access", api.isPermitted(geezerContext, "doc.putDoc", "doc://Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(geezerContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(geezerContext, "/data/write/$f(docURI)", "doc://Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(geezerContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(geezerContext, "/data/write/Black/Sabbath/Paranoid", null));
        
        entApi.addUserToEntitlementGroup(rootContext, vocals, ozzy);
        assertTrue("At this point Ozzy should have access", api.isPermitted(ozzyContext, "doc.putDoc", "doc://Black/Sabbath/Paranoid"));
        assertTrue("At this point Ozzy should have access", api.isPermitted(ozzyContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertTrue("At this point Ozzy should have access", api.isPermitted(ozzyContext, "/data/write/$f(docURI)", "doc://Black/Sabbath/Paranoid"));
        assertTrue("At this point Ozzy should have access", api.isPermitted(ozzyContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertTrue("At this point Ozzy should have access", api.isPermitted(ozzyContext, "/data/write/Black/Sabbath/Paranoid", null));
        
        assertFalse("At this point only Ozzy should have access", api.isPermitted(tonyContext, "doc.putDoc", "doc://Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(tonyContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(tonyContext, "/data/write/$f(docURI)", "doc://Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(tonyContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(tonyContext, "/data/write/Black/Sabbath/Paranoid", null));
        
        assertFalse("At this point only Ozzy should have access", api.isPermitted(billContext, "doc.putDoc", "doc://Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(billContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(billContext, "/data/write/$f(docURI)", "doc://Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(billContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(billContext, "/data/write/Black/Sabbath/Paranoid", null));
        
        assertFalse("At this point only Ozzy should have access", api.isPermitted(geezerContext, "doc.putDoc", "doc://Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(geezerContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(geezerContext, "/data/write/$f(docURI)", "doc://Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(geezerContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(geezerContext, "/data/write/Black/Sabbath/Paranoid", null));

        String guitars = "Guitars";
        entApi.addEntitlement(rootContext, ent, guitars);
        assertTrue("The entitlement groups set should still contain the group "+vocals, entApi.getEntitlement(rootContext, ent).getGroups().contains(vocals));
        entApi.addEntitlementGroup(rootContext, guitars);
        entApi.addGroupToEntitlement(rootContext, ent, guitars);
        assertTrue(entApi.getEntitlementGroup(rootContext, guitars).getUsers().isEmpty());
        assertTrue("The entitlement groups set should contain the group "+guitars, entApi.getEntitlement(rootContext, ent).getGroups().contains(guitars));
        assertTrue("The entitlement groups set should still contain the group "+vocals, entApi.getEntitlement(rootContext, ent).getGroups().contains(vocals));
        
        entApi.addUserToEntitlementGroup(rootContext, guitars, geezer);
        entApi.addUserToEntitlementGroup(rootContext, guitars, tony);
        
        assertTrue("At this point Ozzy should still have access", api.isPermitted(ozzyContext, "doc.putDoc", "doc://Black/Sabbath/Paranoid"));
        assertTrue("At this point Ozzy should still have access", api.isPermitted(ozzyContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertTrue("At this point Ozzy should still have access", api.isPermitted(ozzyContext, "/data/write/$f(docURI)", "doc://Black/Sabbath/Paranoid"));
        assertTrue("At this point Ozzy should still have access", api.isPermitted(ozzyContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertTrue("At this point Ozzy should still have access", api.isPermitted(ozzyContext, "/data/write/Black/Sabbath/Paranoid", null));
        
        assertTrue("At this point Tony should have access", api.isPermitted(tonyContext, "doc.putDoc", "doc://Black/Sabbath/Paranoid"));
        assertTrue("At this point Tony should have access", api.isPermitted(tonyContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertTrue("At this point Tony should have access", api.isPermitted(tonyContext, "/data/write/$f(docURI)", "doc://Black/Sabbath/Paranoid"));
        assertTrue("At this point Tony should have access", api.isPermitted(tonyContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertTrue("At this point Tony should have access", api.isPermitted(tonyContext, "/data/write/Black/Sabbath/Paranoid", null));
        
        assertFalse("Bill should still not have access", api.isPermitted(billContext, "doc.putDoc", "doc://Black/Sabbath/Paranoid"));
        assertFalse("Bill should still not have access", api.isPermitted(billContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertFalse("Bill should still not have access", api.isPermitted(billContext, "/data/write/$f(docURI)", "doc://Black/Sabbath/Paranoid"));
        assertFalse("Bill should still not have access", api.isPermitted(billContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertFalse("Bill should still not have access", api.isPermitted(billContext, "/data/write/Black/Sabbath/Paranoid", null));
        
        assertTrue("At this point Geezer should have access", api.isPermitted(geezerContext, "doc.putDoc", "doc://Black/Sabbath/Paranoid"));
        assertTrue("At this point Geezer should have access", api.isPermitted(geezerContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertTrue("At this point Geezer should have access", api.isPermitted(geezerContext, "/data/write/$f(docURI)", "doc://Black/Sabbath/Paranoid"));
        assertTrue("At this point Geezer should have access", api.isPermitted(geezerContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertTrue("At this point Geezer should have access", api.isPermitted(geezerContext, "/data/write/Black/Sabbath/Paranoid", null));

    }

}
