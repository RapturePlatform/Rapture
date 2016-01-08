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

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.ReflexNode;
import reflex.value.ReflexValue;

/**
 * Return a random number
 * 
 * @author amkimian
 * 
 */
public class RoundNode extends BaseNode {

    private ReflexNode valueNode;
    private ReflexNode dpNode;

    public RoundNode(int lineNumber, IReflexHandler handler, Scope scope, ReflexNode valueNode, ReflexNode dpNode) {
        super(lineNumber, handler, scope);
        this.valueNode = valueNode;
        this.dpNode = dpNode;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue value = valueNode.evaluate(debugger, scope);
        int dp = 0;
        if (dpNode != null) {
            ReflexValue dpVal = dpNode.evaluate(debugger, scope);
            dp = (Integer) dpVal.asInt();
        }
        double size = Math.pow(10, dp);
        long val = Math.round(value.asDouble() * size);
        double dVal = val;
        ReflexValue retVal = new ReflexValue(dVal / size);
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    @Override
    public String toString() {
        return super.toString() + " - " + String.format("round(%s,%s)", valueNode, dpNode == null ? "" : dpNode);
    }
}
