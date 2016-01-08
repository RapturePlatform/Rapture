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
package reflex.function;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.ReflexNode;
import reflex.value.ReflexValue;

/**
 * The merge node merges two maps, only if the first does not contain the key of
 * the second
 * 
 * @author amkimian
 * 
 */
public class MergeIfNode extends BaseNode {
    private List<ReflexNode> exprList;

    public MergeIfNode(int lineNumber, IReflexHandler handler, Scope scope, List<ReflexNode> eList) {
        super(lineNumber, handler, scope);
        this.exprList = eList;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        Map<String, Object> ret = new LinkedHashMap<String, Object>();
        for (ReflexNode n : exprList) {
            ReflexValue value = n.evaluate(debugger, scope);
            if (value.isMap()) {
                apply(ret, value.asMap());
            } else {
                throw new ReflexException(lineNumber, "MergeIf only works on maps");
            }
        }
        debugger.stepStart(this, scope);
        ReflexValue retVal = new ReflexValue(ret);
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapFromObject(Object retV) {
        Map<String, Object> retObj = null;
        if (retV instanceof Map) {
            retObj = (Map<String, Object>) retV;
        } else if (retV instanceof ReflexValue) {
            ReflexValue retRV = (ReflexValue) retV;
            if (retRV.isMap()) {
                retObj = retRV.asMap();
            }
        }
        return retObj;
    }

    private void apply(Map<String, Object> ret, Map<String, Object> asMap) {
        for (Map.Entry<String, Object> entry : asMap.entrySet()) {
            if (ret.containsKey(entry.getKey())) {
                // Do an inner apply
                Map<String, Object> retObj = getMapFromObject(ret.get(entry.getKey()));
                Map<String, Object> entryObj = getMapFromObject(entry.getValue());
                if (retObj != null && entryObj != null) {
                    apply(retObj, entryObj);
                    ret.put(entry.getKey(), new ReflexValue(retObj));
                }
            } else {
                ret.put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public String toString() {
        return "mergeIf(...)";
    }
}
