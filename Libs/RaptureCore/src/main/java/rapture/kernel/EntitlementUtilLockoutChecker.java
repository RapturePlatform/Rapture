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

import org.apache.log4j.Logger;
import rapture.common.CallingContext;
import rapture.common.api.EntitlementApi;
import rapture.common.model.RaptureEntitlement;
import rapture.common.model.RaptureEntitlementGroup;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by zanniealvarez on 8/28/15.
 */
class EntitlementUtilLockoutChecker
{
    static Logger log = Logger.getLogger(EntitlementUtilLockoutChecker.class);
    private Set<String> sensitiveEntitlementPaths = new HashSet<>();
    private EntitlementApi api;

    private static Boolean raptureInternalsMode = false;

    EntitlementUtilLockoutChecker(EntitlementApi api) {
        this.api = api;

        sensitiveEntitlementPaths.add("/admin/ent/");
        sensitiveEntitlementPaths.add("/admin/main/");
    }

    // Sometimes the Rapture code itself will do things that temporarily create lockout.
    // Rather than bend over backwards to avoid it, let that code declare itself to be under
    // diplomatic immunity. Just be sure to set it back afterward.
    public static void setRaptureInternalsMode(Boolean raptureInternalsMode) {
        EntitlementUtilLockoutChecker.raptureInternalsMode = raptureInternalsMode;
    }

    Boolean isEntitlementSensitive(String entitlementName) {
        if (!entitlementName.startsWith("/")) {
            entitlementName = "/" + entitlementName;
        }

        if (!entitlementName.endsWith("/")) {
            entitlementName += "/";
        }

        for (String path: sensitiveEntitlementPaths) {
            if (entitlementName.startsWith(path)) {
                return true;
            }
        }

        return false;
    }

    Boolean canAddGroupToEntitlement(CallingContext context, String entitlementName, String groupName) {
        // When creating an entitlement, null can be passed for the initial group, and if so, this
        // does not create lockout even if there are no other groups assigned.
        if (giveFreePass(context) || !isEntitlementSensitive(entitlementName) || groupName == null) {
            return true;
        }

        // If this user is in the group, it's cool. No lockout.
        RaptureEntitlementGroup group = api.getEntitlementGroup(ContextFactory.getKernelUser(), groupName);
        if (group != null && group.getUsers().contains(context.getUser())) {
            return true;
        }

        // If this sensitive entitlement doesn't exist officially yet, then adding a group
        // that this user is not in will create lockout.
        RaptureEntitlement entitlement = api.getEntitlement(ContextFactory.getKernelUser(), entitlementName);
        if (entitlement == null) {
            logLockoutProhibited(entitlementName, context.getUser());
            return false;
        }

        return userHasAlternateAccess(context.getUser(), groupName, entitlement);
    }

    Boolean canRemoveGroupFromEntitlement(CallingContext context, String entitlementName, String groupName) {
        if (giveFreePass(context) || !isEntitlementSensitive(entitlementName)) {
            return true;
        }

        RaptureEntitlement entitlement = api.getEntitlement(ContextFactory.getKernelUser(), entitlementName);
        return isOnlyGroupForEntitlement(groupName, entitlement) || userHasAlternateAccess(context.getUser(), groupName, entitlement);
    }

    Boolean canDeleteEntitlementGroup(CallingContext context, String groupName) {
        if (giveFreePass(context)) {
            return true;
        }

        for (RaptureEntitlement entitlement: api.findEntitlementsByGroup(ContextFactory.getKernelUser(), groupName)) {
            if (isEntitlementSensitive(entitlement.getName()) &&
                    !isOnlyGroupForEntitlement(groupName, entitlement) &&
                    !userHasAlternateAccess(context.getUser(), groupName, entitlement)) {
                // logged from userHasAlternateAccess
                return false;
            }
        }

        return true;
    }

    Boolean canRemoveUserFromEntitlementGroup(CallingContext context, String groupName, String userName) {
        if (giveFreePass(context) || !userName.equals(context.getUser())) {
            return true;
        }

        for (RaptureEntitlement entitlement: api.findEntitlementsByGroup(ContextFactory.getKernelUser(), groupName)) {
            if (isEntitlementSensitive(entitlement.getName()) && !userHasAlternateAccess(userName, groupName, entitlement)) {
                // Logged from userHasAlternateAccess
                return false;
            }
        }

        return true;
    }

    private Boolean isOnlyGroupForEntitlement(String currentGroupName, RaptureEntitlement entitlement) {
        // If this is the only group for this entitlement, removing it will create alternate access that doesn't currently exist
        return (entitlement.getGroups().size() == 1 && entitlement.getGroups().contains(currentGroupName));
    }

    private Boolean userHasAlternateAccess(String userName, String currentGroupName, RaptureEntitlement entitlement) {
        for (String otherGroupName : entitlement.getGroups()) {
            if (!otherGroupName.equals(currentGroupName)) {
                RaptureEntitlementGroup otherGroup = api.getEntitlementGroup(ContextFactory.getKernelUser(), otherGroupName);
                if (otherGroup.getUsers().contains(userName)) {
                    return true;
                }
            }
        }

        logLockoutProhibited(entitlement.getName(), userName);
        return false;
    }

    private Boolean giveFreePass(CallingContext context) {
        return raptureInternalsMode || context.equals(ContextFactory.getKernelUser());
    }

    private void logLockoutProhibited(String entitlementName, String userName) {
        log.info("Lockout prohibited: changes to " + entitlementName +
                " must be performed by a user who will retain access after the change is complete." +
                " Attempt by user " + userName + " denied.");
    }
}
