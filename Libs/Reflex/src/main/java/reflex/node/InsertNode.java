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

import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;

public class InsertNode extends BaseNode {

    private String varName;
    private ReflexNode position;
    private ReflexNode value;

    public InsertNode(int lineNumber, IReflexHandler handler, Scope s, String identifier, ReflexNode position, ReflexNode value) {
        super(lineNumber, handler, s);
        this.varName = identifier;
        this.position = position;
        this.value = value;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {

        debugger.stepStart(this, scope);
        ReflexValue valToModify = scope.resolve(varName);
        if (valToModify == null) {
            throw new ReflexException(lineNumber, "No such variable " + varName);
        }
        ReflexValue posValue = position.evaluate(debugger, scope);
        ReflexValue insertValue = value.evaluate(debugger, scope);
        if (valToModify.isList()) {
            if (!posValue.isInteger()) {
                throw new ReflexException(lineNumber, position + " is " + posValue.getTypeAsString() + " not an integer");
            }
            // Expression should resolve to an integer between 0 and sizeof(list) -1
            List<ReflexValue> brahms = valToModify.asList();
            int index = posValue.asInt();
            if (index < 0 || index > (brahms.size())) {
                throw new ReflexException(lineNumber, varName + " has " + brahms.size() + " elements - cannot insert " + index);
            }
            brahms.add(index, insertValue);
            valToModify = new ReflexValue(brahms);
        } else {
            throw new ReflexException(lineNumber, varName + " is " + valToModify.getTypeAsString() + "not a list or map");
        }

        debugger.stepEnd(this, valToModify, scope);
        return valToModify;
    }

    @Override
    public String toString() {
        return String.format("insert(%s,%s,%s)", varName, position, value);
    }
}