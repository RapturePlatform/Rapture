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

import java.util.HashMap;
import java.util.Map;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexSparseMatrixValue;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class AtomNode extends BaseNode {

    private static AtomNode ONE = new AtomNode(-1, null, null, Integer.valueOf(1));
    private static AtomNode ZERO = new AtomNode(-1, null, null, Integer.valueOf(0));

    public static AtomNode getIntegerAtom(int lineNumber, IReflexHandler handler, Scope s, String integerText) {
        if (integerText.equals("0")) {
            return ZERO;
        } else if (integerText.equals("1")) {
            return ONE;
        } else {
            return new AtomNode(lineNumber, handler, s, java.lang.Integer.parseInt(integerText));
        }
    }

    // Does this really save anything? The goal is not to constantly create
    // AtomNodes which are
    // so lightweight

    private static Map<String, AtomNode> cachedValues = new HashMap<String, AtomNode>();

    public static AtomNode getStringAtom(int lineNumber, IReflexHandler handler, Scope s, String stringVal) {
        if (cachedValues.containsKey(stringVal)) {
            return cachedValues.get(stringVal);
        } else {
            AtomNode node = new AtomNode(-1, null, null, stringVal);
            cachedValues.put(stringVal, node);
            return node;
        }
    }

    private ReflexValue value;

    public AtomNode(int lineNumber, IReflexHandler handler, Scope s) {
        super(lineNumber, handler, s);
        value = new ReflexNullValue(lineNumber);
    }
    
    public AtomNode(int lineNumber, IReflexHandler handler, Scope s, MatrixDim dim) {
    	super(lineNumber, handler, s);
    	value = new ReflexValue(new ReflexSparseMatrixValue(dim.getDimension()));
    }
    
    public AtomNode(int lineNumber, IReflexHandler handler, Scope s, Object v) {
        super(lineNumber, handler, s);
        value = (v == null) ? new ReflexNullValue(lineNumber) : new ReflexValue(lineNumber, v);
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
