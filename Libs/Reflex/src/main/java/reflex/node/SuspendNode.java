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

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexSuspendValue;
import reflex.value.internal.ReflexVoidValue;

// Suspend!

public class SuspendNode extends BaseNode {

    private ReflexNode suspensionTime;

    public SuspendNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode suspensionTime) {
        super(lineNumber, handler, s);
        this.suspensionTime = suspensionTime;
    }

    @Override
    public ReflexValue evaluateWithResume(IReflexDebugger debugger, Scope scope) {
        if (handler.getSuspendHandler().getResumePoint().equals(nodeId)) {
            handler.getSuspendHandler().addResumePoint("");
            return new ReflexVoidValue();
        } else {
            return evaluate(debugger, scope);
        }
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        debugger.recordMessage("Suspending...");
        ReflexValue suspendTime = suspensionTime.evaluate(debugger, scope);
        handler.getSuspendHandler().suspendTime(suspendTime.asInt());
        handler.getSuspendHandler().addResumePoint(nodeId);
        debugger.stepEnd(this, new ReflexSuspendValue(lineNumber), scope);
        return new ReflexSuspendValue(lineNumber);
    }

}
