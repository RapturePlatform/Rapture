package rapture.kernel.scripting;

import org.antlr.runtime.RecognitionException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.script.KernelScript;

public class ReflexCallTest extends ResourceBasedTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testCall() throws RecognitionException {
        Kernel.initBootstrap();

        KernelScript ks = new KernelScript();
        ks.setCallingContext(ContextFactory.getKernelUser());
        scriptApi = ks;
        String output = runTestFor("/call.rfx");
        System.out.println(output);
    }
}
