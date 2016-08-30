package rapture.field;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import rapture.common.FieldType;
import rapture.common.RaptureField;

public class FieldLoaderTest {
    @Test
    public void testLoadField() {
        ResourceLoader loader = new ResourceLoader();
        RaptureField fd = loader.getField("/test/field1");
        assertTrue(fd.getAuthority().equals("//test/field1"));
        assertTrue(fd.getDescription().equals("A test field"));
        assertTrue(fd.getValidationScript().isEmpty());
        assertTrue(fd.getFieldType() == FieldType.STRING);
        assertTrue(fd.getFieldTypeExtra().isEmpty());
    }
}