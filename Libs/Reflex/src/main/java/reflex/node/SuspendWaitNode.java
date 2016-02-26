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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import rapture.common.exception.RaptureExceptionFactory;
import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexSuspendValue;
import reflex.value.internal.ReflexVoidValue;

/**
 * A suspendwait call, takes a wait time and a list of task ids. We check the
 * status of the task ids (by calling the api). If all tasks are *not*
 * completed, we suspend. If they are all completed, we continue.
 * 
 * @author amkimian
 * 
 */

public class SuspendWaitNode extends BaseNode {

    private List<ReflexNode> parameters;

    public SuspendWaitNode(int lineNumber, IReflexHandler handler, Scope s, List<ReflexNode> parameters) {
        super(lineNumber, handler, s);
        this.parameters = parameters;
    }

    @Override
    public ReflexValue evaluateWithResume(IReflexDebugger debugger, Scope scope) {
        ReflexValue ret;
        if (handler.getSuspendHandler().getResumePoint().equals(nodeId)) {
            ReflexValue suspendTime = parameters.get(0).evaluate(debugger, scope);
            handler.getSuspendHandler().addResumePoint("");
            // Now need to check again
            @SuppressWarnings("unchecked")
            List<ReflexValue> handles = (List<ReflexValue>) handler.getSuspendHandler().getResumeContext(nodeId, "handles");
            ret = performCheck(handles, suspendTime);
            debugger.stepEnd(this, ret, scope);
            return ret;
        } else {
            return evaluate(debugger, scope);
        }
    }

    private ReflexValue performCheck(List<ReflexValue> handles, ReflexValue suspendTime) {
        System.out.println("Performing status check");
        if (checkDone(handles)) {
            return new ReflexVoidValue();
        } else {
            handler.getSuspendHandler().addResumeContext(nodeId, "handles", handles);
            handler.getSuspendHandler().suspendTime(suspendTime.asInt());
            handler.getSuspendHandler().addResumePoint(nodeId);
            return new ReflexSuspendValue(lineNumber);
        }
    }

    private boolean checkDone(List<ReflexValue> handles) {
        // Check to see if these handles are all "COMPLETED"
        for (ReflexValue value : handles) {
            List<ReflexValue> apiParams = new ArrayList<ReflexValue>(1);
            apiParams.add(value);
            ReflexValue retVal = KernelExecutor.executeFunction(lineNumber, handler.getApi(), "async", "asyncStatus", apiParams);
            String stateTest = retVal.asMap().get("status").toString();
            if (stateTest.equals("COMPLETED") || stateTest.equals("FAILED")) {
                continue;
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue suspendTime = parameters.get(0).evaluate(debugger, scope);
        if (parameters.size() != 2) {
            throw RaptureExceptionFactory.create("Bad call to @wait, 2 arguments must be passed in");
        }
        ReflexValue handlesParam = parameters.get(1).evaluate(debugger, scope);
        List<ReflexValue> handles;
        if (handlesParam.isList()) {
            handles = handlesParam.asList();
        } else {
            handles = new LinkedList<ReflexValue>();
            handles.add(handlesParam);
        }

        ReflexValue ret = performCheck(handles, suspendTime);
        debugger.stepEnd(this, ret, scope);
        return ret;
    }

}
