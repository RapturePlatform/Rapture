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
package reflex.node;

import java.util.ArrayList;
import java.util.List;

import reflex.Function;
import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.importer.ImportHandler;
import reflex.util.function.FunctionFactory;
import reflex.util.function.FunctionKey;
import reflex.util.function.LanguageRegistry;
import reflex.value.ReflexValue;

public class FunctionCallNode extends BaseNode {

    private String functionName;
    private List<ReflexNode> params;
    private LanguageRegistry languageRegistry;
    private ImportHandler importHandler;
    private String namespacePrefix;

    public FunctionCallNode(int lineNumber, IReflexHandler handler, Scope scope, String functionId, List<ReflexNode> params, LanguageRegistry languageRegistry,
            ImportHandler importHandler, String namespacePrefix) {
        super(lineNumber, handler, scope);
        this.functionName = functionId;
        this.namespacePrefix = namespacePrefix;
        this.params = params == null ? new ArrayList<ReflexNode>() : params;
        this.languageRegistry = languageRegistry;
        this.importHandler = importHandler;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        FunctionKey functionKey;

        if (namespacePrefix == null) {
            functionKey = FunctionFactory.createFunctionKey(functionName, params.size());
        } else {
            functionKey = FunctionFactory.createFunctionKey(namespacePrefix, functionName, params.size());
        }

        Function f = languageRegistry.getFunction(functionKey);
        if (f == null) {
            throw new ReflexException(lineNumber, String.format("No such function: '%s' taking %s argument(s)", functionKey.getName(), functionKey.getNumParams()));
        }

        Function function = new Function(f);

        ReflexValue ret = function.invoke(debugger, lineNumber, params, languageRegistry, handler, scope, importHandler);
        ret.setReturn(false);
        debugger.stepEnd(this, ret, scope);
        return ret;

    }
}
