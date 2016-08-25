package rapture.field;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import rapture.field.model.Structure;
import rapture.field.model.FieldType;

public class StructureLoaderTest {
    @Test
    public void testLoadStructure() {
        ResourceLoader loader = new ResourceLoader();
        Structure s = loader.getStructure("/test/structure1");
        assertTrue(s.getUri().equals("//test/structure1"));
        assertTrue(s.getDescription().equals("A test structure"));
        assertTrue(s.getFields().size() == 1);
        assertTrue(s.getFields().get(0).getKey().equals("field1"));
    }
}