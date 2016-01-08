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
package reflex.node;

import java.util.Map;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.util.MapConverter;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class PatchNode extends BaseNode {

    private ReflexNode display;
    private ReflexNode block;
    private String varName;

    public PatchNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode display, String varName, ReflexNode block) {
        super(lineNumber, handler, s);
        this.display = display;
        this.block = block;
        this.varName = varName;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {

        debugger.stepStart(this, scope);
        ReflexValue retVal = new ReflexNullValue(lineNumber);
        String displayName = display.evaluate(debugger, scope).asString();
        Map<String, Object> pullVal = handler.getDataHandler().pullData(displayName);
        
        if (pullVal != null) {
            retVal = new ReflexValue(MapConverter.convertMap(pullVal));
        }
        
        scope.assign(varName, retVal);
        block.evaluate(debugger, scope);
        ReflexValue newValue = scope.resolve(varName);
        handler.getDataHandler().pushData(displayName, KernelExecutor.convert(newValue.asMap()));

        debugger.stepEnd(this, newValue, scope);
        return retVal;
    }

    @Override
    public String toString() {
        return String.format("patch");
    }
}
