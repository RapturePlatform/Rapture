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
package reflex.node.io;

import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.ReflexNode;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

// Close a port or a file

public class CloseNode extends BaseNode {

    private ReflexNode portExpr;

    public CloseNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode portExpr) {
        super(lineNumber, handler, s);
        this.portExpr = portExpr;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue rv = portExpr.evaluate(debugger, scope);
        if (rv.isPort()) {
            handler.getPortHandler().close(rv.asPort().getPortName());
        } else if (rv.isArchive()) {
            handler.getIOHandler().close(rv.asArchive());
        } else {
            throw new ReflexException(lineNumber, "Cannot close a non-port");
        }
        debugger.stepEnd(this, new ReflexVoidValue(lineNumber), scope);
        return new ReflexVoidValue();
    }

    @Override
    public String toString() {
        return String.format("close(%s)", portExpr);
    }
}
