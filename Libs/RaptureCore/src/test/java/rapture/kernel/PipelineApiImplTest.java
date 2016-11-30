package rapture.kernel;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.CategoryQueueBindings;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.PipelineApi;

public class PipelineApiImplTest extends AbstractFileTest {

    private static CallingContext callingContext;
    private static PipelineApi pipeApi = Kernel.getPipeline();

    @BeforeClass
    static public void setUp() {
        AbstractFileTest.setUp();

        config.RaptureRepo = "REP {} USING FILE {prefix=\"/tmp/" + auth + "\"}";
        config.InitSysConfig = "NREP {} USING FILE { prefix=\"/tmp/" + auth + ".sys.config\"}";

        callingContext = new CallingContext();
        callingContext.setUser("dummy");

        Kernel.initBootstrap();
        callingContext = ContextFactory.getKernelUser();

        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {}");
    }

    @AfterClass
    static public void cleanUp() {
    }

    @Test
    public void testHappyPath() {
        String category = "foo";
        String domain = "foo";
        String rconfig = "";

        List<CategoryQueueBindings> be = pipeApi.getBoundExchanges(callingContext, category);
        pipeApi.createTopicExchange(callingContext, domain, category);
        pipeApi.registerExchangeDomain(callingContext, domain, rconfig);
    }

}
