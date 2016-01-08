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

import java.util.Map;

import reflex.IReflexHandler;
import reflex.ReflexExecutor;
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
 * parameter.
 * 
 * @author amkimian
 * 
 */
public class ChainNode extends BaseNode {

    private ReflexNode scriptNode;
    private ReflexNode paramsNode;

    public ChainNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode scriptNode, ReflexNode paramsNode) {
        super(lineNumber, handler, s);
        this.scriptNode = scriptNode;
        this.paramsNode = paramsNode;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        String script = scriptNode.evaluate(debugger, scope).asString();
        Map<String, Object> params = null;
        if (paramsNode != null) {
            ReflexValue val = paramsNode.evaluate(debugger, scope);
            params = KernelExecutor.convert(val.asMap());
        }
        // Now chain it
        debugger.recordMessage("Chaining " + script);
        Object val = ReflexExecutor.runReflexProgram(script, handler, params);
        ReflexValue retVal = val == null ? new ReflexNullValue(lineNumber) : new ReflexValue(val);
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    @Override
    public String toString() {
        return super.toString() + " - " + String.format("chain(%s,%s)", scriptNode, paramsNode == null ? "" : paramsNode);
    }
}
