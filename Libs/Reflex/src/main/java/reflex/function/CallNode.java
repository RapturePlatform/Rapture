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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.KernelExecutor;
import reflex.node.ReflexNode;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

/**
 * Return a random number
 * 
 * @author amkimian
 * 
 */
public class CallNode extends BaseNode {

    private ReflexNode libNode;
    private ReflexNode fnNode;
    private ReflexNode paramsNode;

    public CallNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode libNode, ReflexNode fnNode, ReflexNode paramsNode) {
        super(lineNumber, handler, s);
        this.libNode = libNode;
        this.fnNode = fnNode;
        this.paramsNode = paramsNode;
    }

    @SuppressWarnings("unchecked")
    private ReflexValue convertListToReflex(List<Object> value) {
        List<Object> retConv = new ArrayList<Object>(value.size());
        for (Object x : value) {
            if (x instanceof List) {
                retConv.add(convertListToReflex((List<Object>) x));
            } else if (x instanceof Map) {
                retConv.add(convertMapToReflex((Map<String, Object>) x));
            } else if (x instanceof ReflexValue) {
                retConv.add(x);
            } else {
                retConv.add(new ReflexValue(x));
            }
        }
        return new ReflexValue(retConv);
    }

    @SuppressWarnings("unchecked")
    private ReflexValue convertMapToReflex(Map<String, Object> ret) {
        Map<String, Object> retConv = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : ret.entrySet()) {
            if (entry.getValue() instanceof Map) {
                retConv.put(entry.getKey(), convertMapToReflex((Map<String, Object>) entry.getValue()));
            } else if (entry.getValue() instanceof List) {
                retConv.put(entry.getKey(), convertListToReflex((List<Object>) entry.getValue()));
            } else if (entry.getValue() instanceof ReflexValue) {
                retConv.put(entry.getKey(), entry.getValue());
            } else {
                retConv.put(entry.getKey(), new ReflexValue(entry.getValue()));
            }
        }
        return new ReflexValue(retConv);
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);

        ReflexValue lib = libNode.evaluate(debugger, scope);
        ReflexValue fnName = fnNode.evaluate(debugger, scope);
        ReflexValue params = paramsNode.evaluate(debugger, scope);

        if (lib.isLib() && fnName.isString() && params.isMap()) {
            debugger.recordMessage("Lib call " + fnName.asString());
            Map<String, Object> validParam = KernelExecutor.convert(params.asMap());
            Map<String, Object> ret = lib.asLib().call(fnName.asString(), validParam);
            ReflexValue retVal = ret == null ? new ReflexNullValue(lineNumber) : convertMapToReflex(ret);
            debugger.stepEnd(this, retVal, scope);
            return retVal;
        } else {
            throw new ReflexException(lineNumber, "Need a lib, name and a param map");
        }
    }

    @Override
    public String toString() {
        return super.toString() + " - " + String.format("call(%s,%s,%s)", libNode, fnNode, paramsNode);
    }
}
