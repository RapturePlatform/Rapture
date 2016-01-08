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

import java.util.ArrayList;
import java.util.List;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

public class BlockNode extends BaseNode {

    private List<ReflexNode> statements;
    private ReflexNode returnStatement;

    public BlockNode(int lineNumber, IReflexHandler handler, Scope s) {
        super(lineNumber, handler, s);
        statements = new ArrayList<ReflexNode>();
        returnStatement = null;
    }

    public void addReturn(ReflexNode stat) {
        returnStatement = stat;
    }

    public void addStatement(ReflexNode stat) {
        statements.add(stat);
    }

    @Override
    public ReflexValue evaluateWithResume(IReflexDebugger debugger, Scope scope) {
        // If we are resuming, we need to get to the correct statement to
        // resume. If there is no
        // loop statment recorded for this node, this block was not active
        // during resume. If it was,
        // we need to jump to that statement, and evaluateWithResume the correct
        // statement
        // once we return from that we will have resumed, so we contrinue with
        // the evaluate loop
        ReflexValue possibleReturn = new ReflexVoidValue(lineNumber);
        if (handler.getSuspendHandler().containsResume(nodeId, "loop")) {
            Integer v = (Integer) handler.getSuspendHandler().getResumeContext(nodeId, "loop");
            int statementCount = 0;
            boolean resumeOn = false;
            int jumpToCount = v.intValue();
            for (ReflexNode stat : statements) {
                ReflexValue value = new ReflexVoidValue(lineNumber);
                if (statementCount == jumpToCount) {
                    value = stat.evaluateWithResume(debugger, scope);
                    resumeOn = true;
                } else if (resumeOn) {
                    value = stat.evaluate(debugger, scope);
                }
                if (value .getValue() == ReflexValue.Internal.BREAK || value.getValue() == ReflexValue.Internal.CONTINUE) {
                    debugger.stepEnd(this, value, scope);
                    return value;
                }
                if (value .getValue() == ReflexValue.Internal.SUSPEND) {
                    handler.getSuspendHandler().addResumeContext(nodeId, "loop", statementCount);
                    return value;
                }
                statementCount++;
            }
            if (returnStatement != null) {
                ReflexValue ret = returnStatement.evaluate(debugger, scope);
                ret.setReturn(true);
                debugger.stepEnd(this, ret, scope);
                return ret;
            } else {
                debugger.stepEnd(this, possibleReturn, scope);
                return possibleReturn;
            }
        } else {
            return new ReflexVoidValue();
        }
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue possibleReturn = new ReflexVoidValue(lineNumber);
        int statementCount = 0;
        for (ReflexNode stat : statements) {
            ReflexValue value = stat.evaluate(debugger, scope);
            // Handle break;
            if (value .getValue() == ReflexValue.Internal.BREAK || value.getValue() == ReflexValue.Internal.CONTINUE) {
                debugger.stepEnd(this, value, scope);
                return value;
            }
            // Handle suspend
            if (value .getValue() == ReflexValue.Internal.SUSPEND) {
                handler.getSuspendHandler().addResumeContext(nodeId, "loop", statementCount);
                return value;
            }
            statementCount++;
            if (value .getValue() != ReflexValue.Internal.VOID) {
                possibleReturn = value;
                if (value.isReturn()) {
                    debugger.stepEnd(this, value, scope);
                    return value;
                }
            } else {
                possibleReturn = new ReflexVoidValue(lineNumber);
            }
        }

        if (returnStatement != null) {
            ReflexValue ret = returnStatement.evaluate(debugger, scope);
            ret.setReturn(true);
            debugger.stepEnd(this, ret, scope);
            return ret;
        } else {
            debugger.stepEnd(this, possibleReturn, scope);
            return possibleReturn;
        }
    }
}
