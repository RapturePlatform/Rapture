package reflex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

public class SwitchTest extends AbstractReflexScriptTest {
	private static Logger log = Logger.getLogger(SwitchTest.class);

	@Test
	public void happyPathMatch() throws RecognitionException {
		String program = "ident = 'foo';\n" + "switch ident do\n" + "case 'foo' do\n" + "println('Success');\n"
				+ "end\n" + "case 'bar' do\n" + "println('Fail');\n" + "end\n" + "default do\n" + "println('Fail');\n"
				+ "end\n" + "end";

		String output = runScript(program, null);
		assertEquals("Success", output.trim());
	}

	@Test
	public void happyPathNoMatch() throws RecognitionException {
		String program = "ident = 'foo';\n" + "switch ident do\n" + "case 'bar' do\n" + "println('Fail');\n" + "end\n"
				+ "default do\n" + "println('Success');\n" + "end\n" + "end";

		String output = runScript(program, null);
		assertEquals("Success", output.trim());
	}

	@Test
	public void happyPathNoDefault() throws RecognitionException {
		String program = "ident = 'foo';\n" + "switch ident do\n" + "case 'bar' do\n" + "println('Fail');\n" + "end\n" + "end";
		String output = runScript(program, null);
		assertEquals("", output);
	}

	@Test
	public void duplicateDefault() throws RecognitionException {
		String program = "ident = 'foo';\n" + "switch ident do\n" + "case 'foo' do\n" + "println('Fail');\n" + "end\n"
				+ "default do\n" + "println('Fail');\n" + "end\n" + "case 'bar' do\n" + "println('Fail');\n" + "end\n"
				+ "default do\n" + "println('Fail');\n" + "end\n" + "end";

		try {
			runScript(program, null);
			fail("Exception expected");
		} catch (ReflexException e) {
			assertEquals("+++ SWITCH MAY ONLY HAVE ONE DEFAULT", e.getMessage());
		}
	}

	@Test
	public void duplicateCase() throws RecognitionException {
		String program = "ident = 'foo';\n" + "switch ident do\n" + "case 'foo' do\n" + "println('Fail');\n" + "end\n"
				+ "default do\n" + "println('Fail');\n" + "end\n" + "case 'bar' do\n" + "println('Fail');\n" + "end\n"
				+ "case 'foo' do\n" + "println('Fail');\n" + "end\n" + "end";

		try {
			runScript(program, null);
			fail("Exception expected");
		} catch (ReflexException e) {
			assertEquals("+++ DUPLICATE CASE foo", e.getMessage());
		}
	}

	/**
	 * Can't get this error to flag correctly 
	 * @throws RecognitionException
	 */
	@Ignore
	@Test
	public void expressionNotConstant() throws RecognitionException {
		String program = "ident = 'foo';\n" + "switch ident do\n" + "case 'f'+'oo' do\n" + "println('Fail');\n"
				+ "end\n" + "default do\n" + "println('Fail');\n" + "end\n" + "case 'bar' do\n" + "println('Fail');\n"
				+ "end\n" + "case 'foo' do\n" + "println('Fail');\n" + "end\n" + "end";

		String output = runScriptCatchingExceptions(program, null);
		assertTrue(output, output.contains("at line 3"));
	}
	
	@Test
	public void quotedString() throws RecognitionException {
		String program = "ident = 'foo';\n" + "switch ident do\n" + "case \"foo\" do\n" + "println('Fail');\n"
				+ "end\n" + "default do\n" + "println('Fail');\n" + "end\n" + "end";

		String output = runScriptCatchingExceptions(program, null);
		assertTrue(output, output.contains("Quoted String found where constant expected"));
	}
	
	@Test
	public void extraSemicolonError() {
		String program = "ident = 'foo';\n" + "switch ident do\n" + "case 'foo' do\n" + "println('Success');\n"
				+ "end\n" + "case 'bar' do\n" + "println('Fail');\n" + "end"
				// illegal semicolon
				+ ";\n" 
				+ "default do\n" + "println('Fail');\n" + "end\n" + "end";

		String output = runScriptCatchingExceptions(program, null);
		assertTrue(output, output.contains("Unexpected semicolon"));
	}
	
	@Test
	public void integerSwitch() throws RecognitionException {
		String program = "ident=1L; \n"+
			"switch ident do\n"+
			"  case 1I do\n"+
			"    println(\"Integer\");\n"+
			"  end\n"+
			"  case 1.0 do\n"+
			"    println(\"Number\");\n"+
			"  end\n"+
			"end\n";
		String output = runScript(program, null);
		assertEquals("Integer", output.trim());

	}
	
	@Test
	public void multipleMatches() throws RecognitionException {
		String program = "for ident in [0, 1, 2, 3, 4, 4.5, 5, 6, 7, 8, 9, 'fish', \"banana\", true, false] do \n" +
		" switch ident do\n" +
		"  default do \n" +
		"    println(ident+\" is neither odd nor even\");\n" +
		"  end\n" +
		"  case 1\n" +
		"  case 3\n" +
		"  case 5\n" +
		"  case 7\n" +
		"  case 9 do\n" +
		"      println(ident+\" is odd\");\n" +
		"    end\n" +
		"  case 2\n" +
		"  case 4\n" +
		"  case 6\n" +
		"  case 8 do\n" +
		"    println(ident+\" is even\");\n" +
		"  end\n" +
		" end\n" +
		"end\n";
		
		String output = runScript(program, null);
		assertEquals(6, output.split("is odd").length);
		assertEquals(5, output.split("is even").length);
		assertEquals(7, output.split("is neither").length);
	}
}