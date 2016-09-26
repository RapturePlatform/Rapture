package rapture.field;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

public class ResourceLoaderTest {
    @Test
    public void testLoader() throws java.io.IOException {
        ResourceLoader loader = new ResourceLoader();
        List<String> res = loader.getResources(null, "/structure/test");
        //System.out.println(res);
    }
}