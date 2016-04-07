package reflex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;

import rapture.common.api.ScriptingApi;
import reflex.node.MatchNode;
import reflex.node.ReflexNode;
import reflex.util.InstrumentDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class MatchTest extends AbstractReflexScriptTest {
	private static Logger log = Logger.getLogger(MatchTest.class);

	@Test
	public void happyPathMatch() throws RecognitionException {
		String program = "ident = 5;\n" + "match ident do\n" + "is > 4 do\n" + "println('Success');\n"
				+ "end\n" + "is < 6 do\n" + "println('Fail');\n" + "end\n" + "otherwise do\n" + "println('Fail');\n"
				+ "end\n" + "end";

		String output = runScript(program, null);
		assertEquals("Success", output.trim());
	}
	
	@Test
	public void testAsClause() throws RecognitionException {
		String program = "i = 5;\n" + "match 2*i as j do\n" + "is > 9 do\n" + "println(j);\n"
				+ "end\n" + "is < 10 do\n" + "println('Fail');\n" + "end\n" + "otherwise do\n" + "println('Fail');\n"
				+ "end\n" + "end";

		String output = runScript(program, null);
		assertEquals("10", output.trim());
	}
	
	@Test
	public void variousMatches() throws RecognitionException {
		String program = "for ident in [1, 2, 3, 4, 5, 6, 7, 8, 9] do \n"+
							" match ident do \n"+
							"  is > 6 do \n"+
							"    println(\"greater than 6\"); \n"+
							"  end \n"+
							"  is == 2+2 \n"+
							"  is == 3+1 \n"+
							"  is == 16/4 \n"+
							"    do \n"+
							"      println(\"We get it. It's 4\"); \n"+
							"    end \n"+
							"  is <= 4 do \n"+
							"    println(\"less than 5\"); \n"+
							"  end \n"+
							"  is == 4 do \n"+
							"    println(\"fail\"); \n"+
							"  end \n"+
							"  is == 5 do \n"+
							"   println(\"It's 5\"); \n"+
							"  end \n"+
							"  otherwise do \n"+
							"    println(\"must be 6\"); \n"+
							"  end \n"+
							" end \n"+
							"end \n" +
							"println('Success');\n";

		String output = runScript(program, null);
		assertTrue(output.contains("We get it. It's 4"));
		assertTrue(output.contains("less than 5"));
		assertTrue(output.contains("must be 6"));
		assertTrue(output.contains("greater than 6"));
		assertTrue(output.contains("less than 5"));
		assertTrue(!output.contains("fail"));
	}
	
	@Test
	public void multipleMatches() throws RecognitionException {
		String program = "for foo in [0, 1, 2, 3, 4, 4.5, 5, 6, 7, 8, 9, 'fish', \"banana\", true, false] do \n" +
		" match foo as bar do\n" +
		"  is == 1\n" +
		"  is == 3\n" +
		"  is == 5\n" +
		"  is == 7\n" +
		"  is == 9 do\n" +
		"    println(foo+\" is odd\");\n" +
		"  end\n" +
		"  is == 2\n" +
		"  is == 4\n" +
		"  is == 6\n" +
		"  is == 8 do\n" +
		"    println(foo+\" is even\");\n" +
		"  end\n" +
		"  otherwise do \n" +
		"    println(foo+\" is neither odd nor even\");\n" +
		"  end\n" +
		" end\n" +
		"end\n";
		
		String output = runScript(program, null);
		assertEquals(6, output.split("is odd").length);
		assertEquals(5, output.split("is even").length);
		assertEquals(7, output.split("is neither").length);
	}

	@Test
	public void assignIsNotLegal() throws RecognitionException {
		String program = "ident = 5;\n" + "match ident do\n" + "is = 5 do\n" + "println('Fail');\n"
				+ "end\n" + "end";

		String output = runScriptCatchingExceptions(program, null);
		assertTrue(output, output.contains("Assignment found where comparator expected"));
	}
	
	@Test
	public void noMatch() throws RecognitionException {
		String program = "ident = 5;\n" + "match ident do\n" + "is == 7 do\n" + "println('Fail');\n"
				+ "end\n" + "end";
		String output = runScriptCatchingExceptions(program, null);
		assertTrue(output, output.contains("Warning: Match had no matches and no default"));
	}
	
	@Test
	public void exclaimIsNotLegal() throws RecognitionException {
		String program = "ident = 5;\n" + "match ident do\n" + "is ! 5 do\n" + "println('Fail');\n"
				+ "end\n" + "end";

		String output = runScriptCatchingExceptions(program, null);
		assertTrue(output, output.contains("Comparator expected"));
	}
}