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
package rapture.workflow.script.ref;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.dp.Step;
import rapture.common.dp.Workflow;
import rapture.common.pipeline.PipelineConstants;
import java.util.LinkedList;
import java.util.List;

/**
 * @author bardhi
 * @since 8/19/14.
 */
public class ScriptRefWorkflowFactory {

    private static final String STEP_NAME = "core.script.ref.ScriptRefStep";

    public static Workflow create() {
        Workflow w = new Workflow();
        w.setWorkflowURI(WorkflowScriptRefConstants.URI);
        w.setCategory(PipelineConstants.CATEGORY_ALPHA);

        w.setStartStep(STEP_NAME);
        List<Step> steps = new LinkedList<Step>();
        Step step = new Step();
        step.setDescription("Run a script passed whose URI is passed in as an argument to the work order");
        step.setName(STEP_NAME);
        step.setExecutable(RaptureURI.builder(Scheme.DP_JAVA_INVOCABLE, STEP_NAME).build().toString());
        steps.add(step);
        w.setSteps(steps);

        return w;
    }
}
