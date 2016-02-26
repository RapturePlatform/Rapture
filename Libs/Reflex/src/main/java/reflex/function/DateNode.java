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

import java.util.ArrayList;
import java.util.List;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.ReflexNode;
import reflex.value.ReflexDateValue;
import reflex.value.ReflexValue;

/**
 * The date node simply returns (currently) a string that is equal to YYYY-MM-DD
 * 
 * @author amkimian
 * 
 */
public class DateNode extends BaseNode {
    private List<ReflexNode> exprList;

    public DateNode(int lineNumber, IReflexHandler handler, Scope scope, List<ReflexNode> eList) {
        super(lineNumber, handler, scope);
        if (eList == null) {
            this.exprList = new ArrayList<ReflexNode>();
        } else {
            this.exprList = eList;
        }
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexDateValue val = null;
        ReflexValue initValue = null;
        String calendarString = "";

        if (exprList.isEmpty()) {
            val = new ReflexDateValue();
        } else {
            // If there is one value it will be a
            initValue = exprList.get(0).evaluate(debugger, scope);
            if (exprList.size() > 1) {
                calendarString = exprList.get(1).evaluate(debugger, scope).asString();
            }
            if (initValue.isDate()) {
                val = new ReflexDateValue(initValue.asDate(), calendarString);
            } else {
                val = new ReflexDateValue(initValue.asString(), calendarString);
            }
        }
        ReflexValue retVal = new ReflexValue(val);
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    @Override
    public String toString() {
        return super.toString() + " - Date()";
    }
}
