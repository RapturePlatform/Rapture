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
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.importer.ImportHandler;
import reflex.value.ReflexValue;

public class PackageCallNode extends BaseNode {

    private String identifier;
    private List<ReflexNode> params;
    private ImportHandler importHandler;
    private NativeCallNode possibleNativeCallNode;

    public PackageCallNode(int lineNumber, IReflexHandler handler, Scope s, ImportHandler importHandler, String id, List<ReflexNode> ps) {
        super(lineNumber, handler, s);
        identifier = id;
        params = ps == null ? new ArrayList<ReflexNode>() : ps;
        this.importHandler = importHandler;
        String[] parts = id.split("\\.");
        possibleNativeCallNode = new NativeCallNode(lineNumber, handler, s, parts[0], parts[1], ps);
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        ReflexValue ret;
        debugger.stepStart(this, scope);
        ret = importHandler.executeImportFunction(this, identifier, params, debugger, scope, possibleNativeCallNode);
        debugger.stepEnd(this, ret, scope);
        return ret;

    }
}
