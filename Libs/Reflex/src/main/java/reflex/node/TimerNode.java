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
import reflex.value.ReflexTimerValue;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class TimerNode extends BaseNode {

    private ReflexNode timerName;

    public TimerNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode timerName) {
        super(lineNumber, handler, s);
        this.timerName = timerName;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {

        debugger.stepStart(this, scope);
        // Two modes. If timerName is null, create a new TimerValue. Otherwise, return the current elapsed time of
        // the timerName (which needs to be a TimerValue)
        ReflexValue retVal = new ReflexNullValue(lineNumber);;
        if (timerName == null) {
            retVal = new ReflexValue(new ReflexTimerValue());
        } else {
            ReflexValue timerVal = timerName.evaluate(debugger, scope);
            if (timerVal.isTimer()) {
                retVal = new ReflexValue(timerVal.asTimer().getElapsed());
            } else {
                throw new ReflexException(lineNumber, String.format("Value is not a timer"));
            }
        }
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    @Override
    public String toString() {
        return String.format("timer(%s)", timerName == null ? "" : timerName);
    }
}
