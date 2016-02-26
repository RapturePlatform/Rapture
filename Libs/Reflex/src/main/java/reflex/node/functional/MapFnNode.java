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

import com.google.common.collect.Table.Cell;

public class MapFnNode extends BaseNode {

    private ReflexNode expression;
    private String fnName;
    private LanguageRegistry functions;
    private ImportHandler importHandler;

    public MapFnNode(int lineNumber, IReflexHandler handler, Scope s, String fnName, ReflexNode e, LanguageRegistry fs, ImportHandler importHandler) {
        super(lineNumber, handler, s);
        this.expression = e;
        this.fnName = fnName;
        this.functions = fs;
        this.importHandler = importHandler;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue arg = expression.evaluate(debugger, scope);
        ReflexValue ret = new ReflexNullValue(lineNumber);
        // for each element in the list, call the fn referenced by fnName with
        // those parameters
        // take the return value and append to a new ret list
        if (arg.isList()) {
            ret = mapMap(debugger, scope, arg);
        } else if (arg.isSparseMatrix()) {
            ret = mapMatrix(debugger, scope, arg);
        }
        debugger.stepEnd(this, ret, scope);
        return ret;
    }

    private ReflexValue mapMatrix(IReflexDebugger debugger, Scope scope, ReflexValue arg) {
        Function f = functions.getFunction(FunctionFactory.createFunctionKey(fnName, 3)); // 1
        // parameter
        ReflexSparseMatrixValue smv = arg.asMatrix();
        ReflexSparseMatrixValue mappedSmv = new ReflexSparseMatrixValue(2);
        mappedSmv.copyOrder(smv);
        for(Cell<ReflexValue, ReflexValue, ReflexValue> cell : smv.getCells()) {
            List<ReflexNode> params = new ArrayList<ReflexNode>();
            params.add(new AtomNode(lineNumber, handler, scope, cell.getRowKey()));
            params.add(new AtomNode(lineNumber, handler, scope, cell.getColumnKey()));
            params.add(new AtomNode(lineNumber, handler, scope, cell.getValue()));
            ReflexValue ret = f.invoke(debugger, lineNumber, params, functions, handler, scope, importHandler);
            // Ret should be a list [ row, col, value ]
            if (ret.isList()) {
                List<ReflexValue> mappedValues = ret.asList();
                if (mappedValues.size() == 3) {
                    mappedSmv.set(mappedValues.get(0), mappedValues.get(1), mappedValues.get(2));
                }
            }
        }
        return new ReflexValue(mappedSmv);
    }
    
    private ReflexValue mapMap(IReflexDebugger debugger, Scope scope, ReflexValue arg) {
        Function f = functions.getFunction(FunctionFactory.createFunctionKey(fnName, 1)); // 1
        // parameter
        List<ReflexValue> inputVals = arg.asList();
        List<ReflexValue> retVals = new ArrayList<ReflexValue>(inputVals.size());
        for (ReflexValue v : inputVals) {
            List<ReflexNode> params = new ArrayList<ReflexNode>(1);
            params.add(new AtomNode(lineNumber, handler, scope, v));
            ReflexValue ret = f.invoke(debugger, lineNumber, params, functions, handler, scope, importHandler);
            retVals.add(ret);
        }

        ReflexValue ret = new ReflexValue(retVals);
        return ret;
    }

    @Override
    public String toString() {
        return super.toString() + " - " + String.format("map(%s,%s)", fnName, expression);
    }
}
