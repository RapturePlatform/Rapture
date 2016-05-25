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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import rapture.common.CallingContext;
import rapture.common.ChildrenTransferObject;
import rapture.common.ConnectionInfo;
import rapture.common.JarStorage;
import rapture.common.NodeEnum;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureIdGenConfig;
import rapture.common.RaptureScriptStorage;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.SysApi;
import rapture.common.connection.ConnectionInfoConfigurer;
import rapture.common.connection.ConnectionType;
import rapture.common.connection.ESConnectionInfoConfigurer;
import rapture.common.connection.MongoConnectionInfoConfigurer;
import rapture.common.connection.PostgresConnectionInfoConfigurer;
import rapture.common.dp.WorkOrderStorage;
import rapture.common.dp.Workflow;
import rapture.common.dp.WorkflowStorage;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentWithMeta;
import rapture.common.model.RaptureEntitlement;
import rapture.common.model.RaptureEntitlementGroup;
import rapture.common.model.RaptureUserStorage;
import rapture.config.MultiValueConfigLoader;
import rapture.kernel.sys.SysArea;
import rapture.repo.Repository;
import rapture.util.IDGenerator;

/**
 * Low level config settings manipulation
 *
 * @author amkimian
 */
public class SysApiImpl extends KernelBase implements SysApi {

    private static Map<ConnectionType, ConnectionInfoConfigurer> configurers =
            ImmutableMap.of(
                    ConnectionType.MONGODB, new MongoConnectionInfoConfigurer(),
                    ConnectionType.POSTGRES, new PostgresConnectionInfoConfigurer(),
                    ConnectionType.ES, new ESConnectionInfoConfigurer()
            );

    Repository ephemeralRepo;
    // Exposed For Testing
    Thread cacheThread = null;
    boolean allowExpireThread = true;
    Set<String> caches = new HashSet<>();
    Long timeToLive = 60 * 60 * 1000L;
    Long cacheRefresh = 5 * 60 * 1000L;

    public SysApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
        ephemeralRepo = getEphemeralRepo();
        String ttlStr = StringUtils.trimToNull(MultiValueConfigLoader.getConfig("Cache-TimeToLive"));
        if (ttlStr != null) {
            try {
                timeToLive = Long.parseLong(ttlStr);
            } catch (NumberFormatException e) {
                log.debug("Cannot parse "+ttlStr+" as long");
            }
        }

        String cacheStr = StringUtils.trimToNull(MultiValueConfigLoader.getConfig("Cache-Refresh"));
        if (cacheStr != null) {
            try {
                cacheRefresh = Long.parseLong(cacheStr);
            } catch (NumberFormatException e) {
                log.debug("Cannot parse "+cacheStr+" as long");
            }
        }
        cacheThread = new Thread(new CacheExpirationThread(ContextFactory.getKernelUser()));
        cacheThread.start();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        allowExpireThread = false;
        cacheThread.interrupt();
    }

    static Logger log = Logger.getLogger(SysApiImpl.class);

    private Repository getRepoFromArea(SysArea area) {
        switch (area) {
            case CONFIG:
                return getConfigRepo();
            default:
                log.error("This statement should not be reachable. All cases have been specified");
            case SETTINGS:
            case PRIVATE:
                return getSettingsRepo();
            case EPHEMERAL:
                return getEphemeralRepo();
            case BOOTSTRAP:
                return getBootstrapRepo();
        }
    }

    private SysArea sysAreaFromArea(String area) {
        return SysArea.valueOf(area.toUpperCase());
    }

    private String transformPathForArea(SysArea sysArea, String path) {
        switch (sysArea) {
            case PRIVATE:
                return "private/" + path;
            default:
                return path;
        }
    }

    @Override
    public String retrieveSystemConfig(CallingContext context, String area, String path) {
        // All we do here is access the config repo and return the
        // information requested
        log.info("Retrieving system config document - " + path);

        SysArea sysArea = sysAreaFromArea(area);
        return getRepoFromArea(sysArea).getDocument(transformPathForArea(sysArea, path));
    }

    @Override
    public String writeSystemConfig(CallingContext context, String area, String path, String content) {
        log.info("Writing system config document - " + path);
        SysArea sysArea = sysAreaFromArea(area);
        getRepoFromArea(sysArea).addDocument(transformPathForArea(sysArea, path), content, context.getUser(), "Saving system config", false);
        return content;
    }

    @Override
    public void removeSystemConfig(CallingContext context, String area, String path) {
        log.info("Removing system config document - " + path);
        SysArea sysArea = sysAreaFromArea(area);
        getRepoFromArea(sysArea).removeDocument(transformPathForArea(sysArea, path), context.getUser(), "Removed system document");
    }

    @Override
    public List<RaptureFolderInfo> getSystemFolders(CallingContext context, String area, String path) {
        SysArea sysArea = sysAreaFromArea(area);
        LowLevelRepoHelper helper = new LowLevelRepoHelper(getRepoFromArea(sysArea));
        return helper.getChildren(path);
    }

    private static final String root = "//";
    private static final String blobParent = Scheme.BLOB + ":" + root;
    private static final String docParent = Scheme.DOCUMENT + ":" + root;
    private static final String scriptParent = Scheme.SCRIPT + ":" + root;
    private static final String seriesParent = Scheme.SERIES + ":" + root;
    private static final String flowParent = Scheme.WORKFLOW + ":" + root;
    private static final String entParent = Scheme.ENTITLEMENT + ":" + root;
    private static final String entGrpParent = Scheme.ENTITLEMENTGROUP + ":" + root;
    // Currently can't list users
    // private static final String userParent = Scheme.USER + ":" + root;
    private static final String jobParent = Scheme.JOB + ":" + root;
    private static final String fountainParent = Scheme.IDGEN + ":" + root;

    @Override
    public List<String> getAllTopLevelRepos(CallingContext context) {
        List<String> ret = new ArrayList<>();

        // What about users? They show up under entitlements. Should we allow browsing of them?
        // Anything else?

        ret.add(blobParent);
        ret.add(docParent);
        ret.add(entParent);
        ret.add(entGrpParent);
        ret.add(flowParent);
        ret.add(fountainParent);
        ret.add(jobParent);
        ret.add(scriptParent);
        ret.add(seriesParent);
        // Currently can't list users
        // ret.add(userParent);
        return ret;
    }

    @Override
    public NodeEnum getFolderInfo(CallingContext context, String uri) {
        RaptureURI ruri = new RaptureURI(uri);
        if (!ruri.hasScheme()) return NodeEnum.NOT_VALID;
        Map<String, RaptureFolderInfo> map = findByUri(context, ruri.getParent(), 2);
        RaptureFolderInfo node = map.get(uri);
        RaptureFolderInfo folder = map.get(uri + "/");
        if (node == null) return (folder == null) ? NodeEnum.NOT_PRESENT : NodeEnum.FOLDER_ONLY;
        else return (folder == null) ? NodeEnum.OBJECT_ONLY : NodeEnum.OBJECT_AND_FOLDER;
    }

    boolean depthCheck(String str, int depth) {
        if (StringUtils.isEmpty(str)) return false;
        String[] split = str.split("/+"); // ignore empty path elements
        int i = split.length;
        if ((i > 0) && StringUtils.isEmpty(split[0])) i--;
        return ((i > 0) && (i <= depth));
    }

    private Map<String, RaptureFolderInfo> findByUri(CallingContext context, String uri, int depth) {

        Map<String, RaptureFolderInfo> children = new HashMap<>();
        if (uri.startsWith(Scheme.ENTITLEMENTGROUP.toString())) {
            List<RaptureEntitlementGroup> entGrps = Kernel.getEntitlement().getEntitlementGroups(context);
            if ((entGrps != null) && !entGrps.isEmpty()) {
                for (RaptureEntitlementGroup ent : entGrps) {
                    int urilen = uri.length();

                    String path = ent.getAddressURI().toString();
                    if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
                    if (!path.startsWith(uri) || path.equals(uri)) continue;

                    int i = path.lastIndexOf('/');
                    String name = path.substring(i + 1);

                    if (depthCheck(path.substring(urilen), depth)) children.put(path, new RaptureFolderInfo(name, true));
                    do {
                        path = path.substring(0, i + 1);
                        i = path.lastIndexOf('/', i - 1);
                        if ((i + 1) < urilen) break;
                        name = path.substring(i + 1, path.length() - 1);
                        if (depthCheck(path.substring(urilen), depth)) children.put(path, new RaptureFolderInfo(name, false));
                    } while (true);
                }
            }
        } else if (uri.startsWith(Scheme.ENTITLEMENT.toString())) {
            List<RaptureEntitlement> ents = Kernel.getEntitlement().getEntitlements(context);
            if ((ents != null) && !ents.isEmpty()) {
                for (RaptureEntitlement ent : ents) {
                    int urilen = uri.length();

                    String path = ent.getAddressURI().toString();
                    if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
                    if (!path.startsWith(uri) || path.equals(uri)) continue;

                    int i = path.lastIndexOf('/');
                    String name = path.substring(i + 1);

                    if (depthCheck(path.substring(urilen), depth)) children.put(path, new RaptureFolderInfo(name, true));
                    do {
                        path = path.substring(0, i + 1);
                        i = path.lastIndexOf('/', i - 1);
                        if ((i + 1) < urilen) break;
                        name = path.substring(i + 1, path.length() - 1);
                        if (depthCheck(path.substring(urilen), depth)) children.put(path, new RaptureFolderInfo(name, false));
                    } while (true);
                }
            }
        } else if (uri.startsWith(Scheme.BLOB.toString())) {
            children = Kernel.getBlob().listBlobsByUriPrefix(context, uri, depth);
        } else if (uri.startsWith(Scheme.DOCUMENT.toString())) {
            children = Kernel.getDoc().listDocsByUriPrefix(context, uri, depth);
        } else if (uri.startsWith(Scheme.SCRIPT.toString())) {
            children = Kernel.getScript().listScriptsByUriPrefix(context, uri, depth);
        } else if (uri.startsWith(Scheme.SERIES.toString())) {
            children = Kernel.getSeries().listSeriesByUriPrefix(context, uri, depth);
        } else if (uri.startsWith(Scheme.WORKFLOW.toString())) {
            List<Workflow> flows = Kernel.getDecision().getAllWorkflows(context);
            for (Workflow flow : flows) {
                RaptureURI furi = flow.getAddressURI();
                String str = furi.toString();
                children.put(str, new RaptureFolderInfo(str.substring(str.indexOf(':') + 3), false));
            }
        } else if (uri.startsWith(Scheme.JOB.toString())) {
            List<String> jobs = Kernel.getSchedule().getJobs(context);
            for (String job : jobs) {
                children.put(job, new RaptureFolderInfo(job.substring(job.indexOf(':') + 3), false));
            }
        } else if (uri.startsWith(Scheme.IDGEN.toString())) {
            List<RaptureIdGenConfig> ids = Kernel.getIdGen().getIdGenConfigs(context, uri);
            for (RaptureIdGenConfig id : ids) {
                RaptureURI furi = id.getAddressURI();
                String str = furi.toString();
                children.put(str, new RaptureFolderInfo(str.substring(str.indexOf(':') + 3), false));
            }
        }
        return children;
    }

    // Exposed For Testing
    private static final char SEPARATOR = '_';
    private static final String PENDING = "_pending";
    private static final String WRITTEN = "_written";

    /**
     * Get children from the current point.
     * 
     * @param context
     * @param uri
     * @param marker
     * @param recursive
     * @param maximum
     * @param timeToLive
     * @return
     */
    
    
    @Override
    public ChildrenTransferObject listByUriPrefix(CallingContext context, String uri, String marker, int depth, Long maximum, Long timeToLive) {

        ChildrenTransferObject cto = new ChildrenTransferObject();
        cto.setRemainder(new Long(0));
        Long count = 0L;
        Map<String, RaptureFolderInfo> children;

        Map<String, RaptureFolderInfo> pending = new HashMap<>();
        Map<String, RaptureFolderInfo> written = new HashMap<>();
        Map<String, RaptureFolderInfo> deleted = new HashMap<>();
        
        String locMarker = marker;
        Long locMax = maximum;
        Long locTTL = (timeToLive > 0) ? timeToLive : this.timeToLive;
        Boolean refresh = (marker == null); // if marker is null we have to

        /**
         * If there are more results than the defined maximum then store the remainder in the cache for quick access next time
         */
        
        // Do we already have a partial read? Ensure that the marker is still valid
        if (marker != null) {
            TypeReference<LinkedHashMap<String, RaptureFolderInfo>> typeRef = new TypeReference<LinkedHashMap<String, RaptureFolderInfo>>() {
            };
            String pendingContent = ephemeralRepo.getDocument(marker+PENDING);
            if (pendingContent != null)
                System.out.println("Found pending content for "+marker+PENDING);
            String writtenContent = ephemeralRepo.getDocument(marker+WRITTEN);

            if ((pendingContent != null) && (writtenContent != null)) {
                pending = JacksonUtil.objectFromJson(pendingContent, typeRef);
                written = JacksonUtil.objectFromJson(writtenContent, typeRef);
            }
            
            ephemeralRepo.removeDocument(marker+WRITTEN, ContextFactory.getKernelUser().getUser(), "Expired");
            ephemeralRepo.removeDocument(marker+PENDING, ContextFactory.getKernelUser().getUser(), "Expired");
            synchronized(caches) {
                caches.remove(locMarker+PENDING);
                caches.remove(locMarker+WRITTEN);
            }
        }
        
        if (pending.size() == 0) refresh = true;

        // If the refresh flag is set then rescan the children
        if (refresh) {
            locMarker = IDGenerator.getUUID(20) + SEPARATOR + (System.currentTimeMillis() + locTTL);
            children = findByUri(context, uri, depth);
            if (!written.isEmpty()) {
                for (String child : ImmutableSet.copyOf(written.keySet())) {
                    if (!children.containsKey(child)) deleted.put(child, written.remove(child));
                    else children.remove(child);
                }
            }
        } else {
            children = pending;
            pending = new HashMap<>();
        }

        // We may have too many. Only want the first N results
        for (String s : ImmutableSet.copyOf(children.keySet())) {
            if (locMax-- > 0) written.put(s, children.get(s));
            else {
                pending.put(s, children.remove(s));
                count++;
            }
        }

        if ((marker != null) || (count > 0)) {
            ephemeralRepo.addDocument(locMarker+PENDING, JacksonUtil.jsonFromObject(pending), ContextFactory.getKernelUser().getUser(), "Unread Data", false);
            ephemeralRepo.addDocument(locMarker+WRITTEN, JacksonUtil.jsonFromObject(written), ContextFactory.getKernelUser().getUser(), "Sent Data", false);
    
            synchronized(caches) {
                caches.add(locMarker+PENDING);
                caches.add(locMarker+WRITTEN);
            }
        } else {
            locMarker = null;
        }
        
        cto.setRemainder(count);
        cto.setParentURI(uri);
        cto.setChildren(children);
        cto.setDeleted(deleted);
        cto.setIndexMark(locMarker);
        return cto;
    }

    class CacheExpirationThread implements Runnable {
        CallingContext context = null;
        
        CacheExpirationThread(CallingContext context) {
            super();
            this.context = (context != null) ? context : ContextFactory.getKernelUser();
        }

        @Override
        public void run() {
            boolean terminate = false;
            while (allowExpireThread && !terminate) {
                long now = System.currentTimeMillis();
                List<String> toRemove = null;
                synchronized(caches) {
                    for (String uri : caches) {
                        try {
                            int start = uri.indexOf(SEPARATOR);
                            int end = uri.lastIndexOf(SEPARATOR);
                            
                            if (end > start) {
                                long time = Long.parseLong(uri.substring(start + 1, end));
                                if (time < now) {
                                    ephemeralRepo.removeDocument(uri, ContextFactory.getKernelUser().getUser(), "Cache expired");
                                    if (toRemove == null) toRemove = new ArrayList<>();
                                    toRemove.add(uri);
                                }
                            }
                        } catch (RaptureException re) {
                            log.warn("Cannot delete cache entry "+uri+": "+re.getMessage());
                        } catch (NumberFormatException nfe) {
                            log.warn("Bad URI - cannot parse " + uri);
                        }
                    }
                        
                    if (toRemove != null) {
                        caches.removeAll(toRemove);
                    }
                }
                try {
                    Thread.sleep(cacheRefresh);
                } catch (InterruptedException e) {
                    // Used for testing to fake a timeout
                    log.debug("Interrupted\n");
                }
            }
        }
    }

    public Long getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(Long timeToLive) {
        this.timeToLive = timeToLive;
    }

    public Long getCacheRefresh() {
        return cacheRefresh;
    }

    public void setCacheRefresh(Long cacheRefresh) {
        this.cacheRefresh = cacheRefresh;
    }

    @Override
    public ChildrenTransferObject getChildren(CallingContext context, String raptureURI) {
        return listByUriPrefix(context, raptureURI, null, 1, 0L, 0L);
    }

    @Override
    public ChildrenTransferObject getAllChildren(CallingContext context, String raptureURI, String marker, Long maximum) {
        return listByUriPrefix(context, raptureURI, marker, Integer.MAX_VALUE, maximum, 0L);
    }


    @Override
    public Map<String, ConnectionInfo> getConnectionInfo(CallingContext context, String connectionType) {
        return getConfigurer(connectionType).getConnectionInfo(context);
    }

    @Override
    public void deleteConnectionInfo(CallingContext context, String connectionType, ConnectionInfo connectionInfo) {
        getConfigurer(connectionType).deleteConnectionInfo(context, connectionInfo);
    }

    @Override
    public void setConnectionInfo(CallingContext context, String connectionType, String instanceName, ConnectionInfo connectionInfo) {
        getConfigurer(connectionType).setConnectionInfo(context, instanceName, connectionInfo);
    }

    private ConnectionInfoConfigurer getConfigurer(String storeType) {
        try {
            ConnectionType type = ConnectionType.valueOf(storeType.toUpperCase());
            return configurers.get(type);
        } catch (IllegalArgumentException e) {
            throw RaptureExceptionFactory.create("Unsupported store type " + storeType);
        }
    }

	@Override
	public DocumentWithMeta getSysDocumentMeta(CallingContext context,
			String raptureURI) {
		// Depending on the raptureURI scheme, call the appropriate object storage technique
		// to get at the DocumentWithMeta
		RaptureURI uri = new RaptureURI(raptureURI);
		switch(uri.getScheme()) {
		case SCRIPT:
			return RaptureScriptStorage.getDocumentWithMeta(uri);
		case WORKFLOW:
			return WorkflowStorage.getDocumentWithMeta(uri);
		case WORKORDER:
			return WorkOrderStorage.getDocumentWithMeta(uri);
		case JAR:
			return JarStorage.getDocumentWithMeta(uri);
		case USER:
			return RaptureUserStorage.getDocumentWithMeta(uri);
		default:
			log.error("Do not currently support reading metadata for " + uri);
			return null;
		}
	}
}
