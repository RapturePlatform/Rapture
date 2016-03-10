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

public class MatchTest {
	private static Logger log = Logger.getLogger(MatchTest.class);

	@Test
	public void happyPathMatch() throws RecognitionException {
		String program = "ident = 5;\n" + "match ident do\n" + "is > 4 do\n" + "println('Success');\n"
				+ "end\n" + "is < 6 do\n" + "println('Fail');\n" + "end\n" + "otherwise do\n" + "println('Fail');\n"
				+ "end\n" + "end";

		String output = runScript(program, null);
		assertEquals("Success", output);
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
		String program = "for ident in [0, 1, 2, 3, 4, 4.5, 5, 6, 7, 8, 9, 'fish', \"banana\", true, false] do \n" +
		" match ident do\n" +
		"  is == 1\n" +
		"  is == 3\n" +
		"  is == 5\n" +
		"  is == 7\n" +
		"  is == 9 do\n" +
		"    println(ident+\" is odd\");\n" +
		"  end\n" +
		"  is == 2\n" +
		"  is == 4\n" +
		"  is == 6\n" +
		"  is == 8 do\n" +
		"    println(ident+\" is even\");\n" +
		"  end\n" +
		"  otherwise do \n" +
		"    println(ident+\" is neither odd nor even\");\n" +
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
		
	public String runScript(String program, Map<String, Object> injectedVars)
			throws RecognitionException, ReflexParseException {
		StringBuilder sb = new StringBuilder();

		ReflexLexer lexer = new ReflexLexer();
		lexer.setCharStream(new ANTLRStringStream(program));
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		ReflexParser parser = new ReflexParser(tokens);

		CommonTree tree = (CommonTree) parser.parse().getTree();

		CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
		ReflexTreeWalker walker = new ReflexTreeWalker(nodes, parser.languageRegistry);

		IReflexHandler handler = walker.getReflexHandler();
		handler.setOutputHandler(new IReflexOutputHandler() {

			@Override
			public boolean hasCapability() {
				return false;
			}

			@Override
			public void printLog(String text) {
				sb.append(text);
			}

			@Override
			public void printOutput(String text) {
				sb.append(text);
			}

			@Override
			public void setApi(ScriptingApi api) {
			}
		});

		if (injectedVars != null && !injectedVars.isEmpty()) {
			for (Map.Entry<String, Object> kv : injectedVars.entrySet()) {
				walker.currentScope.assign(kv.getKey(),
						kv.getValue() == null ? new ReflexNullValue() : new ReflexValue(kv.getValue()));
			}
		}

		@SuppressWarnings("unused")
		ReflexNode returned = walker.walk();
		InstrumentDebugger instrument = new InstrumentDebugger();
		instrument.setProgram(program);
		ReflexValue retVal = (returned == null) ? null : returned.evaluateWithoutScope(instrument);
		instrument.getInstrumenter().log();
		return sb.toString();
	}

	public String runScriptCatchingExceptions(String program, Map<String, Object> injectedVars) {
		StringBuilder sb = new StringBuilder();
		StringBuilder lexerError = new StringBuilder();
		StringBuilder parserError = new StringBuilder();
		StringBuilder logs = new StringBuilder();
		
		Logger.getLogger(MatchNode.class).addAppender(new AppenderSkeleton() {
			@Override
			public void close() {				
			}

			@Override
			public boolean requiresLayout() {
				return false;
			}

			@Override
			protected void append(LoggingEvent event) {
				logs.append(event.getMessage().toString());
			};
		});

		try {
			ReflexLexer lexer = new ReflexLexer() {
				@Override
				public void emitErrorMessage(String msg) {
					lexerError.append(msg);
				}
			};
			lexer.setCharStream(new ANTLRStringStream(program));
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			ReflexParser parser = new ReflexParser(tokens) {
				@Override
				public void emitErrorMessage(String msg) {
					parserError.append(msg);
				}
			};

			CommonTree tree = (CommonTree) parser.parse().getTree();

			CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
			ReflexTreeWalker walker = new ReflexTreeWalker(nodes, parser.languageRegistry);

			IReflexHandler handler = walker.getReflexHandler();
			handler.setOutputHandler(new IReflexOutputHandler() {

				@Override
				public boolean hasCapability() {
					return false;
				}

				@Override
				public void printLog(String text) {
					sb.append(text);
				}

				@Override
				public void printOutput(String text) {
					sb.append(text);
				}

				@Override
				public void setApi(ScriptingApi api) {
				}
			});

			if (injectedVars != null && !injectedVars.isEmpty()) {
				for (Map.Entry<String, Object> kv : injectedVars.entrySet()) {
					walker.currentScope.assign(kv.getKey(),
							kv.getValue() == null ? new ReflexNullValue() : new ReflexValue(kv.getValue()));
				}
			}

			@SuppressWarnings("unused")
			ReflexNode returned = walker.walk();
			InstrumentDebugger instrument = new InstrumentDebugger();
			instrument.setProgram(program);
			ReflexValue retVal = (returned == null) ? null : returned.evaluateWithoutScope(instrument);
			instrument.getInstrumenter().log();
		} catch (Exception e) {
			sb.append(e.getMessage()).append("\n");
		}
		sb.append("-----\n");
		sb.append(lexerError.toString()).append("\n");
		sb.append("-----\n");
		sb.append(parserError.toString()).append("\n");
		sb.append("-----\n");
		sb.append(logs.toString()).append("\n");
		return sb.toString();
	}
}