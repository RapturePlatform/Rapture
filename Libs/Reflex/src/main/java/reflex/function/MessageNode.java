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

import java.util.ArrayList;
import java.util.List;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.KernelExecutor;
import reflex.node.ReflexNode;
import reflex.value.ReflexValue;

/**
 * A message node attempts to punt a message to a notification provider (of type
 * text)
 * 
 * @author amkimian
 * 
 */
public class MessageNode extends BaseNode {

    private ReflexNode providerNode;
    private ReflexNode messageNode;

    public MessageNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode p, ReflexNode m) {
        super(lineNumber, handler, s);
        providerNode = p;
        messageNode = m;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue providerName = providerNode.evaluate(debugger, scope);
        ReflexValue message = messageNode.evaluate(debugger, scope);

        // Make an api call
        List<ReflexValue> apiParams = new ArrayList<ReflexValue>(4);
        apiParams.add(providerName);
        apiParams.add(new ReflexValue("reflex"));
        apiParams.add(message);
        apiParams.add(new ReflexValue("STRING"));

        ReflexValue retVal = KernelExecutor.executeFunction(lineNumber, handler.getApi(), "notification", "publishNotification", apiParams);
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    @Override
    public String toString() {
        return super.toString() + " - " + String.format("messge(%s,%s)", providerNode, messageNode);
    }
}
