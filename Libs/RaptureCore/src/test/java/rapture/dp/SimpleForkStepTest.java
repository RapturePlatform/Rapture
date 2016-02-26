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
import static rapture.dp.DPTestUtil.initPipeline;
import static rapture.dp.DPTestUtil.makeSignalStep;
import static rapture.dp.DPTestUtil.makeTransition;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.dp.Step;
import rapture.common.dp.Transition;
import rapture.common.dp.WorkOrderStatus;
import rapture.common.dp.Workflow;
import rapture.dp.invocable.SignalInvocable;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class SimpleForkStepTest {
    private CallingContext ctx = ContextFactory.getKernelUser();
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(SimpleForkStepTest.class);
    private static final String AUTHORITY = "//forksteptest";
    private static final String ALPHA = "alpha";
    private static final String HELLO = "hello";
    private static final String FORK_STEP = "forker";
    private static final String MAIN_CONTINUE = "main2";
    private static final String LEFT_STEP = "left1";
    private static final String RIGHT_STEP = "right1";
    private static final String LEFT_CONTINUE = "left2";
    private static final String HELLO_I = new RaptureURI.Builder(Scheme.DP_JAVA_INVOCABLE, "HelloWorld").build().toString();
    private static final String WF = "workflow://forksteptest/workflow";

    @Before
    public void setup() {
        Kernel.initBootstrap();
        if (!Kernel.getDoc().docRepoExists(ctx, AUTHORITY)) {
            Kernel.getDoc().createDocRepo(ctx, AUTHORITY, "NREP {} USING MEMORY {}");
        }
        initPipeline(ctx);
        createWorkflow();
    }

    private void createWorkflow() {
        List<Step> steps = Lists.newArrayList();

        Step hello = new Step();
        List<Transition> transitions = Lists.newArrayList();
        transitions.add(makeTransition("", FORK_STEP));
        hello.setName(HELLO);
        hello.setExecutable(HELLO_I);
        hello.setTransitions(transitions);
        steps.add(hello);

        Step fork = new Step();
        transitions = Lists.newArrayList();
        transitions.add(makeTransition("", MAIN_CONTINUE));
        fork.setName(FORK_STEP);
        fork.setExecutable("$FORK:" + LEFT_STEP + "," + RIGHT_STEP);
        fork.setTransitions(transitions);
        steps.add(fork);

        Step main = makeSignalStep(MAIN_CONTINUE);
        steps.add(main);

        Step left1 = makeSignalStep(LEFT_STEP);
        left1.getTransitions().add(makeTransition("", LEFT_CONTINUE));
        steps.add(left1);

        Step right = makeSignalStep(RIGHT_STEP);
        steps.add(right);

        Step left2 = makeSignalStep(LEFT_CONTINUE);
        steps.add(left2);

        Workflow wf = new Workflow();
        wf.setWorkflowURI(WF);
        wf.setCategory(ALPHA);
        wf.setStartStep(HELLO);
        wf.setSteps(steps);

        Kernel.getDecision().putWorkflow(ctx, wf);
    }

    @Test
    public void runTest() throws InterruptedException {
        String workOrderUri = Kernel.getDecision().createWorkOrder(ctx, WF, ImmutableMap.of("testName", "#SimpleFork"));
        assertStatus(workOrderUri, ctx, 15000, WorkOrderExecutionState.FINISHED);
        assertTrue(SignalInvocable.Singleton.testSignal("Hello World"));
        assertFalse(SignalInvocable.Singleton.testSignal("Fake Signal"));
        assertTrue(SignalInvocable.Singleton.testSignal(LEFT_STEP));
        assertTrue(SignalInvocable.Singleton.testSignal(RIGHT_STEP));
        assertTrue(SignalInvocable.Singleton.testSignal(LEFT_CONTINUE));
        assertTrue(SignalInvocable.Singleton.testSignal(MAIN_CONTINUE));
        assertEquals(WorkOrderExecutionState.FINISHED, Kernel.getDecision().getWorkOrderStatus(ctx, workOrderUri).getStatus());
    }

    // helper to assert on the status of a work order. TODO(Oliver): This should be in a utility class.
    private void assertStatus(final String workOrderUri, final CallingContext context, int timeout, final WorkOrderExecutionState expectedStatus)
            throws InterruptedException {
        WaitingTestHelper.retry(new Runnable() {
            public void run() {
                WorkOrderStatus status = Kernel.getDecision().getWorkOrderStatus(context, workOrderUri);
                assertEquals(expectedStatus, status.getStatus());
            }
        }, timeout);
    }

}
