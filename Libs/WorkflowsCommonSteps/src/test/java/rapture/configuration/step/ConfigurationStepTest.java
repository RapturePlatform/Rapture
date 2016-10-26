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
package rapture.configuration.step;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import rapture.common.CallingContext;
import rapture.common.CreateResponse;
import rapture.common.RaptureConstants;
import rapture.common.RaptureScript;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.api.DocApi;
import rapture.common.api.ScriptApi;
import rapture.common.dp.Step;
import rapture.common.dp.StepRecord;
import rapture.common.dp.Steps;
import rapture.common.dp.Transition;
import rapture.common.dp.WorkOrderDebug;
import rapture.common.dp.Workflow;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.RaptureExchange;
import rapture.common.model.RaptureExchangeQueue;
import rapture.common.model.RaptureExchangeType;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.script.KernelScript;

public class ConfigurationStepTest {

    static String saveInitSysConfig;
    static String saveRaptureRepo;
    private static final String auth = "test" + System.currentTimeMillis();
    private static final String REPO_USING_MEMORY = "REP {} USING MEMORY {prefix=\"/tmp/" + auth + "\"}";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CallingContext context = ContextFactory.getKernelUser();
        RaptureConfig.setLoadYaml(false);
        RaptureConfig config = ConfigLoader.getConf();
        saveRaptureRepo = config.RaptureRepo;
        saveInitSysConfig = config.InitSysConfig;

        config.RaptureRepo = REPO_USING_MEMORY;
        config.InitSysConfig = "NREP {} USING MEMORY { prefix=\"/tmp/" + auth + "/sys.config\"}";

        System.setProperty("LOGSTASH-ISENABLED", "false");
        Kernel.initBootstrap();

        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {prefix=\"/tmp/" + auth + "\"}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");
        Kernel.getIdGen().createIdGen(context, "idgen://sys/dp/workOrder", "IDGEN {} USING MEMORY {}");
        Kernel.getIdGen().createIdGen(context, "idgen://sys/activity/id", "IDGEN {} USING MEMORY {}");

        try {
            Kernel.INSTANCE.restart();
            Kernel.initBootstrap(null, null, true);

            Kernel.getPipeline().getTrusted().registerServerCategory(context, "alpha", "Primary servers");
            Kernel.getPipeline().getTrusted().registerServerCategory(context, "beta", "Secondary servers");

            Kernel.getPipeline().registerExchangeDomain(context, "//main", "EXCHANGE {} USING MEMORY {}");

            RaptureExchange exchange = new RaptureExchange();
            exchange.setName("kernel");
            exchange.setName("kernel");
            exchange.setExchangeType(RaptureExchangeType.FANOUT);
            exchange.setDomain("main");

            List<RaptureExchangeQueue> queues = new ArrayList<RaptureExchangeQueue>();
            RaptureExchangeQueue queue = new RaptureExchangeQueue();
            queue.setName("default");
            queue.setRouteBindings(new ArrayList<String>());
            queues.add(queue);

            exchange.setQueueBindings(queues);

            Kernel.getPipeline().getTrusted().registerPipelineExchange(context, "kernel", exchange);
            Kernel.getPipeline().getTrusted().bindPipeline(context, "alpha", "kernel", "default");

            // Now that the binding is setup, register our server as being part
            // of
            // "alpha"

            Kernel.setCategoryMembership("alpha");

            KernelScript ks = new KernelScript();
            ks.setCallingContext(context);

        } catch (RaptureException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }


    @Test
    public void testConfigurationStep() {

        CallingContext context = ContextFactory.getKernelUser();
        DocApi dapi = Kernel.getDoc();
        String workflowUri = "workflow://foo/bar/baz";
        String docUri = "//test/configuration";
        dapi.putDoc(context, docUri, JacksonUtil.jsonFromObject(ImmutableMap.of("FOO", "BAR")));
        Workflow workflow = new Workflow();
        workflow.setJarUriDependencies(ImmutableList.of("jar://workflows/dynamic/*"));
        workflow.setStartStep("step1");

        String step1Script = "script://matrix/step1";
        ScriptApi scriptApi = Kernel.getScript();
        if (scriptApi.doesScriptExist(context, step1Script)) scriptApi.deleteScript(context, step1Script);

        RaptureScript script1 = new RaptureScript();
        script1.setLanguage(RaptureScriptLanguage.REFLEX);
        script1.setPurpose(RaptureScriptPurpose.PROGRAM);
        script1.setName("step1");
        script1.setScript(
                "workerURI = _params['$DP_WORKER_URI']; \n foo = #decision.getContextValue(workerURI, 'FOO');\n" + "println('foo = '+foo);\n"
                        + "return'next';");

        script1.setAuthority("matrix");
        scriptApi.putScript(context, step1Script, script1);

        String step3Script = "script://matrix/step3";
        if (scriptApi.doesScriptExist(context, step3Script)) scriptApi.deleteScript(context, step3Script);

        RaptureScript script3 = new RaptureScript();
        script3.setLanguage(RaptureScriptLanguage.REFLEX);
        script3.setPurpose(RaptureScriptPurpose.PROGRAM);
        script3.setName("step3");
        script3.setScript(
                "workerURI = _params['$DP_WORKER_URI']; \n foo = #decision.getContextValue(workerURI, 'FOO');\n" + "println('now foo = '+foo);\n"
                        + "return foo;");

        script3.setAuthority("matrix");
        scriptApi.putScript(context, step3Script, script3);

        Step step1 = new Step();
        step1.setExecutable(step1Script);
        step1.setName("step1");

        Transition next1 = new Transition();
        next1.setName(Steps.NEXT.toString());
        next1.setTargetStep("step2");
        step1.setTransitions(ImmutableList.of(next1));

        Step step2 = new Step();
        step2.setExecutable("dp_java_invocable://configuration.steps.ConfigurationStep");
        step2.setName("step2");

        Transition next2 = new Transition();
        next2.setName(Steps.NEXT.toString());
        next2.setTargetStep("step3");
        step2.setTransitions(ImmutableList.of(next2));

        Step step3 = new Step();
        step3.setExecutable(step3Script);
        step3.setName("step3");

        Transition next3 = new Transition();
        next3.setName("BAR");
        next3.setTargetStep("$RETURN");
        step3.setTransitions(ImmutableList.of(next3));


        workflow.setSteps(ImmutableList.of(step1, step2, step3));
        workflow.setView(ImmutableMap.of("CONFIGURATION", "#" + docUri));
        workflow.setWorkflowURI(workflowUri);
        Kernel.getDecision().putWorkflow(context, workflow);

        CreateResponse response = Kernel.getDecision().createWorkOrderP(context, workflowUri, null, null);
        assertTrue(response.getIsCreated());
        WorkOrderDebug debug;
        WorkOrderExecutionState state = WorkOrderExecutionState.NEW;
        long timeout = System.currentTimeMillis() + 60000;
        do {
            debug = Kernel.getDecision().getWorkOrderDebug(context, response.getUri());
            state = debug.getOrder().getStatus();
        } while (((state == WorkOrderExecutionState.NEW) || (state == WorkOrderExecutionState.ACTIVE)) && (System.currentTimeMillis() < timeout));

        StepRecord sr = debug.getWorkerDebugs().get(0).getStepRecordDebugs().get(2).getStepRecord();
        assertEquals("BAR", sr.getRetVal());
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
    }

}
