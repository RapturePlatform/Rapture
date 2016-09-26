package rapture.field;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.ArrayList;

public class ScriptContainerTest {
    @Test
    public void testLoadStructure() {
        ResourceLoader loader = new ResourceLoader();
        ScriptContainer container = new ScriptContainer();
        String script = loader.getScript("//test/simple");
        List<String> ret = new ArrayList<String>();
        container.runValidationScript("test value", script, ret);
        assertTrue(ret.isEmpty());
    }
}