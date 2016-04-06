package reflex.function;

import static org.junit.Assert.*;

import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;
import org.junit.Test;

import reflex.AbstractReflexScriptTest;

public class AssertTest extends AbstractReflexScriptTest {
private static Logger log = Logger.getLogger(AssertTest.class);

	@Test
	public void testAssert() throws RecognitionException {
		String program = "i = 20;\n" +
				"i += 2; \n" +
				"assert('This will fail', i == 1); \n";
	
		String out = runScriptCatchingExceptions(program, null);
		System.out.println(out);
		
		String output[] = out.split("\n");
		assertEquals("This will fail", output[0].trim());
		assertEquals("Line 3 :", output[1].trim());
		assertEquals("assert('This will fail', i == 1);", output[2].trim());
	}

}
