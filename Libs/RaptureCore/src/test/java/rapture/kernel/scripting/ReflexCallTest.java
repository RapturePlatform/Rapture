package rapture.kernel.scripting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.RecognitionException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.WorkOrderExecutionState;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.RaptureExchange;
import rapture.common.model.RaptureExchangeQueue;
import rapture.common.model.RaptureExchangeType;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.script.KernelScript;
import reflex.value.ReflexValue;

public class ReflexCallTest extends ResourceBasedTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    private CallingContext context;

    @Before
    public void setup() {
        context = ContextFactory.getKernelUser();

        try {
            Kernel.INSTANCE.restart();
            Kernel.initBootstrap(null, null, true);

            Kernel.getPipeline().getTrusted().registerServerCategory(context, "alpha", "Primary servers");
            Kernel.getPipeline().getTrusted().registerServerCategory(context, "beta", "Secondary servers");

            Kernel.getPipeline().registerExchangeDomain(context, "//main", "EXCHANGE {} USING MEMORY {}");

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

            Kernel.getPipeline().getTrusted().registerPipelineExchange(context, "kernel", exchange);
            Kernel.getPipeline().getTrusted().bindPipeline(context, "alpha", "kernel", "default");

            // Now that the binding is setup, register our server as being part
            // of
            // "alpha"

            Kernel.setCategoryMembership("alpha");

            KernelScript ks = new KernelScript();
            ks.setCallingContext(context);
            scriptApi = ks;

        } catch (RaptureException e) {
            e.printStackTrace();
        }
    }

    // This test requires WorkflowsCore to run, but not to compile.
    // To test it add testRuntime 'net.rapture:WorkflowsCore:3+' to build.gradle
    // Do not use testRuntime project(':WorkflowsCore') as this creates a cyclic dependency
    @Ignore
    @Test
    public void testCall() throws RecognitionException {
        String output = runTestFor("/call.rfx");
        assertNotNull(output);
        System.out.println(output);
        ReflexValue wos = JacksonUtil.objectFromJson(output.substring(output.indexOf('{')), ReflexValue.class);
        assertNotNull(wos);
        Map<String, Object> map = wos.asMap();
        assertNotNull(map);
        assertEquals(output, WorkOrderExecutionState.FINISHED.toString(), map.get("status"));
        String workerOutput = map.get("workerOutput").toString();
        assertNotNull(workerOutput);
        assertEquals(output, "Hello world\n}", workerOutput.substring(workerOutput.indexOf('=') + 1));
    }
}
