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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static rapture.dp.DPTestUtil.ALPHA;
import static rapture.dp.DPTestUtil.initPipeline;
import static rapture.dp.DPTestUtil.makeSignalStep;
import static rapture.dp.DPTestUtil.makeTransition;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import rapture.common.CallingContext;
import rapture.common.WorkOrderExecutionState;
import rapture.common.dp.Step;
import rapture.common.dp.WorkOrderStatus;
import rapture.common.dp.Workflow;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.dp.invocable.SignalInvocable;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.Pipeline2ApiImpl;

public class NestedSplitStepTest {
    private static final String AUTHORITY = "//splitsteptest";
    private CallingContext ctx = ContextFactory.getKernelUser();
    private static final String HELLO = "howdy";
    private static final String SPLIT_STEP = "splitter";
    private static final String SPLIT_STEP_A = "splittera";
    private static final String SPLIT_STEP_B = "splitterb";
    private static final String AFTER_SPLIT = "goodbye";
    private static final String AFTER_SPLIT_B = "goodbyeb";
    private static final String LEFT_A = "left1a";
    private static final String LEFT_CONTINUE_A = "left2a";
    private static final String LEFT_FINISH_A = "left3a";
    private static final String RIGHT_A = "right1a";
    private static final String LEFT_B = "left1b";
    private static final String LEFT_CONTINUE_B = "left2b";
    private static final String LEFT_FINISH_B = "left3b";
    private static final String RIGHT_B = "right1b";
    private static final String WF = "workflow://splitsteptest/workflow";

    @Before
    public void setup() {
        RaptureConfig config = ConfigLoader.getConf();
        config.DefaultExchange = "PIPELINE {} USING GCP_PUBSUB { projectid=\"todo3-incap\"}";
        Kernel.initBootstrap();
        if (!Kernel.getDoc().docRepoExists(ctx, AUTHORITY)) {
            Kernel.getDoc().createDocRepo(ctx, AUTHORITY, "NREP {} USING MEMORY {}");
        }
        if (!Pipeline2ApiImpl.usePipeline2) initPipeline(ctx);
        createWorkflow();
    }

    @After
    public void tearDown() {
        String[] signals = { HELLO, SPLIT_STEP, SPLIT_STEP_A, SPLIT_STEP_B, AFTER_SPLIT, AFTER_SPLIT_B, LEFT_A, LEFT_CONTINUE_A, LEFT_FINISH_A, RIGHT_A, LEFT_B,
                LEFT_CONTINUE_B, LEFT_FINISH_B, RIGHT_B };
        for (String signal : Arrays.asList(signals)) {
            SignalInvocable.Singleton.clearSignal(signal);
        }
    }

    private void createWorkflow() {
        List<Step> steps = Lists.newArrayList();

        Step step = makeSignalStep(HELLO);
        step.setTransitions(Lists.newArrayList(makeTransition("", SPLIT_STEP)));
        steps.add(step);

        step = new Step();
        step.setName(SPLIT_STEP);
        step.setExecutable("$SPLIT:" + SPLIT_STEP_A + "," + SPLIT_STEP_B);
        step.setTransitions(Lists.newArrayList(makeTransition("", AFTER_SPLIT)));
        steps.add(step);

        step = new Step();
        step.setName(SPLIT_STEP_A);
        step.setExecutable("$SPLIT:" + LEFT_A + "," + RIGHT_A);
        step.setTransitions(Lists.newArrayList(makeTransition("", "$JOIN")));
        steps.add(step);

        step = new Step();
        step.setName(SPLIT_STEP_B);
        step.setExecutable("$SPLIT:" + LEFT_B + "," + RIGHT_B);
        step.setTransitions(Lists.newArrayList(makeTransition("", AFTER_SPLIT_B)));
        steps.add(step);

        step = makeSignalStep(AFTER_SPLIT);
        steps.add(step);

        step = makeSignalStep(AFTER_SPLIT_B);
        step.setTransitions(Lists.newArrayList(makeTransition("", "$JOIN")));
        steps.add(step);

        step = makeSignalStep(RIGHT_A);
        step.setTransitions(Lists.newArrayList(makeTransition("", "$JOIN")));
        steps.add(step);

        step = makeSignalStep(RIGHT_B);
        step.setTransitions(Lists.newArrayList(makeTransition("", "$JOIN")));
        steps.add(step);

        step = makeSignalStep(LEFT_A);
        step.setTransitions(Lists.newArrayList(makeTransition("", LEFT_CONTINUE_A)));
        steps.add(step);

        step = makeSignalStep(LEFT_B);
        step.setTransitions(Lists.newArrayList(makeTransition("", LEFT_CONTINUE_B)));
        steps.add(step);

        step = makeSignalStep(LEFT_CONTINUE_A);
        step.setTransitions(Lists.newArrayList(makeTransition("", LEFT_FINISH_A)));
        steps.add(step);

        step = makeSignalStep(LEFT_CONTINUE_B);
        step.setTransitions(Lists.newArrayList(makeTransition("", LEFT_FINISH_B)));
        steps.add(step);

        step = makeSignalStep(LEFT_FINISH_A);
        step.setTransitions(Lists.newArrayList(makeTransition("", "$JOIN")));
        steps.add(step);

        step = makeSignalStep(LEFT_FINISH_B);
        step.setTransitions(Lists.newArrayList(makeTransition("", "$JOIN")));
        steps.add(step);

        Workflow wf = new Workflow();
        wf.setWorkflowURI(WF);
        wf.setCategory(ALPHA);
        wf.setStartStep(HELLO);
        wf.setSteps(steps);

        Kernel.getDecision().putWorkflow(ctx, wf);
    }

    // @Ignore
    @Test
    // TODO: fix flaky test
    public void runTest() throws InterruptedException {
        // 2 level nested join. A side join joins to the outer, B side join has a step then joins to the outer.
        String workOrderUri = Kernel.getDecision().createWorkOrder(ctx, WF, ImmutableMap.of("testName", "#SimpleSplit"));
        assertStatus(workOrderUri, ctx, 15000, WorkOrderExecutionState.FINISHED);
        assertTrue(SignalInvocable.Singleton.testSignal(HELLO));
        assertFalse(SignalInvocable.Singleton.testSignal("Fake Signal"));
        assertTrue(SignalInvocable.Singleton.testSignal(LEFT_A));
        assertTrue(SignalInvocable.Singleton.testSignal(RIGHT_A));
        assertTrue(SignalInvocable.Singleton.testSignal(LEFT_B));
        assertTrue(SignalInvocable.Singleton.testSignal(RIGHT_B));
        assertTrue(SignalInvocable.Singleton.testSignal(LEFT_CONTINUE_A));
        assertTrue(SignalInvocable.Singleton.testSignal(LEFT_CONTINUE_B));
        assertTrue(SignalInvocable.Singleton.testSignal(LEFT_FINISH_B));
        assertTrue(SignalInvocable.Singleton.testSignal(AFTER_SPLIT));
    }

    // helper to assert on the status of a work order. TODO(Oliver): This should be in a utility class.
    private void assertStatus(final String workOrderUri, final CallingContext context, int timeout, final WorkOrderExecutionState expectedStatus)
            throws InterruptedException {
        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {
                WorkOrderStatus status = Kernel.getDecision().getWorkOrderStatus(context, workOrderUri);
                assertEquals(expectedStatus, status.getStatus());
            }
        }, timeout);
    }

}
