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
package reflex.node.functional;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Table.Cell;

import reflex.Function;
import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.importer.ImportHandler;
import reflex.node.AtomNode;
import reflex.node.BaseNode;
import reflex.node.ReflexNode;
import reflex.util.function.FunctionFactory;
import reflex.util.function.LanguageRegistry;
import reflex.value.ReflexSparseMatrixValue;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class FoldNode extends BaseNode {

    private ReflexNode expression;
    private ReflexNode accumulator;
    private String fnName;
    private LanguageRegistry functions;
    private ImportHandler importHandler;

    public FoldNode(int lineNumber, IReflexHandler handler, Scope s, String fnName, ReflexNode accum, ReflexNode e, LanguageRegistry fs,
            ImportHandler importHandler) {
        super(lineNumber, handler, s);
        this.expression = e;
        this.fnName = fnName;
        this.accumulator = accum;
        this.functions = fs;
        this.importHandler = importHandler;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue arg = expression.evaluate(debugger, scope);
        // for each element in the list, call the fn referenced by fnName with
        // those parameters
        // and the accumulator (so two parameters)
        // the return value is the new accumulator, and the accumulator is
        // returned
        ReflexValue accum = new ReflexNullValue(lineNumber);
        if (arg.isList()) {
            accum = accumList(debugger, scope, arg);
        } else if (arg.isSparseMatrix()) {
            accum = accumMatrix(debugger, scope, arg);
        }

        debugger.stepEnd(this, accum, scope);
        return accum;
    }

    private ReflexValue accumMatrix(IReflexDebugger debugger, Scope scope, ReflexValue arg) {
        Function f = functions.getFunction(FunctionFactory.createFunctionKey(fnName, 4)); // 2
                                                                                  // parameters,
                                                                                  // accumulator
                                                                                  // and
        // array value
        ReflexValue accum = accumulator.evaluate(debugger, scope);
        ReflexSparseMatrixValue smv = arg.asMatrix();
        
        for (Cell<ReflexValue, ReflexValue, ReflexValue> cell : smv.getCells()) {
            List<ReflexNode> params = new ArrayList<ReflexNode>();
            params.add(new AtomNode(lineNumber, handler, scope, accum));
            params.add(new AtomNode(lineNumber, handler, scope, cell.getRowKey()));
            params.add(new AtomNode(lineNumber, handler, scope, cell.getColumnKey()));
            params.add(new AtomNode(lineNumber, handler, scope, cell.getValue()));
            accum = f.invoke(debugger, lineNumber, params, functions, handler, scope, importHandler);
        }
        return accum;
    }
    
    private ReflexValue accumList(IReflexDebugger debugger, Scope scope, ReflexValue arg) {
        Function f = functions.getFunction(FunctionFactory.createFunctionKey(fnName, 2)); // 2
                                                                                  // parameters,
                                                                                  // accumulator
                                                                                  // and
        // array value
        ReflexValue accum = accumulator.evaluate(debugger, scope);
        List<ReflexValue> inputVals = arg.asList();
        for (ReflexValue v : inputVals) {
            List<ReflexNode> params = new ArrayList<ReflexNode>(2);
            params.add(new AtomNode(lineNumber, handler, scope, accum));
            params.add(new AtomNode(lineNumber, handler, scope, v));
            accum = f.invoke(debugger, lineNumber, params, functions, handler, scope, importHandler);
        }
        return accum;
    }

    @Override
    public String toString() {
        return super.toString() + " - " + String.format("fold(%s,%s)", fnName, accumulator, expression);
    }
}
