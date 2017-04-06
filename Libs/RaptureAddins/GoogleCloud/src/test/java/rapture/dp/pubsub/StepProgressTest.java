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
import static org.junit.Assert.assertTrue;
import static rapture.dp.DPTestUtil.ALPHA;

import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rapture.common.Activity;
import rapture.common.ActivityStatus;
import rapture.common.CallingContext;
import rapture.common.CreateResponse;
import rapture.common.QueueSubscriber;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.WorkOrderExecutionState;
import rapture.common.dp.Step;
import rapture.common.dp.StepRecordDebug;
import rapture.common.dp.Transition;
import rapture.common.dp.WorkOrderDebug;
import rapture.common.dp.WorkerDebug;
import rapture.common.dp.Workflow;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.dp.WaitingTestHelper;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.Pipeline2ApiImpl;

/**
 * @author bardhi
 * @since 3/20/15.
 */
public class StepProgressTest {
    private static final int NUM_STEPS = 3;
    private static final String RFX_WORKFLOW_URI = "workflow://stepProgress/rfx";
    private static final String JAVA_WORKFLOW_URI = "workflow://stepProgress/java";
    private static final int MAX_WAIT = 20000;
    private static final String CATEGORY = "alpha";
    private CallingContext context;
    private QueueSubscriber subscriber = null;

    @Before
    public void setUp() throws Exception {
        Pipeline2ApiImpl.usePipeline2 = true;
        RaptureConfig config = ConfigLoader.getConf();
        config.DefaultExchange = "PIPELINE {} USING GCP_PUBSUB { projectid=\"todo3-incap\"}";

        Kernel.initBootstrap();
        context = ContextFactory.getKernelUser();

        subscriber = Kernel.INSTANCE.createAndSubscribe(ALPHA, "PIPELINE {} USING GCP_PUBSUB { projectid=\"todo3-incap\"}");

        createRfxWorkflow();
        createJavaWorkflow();

    }


    private void createJavaWorkflow() {
        String execPattern = "dp_java_invocable://step.progress.Invocable%s";
        createWorkflow(JAVA_WORKFLOW_URI, execPattern);

    }

    private void createRfxWorkflow() {
        String execPattern = "script://stepProgress/rfx/script-%s.rfx";
        for (int i = 0; i < NUM_STEPS; i++) {
            String scriptURI = String.format(execPattern, i);
            Kernel.getScript().deleteScript(context, scriptURI);

            int progress;
            if (i == NUM_STEPS - 1) {
                progress = 10;
            } else {
                progress = i;
            }
            Kernel.getScript().createScript(
                    context,
                    scriptURI,
                    RaptureScriptLanguage.REFLEX,
                    RaptureScriptPurpose.PROGRAM,
                    String.format("println(\"Hello from ok step %s\");\n"
                            + "println(\"workerURI is:\");\n"
                            + "workerURI = _params[\"$DP_WORKER_URI\"];\n"
                            + "println(workerURI);\n"
                            + "println(\"step start time is:\");\n"
                            + "stepStartTime = _params[\"$DP_STEP_START_TIME\"];\n"
                            + "println(stepStartTime);\n"
                            + "\n"
                            + "#decision.reportStepProgress(workerURI, stepStartTime, \"Hello, this is going %s\", %s, 10);\n"
                            + "\n"
                            + "return \"ok\";", i, i, progress));
        }
        createWorkflow(RFX_WORKFLOW_URI, execPattern);
    }

    private void createWorkflow(String workflowURI, String execPattern) {
        Workflow w = new Workflow();
        w.setStartStep("step0");
        List<Step> steps = new LinkedList<>();
        for (int i = 0; i < NUM_STEPS; i++) {
            Step step = new Step();
            step.setExecutable(String.format(execPattern, i));
            step.setName("step" + i);
            step.setDescription("desc for " + i);
            List<Transition> transitions = new LinkedList<>();
            if (i < NUM_STEPS - 1) {
                Transition t = new Transition();
                t.setName("ok");
                t.setTargetStep(String.format("step%s", i + 1));
                transitions.add(t);
            }
            step.setTransitions(transitions);
            steps.add(step);
        }
        w.setSteps(steps);
        w.setWorkflowURI(workflowURI);
        Kernel.getDecision().putWorkflow(context, w);
    }

    @After
    public void tearDown() throws Exception {
        if (subscriber != null) Kernel.getPipeline2().unsubscribeQueue(context, subscriber);
    }

    @Test
    public void testJava() throws Exception {
        runTest(JAVA_WORKFLOW_URI);
    }

    @Test
    public void testReflex() throws InterruptedException {
        runTest(RFX_WORKFLOW_URI);
    }

    private void runTest(String workflowURI) throws InterruptedException {
        final CreateResponse response = Kernel.getDecision().createWorkOrderP(context, workflowURI, null, null);
        assertTrue(response.getIsCreated());
        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {
                WorkOrderDebug debug = Kernel.getDecision().getWorkOrderDebug(context, response.getUri());
                assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
                WorkerDebug worker = debug.getWorkerDebugs().get(0);
                List<StepRecordDebug> steps = worker.getStepRecordDebugs();
                System.out.println("STEPS = " + JacksonUtil.formattedJsonFromObject(steps));
                assertEquals(NUM_STEPS, steps.size());
                int i = 0;
                for (StepRecordDebug step : steps) {
                    Activity activity = step.getActivity();
                    assertEquals(10, activity.getMax().longValue());
                    if (i == NUM_STEPS - 1) {
                        assertEquals(10, activity.getProgress().longValue());
                        assertTrue(String.format("message is [%s]", activity.getMessage()), activity.getMessage().contains("Hello, this is going"));
                    } else {
                        assertEquals(i, activity.getProgress().longValue());
                        assertTrue(String.format("message is [%s]", activity.getMessage()), activity.getMessage().contains("Step finished"));
                    }
                    assertEquals(ActivityStatus.FINISHED, activity.getStatus());
                    i++;
                }
            }
        }, MAX_WAIT);
    }
}
