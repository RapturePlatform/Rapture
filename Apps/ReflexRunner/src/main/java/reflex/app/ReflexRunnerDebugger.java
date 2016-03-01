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
package reflex.app;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import jline.console.ConsoleReader;
import reflex.IReflexHandler;
import reflex.ReflexExecutor;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.debug.NullDebugger;
import reflex.node.ReflexNode;
import reflex.util.function.LanguageRegistry;
import reflex.value.ReflexValue;

public class ReflexRunnerDebugger implements IReflexDebugger {
    private ConsoleReader reader;
    private PrintWriter writer;
    private int level = 0;
    private int skipLevel = 2;
    private String lastCommand = "";
    private IReflexHandler handler;

    private String[] lines;

    private LanguageRegistry languageRegistry;

    public ReflexRunnerDebugger(IReflexHandler handler, ConsoleReader reader, PrintWriter writer) {
        this.reader = reader;
        this.writer = writer;
        this.handler = handler;
        writer.println("Reflex Debugger");
        writer.println();
        writer.println("Type h <CR> for help");
        writer.flush();
        reader.setPrompt("reflex> ");
    }

    private void doExecute(String command, Scope currentScope) {
        // Need to execute the command after the "x " in the current Reflex
        // context (i.e. in the current scope, with the current functions)
        String program = command.substring(2);
        Object ret = ReflexExecutor.evalReflexProgram(program, languageRegistry, handler, currentScope, new NullDebugger());
        writer.println("Ret = " + ret.toString());
        writer.flush();
    }

    @Override
    public boolean isDebug() {
        return true;
    }

    private void printScope(Scope scope) {
        for (Map.Entry<String, ReflexValue> entry : scope.retrieveVariableSet()) {
            writer.println(entry.getKey() + "=" + entry.getValue().toString());
        }
        if (scope.getParent() != null) {
            printScope(scope.getParent());
        }
        writer.flush();
    }

    @Override
    public void setRegistry(LanguageRegistry newValue) {
        this.languageRegistry = newValue;
    }

    @Override
    public void setProgram(String program) {
        lines = program.split("\n");
    }

    @Override
    public void stepEnd(ReflexNode node, ReflexValue value, Scope scope) {
        level--;
    }

    @Override
    public void stepStart(ReflexNode node, Scope scope) {
        level++;
        if (level > skipLevel) {
            return;
        }
        boolean cont = false;
        while (!cont) {
            writer.println("(" + level + "," + node.getLineNumber() + ") " + node.toString());
            writer.flush();
            try {
                String command = reader.readLine();
                if (command.isEmpty()) {
                    command = lastCommand;
                }
                if (command.equals("h")) {
                    writer.println("s - step into, n - step over, r - return, p - print vars, q - quit, l - list line, b - breakpoint, x - eval, [ret] do last step");
                    writer.flush();
                } else if (command.equals("s")) {
                    skipLevel++;
                    cont = true;
                } else if (command.equals("n")) {
                    skipLevel = level;
                    cont = true;
                } else if (command.equals("r")) {
                    skipLevel--;
                    cont = true;
                } else if (command.equals("p")) {
                    printScope(scope);
                } else if (command.equals("l")) {
                    if (node.getLineNumber() > 0 && node.getLineNumber() <= lines.length) {
                        writer.println(lines[node.getLineNumber() - 1]);
                    }
                } else if (command.equals("q")) {
                    System.exit(-1);
                } else if (command.startsWith("x")) {
                    doExecute(command, scope);
                }
                lastCommand = command;
            } catch (IOException e) {
            }
        }
    }

    @Override
    public void recordMessage(String arg0) {
        System.out.println(arg0);
    }

}
