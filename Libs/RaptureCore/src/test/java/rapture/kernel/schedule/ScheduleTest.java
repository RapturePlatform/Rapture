package rapture.kernel.schedule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureJobExec;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.dp.Step;
import rapture.common.dp.Workflow;
import rapture.dp.WorkflowFactory;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class ScheduleTest {

    private CallingContext ctx = ContextFactory.getKernelUser();

    private static final String REPO_URI = "//scheduleTestRepo";

    private static final String workflowUri = "workflow://scheduleTestWorkflows/wf";
    private static final String scriptWithoutSleep = REPO_URI + "/scriptWithoutSleep";
    private static final String scriptWithSleep = REPO_URI + "/scriptWithSleep";

    @Before
    public void setup() {
        Kernel.INSTANCE.restart();
        Kernel.initBootstrap();
        Kernel.setCategoryMembership("alpha");
        Kernel.getScript().deleteScript(ctx, scriptWithoutSleep);
        Kernel.getScript().createScript(ctx, scriptWithoutSleep, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM,
                "println(\"Hello there\"); //sleep(5000); return \"ok\";");
        Kernel.getScript().deleteScript(ctx, scriptWithSleep);
        Kernel.getScript().createScript(ctx, scriptWithSleep, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM,
                "println(\"Hello there\"); sleep(5000); return \"ok\";");
        List<Step> steps = new ArrayList<Step>();
        Step s1 = new Step();
        s1.setName("start");
        s1.setExecutable("script://" + scriptWithoutSleep);
        s1.setTransitions(WorkflowFactory.createTransition("ok", "$RETURN"));
        steps.add(s1);
        Workflow workflow = new Workflow();
        workflow.setSteps(steps);
        workflow.setWorkflowURI(workflowUri);
        workflow.setStartStep("start");
        Kernel.getDecision().putWorkflow(ctx, workflow);
    }

    @Test
    public void testGetRunningWorkflowJobs() throws InterruptedException {
        List<RaptureJobExec> res = Kernel.getSchedule().getRunningWorkflowJobs(ctx);
        assertNotNull(res);
        assertTrue(res.isEmpty());
        final String jobUri = "job://scheduleTest/job1";
        Kernel.getSchedule().createWorkflowJob(ctx, jobUri, null, workflowUri, "* * * * *", "America/New_York", new HashMap<>(), false, 1, null);
        Kernel.getSchedule().runJobNow(ctx, jobUri, null);
        Thread.sleep(1000);
        res = Kernel.getSchedule().getRunningWorkflowJobs(ctx);
        assertTrue(res.isEmpty());
        Workflow wf = Kernel.getDecision().getWorkflow(ctx, workflowUri);
        wf.getSteps().get(0).setExecutable("script://" + scriptWithSleep);
        Kernel.getDecision().putWorkflow(ctx, wf);
        Kernel.getSchedule().runJobNow(ctx, jobUri, null);
        Thread.sleep(1000);
        res = Kernel.getSchedule().getRunningWorkflowJobs(ctx);
        assertFalse(res.isEmpty());
    }

}
