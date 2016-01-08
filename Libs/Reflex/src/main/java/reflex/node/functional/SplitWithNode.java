/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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
import reflex.value.ReflexValue;

public class SplitWithNode extends BaseNode {

    private ReflexNode expression;
    private String fnName;
    private ImportHandler importHandler;
    private LanguageRegistry languageRegistry;

    public SplitWithNode(int lineNumber, IReflexHandler handler, Scope s, String fnName, ReflexNode e, LanguageRegistry languageRegistry,
            ImportHandler importHandler) {
        super(lineNumber, handler, s);
        this.expression = e;
        this.fnName = fnName;
        this.languageRegistry = languageRegistry;
        this.importHandler = importHandler;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue argList = expression.evaluate(debugger, scope);
        // for each element in the list, call the fn referenced by fnName with
        // those parameters
        // if the return value of that function is Boolean true, add the
        // parameter to the return value
        Function f = languageRegistry.getFunction(FunctionFactory.createFunctionKey(fnName, 1)); // 1
                                                                                         // parameter
        List<ReflexValue> inputVals = argList.asList();
        List<ReflexValue> retValsFirst = new ArrayList<ReflexValue>(inputVals.size());
        List<ReflexValue> retValsLast = new ArrayList<ReflexValue>(inputVals.size());
        boolean taking = true;
        for (ReflexValue v : inputVals) {
            if (taking) {
                List<ReflexNode> params = new ArrayList<ReflexNode>();
                params.add(new AtomNode(lineNumber, handler, scope, v));
                ReflexValue ret = f.invoke(debugger, lineNumber, params, languageRegistry, handler, scope, importHandler);
                if (ret.isBoolean() && ret.asBoolean() == false) {
                    taking = false;
                }
            }

            if (taking) {
                retValsFirst.add(v);
            } else {
                retValsLast.add(v);
            }
        }

        List<ReflexValue> retVals = new ArrayList<ReflexValue>();
        retVals.add(new ReflexValue(retValsFirst));
        retVals.add(new ReflexValue(retValsLast));
        ReflexValue ret = new ReflexValue(retVals);
        debugger.stepEnd(this, ret, scope);
        return ret;
    }

    @Override
    public String toString() {
        return super.toString() + " - " + String.format("filter(%s,%s)", fnName, expression);
    }
}
