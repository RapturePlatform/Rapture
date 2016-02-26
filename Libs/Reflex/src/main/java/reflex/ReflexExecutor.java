/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Incapture Technologies LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package reflex;

import java.util.HashMap;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import rapture.common.client.ScriptClient;
import reflex.debug.IReflexDebugger;
import reflex.debug.NullDebugger;
import reflex.node.ReflexNode;
import reflex.util.function.LanguageRegistry;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class ReflexExecutor {

    public static Object evalReflexProgram(String program, LanguageRegistry languageRegistry, IReflexHandler handler, Scope currentScope,
            IReflexDebugger iReflexDebugger) {
        ReflexLexer lexer = new ReflexLexer();
        iReflexDebugger.setProgram(program);
        if (handler != null && handler.getScriptHandler() != null) {
            lexer.dataHandler = handler.getScriptHandler();
        }
        lexer.setCharStream(new ANTLRStringStream(program));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ReflexParser parser = new ReflexParser(tokens);
        CommonTree tree;
        try {
            tree = (CommonTree) parser.parse().getTree();
            CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);

            ReflexTreeWalker walker = new ReflexTreeWalker(nodes, languageRegistry);
            if (handler != null) {
                walker.setReflexHandler(handler);
            }
            walker.currentScope = currentScope;
            ReflexNode returned = walker.walk();
            ReflexValue val = returned.evaluateWithoutScope(iReflexDebugger);
            return val.asObject();
        } catch (ReflexParseException e) {
            throw e;
        } catch (RecognitionException e) {
            throw new ReflexException(-1, getParserExceptionDetails(e), e); //$NON-NLS-1$
        } catch (ReflexException e) {
            throw e;
        }

    }

    public static void parseReflexProgram(String program) {
        parseReflexProgram(program, null);
    }

    public static void parseReflexProgram(String program, IReflexScriptHandler scriptHandler) {
        ReflexLexer lexer = new ReflexLexer();
        if (scriptHandler != null) {
            lexer.dataHandler = scriptHandler;
        }
        lexer.setCharStream(new ANTLRStringStream(program));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ReflexParser parser = new ReflexParser(tokens);
        try {
            parser.parse().getTree();
        } catch (ReflexParseException e) {
            throw e;
        } catch (RecognitionException e) {
            throw new ReflexException(-1, getParserExceptionDetails(e), e); //$NON-NLS-1$
        } catch (ReflexException e) {
            throw e;
        }
    }

    public static IReflexHandler getStandardClientHandler(ScriptClient api) {
        return new StandardReflexHandler(api);
    }

    public static Object runReflexProgram(String program) {
        return runReflexProgram(program, null, null);
    }

    public static Object runReflexProgram(String program, IReflexHandler handler, Map<String, Object> injectedVars) {
        return runReflexProgram(program, handler, injectedVars, new NullDebugger());
    }

    public static ReflexTreeWalker getWalkerForProgramWithRegistry(LanguageRegistry savedRegistry, String program, IReflexHandler handler) {
        ReflexLexer lexer = new ReflexLexer();
        if (handler != null && handler.getScriptHandler() != null) {
            lexer.dataHandler = handler.getScriptHandler();
        }
        lexer.setCharStream(new ANTLRStringStream(program));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ReflexParser parser = new ReflexParser(tokens);
        CommonTree tree;
        try {
            tree = (CommonTree) parser.parse().getTree();
            CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
            parser.languageRegistry.merge(savedRegistry);
            ReflexTreeWalker walker = new ReflexTreeWalker(nodes, parser.languageRegistry);
            return walker;
        } catch (ReflexParseException e) {
            throw e;
        } catch (RecognitionException e) {
            throw new ReflexException(-1, getParserExceptionDetails(e), e); //$NON-NLS-1$
        } catch (ReflexException e) {
            throw e;
        }
    }

    public static ReflexTreeWalker getWalkerForProgram(String program, IReflexHandler handler) {
        ReflexLexer lexer = new ReflexLexer();
        if (handler != null && handler.getScriptHandler() != null) {
            lexer.dataHandler = handler.getScriptHandler();
        }
        lexer.setCharStream(new ANTLRStringStream(program));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ReflexParser parser = new ReflexParser(tokens);
        CommonTree tree;
        try {
            tree = (CommonTree) parser.parse().getTree();
            CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
            ReflexTreeWalker walker = new ReflexTreeWalker(nodes, parser.languageRegistry);
            return walker;
        } catch (ReflexParseException e) {
            throw e;
        } catch (RecognitionException e) {
            throw new ReflexException(e.line, getParserExceptionDetails(e), e); //$NON-NLS-1$
        } catch (ReflexException e) {
            throw e;
        }
    }

    public static Object runReflexWalker(IReflexHandler handler, IReflexDebugger iReflexDebugger, ReflexTreeWalker walker, Map<String, Object> injectedVars) {
        try {
            if (handler != null) {
                walker.setReflexHandler(handler);
            }
            if (injectedVars != null && !injectedVars.isEmpty()) {
                for (Map.Entry<String, Object> kv : injectedVars.entrySet()) {
                    Scope s = walker.currentScope;
                    s.getGlobalScope().assign(kv.getKey(), kv.getValue() == null ? new ReflexNullValue() : new ReflexValue(kv.getValue()));
                }
            }
            ReflexNode returned = walker.walk();
            ReflexValue val = returned.evaluateWithoutScope(iReflexDebugger);
            return val.asObject();
        } catch (ReflexParseException e) {
            throw e;
        } catch (RecognitionException e) {
            throw new ReflexException(e.line, getParserExceptionDetails(e), e); //$NON-NLS-1$
        } catch (ReflexException e) {
            throw e;
        }
    }
    
    // Not sure if this belongs here or not
    public static String getParserExceptionDetails(RecognitionException e) {
        StringBuilder sb = new StringBuilder();
        CommonToken token = (CommonToken) e.token;
        
        String[] lines = token.getInputStream().substring(0, token.getInputStream().size() - 1).split("\n");
        
        sb.append("Error at token ").append(token.getText()).append(" on line ").append(e.line).append(" while parsing: \n");
        
        int start = Math.max(0, token.getLine() -5);
        int end = Math.min(lines.length, token.getLine() +5);
        int badline = token.getLine() -1;
        
        for (int i = start; i < end; i++) {
            sb.append(String.format("%5d: %s\n", i+1, lines[i]));
            if (i == badline) {
                for (int j = 0; j < e.charPositionInLine+7; j++) sb.append(" ");
                for (int j = 0; j < token.getText().length(); j++) sb.append("^");
                sb.append("\n");
            }
        }
        
        return sb.toString();
    }

    public static Object runReflexProgram(String program, IReflexHandler handler, Map<String, Object> injectedVars, IReflexDebugger iReflexDebugger) {
        ReflexLexer lexer = new ReflexLexer();
        iReflexDebugger.setProgram(program);
        if (handler != null && handler.getScriptHandler() != null) {
            lexer.dataHandler = handler.getScriptHandler();
        }
        lexer.setCharStream(new ANTLRStringStream(program));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ReflexParser parser = new ReflexParser(tokens);
        CommonTree tree;
        try {
            tree = (CommonTree) parser.parse().getTree();
            CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
            iReflexDebugger.setRegistry(parser.languageRegistry);
            ReflexTreeWalker walker = new ReflexTreeWalker(nodes, parser.languageRegistry);
            if (handler != null) {
                walker.setReflexHandler(handler);
            }
            if (injectedVars != null && !injectedVars.isEmpty()) {
                for (Map.Entry<String, Object> kv : injectedVars.entrySet()) {
                    walker.currentScope.assign(kv.getKey(), kv.getValue() == null ? new ReflexNullValue() : new ReflexValue(kv.getValue()));
                }
            }
            injectSystemIntoScope(walker.currentScope);
            ReflexNode returned = walker.walk();
            ReflexValue val = returned.evaluateWithoutScope(iReflexDebugger);
            if (val.getValue() == ReflexValue.Internal.SUSPEND) {
                boolean finished = false;
                while (!finished) {
                    try {
                        Thread.sleep(walker.getReflexHandler().getSuspendHandler().getSuspendTime() * 1000);
                        ReflexValue newRet = returned.evaluateWithResume(iReflexDebugger, returned.getScope());
                        if (newRet.getValue() != ReflexValue.Internal.SUSPEND) {
                            val = newRet;
                            finished = true;
                            break;
                        }
                    } catch (Exception e) {

                    }
                }
            }
            return val.asObject();
        } catch (ReflexParseException e) {
            throw e;
        } catch (RecognitionException e) {
            throw new ReflexException(-1, getParserExceptionDetails(e), e); //$NON-NLS-1$
        } catch (ReflexException e) {
            throw e;
        }
    }
    
    public static void injectSystemIntoScope(Scope currentScope) {
        // Inject a ENV and PROPS variable into the scope, showing the
        // environment and System properties
        Map<String, Object> envVar = new HashMap<String, Object>();
        for (Map.Entry<String, String> envEntry : System.getenv().entrySet()) {
            envVar.put(envEntry.getKey(), envEntry.getValue());
        }
        currentScope.assign("ENV", new ReflexValue(envVar)); //$NON-NLS-1$

        Map<String, Object> propVar = new HashMap<String, Object>();
        for (Map.Entry<Object, Object> propEntry : System.getProperties().entrySet()) {
            propVar.put(propEntry.getKey().toString(), propEntry.getValue());
        }
        currentScope.assign("PROPS", new ReflexValue(propVar)); //$NON-NLS-1$
    }

}
