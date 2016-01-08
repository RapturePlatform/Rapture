/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.WorkOrderExecutionState;
import rapture.common.dp.ExecutionContext;
import rapture.common.dp.Step;
import rapture.common.dp.Transition;
import rapture.common.dp.WorkOrderDebug;
import rapture.common.dp.Workflow;
import rapture.common.model.RaptureExchange;
import rapture.common.model.RaptureExchangeQueue;
import rapture.common.model.RaptureExchangeType;
import rapture.config.ConfigLoader;

import com.google.common.collect.Lists;

public class DecisionApiTest {
    private static CallingContext ctx = ContextFactory.getKernelUser();
    private static final String AUTHORITY = "//branchrepo";
    private static final String EXCHANGE = "questionTestExchange";
    private static final String ALPHA = "alpha";
    private static final String WORKFLOW = "workflow:";
    private static final String LITERAL_RETURN_WORKFLOW = WORKFLOW + AUTHORITY + "/literalReturnWorkflow";

    private static final String SCRIPT = "script:";
    private static final String PUT_SCRIPT = SCRIPT + AUTHORITY + "/script/put";
    private static final String PUT_SCRIPT_2 = SCRIPT + AUTHORITY + "/script/put2";

    @BeforeClass
    public static void setupClass() {
        Kernel.initBootstrap();
        if (!Kernel.getDoc().docRepoExists(ctx, AUTHORITY)) {
            Kernel.getDoc().createDocRepo(ctx, AUTHORITY, "NREP {} USING MEMORY {}");
        }
        String systemBlobRepo = "//sys.blob";
        Kernel.getBlob().deleteBlobRepo(ctx, systemBlobRepo);
        Kernel.getBlob().createBlobRepo(ctx, systemBlobRepo, ConfigLoader.getConf().DefaultSystemBlobConfig,
                ConfigLoader.getConf().DefaultSystemBlobFoldersConfig);
        initPipeline();
        Kernel.getScript().deleteScript(ctx, PUT_SCRIPT);
        Kernel.getScript().deleteScript(ctx, PUT_SCRIPT_2);

        Kernel.getScript().createScript(
                ctx,
                PUT_SCRIPT,
                RaptureScriptLanguage.REFLEX,
                RaptureScriptPurpose.PROGRAM,
                "workerURI = _params['$DP_WORKER_URI'];\n" +
                        "#decision.setContextLiteral(workerURI,'hello','kitty');\n" +
                        "#decision.setContextLiteral(workerURI,'bye','goodbye');\nreturn '#kitty';");
        Kernel.getScript().createScript(
                ctx,
                PUT_SCRIPT_2,
                RaptureScriptLanguage.REFLEX,
                RaptureScriptPurpose.PROGRAM,
                "workerURI = _params['$DP_WORKER_URI'];\n" +
                        "#decision.setContextLiteral(workerURI,'hello','sanrio');\n" +
                        "#decision.setContextLiteral(workerURI,'bye','adios');\nreturn '#kitty';");
    }

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void RAP2711() {
        String workerURI = "//1234567890/WO00000001";
        CallingContext context = ContextFactory.getKernelUser();
        ExecutionContext ec = new ExecutionContext();
        ec.setWorkOrderURI(workerURI);
        ec.setData(new HashMap<String, String>());
        Kernel.getDecision().setContextLiteral(context, workerURI, "FOO", "");
        String foo = Kernel.getDecision().getContextValue(context, workerURI, "FOO");
        Assert.assertEquals("", foo);
    }

    @Test
    public void testConcurrentSetContextLiteral() throws InterruptedException {
        final String workOrderUri = "//1234567890/WO00000001";
        final CallingContext context = ContextFactory.getKernelUser();
        // Used for number of threads to cause a collision. It seems to happen even with just 2 concurrent writes.
        final int SIZE = 10;
        ExecutionContext ec = new ExecutionContext();
        ec.setWorkOrderURI(workOrderUri);
        ec.setData(new HashMap<String, String>());
        Runnable[] threads = new Runnable[SIZE];
        int i;
        for (i = 0; i < SIZE; i++) {
            final String var = "var" + Integer.toString(i);
            threads[i] = new Runnable() {
                public void run() {
                    for (int j = 0; j < SIZE; j++) {
                        String varJ = var + "." + Integer.toString(j);
                        Kernel.getDecision().setContextLiteral(context, workOrderUri, varJ, varJ);
                    }
                }
            };
        }
        for (i = 0; i < SIZE; i++) {
            Thread t = new Thread(threads[i]);
            t.start();
        }
        Thread.sleep(1000);
        for (i = 0; i < SIZE; i++) {
            String var = "var" + Integer.toString(i);
            for (int j = 0; j < SIZE; j++) {
                String varJ = var + "." + Integer.toString(j);
                assertEquals(varJ, Kernel.getDecision().getContextValue(context, workOrderUri, varJ));
            }
        }
    }

    @Test
    public void testGetWorkOrderDebug() throws InterruptedException {
        Step returnStep = new Step();
        returnStep.setExecutable(PUT_SCRIPT);
        returnStep.setName("get");
        returnStep.setTransitions(Lists.<Transition> newArrayList());
        List<Step> steps = Lists.newArrayList(returnStep);
        String workOrderUri = createWorkOrder("get", steps, LITERAL_RETURN_WORKFLOW);

        assertEquals(WorkOrderExecutionState.FINISHED, Kernel.getDecision().getWorkOrderStatus(ctx, workOrderUri).getStatus());

        // run the workflow a second time with a different view definition. getWorkOrderDebug should only return values for the second run.
        returnStep.setExecutable(PUT_SCRIPT_2);
        workOrderUri = createWorkOrder("get", steps, LITERAL_RETURN_WORKFLOW + "2");
        WorkOrderDebug workOrderDebug = Kernel.getDecision().getWorkOrderDebug(ctx, workOrderUri);
        assertEquals(workOrderUri, workOrderDebug.getOrder().getWorkOrderURI());
        assertThat(workOrderDebug.getLogURI(), CoreMatchers.containsString("literalReturnWorkflow"));
        System.out.println("logURI:" + workOrderDebug.getLogURI());
        assertNull(workOrderDebug.getParentJobURI());
        assertEquals(1, workOrderDebug.getWorkerDebugs().size());

        ExecutionContext ec = workOrderDebug.getContext();
        assertNotNull(ec);
        assertNotNull(ec.getData());
        System.out.println("ec.getData()");
        for (String key : ec.getData().keySet()) {
            System.out.println("key: " + key + ", value: " + ec.getData().get(key));
        }
        assertEquals("#sanrio", ec.getData().get("hello"));
        assertEquals("#adios", ec.getData().get("bye"));
    }

    private static void initPipeline() {
        Kernel.getPipeline().setupStandardCategory(ctx, ALPHA);
        Kernel.getPipeline().registerExchangeDomain(ctx, "//questions", "EXCHANGE {} USING MEMORY {}");

        RaptureExchange exchange = new RaptureExchange();
        exchange.setName(EXCHANGE);
        exchange.setExchangeType(RaptureExchangeType.FANOUT);
        exchange.setDomain("questions");

        List<RaptureExchangeQueue> queues = new ArrayList<RaptureExchangeQueue>();
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
}
