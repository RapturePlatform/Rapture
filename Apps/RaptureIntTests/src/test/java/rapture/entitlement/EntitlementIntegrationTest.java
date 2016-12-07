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
package rapture.entitlement;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import rapture.common.EntitlementSet;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.client.HttpAdminApi;
import rapture.common.client.HttpBlobApi;
import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpEntitlementApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.HttpScriptApi;
import rapture.common.client.HttpSearchApi;
import rapture.common.client.HttpSeriesApi;
import rapture.common.client.HttpUserApi;
import rapture.common.impl.jackson.MD5Utils;
import rapture.common.model.RaptureEntitlement;
import rapture.common.model.RaptureEntitlementGroup;
import rapture.helper.IntegrationTestHelper;

public class EntitlementIntegrationTest {

    private IntegrationTestHelper helper;
    private HttpLoginApi raptureLogin = null;
    private HttpUserApi userApi = null;
    private HttpEntitlementApi entApi = null;
    private HttpSeriesApi seriesApi = null;
    private HttpScriptApi scriptApi = null;
    private HttpSearchApi searchApi = null;
    private HttpDocApi docApi = null;
    private HttpBlobApi blobApi = null;

    private IntegrationTestHelper helper2;
    private HttpUserApi userApi2 = null;
    private HttpDocApi docApi2 = null;
    private HttpLoginApi raptureLogin2 = null;
    private HttpAdminApi admin = null;
    private RaptureURI repoUri = null;

    private static final String user = "User";

    /**
     * Setup TestNG method to create Rapture login object and objects.
     *
     * @param RaptureURL
     *            Passed in from <env>_testng.xml suite file
     * @param RaptureUser
     *            Passed in from <env>_testng.xml suite file
     * @param RapturePassword
     *            Passed in from <env>_testng.xml suite file
     * @return none
     */
    @BeforeClass(groups = { "nightly", "search" })
    @Parameters({ "RaptureURL", "RaptureUser", "RapturePassword" })
    public void setUp(@Optional("http://localhost:8665/rapture") String url, @Optional("rapture") String username, @Optional("rapture") String password) {

        // If running from eclipse set env var -Penv=docker or use the following
        // url variable settings:
        // url="http://192.168.99.101:8665/rapture"; //docker
        // url="http://localhost:8665/rapture";

        helper = new IntegrationTestHelper(url, username, password);
        raptureLogin = helper.getRaptureLogin();
        entApi = helper.getEntApi();
        userApi = helper.getUserApi();
        seriesApi = helper.getSeriesApi();
        scriptApi = helper.getScriptApi();
        docApi = helper.getDocApi();
        blobApi = helper.getBlobApi();
        searchApi = helper.getSearchApi();
        admin = helper.getAdminApi();
        if (!admin.doesUserExist(user)) {
            admin.addUser(user, "Another User", MD5Utils.hash16(user), "user@incapture.net", "ignored");
        }
        helper2 = new IntegrationTestHelper(url, user, user);
        userApi2 = helper2.getUserApi();
        docApi2 = helper2.getDocApi();

        repoUri = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repoUri, "MONGODB"); // TODO Make this configurable
    }

    @AfterMethod(groups = { "nightly", "search" })
    public void afterMethod() {
        helper.cleanAllAssets();
        helper2.cleanAllAssets();
        if (admin.doesUserExist(user)) {
            admin.destroyUser(user);
        }
    }

    @Test
    public void testEntitlements() {

        String ent = EntitlementSet.Doc_putDoc.getPath().replaceAll("\\$.*", repoUri.getAuthority());
        String group = "Group";
        RaptureURI documentUri = RaptureURI.builder(repoUri).docPath("test/doc").build();
        String documentUriStr = documentUri.toString();

        assertTrue("Unrestricted", userApi2.isPermitted("doc.putDoc", documentUriStr));

        RaptureEntitlement entitlement = entApi.addEntitlement(ent, group);
        RaptureEntitlementGroup entitlementGroup = entApi.addEntitlementGroup(group);
        RaptureEntitlement entitlement2 = entApi.addGroupToEntitlement(ent, group);

        assertTrue(entApi.getEntitlementGroup(group).getUsers().isEmpty());
        assertTrue(entApi.getEntitlement(ent).getGroups().contains(group));

        assertFalse("Access not yet granted", userApi2.isPermitted("doc.putDoc", documentUriStr));
        entApi.addUserToEntitlementGroup(group, user);
        assertTrue("Access is now granted", userApi2.isPermitted("doc.putDoc", documentUriStr));
        entApi.removeUserFromEntitlementGroup(group, user);
        assertFalse("Access no longer granted", userApi2.isPermitted("doc.putDoc", documentUriStr));
        entApi.deleteEntitlementGroup(group);
        entApi.deleteEntitlement(ent);
        assertTrue("Unrestricted", userApi2.isPermitted("doc.putDoc", documentUriStr));
    }

}
