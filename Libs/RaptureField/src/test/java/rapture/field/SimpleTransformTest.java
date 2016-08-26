package rapture.field;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

public class SimpleTransformTest {
    @Test
    public void testLoader() {
        ResourceLoader loader = new ResourceLoader();
        TransformEngine engine = new TransformEngine(loader, loader, loader, loader);
        String doc = loader.getData("//standard/prime");
        String resp = engine.transform(doc, "//standard/source", "//standard/target");
        System.out.println(resp);
    }
}