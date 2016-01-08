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
package rapture.script.js;

import rapture.common.CallingContext;
import rapture.common.RaptureScript;
import rapture.common.ScriptResult;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.index.IndexHandler;
import rapture.kernel.script.KernelScript;
import rapture.kernel.script.KernelScriptImplBase;
import rapture.script.IActivityInfo;
import rapture.script.IRaptureScript;
import rapture.script.RaptureDataContext;

import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.log4j.Logger;

public class JavaScript implements IRaptureScript {

    private static Logger log = Logger.getLogger(JavaScript.class);

    ScriptEngineManager factory = new ScriptEngineManager();

    @Override
    public boolean runFilter(CallingContext context, RaptureScript script, RaptureDataContext data, Map<String, Object> parameters) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void runIndexEntry(CallingContext context, RaptureScript script, IndexHandler indexHandler, RaptureDataContext data) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<Object> runMap(CallingContext context, RaptureScript script, RaptureDataContext data, Map<String, Object> parameters) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String runOperation(CallingContext context, RaptureScript script, String ctx, Map<String, Object> params) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String runProgram(CallingContext context, IActivityInfo activityInfo, RaptureScript script, Map<String, Object> extraVals) {

        KernelScript ks = new KernelScript();
        ks.setCallingContext(context);

        Object eval = "";
        try {
            ScriptEngine engine = factory.getEngineByName("JavaScript");
            engine.getBindings(ScriptContext.ENGINE_SCOPE).putAll(buildAPIReferences(ks));

            //It would be more efficient to use NativeObject here, but this is simple.
            String jsonParams = JacksonUtil.jsonFromObject(extraVals);
            engine.eval("var _params = " + jsonParams);

            eval = engine.eval(script.getScript());
        } catch (ScriptException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Exception executing script", e);
        }
        if (eval != null) {
            String json = JacksonUtil.jsonFromObject(eval);
            log.debug("result=" + eval + ", json=" + json);
            return json;
        }
        return "";
    }

    @Override
    public ScriptResult runProgramExtended(CallingContext context, IActivityInfo activity, RaptureScript script, Map<String, Object> params) {
        ScriptResult retVal = new ScriptResult();
        String scriptOutput = runProgram(context, activity, script, params);
        retVal.setReturnValue(scriptOutput);
        return retVal;
    }

    @Override
    public String validateProgram(CallingContext context, RaptureScript script) {
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        if (engine instanceof Compilable) {
            try {
                CompiledScript compile = ((Compilable) engine).compile(script.getScript());
            } catch (ScriptException e) {
                return e.getMessage();
            }
        }
        return "";
    }

    private Map<String, Object> buildAPIReferences(KernelScript ks) {
        Map<String, Object> retVal = new HashMap<>();
        for (Method method : KernelScript.class.getMethods()) {
            String methodName = method.getName();
            if (methodName.startsWith("get") && KernelScriptImplBase.class.isAssignableFrom(method.getReturnType())) {
                String objectName = methodName.substring(3, methodName.length()) + "API";
                objectName = Introspector.decapitalize(objectName);
                try {
                    retVal.put(objectName, method.invoke(ks, new Object[0]));
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Unable to get API: " + methodName, e);
                }
            }
        }
        return retVal;
    }

}
