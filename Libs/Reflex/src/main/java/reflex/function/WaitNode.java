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
package reflex.function;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.KernelExecutor;
import reflex.node.ReflexNode;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

/**
 * We wait for a document to become available, optionally delaying and then
 * retrying a number of times
 * 
 * @author amkimian
 * 
 */
public class WaitNode extends BaseNode {

    private ReflexNode displayNameNode;
    private ReflexNode intervalNode;
    private ReflexNode retryNode;

    public WaitNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode displayNameNode, ReflexNode intervalNode, ReflexNode retryNode) {
        super(lineNumber, handler, s);
        this.displayNameNode = displayNameNode;
        this.intervalNode = intervalNode;
        this.retryNode = retryNode;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        // Wait is "wait(displayName, interval, retryCount)"
        debugger.stepStart(this, scope);
        ReflexValue firstParam = displayNameNode.evaluate(debugger, scope);
        int retryCount = 0;
        long interval = 0;
        if (intervalNode != null) {
            interval = (Long) intervalNode.evaluate(debugger, scope).asLong();
        }
        if (retryNode != null) {
            retryCount = (Integer) retryNode.evaluate(debugger, scope).asInt();
        }

        System.out.println("First param is " + firstParam.getTypeAsString());
        if (firstParam.isString()) {
            String displayName = firstParam.asString();

            Map<String, Object> data = handler.getDataHandler().pullData(displayName);
            if (data == null) {
                while (retryCount > 0) {
                    debugger.recordMessage("Waitsleep");
                    try {
                        Thread.sleep(interval * 1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                    data = handler.getDataHandler().pullData(displayName);
                    retryCount--;
                    if (data != null) {
                        break;
                    }
                }
            }
            ReflexValue retVal = data == null ? new ReflexNullValue(lineNumber) : new ReflexValue(data);
            debugger.stepEnd(this, retVal, scope);
            return retVal;
        } else if (firstParam.isProcess()) {
            int exitCode = firstParam.asProcess().waitFor();
            ReflexValue retVal = new ReflexValue(exitCode);
            debugger.stepEnd(this, retVal, scope);
            return retVal;
        } else if (firstParam.isMap()) {
            ReflexValue retVal = firstParam;
            Boolean finished = false;
            do {
                Map<String, Object> theMap = retVal.asMap();
                if (theMap.containsKey("CLASS") && theMap.get("CLASS").toString().equals("rapture.common.RaptureApplicationInstance")) {
                    // Wait on an application instance to be finished
                    String serverGroup = theMap.get("serverGroup").toString();
                    String name = theMap.get("name").toString();
                    finished = (Boolean) theMap.get("finished");
                    if (!finished) {
                        try {
                            Thread.sleep(interval * 1000);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                    List<ReflexValue> apiParams = new ArrayList<ReflexValue>(2);
                    apiParams.add(new ReflexValue(name));
                    apiParams.add(new ReflexValue(serverGroup));

                    retVal = KernelExecutor.executeFunction(lineNumber, handler.getApi(), "runner", "retrieveApplicationInstance", apiParams);
                }
            } while (!finished && retryCount > 0);
            return retVal;
        }
        return new ReflexNullValue();
    }

    @Override
    public String toString() {
        return super.toString() + " - "
                + String.format("wait(%s,%s,%s)", displayNameNode, intervalNode == null ? "" : intervalNode, retryNode == null ? "" : retryNode);
    }
}
