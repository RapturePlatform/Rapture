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
import java.util.Map;

import rapture.common.impl.jackson.JacksonUtil;
import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.util.MapConverter;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;


/**
 * Given a string that is JSON formatted, this class will return an object
 * that represents that JSON.  It can be either a Map or a List.
 */
public class FromJsonNode extends BaseNode {

    private ReflexNode strExpr;

    public FromJsonNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode strExpr) {
        super(lineNumber, handler, s);
        this.strExpr = strExpr;
    }

    @SuppressWarnings("unchecked")
	@Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue rv = strExpr.evaluate(debugger, scope);
        String toUse = rv.toString();
        if (rv.isObject()) {
            toUse = JacksonUtil.jsonFromObject(rv.asObject());
        }
        Object ret = JacksonUtil.objectFromJson(toUse, Object.class);
        if (ret instanceof Map) {
            ret = MapConverter.convertMap((Map<String, Object>) ret);
        } else if (ret instanceof List) {
            ret = MapConverter.convertSimpleList((List<Object>) ret);
        }
        ReflexValue retVal = ret == null ? new ReflexNullValue(lineNumber) : new ReflexValue(ret);
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    @Override
    public String toString() {
        return String.format("fromjson(%s)", strExpr);
    }
}
