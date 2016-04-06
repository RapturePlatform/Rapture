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
package rapture.script.reflex;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.apache.log4j.Logger;

import rapture.audit.AuditLog;
import rapture.common.CallingContext;
import rapture.common.RaptureScript;
import rapture.common.RaptureURI;
import rapture.common.ScriptResult;
import rapture.common.api.ScriptingApi;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.config.ConfigLoader;
import rapture.index.IndexHandler;
import rapture.kernel.Kernel;
import rapture.kernel.pipeline.PipelineReflexSuspendHandler;
import rapture.kernel.script.KernelScript;
import rapture.log.MDCService;
import rapture.script.IActivityInfo;
import rapture.script.IRaptureScript;
import rapture.script.RaptureDataContext;
import rapture.script.ScriptRunInfoCollector;
import reflex.AddingOutputReflexHandler;
import reflex.DummyReflexOutputHandler;
import reflex.IReflexHandler;
import reflex.IReflexOutputHandler;
import reflex.ReflexException;
import reflex.ReflexExecutor;
import reflex.ReflexLexer;
import reflex.ReflexParser;
import reflex.ReflexTreeWalker;
import reflex.Scope;
import reflex.debug.NullDebugger;
import reflex.node.ReflexNode;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

/**
 * Provides an execution context for Rapture Reflex Scripts
 * 
 * @author amkimian
 */
public class ReflexRaptureScript implements IRaptureScript {
    private static Logger log = Logger.getLogger(ReflexRaptureScript.class);

    private static final String EXCEPTION = "exception";
    private String auditLogUri = null;

    public void setAuditLogUri(String uri) {
        auditLogUri = uri;
    }

    private void addContextScope(ReflexTreeWalker walker, CallingContext context) {
        walker.currentScope.assign("_ctx", context == null ? new ReflexNullValue() : new ReflexValue(context));
        KernelScript kh = new KernelScript();
        kh.setCallingContext(context);
        walker.getReflexHandler().setApi(kh);
        // walker.currentScope.assign("_rk", new ReflexValue(kh));
        walker.currentScope.assign("_cfg", new ReflexValue(ConfigLoader.getConf()));
        // addStandard(walker, context, kh);
    }

    private void addObjectExtra(ReflexTreeWalker walker, Map<String, ?> extra) {

        walker.currentScope.assign("_params", new ReflexValue(extra));
    }

    private ReflexTreeWalker getParserWithStandardContext(CallingContext context, String script, Map<String, ?> extra) throws RecognitionException {
        ReflexTreeWalker walker = getStandardWalker(context, script);
        if (extra != null && !extra.isEmpty()) {
            addObjectExtra(walker, extra);
        }
        addContextScope(walker, context);
        return walker;
    }

    public ReflexParser getParser(CallingContext ctx, String script) throws RecognitionException {
        ReflexLexer lexer = new ReflexLexer();
        lexer.dataHandler = new ReflexIncludeHelper(ctx);
        lexer.setCharStream(new ANTLRStringStream(script));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ReflexParser parser = new ReflexParser(tokens);
        parser.parse();
        return parser;
    }

    private ReflexTreeWalker getStandardWalker(CallingContext ctx, String script) throws RecognitionException {
        ReflexLexer lexer = new ReflexLexer();
        lexer.dataHandler = new ReflexIncludeHelper(ctx);
        lexer.setCharStream(new ANTLRStringStream(script));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ReflexParser parser = new ReflexParser(tokens);
        CommonTree tree;
        tree = (CommonTree) parser.parse().getTree();
        CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
        ReflexTreeWalker walker = new ReflexTreeWalker(nodes, parser.languageRegistry);
        walker.setReflexHandler(new AddingOutputReflexHandler());
        walker.getReflexHandler().setOutputHandler(new SimpleCollectingOutputHandler());
        walker.getReflexHandler().setOutputHandler(new DummyReflexOutputHandler());
        walker.getReflexHandler().setDataHandler(new ReflexDataHelper(ctx));
        walker.getReflexHandler().setIOHandler(new BlobOnlyIOHandler());
        return walker;
    }

    @Override
    public boolean runFilter(CallingContext context, RaptureScript script, RaptureDataContext data, Map<String, Object> parameters) {

        // A filter is basically a program that returns true or false. No return
        // is equivalent to false
        try {
            ReflexTreeWalker walker = getParserWithStandardContext(context, script.getScript(), parameters);
            ReflexNode res = walker.walk();
            return res.evaluateWithoutScope(new NullDebugger()).asBoolean();
        } catch (RecognitionException e) {
            String message = getErrorInfo(e.getMessage(), script, e.line, e.charPositionInLine);
            Kernel.writeAuditEntry(EXCEPTION, 2, message);
            throw new ReflexException(e.line, message, e);
        }
    }

    @Override
    public void runIndexEntry(CallingContext context, RaptureScript script, IndexHandler indexHandler, RaptureDataContext data) {
        try {
            ReflexTreeWalker walker = getParserWithStandardContext(context, script.getScript(), null);
            walker.currentScope.assign("_data", new ReflexValue(JacksonUtil.getHashFromObject(data)));
            walker.currentScope.assign("_index", new ReflexValue(indexHandler));
            ReflexNode res = walker.walk();
            res.evaluateWithoutScope(new NullDebugger());
        } catch (RecognitionException e) {
            String message = getErrorInfo(e.getMessage(), script, e.line, e.charPositionInLine);
            Kernel.writeAuditEntry(EXCEPTION, 2, message);
            throw new ReflexException(e.line, message, e);
        }
    }

    @Override
    public List<Object> runMap(CallingContext context, RaptureScript script, RaptureDataContext data, Map<String, Object> parameters) {
        try {
            ReflexTreeWalker walker = getParserWithStandardContext(context, script.getScript(), parameters);
            walker.currentScope.assign("_data", new ReflexValue(JacksonUtil.getHashFromObject(data)));
            ReflexNode res = walker.walk();
            List<ReflexValue> ret = res.evaluateWithoutScope(new NullDebugger()).asList();
            List<Object> realRet = new ArrayList<Object>(ret.size());
            for (ReflexValue v : ret) {
                realRet.add(v.asObject());
            }
            return realRet;
        } catch (RecognitionException e) {
            String message = getErrorInfo(e.getMessage(), script, e.line, e.charPositionInLine);
            Kernel.writeAuditEntry(EXCEPTION, 2, message);
            throw new ReflexException(e.line, message, e);
        }
    }

    @Override
    public String runOperation(CallingContext context, RaptureScript script, String ctx, Map<String, Object> params) {
        try {
            ReflexTreeWalker walker = getParserWithStandardContext(context, script.getScript(), params);
            walker.currentScope.assign("_ctx", new ReflexValue(ctx));
            ReflexNode res = walker.walk();
            return res.evaluateWithoutScope(new NullDebugger()).toString();
        } catch (RecognitionException e) {
            String message = getErrorInfo(e.getMessage(), script, e.line, e.charPositionInLine);
            Kernel.writeAuditEntry(EXCEPTION, 2, message);
            throw new ReflexException(e.line, message, e);
        }
    }

    @Override
    public String runProgram(CallingContext context, IActivityInfo activity, RaptureScript script, Map<String, Object> extraVals) {
        return runProgram(context, activity, script, extraVals, -1);
    }

    // how many lines to show (if possible) each side of the error
    private static final int DEBUG_CONTEXT = 5;

    private String getErrorInfo(String message, RaptureScript script, int lineNum, int posInLine) {
        StringBuilder msg = new StringBuilder();
        msg.append(message).append(" in script ").append(script.getName()).append("\n");

        if (lineNum > 0) {
            String[] lines = script.getScript().split("\n");

            int start = (lineNum > DEBUG_CONTEXT) ? lineNum - DEBUG_CONTEXT : 0;
            int end = lineNum + DEBUG_CONTEXT;
            if (end > lines.length) end = lines.length;
            while (start < end) {
                String l = lines[start++];
                msg.append(start).append(": ").append(l).append("\n");
                if (start == lineNum) {
                    for (int i = -4; i < posInLine; i++)
                        msg.append("-");
                    for (int i = posInLine; i < l.length(); i++)
                        msg.append("^");
                }
            }
        }
        return msg.toString();
    }

    public String runProgram(CallingContext context, IActivityInfo activity, RaptureScript script, Map<String, Object> extraVals, int timeout) {
        if (script == null) {
            log.info("in runProgram: RaptureScript is null");
            return null;
        } else try {
            MDCService.INSTANCE.setReflexMDC(script.getName());
            ScriptRunInfoCollector collector = ScriptRunInfoCollector.createServerCollector(context, script.getAddressURI().getFullPath());
            
            log.info("Running script " + getScriptName(script));

            ScriptResult res = _doRunProgram(context, activity, script, extraVals, -1, ScriptRunInfoCollector.createServerCollector(context, "remote"));
            if (auditLogUri != null) {
                Kernel.getAudit().writeAuditEntry(context, auditLogUri, "debug", 1, collector.getBlobContent());
            }
            return res.getReturnValue();
        } catch (RecognitionException e) {
            String message = getErrorInfo(e.getMessage(), script, e.line, e.charPositionInLine);
            Kernel.writeAuditEntry(EXCEPTION, 2, message);
            throw new ReflexException(e.line, message, e);
        } catch (ReflexException e) {
            throw new ReflexException(e.getLineNumber(), getErrorInfo(e.getMessage(), script, e.getLineNumber(), 0), e);
        } finally {
            MDCService.INSTANCE.clearReflexMDC();
        }
    }

    private String getScriptName(RaptureScript script) {
        if (script.getAuthority() == null) {
            return script.getName();
        } else {
            return script.getStorageLocation().toString();
        }
    }

    @Override
    public ScriptResult runProgramExtended(CallingContext context, IActivityInfo activity, RaptureScript script, Map<String, Object> params) {
        ScriptResult res = new ScriptResult();

        try {
            res = _doRunProgram(context, activity, script, params, -1, ScriptRunInfoCollector.createServerCollector(context, "remote"));
        } catch (RecognitionException e) {
            String message = getErrorInfo(e.getMessage(), script, e.line, e.charPositionInLine);
            Kernel.writeAuditEntry(EXCEPTION, 2, message);
            throw new ReflexException(e.line, message, e);
        } catch (ReflexException e) {
            res.setInError(true);
            res.setReturnValue(getErrorInfo(e.getMessage(), script, e.getLineNumber(), 0));
            res.getOutput().add("Error when running script");
        }
        return res;
    }

    private ScriptResult _doRunProgram(CallingContext context, IActivityInfo activity, RaptureScript script, Map<String, Object> params, int timeout,
            ScriptRunInfoCollector collector) throws RecognitionException, ReflexException {
        ScriptResult res = new ScriptResult();
            ReflexTreeWalker walker = getParserWithStandardContext(context, script.getScript(), params);
            ProgressDebugger progress = (timeout > 0) ? new TimeoutReflexDebugger(activity, script.getScript(), timeout)
                    : new ProgressDebugger(activity, script.getScript());
            // Setup an alternate output handler, and a standard data handler
            walker.getReflexHandler().setDataHandler(new ReflexDataHelper(context));
            walker.getReflexHandler().setOutputHandler(new ScriptResultOutputHandler(res));
            ReflexNode execRes = walker.walk();

            ReflexExecutor.injectSystemIntoScope(walker.currentScope);

            for (Map.Entry<String, Object> val : params.entrySet()) {
                log.debug("Looking to inject " + val.getKey());
                ReflexValue v = walker.currentScope.resolve(val.getKey());
                if (v != null && v.getValue() != ReflexValue.Internal.VOID && v.getValue() != ReflexValue.Internal.NULL) {
                    log.debug("Injecting " + v.asObject() + " as " + val.getKey());
                    val.setValue(v.asObject());
                } else walker.currentScope.assign(val.getKey(), val.getValue() == null ? new ReflexNullValue() : new ReflexValue(val.getValue()));

            }
            // TODO replace this with abortable invocation
            if (timeout > 0) log.info("Warning: script is not abortable");
            ReflexValue val = execRes.evaluateWithoutScope(progress);
            progress.getInstrumenter().log();

            if (walker.getReflexHandler() instanceof AddingOutputReflexHandler) {
                AddingOutputReflexHandler aorf = (AddingOutputReflexHandler) walker.getReflexHandler();
                SimpleCollectingOutputHandler sc = aorf.getOutputHandlerLike(SimpleCollectingOutputHandler.class);
                collector.addOutput(sc.getLog());
            }
            collector.addInstrumentationLog(progress.getInstrumenter().getTextLog());
            // Now record collector
            ScriptCollectorHelper.writeCollector(context, collector);
            res.setReturnValue(val.asString());
        return res;
    }

    public String runProgramWithScope(CallingContext context, String script, Scope s) throws RecognitionException {
        IReflexHandler handler = new ReflexHandler(context);
        ReflexTreeWalker walker = ReflexExecutor.getWalkerForProgram(script, handler);
        walker.setReflexHandler(handler);

        final StringBuilder sb = new StringBuilder();
        walker.getReflexHandler().setOutputHandler(new IReflexOutputHandler() {

            @Override
            public boolean hasCapability() {
                return true;
            }

            @Override
            public void printLog(String text) {
                sb.append(text);
            }

            @Override
            public void printOutput(String text) {
                sb.append(text);
            }

            @Override
            public void setApi(ScriptingApi api) {

            }

        });
        ReflexNode res = walker.walk();
        res.evaluate(new NullDebugger(), s);
        s = walker.currentScope;
        return sb.toString();
    }

    public ReflexValue runProgram(CallingContext context, ReflexTreeWalker walker, IActivityInfo activity, Map<String, Object> extraVals,
            RaptureScript script) {
        walker.getReflexHandler().setDataHandler(new ReflexDataHelper(context));
        walker.currentScope.assign("_params", new ReflexValue(extraVals));
        ReflexNode res;
        try {
            res = walker.walk();
            return res.evaluateWithoutScope(new ProgressDebugger(activity, ""));
        } catch (RecognitionException e) {
            String message = getErrorInfo(e.getMessage(), script, e.line, e.charPositionInLine);
            Kernel.writeAuditEntry(EXCEPTION, 2, message);
            throw new ReflexException(e.line, message, e);
        }
    }

    public String runProgramWithSuspend(CallingContext context, RaptureScript script, IActivityInfo activity, Map<String, Object> extraVals,
            PipelineReflexSuspendHandler suspendHandler, IReflexOutputHandler outputHandler) {
        try {
            ScriptResult result = new ScriptResult();
            ReflexTreeWalker walker = getParserWithStandardContext(context, script.getScript(), extraVals);
            walker.getReflexHandler().setSuspendHandler(suspendHandler);
            if (outputHandler != null) {
                walker.getReflexHandler().setOutputHandler(outputHandler);
            } else {
                walker.getReflexHandler().setOutputHandler(new ScriptResultOutputHandler(result));
            }
            ReflexNode res = walker.walk();
            ProgressDebugger progress = new ProgressDebugger(activity, script.getScript());
            String scriptName = getScriptName(script);
            if (scriptName == null) {
                log.info("Running anonymous Reflex script");
            } else {
                log.info(String.format("Running script with name '%s'", scriptName));
            }
            ReflexValue retVal = res.evaluateWithoutScope(progress);
            for (Map.Entry<String, Object> val : extraVals.entrySet()) {
                ReflexValue v = walker.currentScope.resolve(val.getKey());
                if (v != null && v.getValue() != ReflexValue.Internal.VOID && v.getValue() != ReflexValue.Internal.NULL) {
                    val.setValue(v.asObject());
                }
            }
            progress.getInstrumenter().log();
            return retVal.toString();
        } catch (RecognitionException e) {
            String message = getErrorInfo(e.getMessage(), script, e.line, e.charPositionInLine);
            Kernel.writeAuditEntry(EXCEPTION, 2, message);
            throw new ReflexException(e.line, message, e);
        }
    }

    public String runProgramWithResume(CallingContext context, RaptureScript script, IActivityInfo activity, Map<String, Object> extraVals,
            PipelineReflexSuspendHandler suspendHandler, IReflexOutputHandler outputHandler, String scopeContext) {
        try {
            ReflexTreeWalker walker = getParserWithStandardContext(context, script.getScript(), extraVals);
            walker.getReflexHandler().setSuspendHandler(suspendHandler);
            walker.getReflexHandler().setOutputHandler(outputHandler);
            ReflexNode res = walker.walk();
            Scope scope = JacksonUtil.objectFromJson(scopeContext, Scope.class);
            ProgressDebugger progress = new ProgressDebugger(activity, script.getScript());
            log.info("Running script " + getScriptName(script));
            res.evaluateWithResume(progress, scope);
            for (Map.Entry<String, Object> val : extraVals.entrySet()) {
                ReflexValue v = walker.currentScope.resolve(val.getKey());
                if (v != null && v.getValue() != ReflexValue.Internal.VOID && v.getValue() != ReflexValue.Internal.NULL) {
                    val.setValue(v.asObject());
                }
            }
            progress.getInstrumenter().log();
            return JacksonUtil.jsonFromObject(res.getScope());
        } catch (RecognitionException e) {
            String message = getErrorInfo(e.getMessage(), script, e.line, e.charPositionInLine);
            Kernel.writeAuditEntry(EXCEPTION, 2, message);
            throw new ReflexException(e.line, message, e);
        }
    }

    @Override
    public String validateProgram(CallingContext context, RaptureScript script) {
        try {
            // We call this as it parses the program and throws an exception if
            // the script
            // is not parseable.
            getStandardWalker(context, script.getScript());
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage() == null) {
                return e.getClass().toString();
            } else {
                return e.getMessage();
            }
        }
        return "";
    }
}
