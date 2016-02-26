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
package reflex.importer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rapture.common.exception.RaptureExceptionFactory;
import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.NativeCallNode;
import reflex.node.ReflexNode;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

/**
 * The ImportHandler is held by the TreeWalker during Reflex execution.
 * 
 * As we import modules the ImportHandler adds them.
 * 
 * As we call imported methods the ImportHandler attempts to resolve them
 * 
 * @author amkimian
 * 
 */
public class ImportHandler {
    private IReflexHandler handler;

    private Map<String, Module> modules = new HashMap<String, Module>();

    public void setReflexHandler(IReflexHandler handler) {
        this.handler = handler;
    }

    public void addImportModule(String name, List<ReflexValue> config, IReflexDebugger debugger) {
        Module module = ModuleFactory.createModule(name, "", handler, debugger);
        module.configure(config);
        modules.put(name, module);
    }

    public void addImportModuleWithAlias(String name, String alias, List<ReflexValue> configParams, IReflexDebugger debugger) {
        Module module = ModuleFactory.createModule(name, alias, handler, debugger);
        module.configure(configParams);
        modules.put(alias, module);
    }

    private void log(String msg) {
        handler.getOutputHandler().printOutput(msg);
    }

    public ReflexValue executeImportFunction(ReflexNode node, String functionName, List<ReflexNode> params, IReflexDebugger debugger, Scope scope,
            NativeCallNode possibleNativeCallNode) {
        String[] fnParts = functionName.split("\\.");
        if (fnParts.length != 2) {
            throw new ReflexException(-1, "Cannot call module method like this");
        }
        // log("Attempting to call " + functionName);
        // log("Module name (or alias) is " + fnParts[0]);
        if (!modules.containsKey(fnParts[0])) {
            try {
                return possibleNativeCallNode.evaluate(debugger, scope);
            } catch (Exception e) {
                // No native call possible
            }
            throw new ReflexException(-1, "No such module " + fnParts[0]);
        }
        Module module = modules.get(fnParts[0]);
        List<ReflexValue> parameters = evaluateParameters(params, debugger, scope);
        if (module.handlesKeyhole()) {
            // log("Attempting keyhole call of " + fnParts[1]);
            return module.keyholeCall(fnParts[1], parameters);
        } else if (module.canUseReflection()) {
            // log("Attempting reflect call of " + fnParts[1]);
            return reflectCall(module, node, scope, fnParts[1], parameters, debugger);
        } else {
            throw new ReflexException(-1, "Unexpected module cannot handle anything");
        }
    }

    private ReflexValue reflectCall(Module module, ReflexNode node, Scope scope, String name, List<ReflexValue> parameters, IReflexDebugger debugger) {
        Method[] methods = module.getClass().getDeclaredMethods();
        for (Method m : methods) {
            // log("Testing " + m.getName());
            if (m.getName().equals(name)) {
                try {
                    Object ret = null;
                    if (m.getParameterTypes().length == 1) {
                        ret = m.invoke(module, parameters);
                    } else {
                        ret = m.invoke(module,  debugger, node, scope, parameters);
                    }
                    if (ret instanceof ReflexValue) {
                        return (ReflexValue) ret;
                    }
                } catch (InvocationTargetException e) {
                    log("Underlying exception thrown of type " + e.getTargetException().getClass().toString() + " with message "
                            + e.getTargetException().getMessage());
                    logStack(e.getTargetException());
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error with reflection call", e);
                } catch (Exception e) {
                    log("Found error of class " + e.getClass().toString());
                    throw new ReflexException(-1, "Cannot handle module invocation " + e.getMessage());
                }
            }
        }
        return new ReflexVoidValue();
    }

    private void logStack(Throwable targetException) {
        for (StackTraceElement el : targetException.getStackTrace()) {
            log(String.format("%s:%d %s", el.getFileName(), el.getLineNumber(), el.getMethodName()));
        }
    }

    private List<ReflexValue> evaluateParameters(List<ReflexNode> params, IReflexDebugger debugger, Scope scope) {
        List<ReflexValue> ret = new ArrayList<ReflexValue>(params.size());
        for (ReflexNode param : params) {
            ret.add(param.evaluate(debugger, scope));
        }
        return ret;
    }
}
