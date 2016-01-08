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

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.ReflexNode;
import reflex.value.ReflexValue;

/**
 * The difference node returns those elements that are not common in the
 * parameters
 * 
 * @author amkimian
 * 
 */
public class JoinNode extends BaseNode {
    private List<ReflexNode> exprList;

    public JoinNode(int lineNumber, IReflexHandler handler, Scope scope, List<ReflexNode> eList) {
        super(lineNumber, handler, scope);
        this.exprList = eList;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        // Join('a','b','c') == 'abc')
        // join(1,2,3) = [ 1 2 3];

        StringBuilder posRes = new StringBuilder();
        List<ReflexValue> possibleList = new ArrayList<ReflexValue>();
        boolean stringsOk = true;
        for (ReflexNode p : exprList) {
            ReflexValue v = p.evaluate(debugger, scope);
            stringsOk = addValue(v, stringsOk, posRes, possibleList);

        }
        ReflexValue retVal = new ReflexValue(stringsOk ? posRes.toString() : possibleList);
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    private boolean addValue(ReflexValue v, boolean stringsOk, StringBuilder posRes, List<ReflexValue> possibleList) {
        if (stringsOk) {
            if (v.isString()) {
                posRes.append(v.asString());
            } else if (v.isList()) {
                List<ReflexValue> lval = v.asList();
                for (ReflexValue l : lval) {
                    stringsOk = addValue(l, stringsOk, posRes, possibleList);
                }
            } else {
                stringsOk = false;
            }
        }
        possibleList.add(v);
        return stringsOk;
    }

    @Override
    public String toString() {
        return "join(...)";
    }
}
