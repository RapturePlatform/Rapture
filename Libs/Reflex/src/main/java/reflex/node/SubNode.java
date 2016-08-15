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

import java.util.List;

import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class SubNode extends BaseNode {

    private ReflexNode lhs;
    private ReflexNode rhs;

    public SubNode(int line, IReflexHandler handler, Scope s, ReflexNode lhs, ReflexNode rhs) {
        super(line, handler, s);
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue a = lhs.evaluate(debugger, scope);
        ReflexValue b = rhs.evaluate(debugger, scope);
        ReflexValue ret = new ReflexNullValue(lineNumber);;

        // number - number
        if (a.isInteger() && b.isInteger()) {
            // Don't overflow.
            Long longer = a.asLong() - b.asLong();
            int lint = longer.intValue();
            // DO NOT USE THE TERNARY OPERATOR HERE - it forces val to be of type LONG 
            Object val = lint;
            if (longer != lint) {
            	val = longer;
            }
            ret = new ReflexValue(val);
        } else if (a.isNumber() && b.isNumber()) {
        	ret = new ReflexValue(a.asBigDecimal().subtract(b.asBigDecimal()));
        } else if (a.isDate() && b.isNumber()) {
            ret = new ReflexValue(a.asDate().sub(b.asInt()));
        } else if (a.isList()) {
            List<ReflexValue> list = a.asList();
            if (!list.contains(b)) {
                throw new ReflexException(lineNumber, "List does not contain the value " + b.toString());
            }
            list.remove(b);
            ret = new ReflexValue(lineNumber, list);
        } else {
            throwError("both sides must be numeric or the left side must be a list", lhs, rhs, a, b);
        }
        debugger.stepEnd(this, ret, scope);
        return ret;
    }

    @Override
    public String toString() {
        return String.format("(%s - %s)", lhs, rhs);
    }
}
