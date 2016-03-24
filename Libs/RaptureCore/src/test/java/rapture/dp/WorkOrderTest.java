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

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.dp.Step;
import rapture.common.dp.Workflow;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class WorkOrderTest {

    private CallingContext ctx = ContextFactory.getKernelUser();

    private static final String REPO_URI = "//testRepoUrixyz";

    @Before
    public void setup() {
        Kernel.initBootstrap();
    }

    @Test
    public void testGetWorkOrdersByWorkflow() {
        String scr1 = "script1";
        Kernel.getScript().deleteScript(ctx, REPO_URI + "/" + scr1);
        Kernel.getScript().createScript(ctx, REPO_URI + "/" + scr1, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM,
                "println(\"Hello there\"); return \"ok\";");
        String workflowUri = "workflow://testthisworkflow/wf";
        List<Step> steps = new ArrayList<Step>();
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
        final String workOrderUri = Kernel.getDecision().createWorkOrder(ctx, workflowUri, null);

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

}
