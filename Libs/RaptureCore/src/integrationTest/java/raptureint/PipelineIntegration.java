/**
 * Copyright (C) 2011-2015 Incapture Technologies LLC
 *
 * This is an autogenerated license statement. When copyright notices appear below
 * this one that copyright supercedes this statement.
 *
 * Unless required by applicable law or agreed to in writing, software is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * Unless explicit permission obtained in writing this software cannot be distributed.
 */
package raptureint;

import java.util.ArrayList;
import java.util.List;

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
import rapture.util.ResourceLoader;
import rapture.config.MultiValueConfigLoader;
import rapture.config.ValueReader;

@SuppressWarnings("all")
public class PipelineIntegration {
    @Before
    public void setup() {
        CallingContext ctx = ContextFactory.getKernelUser();
        MultiValueConfigLoader.setEnvReader(new ValueReader() {

            @Override
            public String getValue(String property) {
                if (property.equals("RABBITMQ-DEFAULT")) {
                    return "amqp://guest:guest@localhost:5672/%2f";
                }
                return null;
            }

        });
        try {
            Kernel.initBootstrap(null, null, true);

            Kernel.getPipeline().getTrusted().registerServerCategory(ctx, "alpha", "Primary servers");
            Kernel.getPipeline().getTrusted().registerServerCategory(ctx, "beta", "Secondary servers");

            Kernel.getPipeline().registerExchangeDomain(ctx, "//main", "EXCHANGE {} USING RABBITMQ {}");

            RaptureExchange exchange = new RaptureExchange();
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

    /*
     * @Test public void testKernelPipeline() { CallingContext ctx = ContextFactory.getKernelUser();
     * 
     * 
     * RapturePipelineTask pTask = new RapturePipelineTask();
     * 
     * MimeReflexScript reflexScript = new MimeReflexScript(); reflexScript .setReflexScript("println('Hello from the Reflex Script');");
     * pTask.addMimeObject(reflexScript);
     * 
     * pTask.setPriority(1); pTask.setCategoryList(new ArrayList<String>()); pTask.setContentType(MimeReflexScript.getMimeType());
     * 
     * Kernel.getPipeline().publishPipelineMessage(ctx, "kernel", pTask);
     * 
     * try { Thread.sleep(2000); } catch (InterruptedException e) { // TODO Auto-generated catch block e.printStackTrace(); }
     * 
     * }
     */

    @Test
    public void testKernelPipelineSuspend() {
        CallingContext ctx = ContextFactory.getKernelUser();

        RapturePipelineTask pTask = new RapturePipelineTask();

        MimeReflexScript reflexScript = new MimeReflexScript();
        reflexScript.setReflexScript(ResourceLoader.getResourceAsString(this, "/suspendPipelineTest.rfx"));
        pTask.addMimeObject(reflexScript);
        pTask.setPriority(1);
        pTask.setCategoryList(new ArrayList<String>());
        pTask.setContentType(MimeReflexScript.getMimeType());

        Kernel.getPipeline().getTrusted().publishPipelineMessage(ctx, "kernel", pTask);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
