package reflex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.apache.log4j.Logger;
import org.junit.Test;

import rapture.common.api.ScriptingApi;
import reflex.node.ReflexNode;
import reflex.util.InstrumentDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class SwitchTest {
	private static Logger log = Logger.getLogger(SwitchTest.class);

	@Test
	public void happyPathMatch() throws RecognitionException {
		String program = "ident = 'foo';\n" + "switch ident do\n" + "case 'foo' do\n" + "println('Success');\n"
				+ "end\n" + "case 'bar' do\n" + "println('Fail');\n" + "end\n" + "default do\n" + "println('Fail');\n"
				+ "end\n" + "end";

		String output = runScript(program, null);
		assertEquals("Success", output);
	}

	@Test
	public void happyPathNoMatch() throws RecognitionException {
		String program = "ident = 'foo';\n" + "switch ident do\n" + "case 'bar' do\n" + "println('Fail');\n" + "end\n"
				+ "default do\n" + "println('Success');\n" + "end\n" + "end";

		String output = runScript(program, null);
		assertEquals("Success", output);
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

	@Test
	public void expressionNotConstant() throws RecognitionException {
		String program = "ident = 'foo';\n" + "switch ident do\n" + "case 'f'+'oo' do\n" + "println('Fail');\n"
				+ "end\n" + "default do\n" + "println('Fail');\n" + "end\n" + "case 'bar' do\n" + "println('Fail');\n"
				+ "end\n" + "case 'foo' do\n" + "println('Fail');\n" + "end\n" + "end";

		String output = runScriptCatchingExceptions(program, null);
		assertTrue(output, output.contains("Expression found where constant expected"));
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
		String program = "ident=1; \n"+
			"switch ident do\n"+
			"  case '1' do\n"+
			"    println(\"String\");\n"+
			"  end\n"+
			"  case 1I do\n"+
			"    println(\"Integer\");\n"+
			"  end\n"+
			"  case 1L do\n"+
			"    println(\"Long\");\n"+
			"  end\n"+
			"  case 1.0 do\n"+
			"    println(\"Number\");\n"+
			"  end\n"+
			"end\n";
		String output = runScript(program, null);
		assertEquals("Integer", output);

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
		return sb.toString();
	}
}