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
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

public class IfNode extends BaseNode {

    private class Choice {

        ReflexNode expression;
        ReflexNode block;

        Choice(ReflexNode e, ReflexNode b) {
            expression = e;
            block = b;
        }
    }

    private List<Choice> choices;

    public IfNode(int lineNumber, IReflexHandler handler, Scope scope) {
        super(lineNumber, handler, scope);
        choices = new ArrayList<Choice>(2);
    }

    public void addChoice(ReflexNode e, ReflexNode b) {
        choices.add(new Choice(e, b));
    }

    @Override
    public ReflexValue evaluateWithResume(IReflexDebugger debugger, Scope scope) {
        ReflexValue ret = new ReflexVoidValue(lineNumber);
        if (handler.getSuspendHandler().containsResume(nodeId, "choice")) {
            int choice = (Integer) handler.getSuspendHandler().getResumeContext(nodeId, "choice");
            Choice ch = choices.get(choice);
            ret = ch.block.evaluateWithResume(debugger, scope);
            if (ret .getValue() == ReflexValue.Internal.SUSPEND) {
                handler.getSuspendHandler().addResumeContext(nodeId, "choice", choice);
            }
        }
        return ret;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue ret = new ReflexVoidValue(lineNumber);
        int choiceTaken = 0;
        for (Choice ch : choices) {
            ReflexValue value = ch.expression.evaluate(debugger, scope);

            if (!value.isBoolean()) {
                throw new ReflexException(lineNumber, "illegal boolean expression " + "inside if-statement: " + ch.expression);
            }

            if (value.asBoolean()) {
                ret = ch.block.evaluate(debugger, scope);
                if (ret .getValue() == ReflexValue.Internal.SUSPEND) {
                    handler.getSuspendHandler().addResumeContext(nodeId, "choice", choiceTaken);
                }
                break;
            }
            choiceTaken++;
        }
        debugger.stepEnd(this, ret, scope);
        return ret;
    }
}
