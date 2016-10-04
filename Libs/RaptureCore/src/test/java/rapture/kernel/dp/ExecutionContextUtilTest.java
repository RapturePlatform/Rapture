package rapture.kernel.dp;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class ExecutionContextUtilTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() {
        Map<String, String> m = new HashMap<>();
        CallingContext callingContext = ContextFactory.getKernelUser();
        String workOrderUri = RaptureURI.builder(Scheme.WORKORDER, "/test/").asString();
        Kernel.getDoc().putDoc(callingContext, "document://foo/bar",
                JacksonUtil.jsonFromObject(ImmutableMap.of("FOO", "!document://foo/bar#FOO", "BAR", "bar")));
        Kernel.getDecision().setContextLink(callingContext, workOrderUri, "FOO", "document://foo/bar#FOO");
        Kernel.getDecision().setContextLink(callingContext, workOrderUri, "BAR", "document://foo/bar#BAR");
        Kernel.getDecision().setContextLiteral(callingContext, workOrderUri, "FLIP", "FLOP");
        Kernel.getDecision().setContextLink(callingContext, workOrderUri, "WIBBLE", "document://foo/bar#WIBBLE$Undefined");
        Assert.assertNull(ExecutionContextUtil.getValueECF(callingContext, workOrderUri, "FOO", m));

        Kernel.getDecision().setContextTemplate(callingContext, workOrderUri, "Bar", "%%%%%%%%%%%%%%%%%%Foo");
        Kernel.getDecision().setContextTemplate(callingContext, workOrderUri, "Baz", "%%%%%%%%%%%%%%%%%%%%%%%Too many dereferences");
        Assert.assertEquals("bar", ExecutionContextUtil.getValueECF(callingContext, workOrderUri, "BAR", m));
        Assert.assertEquals("Foo", ExecutionContextUtil.getValueECF(callingContext, workOrderUri, "Bar", m));
        Assert.assertNull(ExecutionContextUtil.getValueECF(callingContext, workOrderUri, "Baz", m));
        Assert.assertEquals("Undefined", ExecutionContextUtil.getValueECF(callingContext, workOrderUri, "WIBBLE", m));

        Assert.assertEquals("FLOP", ExecutionContextUtil.getValueECF(callingContext, workOrderUri, "%${FLIP}", m));

        // ${X$y} evaluates to $X if X is set otherwise y

        Assert.assertEquals("FLIP", ExecutionContextUtil.getValueECF(callingContext, workOrderUri, "%${FLOP$FLIP}", m));

        // ${$X} requires that X be defined
        try {
            ExecutionContextUtil.getValueECF(callingContext, workOrderUri, "%${$FOO}", m);
            Assert.fail("Expected exception");
        } catch (Exception e) {
            Assert.assertEquals("Variable ${$FOO} required but missing", e.getMessage());
        }

        // So ${X$$Y} evaluates to $X if X is defined else $Y and throws an exception if $Y is not defined
        Assert.assertEquals("FLOP", ExecutionContextUtil.getValueECF(callingContext, workOrderUri, "%${UNDEFINED$$FLIP}", m));

        String err = ExecutionContextUtil.getValueECF(callingContext, workOrderUri, "%!document://matrix/${JOBNAME$default}#${STEPNAME$Undefined}_ERROR$$Error", m);
    }

}
