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

import rapture.common.impl.jackson.MD5Utils;
import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;

// Compute the MD5 hash of the argument (when expressed as a string)

public class MD5Node extends BaseNode {

    private ReflexNode strExpr;

    public MD5Node(int lineNumber, IReflexHandler handler, Scope scope, ReflexNode strExpr) {
        super(lineNumber, handler, scope);
        this.strExpr = strExpr;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue rv = strExpr.evaluate(debugger, scope);
        String md5Hash = MD5Utils.hash16(rv.toString());
        ReflexValue retVal = new ReflexValue(md5Hash);
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    @Override
    public String toString() {
        return String.format("md5(%s)", strExpr);
    }
}
