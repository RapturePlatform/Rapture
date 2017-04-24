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
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import rapture.common.CallingContext;
import rapture.common.QueueSubscriber;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.WorkOrderExecutionState;
import rapture.common.dp.ContextVariables;
import rapture.common.dp.Step;
import rapture.common.dp.Transition;
import rapture.common.dp.Workflow;
import rapture.common.model.RaptureExchange;
import rapture.common.model.RaptureExchangeQueue;
import rapture.common.model.RaptureExchangeType;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.dp.WaitingTestHelper;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.Pipeline2ApiImpl;

public class WorkflowReturnTest {
    private static final String GET = "get";
    private static final String PUT = "put";
    private static final String OK = "ok";
    private static CallingContext ctx = ContextFactory.getKernelUser();
    private static Step kittyStep;
    private static Step putStep;
    private static Transition kittyTransition;
    private static Transition putTransition;
    private static Step subWorkflowStep;
    private static final String SCRIPT = "script:";
    private static final String AUTHORITY = "//branchrepo";
    private static final String HELLO_SCRIPT = SCRIPT + AUTHORITY + "/script/hello";
    private static final String KITTY_SCRIPT = SCRIPT + AUTHORITY + "/script/kitty";
    private static final String PUT_SCRIPT = SCRIPT + AUTHORITY + "/script/put";
    private static final String WORKFLOW = "workflow:";
    private static final String LITERAL_RETURN_WORKFLOW = WORKFLOW + AUTHORITY + "/literalReturnWorkflow";
    private static final String STRING_RETURN_WORKFLOW = WORKFLOW + AUTHORITY + "/stringReturnWorkflow";
    private static final String CONTEXT_VARIABLE_RETURN_WORKFLOW = WORKFLOW + AUTHORITY + "/contextVariableReturnWorkflow";
    private static final String VIEW_VARIABLE_RETURN_WORKFLOW = WORKFLOW + AUTHORITY + "/viewVariableReturnWorkflow";
    private static final String TEMPLATE_RETURN_WORKFLOW = WORKFLOW + AUTHORITY + "/templateReturnWorkflow";
    private static final String SUB_WORKFLOW = WORKFLOW + AUTHORITY + "/subWorkflow";
    private static final String EXCHANGE = "questionTestExchange";
    private static final String ALPHA = "alpha";
    private static final String KITTY_STEP = "kitty";
    private static QueueSubscriber subscriber = null;
    private static final int MAX_WAIT = 20000;

    @BeforeClass
    public static void setupClass() {
        RaptureConfig config = ConfigLoader.getConf();
        config.DefaultExchange = "PIPELINE {} USING GCP_PUBSUB { projectid=\"todo3-incap\"}";

        Kernel.initBootstrap();

        String idGenUri = "idgen://sys/event/id";
        if (!Kernel.getIdGen().idGenExists(ctx, idGenUri)) Kernel.getIdGen().createIdGen(ctx, idGenUri, "IDGEN {} USING MEMORY {}");
        if (!Kernel.getDoc().docRepoExists(ctx, AUTHORITY)) {
            Kernel.getDoc().createDocRepo(ctx, AUTHORITY, "NREP {} USING MEMORY {}");
        }
        String systemBlobRepo = "//sys.blob";
        Kernel.getBlob().deleteBlobRepo(ctx, systemBlobRepo);
        Kernel.getBlob().createBlobRepo(ctx, systemBlobRepo, ConfigLoader.getConf().DefaultSystemBlobConfig,
                ConfigLoader.getConf().DefaultSystemBlobFoldersConfig);
        subscriber = Kernel.INSTANCE.createAndSubscribe(ALPHA, "PIPELINE {} USING GCP_PUBSUB { projectid=\"todo3-incap\"}");
        createScripts();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (subscriber != null) Kernel.getPipeline2().unsubscribeQueue(ctx, subscriber);
    }

    @Before
    public void setup() {
        createStepsAndTransitions();
    }

    private static void createScripts() {
        Kernel.getScript().deleteScript(ctx, HELLO_SCRIPT);
        Kernel.getScript().createScript(
                ctx,
                HELLO_SCRIPT,
                RaptureScriptLanguage.REFLEX,
                RaptureScriptPurpose.PROGRAM,
                "return 'kitty';");
        Kernel.getScript().deleteScript(ctx, KITTY_SCRIPT);
        Kernel.getScript().createScript(
                ctx,
                KITTY_SCRIPT,
                RaptureScriptLanguage.REFLEX,
                RaptureScriptPurpose.PROGRAM,
                "println('KITTY');\n");
        Kernel.getScript().deleteScript(ctx, PUT_SCRIPT);
        Kernel.getScript().createScript(
                ctx,
                PUT_SCRIPT,
                RaptureScriptLanguage.REFLEX,
                RaptureScriptPurpose.PROGRAM,
                "workerURI = _params['$DP_WORKER_URI'];\n" +
                        "#decision.setContextLiteral(workerURI,'hello','kitty');\n" +
                        "#decision.setContextLiteral(workerURI,'bye','goodbye');\nreturn 'ok';");
    }

    private static void createStepsAndTransitions() {
        kittyTransition = new Transition();
        kittyTransition.setName("kitty");
        kittyTransition.setTargetStep(KITTY_STEP);

        putTransition = new Transition();
        putTransition.setName(OK);
        putTransition.setTargetStep(GET);

        putStep = new Step();
        putStep.setExecutable(PUT_SCRIPT);
        putStep.setName(PUT);
        putStep.setTransitions(Lists.newArrayList(putTransition));

        kittyStep = new Step();
        kittyStep.setExecutable(KITTY_SCRIPT);
        kittyStep.setName(KITTY_STEP);
        kittyStep.setTransitions(Lists.<Transition> newArrayList());

        subWorkflowStep = new Step();
        subWorkflowStep.setName("subWorkflow");
        subWorkflowStep.setTransitions(Lists.newArrayList(kittyTransition));
    }

    private static void initPipeline() {
        Kernel.getPipeline().setupStandardCategory(ctx, ALPHA);
        Kernel.getPipeline().registerExchangeDomain(ctx, "//questions", "EXCHANGE {} USING MEMORY {}");

        RaptureExchange exchange = new RaptureExchange();
        exchange.setName(EXCHANGE);
        exchange.setExchangeType(RaptureExchangeType.FANOUT);
        exchange.setDomain("questions");

        List<RaptureExchangeQueue> queues = new ArrayList<>();
        RaptureExchangeQueue queue = new RaptureExchangeQueue();
        queue.setName("default");
        queue.setRouteBindings(new ArrayList<String>());
        queues.add(queue);

        exchange.setQueueBindings(queues);

        Kernel.getPipeline().getTrusted().registerPipelineExchange(ctx, EXCHANGE, exchange);
        Kernel.getPipeline().getTrusted().bindPipeline(ctx, ALPHA, EXCHANGE, "default");
        Kernel.setCategoryMembership(ALPHA);
    }

    // Helper method, based on parameters create a Workflow, and then a WorkOrder from that Workflow, returns the URI of that WorkOrder.
    private String createWorkOrder(String initialStep, List<Step> steps, String workflowUri) {
        createWorkflow(initialStep, steps, workflowUri);

        String workOrderUri = Kernel.getDecision().createWorkOrder(ctx, workflowUri, null);
        assertNotNull(workOrderUri);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return workOrderUri;
    }

    // Helper method, based on parameters create a workflow
    private void createWorkflow(String initialStep, List<Step> steps, String workflowUri) {
        Workflow workflow = new Workflow();
        workflow.setCategory(ALPHA);
        workflow.setStartStep(initialStep);
        workflow.setSteps(steps);
        workflow.setWorkflowURI(workflowUri);
        Kernel.getDecision().putWorkflow(ctx, workflow);
    }

    // Helper method, create a workflow with subWorkflowStep, kittyStep, and then a workOrder from that workflow.
    private String createSubWorkflow() {
        List<Step> steps = Lists.newArrayList(subWorkflowStep, kittyStep);
        String workOrderUri = createWorkOrder("subWorkflow", steps, SUB_WORKFLOW);
        return workOrderUri;
    }

    @Test
    public void testReturnString() {
        Step returnStep = new Step();
        returnStep.setExecutable(HELLO_SCRIPT);
        returnStep.setName(GET);
        returnStep.setTransitions(Lists.<Transition> newArrayList());

        List<Step> steps = Lists.newArrayList(putStep, returnStep);
        createWorkflow(PUT, steps, STRING_RETURN_WORKFLOW);
        subWorkflowStep.setExecutable(STRING_RETURN_WORKFLOW);
        String workOrderUri = createSubWorkflow();
        assertNotNull(workOrderUri);
    }

    @Test
    public void testReturnLiteral() throws InterruptedException {
        Step returnStep = new Step();
        returnStep.setExecutable("$RETURN:#kitty");
        returnStep.setName(GET);
        returnStep.setTransitions(Lists.<Transition> newArrayList());

        List<Step> steps = Lists.newArrayList(putStep, returnStep);
        createWorkflow(PUT, steps, LITERAL_RETURN_WORKFLOW);
        subWorkflowStep.setExecutable(LITERAL_RETURN_WORKFLOW);
        String workOrderUri = createSubWorkflow();
        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {
                assertEquals(WorkOrderExecutionState.FINISHED, Kernel.getDecision().getWorkOrderStatus(ctx, workOrderUri).getStatus());
            }
        }, MAX_WAIT);
    }

    // Without a special character, return value should be treated as a literal not a variable.
    @Test
    public void testReturnNoSpecifier() throws InterruptedException {
        Step returnStep = new Step();
        returnStep.setExecutable("$RETURN:kitty");
        returnStep.setName(GET);
        returnStep.setTransitions(Lists.<Transition> newArrayList());

        List<Step> steps = Lists.newArrayList(putStep, returnStep);
        createWorkflow(PUT, steps, LITERAL_RETURN_WORKFLOW);
        subWorkflowStep.setExecutable(LITERAL_RETURN_WORKFLOW);
        String workOrderUri = createSubWorkflow();
        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {
                assertEquals(WorkOrderExecutionState.FINISHED, Kernel.getDecision().getWorkOrderStatus(ctx, workOrderUri).getStatus());
            }
        }, MAX_WAIT);
    }

    // Test with context variable hello set to "kitty".
    @Test
    public void testReturnContextVariable() throws InterruptedException {
        Step returnStep = new Step();
        returnStep.setExecutable("$RETURN:$hello");
        returnStep.setName(GET);
        returnStep.setTransitions(Lists.<Transition> newArrayList());

        List<Step> steps = Lists.newArrayList(putStep, returnStep);
        createWorkflow(PUT, steps, CONTEXT_VARIABLE_RETURN_WORKFLOW);

        subWorkflowStep.setExecutable(CONTEXT_VARIABLE_RETURN_WORKFLOW);
        String workOrderUri = createSubWorkflow();
        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {
                assertEquals(WorkOrderExecutionState.FINISHED, Kernel.getDecision().getWorkOrderStatus(ctx, workOrderUri).getStatus());
            }
        }, MAX_WAIT);
    }

    // Test with reserved name context variable.
    @Test
    public void testReturnReservedVariable() throws InterruptedException {
        long timestamp = System.currentTimeMillis();
        DateTimeZone timezone = DateTimeZone.UTC;
        LocalDate ld = new LocalDate(timestamp, timezone);
        String date = ContextVariables.FORMATTER.print(ld);
        Step returnStep = new Step();
        returnStep.setExecutable("$RETURN:$$__date_string");
        returnStep.setName(GET);
        returnStep.setTransitions(Lists.<Transition> newArrayList());

        List<Step> steps = Lists.newArrayList(putStep, returnStep);
        kittyTransition.setName(date);
        createWorkflow(PUT, steps, CONTEXT_VARIABLE_RETURN_WORKFLOW);

        subWorkflowStep.setExecutable(CONTEXT_VARIABLE_RETURN_WORKFLOW);
        String workOrderUri = createSubWorkflow();
        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {
                assertEquals(WorkOrderExecutionState.FINISHED, Kernel.getDecision().getWorkOrderStatus(ctx, workOrderUri).getStatus());
            }
        }, MAX_WAIT);
    }

    // Test with context variable hello="kitty", view alias sanrio="$hello".
    @Test
    public void testReturnViewVariable() throws InterruptedException {
        Step returnStep = new Step();
        returnStep.setExecutable("$RETURN:$sanrio");
        returnStep.setName(GET);
        Map<String, String> view = ImmutableMap.of("sanrio", "$hello");
        ;
        returnStep.setView(view);
        returnStep.setTransitions(Lists.<Transition> newArrayList());

        List<Step> steps = Lists.newArrayList(putStep, returnStep);
        createWorkflow(PUT, steps, VIEW_VARIABLE_RETURN_WORKFLOW);
        subWorkflowStep.setExecutable(VIEW_VARIABLE_RETURN_WORKFLOW);
        String workOrderUri = createSubWorkflow();
        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {
                assertEquals(WorkOrderExecutionState.FINISHED, Kernel.getDecision().getWorkOrderStatus(ctx, workOrderUri).getStatus());
            }
        }, MAX_WAIT);
    }

    // Test template substitution with context variables bye="goodbye", hello="kitty", and view alias sanrio="$hello".
    @Test
    public void testReturnTemplate() throws InterruptedException {
        long timestamp = System.currentTimeMillis();
        DateTimeZone timezone = DateTimeZone.UTC;
        LocalDate ld = new LocalDate(timestamp, timezone);
        String date = ContextVariables.FORMATTER.print(ld);
        Step returnStep = new Step();
        returnStep.setExecutable("$RETURN:%temp${bye}${sanrio}${$__date_string}$$abc");
        returnStep.setName(GET);
        Map<String, String> view = ImmutableMap.of("sanrio", "$hello");
        ;
        returnStep.setView(view);
        returnStep.setTransitions(Lists.<Transition> newArrayList());

        List<Step> steps = Lists.newArrayList(putStep, returnStep);
        createWorkflow(PUT, steps, TEMPLATE_RETURN_WORKFLOW);
        subWorkflowStep.setExecutable(TEMPLATE_RETURN_WORKFLOW);
        kittyTransition.setName("tempgoodbyekitty" + date + "$abc");
        String workOrderUri = createSubWorkflow();
        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {
                assertEquals(WorkOrderExecutionState.FINISHED, Kernel.getDecision().getWorkOrderStatus(ctx, workOrderUri).getStatus());
            }
        }, MAX_WAIT);
    }
}
