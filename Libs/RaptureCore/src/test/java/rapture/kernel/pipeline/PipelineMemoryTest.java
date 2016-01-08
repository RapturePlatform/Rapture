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
package rapture.kernel.pipeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RapturePipelineTask;
import rapture.common.exception.RaptureException;
import rapture.common.mime.MimeReflexScript;
import rapture.common.model.RaptureExchange;
import rapture.common.model.RaptureExchangeQueue;
import rapture.common.model.RaptureExchangeType;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

@SuppressWarnings("all")
public class PipelineMemoryTest {

    private CallingContext ctx;

    @Before
    public void setup() {
        ctx = ContextFactory.getKernelUser();

        try {
            Kernel.INSTANCE.restart();
            Kernel.initBootstrap(null, null, true);

            Kernel.getPipeline().getTrusted().registerServerCategory(ctx, "alpha", "Primary servers");
            Kernel.getPipeline().getTrusted().registerServerCategory(ctx, "beta", "Secondary servers");

            Kernel.getPipeline().registerExchangeDomain(ctx, "//main", "EXCHANGE {} USING MEMORY {}");

            RaptureExchange exchange = new RaptureExchange();
            exchange.setName("kernel");
            exchange.setName("kernel");
            exchange.setExchangeType(RaptureExchangeType.FANOUT);
            exchange.setDomain("main");

            List<RaptureExchangeQueue> queues = new ArrayList<RaptureExchangeQueue>();
            RaptureExchangeQueue queue = new RaptureExchangeQueue();
            queue.setName("default");
            queue.setRouteBindings(new ArrayList<String>());
            queues.add(queue);

            exchange.setQueueBindings(queues);

            Kernel.getPipeline().getTrusted().registerPipelineExchange(ctx, "kernel", exchange);
            Kernel.getPipeline().getTrusted().bindPipeline(ctx, "alpha", "kernel", "default");

            // Now that the binding is setup, register our server as being part
            // of
            // "alpha"

            Kernel.setCategoryMembership("alpha");

        } catch (RaptureException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testExchanges() {
        List<String> exchanges = Kernel.getPipeline().getExchanges(ctx);
        int originalSize = exchanges.size();
        assertTrue(String.format("Exchanges are [%s]", StringUtils.join(exchanges, ",")), exchanges.contains("kernel"));

        Kernel.getPipeline().deregisterPipelineExchange(ctx, "kernel");
        exchanges = Kernel.getPipeline().getExchanges(ctx);
        assertEquals(originalSize - 1, exchanges.size());
    }

    @Test
    public void testExchangeDomains() {
        List<String> domains = Kernel.getPipeline().getExchangeDomains(ctx);
        int originalSize = domains.size();
        assertTrue(domains.contains("main"));

        Kernel.getPipeline().deregisterExchangeDomain(ctx, "//main");
        domains = Kernel.getPipeline().getExchangeDomains(ctx);
        assertEquals(--originalSize, domains.size());

        Kernel.getPipeline().registerExchangeDomain(ctx, "//t1", "EXCHANGE {} USING MEMORY {}");
        Kernel.getPipeline().registerExchangeDomain(ctx, "//t2", "EXCHANGE {} USING MEMORY {}");
        domains = Kernel.getPipeline().getExchangeDomains(ctx);
        assertTrue(domains.contains("t1"));
        assertTrue(domains.contains("t2"));
    }

    @Test
    public void testKernelPipeline() {

        RapturePipelineTask pTask = new RapturePipelineTask();
        pTask.setContent("This is the content");
        pTask.setContentType("text/plain");
        pTask.setPriority(1);
        pTask.setCategoryList(new ArrayList<String>());

        Kernel.getPipeline().getTrusted().publishPipelineMessage(ctx, "kernel", pTask);

        MimeReflexScript reflexScript = new MimeReflexScript();
        reflexScript.setReflexScript("println('Hello from the Reflex Script');");
        pTask.addMimeObject(reflexScript);

        pTask.setPriority(1);
        pTask.setCategoryList(new ArrayList<String>());
        pTask.setContentType("application/vnd.rapture.reflex.script");

        Kernel.getPipeline().getTrusted().publishPipelineMessage(ctx, "kernel", pTask);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
