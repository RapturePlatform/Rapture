package rapture.kernel.scripting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.antlr.runtime.RecognitionException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.script.KernelScript;

public class ReflexLongSeriesTest extends ResourceBasedTest {

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
        Kernel.INSTANCE.restart();
        Kernel.initBootstrap(null, null, true);
        KernelScript ks = new KernelScript();
        ks.setCallingContext(context);
        scriptApi = ks;
    }

    @Test
    public void testLongSeries() throws RecognitionException {
        String output = runTestFor("/longseries.rfx");
        assertNotNull(output);
        assertEquals(output, "--RETURNS--{\"value\":true}");
    }
}
