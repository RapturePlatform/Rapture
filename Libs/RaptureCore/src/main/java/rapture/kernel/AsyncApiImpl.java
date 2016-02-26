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
package rapture.kernel;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.api.AsyncApi;
import rapture.common.dp.WorkOrderStatus;
import rapture.common.dp.Workflow;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.workflow.script.ScriptBodyWorkflowFactory;
import rapture.workflow.script.WorkflowScriptConstants;
import rapture.workflow.script.ref.ScriptRefWorkflowFactory;
import rapture.workflow.script.ref.WorkflowScriptRefConstants;

/**
 * Async is all about putting tasks on the Kernel pipeline
 *
 * @author amkimian
 */
public class AsyncApiImpl extends KernelBase implements AsyncApi {
    // 90000000099900
    // this is a comment using the tekgear twiddler!

    public AsyncApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(AsyncApiImpl.class);

    @SuppressWarnings("unused")
    private Map<String, Object> convertParams(Map<String, String> params) {
        Map<String, Object> ret = new HashMap<String, Object>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            ret.put(entry.getKey(), entry.getValue());
        }
        return ret;
    }

    @Override
    public String asyncReflexScript(CallingContext context, String scriptBody, Map<String, String> params) {

        Map<String, String> contextMap = new HashMap<String, String>();
        contextMap.put(WorkflowScriptConstants.BODY, scriptBody);
        String paramsJson = JacksonUtil.jsonFromObject(params);
        contextMap.put(WorkflowScriptConstants.PARAMS, paramsJson);

        return Kernel.getDecision().createWorkOrder(context, WorkflowScriptConstants.URI, contextMap);
    }

    @Override
    public String asyncReflexReference(CallingContext context, String scriptURI, Map<String, String> params) {

        Map<String, String> contextMap = new HashMap<String, String>();
        contextMap.put(WorkflowScriptRefConstants.SCRIPT_URI, scriptURI);
        String paramsJson = JacksonUtil.jsonFromObject(params);
        contextMap.put(WorkflowScriptRefConstants.PARAMS, paramsJson);

        return Kernel.getDecision().createWorkOrder(context, WorkflowScriptRefConstants.URI, contextMap);
    }

    @Override
    public WorkOrderStatus asyncStatus(CallingContext context, String workOrderURI) {
        return Kernel.getDecision().getWorkOrderStatus(context, workOrderURI);
    }

    @Override
    public void setupDefaultWorkflows(CallingContext context, Boolean force) {
        defineWorkflow(context, force, ScriptRefWorkflowFactory.create());
        defineWorkflow(context, force, ScriptBodyWorkflowFactory.create());
    }

    private void defineWorkflow(CallingContext context, Boolean force, Workflow workflow) {
        if (force || Kernel.getDecision().getWorkflow(context, workflow.getWorkflowURI()) == null) {
            Kernel.getDecision().putWorkflow(context, workflow);
        }
    }

}
