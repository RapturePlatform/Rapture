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
import java.util.List;

import rapture.common.InstallableKernelScript;
import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

/**
 * Call a kernel api function
 * 
 * @author amkimian
 * 
 */
public class KernelCallNode extends BaseNode {

    private String sdkName = null;
    private String areaName;
    private String fnName;

    private List<ReflexNode> params;

    public KernelCallNode(int lineNumber, IReflexHandler handler, Scope scope, String areaAndFunction, List<ReflexNode> ps) {
        super(lineNumber, handler, scope);
        String[] parts = areaAndFunction.replaceAll("#", "").split("\\.");
        if (parts == null || parts.length < 2 || parts.length > 3) {
            throw new ReflexException(lineNumber, "Bad Kernel call syntax: '" + areaAndFunction + "' !");
        }
        if (parts.length == 3) {
            this.sdkName = parts[0];
            this.areaName = parts[1];
            this.fnName = parts[2];
        } else {
            this.areaName = parts[0];
            this.fnName = parts[1];
        }
        this.params = ps == null ? new ArrayList<ReflexNode>() : ps;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        if (handler.getApi() == null) {
            throw new ReflexException(lineNumber, "There is no api handler registered");
        }

        List<ReflexValue> callParams = new ArrayList<ReflexValue>(params.size());
        // Now coerce the types...
        for (int i = 0; i < params.size(); i++) {
            ReflexValue v = params.get(i).evaluate(debugger, scope);
            callParams.add(v);
        }

        debugger.recordMessage(String.format("Calling %s.%s", areaName, fnName));

        ReflexValue retVal = new ReflexNullValue(lineNumber);
        ;

        if (sdkName == null) {
            retVal = KernelExecutor.executeFunction(lineNumber, handler.getApi(), areaName, fnName, callParams);
        } else {
            InstallableKernelScript sdkKernel = handler.getApi().getInstalledKernel(sdkName).getKernelScript();
            if (sdkKernel == null) {
                throw new ReflexException(lineNumber, "NO installed kernel named " + sdkName + " found");
            } else {
                retVal = KernelExecutor.executeFunction(lineNumber, sdkKernel, areaName, fnName, callParams);
            }
        }
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

}
