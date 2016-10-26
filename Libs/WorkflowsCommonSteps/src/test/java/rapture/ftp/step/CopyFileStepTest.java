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

package rapture.ftp.step;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.MediaType;

import rapture.common.Activity;
import rapture.common.ActivityStatus;
import rapture.common.CallingContext;
import rapture.common.CreateResponse;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.api.DocApi;
import rapture.common.dp.Step;
import rapture.common.dp.StepRecord;
import rapture.common.dp.StepRecordDebug;
import rapture.common.dp.Transition;
import rapture.common.dp.WorkOrderDebug;
import rapture.common.dp.WorkerDebug;
import rapture.common.dp.Workflow;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.RaptureExchange;
import rapture.common.model.RaptureExchangeQueue;
import rapture.common.model.RaptureExchangeType;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.ftp.common.FTPConnectionConfig;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.script.KernelScript;

public class CopyFileStepTest {

    static String saveInitSysConfig;
    static String saveRaptureRepo;
    private static final String auth = "test" + System.currentTimeMillis();
    private static final String BLOB_USING_MEMORY = "BLOB {} USING MEMORY {prefix=\"/tmp/B" + auth + "\"}";
    private static final String REPO_USING_MEMORY = "REP {} USING MEMORY {prefix=\"/tmp/" + auth + "\"}";
    private static final String META_USING_MEMORY = "REP {} USING MEMORY {prefix=\"/tmp/M" + auth + "\"}";

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
        Kernel.getBlob().createBlobRepo(context, "blob://tmp", BLOB_USING_MEMORY, META_USING_MEMORY);
        Kernel.getBlob().putBlob(context, "blob://tmp/blobby", "And did those feet in ancient time walk upon England's crowded hills?".getBytes(),
                MediaType.ANY_TEXT_TYPE.toString());
        Kernel.getDoc().putDoc(context, "document://tmp/elp",
                "{ \"Band\" : \"Emerson, Lake and Palmer\", \"Album\" : \"Brain Salad Surgery\", \"Track\" : \"Jerusalem\" }");

        defineWorkflow();
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

    static String workflowUri = "workflow://foo/bar/baz";

    static private void defineWorkflow() {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("speedtest.tele2.net").setPort(23).setLoginId("ftp").setPassword("foo@bar")
                .setUseSFTP(false);

        CallingContext context = ContextFactory.getKernelUser();
        DocApi dapi = Kernel.getDoc();
        String configRepo = "document://test" + System.currentTimeMillis();
        String configUri = configRepo + "/Config";

        dapi.createDocRepo(context, configRepo, "NREP {} USING MEMORY {}");
        dapi.putDoc(context, configUri, JacksonUtil.jsonFromObject(ftpConfig));

        Workflow w = new Workflow();
        w.setStartStep("step1");
        List<Step> steps = new LinkedList<>();
        Step step = new Step();
        step.setExecutable("dp_java_invocable://ftp.steps.CopyFileStep");
        step.setName("step1");
        step.setDescription("description");
        List<Transition> transitions = new LinkedList<>();
        step.setTransitions(transitions);
        steps.add(step);

        Map<String, String> viewMap = new HashMap<>();
        viewMap.put("CONFIGURI", "#" + configUri);
        w.setSteps(steps);
        w.setView(viewMap);
        w.setWorkflowURI(workflowUri);
        Kernel.getDecision().putWorkflow(context, w);
    }

    @Test
    public void testCopyFileStep() {

        CallingContext context = ContextFactory.getKernelUser();
        String target = "/tmp/dummy/file";
        Map<String, String> args = new HashMap<>();
        args.put("COPY_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("/bin/ls", target)));

        CreateResponse response = Kernel.getDecision().createWorkOrderP(context, workflowUri, args, null);
        assertTrue(response.getIsCreated());
        WorkOrderDebug debug;
        WorkOrderExecutionState state = WorkOrderExecutionState.NEW;
        long timeout = System.currentTimeMillis() + 60000;
        do {
            debug = Kernel.getDecision().getWorkOrderDebug(context, response.getUri());
            state = debug.getOrder().getStatus();
        } while (((state == WorkOrderExecutionState.NEW) || (state == WorkOrderExecutionState.ACTIVE)) && (System.currentTimeMillis() < timeout));

        // If anything went wrong

        for (WorkerDebug db : debug.getWorkerDebugs()) {
            for (StepRecordDebug srd : db.getStepRecordDebugs()) {
                StepRecord sr = srd.getStepRecord();
                if (sr.getExceptionInfo() != null) System.err.println(sr.getExceptionInfo().getStackTrace());
            }
        }

        WorkerDebug worker = debug.getWorkerDebugs().get(0);
        List<StepRecordDebug> dbgs = worker.getStepRecordDebugs();
        int i = 0;
        for (StepRecordDebug dbg : dbgs) {
            Activity activity = dbg.getActivity();
            if (activity != null) {
                assertEquals(10, activity.getMax().longValue());
                assertEquals(ActivityStatus.FINISHED, activity.getStatus());
                i++;
            }
        }
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
        File dummy = new File(target);
        assertTrue(dummy.exists());
        dummy.delete();
    }

    @Test
    public void testCopyBlobStep() {
        CallingContext context = ContextFactory.getKernelUser();

        String target = "/tmp/blobby/dummy/file";
        Map<String, String> args = new HashMap<>();
        args.put("COPY_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("blob://tmp/blobby", "file:/" + target)));

        CreateResponse response = Kernel.getDecision().createWorkOrderP(context, workflowUri, args, null);
        assertTrue(response.getIsCreated());
        WorkOrderDebug debug;
        WorkOrderExecutionState state = WorkOrderExecutionState.NEW;
        long timeout = System.currentTimeMillis() + 60000;
        do {
            debug = Kernel.getDecision().getWorkOrderDebug(context, response.getUri());
            state = debug.getOrder().getStatus();
        } while (((state == WorkOrderExecutionState.NEW) || (state == WorkOrderExecutionState.ACTIVE)) && (System.currentTimeMillis() < timeout));

        // If anything went wrong

        for (WorkerDebug db : debug.getWorkerDebugs()) {
            for (StepRecordDebug srd : db.getStepRecordDebugs()) {
                StepRecord sr = srd.getStepRecord();
                if (sr.getExceptionInfo() != null) System.err.println(sr.getExceptionInfo().getStackTrace());
            }
        }

        WorkerDebug worker = debug.getWorkerDebugs().get(0);
        List<StepRecordDebug> dbgs = worker.getStepRecordDebugs();
        int i = 0;
        for (StepRecordDebug dbg : dbgs) {
            Activity activity = dbg.getActivity();
            if (activity != null) {
                assertEquals(10, activity.getMax().longValue());
                assertEquals(ActivityStatus.FINISHED, activity.getStatus());
                i++;
            }
        }
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
        File dummy = new File(target);
        assertTrue(dummy.exists());
        dummy.delete();
    }

    @Test
    public void testCopyDocStep() {
        CallingContext context = ContextFactory.getKernelUser();
        String target = "/tmp/emerson/lake/palmer";
        Map<String, String> args = new HashMap<>();
        args.put("COPY_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("document://tmp/elp", "file:/" + target)));

        CreateResponse response = Kernel.getDecision().createWorkOrderP(context, workflowUri, args, null);
        assertTrue(response.getIsCreated());
        WorkOrderDebug debug;
        WorkOrderExecutionState state = WorkOrderExecutionState.NEW;
        long timeout = System.currentTimeMillis() + 60000;
        do {
            debug = Kernel.getDecision().getWorkOrderDebug(context, response.getUri());
            state = debug.getOrder().getStatus();
        } while (((state == WorkOrderExecutionState.NEW) || (state == WorkOrderExecutionState.ACTIVE)) && (System.currentTimeMillis() < timeout));

        // If anything went wrong

        for (WorkerDebug db : debug.getWorkerDebugs()) {
            for (StepRecordDebug srd : db.getStepRecordDebugs()) {
                StepRecord sr = srd.getStepRecord();
                if (sr.getExceptionInfo() != null) System.err.println(sr.getExceptionInfo().getStackTrace());
            }
        }

        WorkerDebug worker = debug.getWorkerDebugs().get(0);
        List<StepRecordDebug> dbgs = worker.getStepRecordDebugs();
        int i = 0;
        for (StepRecordDebug dbg : dbgs) {
            Activity activity = dbg.getActivity();
            if (activity != null) {
                assertEquals(10, activity.getMax().longValue());
                assertEquals(ActivityStatus.FINISHED, activity.getStatus());
                i++;
            }
        }
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
    }

}
