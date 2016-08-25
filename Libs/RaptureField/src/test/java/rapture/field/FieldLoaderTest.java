package rapture.field;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import rapture.field.model.FieldDefinition;
import rapture.field.model.FieldType;

public class FieldLoaderTest {
    @Test
    public void testLoadField() {
        ResourceLoader loader = new ResourceLoader();
        FieldDefinition fd = loader.getField("/test/field1");
        assertTrue(fd.getUri().equals("//test/field1"));
        assertTrue(fd.getDescription().equals("A test field"));
        assertTrue(fd.getValidationScript().isEmpty());
        assertTrue(fd.getFieldType() == FieldType.STRING);
        assertTrue(fd.getFieldTypeExtra().isEmpty());
    }
}