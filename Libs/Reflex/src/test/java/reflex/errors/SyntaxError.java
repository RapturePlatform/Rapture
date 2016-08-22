package reflex.errors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;
import org.junit.Test;

import reflex.AbstractReflexScriptTest;

public class SyntaxError extends AbstractReflexScriptTest {
	private static Logger log = Logger.getLogger(SyntaxError.class);

	@Test
	public void SpuriousSemicolonAfterEnd() throws RecognitionException {
		String program = "i = 0.1; \n" +
		" while (i < 10) do\n" +
		"  i = i + 0.1; \n" +
		"  println(i+\" \\n \"); \n" +
		"end;\n";
		
		String output = this.runScriptCatchingExceptions(program, null);
		assertTrue(output, output.contains("Unexpected semicolon at line 5"));
		assertTrue(output, output.contains("end;\n----------^^"));
	}
	
	@Test
	public void ExtraSemicolonInBlock() throws RecognitionException {
		String program = "i = 0.1; \n" +
		" while (i < 10) do\n" +
		"  i = i + 0.1;; \n" +
		"  println(i+\" \\n \"); \n" +
		"end\n";
		
		String output = this.runScriptCatchingExceptions(program, null);
		assertTrue(output, output.contains("Unexpected character at token ;  at line 3"));
	}
	
	@Test
	public void incrementNotSupported1() throws RecognitionException {
		String program = "i=1; \n i++; \n println(i); \n";
		String output = this.runScriptCatchingExceptions(program, null);
        assertEquals("Unexpected identifier at token i  at line 2", output.split("\n")[0]);
	}
	
	@Test
	public void incrementNotSupported2() throws RecognitionException {
		String program = "i=1; \n ++i; \n println(i); \n";
		String output = this.runScriptCatchingExceptions(program, null);
        assertEquals("Unsupported Operation at token ++  at line 2", output.split("\n")[0]);
	}
	
	@Test
	public void incrementNotSupported3() throws RecognitionException {
		String program = "i=1; \n i--; \n println(i); \n";
		String output = this.runScriptCatchingExceptions(program, null);
        assertEquals("Unexpected identifier at token i  at line 2", output.split("\n")[0]);
	}
	
	@Test
	public void incrementNotSupported4() throws RecognitionException {
		String program = "i=1; \n --i; \n println(i); \n";
		String output = this.runScriptCatchingExceptions(program, null);
        assertEquals("Unsupported Operation at token --  at line 2", output.split("\n")[0]);
	}
}
