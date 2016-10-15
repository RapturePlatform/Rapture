package rapture.dp.invocable.ftp.steps;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import rapture.common.CallingContext;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.dp.ExecutionContextUtil;

public class CheckFileExistsStepTest {

    @SuppressWarnings({ "unused", "static-access" })
    @Test
    public void checkMessageTest() {
        CallingContext ctx = ContextFactory.getAnonymousUser();
        String workerUri = "workorder://x/y#0";
        String workOrderUri = workerUri.substring(0, workerUri.length() - 2);
        CheckFileExistsStep cfes = new CheckFileExistsStep(workerUri, "foo");
        Kernel.getDecision().setContextLiteral(ctx, workerUri, "TODAY", "19780606");
        String today = Kernel.getDecision().getContextValue(ctx, workerUri, "TODAY");
        String val = ExecutionContextUtil.getValueECF(ctx, workerUri, "TODAY", null);
        assertEquals(today, val);
        String filename = "/tmp/${TODAY}";
        String expand = ExecutionContextUtil.evalTemplateECF(ctx, workerUri, filename, null);
        Kernel.getDecision().setContextLiteral(ctx, workerUri, "EXIST_FILENAMES", JacksonUtil.jsonFromObject(ImmutableMap.of(filename, true)));
        assertEquals(cfes.getFailTransition(), cfes.invoke(ctx));
        assertEquals("/tmp/19780606 was not found but was expected ", Kernel.getDecision().getContextValue(ctx, workerUri, "fooError"));
    }
}
