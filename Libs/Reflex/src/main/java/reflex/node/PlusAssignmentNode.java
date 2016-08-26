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
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ImmutableReflexValue;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

public class PlusAssignmentNode extends BaseNode {
    private String identifier;
    private ReflexNode rhs;

    public PlusAssignmentNode(int lineNumber, IReflexHandler handler, Scope s, String i, ReflexNode n) {
        super(lineNumber, handler, s);
        this.identifier = i;
        this.rhs = n;
    }

    @Override
    public String toString() {
        return String.format("(%s += %s)", identifier, rhs);
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        // This is plus assignment. 
        // The purpose is to "add" the rhs to the identifier, 
    	// depending on what type of value it is.

        debugger.stepStart(this, scope);
        ReflexValue var = scope.resolve(identifier);

        if (var.isList() && rhs instanceof UnaryMinusNode) {
            // LIST -= VALUE is a special case
            ((UnaryMinusNode) rhs).removeFromList(debugger, scope, var);
        } else {
            ReflexValue value = rhs.evaluate(debugger, scope);
            if (var == null) {
                throw new ReflexException(lineNumber, identifier + " not found");
            }
            if (var.isList()) {
                var.asList().add(value);
            } else if (var.isInteger() && value.isInteger()) {
                Long result = var.asLong() + value.asLong();
                int result_int = result.intValue();
                if (var instanceof ImmutableReflexValue) {
                    ReflexValue newValue;
                    if (result == result_int) {
                        newValue = new ReflexValue(result_int);
                    } else {
                        newValue = new ReflexValue(result);
                    }
                    scope.assign(identifier, newValue);
                } else {
                    if (result == result_int) {
                        var.setValue(result_int);
                    } else {
                        var.setValue(result);
                    }
                }
            } else if (var.isNumber() && value.isNumber()) {
                if (var instanceof ImmutableReflexValue) {
                    ReflexValue newValue = new ReflexValue(var.asBigDecimal().add(value.asBigDecimal()));
                    scope.assign(identifier, newValue);
                } else {
                    var.setValue(var.asBigDecimal().add(value.asBigDecimal()));
                }
            } else if (var.isString()) {
                if (var instanceof ImmutableReflexValue) {
                    ReflexValue newValue = new ReflexValue(var.asString() + value.toString());
                    scope.assign(identifier, newValue);
                } else {
                    var.setValue(var.asString() + value.toString());
                }
            }
        }
        debugger.stepEnd(this, new ReflexVoidValue(lineNumber), scope);
        return new ReflexVoidValue();
    }
}
