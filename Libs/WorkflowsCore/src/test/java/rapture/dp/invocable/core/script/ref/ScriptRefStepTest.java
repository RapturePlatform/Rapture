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
package rapture.dp.invocable.core.script.ref;

import rapture.config.ConfigLoader;
import org.junit.BeforeClass;
import org.junit.Test;
import rapture.common.*;
import rapture.common.dp.WorkOrderStatus;
import rapture.common.pipeline.PipelineConstants;
import rapture.dp.WaitingTestHelper;
import rapture.dp.invocable.core.script.ScriptHelper;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import static org.junit.Assert.assertEquals;

//@RunWith(PowerMockRunner.class)

public class ScriptRefStepTest {

    private static final int TIMEOUT = 20000;
    private static final CallingContext CTX = ContextFactory.getKernelUser();
    private static final String SCRIPT_URI = RaptureURI.builder(Scheme.SCRIPT, "test").docPath("test1.rfx").build().toString();

    @BeforeClass
    public static void beforeClass() {
        Kernel.initBootstrap();
        String systemBlobRepo = "//sys.blob";
        Kernel.getBlob().deleteBlobRepo(CTX, systemBlobRepo);
        Kernel.getBlob().createBlobRepo(CTX, systemBlobRepo, ConfigLoader.getConf().DefaultSystemBlobConfig,
                ConfigLoader.getConf().DefaultSystemBlobFoldersConfig);
        Kernel.getPipeline().setupStandardCategory(CTX, PipelineConstants.CATEGORY_ALPHA);
        Kernel.setCategoryMembership(PipelineConstants.CATEGORY_ALPHA);

        String script = ScriptHelper.createScript();

        Kernel.getScript().createScript(CTX, SCRIPT_URI, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.OPERATION, script);
    }

    @Test
    public void testAsyncCall() throws Exception {

        final String workOrderURI = Kernel.getAsync().asyncReflexReference(CTX, SCRIPT_URI, ScriptHelper.createParams());

        WaitingTestHelper.retry(new Runnable() {
            @Override public void run() {
                WorkOrderStatus retVal = Kernel.getDecision().getWorkOrderStatus(CTX, workOrderURI);
                assertEquals(WorkOrderExecutionState.FINISHED, retVal.getStatus());
            }
        }, TIMEOUT);
    }

    @Test
    public void testAsyncBadURI() throws Exception {

        final String workOrderURI = Kernel.getAsync().asyncReflexReference(CTX, SCRIPT_URI + ".bad", ScriptHelper.createParams());

        WaitingTestHelper.retry(new Runnable() {
            @Override public void run() {
                WorkOrderStatus retVal = Kernel.getDecision().getWorkOrderStatus(CTX, workOrderURI);
                assertEquals(WorkOrderExecutionState.ERROR, retVal.getStatus());
            }
        }, TIMEOUT);
    }
}
