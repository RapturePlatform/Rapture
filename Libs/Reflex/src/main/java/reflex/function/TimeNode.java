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
package reflex.function;

import java.util.List;

import org.joda.time.DateTimeZone;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.ReflexNode;
import reflex.value.ReflexTimeValue;
import reflex.value.ReflexValue;

/**
 * The date node simply returns (currently) a string that is equal to YYYY-MM-DD
 * 
 * @author amkimian
 * 
 */
public class TimeNode extends BaseNode {
    private List<ReflexNode> exprList;

    public TimeNode(int lineNumber, IReflexHandler handler, Scope scope, List<ReflexNode> eList) {
        super(lineNumber, handler, scope);
        this.exprList = eList;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexTimeValue val = null;
        DateTimeZone timezone = DateTimeZone.UTC;

        if ((exprList == null) || exprList.isEmpty()) {
            val = new ReflexTimeValue();
        } else {
            // If there is one value it will be a
            ReflexValue initValue = exprList.get(0).evaluate(debugger, scope);
            if (exprList.size() > 1) {
                ReflexValue rv = exprList.get(1).evaluate(debugger, scope);
                if (!rv.isNull()) timezone = DateTimeZone.forID(rv.asString());
            }
            if (initValue.isTime()) {
                val = new ReflexTimeValue(initValue.asTime(), timezone);
            } else if (initValue.isNumber()) {
                val = new ReflexTimeValue(initValue.asLong(), timezone);
            } else {
                val = new ReflexTimeValue(initValue.asString(), timezone);
            }
        }

        ReflexValue retVal = new ReflexValue(val);
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    @Override
    public String toString() {
        return super.toString() + " - Time()";
    }
}
