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
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;

public class InNode extends BaseNode {

    private ReflexNode lhs;
    private ReflexNode rhs;

    public InNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode lhs, ReflexNode rhs) {
        super(lineNumber, handler, s);
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue a = lhs.evaluate(debugger, scope);
        ReflexValue b = rhs.evaluate(debugger, scope);

        if (!b.isList()) {
            throw new ReflexException(lineNumber, "illegal expression: " + this);
        }

        List<ReflexValue> list = b.asList();
        ReflexValue ret = new ReflexValue(lineNumber, false);
        for (ReflexValue val : list) {
            if (val.equals(a)) {
                ret = new ReflexValue(lineNumber, true);
                break;
            }
        }
        debugger.stepEnd(this, ret, scope);
        return ret;
    }

    @Override
    public String toString() {
        return String.format("(%s in %s)", lhs, rhs);
    }
}
