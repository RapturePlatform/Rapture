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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import reflex.DebugLevel;
import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class NativeCallNode extends BaseNode {
    private String vName;
    private String fnName;
    private List<ReflexNode> params;

    public NativeCallNode(int lineNumber, IReflexHandler handler, Scope s, String varName, String fnName, List<ReflexNode> ps) {
        super(lineNumber, handler, s);
        this.vName = varName;
        this.fnName = fnName;
        this.params = ps == null ? new ArrayList<ReflexNode>() : ps;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);

        // Evaluate the method on the variable (using reflection)
        ReflexValue val = scope.resolve(vName);
        if (val != null) {
            return evaluate(debugger, val, scope);
        } else {
            throw new ReflexException(lineNumber, "Variable " + vName + " is undefined, cannot call native function on it.");
        }
    }

    ReflexValue evaluate(IReflexDebugger debugger, ReflexValue val, Scope scope) {
        handler.getDebugHandler().statementReached(lineNumber, DebugLevel.SPAM, "Type for native call is " + val.getTypeAsString());
        Object realVal = val.asObject();
        List<Object> evalParams = new ArrayList<Object>(params.size());
        for (ReflexNode p : params) {
            handler.getDebugHandler().statementReached(lineNumber, DebugLevel.SPAM, "Evaluating param");
            // Convert doubles to ints
            evalParams.add(p.evaluate(debugger, scope).asObject());
        }

        Method[] methods = realVal.getClass().getMethods();
        ReflexValue ret = new ReflexNullValue(lineNumber);;
        boolean foundMethod = false;
        for (Method m : methods) {
            if (m.getName().equals(fnName)) {
                // Potential candidate
                handler.getDebugHandler().statementReached(lineNumber, DebugLevel.SPAM, "Found candidate for " + fnName);
                Class<?>[] paramTypes = m.getParameterTypes();
                if (paramTypes.length != evalParams.size()) {
                    handler.getDebugHandler().statementReached(lineNumber, DebugLevel.SPAM, "Incorrect number of parameters for " + fnName);
                    continue;
                }
                for (int i = 0; i < paramTypes.length; i++) {
                    if (paramTypes[i] != evalParams.get(i).getClass() && paramTypes[i] != Object.class) {
                        handler.getDebugHandler().statementReached(lineNumber, DebugLevel.SPAM,
                                "Parameter mismatch, got " + evalParams.get(i).getClass().toString() + " and need " + paramTypes[i].toString());
                        foundMethod = false;
                        break;
                    }
                }
                // This is the one to execute
                try {
                    Object r = m.invoke(realVal, evalParams.toArray());
                    if (r instanceof Object[]) {
                        List<ReflexValue> listVal = new ArrayList<ReflexValue>();
                        Object[] rv = (Object[]) r;
                        for (Object x : rv) {
                            listVal.add(new ReflexValue(x));
                        }
                        ret = new ReflexValue(listVal);
                    } else {
                        if (r == null) {
                            ret = new ReflexNullValue(lineNumber);;
                        } else {
                            ret = new ReflexValue(r);
                        }
                    }
                    foundMethod = true;
                    break;
                } catch (Exception e) {
                    throw new ReflexException(lineNumber, "Error invoking native method", e);
                }
            }
        }
        if (!foundMethod && params.isEmpty()) {
            handler.getDebugHandler().statementReached(lineNumber, DebugLevel.INFO, "Could not find method " + fnName);
            Field[] fields = realVal.getClass().getFields();
            for (Field f : fields) {
                if (f.getName().equals(fnName)) {
                    try {
                        Object v = f.get(realVal);
                        ret = new ReflexValue(v);
                        foundMethod = true;
                        break;
                    } catch (Exception e) {
                        throw new ReflexException(lineNumber, "Error invoking native method", e);
                    }
                }
            }
        }
        if (!foundMethod) {
            throw new ReflexException(lineNumber, "Could not find method " + fnName);
        }
        debugger.stepEnd(this, ret, scope);
        return ret;
    }

}
