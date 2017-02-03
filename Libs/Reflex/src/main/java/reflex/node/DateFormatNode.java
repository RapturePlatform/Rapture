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


import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexTimeValue;
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
    private ReflexNode timezone;

    public DateFormatNode(int lineNumber, IReflexHandler handler, Scope scope, ReflexNode dateNode, ReflexNode format, ReflexNode timezone) {
        super(lineNumber, handler, scope);
        this.dateNode = dateNode;
        this.format = format;
        this.timezone = timezone;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue dateValue = dateNode.evaluate(debugger, scope);

        // could be null
        ReflexValue dateFormatValue = null;
        ReflexValue retVal = null;
        DateTimeZone zone = null;
        if (timezone != null) {
            ReflexValue rv = timezone.evaluate(debugger, scope);
            try {
                if (!rv.isNull()) zone = DateTimeZone.forID(rv.asString());
            } catch (IllegalArgumentException e) {
                log.error("Unrecognised time zone identifier " + rv.asString());
            }
        }

        DateTimeFormatter dtf = null;
        if (format != null) {
            dateFormatValue = format.evaluate(debugger, scope);
            String dateFormat = dateFormatValue.asString();
            dtf = DateTimeFormat.forPattern(dateFormat);
        }

        if (dateValue.isDate()) {
            retVal = new ReflexValue(dateValue.asDate().toString(dtf, zone));
        } else if (dateValue.isTime()) {
            retVal = new ReflexValue(dateValue.asTime().toString(dtf, zone));
        } else if (dateValue.isNumber()) {
            ReflexTimeValue rdv = new ReflexTimeValue(dateValue.asLong(), zone);
            retVal = new ReflexValue(rdv.toString(dtf, zone));
        } else {
            throwError("Illegal argument ", dateNode, format, dateValue, dateFormatValue);
        }

        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

}
