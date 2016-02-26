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

import java.util.ArrayList;
import java.util.List;

import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.importer.ImportHandler;
import reflex.util.function.LanguageRegistry;
import reflex.value.ReflexValue;

public class QualifiedFuncCallNode extends BaseNode {

    private String id;
    private String namespacePrefix;
    private List<ReflexNode> params;
    private LanguageRegistry languageRegistry;
    private ImportHandler importHandler;

    public QualifiedFuncCallNode(int lineNumber, IReflexHandler handler, Scope scope, String id, List<ReflexNode> params, LanguageRegistry languageRegistry,
            ImportHandler importHandler, String namespacePrefix) {
        super(lineNumber, handler, scope);
        this.id = id;
        this.params = params == null ? new ArrayList<ReflexNode>() : params;
        this.languageRegistry = languageRegistry;
        this.importHandler = importHandler;
        this.namespacePrefix = namespacePrefix;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        ReflexValue val;
        if (languageRegistry.hasFunction(namespacePrefix, id, params.size())) {
            FunctionCallNode node = new FunctionCallNode(lineNumber, handler, scope, id, params, languageRegistry, importHandler, namespacePrefix);
            return node.evaluate(debugger, scope);
        } else { // check if native call
            String[] parts = id.split("\\.");
            if (parts.length == 2) {
                val = scope.resolve(parts[0]);
                if (val != null) {
                    // Evaluate the method on the variable (using reflection)
                    NativeCallNode node = new NativeCallNode(lineNumber, handler, scope, parts[0], parts[1], params);
                    return node.evaluate(debugger, val, scope);
                }
            }
        }

        throw new ReflexException(getLineNumber(), id + " is not a valid function call nor a native call");
    }

}
