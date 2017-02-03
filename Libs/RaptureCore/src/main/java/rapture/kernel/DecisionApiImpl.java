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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.Period;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import rapture.common.AppStatus;
import rapture.common.AppStatusGroup;
import rapture.common.AppStatusGroupStorage;
import rapture.common.AppStatusStorage;
import rapture.common.CallingContext;
import rapture.common.CreateResponse;
import rapture.common.ErrorWrapper;
import rapture.common.LogQueryResponse;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureJobExec;
import rapture.common.RaptureJobExecStorage;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.SemaphoreAcquireResponse;
import rapture.common.WorkOrderExecutionState;
import rapture.common.WorkflowJobDetails;
import rapture.common.api.DecisionApi;
import rapture.common.dp.AppStatusDetails;
import rapture.common.dp.ContextValueType;
import rapture.common.dp.ContextVariables;
import rapture.common.dp.ExecutionContext;
import rapture.common.dp.ExecutionContextField;
import rapture.common.dp.ExecutionContextFieldStorage;
import rapture.common.dp.ExpectedArgument;
import rapture.common.dp.PassedArgument;
import rapture.common.dp.Step;
import rapture.common.dp.StepHelper;
import rapture.common.dp.StepRecord;
import rapture.common.dp.Transition;
import rapture.common.dp.WorkOrder;
import rapture.common.dp.WorkOrderArguments;
import rapture.common.dp.WorkOrderArgumentsStorage;
import rapture.common.dp.WorkOrderCancellation;
import rapture.common.dp.WorkOrderCancellationStorage;
import rapture.common.dp.WorkOrderDebug;
import rapture.common.dp.WorkOrderInitialArgsHash;
import rapture.common.dp.WorkOrderInitialArgsHashStorage;
import rapture.common.dp.WorkOrderPathBuilder;
import rapture.common.dp.WorkOrderStatus;
import rapture.common.dp.WorkOrderStorage;
import rapture.common.dp.Worker;
import rapture.common.dp.WorkerExecutionState;
import rapture.common.dp.WorkerStorage;
import rapture.common.dp.Workflow;
import rapture.common.dp.WorkflowHistoricalMetrics;
import rapture.common.dp.WorkflowStorage;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.dp.ArgsHashFactory;
import rapture.dp.DecisionProcessExecutorFactory;
import rapture.dp.InvocableUtils;
import rapture.dp.WorkOrderFactory;
import rapture.dp.metrics.WorkflowMetricsFactory;
import rapture.dp.semaphore.LockKeyFactory;
import rapture.dp.semaphore.WorkOrderSemaphore;
import rapture.dp.semaphore.WorkOrderSemaphoreFactory;
import rapture.kernel.dp.DpDebugReader;
import rapture.kernel.dp.ExecutionContextUtil;
import rapture.kernel.dp.StepRecordUtil;
import rapture.kernel.dp.WorkflowValidator;
import rapture.log.management.LogManagerConnection;
import rapture.log.management.LogReadException;
import rapture.log.management.SessionExpiredException;

public class DecisionApiImpl extends KernelBase implements DecisionApi {
    private static final String ERROR_LIST_CONSTANT = "errorList";
    private static final Logger logger = Logger.getLogger(DecisionApiImpl.class);
    private static final String FIRST_WORKER_ID = "0";
    private static final String LOCK_TIMEOUT_SECONDS = "LOCK_TIMEOUT_SECONDS";

    public DecisionApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    @Override
    public void putWorkflow(CallingContext context, Workflow workflow) {
        WorkflowValidator.validate(workflow);
        WorkflowStorage.add(new RaptureURI(workflow.getWorkflowURI(), Scheme.WORKFLOW), workflow, context.getUser(), "Define workflow");
    }

    @Override
    public Workflow getWorkflow(CallingContext context, String workflowURI) {
        RaptureURI addressURI = new RaptureURI(workflowURI, Scheme.WORKFLOW);
        return getWorkflow(addressURI);
    }

    private Workflow getWorkflow(RaptureURI uri) {
        return WorkflowStorage.readByAddress(uri.withoutDecoration());
    }

    @Override
    public List<Workflow> getAllWorkflows(CallingContext context) {
        return WorkflowStorage.readAll();
    }

    @Override
    public void addStep(CallingContext context, String workflowURI, Step step) {
        Workflow workflow = getWorkflowNotNull(context, workflowURI);
        List<Step> steps = workflow.getSteps();
        for (Step currStep : steps) {
            if (currStep.getName().equals(step.getName())) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                        String.format("Trying to add step that already exists: Step name '%s'", step.getName()));
            }
        }
        steps.add(step);
        WorkflowStorage.add(new RaptureURI(workflow.getWorkflowURI(), Scheme.WORKFLOW), workflow, context.getUser(), "Add step");
    }

    /**
     * Returns a workflow, or throws a {@link RaptureException} if no workflow exists at the given URI. This should only be used internally by methods that are
     * intended to modify an existing {@link Workflow} and require it to already be defined
     */
    private Workflow getWorkflowNotNull(CallingContext context, String workflowURI) {
        Workflow workflow = getWorkflow(context, workflowURI);
        if (workflow == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, String.format("Workflow does not exist: '%s'", workflowURI));
        } else {
            return workflow;
        }
    }

    private Workflow getWorkflowNotNull(CallingContext context, RaptureURI uri) {
        Workflow workflow = getWorkflow(uri);
        if (workflow == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, String.format("Workflow does not exist: '%s'", uri.toString()));
        } else {
            return workflow;
        }
    }

    @Override
    public void removeStep(CallingContext context, String workflowURI, String stepName) {
        Workflow workflow = getWorkflowNotNull(context, workflowURI);
        List<Step> steps = workflow.getSteps();
        int index = 0;
        boolean found = false;
        for (Step step : steps) {
            if (stepName.equals(step.getName())) {
                found = true;
                break;
            }
            index++;
        }
        if (found) {
            steps.remove(index);
            WorkflowStorage.add(new RaptureURI(workflow.getWorkflowURI(), Scheme.WORKFLOW), workflow, context.getUser(), "Remove step");
        }
    }

    @Override
    public void addTransition(CallingContext context, String workflowURI, String stepName, Transition transition) {
        Workflow workflow = getWorkflowNotNull(context, workflowURI);
        Step step = getStep(workflow, stepName);
        if (step != null) {
            step.getTransitions().add(transition);
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    String.format("Attempting to add a transition from non-existent step: '%s'", stepName));
        }

        WorkflowStorage.add(new RaptureURI(workflow.getWorkflowURI(), Scheme.WORKFLOW), workflow, context.getUser(), "Add transition");
    }

    public static Step getStep(Workflow workflow, String stepName) {
        String decodedName = StepHelper.decode(stepName);
        if (StepHelper.isImpliedStep(decodedName)) {
            Step impliedStep = new Step();
            impliedStep.setName(decodedName);
            impliedStep.setExecutable(decodedName);
            return impliedStep;
        }
        for (Step step : workflow.getSteps()) {
            if (decodedName.equals(step.getName())) {
                return step;
            }
        }
        return null;
    }

    @Override
    public void removeTransition(CallingContext context, String workflowURI, String stepName, String transitionName) {
        Workflow workflow = getWorkflowNotNull(context, workflowURI);
        Step step = getStep(workflow, stepName);
        if (step != null) {
            int index = 0;
            boolean found = false;
            List<Transition> transitions = step.getTransitions();
            for (Transition transition : transitions) {
                if (transitionName.equals(transition.getName())) {
                    found = true;
                    break;
                }
                index++;
            }

            if (found) {
                transitions.remove(index);
                WorkflowStorage.add(new RaptureURI(workflow.getWorkflowURI(), Scheme.WORKFLOW), workflow, context.getUser(), "Remove transition");
            }
        }
    }

    @Override
    public void deleteWorkflow(CallingContext context, String workflowURI) {
        RaptureURI addressURI = new RaptureURI(workflowURI, Scheme.WORKFLOW);
        WorkflowStorage.deleteByAddress(addressURI, context.getUser(), "Delete workflow");
    }

    private static final String WORK_ORDER_IDGEN_URI = new RaptureURI("//sys/dp/workOrder", Scheme.IDGEN).toString();

    @Override
    public String createWorkOrder(CallingContext context, String workflowURI, Map<String, String> contextMap) {
        return createWorkOrderP(context, workflowURI, contextMap, null).getUri();
    }

    @Override
    public CreateResponse createWorkOrderP(CallingContext context, String workflowURI, Map<String, String> argsMap, String appStatusNamePattern) {
        /*
         * Check that we can create the Work Order. If at capacity, return null
         */
        if (logger.isTraceEnabled()) logger.trace("createWorkOrder URI = " + workflowURI + "argsMap = " + argsMap);

        RaptureURI workflowUri = new RaptureURI(workflowURI, Scheme.WORKFLOW);
        workflowURI = workflowUri.toString();
        Workflow workflow = getWorkflowNotNull(context, workflowUri);
        String startStep = workflowUri.hasElement() ? workflowUri.getElement() : workflow.getStartStep();

        Kernel.getStackContainer().pushStack(context, workflowUri.toString());

        Map<String, String> contextMap = setupContextMap(workflow.getExpectedArguments(), argsMap);

        WorkOrderSemaphore semaphore = WorkOrderSemaphoreFactory.create(context, workflow.getSemaphoreType(), workflow.getSemaphoreConfig());
        Long startInstant = System.currentTimeMillis();
        String lockKey = null;
        try {
            lockKey = LockKeyFactory.createLockKey(workflow.getSemaphoreType(), workflow.getSemaphoreConfig(), workflow.getWorkflowURI(), contextMap);
            /**
             * Save the lock key so we can use it when releasing later
             */
            if (lockKey != null) {
                contextMap.put(ContextVariables.LOCK_KEY, lockKey);
            }
        } catch (RaptureException e) {
            CreateResponse ret = new CreateResponse();
            ret.setIsCreated(false);
            ret.setMessage(e.getMessage());
            return ret;
        }

        long timeout = semaphore.getTimeout();
        if (contextMap.containsKey(LOCK_TIMEOUT_SECONDS)) {
            timeout = Long.parseLong(contextMap.get(LOCK_TIMEOUT_SECONDS));
        }
        SemaphoreAcquireResponse response;
        if (timeout > 0) {
            response = semaphore.acquirePermit(workflow.getWorkflowURI(), startInstant, lockKey, timeout);
        } else {
            response = semaphore.tryAcquirePermit(workflow.getWorkflowURI(), startInstant, lockKey);
        }

        if (!response.getIsAcquired()) {
            List<String> existingWoDesc = new LinkedList<>();
            Set<String> existingStakeholderURIs = response.getExistingStakeholderURIs();
            for (String stakeHolderURI : existingStakeholderURIs) {
                String jobURI = getContextValue(context, stakeHolderURI, ContextVariables.PARENT_JOB_URI);
                if (jobURI != null) {
                    existingWoDesc.add(String.format("{workOrderURI=%s, created by jobURI=%s}", stakeHolderURI, jobURI));
                } else {
                    existingWoDesc.add(String.format("{workOrderURI=%s}", stakeHolderURI));
                }
            }
            String error = String
                    .format("Unable to acquire a permit for a new WorkOrder for Workflow %s, with lockKey %s. The lock is already being held by the following" +
                            " WorkOrder(s): %s",
                            workflowURI, lockKey, StringUtils.join(existingWoDesc, ", "));
            logger.warn(error);
            CreateResponse ret = new CreateResponse();
            ret.setIsCreated(false);
            ret.setMessage(error);
            return ret;
        } else {
            RaptureURI workOrderURI = new RaptureURI(response.getAcquiredURI());
            Kernel.getStackContainer().pushStack(context, workOrderURI.toString());

            // Register the separate pieces
            // 1. The WorkOrder object
            WorkOrder workOrder = new WorkOrder();

            workOrder.setWorkOrderURI(workOrderURI.toString());
            workOrder.setWorkflowURI(workflowURI);
            workOrder.setPriority(0);
            workOrder.setStartTime(startInstant);
            List<String> workerIds = Arrays.asList(FIRST_WORKER_ID);
            workOrder.setWorkerIds(workerIds);
            workOrder.setPendingIds(workerIds);
            workOrder.setSemaphoreConfig(workflow.getSemaphoreConfig());
            workOrder.setSemaphoreType(workflow.getSemaphoreType());
            workOrder.setStatus(WorkOrderExecutionState.NEW);

            WorkOrderStorage.add(workOrderURI, workOrder, context.getUser(), "Create work order");
            if (logger.isTraceEnabled()) logger.trace("workOrder = " + workOrder.debug());

            // 2. The ExecutionContext
            ExecutionContext executionContext = new ExecutionContext();
            Map<String, String> data = new HashMap<>();
            for (Entry<String, String> entry : contextMap.entrySet()) {
                data.put(entry.getKey(), ContextValueType.LITERAL.marker + entry.getValue());
            }

            executionContext.setWorkOrderURI(workOrder.getWorkOrderURI());
            executionContext.setData(data);
            ExecutionContextUtil.saveContext(executionContext, context.getUser(), "Create work order");
            if (logger.isTraceEnabled()) logger.trace("Execution Context is " + executionContext.debug() + " user = " + context.getUser());

            // 3. The WorkOrderArguments
            writeWorkOrderArguments(context, argsMap, workOrder);

            // 4. The Worker
            Worker worker = new Worker();
            worker.setWorkOrderURI(workOrder.getWorkOrderURI());
            worker.setId("" + FIRST_WORKER_ID);
            worker.setParent("");
            worker.setSiblingCount(1);
            worker.setSiblingPosition(0);
            worker.setWaitCount(0);
            worker.setStatus(WorkerExecutionState.READY);
            worker.setPriority(workOrder.getPriority());
            worker.setCallingContext(context);
            worker.getLocalView().add(workflow.getView());
            RaptureURI qualifiedURI = new RaptureURI(workflowURI, Scheme.WORKFLOW);
            String activityId = Kernel.getActivity().createActivity(context, qualifiedURI.toString(), "Started", 0L, 100L);
            worker.setActivityId(activityId);

            String appStatusURI = InvocableUtils.createAppStatusName(context, workflow, worker, appStatusNamePattern);
            worker.getAppStatusNameStack().add(0, appStatusURI);

            List<String> stack = worker.getStack();
            String encodedName = StepHelper.encode(startStep);
            stack.add(RaptureURI.builder(new RaptureURI(workflowURI, Scheme.WORKFLOW)).element(encodedName).asString());
            WorkerStorage.add(worker, context.getUser(), "Create work order");
            if (logger.isTraceEnabled()) logger.trace("worker = " + worker.debug());

            // 4. The workorder initial args hash
            String hashValue = ArgsHashFactory.createHashValue(contextMap);
            WorkOrderInitialArgsHash argsHash = new WorkOrderInitialArgsHash();
            argsHash.setWorkOrderURI(workOrderURI.toString());
            argsHash.setHashValue(hashValue);
            WorkOrderInitialArgsHashStorage.add(argsHash, context.getUser(), "Created work order");

            // start the work order
            DecisionProcessExecutorFactory.getDefault().start(context, worker);
            Kernel.getStackContainer().popStack(context);
            Kernel.getStackContainer().popStack(context);
            CreateResponse ret = new CreateResponse();
            String order = workOrderURI.toString();
            ret.setIsCreated(true);
            ret.setUri(order);

            return ret;
        }
    }

    private static void writeWorkOrderArguments(CallingContext context, Map<String, String> argsMap, WorkOrder workOrder) {
        WorkOrderArguments workOrderArguments = new WorkOrderArguments();
        workOrderArguments.setWorkOrderURI(workOrder.getWorkOrderURI());
        if (argsMap != null) {
            for (Entry<String, String> entry : argsMap.entrySet()) {
                PassedArgument arg = new PassedArgument();
                arg.setName(entry.getKey());
                arg.setValue(entry.getValue());
                workOrderArguments.getArguments().add(arg);
            }
        }
        WorkOrderArgumentsStorage.add(workOrderArguments, context.getUser(), "Create work order");
    }

    private Map<String, String> setupContextMap(List<ExpectedArgument> expectedArguments, Map<String, String> argsMap) {
        HashMap<String, String> contextMap;
        if (argsMap == null) {
            contextMap = new HashMap<>();
        } else {
            contextMap = Maps.newHashMap(argsMap);
        }

        long timestamp = System.currentTimeMillis();
        if (!contextMap.containsKey(ContextVariables.TIMESTAMP)) {
            contextMap.put(ContextVariables.TIMESTAMP, timestamp + "");
        }
        if (!contextMap.containsKey(ContextVariables.LOCAL_DATE)) {
            DateTimeZone timezone = DateTimeZone.UTC;
            LocalDate ld = new LocalDate(timestamp, timezone);
            contextMap.put(ContextVariables.LOCAL_DATE, ContextVariables.FORMATTER.print(ld));
        }

        for (ExpectedArgument expectedArgument : expectedArguments) {
            if (!contextMap.containsKey(expectedArgument.getName()) && !StringUtils.isBlank(expectedArgument.getDefaultValue())) {
                contextMap.put(expectedArgument.getName(), expectedArgument.getDefaultValue());
            }
        }

        return contextMap;
    }

    @Override
    public void releaseWorkOrderLock(CallingContext context, String workOrderURI) {
        releaseWorkOrderLock(context, WorkOrderFactory.getWorkOrderNotNull(context, workOrderURI));
    }

    public void releaseWorkOrderLock(CallingContext ctx, WorkOrder workOrder) {
        WorkOrderSemaphore semaphore = WorkOrderSemaphoreFactory.create(ctx, workOrder.getSemaphoreType(), workOrder.getSemaphoreConfig());
        String lockKey = getContextValue(ctx, workOrder.getWorkOrderURI(), ContextVariables.LOCK_KEY);
        semaphore.releasePermit(workOrder.getWorkOrderURI(), lockKey);
    }

    public RaptureURI generateWorkOrderURI(CallingContext context, String workflowURI, Long startInstant) {
        // use epoch at start of day as authority
        String authority = getStartOfDayEpoch(startInstant);
        String id = Kernel.getIdGen().nextIds(context, WORK_ORDER_IDGEN_URI, 1L);
        String docPath = new RaptureURI(workflowURI, Scheme.WORKFLOW).getFullPath() + "/" + id;
        return RaptureURI.builder(Scheme.WORKORDER, authority).docPath(docPath).build();
    }

    private String getStartOfDayEpoch(Long startInstant) {
        DateTime dateTime = new DateTime(startInstant, DateTimeZone.UTC);
        return "" + dateTime.withTimeAtStartOfDay().toInstant().getMillis() / 1000;
    }

    @Override
    public WorkOrderStatus getWorkOrderStatus(CallingContext context, String workOrderURI) {

        if (workOrderURI == null) return null;
        WorkOrder workOrder = WorkOrderFactory.getWorkOrderNotNull(context, workOrderURI);
        WorkOrderStatus retVal = new WorkOrderStatus();
        retVal.setStatus(workOrder.getStatus());
        List<String> workerIds = workOrder.getWorkerIds();
        Map<String, String> workerOutput = workOrder.getOutputs();
        if (workerOutput == null) {
            workerOutput = new HashMap<>();
        }
        retVal.setWorkerOutput(workerOutput);

        DocApiImpl docApi = Kernel.getDoc().getTrusted();
        String outputUri = RaptureURI.newScheme(workOrderURI, Scheme.DOCUMENT).toShortString();

        for (String workerId : workerIds) {
            String id = outputUri + "#" + workerId;
            if (!workerOutput.containsKey(id)) {
                String str = docApi.getDocEphemeral(context, outputUri);
                if (str != null) {
                    Map<String, Object> map = JacksonUtil.getMapFromJson(str);
                    Object out = map.get(id);
                    if (out != null) workerOutput.put(id, out.toString());
                }
            }
        }
        return retVal;
    }

    @Override
    public void cancelWorkOrder(CallingContext context, String workOrderURI) {
        if (this.wasCancelCalled(context, workOrderURI)) return;
        String nakedUri = null;
        try {
            RaptureURI uri = new RaptureURI(workOrderURI, Scheme.WORKORDER);
            nakedUri = uri.withoutDecoration().toString();
        } catch (Exception ex) {
            throw RaptureExceptionFactory.create("Unparseable URI");
        }
        WorkOrderCancellation woc = new WorkOrderCancellation();
        woc.setTime(System.currentTimeMillis());
        woc.setWorkOrderURI(nakedUri);
        WorkOrderCancellationStorage.add(woc, context.getUser(), "Try to Cancel");
    }

    /**
     * resumeWorkOrder acts like createWorkOrder except that we must get the context from the previous order
     *
     * @param context
     * @param workOrderURI
     * @param workflowStepURI
     * @return CreateResponse object - same as Create Work Order
     */
    @Override
    public CreateResponse resumeWorkOrder(CallingContext context, String workOrderURI, String workflowStepURI) {
        if (workflowStepURI == null) {
            // Error?
            throw RaptureExceptionFactory.create("Workflow Step to resume from must be specified - cannot be null");
        }
        try {
            // Sanity check. Don't try to resume a flow at a step from a
            // different work order - that would be silly.
            // Is this valid in all cases?
            RaptureURI stepURI = new RaptureURI(workflowStepURI, Scheme.WORKFLOW);
            String stepName = stepURI.getElement();
            if ((stepName == null) || stepName.isEmpty()) {
                throw RaptureExceptionFactory.create("Workflow Step to resume from must be specified - element missing");
            }
            RaptureURI orderURI = new RaptureURI(workOrderURI, Scheme.WORKORDER);
            if (!orderURI.getDocPath().startsWith(stepURI.getShortPath())) {
                throw RaptureExceptionFactory.create("Step " + workflowStepURI + " does not match work order " + workOrderURI);
            }
        } catch (Exception ex) {
            throw RaptureExceptionFactory.create("Not a valid Workflow URI " + workflowStepURI);
        }

        Map<String, String> contextMap = new HashMap<>();
        RaptureURI uri = new RaptureURI(workOrderURI, Scheme.WORKORDER);
        List<ExecutionContextField> ecfs = ExecutionContextFieldStorage.readAll(uri.getFullPath());
        for (ExecutionContextField ecf : ecfs) {
            String value = ecf.getValue();
            contextMap.put(ecf.getVarName(), (value.charAt(0) == '#') ? value.substring(1) : value);
        }

        // How do we recover the original appStatus?
        // I don't see a way - so let's try this:
        String appStatusNamePattern = null;

        // Get the appStatusNameStack from the old work order
        Worker worker = WorkerStorage.readByFields(workOrderURI, "0");
        if (worker != null) {
            List<String> appStatusNameStack = worker.getAppStatusNameStack();
            if ((appStatusNameStack != null) && !appStatusNameStack.isEmpty()) {
                appStatusNamePattern = appStatusNameStack.get(0);
                if ((appStatusNamePattern != null) && (!appStatusNamePattern.startsWith("%"))) appStatusNamePattern = "%" + appStatusNamePattern;
            }
        }
        return createWorkOrderP(context, workflowStepURI, contextMap, appStatusNamePattern);

    }

    @Override
    public WorkOrderDebug getWorkOrderDebug(CallingContext context, String workOrderURI) {
        DpDebugReader reader = new DpDebugReader();
        return reader.getWorkOrderDebug(context, workOrderURI);
    }

    public List<Worker> getWorkers(WorkOrder workOrder) {
        List<String> workerIds = workOrder.getWorkerIds();
        List<Worker> workers = new ArrayList<>(workerIds.size());
        for (String workerId : workerIds) {
            workers.add(getWorkerNotNull(workOrder.getWorkOrderURI(), workerId));
        }
        return workers;
    }

    @Override
    public void setWorkOrderIdGenConfig(CallingContext context, String config, Boolean force) {
        if (force || !Kernel.getIdGen().idGenExists(context, WORK_ORDER_IDGEN_URI)) {
            Kernel.getIdGen().createIdGen(context, WORK_ORDER_IDGEN_URI, config);
        }
    }

    @Override
    public Step getWorkflowStep(CallingContext context, String stepURI) {
        RaptureURI uri = new RaptureURI(stepURI, Scheme.WORKFLOW);
        String stepName = uri.getElement();
        if (stepName == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    String.format("The Step URI passed in does not contain an 'element' indicating the step, but it requires one: [%s]", uri.toString()));
        }
        Workflow workflow = getWorkflowNotNull(context, stepURI);
        return getStep(workflow, stepName);
    }

    /**
     * Trusted method to get the workflow and the selected step as a pair.
     */
    public Pair<Workflow, Step> getWorkflowWithStep(CallingContext context, String stepURI) {
        RaptureURI uri = new RaptureURI(stepURI, Scheme.WORKFLOW);
        String stepName = uri.getElement();
        if (stepName == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    String.format("The Step URI passed in does not contain an 'element' indicating the step, but it requires one: [%s]", uri.toString()));
        }
        Workflow workflow = getWorkflowNotNull(context, stepURI);
        return ImmutablePair.of(workflow, getStep(workflow, stepName));
    }

    @Override
    public String getStepCategory(CallingContext context, String stepURI) {
        RaptureURI uri = new RaptureURI(stepURI, Scheme.WORKFLOW);
        Workflow workflow = getWorkflowNotNull(context, stepURI);
        if (uri.getElement() == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    String.format("The Step URI passed in does not contain an 'element' indicating the step, but it requires one: '%s'", uri.toString()));
        }
        Step step = getStep(workflow, uri.getElement());
        if (step == null) {
            throw RaptureExceptionFactory.create(String.format("Error! No step exists for URI %s", stepURI));
        } else if (step.getCategoryOverride() == null || "".equals(step.getCategoryOverride())) {
            return workflow.getCategory();
        } else {
            return step.getCategoryOverride();
        }
    }

    @Override
    public void setContextTemplate(CallingContext context, String workerURI, String varAlias, String literalValue) {
        setContextValue(context, workerURI, varAlias, ContextValueType.TEMPLATE, literalValue);
    }

    @Override
    public void setContextLiteral(CallingContext context, String workerURI, String varAlias, String literalValue) {
        setContextValue(context, workerURI, varAlias, ContextValueType.LITERAL, literalValue);
    }

    private void setContextValue(CallingContext context, String workerURI, String varAlias, ContextValueType type, String literalValue) {
        RaptureURI uri = new RaptureURI(workerURI, Scheme.WORKORDER);
        String workOrderUri = uri.toShortString();
        Map<String, String> view = getView(uri, workOrderUri);
        ExecutionContextUtil.setValueECF(context, workOrderUri, view, varAlias, type, literalValue);
    }

    private Map<String, String> getView(RaptureURI uri, String workOrderURI) {
        String workerId = uri.getElement();
        if (workerId == null) {
            workerId = FIRST_WORKER_ID;
        }
        Worker worker = WorkerStorage.readByFields(workOrderURI, workerId);
        Map<String, String> view;
        if (worker != null) {
            view = InvocableUtils.getLocalViewOverlay(worker);
        } else {
            view = new HashMap<>();
        }
        return view;
    }

    @Override
    public void setContextLink(CallingContext context, String workerURI, String varAlias, String expressionValue) {
        setContextValue(context, workerURI, varAlias, ContextValueType.LINK, expressionValue);
    }

    @Override
    public String getContextValue(CallingContext callingContext, String workerURI, String varAlias) {
        RaptureURI uri = new RaptureURI(workerURI, Scheme.WORKORDER);
        String workOrderURI = uri.toShortString();
        Map<String, String> view = getView(uri, workOrderURI);
        return ExecutionContextUtil.getValueECF(callingContext, workOrderURI, varAlias, view);
    }

    @Override
    public void addErrorToContext(CallingContext context, String workerURI, ErrorWrapper errorWrapper) {
        List<ErrorWrapper> errorList = getErrorsFromContext(context, workerURI);
        errorList.add(errorWrapper);
        String json = JacksonUtil.jsonFromObject(errorList);
        setContextLiteral(ContextFactory.getKernelUser(), workerURI, ERROR_LIST_CONSTANT, json);
    }

    @Override
    public List<ErrorWrapper> getErrorsFromContext(CallingContext context, String workerURI) {
        String json = getContextValue(ContextFactory.getKernelUser(), workerURI, ERROR_LIST_CONSTANT);
        List<ErrorWrapper> errorList;
        if (json == null) {
            errorList = new LinkedList<>();
        } else {
            errorList = JacksonUtil.objectFromJson(json, new TypeReference<List<ErrorWrapper>>() {
            });
        }
        return errorList;
    }

    @Override
    public List<ErrorWrapper> getExceptionInfo(CallingContext context, String workOrderURI) {
        WorkOrder order = getWorkOrder(context, workOrderURI);
        List<ErrorWrapper> list = new LinkedList<>();
        if (order != null) {
            List<Worker> workers = getWorkers(order);
            for (Worker worker : workers) {
                ErrorWrapper workerEx = worker.getExceptionInfo();
                if (workerEx != null) {
                    list.add(workerEx);
                }
                for (StepRecord stepRecord : StepRecordUtil.getStepRecords(worker)) {
                    ErrorWrapper stepEx = stepRecord.getExceptionInfo();
                    if (stepEx != null) {
                        list.add(stepEx);
                    }
                }
            }
        } else {
            throw RaptureExceptionFactory.create(HttpStatus.SC_BAD_REQUEST, String.format("WorkOrder not found for URI: [%s]", workOrderURI));
        }
        return list;
    }

    @Override
    public void reportStepProgress(CallingContext context, String workerURI, Long stepStartTime, String message, Long progress, Long max) {
        RaptureURI uri = new RaptureURI(workerURI, Scheme.WORKORDER);
        String workOrderURI = uri.toShortString();
        String workerId = uri.getElement();
        if (workerId == null) {
            throw RaptureExceptionFactory
                    .create(HttpStatus.SC_BAD_REQUEST, String.format("Bad uri passed, a workerURI must contain the worker id: [%s]", workerURI));
        }
        Optional<StepRecord> stepRecordOptional = StepRecordUtil.getRecord(workOrderURI, workerId, stepStartTime);
        if (stepRecordOptional.isPresent()) {
            StepRecord stepRecord = stepRecordOptional.get();
            String activityId = stepRecord.getActivityId();
            boolean isFinished = progress.longValue() == max.longValue();
            if (activityId == null) {
                activityId = Kernel.getActivity().createActivity(context, stepRecord.getStepURI(), message, progress, max);
                if (isFinished) {
                    Kernel.getActivity().finishActivity(context, activityId, message);
                }
                stepRecord.setActivityId(activityId);
                StepRecordUtil.writeStepRecord(context, workOrderURI, workerId, stepRecord, "reportStepProgress(): adding activity id");
            } else {
                if (isFinished) {
                    Kernel.getActivity().finishActivity(context, activityId, message);
                } else {
                    Kernel.getActivity().updateActivity(context, activityId, message, progress, max);
                }
            }
            InvocableUtils.writeWorkflowAuditEntry(context, workerURI, message + String.format("; progress is %s of %s", progress, max), false);
        } else {
            log.error(String.format("Step record not found; workOrderURI [%s], workerId [%s], stepStartTime [%s]", workOrderURI, workerId, stepStartTime));
        }
    }

    private Worker getWorkerNotNull(String workOrderURI, String workerId) {
        Worker found = WorkerStorage.readByFields(workOrderURI, workerId);
        if (found == null) {
            throw RaptureExceptionFactory.create(String.format("No worker found for id: '%s'", workerId));
        } else {
            return found;
        }
    }

    @Override
    public Worker getWorker(CallingContext context, String workOrderURI, String workerId) {
        return WorkerStorage.readByFields(workOrderURI, workerId);
    }

    @Override
    public List<RaptureFolderInfo> getWorkflowChildren(CallingContext context, String parentFolderPath) {
        return WorkflowStorage.getChildren(parentFolderPath);
    }

    @Override
    public List<RaptureFolderInfo> getWorkOrderChildren(CallingContext context, String parentFolderPath) {
        return WorkOrderStorage.getChildren(parentFolderPath);
    }

    @Override
    public WorkOrder getWorkOrder(CallingContext context, String workOrderURI) {
        if (StringUtils.isBlank(workOrderURI)) {
            throw RaptureExceptionFactory.create(HttpStatus.SC_BAD_REQUEST, String.format("Invalid workOrderURI: [%s]", workOrderURI));
        }
        RaptureURI addressURI = new RaptureURI(workOrderURI, Scheme.WORKORDER).withoutDecoration();
        return WorkOrderStorage.readByAddress(addressURI);
    }

    @Override
    public List<WorkOrder> getWorkOrdersByDay(CallingContext context, Long startTimeInstant) {
        String startDate = getStartOfDayEpoch(startTimeInstant);
        return WorkOrderStorage.readAll(startDate);
    }

    @Override
    public Map<String, List<String>> getWorkOrderStatusesByWorkflow(CallingContext context, Long startTimeInstant, String workflowUri) {
        Map<String, List<String>> ret = new HashMap<>();
        List<WorkOrder> wos = getWorkOrderObjectsByWorkflow(context, startTimeInstant, workflowUri);
        for (WorkOrder wo : wos) {
            String status = wo.getStatus().toString();
            List<String> workorders = ret.get(status);
            if (workorders == null) {
                ret.put(status, new ArrayList<>(Arrays.asList(wo.getWorkOrderURI())));
            } else {
                workorders.add(wo.getWorkOrderURI());
            }
        }
        return ret;
    }

    @Override
    public List<String> getWorkOrdersByWorkflow(CallingContext context, Long startTimeInstant, String workflowUri) {
        List<String> ret = new ArrayList<>();
        List<WorkOrder> wos = getWorkOrderObjectsByWorkflow(context, startTimeInstant, workflowUri);
        for (WorkOrder wo : wos) {
            ret.add(wo.getWorkOrderURI());
        }
        return ret;
    }

    private List<WorkOrder> getWorkOrderObjectsByWorkflow(CallingContext context, Long startTimeInstant, String workflowUri) {
        List<WorkOrder> ret = new ArrayList<>();
        // if the document://WorkOrder repo doesn't exist, aka no workflows have been run yet, just return empty
        if (!Kernel.getDoc().docRepoExists(context, WorkOrderPathBuilder.getRepoName())) {
            return ret;
        }
        if (startTimeInstant == null) {
            startTimeInstant = 0L;
        }
        DateTime startDate = new DateTime(startTimeInstant, DateTimeZone.UTC).withTimeAtStartOfDay();
        DateTime nowDate = new DateTime(System.currentTimeMillis(), DateTimeZone.UTC).withTimeAtStartOfDay();
        RaptureURI uri = new RaptureURI(workflowUri, Scheme.WORKFLOW);
        log.info(String.format("Requested startDate is [%s] and current date is [%s]", startDate.toString(), nowDate.toString()));

        final String workOrderPrefix = new WorkOrderPathBuilder().buildStorageLocation().toString();
        Map<String, RaptureFolderInfo> existingTimes = Kernel.getDoc().listDocsByUriPrefix(context, workOrderPrefix, 1);
        Map<String, RaptureFolderInfo> existingTimesWithAuthority = Kernel.getDoc().listDocsByUriPrefix(context, workOrderPrefix, 2);

        for (Map.Entry<String, RaptureFolderInfo> entry : existingTimes.entrySet()) {
            DateTime potentialTimestamp = new DateTime(Long.parseLong(entry.getValue().getName()) * 1000, DateTimeZone.UTC);
            // check if the timestamp is within range and also if there is an matching workflow authority
            if (!startDate.isAfter(potentialTimestamp) && existingTimesWithAuthority.containsKey(String.format("%s%s/", entry.getKey(), uri.getAuthority()))) {
                String prefix = String.format("%s/%s/%s", entry.getValue().getName(), uri.getAuthority(), uri.getDocPath());
                ret.addAll(WorkOrderStorage.readAll(prefix));
            }
        }
        return ret;
    }

    @Override
    public WorkOrder getWorkOrderByJobExec(CallingContext context, RaptureJobExec raptureJobExec) {
        if (raptureJobExec == null) {
            throw RaptureExceptionFactory.create("Invalid jobexec");
        }
        String execDetails = raptureJobExec.getExecDetails();
        if (StringUtils.isNotBlank(execDetails)) {
            return getWorkOrder(context, JacksonUtil.objectFromJson(execDetails, WorkflowJobDetails.class).getWorkOrderURI());
        }
        return null;
    }

    @Override
    public Map<RaptureJobExec, WorkOrder> getJobExecsAndWorkOrdersByDay(CallingContext context, Long startTimeInstant) {
        Map<RaptureJobExec, WorkOrder> ret = new HashMap<>();
        List<WorkOrder> workOrders = getWorkOrdersByDay(context, startTimeInstant);
        for (WorkOrder workOrder : workOrders) {
            // we find the associated RaptureJobExec using the built-in execution context params
            String jobUri = ExecutionContextUtil.getValueECF(context, workOrder.getWorkOrderURI(), ContextVariables.PARENT_JOB_URI, null);
            // this will be blank if its a workorder that was not initiated by a job, so we skip those
            if (!StringUtils.isBlank(jobUri)) {
                String execTime = ExecutionContextUtil.getValueECF(context, workOrder.getWorkOrderURI(), ContextVariables.TIMESTAMP, null);
                log.info(String.format("Retrieving job exec for job uri [%s] and time [%s]", jobUri, execTime));
                ret.put(RaptureJobExecStorage.readByFields(jobUri, Long.valueOf(execTime)), workOrder);
            }
        }
        return ret;
    }

    @Override
    public Boolean wasCancelCalled(CallingContext context, String workOrderURI) {
        String nakedUri;
        try {
            RaptureURI uri = new RaptureURI(workOrderURI, Scheme.WORKORDER);
            nakedUri = uri.withoutDecoration().toString();
        } catch (Exception ex) {
            throw RaptureExceptionFactory.create("Unparseable URI");
        }
        try {
            WorkOrderCancellation woc = WorkOrderCancellationStorage.readByFields(nakedUri);
            return woc != null;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public WorkOrderCancellation getCancellationDetails(CallingContext context, String workOrderURI) {
        try {
            return WorkOrderCancellationStorage.readByFields(workOrderURI);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public List<AppStatus> getAppStatuses(CallingContext context, String prefix) {
        AppStatusGroup sample = new AppStatusGroup();
        sample.setName(prefix);
        String encodedPrefix = sample.getStoragePath();
        List<AppStatusGroup> groups = AppStatusGroupStorage.readAll(encodedPrefix);
        List<AppStatus> ret = new LinkedList<>();
        for (AppStatusGroup group : groups) {
            ret.addAll(group.getIdToStatus().values());
        }
        // TODO remove line below, it's just here so we read data stored prior
        // to migration
        ret.addAll(AppStatusStorage.readAll(encodedPrefix));
        return ret;

    }

    @Override
    public List<AppStatusDetails> getAppStatusDetails(CallingContext context, String prefix, List<String> extraContextValues) {
        List<AppStatus> statuses = getAppStatuses(context, prefix);
        List<AppStatusDetails> detailedList = new ArrayList<>(statuses.size());
        for (AppStatus status : statuses) {
            AppStatusDetails asd = new AppStatusDetails();
            asd.setAppStatus(status);
            String workOrderURI = status.getWorkOrderURI();
            String logURI = InvocableUtils.getWorkflowAuditLog(status.getName(), workOrderURI, null);
            asd.setLogURI(logURI);
            WorkOrder workOrder = WorkOrderFactory.getWorkOrderNotNull(context, workOrderURI);
            List<Worker> workers = getWorkers(workOrder);
            Map<String, List<StepRecord>> workerIdToSteps = new HashMap<>();
            for (String varAlias : extraContextValues) {
                String value = getContextValue(context, status.getWorkOrderURI(), varAlias);
                asd.getExtraContextValues().put(varAlias, value);
            }
            for (Worker worker : workers) {
                List<StepRecord> steps = StepRecordUtil.getStepRecords(worker);
                workerIdToSteps.put(worker.getId(), steps);
            }
            asd.setWorkerIdToSteps(workerIdToSteps);
            detailedList.add(asd);
        }
        return detailedList;
    }

    @Override
    public WorkflowHistoricalMetrics getMonthlyMetrics(CallingContext context, String workflowURIIn, String jobURIIn, String argsHashValue, String stateIn) {
        WorkOrderExecutionState state = WorkOrderExecutionState.valueOf(stateIn);

        if (StringUtils.isEmpty(workflowURIIn)) {
            throw RaptureExceptionFactory.create(HttpStatus.SC_BAD_REQUEST, "Workflow URI must be defined!");
        }

        RaptureURI workflowURI = new RaptureURI(workflowURIIn, Scheme.WORKFLOW);

        WorkflowHistoricalMetrics result = new WorkflowHistoricalMetrics();

        try {
            String workflowMetric = WorkflowMetricsFactory.createWorkflowMetricName(workflowURI, state);
            result.setWorkflowAverage(Kernel.getMetricsService().getMetricAverage(workflowMetric, new Period().withMonths(1)));
            Long workflowAverageCount = Kernel.getMetricsService().getMetricCount(workflowMetric, new Period().withMonths(1));

            result.setWorkflowMetricName(workflowMetric);

            if (!StringUtils.isEmpty(jobURIIn)) {
                RaptureURI jobURI = new RaptureURI(jobURIIn, Scheme.JOB);
                String jobMetric = WorkflowMetricsFactory.createJobMetricName(jobURI, state);
                Long averageCount = Kernel.getMetricsService().getMetricCount(jobMetric, new Period().withMonths(1));
                if (averageCount > 10 && workflowAverageCount > averageCount) {
                    // if this is too small, it's a negligible stat and we should use the workflow average instead
                    Double average = Kernel.getMetricsService().getMetricAverage(jobMetric, new Period().withMonths(1));
                    result.setJobAverage(average);
                }
                result.setJobMetricName(jobMetric);
            }

            if (!StringUtils.isEmpty(argsHashValue)) {
                String argsMetric = WorkflowMetricsFactory.createWorkflowWithArgsMetric(workflowURI, state, argsHashValue);
                Long averageCount = Kernel.getMetricsService().getMetricCount(argsMetric, new Period().withMonths(1));
                if (averageCount > 10 && workflowAverageCount > averageCount) {
                    // if this is too small, it's a negligible stat and we should use the workflow average instead
                    Double metricAverage = Kernel.getMetricsService().getMetricAverage(argsMetric, new Period().withMonths(1));
                    result.setWorkflowWithArgsAverage(metricAverage);
                }
                result.setArgsHashMetricName(argsMetric);
            }
        } catch (IOException e) {
            throw RaptureExceptionFactory.create("Error while getting average: " + e.getMessage(), e);
        }

        return result;
    }

    @Override
    public LogQueryResponse queryLogs(CallingContext context, String workOrderURI, Long startTime, Long endTime, Long keepAlive, Long bufferSize,
            String nextBatchId, String stepName, String stepStartTimeStr) {

        try {
            LogManagerConnection connection = Kernel.getLogManagerConnection();
            if (StringUtils.isBlank(nextBatchId)) {
                Long stepStartTimeLong = null;
                try {
                    if (!StringUtils.isBlank(stepStartTimeStr)) {
                        stepStartTimeLong = Long.parseLong(stepStartTimeStr);
                    }
                } catch (NumberFormatException e) {
                    log.error(ExceptionToString.format(e));
                }
                Optional<Long> stepStartTime = Optional.fromNullable(stepStartTimeLong);
                nextBatchId = connection
                        .searchByWorkOrder(workOrderURI, Optional.fromNullable(StringUtils.trimToNull(stepName)), stepStartTime, startTime, endTime,
                                keepAlive.intValue(),
                                bufferSize.intValue());
            }
            return connection.getScrollingResults(nextBatchId, keepAlive.intValue());
        } catch (LogReadException e) {
            log.error(ExceptionToString.format(e));
            throw RaptureExceptionFactory.create(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Error while reading from the log server.", e);
        } catch (SessionExpiredException e) {
            log.error(ExceptionToString.format(e));
            throw RaptureExceptionFactory.create(HttpStatus.SC_BAD_REQUEST,
                    String.format("Error reading logs, passed in expired batch id [%s].", nextBatchId), e);
        }
    }

    @Override
    public void writeWorkflowAuditEntry(CallingContext context, String workOrderURI, String message, Boolean error) {
        InvocableUtils.writeWorkflowAuditEntry(context, workOrderURI, message, error);
    }

}
