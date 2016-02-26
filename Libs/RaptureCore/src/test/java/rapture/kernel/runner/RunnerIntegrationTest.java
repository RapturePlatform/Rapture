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

import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureApplicationInstance;
import rapture.common.exception.RaptureException;
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
public class RunnerIntegrationTest {
    @Before
    public void setup() {
        try {
            Kernel.initBootstrap(null, null, true);

        } catch (RaptureException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testServerConfig() {
        CallingContext ctx = ContextFactory.getKernelUser();

        Kernel.getRunner().createServerGroup(ctx, "ComputeFarm", "This is the compute farm group");
        Kernel.getRunner().createApplicationDefinition(ctx, "RaptureComputeServer", "0.99.9", "General Compute Server");
        Kernel.getRunner().createApplicationInstance(ctx, "Compute1", "An instance of the compute server", "ComputeFarm", "RaptureComputeServer", "1100 1400",
                3, "", "");

        List<RaptureApplicationInstance> serversToRun = Kernel.getRunner().getApplicationsForServer(ctx, "localhost");
        for (RaptureApplicationInstance x : serversToRun) {
            System.out.println("Would run " + x.getAppName());
        }
    }
}
