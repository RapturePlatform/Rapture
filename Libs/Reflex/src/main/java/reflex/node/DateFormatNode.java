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

import java.text.SimpleDateFormat;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;

/**
 * Format
 * 
 * @author amkimian
 * 
 */
public class DateFormatNode extends BaseNode {

    private ReflexNode dateNode;
    private ReflexNode format;

    public DateFormatNode(int lineNumber, IReflexHandler handler, Scope scope, ReflexNode dateNode, ReflexNode format) {
        super(lineNumber, handler, scope);
        this.dateNode = dateNode;
        this.format = format;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue dateValue = dateNode.evaluate(debugger, scope);

        // could be null
        ReflexValue dateFormatValue = null;
        ReflexValue retVal = null;

        if (dateValue.isDate()) {
            DateTimeFormatter dtf = null;
            if (format != null) {
                dateFormatValue = format.evaluate(debugger, scope);
                String dateFormat = dateFormatValue.asString();
                dtf = DateTimeFormat.forPattern(dateFormat);
            }
            retVal = new ReflexValue(dateValue.asDate().toString(dtf));
        } else if (dateValue.isTime()) {
            // ReflexTime still uses legacy Java date. See RAP-4113
            SimpleDateFormat dtf = null;
            if (format != null) {
                dateFormatValue = format.evaluate(debugger, scope);
                String dateFormat = dateFormatValue.asString();
                dtf = new SimpleDateFormat(dateFormat);
            }
            retVal = new ReflexValue(dateValue.asTime().toString(dtf));
        } else {
            throwError("Illegal argument ", dateNode, format, dateValue, dateFormatValue);
        }

        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

}
