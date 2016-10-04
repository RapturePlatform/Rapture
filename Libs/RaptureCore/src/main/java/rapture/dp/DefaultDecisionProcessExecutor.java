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
package rapture.dp;

import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import rapture.common.AppStatus;
import rapture.common.AppStatusGroup;
import rapture.common.AppStatusGroupStorage;
import rapture.common.CallingContext;
import rapture.common.ErrorWrapper;
import rapture.common.ErrorWrapperFactory;
import rapture.common.LockHandle;
import rapture.common.RapturePipelineTask;
import rapture.common.RaptureScript;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.dp.AbstractInvocable;
import rapture.common.dp.ContextVariables;
import rapture.common.dp.Step;
import rapture.common.dp.StepHelper;
import rapture.common.dp.StepRecord;
import rapture.common.dp.Transition;
import rapture.common.dp.WorkOrder;
import rapture.common.dp.WorkOrderInitialArgsHash;
import rapture.common.dp.WorkOrderInitialArgsHashStorage;
import rapture.common.dp.WorkOrderStorage;
import rapture.common.dp.Worker;
import rapture.common.dp.WorkerExecutionState;
import rapture.common.dp.WorkerStorage;
import rapture.common.dp.Workflow;
import rapture.common.dp.WorkflowStorage;
import rapture.common.event.DPEventConstants;
import rapture.common.event.EventConstants;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.jar.ChildFirstClassLoader;
import rapture.common.jar.ParentFirstClassLoader;
import rapture.common.mime.MimeDecisionProcessAdvance;
import rapture.config.LocalConfigService;
import rapture.dp.event.WorkOrderStatusUpdateEvent;
import rapture.dp.metrics.WorkflowMetricsService;
import rapture.event.EventLevel;
import rapture.kernel.ContextFactory;
import rapture.kernel.DocApiImpl;
import rapture.kernel.Kernel;
import rapture.kernel.LockApiImpl;
import rapture.kernel.dp.ExecutionContextUtil;
import rapture.kernel.dp.StepRecordUtil;
import rapture.kernel.dp.WorkOrderStatusUtil;
import rapture.kernel.script.KernelScript;
import rapture.log.MDCService;
import rapture.script.reflex.ReflexRaptureScript;
import rapture.server.dp.JoinCountdown;
import rapture.server.dp.JoinCountdownStorage;
import reflex.value.ReflexValue;

/**
 * Default implementation that submits steps sequentially to the pipeline. Steps are picked up and acted upon and this is repeated until the process has
 * completed.
 *
 * @author dukenguyen
 */
public class DefaultDecisionProcessExecutor implements DecisionProcessExecutor {
    private static final boolean FORCE = true;
    private static final boolean NO_FORCE = false;
    private static final String HOST_NAME = LocalConfigService.getServerName();
    public static final String FAIL_TRANSITION = "$FAIL";
    private static final Optional<RaptureException> EXCEPTION_ABSENT = Optional.absent();
    private static Logger log = Logger.getLogger(DefaultDecisionProcessExecutor.class);
    private static final String REPUBLISHED = "$__reserved__REPUBLISHED";
    private static final String JOIN = StepHelper.JOIN_PREFIX;
    private static final String OKAY = "ok";
    private static final Transition RETURN = new Transition();

    private static final ExecutorService metricsExecutor = Executors
            .newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("DP-Metrics-Executor").build());

    static {
        RETURN.setName("$RETURN");
        RETURN.setTargetStep("$RETURN");
    }

    private static String dumpClasspath(ClassLoader loader) {
        StringBuilder sb = new StringBuilder();

        sb.append("Classloader ").append(loader).append(":");

        if (loader instanceof URLClassLoader) {
            URLClassLoader ucl = (URLClassLoader) loader;
            sb.append("\n").append(Arrays.toString(ucl.getURLs()));
        } else sb.append("\n(cannot display components as not a URLClassLoader)");

        if (loader.getParent() != null) sb.append(dumpClasspath(loader.getParent()));
        return sb.toString();
    }

    public static Transition getTransition(Step step, String name) {
        Transition defawlt = null; // spelled because reserved word
        List<Transition> transitions = step.getTransitions();
        if (transitions == null || transitions.isEmpty()) {
            return RETURN;
        }
        for (Transition transition : transitions) {
            if (transition.getName().equals(name)) {
                return transition;
            }
            if ("".equals(transition.getName())) {
                defawlt = transition;
            }
        }
        return defawlt;
    }

    /**
     * Create a new RapturePipelineTask with the WorkOrder as content and dump it on the pipeline with the associated priority
     *
     * @param worker
     *            - WorkOrder to json-ify and place on pipeline for listeners to pickup
     * @param category
     *            The category associated with this step
     */
    public static void publishStep(Worker worker, String category) {
        RapturePipelineTask task = new RapturePipelineTask();
        task.setPriority(worker.getPriority());
        task.setCategoryList(ImmutableList.of(category));
        task.addMimeObject(worker);
        task.setContentType(MimeDecisionProcessAdvance.getMimeType());
        task.initTask();
        Kernel.getPipeline().publishMessageToCategory(ContextFactory.getKernelUser(), task);
    }

    private void publishForkChildren(Worker worker, Step step, Workflow flow) {
        String base = step.getExecutable().substring(StepHelper.FORK_PREFIX.length() + 1);
        String names[] = base.split(",");
        String workOrderUri = worker.getWorkOrderURI();
        WorkOrder workOrder = WorkOrderStorage.readByFields(workOrderUri);
        List<Pair<Worker, Step>> togo = Lists.newArrayList();

        try {
            grabMultiWorkerLock(workOrder, worker, FORCE);
            workOrder = WorkOrderStorage.readByFields(workOrderUri);
            int nextId = workOrder.getWorkerIds().size();
            for (String name : names) {
                Step target = getStep(name, flow);
                Worker child = SplitUtils.createForkChild(workOrder, worker, flow, nextId, target);
                workOrder.getWorkerIds().add(child.getId());
                workOrder.getPendingIds().add(child.getId());
                if (target == null) {
                    child.setDetail("Attempt to start worker with non-extant step " + name + " from " + step.getName() + " in " + flow.getWorkflowURI());
                    log.error(child.getDetail());
                    child.setStatus(WorkerExecutionState.ERROR);
                    saveWorker(child);
                } else {
                    saveWorker(child);
                    ImmutablePair<Worker, Step> pair = new ImmutablePair<>(child, step);
                    togo.add(pair);
                }
                nextId++;
            }
            WorkOrderStorage.add(new RaptureURI(workOrderUri, Scheme.WORKORDER), workOrder, ContextFactory.getKernelUser().getUser(), "Update for fork");
            saveWorker(worker);
        } finally {
            releaseMultiWorkerLock(workOrder, worker, FORCE);
        }
        for (Pair<Worker, Step> pair : togo) {
            publishStep(pair.getLeft(), calculateCategory(pair.getRight(), flow));
        }
    }

    public void publishSplitChildren(Worker parent, Step step, Workflow flow) {
        String base = step.getExecutable().substring(StepHelper.SPLIT_PREFIX.length() + 1);
        String names[] = base.split(",");
        parent.setWaitCount(names.length);
        parent.setStatus(WorkerExecutionState.BLOCKED);
        List<ImmutablePair<Worker, String>> children = Lists.newArrayList();
        List<Worker> stillborn = Lists.newArrayList();

        // prepare children and eliminate
        for (int i = 0; i < names.length; i++) {
            Step target = getStep(names[i], flow);
            Worker child = SplitUtils.createSplitChild(parent, flow, i, names.length, target);
            if (target == null) {
                child.setDetail("Attempt to start worker with non-extant step " + names[i] + " from " + step.getName() + " in " + flow.getWorkflowURI());
                log.error(child.getDetail());
                parent.setWaitCount(parent.getWaitCount() - 1);
                child.setStatus(WorkerExecutionState.ERROR);
                saveWorker(child);
                stillborn.add(child);
            } else {
                saveWorker(child);
                children.add(ImmutablePair.of(child, calculateCategory(target, flow)));
            }
        }
        saveWorker(parent);

        // register workers with workorder
        String workOrderUri = parent.getWorkOrderURI();
        WorkOrder workOrder = WorkOrderStorage.readByFields(workOrderUri);

        try {
            grabMultiWorkerLock(workOrder, parent, FORCE);
            workOrder = WorkOrderStorage.readByFields(workOrderUri);
            for (Pair<Worker, String> pair : children) {
                workOrder.getWorkerIds().add(pair.getLeft().getId());
                if (log.isDebugEnabled()) {
                    String at = (pair.getLeft().getStack().size() > 0) ? pair.getLeft().getStack().get(0) : "UNKNOWN_LOCATION";
                    log.debug("Adding new worker " + pair.getLeft().getId() + " at " + at);
                }
            }
            for (Worker child : stillborn) {
                workOrder.getWorkerIds().add(child.getId());
            }
            WorkOrderStorage.add(new RaptureURI(workOrderUri, Scheme.WORKORDER), workOrder, ContextFactory.getKernelUser().getUser(), "Update for split");
            JoinCountdown countdown = new JoinCountdown();
            countdown.setParentId(parent.getId());
            countdown.setWorkOrderURI(parent.getWorkOrderURI());
            countdown.setWaitCount(children.size());
            JoinCountdownStorage.add(null, countdown, ContextFactory.getKernelUser().getUser(), "Starting Countdown");
        } finally {
            releaseMultiWorkerLock(workOrder, parent, FORCE);
        }

        // publish viable children
        for (Pair<Worker, String> pair : children) {
            publishStep(pair.getLeft(), pair.getRight());
        }
    }

    // return -1 if the step has no timeout
    private static int getTimeLimit(Step step, Workflow flow) {
        return step.getSoftTimeout();
    }

    private String calculateCategory(Step step, Workflow flow) {
        String category = step.getCategoryOverride();
        return (category == null) ? flow.getCategory() : category;
    }

    private static Step getStep(String name, Workflow flow) {
        List<Step> steps = flow.getSteps();
        for (Step step : steps) {
            if (step.getName().equals(name)) return step;
        }
        return null;
    }

    private static void recordWorkerActivity(Worker worker, String message) {
        recordWorkerActivity(worker, message, false);
    }

    private static void recordWorkerActivity(Worker worker, String message, boolean finished) {
        if (worker.getActivityId() != null) {
            if (!finished) {
                Kernel.getActivity().updateActivity(worker.getCallingContext(), worker.getActivityId(), message, 10L, 100L);
            } else {
                Kernel.getActivity().finishActivity(worker.getCallingContext(), worker.getActivityId(), message);
            }
        }
    }

    /**
     * Adjust a fully-qualified workflow uri that has a step e.g. //xyz/x#step1 and change the step to a different step
     *
     * @param originalStepURI
     *            - the uri to change
     * @param targetStepName
     *            - the name of the step to place in the uri
     * @return - String representing the fully-qualified uri with the new step
     */
    String changeStepUri(String originalStepURI, String targetStepName) {
        RaptureURI orig = new RaptureURI(originalStepURI, Scheme.WORKFLOW);
        String encodedName = StepHelper.encode(targetStepName);
        return RaptureURI.builder(orig).element(encodedName).asString();
    }

    private RaptureURI createWorkerURI(String workOrderURI, String id) {
        return RaptureURI.builder(new RaptureURI(workOrderURI, Scheme.WORKORDER)).element(id).build();
    }

    @Override
    public void executeStep(Worker worker) {
        WorkOrder workOrder = WorkOrderFactory.loadWorkOrder(worker);

        List<String> stack = worker.getStack();
        worker.setStatus(WorkerExecutionState.RUNNING);
        saveWorker(worker);

        String workerURI = createWorkerURI(worker.getWorkOrderURI(), worker.getId()).toString();

        workOrder.setStatus(WorkOrderStatusUtil.computeStatus(workOrder, false));
        WorkOrderStorage.add(new RaptureURI(workOrder.getWorkOrderURI(), Scheme.WORKORDER), workOrder, ContextFactory.getKernelUser().getUser(),
                "Updating status");

        String stepURI = stack.get(0); // don't pop stack just yet -- we are
        // currently executing this
        log.info("Processing step: " + stepURI);

        CallingContext kernelUser = ContextFactory.getKernelUser();
        Pair<Workflow, Step> pair = Kernel.getDecision().getTrusted().getWorkflowWithStep(kernelUser, stepURI);
        Workflow flow = pair.getLeft();
        Step step = pair.getRight();
        if (step == null) {
            RaptureException re = RaptureExceptionFactory.create("Step to be executed not found: " + stepURI);
            markAsFinished(workOrder, worker, WorkerExecutionState.ERROR, Optional.of(re));
            throw re;
        } else {
            try {
                String transitionName = null;
                StepRecord stepRecord = null;
                RaptureException re = null;
                try {
                    recordWorkerActivity(worker, "Start " + step.getName());
                    stepRecord = preExecuteStep(workOrder, worker, step, stepURI);
                    recordWorkerActivity(worker, "Execute " + step.getName());
                    transitionName = runExecutable(step, flow, worker, workerURI, stepRecord);
                } catch (RaptureException e) {
                    re = e;
                } catch (Throwable t) {
                    re = RaptureExceptionFactory.create("Error while executing workorder", t);
                }
                /**
                 * always mark step as finished if possible
                 */
                // refresh StepRecord! this may have changed during execution, e.g. by adding an activity id
                Optional<StepRecord> stepRecordOptional;
                if (stepRecord != null) {
                    stepRecordOptional = StepRecordUtil.getRecord(worker.getWorkOrderURI(), worker.getId(), stepRecord.getStartTime());
                } else {
                    stepRecordOptional = Optional.absent();
                }
                markStepAsFinished(worker, workOrder, stack, stepURI, step, transitionName, stepRecordOptional, Optional.fromNullable(re));
                if (re == null) {
                    // RAP-2956 - check if workflow was cancelled before we go any further
                    if (Kernel.getDecision().wasCancelCalled(worker.getCallingContext(), workOrder.getWorkOrderURI())) {
                        markAsFinished(workOrder, worker, WorkerExecutionState.CANCELLED, EXCEPTION_ABSENT);
                    } else if (REPUBLISHED.equals(transitionName)) {
                        // we don't do anything in this case -- don't want to republish
                    } else if (ReflexValue.Internal.SUSPEND.toString().equals(transitionName)) {
                        worker.setStatus(WorkerExecutionState.BLOCKED);
                        saveWorker(worker);

                        workOrder.setStatus(WorkOrderStatusUtil.computeStatus(workOrder, false));
                        WorkOrderStorage.add(new RaptureURI(workOrder.getWorkOrderURI(), Scheme.WORKORDER), workOrder, ContextFactory.getKernelUser().getUser(),
                                "Updating status");
                    } else {
                        log.trace("no suppress " + transitionName);
                        transitionWorker(worker, workOrder, step, stepURI, transitionName);
                    }
                } else {
                    log.error("Step failed with error - " + re.getFormattedMessage());
                    markAsFinished(workOrder, worker, WorkerExecutionState.ERROR, EXCEPTION_ABSENT);
                }
            } catch (RaptureException e) {
                handleException(e, workOrder, worker, workerURI);
            } catch (Throwable t) {
                /**
                 * Catch Throwable here, not just Exception. This is in case something went wrong after executing the step and while interacting with Rapture.
                 * Catch every Throwable, not just Exception-s. Anything causing a failure here should terminate the workorder in error. It's not OK to ignore
                 * NPEs, for example.
                 */
                String stepName = step.getName();
                handleException(RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Unknown error during execution of step " + stepName, t),
                        workOrder, worker, workerURI);
            }
        }
    }

    protected void markStepAsFinished(Worker worker, WorkOrder workOrder, List<String> stack, String stepURI, Step step, String transitionName,
            Optional<StepRecord> stepRecordOptional, Optional<RaptureException> re) {
        String message = "Returned " + transitionName + " from " + step.getName();
        recordWorkerActivity(worker, message);
        log.info(message);
        postExecuteStep(workOrder, worker, step, stepURI, transitionName, stepRecordOptional, re);
        stack.remove(0); // done executing, now pop stack
    }

    private String getReturnValue(String workerURI, String value) {
        return Kernel.getDecision().getTrusted().getContextValue(ContextFactory.getKernelUser(), workerURI,
                ExecutionContextUtil.treatValueAsDefaultLiteral(value));
    }

    @SuppressWarnings({ "rawtypes", "resource" })
    private AbstractInvocable findInvocable(CallingContext ctx, Step step, Workflow workflow, RaptureURI executableUri, String workerUri,
            StepRecord stepRecord) {
        String className = String.format("rapture.dp.invocable.%s", executableUri.getAuthority());
        Class<?> invocableImpl;
        ClassLoader classLoader;
        try {
            List<String> stepAndWorkflowDeps = new ArrayList<>(step.getJarUriDependencies());
            stepAndWorkflowDeps.addAll(workflow.getJarUriDependencies());
            KernelScript ks = new KernelScript();
            ks.setCallingContext(ctx);
            if (workflow.getUseParentFirstClassLoader()) {
                classLoader = new ParentFirstClassLoader(this.getClass().getClassLoader(), ks, stepAndWorkflowDeps);
            } else {
                classLoader = new ChildFirstClassLoader(this.getClass().getClassLoader(), ks, stepAndWorkflowDeps);
            }
            invocableImpl = classLoader.loadClass(className);
        } catch (ClassNotFoundException | ExecutionException e) {
            log.error("Cannot load class " + className, e);
            log.debug(dumpClasspath(AbstractInvocable.class.getClassLoader()));
            throw RaptureExceptionFactory.create("Error executing workflow: " + e.getMessage(), e);
        }
        if (AbstractInvocable.class.isAssignableFrom(invocableImpl)) {
            try {
                AbstractInvocable invocable = (AbstractInvocable) invocableImpl.getConstructor(String.class, String.class).newInstance(workerUri,
                        stepRecord.getName());
                invocable.setClassLoader(classLoader);
                invocable.setStepStartTime(stepRecord.getStartTime());
                return invocable;
            } catch (InstantiationException e) {
                throw RaptureExceptionFactory.create(String.format("Error executing workflow -- class %s not found", className), e);
            } catch (IllegalAccessException e) {
                throw RaptureExceptionFactory.create(String.format("Error executing workflow -- class %s not public", className), e);
            } catch (IllegalArgumentException e) {
                throw RaptureExceptionFactory.create(String.format("Error executing workflow -- %s is not a valid name", className), e);
            } catch (InvocationTargetException e) {
                throw RaptureExceptionFactory.create(String.format("Error executing workflow -- class %s no constructor", className), e);
            } catch (NoSuchMethodException e) {
                throw RaptureExceptionFactory.create(String.format("Error executing workflow -- class %s constructor args", className), e);
            } catch (SecurityException e) {
                throw RaptureExceptionFactory.create("Error executing workflow -- security", e);
            }
        } else {
            throw RaptureExceptionFactory.create("Error executing workflow.",
                    new Exception(String.format("Native call to class %s failed because is not an implementation of AbstractInvocable", className)));
        }

    }

    private AppStatusGroup getAppStatusGroup(String appStatusName, String workOrderURI) {
        AppStatusGroup group = AppStatusGroupStorage.readByFields(appStatusName);
        if (group == null) {
            group = new AppStatusGroup();
            group.setName(appStatusName);
        }
        AppStatus status = group.getIdToStatus().get(workOrderURI);
        if (status == null) {
            status = new AppStatus();
            status.setName(appStatusName);
            group.getIdToStatus().put(workOrderURI, status);
            status.setWorkOrderURI(workOrderURI);
        }
        return group;
    }

    private void handleException(RaptureException e, WorkOrder workOrder, Worker worker, String workerURI) {
        List<ErrorWrapper> errors = Kernel.getDecision().getTrusted().getErrorsFromContext(ContextFactory.getKernelUser(), workerURI);
        if (errors.size() > 0) {
            log.error("Got these errors during execution (json): " + JacksonUtil.jsonFromObject(errors));
        }
        log.error("Failed with error - " + e.getFormattedMessage());
        markAsFinished(workOrder, worker, WorkerExecutionState.ERROR, Optional.of(e));
    }

    private void markAsFinished(final WorkOrder workOrder, final Worker worker, WorkerExecutionState status, Optional<RaptureException> re) {
        // If non-null, worker parent to wake after we release the lock.
        Worker parentToWake = null;
        try {
            grabMultiWorkerLock(workOrder, worker, NO_FORCE);
            worker.setStatus(status);
            if (re.isPresent()) {
                if (worker.getDetail() == null) worker.setDetail("State is " + status.name() + " due to exception");
                ErrorWrapper exceptionInfo = ErrorWrapperFactory.create(re.get());
                worker.setExceptionInfo(exceptionInfo);
            } else if (status == WorkerExecutionState.ERROR) {
                if (worker.getDetail() == null) worker.setDetail("Error is not due to an exception");
            }
            CallingContext kernelUser = ContextFactory.getKernelUser();
            saveWorker(worker);
            String id = worker.getId();
            List<String> ids = workOrder.getPendingIds();
            String group = worker.getParent();
            if (group != null && !group.isEmpty()) {
                JoinCountdown countdown = JoinCountdownStorage.readByFields(worker.getWorkOrderURI(), group);
                int count = countdown.getWaitCount();
                if (count <= 1) {
                    parentToWake = WorkerStorage.readByFields(worker.getWorkOrderURI(), group);
                    JoinCountdownStorage.deleteByFields(worker.getWorkOrderURI(), group, ContextFactory.getKernelUser().getUser(), "remove old counter");
                } else {
                    countdown.setWaitCount(count - 1);
                    JoinCountdownStorage.add(null, countdown, ContextFactory.getKernelUser().getUser(), "decrement join countdown");
                }
            } else if (ids.size() == 1 && ids.get(0).equals(id)) {
                workOrder.setEndTime(System.currentTimeMillis());
                try {
                    WorkOrderStorage.add(new RaptureURI(workOrder.getWorkOrderURI(), Scheme.WORKORDER), workOrder, kernelUser.getUser(), "Finished execution");
                } finally {
                    Kernel.getDecision().getTrusted().releaseWorkOrderLock(kernelUser, workOrder);
                    WorkOrderExecutionState overallStatus = WorkOrderStatusUtil.computeStatus(workOrder, true);
                    workOrder.setStatus(overallStatus);
                    recordAppStatusEnded(workOrder, worker);
                    submitMetrics(workOrder, worker, overallStatus);
                }
            } else {
                workOrder.setStatus(WorkOrderStatusUtil.computeStatus(workOrder, false));
            }
            // remove the completed id from the master list
            ids.remove(id);
            workOrder.setPendingIds(ids);

            // Get workflow output from ephemeral storage
            DocApiImpl docApi = Kernel.getDoc().getTrusted();
            String outputUri = RaptureURI.newScheme(worker.getWorkOrderURI(), Scheme.DOCUMENT).toShortString();

            String doc = docApi.getDocEphemeral(kernelUser, outputUri);
            if (doc != null) {
                Map<String, Object> map = JacksonUtil.getMapFromJson(doc);
                Map<String, String> outputs = workOrder.getOutputs();
                if (outputs == null) {
                    outputs = new LinkedHashMap<>();
                    workOrder.setOutputs(outputs);
                }
                for (String key : map.keySet()) {
                    outputs.put(key, map.get(key).toString());
                }
            }
            WorkOrderStorage.add(new RaptureURI(workOrder.getWorkOrderURI(), Scheme.WORKORDER), workOrder, ContextFactory.getKernelUser().getUser(),
                    "Updating status");
        } finally {
            releaseMultiWorkerLock(workOrder, worker, NO_FORCE);
        }
        if (parentToWake != null) {
            awakenWorker(workOrder, parentToWake, worker.getSiblingCount());
        }
        fireStatusUpdateEvent(status, workOrder);
    }

    private void fireStatusUpdateEvent(WorkerExecutionState status, WorkOrder workOrder) {
        WorkOrderStatusUpdateEvent event = new WorkOrderStatusUpdateEvent(getEventLevel(status), workOrder.getWorkOrderURI(), status.name());
        Map<String, String> eventContextMap = new HashMap<>();
        eventContextMap.put(DPEventConstants.EVENT_TYPE, event.getType());
        eventContextMap.put(DPEventConstants.EVENT_CONTENT, JacksonUtil.jsonFromObject(event));

        Kernel.getEvent().runEventWithContext(ContextFactory.getKernelUser(), EventConstants.EVENT_ALERT_URI, workOrder.getWorkOrderURI(), eventContextMap);
    }

    private EventLevel getEventLevel(WorkerExecutionState status) {
        switch (status) {
        case ERROR:
            return EventLevel.ERROR;
        case BLOCKED:
            return EventLevel.WARNING;
        default:
            return EventLevel.INFO;
        }
    }

    private Future<?> submitMetrics(final WorkOrder workOrder, final Worker worker, final WorkOrderExecutionState status) {
        return metricsExecutor.submit(new Runnable() {
            @Override
            public void run() {
                WorkOrderInitialArgsHash argsHash = WorkOrderInitialArgsHashStorage.readByFields(workOrder.getWorkOrderURI());
                String workerURI = createWorkerURI(worker.getWorkOrderURI(), worker.getId()).toString();
                String jobUriString = Kernel.getDecision().getTrusted().getContextValue(ContextFactory.getKernelUser(), workerURI,
                        ContextVariables.PARENT_JOB_URI);
                RaptureURI jobURI;
                if (jobUriString != null) {
                    jobURI = new RaptureURI(jobUriString, Scheme.JOB);
                } else {
                    jobURI = null;
                }
                WorkflowMetricsService.workOrderFinished(Kernel.getMetricsService(), workOrder, argsHash, jobURI, status);
            }
        });
    }

    private void awakenWorker(WorkOrder workOrder, Worker worker, int litterSize) {
        String code = joinChildFailed(workOrder, worker, litterSize) ? "error" : OKAY;
        String stepURI = worker.getStack().remove(0);
        Pair<Workflow, Step> pair = Kernel.getDecision().getTrusted().getWorkflowWithStep(ContextFactory.getKernelUser(), stepURI);
        Step step = pair.getRight();
        if (step == null) {
            throw RaptureExceptionFactory.create("SPLIT step missing when JOIN finished for " + stepURI);
        }
        transitionWorker(worker, workOrder, step, stepURI, code);
    }

    private boolean joinChildFailed(WorkOrder workOrder, Worker parent, int litterSize) {
        for (int i = 0; i < litterSize; i++) {
            String name = SplitUtils.makeChildName(parent.getId(), i);
            Worker child = WorkerStorage.readByFields(workOrder.getWorkOrderURI(), name);
            if (child == null) {
                log.error("No record of child worker found: " + workOrder.getWorkOrderURI() + "#" + name);
                return true;
            }
            if (child.getStatus() == WorkerExecutionState.ERROR) return true;
        }
        return false;
    }

    private static Transition RETURN_TRANSITION;

    static {
        RETURN_TRANSITION = new Transition();
        RETURN_TRANSITION.setName("");
        RETURN_TRANSITION.setTargetStep("$RETURN");
    }

    public void transitionWorker(Worker worker, WorkOrder workOrder, Step step, String stepURI, String transitionName) {
        String workerURI = worker.getWorkOrderURI();
        List<String> stack = worker.getStack();

        while (true) {
            log.trace("Step " + step.getName() + " executed.  Transition name: " + transitionName);
            Transition transition = getTransition(step, transitionName);
            if (transition == null) transition = RETURN_TRANSITION;
            String targetName = transition.getTargetStep();
            if (targetName == null) {
                throw RaptureExceptionFactory.create("Null targetStep in transition");
            }
            if (targetName.startsWith("$RETURN")) {
                if (stack.isEmpty()) {
                    markAsFinished(workOrder, worker, WorkerExecutionState.FINISHED, EXCEPTION_ABSENT);
                    recordWorkerActivity(worker, "Finished", true);
                    return;
                }
                stepURI = stack.remove(0);
                // Pop the view from the workflow just returned from
                // off the stack so it isn't used
                // when resolving a view alias request
                worker.getLocalView().remove(0);
                worker.getAppStatusNameStack().remove(0);
                saveWorker(worker);
                step = Kernel.getDecision().getWorkflowStep(ContextFactory.getKernelUser(), stepURI);
                if (step == null) {
                    RaptureException re = RaptureExceptionFactory.create("Step does not exist: " + stepURI);
                    markAsFinished(workOrder, worker, WorkerExecutionState.ERROR, EXCEPTION_ABSENT);
                    throw re;
                }
                if (targetName.contains(":")) {
                    transitionName = targetName.substring("$RETURN:".length());
                    transitionName = getReturnValue(workerURI, transitionName);
                    log.debug(String.format("Worker %s transition: %s", workerURI, transitionName));
                }
                // We should also attempt to update the app status
                // of this step (that we've just returned from)
                recordAppStatusStepFinish(workOrder, worker, step);
            } else if (FAIL_TRANSITION.equals(targetName)) {
                markAsFinished(workOrder, worker, WorkerExecutionState.ERROR, EXCEPTION_ABSENT);
                recordWorkerActivity(worker, "Failed", true);
                return;
            } else if ("$CANCEL".equals(targetName)) {
                markAsFinished(workOrder, worker, WorkerExecutionState.CANCELLED, EXCEPTION_ABSENT);
                recordWorkerActivity(worker, "Cancelled", true);
                return;
            } else if ("$JOIN".equals(targetName)) {
                markAsFinished(workOrder, worker, WorkerExecutionState.FINISHED, EXCEPTION_ABSENT);
                recordWorkerActivity(worker, "Joined", true);
                return;
            } else {
                String nextStepURI = changeStepUri(stepURI, targetName);
                log.trace("Target transition: " + nextStepURI);
                stack.add(0, nextStepURI);
                String stepCategory = Kernel.getDecision().getStepCategory(ContextFactory.getKernelUser(), nextStepURI);
                saveWorker(worker);
                publishStep(worker, stepCategory);
                return;
            }
        }
    }

    private static final int JOIN_MAX_DELAY = 10000;
    private static final String LOCK_PROVIDER = LockApiImpl.WORKFLOW_MANAGER_URI.toString();
    private static final ConcurrentMap<String, LockHandle> lockHandleMap = Maps.newConcurrentMap();

    /**
     * Check the worker to see if this is a mono-worker or multiworker case. If monoworker, do nothing. If multiworker, grab the mutex.
     */
    private void grabMultiWorkerLock(WorkOrder workOrder, Worker worker, boolean force) {
        if (force || multiWorker(workOrder, worker)) {
            String lockName = getLockName(workOrder);
            LockHandle handle = Kernel.getLock().getTrusted().acquireLock(worker.getCallingContext(), LOCK_PROVIDER, lockName, JOIN_MAX_DELAY, JOIN_MAX_DELAY);
            lockHandleMap.put(lockName, handle);
        }
    }

    /**
     * Check to see whether this is a monoworker or multiworker case. If monoworker, do nothing. If multiworker, release the mutex.
     */
    private void releaseMultiWorkerLock(WorkOrder workOrder, Worker worker, boolean force) {
        if (force || multiWorker(workOrder, worker)) {
            String lockName = getLockName(workOrder);
            LockHandle lockHandle = lockHandleMap.get(lockName);
            Kernel.getLock().getTrusted().releaseLock(worker.getCallingContext(), LOCK_PROVIDER, lockName, lockHandle);
        }
    }

    /**
     * currently this just locks the whole work order. In theory we could have a version that just lock the join group, but I'm deferring that optimization for
     * now. The difference won't be noticable until we have super-complex workflows anyway.
     */
    private String getLockName(WorkOrder workOrder) {
        RaptureURI uri = new RaptureURI(workOrder.getWorkOrderURI(), Scheme.WORKORDER);
        return uri.getDocPath();
    }

    /**
     * Check whether the given workerOrder has more than one workers
     */
    private boolean multiWorker(WorkOrder workOrder, Worker worker) {
        return (workOrder.getWorkerIds().size() > 1) || (worker.getParent() != null && worker.getParent().length() > 0);
    }

    /**
     * Finish up a worker execution
     */
    public Boolean postExecuteStep(WorkOrder workOrder, Worker worker, Step step, String stepURI, String stepRetVal, Optional<StepRecord> stepRecordOptional,
            Optional<RaptureException> re) {
        Map<String, String> emptyMap = Collections.emptyMap();
        if (!stepRecordOptional.isPresent()) {
            log.error(String.format("Error! Step %s ended but stepRecord object is null", stepURI));
        } else {
            StepRecord stepRecord = stepRecordOptional.get();
            if (stepRecord.getStepURI().equals(stepURI)) {
                stepRecord.setEndTime(System.currentTimeMillis());
                stepRecord.setRetVal(stepRetVal);
                Transition transition = getTransition(step, stepRetVal);
                if (transition == null) transition = RETURN_TRANSITION;
                boolean present = re.isPresent();
                if (present || FAIL_TRANSITION.equals(transition.getTargetStep())) {
                    stepRecord.setStatus(WorkOrderExecutionState.ERROR);
                    if (present) {
                        stepRecord.setExceptionInfo(ErrorWrapperFactory.create(re.get()));
                    }

                    if (worker.getDetail() == null) {
                        worker.setDetail("Target step is " + transition.getTargetStep() + " - exception is " + ((present) ? "present" : "absent"));
                    }
                    String activityId = stepRecord.getActivityId();
                    if (activityId != null) {
                        Kernel.getActivity().requestAbortActivity(worker.getCallingContext(), activityId, "Step failed");
                    }
                } else {
                    stepRecord.setStatus(WorkOrderExecutionState.FINISHED);
                    String activityId = stepRecord.getActivityId();
                    if (activityId != null) {
                        Kernel.getActivity().finishActivity(worker.getCallingContext(), activityId, "Step finished");
                    }
                }
                StepRecordUtil.writeStepRecord(ContextFactory.getKernelUser(), worker.getWorkOrderURI(), worker.getId(), stepRecord,
                        "Updating step record from post-execute-step");
            } else {
                log.error(String.format("Error! Step '%s' ended but different from last recorded '%s' found in worker %s-%s", stepURI, stepRecord.getStepURI(),
                        worker.getWorkOrderURI(), worker.getId()));
            }
        }
        recordAppStatusStepFinish(workOrder, worker, step);
        worker.setViewOverlay(emptyMap);
        WorkerStorage.add(null, worker, ContextFactory.getKernelUser().getUser(), "Post execute step");
        if (log.isDebugEnabled()) {
            log.debug(String.format("POST: Saving worker: \n%s", JacksonUtil.jsonFromObject(worker)));
        }
        if (stepRecordOptional.isPresent()) {
            StepRecord stepRecord = stepRecordOptional.get();
            MDCService.INSTANCE.clearWorkOrderStepMDC(stepRecord.getName(), stepRecord.getStartTime());
        }

        return true;
    }

    /**
     * Prepare any any data needed to execute a step, e.g. the execution context overlay. Also do any other housekeeping related to the step execution (e.g.
     * record any relevant stats)
     */
    public StepRecord preExecuteStep(WorkOrder workOrder, Worker worker, Step step, String stepURI) {
        worker.setViewOverlay(step.getView());
        recordAppStatusStepStart(workOrder, worker, step);
        WorkerStorage.add(null, worker, ContextFactory.getKernelUser().getUser(), "Pre execute step");

        StepRecord stepRecord = new StepRecord();
        stepRecord.setStepURI(stepURI);
        stepRecord.setName(step.getName());
        stepRecord.setStartTime(System.currentTimeMillis());
        stepRecord.setHostname(HOST_NAME);
        stepRecord.setStatus(WorkOrderExecutionState.ACTIVE);
        StepRecordUtil.writeStepRecord(ContextFactory.getKernelUser(), worker.getWorkOrderURI(), worker.getId(), stepRecord,
                "Writing step from pre-execute-step");

        if (log.isDebugEnabled()) {
            log.debug(String.format("PRE: Saving worker:\n%s", JacksonUtil.jsonFromObject(worker)));
        }
        MDCService.INSTANCE.setWorkOrderStepMDC(step.getName(), stepRecord.getStartTime());
        return stepRecord;
    }

    private void recordAppStatusEnded(WorkOrder workOrder, Worker worker) {
        // Attempt to update the fact that this status has now ended
        String appStatusURI = InvocableUtils.getAppStatusName(worker);
        if (appStatusURI == null || appStatusURI.isEmpty()) {
            return;
        }
        String workOrderURI = workOrder.getWorkOrderURI();
        AppStatusGroup group = getAppStatusGroup(appStatusURI, workOrderURI);
        AppStatus appStatus = group.getIdToStatus().get(workOrderURI);
        appStatus.setOverallStatus(workOrder.getStatus());
        appStatus.setLastUpdated(System.currentTimeMillis());
        AppStatusGroupStorage.add(group, ContextFactory.getKernelUser().getUser(), "WorkOrder ended");
    }

    private void recordAppStatusStepFinish(WorkOrder workOrder, Worker worker, Step step) {
        String appStatusName = InvocableUtils.getAppStatusName(worker);
        if (appStatusName == null || appStatusName.isEmpty()) {
            return;
        }
        String workflowLogUri = InvocableUtils.getWorkflowAuditLog(appStatusName, workOrder.getWorkOrderURI(), step.getName());
        Kernel.getAudit().writeAuditEntry(ContextFactory.getKernelUser(), workflowLogUri, "workflow", 1, step.getName() + " finished");
        String workOrderURI = worker.getWorkOrderURI();
        AppStatusGroup group = getAppStatusGroup(appStatusName, workOrderURI);
        AppStatus appStatus = group.getIdToStatus().get(workOrderURI);
        appStatus.setLastUpdated(System.currentTimeMillis());
        appStatus.setOverallStatus(workOrder.getStatus());
        AppStatusGroupStorage.add(group, ContextFactory.getKernelUser().getUser(), "Step finished");
    }

    /**
     * If the worker view overlay contains DP_APP_STATUS_URI then that is a URI we should write or update a {@link AppStatus} document.
     *
     * @param worker
     * @param step
     */
    private void recordAppStatusStepStart(WorkOrder workOrder, Worker worker, Step step) {
        String appStatusName = InvocableUtils.getAppStatusName(worker);
        if (appStatusName == null || appStatusName.isEmpty()) {
            return;
        }
        String workflowLogUri = InvocableUtils.getWorkflowAuditLog(appStatusName, workOrder.getWorkOrderURI(), step.getName());
        Kernel.getAudit().writeAuditEntry(ContextFactory.getKernelUser(), workflowLogUri, "workflow", 1, step.getName() + " started");
        String workOrderURI = worker.getWorkOrderURI();
        AppStatusGroup group = getAppStatusGroup(appStatusName, workOrderURI);
        AppStatus appStatus = group.getIdToStatus().get(workOrderURI);
        appStatus.setLastUpdated(System.currentTimeMillis());
        appStatus.setOverallStatus(workOrder.getStatus());
        AppStatusGroupStorage.add(group, ContextFactory.getKernelUser().getUser(), "Step started");
    }

    /**
     * Given a uri that points to something that can be executable, execute that URI. These currently include reflex scripts and questions. The return value of
     * the executable should represent the next transition in the workflow. If null is returned, the workflow is assumed to be in a BLOCKED state. This would be
     * the case for a question, for example.
     *
     * @param step
     * @param flow
     * @param worker
     * @param workerURI
     * @return - String representing the next transition, or SUSPEND if BLOCKED
     */
    private String runExecutable(Step step, Workflow flow, Worker worker, String workerURI, StepRecord stepRecord) {

        if (StepHelper.isSpecialStep(step)) {
            if (StepHelper.isReturnStep(step)) {
                String retVal = StepHelper.getReturnValue(step);
                retVal = getReturnValue(workerURI, retVal);
                log.info(String.format("Worker %s returned: %s", workerURI, retVal));
                return retVal;
            } else if (StepHelper.isSplitStep(step)) {
                publishSplitChildren(worker, step, flow);
                return REPUBLISHED;
            } else if (StepHelper.isJoinStep(step)) {
                return JOIN;
            } else if (StepHelper.isForkStep(step)) {
                publishForkChildren(worker, step, flow);
                return OKAY;
            } else {
                // TODO handle other special forms ($SLEEP, etc.)
                throw RaptureExceptionFactory.create("Unknown special form: " + step.getExecutable());
            }
        } else {
            String executable = step.getExecutable();
            RaptureURI executableUri = new RaptureURI(executable);
            CallingContext ctx = worker.getCallingContext();
            int limit = getTimeLimit(step, flow);

            switch (executableUri.getScheme()) {
            case SCRIPT:
                String workerAuditUri = InvocableUtils.getWorkflowAuditUri(worker);
                RaptureScript script = Kernel.getScript().getScript(ctx, executable);
                if (script == null) {
                    throw RaptureExceptionFactory.create(String.format("Executable [%s] not found for step [%s]", executable, step.getName()));
                }
                ReflexRaptureScript rScript = new ReflexRaptureScript();
                if (workerAuditUri != null) {
                    rScript.setAuditLogUri(workerAuditUri);
                }
                String auditLogUri = InvocableUtils.getWorkflowAuditLog(InvocableUtils.getAppStatusName(worker), worker.getWorkOrderURI(), step.getName());
                Object result = rScript.runProgram(ctx, null, script, createScriptValsMap(worker, workerURI, stepRecord, auditLogUri), limit);
                return (result == null) ? "" : result.toString();
            case WORKFLOW:
                Workflow workflow = WorkflowStorage.readByAddress(executableUri);
                String stepName = executableUri.getElement();
                if (stepName == null) {
                    stepName = workflow.getStartStep();
                }
                if (stepName == null) {
                    throw RaptureExceptionFactory.create("Unable to determine start step for " + executableUri);
                }
                String stepURI = RaptureURI.builder(executableUri).element(stepName).build().toString();
                // TODO optimize to just immediately execute if current
                // server supports listed category
                String category = Kernel.getDecision().getTrusted().getStepCategory(ctx, stepURI);
                worker.getStack().add(0, stepURI);
                // Push the view context for this workflow onto the local
                // view stack so that it is
                // used in alias resolution
                worker.getLocalView().add(0, workflow.getView());
                // Push the new app status uri
                String appStatusName = InvocableUtils.createAppStatusName(ctx, workflow, worker, "");
                if (appStatusName == null) {
                    appStatusName = "";
                }
                worker.getAppStatusNameStack().add(0, appStatusName);
                saveWorker(worker);
                publishStep(worker, category);
                return REPUBLISHED;
            case DP_JAVA_INVOCABLE:
                AbstractInvocable<?> abstractInvocable = findInvocable(ctx, step, flow, executableUri, workerURI, stepRecord);
                if (limit > 0) {
                    return abstractInvocable.abortableInvoke(ctx, limit);
                } else {
                    return abstractInvocable.invoke(ctx);
                }
            default:
                log.error("Unsupported executable URI: " + executable);
                return ReflexValue.Internal.SUSPEND.toString();
            }
        }
    }

    private Map<String, Object> createScriptValsMap(Worker worker, String workerUriString, StepRecord stepRecord, String auditLogUri) {
        Map<String, Object> extraVals = Maps.newHashMap();

        String workOrderUri = worker.getWorkOrderURI();
        extraVals.put(ContextVariables.DP_WORK_ORDER_URI, workOrderUri);

        RaptureURI workerURI = new RaptureURI(workerUriString);
        extraVals.put(ContextVariables.DP_WORKER_URI, workerURI.toString());
        extraVals.put(ContextVariables.DP_WORKER_ID, workerURI.getElement());
        extraVals.put(ContextVariables.DP_AUDITLOG_URI, auditLogUri);

        extraVals.put(ContextVariables.DP_STEP_NAME, stepRecord.getName());
        extraVals.put(ContextVariables.DP_STEP_START_TIME, stepRecord.getStartTime());

        return extraVals;
    }

    /**
     * Save the worker after each step (i.e. the updated stack)
     *
     * @param worker
     *            - The current {@link Worker} that reflects an execution status of the Workflow
     */
    private static void saveWorker(Worker worker) {
        WorkerStorage.add(worker, ContextFactory.getKernelUser().getUser(), "Updating Worker");
    }

    @Override
    public void start(CallingContext context, Worker worker) {
        log.trace("DefaultDecisionProcessExecutor.start");

        String stepURI = worker.getStack().get(0);
        log.trace("step = " + stepURI);
        String errorMessage = null;
        if (stepURI != null) {
            String stepCategory;
            try {
                stepCategory = Kernel.getDecision().getStepCategory(ContextFactory.getKernelUser(), stepURI);
                recordWorkerActivity(worker, "Step " + stepURI);
            } catch (RaptureException e) {
                Kernel.getDecision().getTrusted().releaseWorkOrderLock(context, worker.getWorkOrderURI());
                throw e;
            }
            if (!StringUtils.isEmpty(stepCategory)) {
                WorkflowMetricsService.startMonitoring(Kernel.getMetricsService(), worker);
                publishStep(worker, stepCategory);
            } else {
                errorMessage = String.format("Unable to determine category for step %s of WorkOrder %s", stepURI, worker.getWorkOrderURI());
            }
        } else {
            errorMessage = "Start step not found for Work Order " + worker.getWorkOrderURI();
        }

        if (errorMessage != null) {
            Kernel.getDecision().getTrusted().releaseWorkOrderLock(context, worker.getWorkOrderURI());
            log.error(errorMessage);
        }
    }
}
