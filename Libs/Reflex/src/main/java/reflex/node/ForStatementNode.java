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

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

public class ForStatementNode extends BaseNode {

    private String identifier;
    private ReflexNode startExpr;
    private ReflexNode stopExpr;
    private ReflexNode block;

    public ForStatementNode(int lineNumber, IReflexHandler handler, Scope s, String id, ReflexNode start, ReflexNode stop, ReflexNode bl) {
        super(lineNumber, handler, s);
        identifier = id;
        startExpr = start;
        stopExpr = stop;
        block = bl;
    }

    @Override
    public ReflexValue evaluateWithResume(IReflexDebugger debugger, Scope scope) {
        if (handler.getSuspendHandler().containsResume(nodeId, "iteration")) {
            int start = (Integer) handler.getSuspendHandler().getResumeContext(nodeId, "start");
            int stop = (Integer) handler.getSuspendHandler().getResumeContext(nodeId, "stop");
            int iteration = (Integer) handler.getSuspendHandler().getResumeContext(nodeId, "iteration");
            for (int i = iteration; i <= stop; i++) {
                scope.assign(identifier, new ReflexValue(i));
                ReflexValue returnValue = new ReflexVoidValue(lineNumber);
                if (i == iteration) {
                    returnValue = block.evaluateWithResume(debugger, scope);
                } else {
                    returnValue = block.evaluate(debugger, scope);
                }
                if (returnValue .getValue() == ReflexValue.Internal.BREAK || returnValue .getValue() == ReflexValue.Internal.SUSPEND
                        || (returnValue .getValue() != ReflexValue.Internal.VOID && returnValue.getValue() != ReflexValue.Internal.CONTINUE && returnValue.isReturn())) {
                    debugger.stepEnd(this, returnValue, scope);
                    if (returnValue .getValue() == ReflexValue.Internal.BREAK) {
                        returnValue = new ReflexVoidValue(lineNumber);
                    }
                    if (returnValue .getValue() == ReflexValue.Internal.SUSPEND) {
                        handler.getSuspendHandler().addResumeContext(nodeId, "start", start);
                        handler.getSuspendHandler().addResumeContext(nodeId, "stop", stop);
                        handler.getSuspendHandler().addResumeContext(nodeId, "iteration", i);
                    }
                    return returnValue;
                }
            }

        }
        return new ReflexVoidValue();
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        int start = startExpr.evaluate(debugger, scope).asDouble().intValue();
        int stop = stopExpr.evaluate(debugger, scope).asDouble().intValue();

        for (int i = start; i <= stop; i++) {
            scope.assign(identifier, new ReflexValue(i));
            ReflexValue returnValue = block.evaluate(debugger, scope);
            if (returnValue .getValue() == ReflexValue.Internal.BREAK || returnValue .getValue() == ReflexValue.Internal.SUSPEND
                    || (returnValue .getValue() != ReflexValue.Internal.VOID && returnValue.getValue() != ReflexValue.Internal.CONTINUE && returnValue.isReturn())) {
                debugger.stepEnd(this, returnValue, scope);
                if (returnValue .getValue() == ReflexValue.Internal.BREAK) {
                    returnValue = new ReflexVoidValue(lineNumber);
                }
                if (returnValue .getValue() == ReflexValue.Internal.SUSPEND) {
                    handler.getSuspendHandler().addResumeContext(nodeId, "start", start);
                    handler.getSuspendHandler().addResumeContext(nodeId, "stop", stop);
                    handler.getSuspendHandler().addResumeContext(nodeId, "iteration", i);
                }
                return returnValue;
            }
        }

        debugger.stepEnd(this, new ReflexVoidValue(lineNumber), scope);
        return new ReflexVoidValue();
    }
}
