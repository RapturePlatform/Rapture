package rapture.kernel.dp;

import java.util.HashMap;

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
        CallingContext callingContext = ContextFactory.getKernelUser();
        String workOrderUri = RaptureURI.builder(Scheme.WORKORDER, "/test/").asString();
        Kernel.getDoc().putDoc(callingContext, "document://foo/bar",
                JacksonUtil.jsonFromObject(ImmutableMap.of("FOO", "!document://foo/bar#FOO", "BAR", "bar")));
        Kernel.getDecision().setContextLink(callingContext, workOrderUri, "FOO", "document://foo/bar#FOO");
        Kernel.getDecision().setContextLink(callingContext, workOrderUri, "BAR", "document://foo/bar#BAR");
        Assert.assertNull(ExecutionContextUtil.getValueECF(callingContext, workOrderUri, "FOO", new HashMap()));

        Kernel.getDecision().setContextTemplate(callingContext, workOrderUri, "Bar", "%%%%%%%%%%%%%%%%%%Foo");
        Kernel.getDecision().setContextTemplate(callingContext, workOrderUri, "Baz", "%%%%%%%%%%%%%%%%%%%%%%%Too many dereferences");
        Assert.assertEquals("bar", ExecutionContextUtil.getValueECF(callingContext, workOrderUri, "BAR", new HashMap()));
        Assert.assertEquals("Foo", ExecutionContextUtil.getValueECF(callingContext, workOrderUri, "Bar", new HashMap()));
        Assert.assertNull(ExecutionContextUtil.getValueECF(callingContext, workOrderUri, "Baz", new HashMap()));

    }

}
