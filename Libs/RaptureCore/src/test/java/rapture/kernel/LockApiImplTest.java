package rapture.kernel;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.LockHandle;
import rapture.common.RaptureConstants;
import rapture.common.RaptureLockConfig;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.LockApi;

public class LockApiImplTest extends AbstractFileTest {

    private static CallingContext callingContext;
    private static LockApi lockApi = Kernel.getLock();

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
        String managerUri = "lock://manager";
        String lockName = "Jeff";
        RaptureLockConfig lm = lockApi.createLockManager(callingContext, managerUri, "LOCKING USING MEMORY {}", "");
        Assert.assertNotNull(lm);
        LockHandle al = lockApi.acquireLock(callingContext, managerUri, lockName, 60, 60);
        Assert.assertNotNull(al);

        Boolean lme = lockApi.lockManagerExists(callingContext, managerUri);
        Assert.assertTrue(lme);

        Boolean rl = lockApi.releaseLock(callingContext, managerUri, lockName, al);
        Assert.assertTrue(rl);

        List<RaptureLockConfig> lmc = lockApi.getLockManagerConfigs(callingContext, managerUri);
        Assert.assertNotNull(lmc);
        Assert.assertEquals(1, lmc.size());
    }

    @Test
    public void testLockHappyPath() {
        String managerUri = "lock://manager";
        String lockName = "Jeff";
        RaptureLockConfig lm = lockApi.createLockManager(callingContext, managerUri, "LOCKING USING MEMORY {}", "");
        Assert.assertNotNull(lm);
        LockHandle al = lockApi.acquireLock(callingContext, managerUri, lockName, 60, 60);
        Assert.assertNotNull(al);

        CallingContext c2 = new CallingContext();
        c2.setUser("dummy");

        LockHandle al2 = lockApi.acquireLock(c2, managerUri, lockName, 1, 1);
        Assert.assertNull("Lock request should fail", al2);

        Boolean rl = lockApi.releaseLock(callingContext, managerUri, lockName, al);
        Assert.assertTrue(rl);

        al2 = lockApi.acquireLock(c2, managerUri, lockName, 60, 60);
        Assert.assertNotNull("Lock request should succeed", al2);

        al = lockApi.acquireLock(callingContext, managerUri, lockName, 1, 1);
        Assert.assertNull("Lock request should fail", al);

        lockApi.forceReleaseLock(callingContext, managerUri, lockName);

        al = lockApi.acquireLock(callingContext, managerUri, lockName, 1, 1);
        Assert.assertNotNull("Lock request should succeed", al);

    }

}
