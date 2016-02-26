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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.ReflexNode;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class SortNode extends BaseNode {
    private ReflexNode expr;
    private ReflexNode ascend;

    public SortNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode arg, ReflexNode asc) {
        super(lineNumber, handler, s);
        this.expr = arg;
        this.ascend = asc;
    }

    class LocalComparator<T extends Comparable<T>> implements Comparator<T> {
        boolean ascending = true;
        boolean compareStringsAsNumbers = true;
        
        public LocalComparator<T> setCompareStringsAsNumbers(boolean flag) {
            compareStringsAsNumbers = flag;
            return this;
        }
        
        public LocalComparator<T> setAscending(boolean flag) {
            ascending = flag;
            return this;
        }
        
        public LocalComparator<T> setDescending() {
            ascending = false;
            return this;
        }

        @Override
        public int compare(T o1, T o2) {
            
            if (compareStringsAsNumbers) {
                ReflexValue n1 = null;
                ReflexValue n2 = null;
                if (o1 instanceof ReflexValue) try {
                    ReflexValue r1 = (ReflexValue) o1;
                    if (r1.isString()) {
                        n1 = new ReflexValue(Double.parseDouble(r1.asString()));
                    } else if (r1.isNumber()) {
                        n1 = r1;
                    }
                } catch (NumberFormatException e) {
                }
                if (o2 instanceof ReflexValue) try {
                    ReflexValue r2 = (ReflexValue) o2;
                    if (r2.isString()) {
                        n2 = new ReflexValue(Double.parseDouble(r2.asString()));
                    } else if (r2.isNumber()) {
                        n2 = r2;
                    }
                } catch (NumberFormatException e) {
                }
                if ((n1 != null) && (n2 != null)) {
                    if (ascending)
                        return n1.compareTo(n2);
                    else
                        return n2.compareTo(n1);
                }
            }
            if (ascending)
                return o1.compareTo(o2);
            else
                return o2.compareTo(o1);
        }
    }
    
    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue retVal = new ReflexNullValue(lineNumber);
        ReflexValue val = expr.evaluate(debugger, scope);
        
        // TODO specify sort collation (assume ASCII order for now)
        // - May want to sort strings numerically not lexically
        // - how do you order Macallan, McAvoy, McDonald, McIlroy, MacIntyre, McTavish, Mabel
        // (Trick question: for a phone book that's the right order)
        
        // TODO Should be able to order maps by values?
        
        // TODO What about strings that begin with numbers: "10 Downing Street", "22 Acacia Avenue", "1600 Pennsyvania Avenue"

        boolean ascending = true;
        ReflexValue asc = ascend.evaluate(debugger, scope);
        if (asc.isBoolean()) ascending = asc.asBoolean();
        
        if (val.isMap()) {
            LocalComparator<String> comparator = new LocalComparator<String>().setAscending(ascending);
            SortedMap<String, Object> sortedMap = new TreeMap<>(comparator);
            Map<String, Object> map = val.asMap();
            for (String key : map.keySet()) {
                sortedMap.put(key, map.get(key));
            }
            retVal = new ReflexValue(sortedMap);
        } else if (val.isList()) {
            List<ReflexValue> list = val.asList();
            ReflexValue[] array = list.toArray(new ReflexValue[list.size()]);
            Arrays.sort(array, new LocalComparator<ReflexValue>().setAscending(ascending));
            retVal = new ReflexValue(Arrays.asList(array));
        }
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }
}
