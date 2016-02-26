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
package reflex.function;

import java.util.HashMap;
import java.util.Map;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.ReflexNode;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

/**
 * We wait for a document to become available, optionally delaying and then
 * retrying a number of times
 * 
 * @author amkimian
 * 
 */
public class SignalNode extends BaseNode {

    private ReflexNode displayNameNode;
    private ReflexNode valueNode;

    public SignalNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode displayNameNode, ReflexNode valueNode) {
        super(lineNumber, handler, s);
        this.displayNameNode = displayNameNode;
        this.valueNode = valueNode;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        // Signal is "signal(displayName, value)"
        debugger.stepStart(this, scope);
        String displayName = displayNameNode.evaluate(debugger, scope).asString();
        ReflexValue val = valueNode.evaluate(debugger, scope);

        // Now push the value to the displayName
        if (val.isMap()) {
            handler.getDataHandler().pushData(displayName, val.asMap());
        } else {
            Map<String, Object> newDataMap = new HashMap<String, Object>();
            newDataMap.put("value", val.toString());
            handler.getDataHandler().pushData(displayName, newDataMap);
        }
        debugger.stepEnd(this, new ReflexVoidValue(lineNumber), scope);
        return new ReflexVoidValue(lineNumber);
    }

    @Override
    public String toString() {
        return super.toString() + " - " + String.format("signal(%s,%s)", displayNameNode, valueNode);
    }
}
