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
package reflex.node;

import reflex.DebugLevel;
import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ImmutableReflexValue;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

public class ContainsNode extends BaseNode {

    private String varName = null;
    private String namespacePrefix;
    private ReflexNode expression;

    public ContainsNode(int lineNumber, IReflexHandler handler, Scope scope, String varName, ReflexNode expression, String namespacePrefix) {
        super(lineNumber, handler, scope);
        this.varName = varName;
        this.namespacePrefix = namespacePrefix;
        this.expression = expression;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue list = scope.resolve(varName, namespacePrefix);
        ReflexValue value = expression.evaluate(debugger, scope);

        handler.getDebugHandler().statementReached(lineNumber, DebugLevel.INFO, "Does " + varName + " contain " + value.toString());

        if (!list.isList()) {
            String errorMessage = "Contains expects a List but argument " + varName + " is " + list.getValueTypeString();
            throw new ReflexException(lineNumber, errorMessage);
        }

        ReflexValue ret = ImmutableReflexValue.FALSE;
        for (ReflexValue rv : list.asList()) {
            if (rv.equals(value)) ret = ImmutableReflexValue.TRUE;
        }
        debugger.stepEnd(this, new ReflexVoidValue(lineNumber), scope);
        return ret;
    }
}
