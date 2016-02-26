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

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;

/**
 * A quoted string can have parameters in built (e.g. ${var} ) which should get expanded out to 
 * the content of the variable var in the current scope.
 * 
 * @author amkimian
 *
 */
public class QuotedStringNode extends BaseNode {

    private String value;
    private ReflexNode valueNode = null;

    public QuotedStringNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode valueNode) {
        super(lineNumber, handler, s);
        this.valueNode = valueNode;
        this.value = null;
    }
    public QuotedStringNode(int lineNumber, IReflexHandler handler, Scope s, String v) {
        super(lineNumber, handler, s);
        this.value = v;
        this.valueNode = null;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        // Here we would do the parsing in the scope
        if (valueNode != null) {
            value = valueNode.evaluate(debugger, scope).asString();
        }
        StringBuilder sb = new StringBuilder();
        boolean done = false;
        int fromPoint = 0;
        while(!done) {
            int pos = value.indexOf("${", fromPoint);
            if (pos == -1) {
                done = true;
                sb.append(value.substring(fromPoint));
            } else {
                int endPos = value.indexOf("}", pos);
                if (endPos == -1) {
                    done = true;
                    sb.append(value.substring(fromPoint));
                } else {
                    sb.append(value.substring(fromPoint, pos));
                    String variable = value.substring(pos+2, endPos);
                    ReflexValue rVal = scope.resolve(variable);
                    if (rVal == null) {
                        sb.append("null");
                    } else {
                        sb.append(rVal.asString());
                    }
                    fromPoint = endPos+1;
                }
            }
        }
        return new ReflexValue(sb.toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
