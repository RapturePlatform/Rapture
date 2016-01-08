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

import java.util.List;

import reflex.IReflexHandler;
import reflex.IReflexLineCallback;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;
import reflex.value.internal.ReflexSuspendValue;
import reflex.value.internal.ReflexVoidValue;

public class ForInStatementNode extends BaseNode {

    private String identifier;
    private ReflexNode listExpr;
    private ReflexNode block;

    public ForInStatementNode(int lineNumber, IReflexHandler handler, Scope s, String id, ReflexNode list, ReflexNode bl) {
        super(lineNumber, handler, s);
        identifier = id;
        listExpr = list;
        block = bl;
    }

    @Override
    public ReflexValue evaluateWithResume(IReflexDebugger debugger, Scope scope) {
        if (handler.getSuspendHandler().containsResume(nodeId, "iteration")) {
            ReflexValue list = (ReflexValue) handler.getSuspendHandler().getResumeContext(nodeId, "list");
            int drop = (Integer) handler.getSuspendHandler().getResumeContext(nodeId, "iteration");
            List<?> vals = list.asList();
            int iterCount = 0;
            for (Object v : vals) {
                if (iterCount < drop) {
                    iterCount++;
                    continue;
                }
                if (v instanceof ReflexValue) {
                    scope.assign(identifier, (ReflexValue) v);
                } else {
                    scope.assign(identifier, v == null ? new ReflexNullValue(lineNumber) : new ReflexValue(v));
                }
                ReflexValue result = (iterCount == drop) ? block.evaluateWithResume(debugger, scope) : block.evaluate(debugger, scope);
                if (result .getValue() == ReflexValue.Internal.BREAK) return new ReflexVoidValue();
                if (result .getValue() == ReflexValue.Internal.SUSPEND) {
                    handler.getSuspendHandler().addResumeContext(nodeId, "list", list);
                    handler.getSuspendHandler().addResumeContext(nodeId, "iteration", iterCount);
                    return new ReflexSuspendValue(lineNumber);
                }
                if (result .getValue() != ReflexValue.Internal.VOID && result.isReturn()) return result;
                iterCount++;
            }
        }
        return new ReflexVoidValue();
    }

    @Override
    public ReflexValue evaluate(final IReflexDebugger debugger, final Scope scope) {
        debugger.stepStart(this, scope);
        // listExpr must evaluate to a list or to a file
        ReflexValue list = listExpr.evaluate(debugger, scope);
        if (list.isList()) {
            List<?> vals = list.asList();
            int iterCount = 0;
            for (Object v : vals) {
                if (v instanceof ReflexValue) {
                    scope.assign(identifier, (ReflexValue) v);
                } else {
                    scope.assign(identifier, v == null ? new ReflexNullValue(lineNumber) : new ReflexValue(v));
                }
                ReflexValue result = block.evaluate(debugger, scope);
                if (result .getValue() == ReflexValue.Internal.BREAK) return new ReflexVoidValue();
                if (result .getValue() == ReflexValue.Internal.SUSPEND) {
                    handler.getSuspendHandler().addResumeContext(nodeId, "list", list);
                    handler.getSuspendHandler().addResumeContext(nodeId, "iteration", iterCount);
                    return new ReflexSuspendValue(lineNumber);
                }
                if (result .getValue() != ReflexValue.Internal.VOID && result.isReturn()) return result;
                iterCount++;
            }
        } else if (list.isStreamBased()) {
            // Pretend a list here

            ReflexValue retVal = handler.getIOHandler().forEachLine(list.asStream(), new IReflexLineCallback() {

                @Override
                public ReflexValue callback(String line) {
                    ReflexValue v = new ReflexValue(line);
                    scope.assign(identifier, v);
                    ReflexValue returnValue = block.evaluate(debugger, scope);
                    return returnValue;
                }

            });
            debugger.stepEnd(this, retVal, scope);
            return retVal;
        } else {
            throw new ReflexException(lineNumber, "The rhs of a for-in clause must be a list, we have a " + list.getTypeAsString());
        }

        return new ReflexVoidValue();
    }
}
