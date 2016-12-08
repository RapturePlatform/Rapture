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
package rapture.user;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import rapture.common.client.HttpAdminApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.HttpUserApi;
import rapture.common.client.SimpleCredentialsProvider;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.MD5Utils;
import rapture.common.model.RaptureUser;
import rapture.helper.IntegrationTestHelper;

public class UserApiTest {
    IntegrationTestHelper helper = null;
    HttpAdminApi adminApi = null;
    String raptureUrl = null;

    @BeforeClass(groups = { "user", "nightly" })
    @Parameters({ "RaptureURL", "RaptureUser", "RapturePassword" })
    public void beforeTest(@Optional("http://localhost:8665/rapture") String url, @Optional("rapture") String user, @Optional("rapture") String password) {
        helper = new IntegrationTestHelper(url, user, password);
        adminApi = helper.getAdminApi();
        raptureUrl = url;
    }

    @Test(groups = { "user", "nightly" })
    public void userGetWhoAmITest() {
        String userName = "testuser" + System.nanoTime();
        String description = "This is Test User";
        String pwd = "testpassword";
        String email = userName + "@test.com";

        adminApi.addUser(userName, description, MD5Utils.hash16(pwd), email);
        HttpLoginApi userRaptureLogin = new HttpLoginApi(raptureUrl, new SimpleCredentialsProvider(userName, pwd));
        userRaptureLogin.login();
        HttpUserApi user = new HttpUserApi(userRaptureLogin);
        RaptureUser testUser = user.getWhoAmI();
        Assert.assertTrue(testUser.getUsername().equals(userName));
    }

    @Test(groups = { "user", "nightly" })
    public void userUpdateDescriptionTest() {

        String userName = "testuser" + System.nanoTime();
        String description = "This is Test User";
        String pwd = "testpassword";
        String email = userName + "@test.com";

        adminApi.addUser(userName, description, MD5Utils.hash16(pwd), email);

        HttpLoginApi userRaptureLogin = new HttpLoginApi(raptureUrl, new SimpleCredentialsProvider(userName, pwd));
        userRaptureLogin.login();
        HttpUserApi user = new HttpUserApi(userRaptureLogin);

        RaptureUser testUser = user.updateMyDescription("new description");
        Assert.assertEquals(testUser.getDescription(), "new description");
    }

    @Test(groups = { "user", "nightly" })
    public void userChangePasswordTest() {
        String userName = "testuser" + System.nanoTime();
        String description = "This is Test User";
        String pwd = "testpassword";
        String email = userName + "@test.com";

        String newUserName = userName + System.nanoTime();

        adminApi.addUser(newUserName, description, MD5Utils.hash16(pwd), email);

        HttpLoginApi userRaptureLogin = new HttpLoginApi(raptureUrl, new SimpleCredentialsProvider(newUserName, pwd));
        userRaptureLogin.login();
        HttpUserApi user = new HttpUserApi(userRaptureLogin);

        RaptureUser testUser = user.changeMyPassword(MD5Utils.hash16(pwd), MD5Utils.hash16("new password"));
        Assert.assertEquals(testUser.getHashPassword(), MD5Utils.hash16("new password"));
        user.changeMyPassword(MD5Utils.hash16("new password"), MD5Utils.hash16(pwd));

    }

    @Test(groups = { "user", "nightly" })
    public void userDestroyTestPositive() {

        String userName = "testuser" + System.nanoTime();
        String description = "This is Test User";
        String pwd = "testpassword";
        String email = userName + "@test.com";

        String newUserName = userName + System.nanoTime();

        adminApi.addUser(newUserName, description, MD5Utils.hash16(pwd), email);
        Assert.assertTrue(adminApi.doesUserExist(newUserName), newUserName + " not created.");
        HttpLoginApi userRaptureLogin = new HttpLoginApi(raptureUrl, new SimpleCredentialsProvider(newUserName, pwd));
        userRaptureLogin.login();
        adminApi.destroyUser(newUserName);
        Assert.assertFalse(adminApi.doesUserExist(newUserName), newUserName + " not destroyed.");
    }

    @Test(groups = { "user", "nightly" }, expectedExceptions = RaptureException.class)
    public void userDestroyTestNegative() {

        String userName = "testuser" + System.nanoTime();

        String newUserName = userName + System.nanoTime();
        Assert.assertFalse(adminApi.doesUserExist(newUserName), newUserName + " not created.");
        adminApi.destroyUser(newUserName);
    }

    @Test(groups = { "user", "nightly" }, expectedExceptions = RaptureException.class)
    public void testFailChangePassword() {
        String userName = "testuser" + System.nanoTime();
        String description = "This is Test User";
        String pwd = "testpassword";
        String email = userName + "@test.com";

        adminApi.addUser(userName, description, MD5Utils.hash16(pwd), email);

        HttpLoginApi userRaptureLogin = new HttpLoginApi(raptureUrl, new SimpleCredentialsProvider(userName, pwd));
        userRaptureLogin.login();
        HttpUserApi user = new HttpUserApi(userRaptureLogin);

        user.changeMyPassword(MD5Utils.hash16(pwd + "bad"), MD5Utils.hash16("new password"));
    }

    @Test(groups = { "user", "nightly" })
    public void userLogoutTest() {

        String userName = "testuser" + System.nanoTime();
        String description = "This is Test User";
        String pwd = "testpassword";
        String email = userName + "@test.com";

        adminApi.addUser(userName, description, MD5Utils.hash16(pwd), email);

        HttpLoginApi userRaptureLogin = new HttpLoginApi(raptureUrl, new SimpleCredentialsProvider(userName, pwd));
        userRaptureLogin.login();
        HttpUserApi user = new HttpUserApi(userRaptureLogin);

        try {
            user.logoutUser();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test(groups = { "user", "nightly" })
    public void testAddApiKeys() {
        String userName = "testuser" + System.nanoTime();
        String description = "This is Test User";
        String pwd = "testpassword";
        String email = userName + "@test.com";
        int KEY_COUNT = 5;

        adminApi.addUser(userName, description, MD5Utils.hash16(pwd), email);

        HttpLoginApi userRaptureLogin = new HttpLoginApi(raptureUrl, new SimpleCredentialsProvider(userName, pwd));
        userRaptureLogin.login();

        HttpUserApi userApiLocal = new HttpUserApi(userRaptureLogin);
        Set<String> userKeySet = new HashSet<>();
        for (int i = 0; i < KEY_COUNT; i++)
            userKeySet.add("testkey" + System.nanoTime());

        for (String currKey : userKeySet)
            userApiLocal.addApiKey(currKey);

        Set<String> allUserKeys = new HashSet<>();
        for (String currKey : userApiLocal.getApiKeyPairs())
            allUserKeys.add(currKey.split("/")[0]);
        Assert.assertEquals(allUserKeys, userKeySet);
    }

    @Test(groups = { "user", "nightly" })
    public void testRevokeApiKeys() {
        String userName = "testuser" + System.nanoTime();
        String description = "This is Test User";
        String pwd = "testpassword";
        String email = userName + "@test.com";

        adminApi.addUser(userName, description, MD5Utils.hash16(pwd), email);

        HttpLoginApi userRaptureLogin = new HttpLoginApi(raptureUrl, new SimpleCredentialsProvider(userName, pwd));
        userRaptureLogin.login();

        HttpUserApi userApiLocal = new HttpUserApi(userRaptureLogin);
        Set<String> userKeySet = new HashSet<>();
        int KEY_COUNT = 5;
        for (int i = 0; i < KEY_COUNT; i++)
            userKeySet.add("testkey" + System.nanoTime());

        for (String currKey : userKeySet)
            userApiLocal.addApiKey(currKey);

        List<String> keyList = userApiLocal.getApiKeyPairs();
        int MAX_REVOKE = 3;
        for (int i = 0; i < MAX_REVOKE; i++) {
            String[] currPair = keyList.get(i).split("/");
            userApiLocal.revokeApiKey(currPair[0], currPair[1]);
            userKeySet.remove(currPair[0]);
        }

        Set<String> allUserKeys = new HashSet<>();

        for (String currKey : userApiLocal.getApiKeyPairs()) {
            allUserKeys.add(currKey.split("/")[0]);
        }
        Assert.assertEquals(allUserKeys, userKeySet);
    }
}
