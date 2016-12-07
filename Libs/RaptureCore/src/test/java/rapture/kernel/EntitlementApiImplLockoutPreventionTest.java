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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.api.UserApi;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.MD5Utils;
import rapture.common.model.RaptureEntitlement;
import rapture.common.model.RaptureEntitlementGroup;

public class EntitlementApiImplLockoutPreventionTest {
    EntitlementApiImpl entApi = null;
    UserApi userApi = null;
    EntitlementUtilLockoutChecker lockoutChecker = null;

    CallingContext rootContext;
    CallingContext aliceContext;
    CallingContext bobContext;
    String alice = "alice";
    String bob = "bob";

    String sensitiveEntitlement = "/admin/ent";
    String unassignedSensitiveEntitlement = "/admin/main";
    String otherEntitlementWithAlice = "/amooseoncebitmysister/withbob";
    String otherEntitlementWithBob = "/amooseoncebitmysister/withalice";
    String unassignedOtherEntitlement = "/amooseoncebitmysister/elderberries";

    String aGroupAssignedToSensitiveEnt = "sensitiveEntitlementGroup";
    String emptyGroupAssignedToSensitiveEnt = "sensitiveEntitlementGroup2";
    String aliceGroup = "aliceGroup";
    String otherAliceGroup = "otherAliceGroup";
    String bobGroup = "bobGroup";
    String emptyGroup = "emptyGroup";

    @Before
    public void setUp() {
        entApi = new EntitlementApiImpl(Kernel.getKernel());
        userApi = Kernel.getUser();
        lockoutChecker = new EntitlementUtilLockoutChecker(entApi);

        rootContext = ContextFactory.getKernelUser();
        if (!Kernel.getAdmin().doesUserExist(rootContext, alice)) {
            Kernel.getAdmin().addUser(rootContext, alice, "Alice Aardvark", MD5Utils.hash16(alice), "alice@aardvark.com");
        }
        if (!Kernel.getAdmin().doesUserExist(rootContext, bob)) {
            Kernel.getAdmin().addUser(rootContext, bob, "Bob Bubble", MD5Utils.hash16(bob), "bob@bubble.com");
        }

        aliceContext = Kernel.getLogin().login(alice, alice, null);
        bobContext = Kernel.getLogin().login(bob, bob, null);

        entApi.addEntitlementGroup(rootContext, aGroupAssignedToSensitiveEnt);
        entApi.addEntitlementGroup(rootContext, emptyGroupAssignedToSensitiveEnt);
        entApi.addEntitlementGroup(rootContext, aliceGroup);
        entApi.addEntitlementGroup(rootContext, otherAliceGroup);
        entApi.addEntitlementGroup(rootContext, bobGroup);
        entApi.addEntitlementGroup(rootContext, emptyGroup);

        entApi.addUserToEntitlementGroup(rootContext, aGroupAssignedToSensitiveEnt, alice);
        entApi.addUserToEntitlementGroup(rootContext, aGroupAssignedToSensitiveEnt, bob);
        entApi.addUserToEntitlementGroup(rootContext, aliceGroup, alice);
        entApi.addUserToEntitlementGroup(rootContext, otherAliceGroup, alice);
        entApi.addUserToEntitlementGroup(rootContext, bobGroup, bob);

        entApi.addEntitlement(rootContext, sensitiveEntitlement, aGroupAssignedToSensitiveEnt);
        entApi.addEntitlement(rootContext, sensitiveEntitlement, emptyGroupAssignedToSensitiveEnt);
        entApi.addEntitlement(rootContext, otherEntitlementWithAlice, aliceGroup);
        entApi.addEntitlement(rootContext, otherEntitlementWithBob, bobGroup);
        entApi.addEntitlement(rootContext, unassignedSensitiveEntitlement, null);
        entApi.addEntitlement(rootContext, unassignedOtherEntitlement, null);
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
    public void testInitializingEntitlementWithEmptyGroup() {
        entApi.addEntitlement(aliceContext, unassignedOtherEntitlement, emptyGroup);
        assertFalse("Adding an empty group should create lockout.",
                userApi.isPermitted(aliceContext, unassignedOtherEntitlement, null));
        assertFalse("Adding an empty group should create lockout.",
                userApi.isPermitted(bobContext, unassignedOtherEntitlement, null));

        String anotherOne = unassignedOtherEntitlement + 2;
        entApi.addEntitlement(bobContext, anotherOne, null);
        assertTrue("Passing null for initialGroup does not restrict permissions.",
                userApi.isPermitted(aliceContext, anotherOne, null));
        assertTrue("Passing null for initialGroup does not restrict permissions.",
                userApi.isPermitted(bobContext, anotherOne, null));
    }

    @Test
    public void testInitializingEntitlementWithEmptyGroupSensitive() {
        try {
            entApi.addEntitlement(aliceContext, unassignedSensitiveEntitlement, emptyGroup);
            fail("Adding empty group to sensitive entitlement should have thrown exception");
        }
        catch (RaptureException e) {
            assertEquals("Expecting Lockout exception", "Lockout Prohibited", e.getMessage());

            assertTrue("Alice should not have been able to lock everyone out",
                    userApi.isPermitted(aliceContext, unassignedSensitiveEntitlement, null));
            assertTrue("Alice should not have been able to lock everyone out",
                    userApi.isPermitted(bobContext, unassignedSensitiveEntitlement, null));
        }

        entApi.addEntitlement(aliceContext, unassignedSensitiveEntitlement, null);
        assertTrue("Passing null for initialGroup does not restrict permissions.",
                userApi.isPermitted(aliceContext, unassignedSensitiveEntitlement, null));
        assertTrue("Passing null for initialGroup does not restrict permissions.",
                userApi.isPermitted(bobContext, unassignedSensitiveEntitlement, null));
    }


    @Test
    public void testAddingEmptyGroupToUnrestrictedEntitlement() {
        entApi.addGroupToEntitlement(aliceContext, unassignedOtherEntitlement, emptyGroup);
        assertEquals("Should be able to add any group to unrestricted entitlement",
                1, entApi.getEntitlement(rootContext, unassignedOtherEntitlement).getGroups().size());

        assertFalse("No one should have access to a locked out entitlement",
                userApi.isPermitted(aliceContext, unassignedOtherEntitlement, null));
        assertFalse("No one should have access to a locked out entitlement",
                userApi.isPermitted(bobContext, unassignedOtherEntitlement, null));
    }

    @Test
    public void testAddingEmptyGroupToUnrestrictedEntitlementSensitive() {
        try {
            entApi.addGroupToEntitlement(aliceContext, unassignedSensitiveEntitlement, emptyGroup);
            fail("Adding empty group to sensitive entitlement should have thrown exception");
        }
        catch (RaptureException e) {
            assertEquals("Expecting Lockout exception", "Lockout Prohibited", e.getMessage());

            assertTrue("Alice should not have been able to lock everyone out",
                    userApi.isPermitted(aliceContext, unassignedSensitiveEntitlement, null));
            assertTrue("Alice should not have been able to lock everyone out",
                    userApi.isPermitted(bobContext, unassignedSensitiveEntitlement, null));
        }
    }

    @Test
    public void testAddingNonEmptyGroupToUnrestrictedEntitlement() {
        entApi.addGroupToEntitlement(aliceContext, unassignedOtherEntitlement, bobGroup);
        assertEquals("Should be able to add any group to unrestricted entitlement",
                1, entApi.getEntitlement(rootContext, unassignedOtherEntitlement).getGroups().size());

        assertFalse("Alice should have locked herself out of this entitlement",
                userApi.isPermitted(aliceContext, unassignedOtherEntitlement, null));
        assertTrue("Alice gave Bob this entitlement, so he should have it",
                userApi.isPermitted(bobContext, unassignedOtherEntitlement, null));
    }

    @Test
    public void testAddingNonEmptyGroupToUnrestrictedEntitlementSensitive() {
        try {
            entApi.addGroupToEntitlement(aliceContext, unassignedSensitiveEntitlement, bobGroup);
            fail("Adding group not containing self to sensitive entitlement should have thrown exception");
        }
        catch (RaptureException e) {
            assertEquals("Expecting Lockout exception", "Lockout Prohibited", e.getMessage());

            assertTrue("Alice should not have been able to lock everyone out",
                    userApi.isPermitted(aliceContext, unassignedSensitiveEntitlement, null));
            assertTrue("Alice should not have been able to lock everyone out",
                    userApi.isPermitted(bobContext, unassignedSensitiveEntitlement, null));
        }

        entApi.addGroupToEntitlement(aliceContext, unassignedSensitiveEntitlement, aliceGroup);
        assertEquals("Should be able to add group containing self to sensitive entitlement",
                1, entApi.getEntitlement(rootContext, unassignedSensitiveEntitlement).getGroups().size());

        assertTrue("Alice gave herself this entitlement",
                userApi.isPermitted(aliceContext, unassignedSensitiveEntitlement, null));
        assertFalse("Bob should not have this entitlement",
                userApi.isPermitted(bobContext, unassignedSensitiveEntitlement, null));
    }

    @Test
    public void testRemovingUserFromEntitlementGroup() {
        entApi.removeUserFromEntitlementGroup(aliceContext, bobGroup, bob);
        RaptureEntitlementGroup group = entApi.getEntitlementGroup(rootContext, bobGroup);
        assertFalse("Alice removed Bob from his group", group.getUsers().contains(bob));

        entApi.removeUserFromEntitlementGroup(aliceContext, aliceGroup, alice);
        group = entApi.getEntitlementGroup(rootContext, aliceGroup);
        assertFalse("Alice removed herself from her group", group.getUsers().contains(alice));
    }

    @Test
    public void testRemovingUserFromEntitlementGroupSensitive() {
        entApi.removeUserFromEntitlementGroup(aliceContext, aGroupAssignedToSensitiveEnt, bob);
        RaptureEntitlementGroup group = entApi.getEntitlementGroup(rootContext, aGroupAssignedToSensitiveEnt);
        assertFalse("Alice removed Bob from aGroupAssignedToSensitiveEnt", group.getUsers().contains(bob));

        try {
            entApi.removeUserFromEntitlementGroup(aliceContext, aGroupAssignedToSensitiveEnt, alice);
            fail("Removing self from a group assigned to a sensitive entitlement should have thrown exception");
        }
        catch (RaptureException e) {
            assertEquals("Expecting Lockout exception", "Lockout Prohibited", e.getMessage());

            assertTrue("Alice should still have access to sensitiveEntitlement",
                    userApi.isPermitted(aliceContext, sensitiveEntitlement, null));

            group = entApi.getEntitlementGroup(rootContext, aGroupAssignedToSensitiveEnt);
            assertTrue("Alice should have failed to remove herself from aGroupAssignedToSensitiveEnt",
                    group.getUsers().contains(alice));
        }

        entApi.addGroupToEntitlement(rootContext, sensitiveEntitlement, aliceGroup);
        entApi.removeUserFromEntitlementGroup(aliceContext, aGroupAssignedToSensitiveEnt, alice);

        group = entApi.getEntitlementGroup(rootContext, aGroupAssignedToSensitiveEnt);
        assertFalse("Alice removed herself from aGroupAssignedToSensitiveEnt",
                group.getUsers().contains(alice));
    }

    @Test
    public void testRemovingGroupFromEntitlement() {
        entApi.removeGroupFromEntitlement(aliceContext, otherEntitlementWithBob, bobGroup);
        assertFalse("Should be able to remove any group from a non-sensitive entitlement",
                entApi.getEntitlement(rootContext, otherEntitlementWithBob).getGroups().contains(bobGroup));

        entApi.removeGroupFromEntitlement(aliceContext, otherEntitlementWithAlice, aliceGroup);
        assertFalse("Should be able to remove any group, including last group containing self, from a non-sensitive entitlement",
                entApi.getEntitlement(rootContext, otherEntitlementWithAlice).getGroups().contains(aliceGroup));
    }

    @Test
    public void testRemovingGroupFromEntitlementSensitive() {
        try {
            entApi.removeGroupFromEntitlement(aliceContext, sensitiveEntitlement, aGroupAssignedToSensitiveEnt);
            fail("Removing only group containing self from a sensitive entitlement should have thrown exception");
        }
        catch (RaptureException e) {
            assertEquals("Expecting Lockout exception", "Lockout Prohibited", e.getMessage());

            assertTrue("Should not be able to remove last group containing self from a sensitive entitlement",
                    entApi.getEntitlement(rootContext, sensitiveEntitlement).getGroups().contains(aGroupAssignedToSensitiveEnt));
        }

        entApi.removeGroupFromEntitlement(aliceContext, sensitiveEntitlement, emptyGroupAssignedToSensitiveEnt);
        assertFalse("Should be able to remove a group not containing self from a sensitive entitlement",
                entApi.getEntitlement(rootContext, sensitiveEntitlement).getGroups().contains(emptyGroupAssignedToSensitiveEnt));

        entApi.addGroupToEntitlement(rootContext, unassignedSensitiveEntitlement, aliceGroup);
        entApi.removeGroupFromEntitlement(aliceContext, unassignedSensitiveEntitlement, aliceGroup);
        assertFalse("Should be able to remove a group containing self from a sensitive entitlement if the entitlement has no other groups",
                entApi.getEntitlement(rootContext, unassignedSensitiveEntitlement).getGroups().contains(aliceGroup));
    }

    @Test
    public void testDeletingEntitlementGroup() {
        entApi.deleteEntitlementGroup(aliceContext, bobGroup);
        assertFalse("Should be able to delete any group not containing self",
                entApi.getEntitlementGroups(rootContext).contains(bobGroup));

        entApi.deleteEntitlementGroup(aliceContext, aliceGroup);
        assertFalse("Should be able to delete group containing self from non-sensitive entitlement",
                entApi.getEntitlementGroups(rootContext).contains(aliceGroup));
    }

    @Test
    public void testDeletingEntitlementGroupSensitive() {
        try {
            entApi.deleteEntitlementGroup(aliceContext, aGroupAssignedToSensitiveEnt);
            fail("Deleting only group containing self from a sensitive entitlement should have thrown exception");
        }
        catch (RaptureException e) {
            assertEquals("Expecting Lockout exception", "Lockout Prohibited", e.getMessage());
        }

        entApi.addGroupToEntitlement(rootContext, sensitiveEntitlement, aliceGroup);
        entApi.deleteEntitlementGroup(aliceContext, aGroupAssignedToSensitiveEnt);
        assertFalse("Should be able to delete group containing self, even assigned to sensitive entitlement, if user has alternate access",
                entApi.getEntitlementGroups(rootContext).contains(aGroupAssignedToSensitiveEnt));

        entApi.addGroupToEntitlement(rootContext, sensitiveEntitlement, bobGroup);
        entApi.deleteEntitlementGroup(aliceContext, bobGroup);
        assertFalse("Should be able to delete any group not containing self, even assigned to sensitive entitlement",
                entApi.getEntitlementGroups(rootContext).contains(bobGroup));

        entApi.addGroupToEntitlement(rootContext, unassignedSensitiveEntitlement, otherAliceGroup);
        entApi.deleteEntitlementGroup(aliceContext, otherAliceGroup);
        assertFalse("Should be able to delete a group containing self from a sensitive entitlement if the entitlement has no other groups",
                entApi.getEntitlement(rootContext, unassignedSensitiveEntitlement).getGroups().contains(otherAliceGroup));
    }
}
