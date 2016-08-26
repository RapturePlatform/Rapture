package rapture.field;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

public class FieldEngineTest {
    private ResourceLoader loader;
    private FieldEngine engine;
    
    @Before
    public void setup() {
        loader = new ResourceLoader();
        engine = new FieldEngine(loader, loader, loader, loader);
    }
    @Test
    public void testGeneral() {
       List<String> res = engine.validateDocument(loader.getData("/test/test1"), "/test/structure1");
       assertTrue(res.isEmpty());
    }
    
    @Test
    public void testNoConform1() {
       List<String> res = engine.validateDocument(loader.getData("/test/testNoField"), "/test/structure1");
       assertTrue(res.size() == 1);
    }
    
    @Test
    public void testWrongType() {
       List<String> res = engine.validateDocument(loader.getData("/test/testWrongType"), "/test/structure1");
       assertTrue(res.size() == 1);
    }
    
    @Test
    public void testComplex() {
       List<String> res = engine.validateDocument(loader.getData("/test/complex"), "/test/outer");
       assertTrue(res.size() == 0);
    }
    
    @Test
    public void testComplexArray() {
       List<String> res = engine.validateDocument(loader.getData("/test/complexArray"), "/test/outerArray");
       assertTrue(res.size() == 0);
    }
    
    @Test
    public void testValidating() {
       List<String> res = engine.validateDocument(loader.getData("/test/validating"), "/test/validating");
       assertTrue(res.size() == 0);
    }
}