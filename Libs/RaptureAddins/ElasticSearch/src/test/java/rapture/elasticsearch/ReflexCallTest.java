package rapture.elasticsearch;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.RecognitionException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.mime.MimeSearchUpdateObject;
import rapture.common.model.DocumentWithMeta;
import rapture.common.model.RaptureExchange;
import rapture.common.model.RaptureExchangeQueue;
import rapture.common.model.RaptureExchangeType;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.script.KernelScript;
import reflex.ResourceBasedTest;

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

            RaptureConfig config = ConfigLoader.getConf();
            config.FullTextSearchOn = true;
            Kernel.getSearch().createSearchRepo(context, "search://main", "SEARCH {} USING ELASTIC { index = \"rapturemain\"}");
            Kernel.getSearch().startSearchRepos(context);

        } catch (RaptureException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testJson() throws RecognitionException {
        MimeSearchUpdateObject msuo = new MimeSearchUpdateObject();
        msuo.setDoc(new DocumentWithMeta());
        String j = JacksonUtil.jsonFromObject(msuo);
        MimeSearchUpdateObject msuo2 = JacksonUtil.objectFromJson(j, MimeSearchUpdateObject.class);
        System.out.println(j);
    }

    @Test
    public void testRap4084() throws RecognitionException {
        runTestFor("/rap4084.rfx");
    }
}
