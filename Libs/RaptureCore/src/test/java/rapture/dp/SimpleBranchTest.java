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
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.WorkOrderExecutionState;
import rapture.common.api.ScriptApi;
import rapture.common.dp.Step;
import rapture.common.dp.Transition;
import rapture.common.dp.Workflow;
import rapture.common.model.RaptureExchange;
import rapture.common.model.RaptureExchangeQueue;
import rapture.common.model.RaptureExchangeType;
import rapture.config.ConfigLoader;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

import com.google.common.collect.Lists;

public class SimpleBranchTest {
    private static CallingContext ctx = ContextFactory.getKernelUser();
    private static final String SCRIPT = "script:";
    private static final String AUTHORITY = "//branchrepo";
    private static final String START_SCRIPT = SCRIPT + AUTHORITY + "/script/before";
    private static final String WIN_SCRIPT = SCRIPT + AUTHORITY + "/script/win";
    private static final String LOSE_SCRIPT = SCRIPT + AUTHORITY + "/script/lose";
    private static final String WORKFLOW = AUTHORITY + "/worflow";
    private static final String EXCHANGE = "questionTestExchange";
    private static final String ALPHA = "alpha";
    private static final String START = "jdsfhdfsuh";
    private static final String WIN_STEP = "hollow victory";
    private static final String LOSE_STEP = "consequences delivered";
    private static final String systemBlobRepo = "//sys.blob";

    @AfterClass
    public static void cleanUp() {
        Kernel.getScript().deleteScript(ctx, START_SCRIPT);
        Kernel.getScript().deleteScript(ctx, WIN_SCRIPT);
        Kernel.getScript().deleteScript(ctx, LOSE_SCRIPT);
        Kernel.getDoc().deleteDocRepo(ctx, AUTHORITY);
        Kernel.getBlob().deleteBlobRepo(ctx, systemBlobRepo);
    }
    
    @BeforeClass
    public static void setup() {
        System.setProperty("LOGSTASH-ISENABLED", "false");
        Kernel.initBootstrap();
        cleanUp();
    
        if (!Kernel.getDoc().docRepoExists(ctx, AUTHORITY)) {
            Kernel.getDoc().createDocRepo(ctx, AUTHORITY, "NREP {} USING MEMORY {}");
        }
        Kernel.getBlob().createBlobRepo(ctx, systemBlobRepo, ConfigLoader.getConf().DefaultSystemBlobConfig,
                ConfigLoader.getConf().DefaultSystemBlobFoldersConfig);

        initPipeline();
        createScripts();
        createWorkflow();
    }

    private static void createScripts() {
        ScriptApi scrapi = Kernel.getScript();
        if (!scrapi.doesScriptExist(ctx, START_SCRIPT))
            scrapi.createScript(
                ctx,
                START_SCRIPT,
                RaptureScriptLanguage.REFLEX,
                RaptureScriptPurpose.PROGRAM,
                "println(\"Starting\"); return \"fail\";");
        if (!scrapi.doesScriptExist(ctx, WIN_SCRIPT))
            scrapi.createScript(
                ctx,
                WIN_SCRIPT,
                RaptureScriptLanguage.REFLEX,
                RaptureScriptPurpose.PROGRAM,
                "println(\"HAPPY\");");
        if (!scrapi.doesScriptExist(ctx, LOSE_SCRIPT))
            scrapi.createScript(
                ctx,
                LOSE_SCRIPT,
                RaptureScriptLanguage.REFLEX,
                RaptureScriptPurpose.PROGRAM,
                "println(\"SAD\");");
    }

    private static void createWorkflow() {
        List<Step> steps = Lists.newArrayList();
        List<Transition> transitions = Lists.newArrayList();

        Transition tWin = new Transition();
        tWin.setName("success");
        tWin.setTargetStep(WIN_STEP);
        transitions.add(tWin);

        Transition tLose = new Transition();
        tLose.setName("fail");
        tLose.setTargetStep(LOSE_STEP);
        transitions.add(tLose);

        Step start = new Step();
        start.setExecutable(START_SCRIPT);
        start.setName(START);
        start.setTransitions(transitions);
        steps.add(start);

        Step win = new Step();
        win.setExecutable(WIN_SCRIPT);
        win.setName(WIN_STEP);
        win.setTransitions(Lists.<Transition> newArrayList());
        steps.add(win);

        Step lose = new Step();
        lose.setExecutable(LOSE_SCRIPT);
        lose.setName(LOSE_STEP);
        lose.setTransitions(Lists.<Transition> newArrayList());
        steps.add(lose);

        Workflow workflow = new Workflow();
        workflow.setCategory(ALPHA);
        workflow.setStartStep(START);
        workflow.setSteps(steps);
        workflow.setWorkflowURI(WORKFLOW);
        Kernel.getDecision().putWorkflow(ctx, workflow);
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

    @Test
    public void testFailThenSuccess() {
        // TODO MEL Store FAIL in testDoc
        String workOrderUri = Kernel.getDecision().createWorkOrder(ctx, WORKFLOW, null);
        assertNotNull(workOrderUri);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(WorkOrderExecutionState.FINISHED, Kernel.getDecision().getWorkOrderStatus(ctx, workOrderUri).getStatus());
        // TODO MEL Test that correct side of branch ran
    }
}
