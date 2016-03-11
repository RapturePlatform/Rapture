package reflex;

import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import rapture.common.api.ScriptingApi;
import reflex.node.MatchNode;
import reflex.node.ReflexNode;
import reflex.util.InstrumentDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public abstract class AbstractReflexScriptTest {
		
	public String runScript(String program, Map<String, Object> injectedVars)
			throws RecognitionException, ReflexParseException {
		final StringBuilder sb = new StringBuilder();

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
		final StringBuilder sb = new StringBuilder();
		final StringBuilder lexerError = new StringBuilder();
		final StringBuilder parserError = new StringBuilder();
		final StringBuilder logs = new StringBuilder();
		
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