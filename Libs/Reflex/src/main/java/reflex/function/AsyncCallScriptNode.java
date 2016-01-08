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
package reflex.function;

import java.util.ArrayList;
import java.util.List;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.KernelExecutor;
import reflex.node.ReflexNode;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

/**
 * The first parameter is a script, we run that script as a Reflex script, after
 * injecting the parameters defined by the map in the *optional* second
 * parameter. We run the script asynchronously, and return the task id as the
 * return value
 * 
 * @author amkimian
 * 
 */
public class AsyncCallScriptNode extends BaseNode {

    private ReflexNode authorityNode;
    private ReflexNode scriptNode;
    private ReflexNode paramsNode;

    public AsyncCallScriptNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode authorityNode, ReflexNode scriptNode, ReflexNode paramsNode) {
        super(lineNumber, handler, s);
        this.authorityNode = authorityNode;
        this.scriptNode = scriptNode;
        this.paramsNode = paramsNode;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue authority = authorityNode.evaluate(debugger, scope);
        ReflexValue script = scriptNode.evaluate(debugger, scope);
        ReflexValue val = new ReflexNullValue(lineNumber);;
        if (paramsNode != null) {
            val = paramsNode.evaluate(debugger, scope);
        }
        // String asyncReflexReference(CallingContext context, String authority,
        // String scriptName, Map<String, String> params);

        List<ReflexValue> apiParams = new ArrayList<ReflexValue>(3);
        apiParams.add(authority);
        apiParams.add(script);
        apiParams.add(val);

        ReflexValue retVal = KernelExecutor.executeFunction(lineNumber, handler.getApi(), "async", "asyncReflexReference", apiParams);
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    @Override
    public String toString() {
        return super.toString() + " - " + String.format("!callScript(%s,%s)", scriptNode, paramsNode == null ? "" : paramsNode);
    }
}
