package rapture.idgen.google;

import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testng.Assert;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import com.google.common.collect.ImmutableMap;

import rapture.common.exception.RaptureException;
import rapture.dsl.idgen.RaptureIdGen;

public class IdGenGoogleStoreTest {
    UUID uuid = UUID.randomUUID();
    LocalDatastoreHelper helper = LocalDatastoreHelper.create();
    Datastore localDatastore = helper.getOptions().getService();

    @Before
    public void setup() throws IOException, InterruptedException {
        helper.start(); // Starts the local Datastore emulator in a separate process
    }

    @After
    public void tidyUp() throws IOException, InterruptedException, TimeoutException {
        helper.stop(new Duration(6000));
    }

    @Test
    public void testIdGen1() {
        RaptureIdGen f = new RaptureIdGen();
        ImmutableMap<String, String> map = ImmutableMap.of("initial", "10", "base", "26", "length", "8", "prefix", "TST");
        f.setIdGenStore(new IdGenGoogleStore(localDatastore, "TST"));
        f.setProcessorConfig(map);
        String result = f.incrementIdGen(10L);
        assertEquals("TST0000000K", result);
    }

    @Test
    public void testIdGen2() {
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
