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

import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.collect.Sets;

import rapture.common.CallingContext;
import rapture.common.Messages;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.EntitlementApi;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.model.EntitlementType;
import rapture.common.model.RaptureEntitlement;
import rapture.common.model.RaptureEntitlementGroup;
import rapture.common.model.RaptureEntitlementGroupStorage;
import rapture.common.model.RaptureEntitlementStorage;
import rapture.object.storage.ObjectFilter;

public class EntitlementApiImpl extends KernelBase implements EntitlementApi {

    static Logger log = Logger.getLogger(EntitlementApiImpl.class);
    EntitlementUtilLockoutChecker lockoutChecker = new EntitlementUtilLockoutChecker(this);

    public EntitlementApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    @Override
    public RaptureEntitlement addEntitlement(CallingContext context, String entitlementName, String groupName) {
        throwLockoutExceptionAsNeeded(lockoutChecker.canAddGroupToEntitlement(context, entitlementName, groupName));

        RaptureEntitlement ent = getEntitlement(context, entitlementName);
        if (ent == null) {
            ent = new RaptureEntitlement();
            ent.setName(entitlementName);
            log.info("Created new " + ent.getAddressURI());
        }
        if (ent.getGroups() == null) {
            ent.setGroups(new HashSet<String>());
            ent.setEntType(EntitlementType.GROUPMEMBERSHIP);
        }
        if (groupName != null) {
            ent.getGroups().add(groupName);
        }
        RaptureEntitlementStorage.add(ent, context.getUser(), Messages.getString("Entitlement.CreateEnt") + entitlementName); //$NON-NLS-1$
        return ent;
    }

    @Override
    public RaptureEntitlementGroup addEntitlementGroup(CallingContext context, String groupName) {
        RaptureEntitlementGroup group = new RaptureEntitlementGroup();
        group.setUsers(new HashSet<String>());
        group.setName(groupName);
        RaptureEntitlementGroupStorage.add(group, context.getUser(), Messages.getString("Entitlement.CreateEntGroup") + groupName); //$NON-NLS-1$
        return group;
    }

    @Override
    public RaptureEntitlement addGroupToEntitlement(CallingContext context, String entitlementName, String groupName) {
        throwLockoutExceptionAsNeeded(lockoutChecker.canAddGroupToEntitlement(context, entitlementName, groupName));

        RaptureEntitlement ent = RaptureEntitlementStorage.readByFields(entitlementName);
        if (ent != null) {
            ent.getGroups().add(groupName);
            RaptureEntitlementStorage.add(ent, context.getUser(), Messages.getString("Entitlement.AddGroup")); //$NON-NLS-1$
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("Entitlement.NotExist")); //$NON-NLS-1$
        }
        return ent;
    }

    @Override
    public RaptureEntitlementGroup addUserToEntitlementGroup(CallingContext context, String groupName, String user) {
        RaptureEntitlementGroup group = RaptureEntitlementGroupStorage.readByFields(groupName);
        if (group != null) {
            group.getUsers().add(user);
            RaptureEntitlementGroupStorage.add(group, context.getUser(), Messages.getString("Entitlement.AddUserToGroup")); //$NON-NLS-1$
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("Entitlement.NotExistEntGroup")); //$NON-NLS-1$
        }
        return group;
    }

    @Override
    public void deleteEntitlement(CallingContext context, String entitlementName) {
        // If we delete an entitlement we just drop it - it doesn't depend on
        // anything (it's at the top)
        RaptureEntitlementStorage.deleteByFields(entitlementName, context.getUser(), Messages.getString("Entitlement.RemoveEnt"));
    }

    @Override
    public void deleteEntitlementGroup(CallingContext context, String groupName) {
        throwLockoutExceptionAsNeeded(lockoutChecker.canDeleteEntitlementGroup(context, groupName));

        // If we delete an entitlement group we need to remove that group from
        // all entitlements
        List<RaptureEntitlement> ents = getEntitlements(context);
        for (RaptureEntitlement e : ents) {
            if (e.getGroups().contains(groupName)) {
                e.getGroups().remove(groupName);
                RaptureEntitlementStorage.add(e, context.getUser(),
                        Messages.getString("Entitlement.Group") + groupName + Messages.getString("Entitlement.Deleted")); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        RaptureEntitlementGroupStorage.deleteByFields(groupName, context.getUser(), Messages.getString("Entitlement.RemovingGroup"));
    }

    @Override
    public List<RaptureEntitlementGroup> getEntitlementGroups(CallingContext context) {
        return RaptureEntitlementGroupStorage.readAll();
    }

    @Override
    public List<RaptureEntitlement> getEntitlements(CallingContext context) {
        return RaptureEntitlementStorage.readAll();
    }

    @Override
    public RaptureEntitlement removeGroupFromEntitlement(CallingContext context, String entitlementName, String groupName) {
        throwLockoutExceptionAsNeeded(lockoutChecker.canRemoveGroupFromEntitlement(context, entitlementName, groupName));

        RaptureEntitlement ent = RaptureEntitlementStorage.readByFields(entitlementName);
        if (ent != null) {
            if (ent.getGroups().contains(groupName)) {
                ent.getGroups().remove(groupName);
                RaptureEntitlementStorage.add(ent, context.getUser(), Messages.getString("Entitlement.RemoveGroupEntitle")); //$NON-NLS-1$
            }
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("Entitlement.NoExistEntitle")); //$NON-NLS-1$
        }
        return ent;

    }

    @Override
    public RaptureEntitlementGroup removeUserFromEntitlementGroup(CallingContext context, String groupName, String user) {
        throwLockoutExceptionAsNeeded(lockoutChecker.canRemoveUserFromEntitlementGroup(context, groupName, user));

        RaptureEntitlementGroup entGroup = RaptureEntitlementGroupStorage.readByFields(groupName);
        if (entGroup != null) {
            if (entGroup.getUsers().contains(user)) {
                entGroup.getUsers().remove(user);
                RaptureEntitlementGroupStorage.add(entGroup, context.getUser(), Messages.getString("Entitlement.RemoveUserGroup")); //$NON-NLS-1$
            }
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("Entitlement.NoExistGroup")); //$NON-NLS-1$
        }
        return entGroup;
    }

    @Override
    public RaptureEntitlement getEntitlement(CallingContext context, String entitlementName) {
        return RaptureEntitlementStorage.readByFields(entitlementName);
    }

    @Override
    public RaptureEntitlementGroup getEntitlementGroup(CallingContext context, String groupName) {
        return RaptureEntitlementGroupStorage.readByFields(groupName);
    }

    @Override
    public RaptureEntitlement getEntitlementByAddress(CallingContext context, String entitlementURI) {
        RaptureURI uri = new RaptureURI(entitlementURI, Scheme.ENTITLEMENT);
        return RaptureEntitlementStorage.readByAddress(uri);
    }

    @Override
    public RaptureEntitlementGroup getEntitlementGroupByAddress(CallingContext context, String groupURI) {
        RaptureURI uri = new RaptureURI(groupURI, Scheme.ENTITLEMENTGROUP);
        return RaptureEntitlementGroupStorage.readByAddress(uri);
    }

    @Override
    public List<RaptureEntitlement> getEntitlementsForUser(CallingContext context, String username) {
        UserEntitlementFilter filter = new UserEntitlementFilter(context, username);
        return RaptureEntitlementStorage.filterAll(filter);
    }

    @Override
    public List<RaptureEntitlement> getEntitlementsForGroup(CallingContext context, String groupname) {
        GroupEntitlementFilter filter = new GroupEntitlementFilter(groupname);
        return RaptureEntitlementStorage.filterAll(filter);
    }

    @Override
    public List<RaptureEntitlement> getEntitlementsForSelf(CallingContext context) {
        UserEntitlementFilter filter = new UserEntitlementFilter(context, context.getUser());
        return RaptureEntitlementStorage.filterAll(filter);
    }

    private void throwLockoutExceptionAsNeeded(Boolean operationAllowed) {
        if (!operationAllowed) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("Entitlement.LockoutProhibited")); //$NON-NLS-1$
        }
    }

    private class UserEntitlementFilter implements ObjectFilter<RaptureEntitlement> {
        Set<String> groupnames = Sets.newHashSet();

        private UserEntitlementFilter(CallingContext context, String username) {
            groupnames.addAll(Kernel.getAdmin().getTrusted().findGroupNamesByUser(context, username));
        }

        @Override
        public boolean shouldInclude(RaptureEntitlement obj) {
            for (String groupname : obj.getGroups()) {
                if (groupnames.contains(groupname)) {
                    return true;
                }
            }
            return false;
        }

    }

    private class GroupEntitlementFilter implements ObjectFilter<RaptureEntitlement> {
        private final String groupname;

        private GroupEntitlementFilter(String groupname) {
            this.groupname = groupname;
        }

        @Override
        public boolean shouldInclude(RaptureEntitlement obj) {
            for (String g : obj.getGroups()) {
                if (groupname.equals(g)) {
                    return true;
                }
            }
            return false;
        }
    }
}

