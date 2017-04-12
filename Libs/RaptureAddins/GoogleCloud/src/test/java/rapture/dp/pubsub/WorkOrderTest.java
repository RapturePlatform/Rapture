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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static rapture.dp.DPTestUtil.ALPHA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.JobType;
import rapture.common.QueueSubscriber;
import rapture.common.RaptureConstants;
import rapture.common.RaptureJob;
import rapture.common.RaptureJobExec;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.dp.Step;
import rapture.common.dp.WorkOrder;
import rapture.common.dp.Workflow;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.dp.WorkflowFactory;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.Pipeline2ApiImpl;

public class WorkOrderTest {

    private static final Logger log = Logger.getLogger(WorkOrderTest.class);

    private CallingContext ctx = ContextFactory.getKernelUser();

    private static final String REPO_URI = "//testRepoUrixyz";

    private static final String workflowUri = "workflow://testthisworkflow/wf";
    private static final String workflowUri2 = "workflow://anotherworkflow/xx";
    private QueueSubscriber subscriber = null;

    @Before
    public void setup() {
        Kernel.getKernel().restart();
        Pipeline2ApiImpl.usePipeline2 = true;
        RaptureConfig config = ConfigLoader.getConf();
        config.DefaultExchange = "PIPELINE {} USING GCP_PUBSUB { projectid=\"todo3-incap\"}";

        Kernel.initBootstrap();

        subscriber = Kernel.createAndSubscribe(ALPHA, "PIPELINE {} USING GCP_PUBSUB { projectid=\"todo3-incap\"}");
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {prefix=\"/tmp/" + UUID.randomUUID() + "\"}");

        String scr1 = "script1";
        Kernel.getScript().deleteScript(ctx, REPO_URI + "/" + scr1);
        Kernel.getScript().createScript(ctx, REPO_URI + "/" + scr1, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM,
                "println(\"Hello there\"); return \"ok\";");

        List<Step> steps = new ArrayList<>();
        Step s1 = new Step();
        s1.setName("start");
        s1.setExecutable("script://" + REPO_URI + "/" + scr1);
        s1.setTransitions(WorkflowFactory.createTransition("ok", "$RETURN"));
        steps.add(s1);

        Workflow workflow = new Workflow();
        workflow.setSteps(steps);
        workflow.setWorkflowURI(workflowUri);
        workflow.setStartStep("start");

        Kernel.getDecision().putWorkflow(ctx, workflow);

        Workflow workflow2 = new Workflow();
        workflow2.setSteps(steps);
        workflow2.setWorkflowURI(workflowUri2);
        workflow2.setStartStep("start");

        Kernel.getDecision().putWorkflow(ctx, workflow2);
    }

    @After
    public void tearDown() throws Exception {
        if (subscriber != null) Kernel.getPipeline2().unsubscribeQueue(ctx, subscriber);
    }

    @Test
    public void testGetWorkOrderByWorkflowOnEmptySystem() {
        List<String> ret = Kernel.getDecision().getWorkOrdersByWorkflow(ctx, System.currentTimeMillis(), "//doesntexist/soidontcare/coolstorybro");
        assertNotNull(ret);
        assertTrue(ret.isEmpty());
    }

    @Test
    public void testGetWorkOrdersByWorkflow() {
        final String workOrderUri = Kernel.getDecision().createWorkOrder(ctx, workflowUri, null);
        Kernel.getDecision().createWorkOrder(ctx, workflowUri2, null);

        List<String> ret = Kernel.getDecision().getWorkOrdersByWorkflow(ctx, System.currentTimeMillis(), workflowUri);
        assertEquals(1, ret.size());
        assertEquals(workOrderUri, ret.get(0));

        // test past date
        DateTime inThePast = new DateTime(System.currentTimeMillis(), DateTimeZone.UTC).withTimeAtStartOfDay();
        inThePast = inThePast.minusDays(10);
        ret = Kernel.getDecision().getWorkOrdersByWorkflow(ctx, inThePast.getMillis(), workflowUri);
        assertEquals(1, ret.size());
        assertEquals(workOrderUri, ret.get(0));

        // test future
        DateTime inTheFuture = new DateTime(System.currentTimeMillis(), DateTimeZone.UTC).withTimeAtStartOfDay();
        inTheFuture = inTheFuture.plusDays(1);
        ret = Kernel.getDecision().getWorkOrdersByWorkflow(ctx, inTheFuture.getMillis(), workflowUri);
        assertEquals(0, ret.size());

        // test diff workflow
        ret = Kernel.getDecision().getWorkOrdersByWorkflow(ctx, inThePast.getMillis(), "//unknown/x");
        assertEquals(0, ret.size());

        // test null param
        ret = Kernel.getDecision().getWorkOrdersByWorkflow(ctx, null, workflowUri);
        assertEquals(1, ret.size());
        assertEquals(workOrderUri, ret.get(0));

    }

    @Test
    public void testGetWorkOrderStatusesByWorkflow() {
        final String workOrderUri = Kernel.getDecision().createWorkOrder(ctx, workflowUri, null);
        Map<String, List<String>> statuses = Kernel.getDecision().getWorkOrderStatusesByWorkflow(ctx, System.currentTimeMillis(), workflowUri);
        assertEquals(1, statuses.size());
        assertEquals(WorkOrderExecutionState.NEW.toString(), statuses.entrySet().iterator().next().getKey());
        assertEquals(workOrderUri, statuses.entrySet().iterator().next().getValue().get(0));
    }

    @Test
    public void testGetWorkOrderByJobExec() {
        final String jobUri = "job://workorderjobz/job1";
        RaptureJobExec r = new RaptureJobExec();
        r.setJobType(JobType.WORKFLOW);
        r.setJobURI(jobUri);
        r.setExecTime(System.currentTimeMillis());
        assertNull(Kernel.getDecision().getWorkOrderByJobExec(ctx, r));
        RaptureJob job = Kernel.getSchedule().createWorkflowJob(ctx, jobUri, null, workflowUri, "* * * * *", "America/New_York", new HashMap<>(), false, 1,
                null);
        WorkOrder ret = Kernel.getDecision().getWorkOrderByJobExec(ctx, r);
        assertNull("Work order should be null", ret);
        String returnedWorkOrderUri = Kernel.getSchedule().runJobNow(ctx, jobUri, null);
        assertNotNull("Work order URI should not be null", returnedWorkOrderUri);
        List<RaptureJobExec> rje = Kernel.getSchedule().getJobExecs(ctx, jobUri, 0, 1, false);
        log.info("jobexec is: " + JacksonUtil.jsonFromObject(rje.get(0)));
        ret = Kernel.getDecision().getWorkOrderByJobExec(ctx, rje.get(0));
        assertNotNull("Work order should not be null", ret);
        assertEquals(workflowUri, ret.getWorkflowURI());
    }

    @Test
    public void testGetJobExecsAndWorkOrdersByDay() {
        final String jobUri = "job://workorderjobz/job2";
        Kernel.getSchedule().createWorkflowJob(ctx, jobUri, null, workflowUri, "* * * * *", "America/New_York", new HashMap<>(), false, 1, null);
        String returnedWorkOrderUri = Kernel.getSchedule().runJobNow(ctx, jobUri, null);
        assertNotNull(returnedWorkOrderUri);
        Map<RaptureJobExec, WorkOrder> ret = Kernel.getDecision().getJobExecsAndWorkOrdersByDay(ctx, System.currentTimeMillis());
        assertEquals(1, ret.size());
        Map.Entry<RaptureJobExec, WorkOrder> entry = ret.entrySet().iterator().next();
        assertEquals(jobUri, entry.getKey().getJobURI());
        assertEquals(workflowUri, entry.getValue().getWorkflowURI());
    }

}
