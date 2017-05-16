package rapture.repo.google;


import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.UUID;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.exception.RaptureException;
import rapture.dsl.idgen.RaptureIdGen;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class IdGenGoogleDatastoreTest extends LocalDataStoreTest {
    UUID uuid = UUID.randomUUID();


    @BeforeClass
    public static void setup() throws IOException, InterruptedException {
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {prefix=\"/tmp/" + UUID.randomUUID() + "\"}");
    }

    @Test
    public void testIdGen1() {
        RaptureIdGen f = new RaptureIdGen();
        ImmutableMap<String, String> map = ImmutableMap.of("initial", "10", "base", "26", "length", "8", "prefix", "TST");
        f.setIdGenStore(new IdGenGoogleDatastore(localDatastore, "TST"));
        f.setProcessorConfig(map);
        String result = f.incrementIdGen(10L);
        assertEquals("TST0000000K", result);
    }

    @Test
    public void testIdGen2() {
        RaptureIdGen f = new RaptureIdGen();
        ImmutableMap<String, String> map = ImmutableMap.of("initial", "706216874", "base", "36", "length", "6", "prefix", "OI-");
        f.setIdGenStore(new IdGenGoogleDatastore(localDatastore, "OI-"));
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
