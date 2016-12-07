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

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.api.EntitlementApi;
import rapture.common.api.UserApi;
import rapture.common.impl.jackson.MD5Utils;
import rapture.common.model.RaptureEntitlement;
import rapture.common.model.RaptureEntitlementGroup;

public class EntitlementUtilLockoutCheckerTest {
    EntitlementApi entApi = null;
    UserApi userApi = null;
    EntitlementUtilLockoutChecker lockoutChecker = null;

    CallingContext rootContext;
    CallingContext aliceContext;
    CallingContext bobContext;
    String alice = "alice";
    String bob = "bob";

    String sensitiveEntitlement = "/admin/ent";
    String unassignedSenitiveEntitlement = "/admin/main";
    String otherEntitlementWithAlice = "/amooseoncebitmysister/withbob";
    String otherEntitlementWithBob = "/amooseoncebitmysister/withalice";
    String unassignedOtherEntitlement = "/amooseoncebitmysister/elderberries";

    String aGroupAssignedToSensitiveEnt = "sensitiveEntitlementGroup";
    String emptyGroupAssignedToSensitiveEnt = "sensitiveEntitlementGroup2";
    String aliceGroup = "aliceGroup";
    String bobGroup = "bobGroup";
    String emptyGroup = "emptyGroup";

    @Before
    public void setUp() {
        entApi = Kernel.getEntitlement();
        userApi = Kernel.getUser();
        lockoutChecker = new EntitlementUtilLockoutChecker(entApi);

        rootContext = ContextFactory.getKernelUser();
        if (!Kernel.getAdmin().doesUserExist(rootContext, alice)) {
            Kernel.getAdmin().addUser(rootContext, alice, "Alice Aardvark", MD5Utils.hash16(alice), "alice@aardvark.com", "ignored");
        }
        if (!Kernel.getAdmin().doesUserExist(rootContext, bob)) {
            Kernel.getAdmin().addUser(rootContext, bob, "Bob Bubble", MD5Utils.hash16(bob), "bob@bubble.com", "ignored");
        }

        aliceContext = Kernel.getLogin().login(alice, alice, null);
        bobContext = Kernel.getLogin().login(bob, bob, null);

        entApi.addEntitlementGroup(rootContext, aGroupAssignedToSensitiveEnt);
        entApi.addEntitlementGroup(rootContext, emptyGroupAssignedToSensitiveEnt);
        entApi.addEntitlementGroup(rootContext, aliceGroup);
        entApi.addEntitlementGroup(rootContext, bobGroup);
        entApi.addEntitlementGroup(rootContext, emptyGroup);

        entApi.addUserToEntitlementGroup(rootContext, aGroupAssignedToSensitiveEnt, alice);
        entApi.addUserToEntitlementGroup(rootContext, aGroupAssignedToSensitiveEnt, bob);
        entApi.addUserToEntitlementGroup(rootContext, aliceGroup, alice);
        entApi.addUserToEntitlementGroup(rootContext, bobGroup, bob);

        entApi.addEntitlement(rootContext, sensitiveEntitlement, aGroupAssignedToSensitiveEnt);
        entApi.addEntitlement(rootContext, sensitiveEntitlement, emptyGroupAssignedToSensitiveEnt);
        entApi.addEntitlement(rootContext, otherEntitlementWithAlice, aliceGroup);
        entApi.addEntitlement(rootContext, otherEntitlementWithBob, bobGroup);
        entApi.addEntitlement(rootContext, unassignedSenitiveEntitlement, null);
    }

    @After
    public void tearDown() {
        List<RaptureEntitlement> ents = entApi.getEntitlements(rootContext);
        for (RaptureEntitlement e: ents) {
            entApi.deleteEntitlement(rootContext, e.getName());
        }

        List<RaptureEntitlementGroup> groups = entApi.getEntitlementGroups(rootContext);
        for (RaptureEntitlementGroup g: groups) {
            entApi.deleteEntitlementGroup(rootContext, g.getName());
        }
    }

    @Test
    public void testIsEntitlementSensitive() {
        assertTrue("/admin/main/ should be recognized with slashes at both the beginning and end",
                lockoutChecker.isEntitlementSensitive("/admin/main/"));

        assertTrue("/admin/main should be recognized with a slash at the beginning but not at the end",
                lockoutChecker.isEntitlementSensitive("/admin/main"));

        assertTrue("admin/main/ should be recognized with a slash at the end but not at the beginning",
                lockoutChecker.isEntitlementSensitive("admin/main/"));

        assertTrue("admin/main should be recognized without slashes at the beginning or end",
                lockoutChecker.isEntitlementSensitive("admin/main"));

        assertTrue("/admin/ent should be recognized",
                lockoutChecker.isEntitlementSensitive("/admin/ent"));

        assertFalse("/bossa/nova should not be considered sensitive",
                lockoutChecker.isEntitlementSensitive("/bossa/nova"));

        assertFalse("/admin/entire should not create a false positive",
                lockoutChecker.isEntitlementSensitive("/admin/entire"));
    }

    @Test
    public void testCanAddGroupToEntitlement() {
        assertFalse("Should not be able to assign empty group to unassigned sensitive entitlement",
                lockoutChecker.canAddGroupToEntitlement(aliceContext, unassignedSenitiveEntitlement, emptyGroup));

        assertFalse("Should not be able to assign group not containing self to unassigned sensitive entitlement",
                lockoutChecker.canAddGroupToEntitlement(aliceContext, unassignedSenitiveEntitlement, bobGroup));

        assertTrue("Should be able to assign group containing self to unassigned sensitive entitlement",
                lockoutChecker.canAddGroupToEntitlement(aliceContext, unassignedSenitiveEntitlement, aliceGroup));

        assertTrue("Should be able to assign empty group to unassigned non-sensitive entitlement",
                lockoutChecker.canAddGroupToEntitlement(aliceContext, unassignedOtherEntitlement, emptyGroup));

        assertTrue("Should be able to assign group not containing self to unassigned non-sensitive entitlement",
                lockoutChecker.canAddGroupToEntitlement(aliceContext, unassignedOtherEntitlement, bobGroup));
    }

    @Test
    public void testCanRemoveGroupFromEntitlement() {
        assertTrue("Should be able to remove last group containing self from non-sensitive entitlement",
                lockoutChecker.canRemoveGroupFromEntitlement(aliceContext, otherEntitlementWithAlice, aliceGroup));

        assertTrue("Should be able to remove last group not containing self from non-sensitive entitlement",
                lockoutChecker.canRemoveGroupFromEntitlement(aliceContext, otherEntitlementWithBob, bobGroup));

        assertFalse("Should not be able to remove last group containing self from sensitive entitlement",
                lockoutChecker.canRemoveGroupFromEntitlement(aliceContext, sensitiveEntitlement, aGroupAssignedToSensitiveEnt));

        assertTrue("Should be able to remove a group not containing self from sensitive entitlement",
                lockoutChecker.canRemoveGroupFromEntitlement(aliceContext, sensitiveEntitlement, emptyGroupAssignedToSensitiveEnt));

        entApi.addGroupToEntitlement(rootContext, unassignedSenitiveEntitlement, aliceGroup);
        assertTrue("Should be able to remove a group containing self from sensitive entitlement if the entitlement has no other groups",
                lockoutChecker.canRemoveGroupFromEntitlement(aliceContext, unassignedSenitiveEntitlement, aliceGroup));
    }

    @Test
    public void testCanDeleteEntitlementGroup() {
        assertTrue("Should be able to delete empty group assigned to sensitive entitlement",
                lockoutChecker.canDeleteEntitlementGroup(aliceContext, emptyGroupAssignedToSensitiveEnt));

        assertFalse("Should not be able to delete last group containing self assigned to sensitive entitlement",
                lockoutChecker.canDeleteEntitlementGroup(aliceContext, aGroupAssignedToSensitiveEnt));

        assertTrue("Should be able to delete last group containing self assigned to non-sensitive entitlement",
                lockoutChecker.canDeleteEntitlementGroup(aliceContext, aliceGroup));

        assertTrue("Should be able to delete last group not containing self assigned to non-sensitive entitlement",
                lockoutChecker.canDeleteEntitlementGroup(aliceContext, bobGroup));

        entApi.addGroupToEntitlement(rootContext, unassignedSenitiveEntitlement, aliceGroup);
        assertTrue("Should be able to delete last group containing self assigned to sensitive entitlement if the entitlment has no other groups",
                lockoutChecker.canDeleteEntitlementGroup(aliceContext, aliceGroup));
    }

    @Test
    public void testCanRemoveUserFromEntitlementGroup() {
        assertFalse("Should not be able to remove self from last group containing self assigned to sensitive entitlement",
                lockoutChecker.canRemoveUserFromEntitlementGroup(aliceContext, aGroupAssignedToSensitiveEnt, alice));

        assertTrue("Should be able to remove self from last group containing self assigned to non-sensitive entitlement",
                lockoutChecker.canRemoveUserFromEntitlementGroup(aliceContext, aliceGroup, alice));

        assertTrue("Should be able to remove other user from group assigned to sensitive entitlement",
                lockoutChecker.canRemoveUserFromEntitlementGroup(aliceContext, aGroupAssignedToSensitiveEnt, bob));

        assertTrue("Should be able to remove other user from group assigned to non-sensitive entitlement",
                lockoutChecker.canRemoveUserFromEntitlementGroup(aliceContext, bobGroup, bob));

    }
}
