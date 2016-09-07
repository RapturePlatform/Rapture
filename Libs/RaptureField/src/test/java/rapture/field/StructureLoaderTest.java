package rapture.field;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import rapture.common.RaptureStructure;

public class StructureLoaderTest {
    @Test
    public void testLoadStructure() {
        ResourceLoader loader = new ResourceLoader();
        RaptureStructure s = loader.getStructure("/test/structure1");
        assertTrue(s.getName().equals("//test/structure1"));
        assertTrue(s.getDescription().equals("A test structure"));
        assertTrue(s.getFields().size() == 1);
        assertTrue(s.getFields().get(0).getKey().equals("field1"));
    }
}
