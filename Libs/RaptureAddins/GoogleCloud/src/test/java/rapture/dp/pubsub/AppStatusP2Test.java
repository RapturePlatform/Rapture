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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.log4j.Logger;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import rapture.common.AppStatus;
import rapture.common.CallingContext;
import rapture.common.QueueSubscriber;
import rapture.common.RaptureJob;
import rapture.common.RaptureJobExec;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.WorkflowJobExecDetails;
import rapture.common.dp.ContextVariables;
import rapture.common.dp.Step;
import rapture.common.dp.Workflow;
import rapture.common.exception.ExceptionToString;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.AuditLogEntry;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.dp.InvocableUtils;
import rapture.dp.WaitingTestHelper;
import rapture.dp.WorkflowFactory;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.schedule.ScheduleManager;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AppStatusP2Test {
    private static final int MAX_WAIT = 20000;
    private static final long TIMESTAMP = System.currentTimeMillis();
    private static final String DEFAULT_APP = "some app's: name!";
    private static final String EXPECTED_OVER_URI = "idp.status a/workflow/" + TIMESTAMP + "/" + DEFAULT_APP;
    private static final String EXPECTED_DEF_URI = "default a/" + TIMESTAMP + "/" + DEFAULT_APP;
    private static final String CATEGORY = "alpha";
    private static final Logger logger = Logger.getLogger(AppStatusP2Test.class);

    private static final String REPO_URI = "//appstatusRepo";
    private static final CallingContext CTX = ContextFactory.getKernelUser();
    private static String wuri;

    private static final String PATTERN = "${timestamp}/${app}";
    private static ExecutorService scheduler;
    private static QueueSubscriber subscriber = null;

    @BeforeClass
    public static void setup() {
        String auditConfig = "LOG {} USING MEMORY {maxEntries=\"100\"}";
        RaptureConfig config = ConfigLoader.getConf();
        config.DefaultExchange = "PIPELINE {} USING GCP_PUBSUB { projectid=\"todo3-incap\"}";
        config.DefaultWorkflowAuditLog = auditConfig;
        System.setProperty("LOGSTASH-ISENABLED", "false");

        Kernel.INSTANCE.restart();

        Kernel.initBootstrap();
        Kernel.getAudit().createAuditLog(CTX, "//workflow", auditConfig);
        subscriber = Kernel.INSTANCE.createAndSubscribe(CATEGORY, config.DefaultExchange);

        Kernel.getDoc().deleteDocRepo(CTX, REPO_URI);
        Kernel.getDoc().createDocRepo(CTX, REPO_URI, "NREP {} USING MEMORY {}");

        setTimestamp(TIMESTAMP);

        String systemBlobRepo = "//sys.blob";
        Kernel.getBlob().deleteBlobRepo(CTX, systemBlobRepo);
        Kernel.getBlob().createBlobRepo(CTX, systemBlobRepo, ConfigLoader.getConf().DefaultSystemBlobConfig,
                ConfigLoader.getConf().DefaultSystemBlobFoldersConfig);

        wuri = createWorkflow();

        ThreadFactory tf = new ThreadFactoryBuilder().setNameFormat("AppStatusTest-ScheduleManager").build();
        scheduler = Executors.newSingleThreadExecutor(tf);
        scheduler.execute(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        try {
                            ScheduleManager.manageJobExecStatus();
                        } catch (Exception e) {
                            logger.error(String.format("Got exception %s when running a task, the show goes on. stack: %s", e.getMessage(),
                                    ExceptionToString.format(e)));
                        }
                    } catch (InterruptedException e) {
                        logger.error("Error while sleeping on main thread");
                        System.exit(-1);
                    }
                }
            }
        });
    }

    @AfterClass
    public static void afterClass() {
        if (scheduler != null) scheduler.shutdown();
        if (subscriber != null) Kernel.getPipeline2().unsubscribeQueue(CTX, subscriber);
    }

    private static void setTimestamp(long timestamp) {
        Map<String, String> timeDoc = new HashMap<>();
        timeDoc.put("timestamp", "" + timestamp);
        timeDoc.put("some_Other_data", "test");
        String json = JacksonUtil.jsonFromObject(timeDoc);
        Kernel.getDoc().putDoc(CTX, "document://appstatusRepo/timeDoc", json);
    }

    private void verifyFirstStepLogs(String appStatusName, String workOrder) {
        verifyStepLogs(appStatusName, workOrder, "firstStep", 1);
    }

    private void verifySecondStepLogs(String appStatusName, String workOrder) {
        verifyStepLogs(appStatusName, workOrder, "secondStep", 2);
    }

    private void verifyStepLogs(String appStatusName, String workOrder, String stepName, int expectedCount) {
        String logURI = InvocableUtils.getWorkflowAuditLog(appStatusName, workOrder, stepName);
        List<AuditLogEntry> logs = Kernel.getAudit().getEntriesSince(CTX, logURI, null);
        assertEquals(expectedCount, logs.size());
    }

    @Test
    public void test1DefaultPattern() throws InterruptedException {

        final CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> contextMap = new HashMap<>();
        final String workOrder = Kernel.getDecision().createWorkOrder(context, wuri, contextMap);

        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {
                List<AppStatus> statuses = Kernel.getDecision().getAppStatuses(CTX, EXPECTED_DEF_URI);
                assertEquals(1, statuses.size());
                AppStatus status = statuses.get(0);
                assertEquals(EXPECTED_DEF_URI, status.getName());
                assertEquals(WorkOrderExecutionState.FINISHED, status.getOverallStatus());

                verifyFirstStepLogs(EXPECTED_DEF_URI, workOrder);
                verifySecondStepLogs(EXPECTED_DEF_URI, workOrder);
            }
        }, MAX_WAIT);

    }

    @Test
    public void test2OverridePattern() throws InterruptedException {
        final CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> contextMap = new HashMap<>();
        String pattern = "%/idp.status a/workflow/" + PATTERN;
        // 3 runs should produce 3 sets of logs
        final List<String> workOrders = new ArrayList<>();

        workOrders.add(Kernel.getDecision().createWorkOrderP(context, wuri, contextMap, pattern).getUri());
        workOrders.add(Kernel.getDecision().createWorkOrderP(context, wuri, contextMap, pattern).getUri());
        workOrders.add(Kernel.getDecision().createWorkOrderP(context, wuri, contextMap, pattern).getUri());

        WaitingTestHelper.retry(new Runnable() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void run() {
                List<AppStatus> defs = Kernel.getDecision().getAppStatuses(CTX, EXPECTED_DEF_URI);
                assertEquals(1, defs.size());

                List<AppStatus> overrides = Kernel.getDecision().getAppStatuses(CTX, EXPECTED_OVER_URI);
                assertEquals(3, overrides.size());

                for (String workOrder : workOrders) {
                    verifyFirstStepLogs(EXPECTED_OVER_URI, workOrder);
                    verifySecondStepLogs(EXPECTED_OVER_URI, workOrder);
                }
            }
        }, MAX_WAIT);

    }

    @Test
    public void test3OverridePatternAndApp() throws InterruptedException {
        /*
         * Verify that if we pass in a context variable with "app" it does not override the one in the view
         */

        final CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> contextMap = new HashMap<>();
        String pattern = "%/idp.status a/workflow/" + PATTERN;
        contextMap.put("app", "somethingElse");
        final String workOrder = Kernel.getDecision().createWorkOrderP(context, wuri, contextMap, pattern).getUri();

        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {
                List<AppStatus> defs = Kernel.getDecision().getAppStatuses(CTX, EXPECTED_DEF_URI);
                assertEquals(1, defs.size());

                List<AppStatus> overrides = Kernel.getDecision().getAppStatuses(CTX, EXPECTED_OVER_URI);
                assertEquals(4, overrides.size());

                verifyFirstStepLogs(EXPECTED_OVER_URI, workOrder);
                verifySecondStepLogs(EXPECTED_OVER_URI, workOrder);
            }
        }, MAX_WAIT);
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyyMMdd");

    @Test
    public void test4FromJob() throws InterruptedException {
        final CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> jobParams = new HashMap<>();
        String pattern = "%/job.status a/${" + ContextVariables.TIMESTAMP + "}/${" + ContextVariables.LOCAL_DATE + "}/${app}";
        final String jobURI = "job://appstatus";
        final RaptureJob job = Kernel.getSchedule().createWorkflowJob(CTX, jobURI, "desc", wuri, "* * * *", "America/New_York", jobParams, false, 500, pattern);
        Kernel.getSchedule().runJobNow(CTX, jobURI, null);

        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {

                RaptureJobExec exec = Kernel.getSchedule().getJobExecs(CTX, jobURI, 0, 1, false).get(0);
                LocalDate ld = new LocalDate(exec.getExecTime(), DateTimeZone.forID(job.getTimeZone()));

                logger.info("date is " + exec.getExecTime());

                List<AppStatus> defs = Kernel.getDecision().getAppStatuses(CTX, EXPECTED_DEF_URI);
                assertEquals(1, defs.size());

                List<AppStatus> overrides = Kernel.getDecision().getAppStatuses(CTX, EXPECTED_OVER_URI);
                assertEquals(4, overrides.size());

                long timestamp = exec.getExecTime();
                String expectedAppStatusName = "job.status a/" + timestamp + "/" + FORMATTER.print(ld) + "/" + DEFAULT_APP;
                List<AppStatus> jobs = Kernel.getDecision().getAppStatuses(CTX, expectedAppStatusName);
                assertEquals(JacksonUtil.formattedJsonFromObject(jobs), 1, jobs.size());

                String workOrder = JacksonUtil.objectFromJson(exec.getExecDetails(), WorkflowJobExecDetails.class).getWorkOrderURI();
                verifyFirstStepLogs(expectedAppStatusName, workOrder);
                verifySecondStepLogs(expectedAppStatusName, workOrder);
            }
        }, MAX_WAIT);
    }

    @Test
    public void test5AppStatus() throws InterruptedException {
        List<AppStatus> idps = Kernel.getDecision().getAppStatuses(CTX, "idp.status a");
        for (AppStatus a : idps) {
            logger.info("AppStatus is: " + JacksonUtil.jsonFromObject(a));
        }
        assertEquals(4, idps.size());

        long newTs = System.currentTimeMillis();
        assertNotEquals(newTs, TIMESTAMP);
        setTimestamp(newTs);
        Map<String, String> contextMap = new HashMap<>();
        assertNotNull(Kernel.getDecision().createWorkOrder(CTX, wuri, contextMap));

        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {

                List<AppStatus> defs = Kernel.getDecision().getAppStatuses(CTX, "default a");
                assertEquals(2, defs.size());

                List<AppStatus> jobs = Kernel.getDecision().getAppStatuses(CTX, "job.status a");
                assertEquals(JacksonUtil.formattedJsonFromObject(jobs), 1, jobs.size());
            }
        }, MAX_WAIT);
    }

    private static String createWorkflow() {
        String wuri = "workflow://appStatus";
        List<Step> steps = new ArrayList<>();
        Step s1 = new Step();
        s1.setName("firstStep");
        s1.setExecutable(new RaptureURI.Builder(Scheme.DP_JAVA_INVOCABLE, "AppStatus1").build().toString());
        s1.setTransitions(WorkflowFactory.createTransition("success", "secondStep"));
        Step s2 = new Step();
        s2.setName("secondStep");
        s2.setExecutable(new RaptureURI.Builder(Scheme.DP_JAVA_INVOCABLE, "AppStatus2").build().toString());
        steps.add(s1);
        steps.add(s2);

        Workflow workflow = new Workflow();
        workflow.setSteps(steps);
        workflow.setWorkflowURI(wuri);
        workflow.setStartStep("firstStep");
        workflow.setCategory(CATEGORY);
        workflow.setDefaultAppStatusNamePattern("%/default a/" + PATTERN);
        Map<String, String> view = new HashMap<>();
        view.put("app", "#" + DEFAULT_APP);
        view.put("timestamp", "!//appstatusRepo/timeDoc#timestamp");
        workflow.setView(view);

        Kernel.getDecision().deleteWorkflow(CTX, wuri);
        Kernel.getDecision().putWorkflow(CTX, workflow);
        return wuri;
    }

}
