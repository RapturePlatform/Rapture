package rapture.idgen.google;

import static org.testng.AssertJUnit.assertEquals;

import java.util.UUID;

import org.junit.Test;
import org.testng.Assert;

import rapture.common.exception.RaptureException;
import rapture.dsl.idgen.IdGenFactory;
import rapture.dsl.idgen.RaptureIdGen;

public class IdGenGoogleStoreTest {
    UUID uuid = UUID.randomUUID();

    @Test
    public void testIdGen1() {
        RaptureIdGen f = IdGenFactory
                .getIdGen("IDGEN { initial = \"10\", base=\"26\", length=\"8\", prefix=\"TST\" } USING GCP_DATASTORE { prefix=\"" + uuid
                        + "\", projectid= \"high-plating-157918\"}");
        String result = f.incrementIdGen(10L);
        assertEquals("TST0000000K", result);
    }

    @Test
    public void testIdGen2() {
        RaptureIdGen f = IdGenFactory.getIdGen("IDGEN { initial=\"706216874\", base=\"36\", length=\"6\", prefix=\"OI-\" } " + "USING GCP_DATASTORE { prefix=\"" + uuid
                + "\", projectid= \"high-plating-157918\"}");

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
