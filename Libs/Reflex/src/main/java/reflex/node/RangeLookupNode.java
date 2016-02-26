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

import java.util.ArrayList;
import java.util.List;

import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

public class RangeLookupNode extends BaseNode {

    private ReflexNode expression;
    private ReflexNode start;
    private ReflexNode end;

    public RangeLookupNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode e, ReflexNode start, ReflexNode end) {
        super(lineNumber, handler, s);
        expression = e;
        this.start = start;
        this.end = end;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue value = expression.evaluate(debugger, scope);
        ReflexValue str = start.evaluate(debugger, scope);
        ReflexValue en = end.evaluate(debugger, scope);
        ReflexValue retVal = new ReflexVoidValue(lineNumber);

        if (value.isList()) {
            Integer iStart = (Integer) str.asInt();
            Integer iEnd = (Integer) en.asInt();
            List<ReflexValue> ret = new ArrayList<ReflexValue>(iEnd - iStart);
            List<ReflexValue> vals = value.asList();
            if (iStart >= 0 && iStart < vals.size() && iEnd > iStart && iEnd <= vals.size()) {
                for (int i = iStart; i < iEnd; i++) {
                    ret.add(vals.get(i));
                }
                retVal = new ReflexValue(ret);
            }
        } else if (value.isString()) {
            Integer iStart = (Integer) str.asInt();
            Integer iEnd = (Integer) en.asInt();
            String newStr = value.asString().substring(iStart, iEnd);
            retVal = new ReflexValue(newStr);
        }
        if (retVal .getValue() == ReflexValue.Internal.VOID) {
            throw new ReflexException(lineNumber, "Cannot run [] like this");
        }
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }
}
