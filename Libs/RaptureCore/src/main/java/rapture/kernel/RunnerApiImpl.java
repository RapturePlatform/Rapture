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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.LockHandle;
import rapture.common.RaptureApplicationDefinition;
import rapture.common.RaptureApplicationDefinitionStorage;
import rapture.common.RaptureApplicationInstance;
import rapture.common.RaptureApplicationInstancePathBuilder;
import rapture.common.RaptureApplicationInstanceStorage;
import rapture.common.RaptureInstanceCapabilities;
import rapture.common.RaptureInstanceCapabilitiesStorage;
import rapture.common.RaptureLibraryDefinition;
import rapture.common.RaptureLibraryDefinitionStorage;
import rapture.common.RapturePipelineTask;
import rapture.common.RaptureRunnerConfig;
import rapture.common.RaptureRunnerConfigStorage;
import rapture.common.RaptureRunnerInstanceStatus;
import rapture.common.RaptureRunnerStatus;
import rapture.common.RaptureRunnerStatusStorage;
import rapture.common.RaptureServerGroup;
import rapture.common.RaptureServerGroupStorage;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.RunnerApi;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.JsonContent;
import rapture.common.mime.custom.MimeRunnerNotification;
import rapture.common.model.RaptureApplicationStatus;
import rapture.common.model.RaptureApplicationStatusStep;
import rapture.common.model.RaptureApplicationStatusStorage;
import rapture.common.pipeline.PipelineConstants;
import rapture.kernel.pipeline.TaskSubmitter;
import rapture.repo.RepoVisitor;
import rapture.util.IDGenerator;

/**
 * Runner is all about RaptureRunner config
 *
 * @author amkimian
 */
public class RunnerApiImpl extends KernelBase implements RunnerApi {
    public static final String RAPTUREAPPLICATIONSTATUS = "RaptureApplicationStatus";
    private static final String RUNNER_STATUS_LOCK = "runnerStatus-%s";
    private static Logger logger = Logger.getLogger(RunnerApiImpl.class);

    public RunnerApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    @Override
    public RaptureServerGroup createServerGroup(CallingContext context, String name, String description) {
        RaptureServerGroup newGroup = new RaptureServerGroup();
        newGroup.setLibraries(new HashSet<String>());
        newGroup.setName(name);
        newGroup.setDescription(description);
        Set<String> inclusions = new HashSet<String>();
        inclusions.add("*");
        newGroup.setInclusions(inclusions);
        newGroup.setExclusions(new HashSet<String>());
        RaptureServerGroupStorage.add(newGroup, context.getUser(), "Create server group");
        return newGroup;
    }

    @Override
    public void deleteServerGroup(CallingContext context, String name) {
        RaptureServerGroupStorage.deleteByFields(name, context.getUser(), "Remove server group");
    }

    @Override
    public RaptureServerGroup addGroupInclusion(CallingContext context, String name, String inclusion) {
        RaptureServerGroup group = getServerGroup(context, name);
        if (group.getInclusions().contains("*")) {
            group.getInclusions().remove("*");
        }
        group.getInclusions().add(inclusion);
        RaptureServerGroupStorage.add(group, context.getUser(), "Add group inclusion");
        return group;
    }

    @Override
    public RaptureServerGroup removeGroupInclusion(CallingContext context, String name, String inclusion) {
        RaptureServerGroup group = getServerGroup(context, name);
        Set<String> inclusions = group.getInclusions();
        inclusions.remove(inclusion);
        if (inclusions.isEmpty()) {
            inclusions.add("*");
        }
        RaptureServerGroupStorage.add(group, context.getUser(), "Remove group inclusion");
        return group;
    }

    @Override
    public RaptureServerGroup addGroupExclusion(CallingContext context, String name, String exclusion) {
        RaptureServerGroup group = getServerGroup(context, name);
        group.getExclusions().add(exclusion);
        RaptureServerGroupStorage.add(group, context.getUser(), "Add group exclusion");
        return group;
    }

    @Override
    public RaptureServerGroup removeGroupExclusion(CallingContext context, String name, String exclusion) {
        RaptureServerGroup group = getServerGroup(context, name);
        group.getExclusions().remove(exclusion);
        RaptureServerGroupStorage.add(group, context.getUser(), "Remove group exclusion");
        return group;
    }

    @Override
    public RaptureServerGroup addLibraryToGroup(CallingContext context, String serverGroup, String libraryName) {
        RaptureServerGroup group = getServerGroup(context, serverGroup);
        group.getLibraries().add(libraryName);
        RaptureServerGroupStorage.add(group, context.getUser(), "Add library to group");
        return group;
    }

    @Override
    public RaptureServerGroup removeLibraryFromGroup(CallingContext context, String serverGroup, String libraryName) {
        RaptureServerGroup group = getServerGroup(context, serverGroup);
        if (group.getLibraries().contains(libraryName)) {
            group.getLibraries().remove(libraryName);
            RaptureServerGroupStorage.add(group, context.getUser(), "Remove library from group");
        }
        return group;
    }

    @Override
    public RaptureServerGroup removeGroupEntry(CallingContext context, String name, String entry) {
        RaptureServerGroup group = getServerGroup(context, name);
        group.getInclusions().remove(entry);
        group.getExclusions().remove(entry);
        RaptureServerGroupStorage.add(group, context.getUser(), "Remove group entry");
        return group;
    }

    @Override
    public RaptureApplicationDefinition createApplicationDefinition(CallingContext context, String name, String ver, String description) {
        RaptureApplicationDefinition rad = new RaptureApplicationDefinition();
        rad.setName(name);
        rad.setDescription(description);
        rad.setVersion(ver);
        RaptureApplicationDefinitionStorage.add(rad, context.getUser(), "Create app definition");
        return rad;
    }

    @Override
    public RaptureLibraryDefinition createLibraryDefinition(CallingContext context, String name, String ver, String description) {
        RaptureLibraryDefinition rld = new RaptureLibraryDefinition();
        rld.setName(name);
        rld.setDescription(description);
        rld.setVersion(ver);
        RaptureLibraryDefinitionStorage.add(rld, context.getUser(), "Create library definition");
        return rld;
    }

    @Override
    public void deleteApplicationDefinition(CallingContext context, String name) {
        RaptureApplicationDefinitionStorage.deleteByFields(name, context.getUser(), "Remove application definition");
    }

    @Override
    public void deleteLibraryDefinition(CallingContext context, String name) {
        RaptureLibraryDefinitionStorage.deleteByFields(name, context.getUser(), "Remove library definition");
    }

    @Override
    public RaptureApplicationDefinition updateApplicationVersion(CallingContext context, String name, String ver) {
        RaptureApplicationDefinition rad = retrieveApplicationDefinition(context, name);
        rad.setVersion(ver);
        RaptureApplicationDefinitionStorage.add(rad, context.getUser(), "Update application version");
        return rad;
    }

    @Override
    public RaptureLibraryDefinition updateLibraryVersion(CallingContext context, String name, String ver) {
        RaptureLibraryDefinition rld = retrieveLibraryDefinition(context, name);
        rld.setVersion(ver);
        RaptureLibraryDefinitionStorage.add(rld, context.getUser(), "Update library version");
        return rld;
    }

    @Override
    public RaptureApplicationInstance createApplicationInstance(CallingContext context, String name, String description, String serverGroup, String appName,
            String timeRange, int retryCount, String parameters, String apiUser) {
        RaptureApplicationInstance rai = new RaptureApplicationInstance();
        rai.setName(name);
        rai.setDescription(description);
        rai.setAppName(appName);
        rai.setRetryCount(retryCount);
        if (parameters.trim().isEmpty()) {
            rai.setParameters("");
        } else {
            rai.setParameters(parameters);
        }
        rai.setTimeRangeSpecification(timeRange);
        rai.setServerGroup(serverGroup);
        if (apiUser.trim().isEmpty()) {
            rai.setApiUser("");
        } else {
            rai.setApiUser(apiUser);
        }
        RaptureApplicationInstanceStorage.add(rai, context.getUser(), "Create app instance");
        notifyRunner(context);
        return rai;
    }

    @Override
    public void deleteApplicationInstance(CallingContext context, String name, String serverGroup) {
        RaptureApplicationInstanceStorage.deleteByFields(serverGroup, name, context.getUser(), "Remove application instance");
        notifyRunner(context);
    }

    private List<RaptureServerGroup> getServerGroupInstances(CallingContext context) {
        return RaptureServerGroupStorage.readAll();
    }

    private List<RaptureApplicationInstance> getApplicationInstancesForServerGroup(CallingContext context, String serverGroup) {
        final List<RaptureApplicationInstance> ret = new ArrayList<RaptureApplicationInstance>();
        String prefix = new RaptureApplicationInstancePathBuilder().serverGroup(serverGroup).buildStorageLocation().getDocPath() + "/";
        getConfigRepo().visitAll(prefix, null, new RepoVisitor() {

            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                if (!isFolder) {
                    ret.add(JacksonUtil.objectFromJson(content.getContent(), RaptureApplicationInstance.class));
                }
                return true;
            }

        });
        return ret;
    }

    @Override
    public List<RaptureApplicationInstance> getApplicationsForServer(CallingContext context, String serverName) {
        // Given a server, determine the groups it belongs to, and return the
        // ApplicationInstances within
        // those groups
        List<RaptureServerGroup> serverGroups = getServerGroupInstances(context);
        List<RaptureApplicationInstance> ret = new ArrayList<RaptureApplicationInstance>();
        for (RaptureServerGroup serverGroup : serverGroups) {
            boolean thisIncluded = false;
            if (serverGroup.getInclusions().contains("*")) {
                thisIncluded = true;
            } else if (serverGroup.getInclusions().contains(serverName)) {
                thisIncluded = true;
            }
            if (serverGroup.getExclusions().contains(serverName)) {
                thisIncluded = false;
            }
            if (thisIncluded) {
                ret.addAll(getApplicationInstancesForServerGroup(context, serverGroup.getName()));
            }
        }
        return ret;
    }

    @Override
    public RaptureApplicationDefinition getApplicationDefinition(CallingContext context, String name) {
        return RaptureApplicationDefinitionStorage.readByFields(name);
    }

    private RaptureApplicationDefinition retrieveApplicationDefinition(CallingContext context, String name) {
        return RaptureApplicationDefinitionStorage.readByFields(name);
    }

    @Override
    public RaptureLibraryDefinition getLibraryDefinition(CallingContext context, String name) {
        return RaptureLibraryDefinitionStorage.readByFields(name);
    }

    private RaptureLibraryDefinition retrieveLibraryDefinition(CallingContext context, String name) {
        return RaptureLibraryDefinitionStorage.readByFields(name);
    }

    @Override
    public void setRunnerConfig(CallingContext context, String name, String value) {
        RaptureRunnerConfig config = getRunnerConfig(context);
        if (config == null) {
            config = new RaptureRunnerConfig();
            config.setConfig(new HashMap<String, String>());
        }
        config.getConfig().put(name, value);
        RaptureRunnerConfigStorage.add(config, context.getUser(), "Set runner config");
    }

    @Override
    public void deleteRunnerConfig(CallingContext context, String name) {
        RaptureRunnerConfig config = getRunnerConfig(context);
        if (config != null) {
            config.getConfig().remove(name);
            RaptureRunnerConfigStorage.add(config, context.getUser(), "Remove runner config");
        }
    }

    @Override
    public RaptureRunnerConfig getRunnerConfig(CallingContext context) {
        RaptureRunnerConfig config = RaptureRunnerConfigStorage.readByFields();
        if (config == null) {
            config = new RaptureRunnerConfig();
            config.setConfig(new HashMap<String, String>());
        }
        return config;
    }

    @Override
    public void recordRunnerStatus(CallingContext context, String serverName, String serverGroup, String instanceName, String appName, String status) {
        String lockName = getRunnerStatusLockName(serverName);
        logger.debug("Trying to acquire lock: Updating status for " + instanceName + " running on server " + serverName);
        LockHandle lockHandle = acquireLock(context, lockName);
        if (lockHandle != null) {
            try {
                logger.debug("Updating status for " + instanceName + " running on server " + serverName);
                RaptureRunnerStatus runnerStatus = getRunnerStatus(context, serverName);
                if (runnerStatus == null) {
                    logger.info("No status recorded, creating new");
                    runnerStatus = new RaptureRunnerStatus();
                    runnerStatus.setStatusByInstanceName(new HashMap<String, RaptureRunnerInstanceStatus>());
                    runnerStatus.setServerName(serverName);
                }

                RaptureRunnerInstanceStatus instanceStatus = new RaptureRunnerInstanceStatus();
                if (runnerStatus.getStatusByInstanceName().containsKey(instanceName)) {
                    logger.debug("Information for this instance already existing");
                    instanceStatus = runnerStatus.getStatusByInstanceName().get(instanceName);
                    // If we are marking this as stopped, or failed, reset the
                    // needs
                    // restart flag
                    if (!status.equals("RUNNING")) {
                        instanceStatus.setNeedsRestart(false);
                    }
                } else {
                    logger.debug("Newly seen application instance");
                    instanceStatus.setAppInstance(instanceName);
                    instanceStatus.setServerGroup(serverGroup);
                    instanceStatus.setAppName(appName);
                }
                instanceStatus.setStatus(status);
                instanceStatus.setLastSeen(new Date());

                logger.debug("Recording update to instance status, before " + runnerStatus.getStatusByInstanceName().keySet().size());
                runnerStatus.getStatusByInstanceName().put(instanceName, instanceStatus);
                logger.debug("Recording update to instance status, after " + runnerStatus.getStatusByInstanceName().keySet().size());
                RaptureRunnerStatusStorage.add(runnerStatus, context.getUser(), "Record runner status");
            } finally {
                logger.debug("Done updating status for " + instanceName + " running on server " + serverName);
                releaseLock(context, lockName, lockHandle);
            }
        } else {
            logger.error(String.format("Unable to acquire lock %s, so unable to record runner status", lockName));
        }
    }

    @Override
    public void recordInstanceCapabilities(CallingContext context, String serverName, String instanceName, Map<String, Object> capabilities) {
        RaptureInstanceCapabilities ric = new RaptureInstanceCapabilities();
        ric.setInstanceName(instanceName);
        ric.setServer(serverName);
        ric.setCapabilities(capabilities);
        RaptureInstanceCapabilitiesStorage.add(ric, context.getUser(), "Setting capabilities");
    }

    @Override
    public Map<String, RaptureInstanceCapabilities> getCapabilities(CallingContext context, String serverName, List<String> instanceNames) {
        Map<String, RaptureInstanceCapabilities> nameToRic = new HashMap<String, RaptureInstanceCapabilities>();
        for (String name : instanceNames) {
            RaptureInstanceCapabilities ric = RaptureInstanceCapabilitiesStorage.readByFields(serverName, name);
            if (ric == null) {
                ric = new RaptureInstanceCapabilities();
                ric.setInstanceName(name);
            }
            nameToRic.put(name, ric);
        }
        return nameToRic;
    }

    private String getRunnerStatusLockName(String serverName) {
        return String.format(RUNNER_STATUS_LOCK, serverName);
    }

    private LockHandle acquireLock(CallingContext context, String lockName) {
        RaptureURI providerURI = Kernel.getLock().getTrusted().getKernelManagerUri();
        return Kernel.getLock().acquireLock(context, providerURI.toString(), lockName, 30, 5);
    }

    private void releaseLock(CallingContext context, String lockName, LockHandle lockHandle) {
        RaptureURI providerURI = Kernel.getLock().getTrusted().getKernelManagerUri();
        Kernel.getLock().releaseLock(context, providerURI.toString(), lockName, lockHandle);
    }

    @Override
    public List<String> getRunnerServers(CallingContext context) {
        final List<String> ret = new ArrayList<String>();
        List<RaptureRunnerStatus> statuses = RaptureRunnerStatusStorage.readAll();
        for (RaptureRunnerStatus status : statuses) {
            ret.add(status.getServerName());
        }
        return ret;
    }

    @Override
    public RaptureRunnerStatus getRunnerStatus(CallingContext context, String serverName) {
        RaptureRunnerStatus runnerStatus = RaptureRunnerStatusStorage.readByFields(serverName);
        if (runnerStatus == null) {
            logger.info("No status found, returning default");
            runnerStatus = new RaptureRunnerStatus();
            runnerStatus.setStatusByInstanceName(new HashMap<String, RaptureRunnerInstanceStatus>());
            runnerStatus.setServerName(serverName);
        }
        return runnerStatus;
    }

    @Override
    public RaptureServerGroup getServerGroup(CallingContext context, String name) {
        return RaptureServerGroupStorage.readByFields(name);
    }

    @Override
    public RaptureApplicationInstance getApplicationInstance(CallingContext context, String name, String serverGroup) {
        return RaptureApplicationInstanceStorage.readByFields(serverGroup, name);
    }

    public RaptureApplicationInstance createOneShot(CallingContext context, String appName, String serverGroup, String parameters, String apiUser) {
        logger.warn("RunnerApi.createOneShot is deprecated. Please change your code to use runApplication");
        RaptureApplicationInstance rai = new RaptureApplicationInstance();
        String instanceId = IDGenerator.getUUID(5);
        rai.setName(appName + "-" + instanceId);
        rai.setDescription("One shot for " + appName);
        rai.setAppName(appName);
        rai.setRetryCount(3);
        rai.setLastStateChange(new Date());
        if (parameters.trim().isEmpty()) {
            rai.setParameters("");
        } else {
            rai.setParameters(parameters);
        }
        rai.setTimeRangeSpecification("* *");
        rai.setServerGroup(serverGroup);
        if (apiUser.trim().isEmpty()) {
            rai.setApiUser("");
        } else {
            rai.setApiUser(apiUser);
        }
        rai.setOneShot(true);
        RaptureApplicationInstanceStorage.add(rai, context.getUser(), "Create one shot");
        notifyRunner(context);
        return rai;
    }

    private void notifyRunner(CallingContext context) {
        MimeRunnerNotification mime = new MimeRunnerNotification();
        // notify all runners, since we may have multiple runners connected to
        // same network
        TaskSubmitter.submitBroadcastToCategory(context, mime, MimeRunnerNotification.getMimeType(), PipelineConstants.CATEGORY_RUNNER);
    }

    public RaptureApplicationInstance updateOneShot(CallingContext context, String appName, String serverGroup, String status, Boolean finished) {
        logger.warn("RunnerApi.updateOneShot is deprecated. Please change your code to use runApplication");
        RaptureApplicationInstance inst = getApplicationInstance(context, appName, serverGroup);
        if (inst.getOneShot()) {
            inst.setStatus(status);
            inst.setFinished(finished);
            inst.setLastStateChange(new Date());
            RaptureApplicationInstanceStorage.add(inst, context.getUser(), "Update one shot");
            notifyRunner(context);
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Instance is not a one-shot instance");
        }
        return inst;
    }

    public Boolean lockOneShot(CallingContext context, String name, String serverGroup, String myServer) {
        logger.warn("RunnerApi.lockOneShot is deprecated. Please change your code to use runApplication");
        RaptureApplicationInstance instance = getApplicationInstance(context, name, serverGroup);
        if (instance.getLockedBy() != null) {
            if (instance.getLockedBy().equals(myServer)) {
                return true;
            } else {
                return false;
            }
        }
        instance.setLockedBy(myServer);
        RaptureApplicationInstanceStorage.add(instance, context.getUser(), "Lock one shot");
        notifyRunner(context);
        instance = getApplicationInstance(context, name, serverGroup);
        if (instance.getLockedBy().equals(myServer)) {
            return true;
        }
        return false;
    }

    @Override
    public List<String> getApplicationsForServerGroup(CallingContext context, String serverGroup) {
        final List<String> ret = new ArrayList<String>();
        final String prefix = new RaptureApplicationInstancePathBuilder().serverGroup(serverGroup).buildStorageLocation().getDocPath() + "/";
        getConfigRepo().visitAll(prefix, null, new RepoVisitor() {

            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                if (!isFolder) {
                    ret.add(name.substring(prefix.length()));
                }
                return true;
            }

        });
        return ret;
    }

    /**
     * Remove old status records from this, then save if anything changed
     */
    @Override
    public void cleanRunnerStatus(CallingContext context, int ageInMinutes) {
        List<String> runnerServers = getRunnerServers(context);
        for (String serverName : runnerServers) {
            String lockName = getRunnerStatusLockName(serverName);
            LockHandle lockHandle = acquireLock(context, lockName);
            if (lockHandle != null) {
                try {
                    logger.debug("Cleaning data on server " + serverName);
                    RaptureRunnerStatus status = getRunnerStatus(context, serverName);
                    List<String> toRemove = new ArrayList<String>();
                    Calendar now = Calendar.getInstance();
                    now.add(Calendar.MINUTE, ageInMinutes * -1);
                    for (Map.Entry<String, RaptureRunnerInstanceStatus> entry : status.getStatusByInstanceName().entrySet()) {
                        Calendar entryDate = Calendar.getInstance();
                        entryDate.setTime(entry.getValue().getLastSeen());
                        if (entryDate.before(now)) {
                            toRemove.add(entry.getKey());
                        }
                    }
                    if (!toRemove.isEmpty()) {
                        for (String instance : toRemove) {
                            status.getStatusByInstanceName().remove(instance);
                        }
                        if (!status.getStatusByInstanceName().isEmpty()) {
                            RaptureRunnerStatusStorage.add(status, context.getUser(), "Clear runner status");
                        } else {
                            RaptureRunnerStatusStorage.deleteByFields(serverName, context.getUser(), "Clear runner status");
                        }
                    }
                } finally {
                    logger.debug("Done cleaning data on server " + serverName);
                    releaseLock(context, lockName, lockHandle);
                }
            } else {
                logger.info(String.format("Unable to acquire lock %s, so unable to clean runner status for server %s this time", lockName, serverName));
            }
        }
    }

    @Override
    public void updateStatus(CallingContext context, String name, String serverGroup, String myServer, String status, Boolean finished) {
        RaptureApplicationInstance instance = getApplicationInstance(context, name, serverGroup);
        if (instance.getLockedBy() != null && instance.getLockedBy().equals(myServer)) {
            instance.setStatus(status);
            instance.setFinished(finished);
            RaptureApplicationInstanceStorage.add(instance, context.getUser(), "Update status");
            notifyRunner(context);
        }
    }

    @Override
    public void markForRestart(CallingContext context, String serverName, String name) {
        String lockName = getRunnerStatusLockName(serverName);
        LockHandle lockHandle = acquireLock(context, lockName);
        if (lockHandle != null) {
            try {
                logger.debug("Setting restart request for " + name + " running on server " + serverName);
                RaptureRunnerStatus runnerStatus = getRunnerStatus(context, serverName);
                if (runnerStatus == null) {
                    logger.debug("No status recorded, nothing to do");
                    return;
                }

                if (runnerStatus.getStatusByInstanceName().containsKey(name)) {
                    logger.info("Setting restart flag");
                    runnerStatus.getStatusByInstanceName().get(name).setNeedsRestart(true);
                    RaptureRunnerStatusStorage.add(runnerStatus, context.getUser(), "Mark for restart");
                }
            } finally {
                logger.debug("Done setting restart request for " + name + " running on server " + serverName);
                releaseLock(context, lockName, lockHandle);
            }
        } else {
            logger.info(String.format("Unable to acquire lock %s, so unable to mark %s on server %s for restart this time", lockName, name, serverName));
        }

    }

    @Override
    public RaptureApplicationStatus runCustomApplication(CallingContext context, String appName, String queueName, Map<String, String> parameterInput,
            Map<String, String> parameterOutput, String customApplicationPath) {
        RaptureApplicationStatus status = new RaptureApplicationStatus();
        status.setAppName(appName);
        status.setInstanceId(IDGenerator.getUUID());
        status.setInputConfig(parameterInput);
        status.setOutputConfig(parameterOutput);
        status.setStatus(RaptureApplicationStatusStep.INITIATED);
        status.setOverrideApplicationPath(customApplicationPath);
        return lowerInitiateApp(context, status);
    }

    @Override
    public RaptureApplicationStatus runApplication(CallingContext context, String appName, String queueName, Map<String, String> parameterInput,
            Map<String, String> parameterOutput) {
        RaptureApplicationStatus status = new RaptureApplicationStatus();
        status.setAppName(appName);
        status.setInstanceId(IDGenerator.getUUID());
        status.setInputConfig(parameterInput);
        status.setOutputConfig(parameterOutput);
        status.setStatus(RaptureApplicationStatusStep.INITIATED);
        return lowerInitiateApp(context, status);
    }

    private RaptureApplicationStatus lowerInitiateApp(CallingContext context, RaptureApplicationStatus status) {
        Date now = new Date();
        String yyyymmdd = new SimpleDateFormat("yyyyMMdd").format(now);
        status.setTheDate(yyyymmdd);
        RaptureApplicationStatusStorage.add(status, context.getUser(), "New application creation");

        RapturePipelineTask pTask = new RapturePipelineTask();
        pTask.setPriority(1);
        List<String> categoryList = new LinkedList<String>();
        categoryList.add(PipelineConstants.CATEGORY_APPMANAGER);
        pTask.setCategoryList(categoryList);
        pTask.initTask();

        pTask.addMimeObject(status);
        pTask.setContentType(RAPTUREAPPLICATIONSTATUS);
        // Check that queue is valid?
        Kernel.getPipeline().publishMessageToCategory(context, pTask);
        return status;
    }

    @Override
    public RaptureApplicationStatus getApplicationStatus(CallingContext context, String applicationStatusURI) {
        RaptureURI parsedURI = new RaptureURI(applicationStatusURI, Scheme.APPSTATUS);
        return RaptureApplicationStatusStorage.readByAddress(parsedURI);
    }

    @Override
    public List<String> getApplicationStatusDates(CallingContext context) {
        final Set<String> dates = new HashSet<String>();
        RaptureApplicationStatusStorage.visitAll(new RepoVisitor() {
            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                RaptureApplicationStatus status = RaptureApplicationStatusStorage.readFromJson(content);
                dates.add(status.getTheDate());
                return true;
            }
        });
        return new ArrayList<String>(dates);
    }

    @Override
    public List<RaptureApplicationStatus> getApplicationStatuses(CallingContext context, String date) {
        return RaptureApplicationStatusStorage.readAll(date);
    }

    @Override
    public RaptureApplicationStatus changeApplicationStatus(CallingContext context, String applicationStatusURI, RaptureApplicationStatusStep statusCode,
            String message) {
        RaptureApplicationStatus status = getApplicationStatus(context, applicationStatusURI);
        if (status != null) {
            status.setStatus(statusCode);
            status.setLastMessage(message);
            if (status.getMessages() == null) {
                status.setMessages(new ArrayList<String>());
            }
            status.getMessages().add(message);
            RaptureApplicationStatusStorage.add(status, context.getUser(), "Updated status");
        }
        return null;
    }

    @Override
    public void recordStatusMessages(CallingContext context, String applicationStatusURI, List<String> messages) {
        RaptureApplicationStatus status = getApplicationStatus(context, applicationStatusURI);
        if (status != null) {
            if (status.getMessages() == null) {
                status.setMessages(new ArrayList<String>());
            }
            status.getMessages().addAll(messages);
            RaptureApplicationStatusStorage.add(status, context.getUser(), "Updated from std out");
        }
    }

    @Override
    public RaptureApplicationStatus terminateApplication(CallingContext context, String applicationStatusURI, String reasonMessage) {
        // Put such a message on the general "broadcast" queue to all interested
        // parties, who would potentially reach back to update the status
        // Depending on the state...
        logger.info("terminateApplication not implemented");
        return null;
    }

    @Override
    public void archiveApplicationStatuses(CallingContext context) {
        // Remove old and boring app statuses.
        logger.info("archiveApplicationStatuses  not implemented");
    }

    @Override
    public List<RaptureServerGroup> getAllServerGroups(CallingContext context) {
        return RaptureServerGroupStorage.readAll();
    }

    @Override
    public List<RaptureApplicationDefinition> getAllApplicationDefinitions(CallingContext context) {
        return RaptureApplicationDefinitionStorage.readAll();
    }

    @Override
    public List<RaptureLibraryDefinition> getAllLibraryDefinitions(CallingContext context) {
        return RaptureLibraryDefinitionStorage.readAll();
    }

    @Override
    public List<RaptureApplicationInstance> getAllApplicationInstances(CallingContext context) {
        return RaptureApplicationInstanceStorage.readAll();
    }
}
