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
import java.util.ArrayList;
import java.util.List;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class MulNode extends BaseNode {

    private ReflexNode lhs;
    private ReflexNode rhs;

    public MulNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode lhs, ReflexNode rhs) {
        super(lineNumber, handler, s);
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue a = lhs.evaluate(debugger, scope);
        ReflexValue b = rhs.evaluate(debugger, scope);
        ReflexValue retVal = new ReflexNullValue(lineNumber);

        // number * number
        if (a.isInteger() && b.isInteger()) {
            // If it'll fit as an integer keep it as an integer otherwise make it a long
            Long longer = a.asLong() * b.asLong();
            int lint = longer.intValue();
            retVal = new ReflexValue((longer == lint) ? lint : longer);
        } else if (a.isNumber() && b.isNumber()) {
        	BigDecimal bigA = a.asBigDecimal();
        	BigDecimal bigB = b.asBigDecimal();
            retVal = new ReflexValue(bigA.multiply(bigB));
        } else if (a.isString() && b.isNumber()) {
            StringBuilder str = new StringBuilder();
            int stop = b.asDouble().intValue();
            for (int i = 0; i < stop; i++) {
                str.append(a.asString());
            }
            retVal = new ReflexValue(lineNumber, str.toString());
        } else if (a.isList() && b.isNumber()) {
            int stop = b.asDouble().intValue();
            List<ReflexValue> total = new ArrayList<ReflexValue>(stop);
            for (int i = 0; i < stop; i++) {
                total.addAll(a.asList());
            }
            retVal = new ReflexValue(lineNumber, total);
        } else {
            throwError("both must be numeric or if the rhs is a number the lhs can be a string or a list", lhs, rhs, a, b);
        }
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    @Override
    public String toString() {
        return String.format("(%s * %s)", lhs, rhs);
    }
}
