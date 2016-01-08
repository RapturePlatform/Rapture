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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.ReflexNode;
import reflex.value.ReflexSparseMatrixValue;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class KeysNode extends BaseNode {
    private ReflexNode expr;

    public KeysNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode expression) {
        super(lineNumber, handler, s);
        this.expr = expression;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        // Evaluate the expression - it should return a map type
        // Return the keys to this map type as a list type
        ReflexValue retVal = new ReflexNullValue(lineNumber);;
        ReflexValue val = expr.evaluate(debugger, scope);
        if (val.isMap()) {
            Set<String> keys = val.asMap().keySet();
            List<ReflexValue> keysAsList = new ArrayList<ReflexValue>(keys.size());
            for (String k : keys) {
                keysAsList.add(new ReflexValue(k));
            }
            retVal = new ReflexValue(keysAsList);
        } else if (val.isSparseMatrix()) {
        	ReflexSparseMatrixValue smv = val.asMatrix();
        	List<ReflexValue> dimArray = new ArrayList<ReflexValue>();
        	Set<ReflexValue> rowSet = smv.getRowSet();
        	List<ReflexValue> listVal = new ArrayList<ReflexValue>();
        	listVal.addAll(rowSet);
        	dimArray.add(new ReflexValue(listVal));
        	Set<ReflexValue> colSet = smv.getColumnSet();
        	listVal = new ArrayList<ReflexValue>();
        	listVal.addAll(colSet);
        	dimArray.add(new ReflexValue(listVal));
        	retVal = new ReflexValue(dimArray);
        }
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

}
