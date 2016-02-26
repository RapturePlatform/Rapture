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
package reflex.util;

import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.ReflexNode;
import reflex.util.function.LanguageRegistry;
import reflex.value.ReflexValue;

public class InstrumentDebugger implements IReflexDebugger {
    private ReflexInstrumenter instrumenter = new ReflexInstrumenter();

    @Override
    public boolean isDebug() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setRegistry(LanguageRegistry functions) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setProgram(String program) {
        instrumenter.setProgram(program);
    }

    @Override
    public void stepEnd(ReflexNode node, ReflexValue value, Scope scope) {
        instrumenter.endLine(node.getLineNumber());
    }

    @Override
    public void stepStart(ReflexNode node, Scope scope) {
        instrumenter.startLine(node.getLineNumber());
    }

    public ReflexInstrumenter getInstrumenter() {
        return instrumenter;
    }

    @Override
    public void recordMessage(String message) {
        // Does nothing
    }

}
