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

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class DivNode extends BaseNode {

    private ReflexNode lhs;
    private ReflexNode rhs;

    public DivNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode lhs, ReflexNode rhs) {
        super(lineNumber, handler, s);
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue a = lhs.evaluate(debugger, scope);
        ReflexValue b = rhs.evaluate(debugger, scope);

        if (b.equals(0)) {
            throwError("division by zero", lhs, rhs, a, b);
        }
        // number / number
        if (a.isNumber() && b.isNumber()) {
            ReflexValue retVal;
            try {
                BigDecimal bigA = a.asBigDecimal();
                BigDecimal bigB = b.asBigDecimal();
                BigDecimal product = bigA.divide(bigB);
                try {
                    retVal = new ReflexValue(product.intValueExact());
                } catch (ArithmeticException e) {
                    retVal = new ReflexValue(product);
                }
            } catch (ArithmeticException e) {
                retVal = new ReflexValue(a.asDouble() / b.asDouble());
            }
            debugger.stepEnd(this, retVal, null);
            return retVal;
        }

        throwError("both must be numeric", lhs, rhs, a, b);
        debugger.stepEnd(this, new ReflexNullValue(), scope);
        return new ReflexNullValue();
    }

    @Override
    public String toString() {
        return String.format("(%s / %s)", lhs, rhs);
    }
}
