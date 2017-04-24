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
import static org.junit.Assert.assertTrue;
import static rapture.dp.DPTestUtil.ALPHA;
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
import rapture.common.QueueSubscriber;
import rapture.common.WorkOrderExecutionState;
import rapture.common.dp.Step;
import rapture.common.dp.Workflow;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.dp.WaitingTestHelper;
import rapture.dp.invocable.SignalInvocable;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.Pipeline2ApiImpl;

public class SimpleSplitStepTest {
    private static final String AUTHORITY = "//splitsteptest";
    private CallingContext ctx = ContextFactory.getKernelUser();
    private static final String HELLO = "howdy";
    private static final String SPLIT_STEP = "splitter";
    private static final String AFTER_SPLIT = "goodbye";
    private static final String LEFT = "left1";
    private static final String LEFT_CONTINUE = "left2";
    private static final String LEFT_FINISH = "left3";
    private static final String RIGHT = "right1";
    private static final String WF = "workflow://splitsteptest/workflow";
    private QueueSubscriber subscriber = null;
    private static final int MAX_WAIT = 20000;

    @Before
    public void setup() {
        RaptureConfig config = ConfigLoader.getConf();
        config.DefaultExchange = "PIPELINE {} USING GCP_PUBSUB { projectid=\"todo3-incap\"}";

        Kernel.initBootstrap();
        if (!Kernel.getDoc().docRepoExists(ctx, AUTHORITY)) {
            Kernel.getDoc().createDocRepo(ctx, AUTHORITY, "NREP {} USING MEMORY {}");
        }
        subscriber = Kernel.INSTANCE.createAndSubscribe(ALPHA, "PIPELINE {} USING GCP_PUBSUB { projectid=\"todo3-incap\"}");
        createWorkflow();
    }
    
    @After
    public void tearDown() {
        String[] signals = { HELLO, SPLIT_STEP, AFTER_SPLIT, LEFT, LEFT_CONTINUE, LEFT_FINISH, RIGHT };
        for (String signal : Arrays.asList(signals)) {
            SignalInvocable.Singleton.clearSignal(signal);
        }
        if (subscriber != null) Kernel.getPipeline2().unsubscribeQueue(ctx, subscriber);
    }

    private void createWorkflow() {
        List<Step> steps = Lists.newArrayList();
        
        Step step = makeSignalStep(HELLO);
        step.setTransitions(Lists.newArrayList(makeTransition("", SPLIT_STEP)));
        steps.add(step);
        
        step = new Step();
        step.setName(SPLIT_STEP);
        step.setExecutable("$SPLIT:" + LEFT + "," + RIGHT);
        step.setTransitions(Lists.newArrayList(makeTransition("", AFTER_SPLIT)));
        steps.add(step);
        
        step = makeSignalStep(AFTER_SPLIT);
        steps.add(step);
        
        step = makeSignalStep(RIGHT);
        step.setTransitions(Lists.newArrayList(makeTransition("", "$JOIN")));
        steps.add(step);

        step = makeSignalStep(LEFT);
        step.setTransitions(Lists.newArrayList(makeTransition("", LEFT_CONTINUE)));
        steps.add(step);

        step = makeSignalStep(LEFT_CONTINUE);
        step.setTransitions(Lists.newArrayList(makeTransition("", LEFT_FINISH)));
        steps.add(step);
        
        step = makeSignalStep(LEFT_FINISH);
        step.setTransitions(Lists.newArrayList(makeTransition("", "$JOIN")));
        steps.add(step);
        
        Workflow wf = new Workflow();
        wf.setWorkflowURI(WF);
        wf.setCategory(ALPHA);
        wf.setStartStep(HELLO);
        wf.setSteps(steps);     
        
        Kernel.getDecision().putWorkflow(ctx, wf);
    }

    @Test
    public void runTest() throws InterruptedException {
        String workOrderUri = Kernel.getDecision().createWorkOrder(ctx, WF, ImmutableMap.of("testName", "#SimpleSplit"));

        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {
                assertEquals(WorkOrderExecutionState.FINISHED, Kernel.getDecision().getWorkOrderStatus(ctx, workOrderUri).getStatus());
            }
        }, MAX_WAIT);

        assertTrue(SignalInvocable.Singleton.testSignal(HELLO));
        assertFalse(SignalInvocable.Singleton.testSignal("Fake Signal"));
        assertTrue(SignalInvocable.Singleton.testSignal(LEFT));
        assertTrue(SignalInvocable.Singleton.testSignal(RIGHT));
        assertTrue(SignalInvocable.Singleton.testSignal(LEFT_CONTINUE));
        assertTrue(SignalInvocable.Singleton.testSignal(LEFT_FINISH));
        assertTrue(SignalInvocable.Singleton.testSignal(AFTER_SPLIT));
    }

    
}
