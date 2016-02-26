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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.ReflexNode;
import reflex.value.ReflexSparseMatrixValue;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

/**
 * The merge node merges two maps, overwriting the first values keys if they are
 * in the second (see merge if for alternate behavior). Also works with sparse matrix as well
 * 
 * @author amkimian
 * 
 */
public class MergeNode extends BaseNode {
    private List<ReflexNode> exprList;

    public MergeNode(int lineNumber, IReflexHandler handler, Scope scope, List<ReflexNode> eList) {
        super(lineNumber, handler, scope);
        this.exprList = eList;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        ReflexValue firstValue = exprList.get(0).evaluate(debugger, scope);
        debugger.stepStart(this, scope);
        ReflexValue retVal = new ReflexNullValue(lineNumber);
        if (firstValue.isMap()) {
            retVal = mergeMap(firstValue, exprList.subList(1, exprList.size()), debugger, scope);
        } else if (firstValue.isSparseMatrix()) {
            retVal = mergeMatrix(firstValue, exprList.subList(1, exprList.size()), debugger, scope);
        }

        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    private ReflexValue mergeMatrix(ReflexValue first, List<ReflexNode> later, IReflexDebugger debugger, Scope scope) {
        ReflexSparseMatrixValue val = new ReflexSparseMatrixValue(2);
        val.merge(first.asMatrix());
        for(ReflexNode n : later) {
            ReflexValue value = n.evaluate(debugger, scope);
            if (value.isSparseMatrix()) {
                val.merge(value.asMatrix());
            } else {
                throw new ReflexException(lineNumber, "Merge only works on maps and sparse matrix");               
            }
        }
        return new ReflexValue(val);
    }
    
    private ReflexValue mergeMap(ReflexValue first, List<ReflexNode> later, IReflexDebugger debugger, Scope scope) {
        Map<String, Object> ret = new LinkedHashMap<String, Object>();
        applyMap(ret, first.asMap());
        for (ReflexNode n : later) {
            ReflexValue value = n.evaluate(debugger, scope);
            if (value.isMap()) {
                applyMap(ret, value.asMap());                
            } else {
                throw new ReflexException(lineNumber, "Merge only works on maps and sparse matrix");
            }
        }
        return new ReflexValue(ret);
    }
    
    @SuppressWarnings("unchecked")
    private void applyMap(Map<String, Object> ret, Map<String, Object> asMap) {
        for (Map.Entry<String, Object> entry : asMap.entrySet()) {
            Object v = entry.getValue();
            boolean innerApply = false;
            Map<String, Object> innerMap = null;
            if (ret.containsKey(entry.getKey())) {
                if (v instanceof Map) {
                    innerApply = true;
                    innerMap = (Map<String, Object>) v;
                } else if (v instanceof ReflexValue) {
                    ReflexValue rv = (ReflexValue) v;
                    if (rv.isMap()) {
                        innerApply = true;
                        innerMap = rv.asMap();
                    }
                }
            }
            if (innerApply) {
                Object retV = ret.get(entry.getKey());
                Map<String, Object> innerRet = null;
                if (retV instanceof Map) {
                    innerRet = (Map<String, Object>) retV;
                } else if (retV instanceof ReflexValue) {
                    ReflexValue retRV = (ReflexValue) retV;
                    if (retRV.isMap()) {
                        innerRet = retRV.asMap();
                    }
                }
                if (innerRet != null) {
                    applyMap(innerRet, innerMap);
                    ret.put(entry.getKey(), new ReflexValue(innerRet));
                } else {
                    ret.put(entry.getKey(), entry.getValue());
                }
            } else {
                ret.put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public String toString() {
        return "merge(...)";
    }
}
