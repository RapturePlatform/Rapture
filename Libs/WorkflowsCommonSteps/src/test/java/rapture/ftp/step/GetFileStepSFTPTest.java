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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import rapture.common.Activity;
import rapture.common.ActivityStatus;
import rapture.common.BlobContainer;
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
import rapture.common.model.AuditLogEntry;
import rapture.common.model.RaptureExchange;
import rapture.common.model.RaptureExchangeQueue;
import rapture.common.model.RaptureExchangeType;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.ftp.common.FTPConnection;
import rapture.ftp.common.FTPConnectionConfig;
import rapture.ftp.common.FTPRequest;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.script.KernelScript;

// These tests require a working SFTP server.
public class GetFileStepSFTPTest {

    static String saveInitSysConfig;
    static String saveRaptureRepo;
    private static final String auth = "test" + System.currentTimeMillis();
    private static final String BLOB_USING_MEMORY = "BLOB {} USING MEMORY {prefix=\"/tmp/B" + auth + "\"}";
    private static final String REPO_USING_MEMORY = "REP {} USING MEMORY {prefix=\"/tmp/" + auth + "\"}";
    private static final String META_USING_MEMORY = "REP {} USING MEMORY {prefix=\"/tmp/M" + auth + "\"}";

    // As of 4 April 2017 test.rebex.net is temporarily unavailable - tests disabled
    static final boolean SFTP_Available = false;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Assume.assumeTrue(SFTP_Available);
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
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");
        Kernel.getIdGen().createIdGen(context, "idgen://sys/dp/workOrder", "IDGEN {} USING MEMORY {}");
        Kernel.getIdGen().createIdGen(context, "idgen://sys/activity/id", "IDGEN {} USING MEMORY {}");

        try {
            Kernel.INSTANCE.restart();
            Kernel.initBootstrap(null, null, true);

            Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                    "LOG {} using MEMORY {prefix=\"/tmp/" + auth + "\"}");
            Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI("//workflow", Scheme.LOG).getAuthority(),
                    "LOG {} using MEMORY {prefix=\"/workflow\"}");

            Kernel.getPipeline().getTrusted().registerServerCategory(context, "alpha", "Primary servers");
            Kernel.getPipeline().getTrusted().registerServerCategory(context, "beta", "Secondary servers");

            Kernel.getPipeline().registerExchangeDomain(context, "//main", "EXCHANGE {} USING MEMORY {}");

            RaptureExchange exchange = new RaptureExchange();
            exchange.setName("kernel");
            exchange.setName("kernel");
            exchange.setExchangeType(RaptureExchangeType.FANOUT);
            exchange.setDomain("main");

            List<RaptureExchangeQueue> queues = new ArrayList<>();
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
        createWorkflow();
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
    static String configRepo = "document://test" + System.currentTimeMillis();
    static String configUri = configRepo + "/Config";

    static private void createWorkflow() {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("test.rebex.net").setPort(22).setLoginId("demo").setPassword("password")
                .setUseSFTP(true);

        CallingContext context = ContextFactory.getKernelUser();
        DocApi dapi = Kernel.getDoc();

        dapi.createDocRepo(context, configRepo, "NREP {} USING MEMORY {}");
        dapi.putDoc(context, configUri, JacksonUtil.jsonFromObject(ftpConfig));

        FTPConnection precheck = new FTPConnection(ftpConfig);
        precheck.connectAndLogin(new FTPRequest(FTPRequest.Action.EXISTS));
        Assume.assumeTrue(precheck.isConnected());
        precheck.logoffAndDisconnect();

        Workflow w = new Workflow();
        w.setStartStep("step1");
        List<Step> steps = new LinkedList<>();
        Step step = new Step();
        step.setExecutable("dp_java_invocable://ftp.steps.GetFileStep");
        step.setName("step1");
        step.setDescription("description");
        List<Transition> transitions = new LinkedList<>();
        step.setTransitions(transitions);
        steps.add(step);

        Map<String, String> viewMap = new HashMap<>();
        viewMap.put("FTP_CONFIGURATION", "#" + configUri);
        w.setSteps(steps);
        w.setView(viewMap);
        w.setWorkflowURI(workflowUri);
        Kernel.getDecision().putWorkflow(context, w);

    }

    @Test
    public void testGetFailNoSuchBlobRepo() {
        CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> args = new HashMap<>();
        args.put("GET_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("pub/example/readme.txt", "blob://nonexistent/readme.txt")));

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
                if (sr.getExceptionInfo() != null) {
                    System.err.println(sr.getExceptionInfo().getStackTrace());
                }

                List<AuditLogEntry> log = Kernel.getAudit().getRecentLogEntries(context, debug.getLogURI() + "/" + sr.getName(), 10);
                assertEquals(5, log.size());
                assertEquals("step1 finished", log.get(0).getMessage());
                assertEquals("Unable to retrieve 1 files", log.get(1).getMessage());
                assertEquals("Unable to retrieve pub/example/readme.txt as blob://nonexistent/readme.txt", log.get(2).getMessage());
                assertEquals("step1: Repository blob://nonexistent does not exist", log.get(3).getMessage());
                assertEquals("step1 started", log.get(4).getMessage());

            }
        }

        WorkerDebug worker = debug.getWorkerDebugs().get(0);
        StepRecordDebug dbg = worker.getStepRecordDebugs().get(0);
        assertEquals("quit", dbg.getStepRecord().getRetVal());
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
    }

    @Test
    public void testGetFailUseFtpToSftpServer() {
        CallingContext context = ContextFactory.getKernelUser();
        DocApi dapi = Kernel.getDoc();

        try {
            FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("speedtest.tele2.net").setPort(22).setLoginId("demo").setPassword("password")
                    .setUseSFTP(false);
            dapi.putDoc(context, configUri, JacksonUtil.jsonFromObject(ftpConfig));
            Map<String, String> args = new HashMap<>();
            args.put("GET_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("pub/example/readme.txt", "blob://tmp/readme.txt")));
            CreateResponse response = Kernel.getDecision().createWorkOrderP(context, workflowUri, args, null);
            assertTrue(response.getIsCreated());
            WorkOrderDebug debug;
            WorkOrderExecutionState state = WorkOrderExecutionState.NEW;
            long timeout = System.currentTimeMillis() + 60000;
            do {
                debug = Kernel.getDecision().getWorkOrderDebug(context, response.getUri());
                state = debug.getOrder().getStatus();
            } while (((state == WorkOrderExecutionState.NEW) || (state == WorkOrderExecutionState.ACTIVE)) && (System.currentTimeMillis() < timeout));
            for (WorkerDebug db : debug.getWorkerDebugs()) {
                for (StepRecordDebug srd : db.getStepRecordDebugs()) {
                    StepRecord sr = srd.getStepRecord();
                    if (sr.getExceptionInfo() != null) {
                        System.err.println(sr.getExceptionInfo().getStackTrace());
                    }

                    List<AuditLogEntry> log = Kernel.getAudit().getRecentLogEntries(context, debug.getLogURI() + "/" + sr.getName(), 10);
                    assertEquals(5, log.size());
                    assertEquals("step1 finished", log.get(0).getMessage());
                    assertEquals("Unable to retrieve 1 files", log.get(1).getMessage());
                    assertEquals("Unable to retrieve pub/example/readme.txt as blob://tmp/readme.txt", log.get(2).getMessage());
                    // assertEquals("step1: 530 This FTP server is anonymous only.\n\nUnable to login to speedtest.tele2.net", log.get(3).getMessage());
                    // assertEquals("step1: 503 Login with USER first.\n\nUnable to login to speedtest.tele2.net", log.get(3).getMessage());
                    assertTrue(log.get(3).getMessage().startsWith("step1: 5"));
                    assertEquals("step1 started", log.get(4).getMessage());

                }
            }
            WorkerDebug worker = debug.getWorkerDebugs().get(0);
            StepRecordDebug dbg = worker.getStepRecordDebugs().get(0);
            assertEquals("quit", dbg.getStepRecord().getRetVal());
            assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());

        } finally {
            FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("test.rebex.net").setPort(22).setLoginId("demo").setPassword("password")
                    .setUseSFTP(true);
            dapi.putDoc(context, configUri, JacksonUtil.jsonFromObject(ftpConfig));
        }
    }

    @Test
    public void testGetFailIncorrectPassword() {
        CallingContext context = ContextFactory.getKernelUser();
        DocApi dapi = Kernel.getDoc();

        try {
            FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("test.rebex.net").setPort(22).setLoginId("demo").setPassword("incorrect")
                    .setUseSFTP(false);
            dapi.putDoc(context, configUri, JacksonUtil.jsonFromObject(ftpConfig));
            Map<String, String> args = new HashMap<>();
            args.put("GET_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("pub/example/readme.txt", "blob://nonexistent/readme.txt")));
            CreateResponse response = Kernel.getDecision().createWorkOrderP(context, workflowUri, args, null);
            assertTrue(response.getIsCreated());
            WorkOrderDebug debug;
            WorkOrderExecutionState state = WorkOrderExecutionState.NEW;
            long timeout = System.currentTimeMillis() + 120000;
            do {
                debug = Kernel.getDecision().getWorkOrderDebug(context, response.getUri());
                state = debug.getOrder().getStatus();
            } while (((state == WorkOrderExecutionState.NEW) || (state == WorkOrderExecutionState.ACTIVE)) && (System.currentTimeMillis() < timeout));
            for (WorkerDebug db : debug.getWorkerDebugs()) {
                for (StepRecordDebug srd : db.getStepRecordDebugs()) {
                    StepRecord sr = srd.getStepRecord();
                    if (sr.getExceptionInfo() != null) {
                        System.err.println(sr.getExceptionInfo().getStackTrace());
                    }

                    List<AuditLogEntry> log = Kernel.getAudit().getRecentLogEntries(context, debug.getLogURI() + "/" + sr.getName(), 10);
                    assertEquals(JacksonUtil.jsonFromObject(log), 5, log.size());
                    assertEquals("step1 finished", log.get(0).getMessage());
                    assertEquals("Unable to retrieve 1 files", log.get(1).getMessage());
                    assertEquals("Unable to retrieve pub/example/readme.txt as blob://nonexistent/readme.txt", log.get(2).getMessage());
                    assertEquals("step1: 530 User cannot log in.\n\nUnable to login to test.rebex.net", log.get(3).getMessage());
                    assertEquals("step1 started", log.get(4).getMessage());

                }
            }
            WorkerDebug worker = debug.getWorkerDebugs().get(0);
            StepRecordDebug dbg = worker.getStepRecordDebugs().get(0);
            assertEquals("quit", dbg.getStepRecord().getRetVal());
            assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
        } finally {
            FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("test.rebex.net").setPort(22).setLoginId("demo").setPassword("password")
                    .setUseSFTP(true);
            dapi.putDoc(context, configUri, JacksonUtil.jsonFromObject(ftpConfig));
        }
    }

    @Test
    public void testGetFailNoSuchServer() {
        CallingContext context = ContextFactory.getKernelUser();
        DocApi dapi = Kernel.getDoc();

        try {
            FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("sun.microsystems.inc").setPort(22).setLoginId("demo").setPassword("incorrect")
                    .setUseSFTP(false);
            dapi.putDoc(context, configUri, JacksonUtil.jsonFromObject(ftpConfig));
            Map<String, String> args = new HashMap<>();
            args.put("GET_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("pub/example/readme.txt", "blob://nonexistent/readme.txt")));
            CreateResponse response = Kernel.getDecision().createWorkOrderP(context, workflowUri, args, null);
            assertTrue(response.getIsCreated());
            WorkOrderDebug debug;
            WorkOrderExecutionState state = WorkOrderExecutionState.NEW;
            long timeout = System.currentTimeMillis() + 120000;
            do {
                debug = Kernel.getDecision().getWorkOrderDebug(context, response.getUri());
                state = debug.getOrder().getStatus();
            } while (((state == WorkOrderExecutionState.NEW) || (state == WorkOrderExecutionState.ACTIVE)) && (System.currentTimeMillis() < timeout));
            for (WorkerDebug db : debug.getWorkerDebugs()) {
                for (StepRecordDebug srd : db.getStepRecordDebugs()) {
                    StepRecord sr = srd.getStepRecord();
                    if (sr.getExceptionInfo() != null) {
                        System.err.println(sr.getExceptionInfo().getStackTrace());
                    }

                    List<AuditLogEntry> log = Kernel.getAudit().getRecentLogEntries(context, debug.getLogURI() + "/" + sr.getName(), 10);
                    assertEquals(JacksonUtil.jsonFromObject(log), 5, log.size());
                    assertEquals("step1 finished", log.get(0).getMessage());
                    assertEquals("Unable to retrieve 1 files", log.get(1).getMessage());
                    assertEquals("Unable to retrieve pub/example/readme.txt as blob://nonexistent/readme.txt", log.get(2).getMessage());
                    assertEquals("step1: Unknown host sun.microsystems.inc", log.get(3).getMessage());
                    assertEquals("step1 started", log.get(4).getMessage());

                }
            }
            WorkerDebug worker = debug.getWorkerDebugs().get(0);
            StepRecordDebug dbg = worker.getStepRecordDebugs().get(0);
            assertEquals("quit", dbg.getStepRecord().getRetVal());
            assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
        } finally {
            FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("test.rebex.net").setPort(22).setLoginId("demo").setPassword("password")
                    .setUseSFTP(true);
            dapi.putDoc(context, configUri, JacksonUtil.jsonFromObject(ftpConfig));
        }
    }

    @Test
    public void testGetFailNoSuchDocRepo() {
        CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> args = new HashMap<>();
        args.put("GET_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("pub/example/readme.txt", "document://nonexistent/readme.txt")));

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
                if (sr.getExceptionInfo() != null) {
                    System.err.println(sr.getExceptionInfo().getStackTrace());
                }

                List<AuditLogEntry> log = Kernel.getAudit().getRecentLogEntries(context, debug.getLogURI() + "/" + sr.getName(), 10);
                assertEquals(5, log.size());
                assertEquals("step1 finished", log.get(0).getMessage());
                assertEquals("Unable to retrieve 1 files", log.get(1).getMessage());
                assertEquals("Unable to retrieve pub/example/readme.txt as document://nonexistent/readme.txt", log.get(2).getMessage());
                assertEquals("step1: Repository document://nonexistent does not exist", log.get(3).getMessage());
                assertEquals("step1 started", log.get(4).getMessage());

            }
        }

        WorkerDebug worker = debug.getWorkerDebugs().get(0);
        StepRecordDebug dbg = worker.getStepRecordDebugs().get(0);
        assertEquals("quit", dbg.getStepRecord().getRetVal());
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
    }

    @Test
    public void testGetFailSourceFileNotFound() {
        CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> args = new HashMap<>();
        args.put("GET_FILES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("pub/example/readme.txt", configRepo + "/tmp/1Kb", "Whoops", configRepo + "/tmp/2KB")));

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
                if (sr.getExceptionInfo() != null) {
                    System.err.println(sr.getExceptionInfo().getStackTrace());
                }

                List<AuditLogEntry> log = Kernel.getAudit().getRecentLogEntries(context, debug.getLogURI() + "/" + sr.getName(), 10);
                assertEquals(JacksonUtil.jsonFromObject(log), 6, log.size());
                assertEquals("step1 finished", log.get(0).getMessage());
                assertEquals("Unable to retrieve 1 files", log.get(1).getMessage());
                assertEquals("Unable to retrieve Whoops as " + configRepo + "/tmp/2KB", log.get(2).getMessage());
                assertEquals("step1: Failed to download Whoops: File not found.", log.get(3).getMessage());
                assertEquals("Retrieved pub/example/readme.txt", log.get(4).getMessage());
                assertEquals("step1 started", log.get(5).getMessage());

            }
        }

        WorkerDebug worker = debug.getWorkerDebugs().get(0);
        StepRecordDebug dbg = worker.getStepRecordDebugs().get(0);
        assertEquals("quit", dbg.getStepRecord().getRetVal());
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
    }

    @Test
    public void testGetFileStepSuccess() {
        CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> args = new HashMap<>();
        args.put("GET_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("pub/example/readme.txt", "/tmp/readme.txt")));

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
                if (sr.getExceptionInfo() != null) {
                    System.err.println(sr.getExceptionInfo().getStackTrace());
                }

                List<AuditLogEntry> log = Kernel.getAudit().getRecentLogEntries(context, debug.getLogURI() + "/" + sr.getName(), 10);
                assertEquals(4, log.size());
                assertEquals("step1 finished", log.get(0).getMessage());
                assertEquals("1 files retrieved", log.get(1).getMessage());
                assertEquals("Retrieved pub/example/readme.txt", log.get(2).getMessage());
                assertEquals("step1 started", log.get(3).getMessage());
            }
        }

        WorkerDebug worker = debug.getWorkerDebugs().get(0);
        List<StepRecordDebug> dbgs = worker.getStepRecordDebugs();
        for (StepRecordDebug dbg : dbgs) {
            Activity activity = dbg.getActivity();
            if (activity != null) {
                assertEquals(10, activity.getMax().longValue());
                assertEquals(ActivityStatus.FINISHED, activity.getStatus());
            }
        }
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
        File local = new File("/tmp/readme.txt");
        assertTrue(local.exists());
        assertEquals(407, local.length());
    }

    @Test
    public void testFetchFileStep() {
        CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> args = new HashMap<>();
        args.put("FETCH_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("pub/example/readme.txt", "/tmp/readme.txt")));

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
                if (sr.getExceptionInfo() != null) {
                    System.err.println(sr.getExceptionInfo().getStackTrace());
                }

                List<AuditLogEntry> log = Kernel.getAudit().getRecentLogEntries(context, debug.getLogURI() + "/" + sr.getName(), 10);
                assertEquals(5, log.size());
                assertEquals("step1 finished", log.get(0).getMessage());
                assertEquals("1 files retrieved", log.get(1).getMessage());
                assertEquals("Retrieved pub/example/readme.txt", log.get(2).getMessage());
                assertEquals("FETCH_FILES parameter is deprecated - please use GET_FILES", log.get(3).getMessage());
                assertEquals("step1 started", log.get(4).getMessage());

            }
        }

        WorkerDebug worker = debug.getWorkerDebugs().get(0);
        List<StepRecordDebug> dbgs = worker.getStepRecordDebugs();
        for (StepRecordDebug dbg : dbgs) {
            Activity activity = dbg.getActivity();
            if (activity != null) {
                assertEquals(10, activity.getMax().longValue());
                assertEquals(ActivityStatus.FINISHED, activity.getStatus());
            }
        }
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
        File local = new File("/tmp/readme.txt");
        assertTrue(local.exists());
        assertEquals(407, local.length());
    }

    @Test
    public void testGetBlobStepSuccess() {
        CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> args = new HashMap<>();
        args.put("GET_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("pub/example/readme.txt", "blob://tmp/readme.txt")));

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
                if (sr.getExceptionInfo() != null) {
                    System.err.println(sr.getExceptionInfo().getStackTrace());
                }
            }
        }

        WorkerDebug worker = debug.getWorkerDebugs().get(0);
        List<StepRecordDebug> dbgs = worker.getStepRecordDebugs();
        for (StepRecordDebug dbg : dbgs) {
            Activity activity = dbg.getActivity();
            if (activity != null) {
                assertEquals(10, activity.getMax().longValue());
                assertEquals(ActivityStatus.FINISHED, activity.getStatus());
            }
        }
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
        BlobContainer bc = Kernel.getBlob().getBlob(context, "blob://tmp/readme.txt");
        assertNotNull(bc);
        assertEquals(407, bc.getContent().length);
    }
}
