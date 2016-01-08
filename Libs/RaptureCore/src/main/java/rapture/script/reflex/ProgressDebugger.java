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
package rapture.script.reflex;

import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.script.IActivityInfo;
import reflex.ReflexAbortException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.ReflexNode;
import reflex.util.ReflexInstrumenter;
import reflex.util.function.LanguageRegistry;
import reflex.value.ReflexValue;

public class ProgressDebugger implements IReflexDebugger {
    private IActivityInfo activity;
    private int roundCount;
    private long fullCount = 0L;
    private int lastKnownLine = 0;
    private static final int REPORTEVERY = 1000;
    private ReflexInstrumenter instrumenter = new ReflexInstrumenter();

    public ProgressDebugger(IActivityInfo activity, String program) {
        this.activity = activity;
        instrumenter.setProgram(program);
        roundCount = 0;
    }

    @Override
    public boolean isDebug() {
        return false;
    }

    @Override
    public void setRegistry(LanguageRegistry functions) {
    }

    @Override
    public void setProgram(String program) {
        instrumenter.setProgram(program);
    }

    @Override
    public void stepEnd(ReflexNode node, ReflexValue value, Scope scope) {
        instrumenter.endLine(node.getLineNumber());
        roundCount++;
        fullCount++;
        if (activity != null) {
            if ((roundCount % REPORTEVERY) == 0) {
                int lineNumber = node.getLineNumber();
                reportHook(lineNumber);
                if (lineNumber == 0) {
                    lineNumber = lastKnownLine;
                } else {
                    lastKnownLine = lineNumber;
                }
                String message = String.format("R L:%d C:%d", lineNumber, fullCount);
                if (!Kernel.getActivity().updateActivity(ContextFactory.getKernelUser(), activity.getActivityId(), message, fullCount, 0L)) {
                    throw new ReflexAbortException(lineNumber, "Stopping because asked to abort");
                }
            }
        }
    }

    protected void reportHook(int timeout) {
        // do nothing -- for derived classes to overide
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
        if (activity != null) {
            if (!Kernel.getActivity().updateActivity(ContextFactory.getKernelUser(), activity.getActivityId(), message, fullCount, 0L)) {
                throw new ReflexAbortException(-1, "Stopping because asked to abort");
            }
        }
    }
}
