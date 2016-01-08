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
package reflex;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import reflex.debug.IReflexDebugger;
import reflex.importer.ImportHandler;
import reflex.node.ReflexNode;
import reflex.util.NamespaceStack;
import reflex.util.function.LanguageRegistry;
import reflex.value.ReflexValue;

public class Function {

    private String id;
    private List<String> identifiers;
    private CommonTree code;
    private NamespaceStack namespaceStack;
    private int entryCount = 0;

    public Function(Function original) {
        // Used for recursively calling functions
        id = original.id;
        identifiers = original.identifiers;
        code = original.code;
        namespaceStack = new NamespaceStack(original.namespaceStack);
    }

    public Function(String name, CommonTree ids, CommonTree block, NamespaceStack namespaceStack) {
        this.id = name;
        identifiers = toList(ids);
        code = block;
        this.namespaceStack = new NamespaceStack(namespaceStack);
    }

    public ReflexValue invoke(IReflexDebugger debugger, int lineNumber, List<ReflexNode> params, LanguageRegistry languageRegistry, IReflexHandler handler,
            Scope scope, ImportHandler importHandler) {

        if (params.size() != identifiers.size()) {
            String msg = String.format(Messages.getString("Function.illegalCall"), identifiers.size(), id); //$NON-NLS-1$
            throw new ReflexException(lineNumber, msg);
        }

        entryCount++;
        if (entryCount > 10) {
            debugger.recordMessage("Deep recursion detected - " + entryCount);
            if (entryCount > 20) {
                throw new ReflexException(lineNumber, "Stack too deep in function call");
            }
        }
        Scope nextScope = Scope.createIsolatedScope(scope);
        // Assign all expression parameters to this function's identifiers
        for (int i = 0; i < identifiers.size(); i++) {
            nextScope.assign(identifiers.get(i), params.get(i).evaluateWithoutScope(debugger));
        }

        try {
            // Create a tree walker to evaluate this function's code block
            CommonTreeNodeStream nodes = new CommonTreeNodeStream(code);
            ReflexTreeWalker walker = new ReflexTreeWalker(nodes, nextScope, languageRegistry, namespaceStack);

            walker.setReflexHandler(handler);
            walker.setImportHandler(importHandler);
            ReflexValue val = walker.walk().evaluateWithoutScope(debugger);
            return val;
        } catch (RecognitionException e) {
            // do not recover from this
            throw new ReflexException(lineNumber, ReflexExecutor.getParserExceptionDetails(e), e); //$NON-NLS-1$
        } finally {
            entryCount--;
        }
    }

    private List<String> toList(CommonTree tree) {
        List<String> ids = new ArrayList<String>();

        // convert the tree to a List of Strings
        for (int i = 0; i < tree.getChildCount(); i++) {
            CommonTree child = (CommonTree) tree.getChild(i);
            ids.add(child.getText());
        }
        return ids;
    }
}
