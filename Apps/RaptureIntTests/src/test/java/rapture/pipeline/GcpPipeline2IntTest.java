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
package rapture.pipeline;

import java.io.IOException;

import javax.mail.MessagingException;

import org.junit.BeforeClass;
import org.junit.Test;

import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.pipeline2.gcp.PubsubPipeline2Handler;

public class GcpPipeline2IntTest extends BasePipeline2IntTest {

    @BeforeClass
    public static void setUp() {
        RaptureConfig.setLoadYaml(false);
        config = ConfigLoader.getConf();
        config.DefaultExchange = "PIPELINE {} USING GCP_PUBSUB { projectid=\"todo3-incap\"}";

        MessagingException me = new MessagingException();

        Kernel.initBootstrap();
        context = ContextFactory.getKernelUser();
        papi = Kernel.getPipeline2().getTrusted();

        PubsubPipeline2Handler pp2h = new PubsubPipeline2Handler();
        pp2h.setInstanceName("test");

        // NOTE: This gets done in the _startup.rfx script
        // papi.registerExchangeDomain(context, "//" + domain, config.DefaultExchange);
        // papi.createTopicExchange(context, domain, "raptureTopic");
        // papi.setupStandardCategory(context, category);
    }

    @Override
    @Test
    public void testSimpleTopicMessage() {
        super.testSimpleTopicMessage();
    }

    // This verifies that the Jar cache gets marked as dirty when a jar file is updated.
    // It demonstrates that the Kernel is using the pipeline internally
    @Test
    @Override
    public void testBroadcastMessage() throws IOException {
        super.testBroadcastMessage();
    }

    @Override
    @Test
    public void testTaskWithResponse() {
        super.testTaskWithResponse();
    }

    @Override
    @Test
    public void testTaskWithTimeout() {
        super.testTaskWithTimeout();
    }

    @Override
    @Test
    public void testTaskWithTwoListeners() {
        super.testTaskWithTwoListeners();
    }

}
