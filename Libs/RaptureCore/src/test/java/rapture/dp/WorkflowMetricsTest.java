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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import rapture.common.CallingContext;
import rapture.common.JobExecStatus;
import rapture.common.RaptureJobExec;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.WorkflowJobExecDetails;
import rapture.common.dp.ContextVariables;
import rapture.common.dp.Step;
import rapture.common.dp.WorkOrder;
import rapture.common.dp.WorkOrderInitialArgsHash;
import rapture.common.dp.WorkOrderInitialArgsHashStorage;
import rapture.common.dp.Workflow;
import rapture.common.exception.ExceptionToString;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.dp.metrics.WorkflowMetricsFactory;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.schedule.ScheduleManager;
import rapture.metrics.MetricsService;
import rapture.metrics.MetricsTestHelper;
import rapture.metrics.NonBlockingMetricsService;
import rapture.metrics.reader.NoOpMetricsReader;
import rapture.metrics.store.DummyMetricsStore;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class WorkflowMetricsTest {
    private static final int MAX_WAIT = 2000;
    private static final String CATEGORY = "alpha";
    private static final Logger log = Logger.getLogger(WorkflowMetricsTest.class);

    private static final CallingContext CTX = ContextFactory.getKernelUser();
    public static final String NO_ARGS_HASH = DigestUtils.sha256Hex("");
    private static final NoOpMetricsReader METRICS_READER = new NoOpMetricsReader();
    public static final String ARGS_FINISHED_PREFIX = WorkflowMetricsFactory.ARGS_WORKFLOW_PREFIX + "." + WorkOrderExecutionState.FINISHED.toString();
    public static final String GENERIC_FINISHED_PREFIX = WorkflowMetricsFactory.GENERIC_WORKFLOW_PREFIX + "." + WorkOrderExecutionState.FINISHED.toString();
    public static final String JOB_FINISHED_PREFIX = WorkflowMetricsFactory.JOB_WORKFLOW_PREFIX + "." + WorkOrderExecutionState.FINISHED;

    private static ScheduledExecutorService scheduler;
    private NonBlockingMetricsService metricsService;

    private static NonBlockingMetricsService createMetricsService(final Collection<String> metricNames) {
        return new NonBlockingMetricsService(0, 100, 100, new DummyMetricsStore() {
            @Override
            public void recordDelta(String parameterName, Long delta) {
                metricNames.add(parameterName);
            }
        }, METRICS_READER);

    }

    private static Collection<String> globalMetricNames;

    private static final Semaphore JOB_SEMAPHORE = new Semaphore(1);

    @Before
    public void before() throws NoSuchFieldException, IllegalAccessException {
        globalMetricNames = new HashSet<>();
        Kernel.INSTANCE.restart();
        Kernel.initBootstrap();
        Field field = Kernel.class.getDeclaredField("metricsService");
        field.setAccessible(true);
        metricsService = createMetricsService(globalMetricNames);
        field.set(Kernel.INSTANCE, metricsService);

        Kernel.getPipeline().setupStandardCategory(CTX, CATEGORY);
        Kernel.setCategoryMembership(CATEGORY);

        ThreadFactory tf = new ThreadFactoryBuilder().setNameFormat("WorkflowMetricsTest-ScheduleManager").build();
        scheduler = Executors.newSingleThreadScheduledExecutor(tf);
        scheduler.scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {
                try {
                    ScheduleManager.manageJobExecStatus();
                    JOB_SEMAPHORE.release();
                } catch (Exception e) {
                    log.error(String.format("Got exception %s when running a task, the show goes on. stack: %s", e.getMessage(),
                            ExceptionToString.format(e)));
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    private static void createWorkflow(String wuri) {
        List<Step> steps = new ArrayList<Step>();
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
        Map<String, String> view = new HashMap<String, String>();
        workflow.setView(view);

        Kernel.getDecision().deleteWorkflow(CTX, wuri);
        Kernel.getDecision().putWorkflow(CTX, workflow);
    }

    @AfterClass
    public static void afterClass() {
        scheduler.shutdown();
    }

    @Test
    public void testNoArgs() throws InterruptedException {
        String wuri = "workflow://workflow/metrics/testNoArgs";
        createWorkflow(wuri);

        final CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> contextMap = new HashMap<String, String>();
        final String uri1 = Kernel.getDecision().createWorkOrder(context, wuri, contextMap);
        final String uri2 = Kernel.getDecision().createWorkOrder(context, wuri, contextMap);

        WaitingTestHelper.retry(new Runnable() {
            public void run() {
                WorkOrder w1 = Kernel.getDecision().getWorkOrder(context, uri1);
                assertEquals(WorkOrderExecutionState.FINISHED, w1.getStatus());
                WorkOrder w2 = Kernel.getDecision().getWorkOrder(context, uri2);
                assertEquals(WorkOrderExecutionState.FINISHED, w2.getStatus());

                WorkOrderInitialArgsHash argsHash = WorkOrderInitialArgsHashStorage.readByFields(uri1);
                assertEquals(NO_ARGS_HASH, argsHash.getHashValue());

                assertEquals(argsHash.getHashValue(), WorkOrderInitialArgsHashStorage.readByFields(uri2).getHashValue());
            }
        }, MAX_WAIT);

        MetricsTestHelper.flushIfNeeded(metricsService);
        assertTrue("metrics are " + globalMetricNames, globalMetricNames.size() >= 2);
        metricsContain(ARGS_FINISHED_PREFIX, "workflow.metrics-testNoArgs");
        metricsContain(GENERIC_FINISHED_PREFIX, "workflow.metrics-testNoArgs");
    }

    private void metricsContain(String... parts) {
        boolean isFound = false;
        for (String metricName : globalMetricNames) {
            boolean isFoundHere = true;
            for (String part : parts) {
                isFoundHere = isFoundHere && metricName.contains(part);
            }
            isFound = isFoundHere;

            if (isFound) {
                break;
            }
        }
        assertTrue(String.format("metrics are: %s", globalMetricNames), isFound);
    }

    @Test
    public void testSameArgs1() throws InterruptedException {
        String wuri = "workflow://workflow/metrics/testSameArgs1";
        createWorkflow(wuri);

        final CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> contextMap = new HashMap<String, String>();
        contextMap.put("arg1", "someValue");
        contextMap.put("arg2", "someValue");
        final String uri1 = Kernel.getDecision().createWorkOrder(context, wuri, contextMap);
        final String uri2 = Kernel.getDecision().createWorkOrder(context, wuri, contextMap);

        WaitingTestHelper.retry(new Runnable() {
            public void run() {
                WorkOrder w1 = Kernel.getDecision().getWorkOrder(context, uri1);
                assertEquals(WorkOrderExecutionState.FINISHED, w1.getStatus());
                WorkOrder w2 = Kernel.getDecision().getWorkOrder(context, uri2);
                assertEquals(WorkOrderExecutionState.FINISHED, w2.getStatus());

                assertEquals(WorkOrderInitialArgsHashStorage.readByFields(uri1).getHashValue(),
                        WorkOrderInitialArgsHashStorage.readByFields(uri2).getHashValue());

                try {
                    MetricsTestHelper.flushIfNeeded(metricsService);
                } catch (InterruptedException e) {
                    log.error(ExceptionToString.format(e));
                }

                assertTrue("metrics are " + globalMetricNames, globalMetricNames.size() >= 2);
                metricsContain(ARGS_FINISHED_PREFIX, "workflow.metrics-testSameArgs1");
                metricsContain(GENERIC_FINISHED_PREFIX, "workflow.metrics-testSameArgs1");

            }
        }, MAX_WAIT);

    }

    @Test
    public void testSameArgs2() throws InterruptedException {
        String wuri = "workflow://workflow/metrics/testSameArgs2";
        createWorkflow(wuri);

        final CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> contextMap = new HashMap<String, String>();
        contextMap.put("arg1", "someValue");
        contextMap.put("arg2", "someValue");
        contextMap.put(ContextVariables.TIMESTAMP, "" + System.currentTimeMillis());
        contextMap.put(ContextVariables.LOCAL_DATE, ContextVariables.FORMATTER.print(DateTime.now()));
        final String uri1 = Kernel.getDecision().createWorkOrder(context, wuri, contextMap);
        contextMap.put(ContextVariables.TIMESTAMP, "" + System.currentTimeMillis() + 5);
        contextMap.put(ContextVariables.LOCAL_DATE, ContextVariables.FORMATTER.print(DateTime.now().minusDays(1)));
        final String uri2 = Kernel.getDecision().createWorkOrder(context, wuri, contextMap);

        WaitingTestHelper.retry(new Runnable() {
            public void run() {
                WorkOrder w1 = Kernel.getDecision().getWorkOrder(context, uri1);
                assertEquals(WorkOrderExecutionState.FINISHED, w1.getStatus());
                WorkOrder w2 = Kernel.getDecision().getWorkOrder(context, uri2);
                assertEquals(WorkOrderExecutionState.FINISHED, w2.getStatus());

                assertEquals(WorkOrderInitialArgsHashStorage.readByFields(uri1).getHashValue(),
                        WorkOrderInitialArgsHashStorage.readByFields(uri2).getHashValue());
                try {
                    MetricsTestHelper.flushIfNeeded(metricsService);
                } catch (InterruptedException e) {
                    log.error(ExceptionToString.format(e));
                }

                assertTrue("metrics are " + globalMetricNames, globalMetricNames.size() >= 2);
                metricsContain(ARGS_FINISHED_PREFIX, "workflow.metrics-testSameArgs2");
                metricsContain(GENERIC_FINISHED_PREFIX, "workflow.metrics-testSameArgs2");

            }
        }, MAX_WAIT);
    }

    @Test
    public void testFromJob() throws InterruptedException, ExecutionException {
        String wuri = "workflow://workflow/metrics/testFromJob";
        createWorkflow(wuri);

        Map<String, String> jobParams = new HashMap<String, String>();
        final String jobURI = "job://workflow/metrics/jobName";
        Kernel.getSchedule().createWorkflowJob(CTX, jobURI, "desc", wuri, "* * * *", "America/New_York", jobParams, false, 500, null);
        runJob(jobParams, jobURI);
        verifyJobsFinished(jobURI, 1);
        jobParams.put("someArg", "someVal");
        runJob(jobParams, jobURI);
        verifyJobsFinished(jobURI, 2);

        runJob(jobParams, jobURI);
        verifyJobsFinished(jobURI, 3);

        //3 runs

        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {

                List<RaptureJobExec> execs = Kernel.getSchedule().getJobExecs(CTX, jobURI, 0, 3, false);
                RaptureJobExec exec1 = execs.get(0);
                RaptureJobExec exec2 = execs.get(1);
                RaptureJobExec exec3 = execs.get(2);

                assertTrue(String.format("exec1=%s, exec2=%s, exec3=%s", exec1, exec2, exec3), exec1.getStatus() ==
                        JobExecStatus.FINISHED && exec2.getStatus() == JobExecStatus.FINISHED && exec3.getStatus() == JobExecStatus.FINISHED);
                String uri1 = JacksonUtil.objectFromJson(exec1.getExecDetails(), WorkflowJobExecDetails.class).getWorkOrderURI();
                WorkOrder w1 = Kernel.getDecision().getWorkOrder(CTX, uri1);
                assertEquals(WorkOrderExecutionState.FINISHED, w1.getStatus());
                String uri2 = JacksonUtil.objectFromJson(exec2.getExecDetails(), WorkflowJobExecDetails.class).getWorkOrderURI();
                WorkOrder w2 = Kernel.getDecision().getWorkOrder(CTX, uri2);
                assertEquals(WorkOrderExecutionState.FINISHED, w2.getStatus());
                String uri3 = JacksonUtil.objectFromJson(exec3.getExecDetails(), WorkflowJobExecDetails.class).getWorkOrderURI();
                WorkOrder w3 = Kernel.getDecision().getWorkOrder(CTX, uri3);
                assertEquals(WorkOrderExecutionState.FINISHED, w3.getStatus());
                try {
                    MetricsTestHelper.flushIfNeeded(metricsService);
                } catch (InterruptedException e) {
                    log.error(ExceptionToString.format(e));
                }

                String hash1 = WorkOrderInitialArgsHashStorage.readByFields(uri1).getHashValue();
                String hash2 = WorkOrderInitialArgsHashStorage.readByFields(uri2).getHashValue();
                String hash3 = WorkOrderInitialArgsHashStorage.readByFields(uri3).getHashValue();
                String uriList = String
                        .format("exec1=%s\nexec2=%s\nexec3=%s\nuri1=%s\nuri2=%s\nuri3=%s\nhash1=%s\nhash2=%s\nhash3=%s", exec1, exec2, exec3, uri1, uri2, uri3,
                                hash1, hash2, hash3);
                assertNotEquals(uriList, hash1, hash2);
                assertEquals(uriList, hash2, hash3);

                assertTrue("metrics are " + globalMetricNames, globalMetricNames.size() >= 2); //2 by args, 1 by job, 1 by workflow
                metricsContain(ARGS_FINISHED_PREFIX, "workflow.metrics-testFromJob");
                metricsContain(GENERIC_FINISHED_PREFIX, "workflow.metrics-testFromJob");
                metricsContain(JOB_FINISHED_PREFIX, "workflow.metrics-jobName");
            }
        }, MAX_WAIT);
    }

    protected void runJob(final Map<String, String> jobParams, final String jobURI) throws InterruptedException, ExecutionException {
        Future<Boolean> future = scheduler.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                log.info("inside the future");
                JOB_SEMAPHORE.acquire();
                Kernel.getSchedule().runJobNow(CTX, jobURI, jobParams);
                return true;
            }
        });
        future.get();
        JOB_SEMAPHORE.acquire(); //when we can acquire again, it means the job ran
        JOB_SEMAPHORE.release(); //we don't need to hold it, release so next run can pick it up if it needs
    }

    private void verifyJobsFinished(final String jobURI, final int count) throws InterruptedException {
        WaitingTestHelper.retry(new Runnable() {
            @Override
            public void run() {
                List<RaptureJobExec> execs = Kernel.getSchedule().getJobExecs(CTX, jobURI, 0, count, false);
                assertEquals(count, execs.size());
                for (int i = 0; i < count; i++) {
                    RaptureJobExec exec = execs.get(i);
                    log.info(String.format("exec %s:=%s", i, exec));
                    assertEquals(String.format("exec %s=%s", i, exec), JobExecStatus.FINISHED, exec.getStatus());
                }

            }
        }, MAX_WAIT);
    }

}
