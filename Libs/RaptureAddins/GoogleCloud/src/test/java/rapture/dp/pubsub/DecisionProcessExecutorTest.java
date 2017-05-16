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
package rapture.dp.pubsub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static rapture.dp.DPTestUtil.ALPHA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.QueueSubscriber;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.SemaphoreAcquireResponse;
import rapture.common.WorkOrderExecutionState;
import rapture.common.dp.ContextVariables;
import rapture.common.dp.SemaphoreType;
import rapture.common.dp.Step;
import rapture.common.dp.StepRecord;
import rapture.common.dp.Transition;
import rapture.common.dp.WorkOrderDebug;
import rapture.common.dp.WorkerDebug;
import rapture.common.dp.Workflow;
import rapture.common.dp.WorkflowBasedSemaphoreConfig;
import rapture.common.exception.ExceptionToString;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.RaptureExchange;
import rapture.common.model.RaptureExchangeQueue;
import rapture.common.model.RaptureExchangeType;
import rapture.common.pipeline.PipelineConstants;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.dp.WaitingTestHelper;
import rapture.dp.WorkflowFactory;
import rapture.dp.invocable.CheckPrerequisiteStep;
import rapture.dp.invocable.PrerequisiteConfig;
import rapture.dp.semaphore.LockKeyFactory;
import rapture.dp.semaphore.WorkOrderSemaphore;
import rapture.dp.semaphore.WorkOrderSemaphoreFactory;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.dp.StepRecordUtil;
import rapture.repo.google.LocalDataStoreTest;

public class DecisionProcessExecutorTest extends LocalDataStoreTest {
    private static final Logger log = Logger.getLogger(DecisionProcessExecutorTest.class);

    private static final int MAX_WAIT = 20000;
    public static final String CONST_ALIAS = "constAlias";
    public static final String SOME_CONSTANT = "SomeConstant";
    public static final String LINK_IN_VIEW = "linkInView";
    public static final String VARIABLE_ALIAS = "varAlias";
    public static final String CONTEXT_VARIABLE_1 = "contextVar1";
    public static final String CONTEXT_VARIABLE_2 = "contextVar2";
    private static final CallingContext CONTEXT = ContextFactory.getKernelUser();
    public static final String REPO_URI = "//dpdocrepo";
    private static final String scr1 = "script1";
    private static final String scr2 = "script2";
    private static final String scr3 = "script3";
    private static final String scr4 = "script4";
    private static QueueSubscriber subscriber = null;

    @AfterClass
    public static void cleanUp() {
        if (subscriber != null) {
            Kernel.getPipeline2().unsubscribeQueue(CONTEXT, subscriber);
            Kernel.getDoc().deleteDocRepo(CONTEXT, REPO_URI);
        }
    }

    @BeforeClass
    public static void setup() {
        RaptureConfig config = ConfigLoader.getConf();
        config.DefaultExchange = "PIPELINE {} USING GCP_PUBSUB { threads=\"5\"}";

        System.setProperty("LOGSTASH-ISENABLED", "false");
        try {
            Kernel.initBootstrap();
        } catch (Exception e) {
            String error = ExceptionToString.format(e);
            if (error.contains("The Application Default Credentials are not available.")) Assume.assumeNoException(e);
            throw e;
        }
        if (Kernel.getDoc().docRepoExists(CONTEXT, REPO_URI)) cleanUp();

        Kernel.getDoc().createDocRepo(CONTEXT, REPO_URI, "NREP {} USING MEMORY {}");

        subscriber = Kernel.INSTANCE.createAndSubscribe(ALPHA, "PIPELINE {} USING GCP_PUBSUB { threads=\"5\"");
        Kernel.getScript().deleteScript(CONTEXT, REPO_URI + "/" + scr1);
        Kernel.getScript().deleteScript(CONTEXT, REPO_URI + "/" + scr2);
        Kernel.getScript().deleteScript(CONTEXT, REPO_URI + "/" + scr3);
        Kernel.getScript().deleteScript(CONTEXT, REPO_URI + "/" + scr4);

        Kernel.getScript().createScript(CONTEXT, REPO_URI + "/" + scr1, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM,
                "println(\"Hello there\"); return \"success\";");
        Kernel.getScript().createScript(CONTEXT, REPO_URI + "/" + scr2, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM,
                "println(\"This is step2\"); return \"success\";");
        Kernel.getScript().createScript(CONTEXT, REPO_URI + "/" + scr3, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM,
                "println(this has bad syntax, it should error out;");
        Kernel.getScript().createScript(CONTEXT, REPO_URI + "/" + scr4, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, "return 'fail';");
    }

    private static void initPipeline() {
        Kernel.getPipeline().setupStandardCategory(CONTEXT, "alpha");
        Kernel.getPipeline().setupStandardCategory(CONTEXT, "beta");

        Kernel.getPipeline().registerExchangeDomain(CONTEXT, "//main", "EXCHANGE {} USING MEMORY {}");

        RaptureExchange exchange = new RaptureExchange();
        exchange.setName("kernel");
        exchange.setExchangeType(RaptureExchangeType.FANOUT);
        exchange.setDomain("main");

        List<RaptureExchangeQueue> queues = new ArrayList<>();
        RaptureExchangeQueue queue = new RaptureExchangeQueue();
        queue.setName("default");
        queue.setRouteBindings(new ArrayList<String>());
        queues.add(queue);

        exchange.setQueueBindings(queues);

        // Kernel.getPipeline().registerPipelineExchange(CONTEXT, "kernel",
        // exchange);
        // Kernel.getPipeline().bindPipeline(CONTEXT, "alpha", "kernel", "default");
        Kernel.setCategoryMembership("alpha");
    }

    @Test
    public void testRunAndCheckStatus() throws InterruptedException {
        String wuri = "workflow://myworkflow/x";
        List<Step> steps = new ArrayList<>();
        Step s1 = new Step();
        s1.setName("start");
        s1.setExecutable("script://" + REPO_URI + "/" + scr1);
        s1.setTransitions(WorkflowFactory.createTransition("success", "nextStep"));
        Step s2 = new Step();
        s2.setName("nextStep");
        s2.setExecutable("script://" + REPO_URI + "/" + scr2);
        steps.add(s1);
        steps.add(s2);

        Workflow workflow = new Workflow();
        String description = "This is a description of a workflow.  I am putting it in here to test";
        workflow.setDescription(description);
        workflow.setSteps(steps);
        workflow.setWorkflowURI(wuri);
        workflow.setStartStep("start");

        Kernel.getDecision().putWorkflow(CONTEXT, workflow);
        Workflow retWf = Kernel.getDecision().getWorkflow(CONTEXT, wuri);
        assertEquals(description, retWf.getDescription());
        final String workOrderUri = Kernel.getDecision().createWorkOrder(CONTEXT, wuri, null);
        assertNotNull(workOrderUri);
        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {
                assertEquals(WorkOrderExecutionState.FINISHED, Kernel.getDecision().getWorkOrderStatus(CONTEXT, workOrderUri).getStatus());
            }
        }, MAX_WAIT);
        List<StepRecord> records = getStepRecords(workOrderUri);
        assertEquals(2, records.size());
        for (StepRecord record : records) {
            assertEquals(WorkOrderExecutionState.FINISHED, record.getStatus());
        }
    }

    protected List<StepRecord> getStepRecords(String workOrderUri) {
        WorkOrderDebug debug = Kernel.getDecision().getWorkOrderDebug(CONTEXT, workOrderUri);
        List<StepRecord> records = new LinkedList<>();
        for (WorkerDebug workerDebug : debug.getWorkerDebugs()) {
            records.addAll(StepRecordUtil.getStepRecords(workerDebug.getWorker()));
        }
        return records;
    }

    @Test
    public void testResume() throws InterruptedException {
        String wuri = "workflow://myworkflow/later";
        List<Step> steps = new ArrayList<>();
        Step s1 = new Step();
        s1.setName("start");
        s1.setExecutable("script://" + REPO_URI + "/" + scr3);
        s1.setTransitions(WorkflowFactory.createTransition("success", "nextStep"));
        Step s2 = new Step();
        s2.setName("nextStep");
        s2.setExecutable("script://" + REPO_URI + "/" + scr2);
        steps.add(s1);
        steps.add(s2);

        Workflow workflow = new Workflow();
        workflow.setSteps(steps);
        workflow.setWorkflowURI(wuri);
        workflow.setStartStep("start");

        Kernel.getDecision().putWorkflow(CONTEXT, workflow);
        final String workOrderUri = Kernel.getDecision().createWorkOrder(CONTEXT, wuri + "#nextStep", null);
        assertNotNull(workOrderUri);
        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {
                assertEquals(WorkOrderExecutionState.FINISHED, Kernel.getDecision().getWorkOrderStatus(CONTEXT, workOrderUri).getStatus());
            }
        }, MAX_WAIT);
        List<StepRecord> records = getStepRecords(workOrderUri);
        assertEquals(1, records.size());
        for (StepRecord record : records) {
            assertEquals(WorkOrderExecutionState.FINISHED, record.getStatus());
        }
    }

    @Test
    public void testRunBadAndCheckStatus() throws InterruptedException {
        String wuri = "workflow://myworkflow/xBad";
        testBad(wuri, scr3);
    }

    @Test
    public void testFailAndCheckStatus() throws InterruptedException {
        String wuri = "workflow://myworkflow/xBad";
        testBad(wuri, scr4);
    }

    protected void testBad(String wuri, String src) throws InterruptedException {
        List<Step> steps = new ArrayList<>();
        Step s1 = new Step();
        s1.setName("start");
        s1.setExecutable("script://" + REPO_URI + "/" + scr1);
        s1.setTransitions(WorkflowFactory.createTransition("success", "nextStep"));
        Step s2 = new Step();
        s2.setName("nextStep");
        s2.setExecutable("script://" + REPO_URI + "/" + scr2);
        s2.setTransitions(WorkflowFactory.createTransition("success", "badStep"));
        Step s3 = new Step();
        s3.setName("badStep");
        s3.setExecutable("script://" + REPO_URI + "/" + src);
        s3.setTransitions(WorkflowFactory.createTransition("fail", "$FAIL"));
        steps.add(s1);
        steps.add(s2);
        steps.add(s3);

        Workflow workflow = new Workflow();
        workflow.setSteps(steps);
        workflow.setWorkflowURI(wuri);
        workflow.setStartStep("start");

        Kernel.getDecision().putWorkflow(CONTEXT, workflow);
        final String workOrderUri = Kernel.getDecision().createWorkOrder(CONTEXT, wuri, null);
        assertNotNull(workOrderUri);
        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {
                assertEquals(WorkOrderExecutionState.ERROR, Kernel.getDecision().getWorkOrderStatus(CONTEXT, workOrderUri).getStatus());
            }
        }, MAX_WAIT);
        List<StepRecord> records = getStepRecords(workOrderUri);
        assertEquals(3, records.size());
        for (StepRecord record : records) {
            if (record.getName().equals("badStep")) {
                assertEquals(WorkOrderExecutionState.ERROR, record.getStatus());
            } else {
                assertEquals(WorkOrderExecutionState.FINISHED, record.getStatus());
            }
        }
    }

    public static final String CONTEXT_LINK = new RaptureURI.Builder(Scheme.DOCUMENT, DecisionProcessExecutorTest.REPO_URI.replaceAll("/", ""))
            .docPath("/invocable/ping/doc").element("first").build().toString();
    private static final String LINK_EXPRESSION = "!" + CONTEXT_LINK;

    @Test
    public void testContextValues() throws InterruptedException {
        String wuri = "workflow://myworkflow/xCtx";
        List<Step> steps = new ArrayList<>();
        Step s1 = new Step();
        s1.setName("start");
        s1.setExecutable(new RaptureURI.Builder(Scheme.DP_JAVA_INVOCABLE, "DecisionTestInvocable").build().toString());
        Map<String, String> view = new HashMap<>();
        view.put(CONST_ALIAS, "#" + SOME_CONSTANT);
        view.put(LINK_IN_VIEW, LINK_EXPRESSION);
        view.put(VARIABLE_ALIAS, "$" + CONTEXT_VARIABLE_1);
        s1.setView(view);
        steps.add(s1);

        Workflow workflow = new Workflow();
        workflow.setSteps(steps);
        workflow.setWorkflowURI(wuri);
        workflow.setStartStep("start");

        Kernel.getDecision().putWorkflow(CONTEXT, workflow);
        final String workOrderUri = Kernel.getDecision().createWorkOrder(CONTEXT, wuri, null);
        assertNotNull(workOrderUri);
        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {
                assertEquals("Check the error log if this fails it means that the DecisionTestInvocable didn't execute successfully. There are some assertions"
                        + " in there.", WorkOrderExecutionState.FINISHED, Kernel.getDecision().getWorkOrderStatus(CONTEXT, workOrderUri).getStatus());
            }
        }, MAX_WAIT);
        List<StepRecord> records = getStepRecords(workOrderUri);
        assertEquals(1, records.size());
        for (StepRecord record : records) {
            assertEquals(WorkOrderExecutionState.FINISHED, record.getStatus());
        }
    }

    @Test
    public void testUnhandledCategory() throws InterruptedException {
        String wuri = "workflow://myworkflow/xUnhandled";
        List<Step> steps = new ArrayList<>();
        Step s1 = new Step();
        s1.setName("start");
        s1.setExecutable("script://" + REPO_URI + "/" + scr1);
        s1.setTransitions(WorkflowFactory.createTransition("success", "nextStep"));
        Step s2 = new Step();
        s2.setName("nextStep");
        s2.setExecutable("script://" + REPO_URI + "/" + scr2);
        s2.setTransitions(WorkflowFactory.createTransition("success", "badStep"));
        Step s3 = new Step();
        s3.setName("badStep");
        s3.setExecutable("script://" + REPO_URI + "/" + scr3);
        steps.add(s1);
        steps.add(s2);
        steps.add(s3);

        Workflow workflow = new Workflow();
        workflow.setSteps(steps);
        workflow.setCategory("unhandled");
        workflow.setWorkflowURI(wuri);
        workflow.setStartStep("start");

        Kernel.getDecision().putWorkflow(CONTEXT, workflow);
        final String workOrderUri = Kernel.getDecision().createWorkOrder(CONTEXT, wuri, null);
        assertNotNull(workOrderUri);
        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {
                assertEquals(WorkOrderExecutionState.NEW, Kernel.getDecision().getWorkOrderStatus(CONTEXT, workOrderUri).getStatus());
            }
        }, MAX_WAIT);
    }

    @Test
    public void testSemaphore() {
        String wuri = "workflow://myworkflow/xSemaphore";
        List<Step> steps = new ArrayList<>();
        Step s1 = new Step();
        s1.setName("start");
        s1.setExecutable("script://" + REPO_URI + "/" + scr1);
        steps.add(s1);

        Workflow workflow = new Workflow();
        workflow.setCategory("unhandled"); // don't want this to be handled
        // before we attempt to re-acquire
        // lock
        workflow.setSteps(steps);
        workflow.setWorkflowURI(wuri);
        workflow.setStartStep("start");
        workflow.setSemaphoreType(SemaphoreType.WORKFLOW_BASED);
        WorkflowBasedSemaphoreConfig config = new WorkflowBasedSemaphoreConfig();
        config.setMaxAllowed(2);
        workflow.setSemaphoreConfig(JacksonUtil.jsonFromObject(config));
        Kernel.getDecision().putWorkflow(CONTEXT, workflow);

        String workOrderUri1 = Kernel.getDecision().createWorkOrder(CONTEXT, wuri, null);
        assertNotNull(workOrderUri1);
        Map<String, String> overlay = new HashMap<>();
        overlay.put(ContextVariables.PARENT_JOB_URI, "job://some/job/uri");
        String workOrderUri2 = Kernel.getDecision().createWorkOrder(CONTEXT, wuri, overlay);
        assertNotNull(workOrderUri2);
        String workOrderUri3 = Kernel.getDecision().createWorkOrder(CONTEXT, wuri, null);
        assertNull(workOrderUri3);

    }

    @Ignore
    public void testSemaphore2() {
        String wuri = "workflow://myworkflow/xSemaphore2";
        List<Step> steps = new ArrayList<>();
        Step s1 = new Step();
        s1.setName("start");
        s1.setExecutable("script://" + REPO_URI + "/" + scr1);
        steps.add(s1);

        Workflow workflow = new Workflow();
        workflow.setCategory("unhandled"); // don't want this to be handled
        // before we attempt to re-acquire
        // lock
        workflow.setSteps(steps);
        workflow.setWorkflowURI(wuri);
        workflow.setStartStep("start");
        workflow.setSemaphoreType(SemaphoreType.WORKFLOW_BASED);
        WorkflowBasedSemaphoreConfig config = new WorkflowBasedSemaphoreConfig();
        config.setMaxAllowed(1);
        workflow.setSemaphoreConfig(JacksonUtil.jsonFromObject(config));
        Kernel.getDecision().putWorkflow(CONTEXT, workflow);

        WorkOrderSemaphore semaphore = WorkOrderSemaphoreFactory.create(CONTEXT, workflow.getSemaphoreType(), workflow.getSemaphoreConfig());
        String lockKey = LockKeyFactory.createLockKey(workflow.getSemaphoreType(), workflow.getSemaphoreConfig(), workflow.getWorkflowURI(), new HashMap<>());
        assertNotNull(lockKey);

        SemaphoreAcquireResponse response = semaphore.tryAcquirePermit(workflow.getWorkflowURI(), System.currentTimeMillis(), lockKey);
        if (!response.getIsAcquired()) {
            Set<String> existingStakeholderURIs = response.getExistingStakeholderURIs();
            for (String stakeHolderURI : existingStakeholderURIs) {
                System.out.println(String.format("{workOrderURI=%s, created by jobURI=%s}", stakeHolderURI,
                        Kernel.getDecision().getContextValue(CONTEXT, stakeHolderURI, ContextVariables.PARENT_JOB_URI)));
            }
        }
        assertTrue(response.getIsAcquired());
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        SemaphoreAcquireResponse response2 = semaphore.tryAcquirePermit(workflow.getWorkflowURI(), System.currentTimeMillis(), lockKey);
        assertFalse(response2.getIsAcquired());

        List<String> existingWoDesc = new LinkedList<>();
        Set<String> existingStakeholderURIs = response.getExistingStakeholderURIs();
        for (String stakeHolderURI : existingStakeholderURIs) {
            String jobURI = Kernel.getDecision().getContextValue(CONTEXT, stakeHolderURI, ContextVariables.PARENT_JOB_URI);
            if (jobURI != null) {
                existingWoDesc.add(String.format("{workOrderURI=%s, created by jobURI=%s}", stakeHolderURI, jobURI));
            } else {
                existingWoDesc.add(String.format("{workOrderURI=%s}", stakeHolderURI));
            }
        }

        System.out.println("lock " + lockKey + " is already being held by: " + StringUtils.join(existingWoDesc, ", "));
        semaphore.releasePermit(workflow.getWorkflowURI(), lockKey);
    }

    @Test
    public void testCheckPrerequisite() throws InterruptedException {
        // create config and series data
        String configUri = "//test_prerequisite/config";
        String seriesUri = "series://datacapture/HIST/MKIT/PMI_BR_MAN_OP";
        writeConfig(configUri, seriesUri);
        writeSeriesData(seriesUri);

        // create workflow
        String workflowUri = putWorkflow();
        Map<String, String> contextMap = new HashMap<>();
        contextMap.put(CheckPrerequisiteStep.CONFIG_URI, configUri);
        final String workOrderUri = Kernel.getDecision().createWorkOrder(CONTEXT, workflowUri, contextMap);

        // check workorder status
        assertNotNull(workOrderUri);
        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {
                assertEquals("Check the error log if this fails it means that the DecisionTestInvocable didn't execute successfully. There are some assertions"
                        + " in there.", WorkOrderExecutionState.FINISHED, Kernel.getDecision().getWorkOrderStatus(CONTEXT, workOrderUri).getStatus());
            }
        }, MAX_WAIT);
    }

    private void writeConfig(String configUri, String seriesUri) {
        PrerequisiteConfig.RequiredData data = new PrerequisiteConfig.RequiredData();
        data.setUri(seriesUri);
        data.setDateWithin("1D");

        PrerequisiteConfig config = new PrerequisiteConfig();
        config.setRequiredData(Arrays.asList(data));
        config.setRetryInMillis(500);
        config.setCutoffTime(getDateTimeString(2));
        config.setCutoffAction(PrerequisiteConfig.CutoffAction.START);

        String content = Kernel.getDoc().putDoc(CONTEXT, configUri, JacksonUtil.jsonFromObject(config));
        log.info("writeConfig, content result: " + content);
    }

    private String getDateTimeString(int seconds) {
        LocalTime localTime = LocalTime.now().plusSeconds(seconds);
        String timeZone = DateTimeZone.getDefault().getID();
        return String.format("%s %s", localTime, timeZone);
    }

    private void writeSeriesData(String uri) {
        Kernel.getSeries().deleteSeriesRepo(CONTEXT, "datacapture");
        Kernel.getSeries().createSeriesRepo(CONTEXT, "datacapture", "SREP {} USING MEMORY { }");
        String today = DateTime.now().toString(DateTimeFormat.forPattern("yyyyMMdd"));
        Kernel.getSeries().addLongToSeries(CONTEXT, uri.toString(), today, 123L);
    }

    private static String putWorkflow() {
        List<Step> steps = new ArrayList<>();
        Step step1 = new Step();
        step1.setName("firstStep");
        step1.setExecutable(new RaptureURI.Builder(Scheme.DP_JAVA_INVOCABLE, "CheckPrerequisiteStep").build().toString());
        List<Transition> transitions = WorkflowFactory.createTransition("next", "secondStep");
        transitions.addAll(WorkflowFactory.createTransition("quit", "$RETURN"));
        step1.setTransitions(transitions);
        steps.add(step1);

        Step step2 = new Step();
        step2.setName("secondStep");
        step2.setExecutable(new RaptureURI.Builder(Scheme.DP_JAVA_INVOCABLE, "AppStatus2").build().toString());
        steps.add(step2);

        String uri = "workflow://checkprerequisites";
        Workflow workflow = new Workflow();
        workflow.setSteps(steps);
        workflow.setWorkflowURI(uri);
        workflow.setStartStep("firstStep");
        workflow.setCategory(PipelineConstants.CATEGORY_ALPHA);

        Kernel.getDecision().deleteWorkflow(CONTEXT, uri);
        Kernel.getDecision().putWorkflow(CONTEXT, workflow);
        return uri;
    }
}
