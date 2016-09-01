package rapture.field;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

public class FileTransformLoaderTest {
    @Test
    public void testLoadDirectory() {
        ResourceLoader loader = new ResourceLoader();
        List<String> ret = loader.getFieldTransforms("/test");
        assertTrue(ret.size() == 1);
    }
}