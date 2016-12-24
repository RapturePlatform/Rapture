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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
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
import org.junit.Ignore;
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

public class GetFileStepFTPTest {

    static String saveInitSysConfig;
    static String saveRaptureRepo;
    private static final String auth = "test" + System.currentTimeMillis();
    private static final String BLOB_USING_MEMORY = "BLOB {} USING MEMORY {prefix=\"/tmp/B" + auth + "\"}";
    private static final String REPO_USING_MEMORY = "REP {} USING MEMORY {prefix=\"/tmp/" + auth + "\"}";
    private static final String META_USING_MEMORY = "REP {} USING MEMORY {prefix=\"/tmp/M" + auth + "\"}";

    static final boolean SFTP_Available = true;

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
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("speedtest.tele2.net").setPort(23).setLoginId("ftp").setPassword("foo@bar")
                .setUseSFTP(false);

        FTPConnection precheck = new FTPConnection(ftpConfig);
        precheck.connectAndLogin(new FTPRequest(FTPRequest.Action.EXISTS));
        Assume.assumeTrue(precheck.isConnected());
        precheck.logoffAndDisconnect();

        CallingContext context = ContextFactory.getKernelUser();
        DocApi dapi = Kernel.getDoc();

        dapi.createDocRepo(context, configRepo, "NREP {} USING MEMORY {}");
        dapi.putDoc(context, configUri, JacksonUtil.jsonFromObject(ftpConfig));

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
    public void testGetFailNoBlobRepo() {
        CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> args = new HashMap<>();
        args.put("GET_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("1KB.zip", "blob://nonexistent/1KB.zip")));

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
                assertEquals("Unable to retrieve 1KB.zip as blob://nonexistent/1KB.zip", log.get(2).getMessage());
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
    public void testGetFailDodgyCredentials1() {
        CallingContext context = ContextFactory.getKernelUser();
        DocApi dapi = Kernel.getDoc();

        try {
            // Bad password
            FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("speedtest.tele2.net").setPort(23).setLoginId("xyzzy").setPassword("plugh")
                    .setUseSFTP(false);
            dapi.putDoc(context, configUri, JacksonUtil.jsonFromObject(ftpConfig));
            Map<String, String> args = new HashMap<>();
            args.put("GET_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("1KB.zip", "blob://nonexistent/1KB.zip")));
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
                    assertEquals("Unable to retrieve 1KB.zip as blob://nonexistent/1KB.zip", log.get(2).getMessage());
                    assertEquals("step1: 530 This FTP server is anonymous only.\r\n\nUnable to login to speedtest.tele2.net", log.get(3).getMessage());
                    assertEquals("step1 started", log.get(4).getMessage());

                }
            }
            WorkerDebug worker = debug.getWorkerDebugs().get(0);
            StepRecordDebug dbg = worker.getStepRecordDebugs().get(0);
            assertEquals("quit", dbg.getStepRecord().getRetVal());
            assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());

        } finally {
            FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("speedtest.tele2.net").setPort(23).setLoginId("ftp").setPassword("foo@bar")
                    .setUseSFTP(false);
            dapi.putDoc(context, configUri, JacksonUtil.jsonFromObject(ftpConfig));
        }

    }

    @Test
    public void testGetFailDodgyCredentials2() {
        CallingContext context = ContextFactory.getKernelUser();
        DocApi dapi = Kernel.getDoc();

        try {
            // This should be a FTP server that doesn't support SFTP
            FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("speedtest.tele2.net").setPort(23).setLoginId("ftp").setPassword("foo@bar")
                    .setUseSFTP(true);
            dapi.putDoc(context, configUri, JacksonUtil.jsonFromObject(ftpConfig));
            Map<String, String> args = new HashMap<>();
            args.put("GET_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("1KB.zip", "blob://nonexistent/1KB.zip")));
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
                    assertEquals(5, log.size());
                    assertEquals("step1 finished", log.get(0).getMessage());
                    assertEquals("Unable to retrieve 1 files", log.get(1).getMessage());
                    assertEquals("Unable to retrieve 1KB.zip as blob://nonexistent/1KB.zip", log.get(2).getMessage());
                    // Actual error message may vary
                    // assertEquals(
                    // "step1: Connecting to speedtest.tele2.net port 23\ncom.jcraft.jsch.JSchException: java.net.ConnectException: Connection refused\nCaused
                    // by: java.net.ConnectException: Connection refused\n",
                    // log.get(3).getMessage());
                    assertEquals("step1 started", log.get(4).getMessage());
                }
            }
            WorkerDebug worker = debug.getWorkerDebugs().get(0);
            StepRecordDebug dbg = worker.getStepRecordDebugs().get(0);
            assertEquals("quit", dbg.getStepRecord().getRetVal());
            assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
        } finally {
            FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("speedtest.tele2.net").setPort(23).setLoginId("ftp").setPassword("foo@bar")
                    .setUseSFTP(false);
            dapi.putDoc(context, configUri, JacksonUtil.jsonFromObject(ftpConfig));
        }
    }

    @Test
    public void testGetFailDodgyCredentials3() {
        CallingContext context = ContextFactory.getKernelUser();
        DocApi dapi = Kernel.getDoc();

        try {
            // This should be a FTP server that doesn't support SFTP
            FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("OUT.OF.CHEESE").setPort(23).setLoginId("ftp").setPassword("foo@bar")
                    .setUseSFTP(true);
            dapi.putDoc(context, configUri, JacksonUtil.jsonFromObject(ftpConfig));
            Map<String, String> args = new HashMap<>();
            args.put("GET_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("1KB.zip", "blob://nonexistent/1KB.zip")));
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
                    assertEquals(5, log.size());
                    assertEquals("step1 finished", log.get(0).getMessage());
                    assertEquals("Unable to retrieve 1 files", log.get(1).getMessage());
                    assertEquals("Unable to retrieve 1KB.zip as blob://nonexistent/1KB.zip", log.get(2).getMessage());
                    assertEquals(
                            "step1: Connecting to OUT.OF.CHEESE port 23\ncom.jcraft.jsch.JSchException: java.net.UnknownHostException: OUT.OF.CHEESE\nCaused by: java.net.UnknownHostException: OUT.OF.CHEESE\n",
                            log.get(3).getMessage());
                    assertEquals("step1 started", log.get(4).getMessage());
                }
            }
            WorkerDebug worker = debug.getWorkerDebugs().get(0);
            StepRecordDebug dbg = worker.getStepRecordDebugs().get(0);
            assertEquals("quit", dbg.getStepRecord().getRetVal());
            assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
        } finally {
            FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("speedtest.tele2.net").setPort(23).setLoginId("ftp").setPassword("foo@bar")
                    .setUseSFTP(false);
            dapi.putDoc(context, configUri, JacksonUtil.jsonFromObject(ftpConfig));
        }
    }

    @Test
    public void testGetFailNoDocRepo() {
        CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> args = new HashMap<>();
        args.put("GET_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("1KB.zip", "document://nonexistent/1KB.zip")));

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
                assertEquals("Unable to retrieve 1KB.zip as document://nonexistent/1KB.zip", log.get(2).getMessage());
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
    public void testGetFailNoSource() {
        CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> args = new HashMap<>();
        args.put("GET_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("1Kb.zip", configRepo + "/tmp/1Kb", "Whoops", configRepo + "/tmp/2KB")));

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
                assertEquals(JacksonUtil.jsonFromObject(log), 7, log.size());
                assertEquals("step1 finished", log.get(0).getMessage());
                assertEquals("Unable to retrieve 2 files", log.get(1).getMessage());
                assertEquals("Unable to retrieve Whoops as " + configRepo + "/tmp/2KB", log.get(2).getMessage());
                // Error message may vary
                // assertEquals("step1: Error retrieving Whoops Server returned response code 550", log.get(3).getMessage());
                assertEquals("Unable to retrieve 1Kb.zip as " + configRepo + "/tmp/1Kb", log.get(4).getMessage());
                // assertEquals("step1: Error retrieving 1Kb.zip Server returned response code 550", log.get(5).getMessage());
                assertEquals("step1 started", log.get(6).getMessage());

            }
        }

        WorkerDebug worker = debug.getWorkerDebugs().get(0);
        StepRecordDebug dbg = worker.getStepRecordDebugs().get(0);
        assertEquals("quit", dbg.getStepRecord().getRetVal());
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
    }

    @Test
    public void testGetFileStep() {
        CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> args = new HashMap<>();
        args.put("GET_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("1KB.zip", "/tmp/1KB.zip")));

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
                assertEquals(JacksonUtil.jsonFromObject(log), 4, log.size());
                assertEquals("step1 finished", log.get(0).getMessage());
                assertEquals("1 files retrieved", log.get(1).getMessage());
                assertEquals("Retrieved 1KB.zip", log.get(2).getMessage());
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
        File local = new File("/tmp/1KB.zip");
        assertTrue(local.exists());
        assertEquals(1024, local.length());
    }

    @Test
    public void testGetBlobStep() {
        CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> args = new HashMap<>();
        args.put("GET_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("1KB.zip", "blob://tmp/1KB.zip")));

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
                assertEquals(JacksonUtil.jsonFromObject(log), 4, log.size());
                assertEquals("step1 finished", log.get(0).getMessage());
                assertEquals("1 files retrieved", log.get(1).getMessage());
                assertEquals("Retrieved 1KB.zip", log.get(2).getMessage());
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
        BlobContainer bc = Kernel.getBlob().getBlob(context, "blob://tmp/1KB.zip");
        assertNotNull(bc);
        assertEquals(1024, bc.getContent().length);
    }

    @Test
    @Ignore
    // Fails on build server. Need investigating.
    public void testGetYchartBlobStep() {
        CallingContext context = ContextFactory.getKernelUser();
        DocApi dapi = Kernel.getDoc();

        try {
            FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("ftp.ycharts.com").setPort(21).setLoginId("incapture").setPassword("CjNF38sk")
                    .setUseSFTP(false);
            dapi.putDoc(context, configUri, JacksonUtil.jsonFromObject(ftpConfig));

            Map<String, String> args = new HashMap<>();
            String source = ZonedDateTime.now().minusDays(7)
                    .format(new DateTimeFormatterBuilder().appendLiteral("events/Events_Data_Delta_").appendValue(ChronoField.YEAR).appendLiteral("_")
                            .appendValue(ChronoField.MONTH_OF_YEAR).appendLiteral("_").appendValue(ChronoField.DAY_OF_MONTH).appendLiteral("_00_00.txt.gz")
                            .toFormatter());

            args.put("GET_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of(source, "blob://tmp/ychart.txt.gz")));

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
                    assertEquals(JacksonUtil.jsonFromObject(log), 4, log.size());
                    assertEquals("step1 finished", log.get(0).getMessage());
                    assertEquals("1 files retrieved", log.get(1).getMessage());
                    assertEquals("Retrieved " + source, log.get(2).getMessage());
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
            BlobContainer bc = Kernel.getBlob().getBlob(context, "blob://tmp/ychart.txt.gz");
            assertNotNull(bc);
            assertTrue(bc.getContent().length >= 274);
        } finally {
            FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("speedtest.tele2.net").setPort(23).setLoginId("ftp").setPassword("foo@bar")
                    .setUseSFTP(false);
            dapi.putDoc(context, configUri, JacksonUtil.jsonFromObject(ftpConfig));
        }
    }

}
