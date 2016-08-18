package reflex;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.antlr.runtime.RecognitionException;
import org.junit.BeforeClass;
import org.junit.Test;

public class ParamsTest extends AbstractReflexScriptTest {

    static Map<String, Object> params = new HashMap<>();

    @BeforeClass
    public static void setup() {
        params.put("Foo", "foo");
        params.put("Bar", -1);
        params.put("Baz", Boolean.TRUE);
    }

    @Test
    public void testParams() throws RecognitionException {
        String program = "println(Foo);\n println(Bar);\n println(Baz);\n";
        String output = runScript(program, params);
        assertEquals("foo\n-1\ntrue", output.trim());
    }

    @Test
    public void testParams2() throws RecognitionException {
        String program = "def checkParam(p) \n println(\"Param is \" +p);\n end\n checkParam(Foo);\n checkParam(Bar);\n checkParam(Baz);\n checkParam(false);\n";
        String output = runScript(program, params);
        assertEquals("Param is foo\nParam is -1\nParam is true\nParam is false", output.trim());
    }

    @Test
    public void testParams3() throws RecognitionException {
        String program = "meta do \n" + "return boolean, 'Boolean'; \n" + "param 'Foo',string,'FOO'; \n" + "param 'Bar',number,'BAR'; \n"
                + "param 'Baz',boolean,'BAZ'; \n" + "end \n" + "println(\"Foo ${Foo} Bar ${Bar} Baz ${Baz}\"); \n" + "return \"${baz}\"; \n";
        String output = runScript(program, params);
        assertEquals("Foo foo Bar -1 Baz true", output.trim());
    }

    @Test
    public void testParams4() throws RecognitionException {
        MetaParam mp = new MetaParam("X", "Integer", "Z");
    }
}
