package reflex.errors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import reflex.AbstractReflexScriptTest;

public class RoundingError extends AbstractReflexScriptTest {
	private static Logger log = Logger.getLogger(RoundingError.class);

	@Test
	public void additionRoundingTest() throws RecognitionException {
		String program = "i = 0.1; \n" +
		" while (i < 10) do\n" +
		"  i = i + 0.1; \n" +
		"  println(i+\" \\n \"); \n" +
		"end\n";
		
		String output = runScript(program, null);
		checkValues(output);
	}

	@Test
	public void singleArgAdditionRoundingTest() throws RecognitionException {
		String program = "i = 0.1; \n" +
		" while (i < 10) do\n" +
		"  i += 0.1; \n" +
		"  println(i+\" \\n \"); \n" +
		"end\n";
		
		String output = runScript(program, null);
		checkValues(output);
	}

	// Reflex currently supports += but not -=
	@Ignore
	@Test
	public void singleArgSubtractionRoundingTest() throws RecognitionException {
		String program = "i = 10.0; \n" +
		" while (i > 0) do\n" +
		"  i -= 0.1; \n" +
		"  println(i+\" \\n \"); \n" +
		"end\n";
		
		String output = runScript(program, null);
		checkValues(output);
	}
	
	@Test
	public void singleArgSubtractionRoundingTest2() throws RecognitionException {
		String program = "i = 10.0; \n" +
		"j = 0.1; \n" +
		"while (i > 0) do\n" +
		"  i += -j; \n" +
		"  println(i+\" \\n \"); \n" +
		"end\n";
		
		String output = runScript(program, null);
		checkValues(output);
	}

	@Test
	public void subtractionRoundingTest() throws RecognitionException {
		String program = "i = 10.0; \n" +
		" while (i > 0) do\n" +
		"  i = i - 0.1; \n" +
		"  println(i+\" \\n \"); \n" +
		"end\n";
		
		String output = runScript(program, null);
		checkValues(output);
	}

	@Test
	public void multiplicationRoundingTest() throws RecognitionException {
		String program = "a = 1.64456; \n" +
				 "b = 0.2342; \n" +
				 "i = a * b; \n" +
				 "println(i+\" \\n \"); \n";
		
		String output = runScript(program, null);
		checkValues(output);
	}
	
	@Test
	public void divisionRoundingTest() throws RecognitionException {
		String program = "a = 787.5; \n" +
				 "b = 10.0; \n" +
				 "c = 100.0; \n" +
				 "d = a * b / c; \n" +
				 "e = a - d; \n" +
				 "f = 1586.6 - e; \n" +
				 "println(f+\" \\n \");";
		String output = runScript(program, null);
		checkValues(output);
	}
	
	public void checkValues(String output) {
		System.out.println(output);
		assertNotNull(output);
		assertFalse(output, output.contains("NULL"));
		assertFalse(output, output.contains("9999"));
		assertFalse(output, output.contains("0000"));
	}
}