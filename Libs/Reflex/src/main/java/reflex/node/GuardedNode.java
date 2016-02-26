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
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

public class GuardedNode extends BaseNode {

    private ReflexNode tryBlock;
    private ReflexNode catchBlock;
    private String exceptionId;

    public GuardedNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode tryBlock, String exceptionId, ReflexNode catchBlock) {
        super(lineNumber, handler, s);
        this.tryBlock = tryBlock;
        this.catchBlock = catchBlock;
        this.exceptionId = exceptionId;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        // Call the tryBlock, catching any exception which then gets pushed to
        // the catchBlock
        ReflexValue ret = new ReflexVoidValue(lineNumber);
        try {
            ret = tryBlock.evaluate(debugger, scope);
        } catch (ReflexException e) {
            Scope s = catchBlock.getScope();
            if (e.getValue() != null) {
                s.assign(exceptionId, e.getValue());
            } else {
                s.assign(exceptionId, new ReflexValue(e));
            }
            ret = catchBlock.evaluateWithoutScope(debugger);
        } catch (Exception e) {
            Scope s = catchBlock.getScope();
            s.assign(exceptionId, new ReflexValue(e));
            ret = catchBlock.evaluateWithoutScope(debugger);
        }
        debugger.stepEnd(this, ret, scope);
        return ret;
    }
}
