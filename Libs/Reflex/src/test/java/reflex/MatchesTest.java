package reflex;

import static org.junit.Assert.assertEquals;

import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;
import org.junit.Test;

public class MatchesTest extends AbstractReflexScriptTest {
	private static Logger log = Logger.getLogger(MatchesTest.class);

    // @Test
    // public void happyPathMatch() throws RecognitionException {
    // Map<String, Object> map = new HashMap<>();
    // map.put("a", "Monday");
    // map.put("b", ".*day");
    // String output = runTestForWithApi("/matches.rfx", map);
    // assertEquals("true", output.trim());
    // }

    @Test
    public void happyPathMatch() throws RecognitionException {

        // Pattern p = Pattern.compile("(.*)day");
        // Matcher m = p.matcher("Monday");
        // while (m.find()) {
        // int i = m.groupCount();
        // if (i == 0) System.out.println(m.group());
        // else {
        // for (int group = 0; group <= m.groupCount(); group++) {
        // System.out.println(m.group(group));
        // }
        // }
        // }
        //
        // m = p.matcher("wibble");
        // while (m.find()) {
        // for (int group = 0; group < m.groupCount(); group++) {
        // System.out.println(m.group(group));
        // }
        // }

        assertEquals("[Monday]", runScript("foo = matches(\"Monday\", \".*day\");\nprintln(foo);\nreturn foo;\n", null).trim());
        assertEquals("[Mon]", runScript("foo = matches(\"Monday\", \"(.*)day\");\nprintln(foo);\nreturn foo;\n", null).trim());
        assertEquals("[Mon, day]", runScript("foo = matches(\"Monday\", \"(.*)(day)\");\nprintln(foo);\nreturn foo;\n", null).trim());
        assertEquals("[day]", runScript("foo = matches(\"Monday\", \".*(day)\");\nprintln(foo);\nreturn foo;\n", null).trim());
        assertEquals("[]", runScript("foo = matches(\"March\", \".*day\");\nprintln(foo);\nreturn foo;\n", null).trim());
        assertEquals("[Mon, y]", runScript("foo = matches(\"Monday\", \"(.*)da(y)\");\nprintln(foo);\nreturn foo;\n", null).trim());
    }

}