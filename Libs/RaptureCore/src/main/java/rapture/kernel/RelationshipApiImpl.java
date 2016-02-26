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

import rapture.common.CallingContext;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureRelationship;
import rapture.common.RaptureRelationshipRegion;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.RelationshipApi;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.model.RelationshipRepoConfig;
import rapture.common.model.RelationshipRepoConfigStorage;
import rapture.config.ConfigLoader;
import rapture.kernel.relationship.RelationshipQueryConfig;
import rapture.relationship.RelationshipStore;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;

public class RelationshipApiImpl extends KernelBase implements RelationshipApi {

    private RelationshipRepoCache repoCache = new RelationshipRepoCache();
    private static Logger log = Logger.getLogger(RelationshipApiImpl.class);

    public RelationshipApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    @Override
    public void createRelationshipRepo(CallingContext context, String relationshipRepoURI, String config) {
        checkParameter("Repository URI", relationshipRepoURI);
        checkParameter("Config", config); //$NON-NLS-1$

        RaptureURI interimUri = new RaptureURI(relationshipRepoURI, Scheme.RELATIONSHIP);
        String authority = interimUri.getAuthority();
        if ((authority == null) || authority.isEmpty()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoAuthority")); //$NON-NLS-1$
        }
        if (interimUri.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", interimUri.toShortString())); //$NON-NLS-1$
        }
        RelationshipRepoConfig repoConfig = new RelationshipRepoConfig();
        repoConfig.setAuthority(authority);
        repoConfig.setConfig(config);
        log.info("Creating relationship repo config " + authority);
        RelationshipRepoConfigStorage.add(repoConfig, context.getUser(), "");
    }

    public Boolean updateRelationshipRepo(CallingContext context, RelationshipRepoConfig data) {
        log.info("Updating relationship repo config " + data.getAuthority());
        RelationshipRepoConfigStorage.add(data, context.getUser(), "updated");
        return true;
    }

    @Override
    public RelationshipRepoConfig getRelationshipRepoConfig(CallingContext context, String relationshipRepoURI) {
        RaptureURI authority = new RaptureURI(relationshipRepoURI, Scheme.RELATIONSHIP);
        log.info("Retrieving repo config for " + authority.getAuthority());
        if (authority.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", authority.toShortString())); //$NON-NLS-1$
        }
        return RelationshipRepoConfigStorage.readByAddress(authority);
    }

    @Override
    public void deleteRelationshipRepo(CallingContext context, String repoURI) {
        if (!ConfigLoader.getConf().RelationshipSystemActive) return;
        RaptureURI authority = new RaptureURI(repoURI, Scheme.RELATIONSHIP);
        if (authority.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", authority.toShortString())); //$NON-NLS-1$
        }
        RelationshipStore repository = repoCache.getRelationshipStore(authority);
        repository.dropStore();
        repoCache.removeEntry(authority);
        RelationshipRepoConfigStorage.deleteByAddress(authority, context.getUser(), "Remove relationship repo");
    }

    @Override
    public String createRelationship(CallingContext context, String relationshipAuthorityURI, String fromURI, String toURI, String label,
            Map<String, String> properties) {
        if (!ConfigLoader.getConf().RelationshipSystemActive) return "";
        RaptureURI parsedURI = new RaptureURI(relationshipAuthorityURI, Scheme.RELATIONSHIP);
        RaptureRelationship relationship = new RaptureRelationship();
        relationship.setAuthorityURI(relationshipAuthorityURI);
        relationship.setFromURI(new RaptureURI(fromURI));
        relationship.setToURI(new RaptureURI(toURI));
        relationship.setLabel(label);
        relationship.setProperties(properties);

        RelationshipStore relationshipStore = repoCache.getRelationshipStore(parsedURI);
        RaptureURI relationshipURI = relationshipStore.createRelationship(relationship, context.getUser());
        return relationshipURI.toString();
    }

    @Override
    public List<RaptureFolderInfo> getChildren(CallingContext context, String prefix) {
        RaptureURI internalURI = new RaptureURI(prefix, Scheme.RELATIONSHIP);
        RaptureURI otherURI = new RaptureURI(internalURI.getAuthority(), Scheme.RELATIONSHIP);
        RelationshipStore repository = repoCache.getRelationshipStore(otherURI);
        if (repository == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,  apiMessageCatalog.getMessage("NoSuchRepo", internalURI.toAuthString())); //$NON-NLS-1$
        }

        String area = internalURI.getDocPath() == null ? "" : internalURI.getDocPath();

        return repository.getSubKeys(area);
    }

    @Override
    public RaptureRelationship getRelationship(CallingContext context, String relationshipURI) {
        RaptureURI parsedURI = new RaptureURI(relationshipURI, Scheme.RELATIONSHIP);
        return repoCache.getRelationshipStore(parsedURI).getRelationship(parsedURI);
    }

    @Override
    public void deleteRelationship(CallingContext context, String relationshipURI) {
        RaptureURI parsedURI = new RaptureURI(relationshipURI, Scheme.RELATIONSHIP);
        repoCache.getRelationshipStore(parsedURI).deleteRelationship(parsedURI, context.getUser());
    }

    @Override
    public List<RaptureRelationship> getOutboundRelationships(CallingContext context, String relationshipRepoURI, String fromURI) {
        RaptureURI parsedURI = new RaptureURI(fromURI, Scheme.RELATIONSHIP);
        RaptureURI repoURI = new RaptureURI(relationshipRepoURI, Scheme.RELATIONSHIP);
        return repoCache.getRelationshipStore(repoURI).getOutboundRelationships(parsedURI);
    }

    @Override
    public List<RaptureRelationship> getInboundRelationships(CallingContext context, String relationshipRepoURI, String toURI) {
        RaptureURI parsedURI = new RaptureURI(toURI, Scheme.RELATIONSHIP);
        RaptureURI repoURI = new RaptureURI(relationshipRepoURI, Scheme.RELATIONSHIP);
        return repoCache.getRelationshipStore(repoURI).getInboundRelationships(parsedURI);
    }

    @Override
    public List<RaptureRelationship> getLabledRelationships(CallingContext context, String relationshipRepoURI, String relationshipLabel) {
        RaptureURI repoURI = new RaptureURI(relationshipRepoURI, Scheme.RELATIONSHIP);
        return repoCache.getRelationshipStore(repoURI).getLabeledRelationships(relationshipLabel);
    }

    @Override
    public List<RelationshipRepoConfig> getAllRelationshipRepoConfigs(CallingContext context) {
        return RelationshipRepoConfigStorage.readAll();
    }

    @Override
    public Boolean doesRelationshipRepoExist(CallingContext context, String repoURI) {
        return (getRelationshipRepoConfig(context, repoURI) != null);
    }

    /**
     * Get a nice view of a local region
     */
    @Override
    public RaptureRelationshipRegion getRelationshipCenteredOn(CallingContext context, String relationshipNodeURI, Map<String, String> options) {

        RaptureRelationshipRegion ret = new RaptureRelationshipRegion();
        RelationshipQueryConfig qC = new RelationshipQueryConfig(options);

        // the uri will be something like
        // relationship://sysrel/document/idp.order/xyz - i.e. it's really two
        // URI's added together
        RaptureURI outer = new RaptureURI(relationshipNodeURI, Scheme.RELATIONSHIP);
        RaptureURI relationshipURI = new RaptureURI(outer.getAuthority(), Scheme.RELATIONSHIP);
        String nodePath = outer.getDocPath();
        int firstPos = nodePath.indexOf('/');
        String scheme = nodePath.substring(0, firstPos);
        String nodeName = nodePath.substring(firstPos + 1);
        RaptureURI nodeURI = new RaptureURI(nodeName, Scheme.valueOf(scheme.toUpperCase()));
        // OK, now we loop
        ret.setCenterNode(nodeURI.toString());
        ret.setDepth(qC.getDepth());
        ret.setNodes(new ArrayList<String>());
        ret.setRelationships(new ArrayList<RaptureRelationship>());

        Set<String> seen = new HashSet<String>();
        Map<String, RaptureRelationship> rels = new HashMap<String, RaptureRelationship>();
        doRun(context, relationshipURI, rels, qC, qC.getDepth(), nodeURI, seen);
        ret.getNodes().addAll(seen);
        ret.getRelationships().addAll(rels.values());
        return ret;
    }

    private void doRun(CallingContext context, RaptureURI repo, Map<String, RaptureRelationship> rels, RelationshipQueryConfig qC, Long depth, RaptureURI node,
            Set<String> seen) {

        seen.add(node.toString());

        if (qC.getIncludeFrom()) {
            List<RaptureRelationship> outbound = getOutboundRelationships(context, repo.toString(), node.toString());
            for (RaptureRelationship r : outbound) {
                if (passes(r, qC)) {
                    rels.put(r.getURI().toString(), r);
                    if (depth > 0) {
                        RaptureURI one = r.getToURI();
                        if (!seen.contains(one.toString())) {
                            doRun(context, repo, rels, qC, depth - 1, one, seen);
                        }
                    }
                }
            }
        }
        if (qC.getIncludeTo()) {
            List<RaptureRelationship> inbound = getInboundRelationships(context, repo.toString(), node.toString());
            for (RaptureRelationship r : inbound) {
                if (passes(r, qC)) {
                    rels.put(r.getURI().toString(), r);
                    if (depth > 0) {
                        RaptureURI one = r.getFromURI();
                        if (!seen.contains(one.toString())) {
                            doRun(context, repo, rels, qC, depth - 1, one, seen);
                        }
                    }
                }
            }
        }
    }

    private boolean passes(RaptureRelationship r, RelationshipQueryConfig qC) {
        if (qC.getAgeInSeconds() == -1L) {
            return true;
        }
        Date now = new Date();
        long checkTime = now.getTime() - qC.getAgeInSeconds() * 1000;
        return r.getCreateDateUTC() > checkTime;
    }

    @Override
    public Boolean doesRelationshipExist(CallingContext context, String relationshipURI) {

        // TODO TECHDEBT This is inefficient. Can we find a better way?

        try {
            return (getRelationship(context, relationshipURI) != null);
        } catch (RaptureException e) {
            return false;
        }
    }

    @Override
    public Map<String, RaptureFolderInfo> getAllChildrenMap(CallingContext context, String prefix) {
        RaptureURI internalURI = new RaptureURI(prefix, Scheme.RELATIONSHIP);
        RaptureURI otherURI = new RaptureURI(internalURI.getAuthority(), Scheme.RELATIONSHIP);
        RelationshipStore repository = repoCache.getRelationshipStore(otherURI);

        if (repository == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,  apiMessageCatalog.getMessage("NoSuchRepo", internalURI.toAuthString())); //$NON-NLS-1$
        }

        String parentDocPath = internalURI.getDocPath() == null ? "" : internalURI.getDocPath();
        if (log.isDebugEnabled()) {
            log.debug("Loading all children from repo " + internalURI.getAuthority() + " with " + internalURI.getDocPath());
        }

        String authority = internalURI.getAuthority();
        Map<String, RaptureFolderInfo> ret = new HashMap<String, RaptureFolderInfo>();

        Stack<String> parentsStack = new Stack<String>();
        parentsStack.push(parentDocPath);

        while (!parentsStack.isEmpty()) {
            String currParentDocPath = parentsStack.pop();
            boolean top = currParentDocPath.isEmpty();
            List<RaptureFolderInfo> children = getChildren(context, currParentDocPath);

            for (RaptureFolderInfo child : children) {
                String childDocPath = currParentDocPath + (top ? "" : "/") + child.getName();
                if (child.getName().isEmpty()) continue;
                String uri = "//" + authority + "/" + childDocPath;
                ret.put(uri, child);
                if (child.isFolder()) {
                    parentsStack.push(childDocPath);
                }
            }
        }
        return ret;
    }
}
