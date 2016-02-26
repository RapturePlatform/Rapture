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
package rapture.script;

import rapture.common.CallingContext;
import rapture.common.RaptureScript;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.config.ConfigLoader;
import rapture.index.IndexHandler;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.script.KernelScript;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.log4j.Logger;

public abstract class BaseRaptureScript implements IRaptureScript {
    private static final String CTX = "ctx";
    private static final String EXCEPTION = "exception";
    private static final String DATA = "data";
    private static final String COLLECTOR = "collector";
    private static final String PARAMS = "params";

    protected ThreadLocal<ScriptEngine> engineRef;
    private static Logger log = Logger.getLogger(BaseRaptureScript.class);

    public BaseRaptureScript(final String engineName) {
        final ScriptEngineManager manager = new ScriptEngineManager();
        engineRef = new ThreadLocal<ScriptEngine>() {
            protected ScriptEngine initialValue() {
                return manager.getEngineByName(engineName);
            }
        };
    }

    private void addStandardContext(CallingContext ctx, ScriptEngine engine) {
        Map<String, String> standards;
        if (Kernel.isUp()) {
            standards = Kernel.getBootstrap().getScriptClasses(ContextFactory.getKernelUser());
        } else {
            standards = new HashMap<String, String>();
        }

        // Add kernel manually
        KernelScript kh = new KernelScript();
        kh.setCallingContext(ctx);
        engine.put("rk", kh);
        engine.put("cfg", ConfigLoader.getConf());

        for (Map.Entry<String, String> entry : standards.entrySet()) {
            IRaptureScriptHelper helper;
            try {
                log.info("Loading " + entry.getValue() + " into context as " + entry.getKey());
                Class<?> x = Class.forName(entry.getValue());
                Object y = x.newInstance();
                helper = (IRaptureScriptHelper) y;
                helper.setCallingContext(ctx);
                helper.setKernelScriptHelper(kh);
                engine.put(entry.getKey(), helper);
                Kernel.getKernel().getStat().registerRunScript();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                generateError(e);
            }
        }
    }

    private void generateError(Exception e) {
        log.error(e.getMessage());
        Kernel.writeAuditEntry(EXCEPTION, 2, e.getMessage());
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage(), e);
    }

    protected abstract CompiledScript getFilterScript(ScriptEngine engine, RaptureScript script) throws ScriptException;

    protected abstract CompiledScript getIndexScript(ScriptEngine engine, IndexHandler indexHandler, CallingContext context, RaptureScript script)
            throws ScriptException;

    protected abstract CompiledScript getMapScript(ScriptEngine engine, RaptureScript script) throws ScriptException;

    protected abstract CompiledScript getOperationScript(ScriptEngine engine, RaptureScript script) throws ScriptException;

    protected abstract CompiledScript getProgramScript(ScriptEngine engine, RaptureScript script) throws ScriptException;

    @Override
    public boolean runFilter(CallingContext context, RaptureScript script, RaptureDataContext data, Map<String, Object> parameters) {
        try {
            ScriptEngine engine = engineRef.get();
            CompiledScript cScript = getFilterScript(engine, script);
            engine.put(PARAMS, parameters);
            engine.put(DATA, JacksonUtil.getHashFromObject(data));
            Kernel.getKernel().getStat().registerRunScript();
            return (Boolean) cScript.eval();
        } catch (ScriptException e) {
            Kernel.writeAuditEntry(EXCEPTION, 2, e.getMessage());
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error running script " + script.getName(), e);
        }
    }

    @Override
    public void runIndexEntry(CallingContext context, RaptureScript script, IndexHandler indexHandler, RaptureDataContext data) {
        try {
            ScriptEngine engine = engineRef.get();
            CompiledScript cScript = getIndexScript(engine, indexHandler, context, script);
            engine.put(PARAMS, "");
            engine.put(COLLECTOR, indexHandler);
            engine.put(DATA, JacksonUtil.getHashFromObject(data));
            cScript.eval();
            Kernel.getKernel().getStat().registerRunScript();

        } catch (ScriptException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error running script " + script.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Object> runMap(CallingContext context, RaptureScript script, RaptureDataContext data, Map<String, Object> parameters) {
        try {
            ScriptEngine engine = engineRef.get();
            CompiledScript cScript = getMapScript(engine, script);
            addStandardContext(context, engine);
            engine.put(PARAMS, parameters);
            engine.put(DATA, JacksonUtil.getHashFromObject(data));
            Kernel.getKernel().getStat().registerRunScript();

            return (List<Object>) cScript.eval();
        } catch (ScriptException e) {
            Kernel.writeAuditEntry(EXCEPTION, 2, e.getMessage());
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error running script " + script.getName(), e);

        }
    }

    @Override
    public String runOperation(CallingContext context, RaptureScript script, String ctx, Map<String, Object> params) {
        // Get the script from the implementation bit, set up the helpers into
        // the context and execute...
        try {
            ScriptEngine engine = engineRef.get();
            CompiledScript cScript = getOperationScript(engine, script);
            addStandardContext(context, engine);
            engine.put(PARAMS, params);
            engine.put(CTX, ctx);
            Kernel.getKernel().getStat().registerRunScript();
            return JacksonUtil.jsonFromObject(cScript.eval());
        } catch (ScriptException e) {
            Kernel.writeAuditEntry(EXCEPTION, 2, e.getMessage());
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error running script " + script.getName(), e);
        }
    }

    @Override
    public String runProgram(CallingContext context, IActivityInfo activity, RaptureScript script, Map<String, Object> extraParams) {
        try {
            ScriptEngine engine = engineRef.get();
            CompiledScript cScript = getProgramScript(engine, script);
            addStandardContext(context, engine);
            for (Map.Entry<String, ?> entry : extraParams.entrySet()) {
                engine.put(entry.getKey(), entry.getValue());
            }
            if (Kernel.getKernel().getStat() != null) {
                Kernel.getKernel().getStat().registerRunScript();
            }
            return JacksonUtil.jsonFromObject(cScript.eval());
        } catch (ScriptException e) {
            Kernel.writeAuditEntry(EXCEPTION, 2, e.getMessage());
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error running script " + script.getName(), e);
        }
    }
}
