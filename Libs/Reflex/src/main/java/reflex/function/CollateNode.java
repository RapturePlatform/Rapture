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

import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.ReflexNode;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class CollateNode extends BaseNode {
    private ReflexNode expr;
    private ReflexNode locale;
    
    private boolean collateNames = true;

    public CollateNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode arg, ReflexNode locale) {
        super(lineNumber, handler, s);
        this.expr = arg;
        this.locale = locale;
    }
    
    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue retVal = new ReflexNullValue(lineNumber);
        ReflexValue val = expr.evaluate(debugger, scope);
        ReflexValue asc = locale.evaluate(debugger, scope);

        String locName = asc.asString();
        Collator collator;
        
        Locale locale = new Locale(locName);
        collator = Collator.getInstance(locale);
        
        if (val.isMap()) {
            SortedMap<String, Object> sortedMap = new TreeMap<>(collator);
            Map<String, Object> map = val.asMap();
            for (String key : map.keySet()) {
                sortedMap.put(key, map.get(key));
            }
            retVal = new ReflexValue(sortedMap);
        } else if (val.isList()) {
            Set<String> set = new TreeSet<>(collator);
            for (ReflexValue rv : val.asList())
                set.add(rv.asString());
            List<ReflexValue> list = new ArrayList<>();
            for (String str : set)
                list.add(new ReflexValue(str));
            retVal = new ReflexValue(list);
        }
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }
}
