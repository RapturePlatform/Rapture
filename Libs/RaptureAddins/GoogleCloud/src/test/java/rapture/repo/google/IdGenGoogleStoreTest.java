package rapture.repo.google;


import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.joda.time.Duration;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import com.google.common.collect.ImmutableMap;

import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.exception.RaptureException;
import rapture.dsl.idgen.RaptureIdGen;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class IdGenGoogleStoreTest {
    UUID uuid = UUID.randomUUID();

    final static LocalDatastoreHelper helper = LocalDatastoreHelper.create();

    @BeforeClass
    public static void setupLocalDatastore() throws IOException, InterruptedException {
        System.out.println("Here");

        helper.start(); // Starts the local Datastore emulator in a separate process
        GoogleDatastoreKeyStore.setDatastoreOptionsForTesting(helper.getOptions());
        GoogleIndexHandler.setDatastoreOptionsForTesting(helper.getOptions());
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {prefix=\"/tmp/" + UUID.randomUUID() + "\"}");
    }

    @AfterClass
    public static void cleanupLocalDatastore() throws IOException, InterruptedException, TimeoutException {
        try {
            helper.stop(new Duration(6000L));
        } catch (Exception e) {
            System.out.println("Exception shutting down LocalDatastoreHelper: " + e.getMessage());
        }
    }


    @Test
    public void testIdGen1() {
        Datastore localDatastore = helper.getOptions().getService();
        RaptureIdGen f = new RaptureIdGen();
        ImmutableMap<String, String> map = ImmutableMap.of("initial", "10", "base", "26", "length", "8", "prefix", "TST");
        f.setIdGenStore(new IdGenGoogleStore(localDatastore, "TST"));
        f.setProcessorConfig(map);
        String result = f.incrementIdGen(10L);
        assertEquals("TST0000000K", result);
    }

    @Test
    public void testIdGen2() {
        Datastore localDatastore = helper.getOptions().getService();
        RaptureIdGen f = new RaptureIdGen();
        ImmutableMap<String, String> map = ImmutableMap.of("initial", "706216874", "base", "36", "length", "6", "prefix", "OI-");
        f.setIdGenStore(new IdGenGoogleStore(localDatastore, "OI-"));
        f.setProcessorConfig(map);

        String result = f.incrementIdGen(1L);
        assertEquals("OI-BOGOFF", result);
        result = f.incrementIdGen(735953563L);
        assertEquals("OI-NUMPTY", result);
        result = f.incrementIdGen(655030524L);
        assertEquals("OI-YOMAMA", result);
        f.invalidate();
        try {
            result = f.incrementIdGen(2L);
            Assert.fail("ID Generator was invalidated");
        } catch (RaptureException e) {
            // assertEquals("IdGenerator has been deleted", e.getMessage());
        }
    }

}
