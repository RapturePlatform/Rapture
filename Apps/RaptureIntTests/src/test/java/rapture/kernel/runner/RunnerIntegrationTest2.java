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
package rapture.kernel.runner;

import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureApplicationInstance;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

/**
 * Create a test that puts a script to be run on a schedule, and show that it
 * runs on that schedule through the pipeline and that the job exec status is
 * updated correctly.
 * 
 * @author amkimian
 * 
 */
public class RunnerIntegrationTest2 {
    @Before
    public void setup() {

        rapture.pipeline2.gcp.PubsubPipeline2Handler pp2h;

        RaptureConfig.setLoadYaml(false);
        ConfigLoader.getConf().DefaultExchange = "PIPELINE {} USING GCP_PUBSUB { projectid=\"todo3-incap\"}";

        try {
            Kernel.initBootstrap(null, null, true);
        } catch (RaptureException e) {
            String error = ExceptionToString.format(e);
            if (error.contains("The Application Default Credentials are not available.") || error.contains("RESOURCE_EXHAUSTED")) Assume.assumeNoException(e);
            throw e;
        }
    }

    @Test
    public void testServerConfig() {
        CallingContext ctx = ContextFactory.getKernelUser();
        String group = UUID.randomUUID().toString();
        String server = UUID.randomUUID().toString();
        String version = "1.2.3";

        Kernel.getRunner().createServerGroup(ctx, group, "A group I just created");
        Kernel.getRunner().createApplicationDefinition(ctx, server, version, "A Server");
        Kernel.getRunner().createApplicationInstance(ctx, "Compute1", "An instance of a server", group, server, null, -1, null, null);

        List<RaptureApplicationInstance> serversToRun = Kernel.getRunner().getApplicationsForServer(ctx, "localhost");
        Assert.assertEquals(1, serversToRun.size());
        Assert.assertEquals(server, serversToRun.get(0).getAppName());
    }
}
