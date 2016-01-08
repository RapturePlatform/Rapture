/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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
package rapture.dp.invocable.core.script;

import rapture.config.ConfigLoader;
import org.junit.BeforeClass;
import org.junit.Test;
import rapture.common.CallingContext;
import rapture.common.WorkOrderExecutionState;
import rapture.common.dp.WorkOrderStatus;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.pipeline.PipelineConstants;
import rapture.dp.WaitingTestHelper;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.workflow.script.WorkflowScriptConstants;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

//@RunWith(PowerMockRunner.class)

public class ScriptBodyStepTest {

    private static final int TIMEOUT = 20000;
    private static final CallingContext CTX = ContextFactory.getKernelUser();

    @BeforeClass
    public static void beforeClass() {
        Kernel.initBootstrap();
        String systemBlobRepo = "//sys.blob";
        Kernel.getBlob().deleteBlobRepo(CTX, systemBlobRepo);
        Kernel.getBlob().createBlobRepo(CTX, systemBlobRepo, ConfigLoader.getConf().DefaultSystemBlobConfig,
                ConfigLoader.getConf().DefaultSystemBlobFoldersConfig);
        Kernel.getPipeline().setupStandardCategory(CTX, PipelineConstants.CATEGORY_ALPHA);
        Kernel.setCategoryMembership(PipelineConstants.CATEGORY_ALPHA);
    }

    @Test
    public void testInvoke() throws Exception {

        Map<String, String> params = ScriptHelper.createParams();
        Map<String, String> contextMap = new HashMap<String, String>();
        String scriptBody = ScriptHelper.createScript();
        contextMap.put(WorkflowScriptConstants.BODY, scriptBody);
        String paramsJson = JacksonUtil.jsonFromObject(params);
        contextMap.put(WorkflowScriptConstants.PARAMS, paramsJson);

        final String workOrderURI = Kernel.getDecision().createWorkOrder(CTX, WorkflowScriptConstants.URI, contextMap);
        WaitingTestHelper.retry(new Runnable() {
            @Override public void run() {
                WorkOrderStatus retVal = Kernel.getDecision().getWorkOrderStatus(CTX, workOrderURI);
                assertEquals(WorkOrderExecutionState.FINISHED, retVal.getStatus());
            }
        }, TIMEOUT);
    }

    @Test
    public void testAsyncCall() throws Exception {
        final String workOrderURI = Kernel.getAsync().asyncReflexScript(CTX, ScriptHelper.createScript(), ScriptHelper.createParams());
        WaitingTestHelper.retry(new Runnable() {
            @Override public void run() {
                WorkOrderStatus retVal = Kernel.getDecision().getWorkOrderStatus(CTX, workOrderURI);
                assertEquals(WorkOrderExecutionState.FINISHED, retVal.getStatus());
            }
        }, TIMEOUT);
    }

}
