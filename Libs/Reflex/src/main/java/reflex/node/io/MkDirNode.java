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
package reflex.node.io;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.ReflexNode;
import reflex.value.ReflexFileValue;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class MkDirNode extends BaseNode {

    private ReflexNode fileDirExpr;

    public MkDirNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode fileDirExpr) {
        super(lineNumber, handler, s);
        this.fileDirExpr = fileDirExpr;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        // The ReflexNode evaluates to a string or a ReflexFileValue, get the
        // directory listing from that, with each
        // entry being a ReflexValue of course
        debugger.stepStart(this, scope);
        ReflexValue retVal = new ReflexNullValue(lineNumber);
        ReflexValue value = fileDirExpr.evaluate(debugger, scope);
        if (value.isString()) {
            value = new ReflexValue(new ReflexFileValue(value.asString()));
        }
        if (value.isFile()) {
            handler.getIOHandler().mkdir(value.asFile());
        }
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    @Override
    public String toString() {
        return String.format("readdir(%s)", fileDirExpr);
    }
}
