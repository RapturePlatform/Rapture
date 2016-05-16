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

import java.math.BigDecimal;
import java.util.List;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexSparseMatrixValue;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class AddNode extends BaseNode {

    private ReflexNode lhs;
    private ReflexNode rhs;

    public AddNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode lhs, ReflexNode rhs) {
        super(lineNumber, handler, s);
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {

        debugger.stepStart(this, scope);
        ReflexValue a = lhs.evaluate(debugger, scope);
        ReflexValue b = rhs.evaluate(debugger, scope);
        ReflexValue retVal = new ReflexNullValue(lineNumber);;

        // number + number
        if (a.isInteger() && b.isInteger()) {
            // Don't overflow.
            Long longer = a.asLong() + b.asLong();
            int lint = longer.intValue();
            retVal = new ReflexValue((longer == lint) ? lint : longer);
            if (!retVal.isInteger()) {
                log.warn("Result exceeds valid range for an Integer.");
            }
        } else if (a.isNumber() && b.isNumber()) {
        	BigDecimal bigA = a.asBigDecimal();
        	BigDecimal bigB = b.asBigDecimal();
            retVal = new ReflexValue(bigA.add(bigB));
        } else if (a.isDate() && b.isNumber()) {
            retVal = new ReflexValue(a.asDate().add(b.asInt()));
        } else if (a.isList()) {
            List<ReflexValue> list = a.asList();
            if (b.isList()) {
                list.addAll(b.asList());
            } else {
                list.add(b);
            }
            retVal = new ReflexValue(list);
        } else if (a.isString()) {
            retVal = new ReflexValue(a.asString() + "" + b.toString());
        } else if (b.isString()) {
            retVal = new ReflexValue(a.toString() + "" + b.asString());
        } else if (a.isSparseMatrix() && b.isSparseMatrix()) {
            ReflexSparseMatrixValue n = new ReflexSparseMatrixValue(2);
            n.merge(a.asMatrix());
            n.merge(b.asMatrix());
            retVal = new ReflexValue(n);
        } else {
            throwError("both must be numeric, or the lhs must be a list or a string", lhs, rhs, a, b);
        }
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    @Override
    public String toString() {
        return String.format("(%s + %s)", lhs, rhs);
    }
}
