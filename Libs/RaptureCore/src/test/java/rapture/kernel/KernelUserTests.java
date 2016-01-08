/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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

import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.MD5Utils;
import rapture.common.model.RaptureUser;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests related to bad (incorrect) action on the user api
 * 
 * @author alan
 * 
 */
public class KernelUserTests {
    private CallingContext ctx = ContextFactory.getKernelUser();
    private static final String RAPTURE = "rapture";
    
    @Test
    public void changeMyPasswordTest() {
        CallingContext userCtx = Kernel.getLogin().login(RAPTURE, RAPTURE, null);
        Kernel.getUser().changeMyPassword(userCtx, MD5Utils.hash16(RAPTURE), MD5Utils.hash16("newpassword"));
        assertNotNull(userCtx);

        // Now this should login ok
        CallingContext userCtx2 = Kernel.getLogin().login(RAPTURE, "newpassword", null);
        assertNotNull(userCtx2);

        // And this shouldn't
        try {
            Kernel.getLogin().login(RAPTURE, "password", null);
            assertTrue(false);
        } catch (RaptureException e) {

        }

        // Now change it back
        Kernel.getUser().changeMyPassword(userCtx2, MD5Utils.hash16("newpassword"), MD5Utils.hash16(RAPTURE));
        Kernel.getLogin().login(RAPTURE, RAPTURE,null);

    }

    @Before
    public void init() {
        Kernel.initBootstrap();
        Kernel.INSTANCE.clearRepoCache(true);
    }

    @Test
    public void testApiUser() {
        RaptureUser usr = Kernel.getAdmin().generateApiUser(ctx, "tst", "A test api user");
        assertTrue(usr.getApiKey());
        // Login as this user

        System.out.println("Api user is " + usr.getUsername());

        Kernel.getLogin().login(usr.getUsername(), "", null);

        // And delete this user

        Kernel.getAdmin().deleteUser(ctx, usr.getUsername());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadPassword() {
        CallingContext userCtx = Kernel.getLogin().login(RAPTURE, null, null);
        RaptureUser who = Kernel.getUser().getWhoAmI(userCtx);
        assertEquals(RAPTURE, who.getUsername());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadUser() {
        CallingContext userCtx = Kernel.getLogin().login(null, "password", null);
        RaptureUser who = Kernel.getUser().getWhoAmI(userCtx);
        assertEquals(RAPTURE, who.getUsername());
    }

    @Test(expected = RaptureException.class)
    public void testFirstStageBogusUser() {
        Kernel.getLogin().getContextForUser("baduser");
    }

    public void testGetWhoAmi() {
        RaptureUser who = Kernel.getUser().getWhoAmI(ctx);
        assertEquals("raptureApi", who.getUsername());
    }

    @Test
    public void testNewUser() {
        // We can't login in as this user at the start
        try {
            Kernel.getLogin().login("fred", "testpassword", null);
            fail("should not have logged in, user was  never created");
        } catch (RaptureException e) {

        }

        Kernel.getAdmin().addUser(ctx, "fred", "A new test user", MD5Utils.hash16("testpassword"), "test@mail.com");

        Kernel.getLogin().login("fred", "testpassword", null);

        // Now delete the user

        Kernel.getAdmin().deleteUser(ctx, "fred");

        // Now we shouldn't be able to login

        try {
            Kernel.getLogin().login("fred", "testpassword", null);
            fail("should not have logged in, user was deleted");
        } catch (RaptureException e) {

        }

    }

    @Test
    public void testResetUserPassword() {
        Kernel.getAdmin().resetUserPassword(ctx, RAPTURE, MD5Utils.hash16("password2"));

        try {
            Kernel.getLogin().login(RAPTURE, RAPTURE, null);
            assertTrue(false);
        } catch (RaptureException e) {

        }

        Kernel.getLogin().login(RAPTURE, "password2", null);

        // And reset it back

        Kernel.getAdmin().resetUserPassword(ctx, RAPTURE, MD5Utils.hash16(RAPTURE));

        Kernel.getLogin().login(RAPTURE, RAPTURE, null);
    }

    @Test
    public void testCreatePasswordResetToken() {
        long expireTime = System.currentTimeMillis() + 24*3600000;
        String token = Kernel.getAdmin().createPasswordResetToken(ctx, RAPTURE);
        RaptureUser user = Kernel.getAdmin().getUser(ctx, RAPTURE);
        assertEquals(token, user.getPasswordResetToken());
        assertTrue(user.getTokenExpirationTime() >= expireTime);
        assertTrue(user.getTokenExpirationTime() <= System.currentTimeMillis() + 24*3600000);
    }

    @Test
    public void testCancelPasswordResetToken() {
        Kernel.getAdmin().createPasswordResetToken(ctx, RAPTURE);
        Kernel.getAdmin().cancelPasswordResetToken(ctx, RAPTURE);
        RaptureUser user = Kernel.getAdmin().getUser(ctx, RAPTURE);
        assertTrue(user.getTokenExpirationTime() <= System.currentTimeMillis() + 24*3600000);
    }

    @Test
    public void testSessions() {
        CallingContext userCtx = Kernel.getLogin().login(RAPTURE, RAPTURE, null);
        List<CallingContext> sessions = Kernel.getAdmin().getSessionsForUser(ctx, RAPTURE);
        boolean found = false;

        for (CallingContext s : sessions) {
            if (s.getContext().equals(userCtx.getContext())) {
                found = true;
            } else {
                System.out.println("Not a match: "+s.getContext());
            }
        }
        assertTrue("Unable to find user context "+userCtx.getContext(), found);
    }

    @Test(expected = RaptureException.class)
    public void testUnknownUser() {
        CallingContext userCtx = Kernel.getLogin().login("fred", "password", null);
        RaptureUser who = Kernel.getUser().getWhoAmI(userCtx);
        assertEquals("fred", who.getUsername());
    }

    @Test(expected = RaptureException.class)
    public void testWithInvalidPassword() {
        CallingContext userCtx = Kernel.getLogin().login(RAPTURE, "password2", null);
        RaptureUser who = Kernel.getUser().getWhoAmI(userCtx);
        assertEquals("alan", who.getUsername());
    }

    @Test
    public void testWithRealUser() {
        CallingContext userCtx = Kernel.getLogin().login(RAPTURE, RAPTURE, null);
        RaptureUser who = Kernel.getUser().getWhoAmI(userCtx);
        assertEquals(RAPTURE, who.getUsername());
    }

}
