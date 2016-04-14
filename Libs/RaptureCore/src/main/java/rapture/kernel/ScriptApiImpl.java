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
package rapture.kernel;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.common.collect.Lists;

import rapture.common.CallingContext;
import rapture.common.EntitlementSet;
import rapture.common.Messages;
import rapture.common.REPLVariable;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureParameter;
import rapture.common.RaptureParameterType;
import rapture.common.RaptureScript;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureScriptStorage;
import rapture.common.RaptureSnippet;
import rapture.common.RaptureSnippetStorage;
import rapture.common.RaptureURI;
import rapture.common.ReflexREPLSession;
import rapture.common.ReflexREPLSessionStorage;
import rapture.common.Scheme;
import rapture.common.ScriptInterface;
import rapture.common.ScriptParameter;
import rapture.common.ScriptResult;
import rapture.common.api.ScriptApi;
import rapture.common.api.ScriptingApi;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.shared.script.DeleteScriptPayload;
import rapture.common.shared.script.GetScriptPayload;
import rapture.kernel.context.ContextValidator;
import rapture.script.IActivityInfo;
import rapture.script.IRaptureScript;
import rapture.script.ScriptFactory;
import rapture.script.reflex.ReflexHandler;
import rapture.script.reflex.ReflexRaptureScript;
import rapture.util.IDGenerator;
import reflex.IReflexHandler;
import reflex.IReflexOutputHandler;
import reflex.MetaParam;
import reflex.MetaReturn;
import reflex.ReflexExecutor;
import reflex.ReflexParser;
import reflex.ReflexTreeWalker;
import reflex.Scope;
import reflex.debug.NullDebugger;
import reflex.node.ReflexNode;
import reflex.util.ValueSerializer;
import reflex.value.ReflexValue;

public class ScriptApiImpl extends KernelBase implements ScriptApi {
    private static final Logger log = Logger.getLogger(ScriptApiImpl.class);

    public ScriptApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    @Override
    public RaptureScript createScript(CallingContext context, String scriptURI, RaptureScriptLanguage language, RaptureScriptPurpose purpose, String script) {

        if (doesScriptExist(context, scriptURI)) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, String.format("Script %s already exists", scriptURI));
        } else {

            RaptureURI internalURI = new RaptureURI(scriptURI, Scheme.SCRIPT);

            RaptureScript s = new RaptureScript();
            s.setLanguage(language);
            s.setPurpose(purpose);
            s.setName(internalURI.getDocPath());
            s.setScript(script);
            s.setAuthority(internalURI.getAuthority());

            RaptureScriptStorage.add(s, context.getUser(), Messages.getString("Script.createdScript")); //$NON-NLS-1$
            return s;
        }
    }

    @Override
    public void createScriptLink(CallingContext context, String fromScriptURI, String toScriptURI) {
        if (doesScriptExist(context, fromScriptURI)) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, String.format("Script %s already exists", fromScriptURI));
        } else {

            RaptureURI internalURI = new RaptureURI(fromScriptURI, Scheme.SCRIPT);

            RaptureScript s = new RaptureScript();
            s.setLanguage(RaptureScriptLanguage.REFLEX);
            s.setPurpose(RaptureScriptPurpose.LINK);
            s.setName(internalURI.getDocPath());
            s.setScript(toScriptURI);
            s.setAuthority(internalURI.getAuthority());

            RaptureScriptStorage.add(s, context.getUser(), Messages.getString("Script.createdScript")); //$NON-NLS-1$
        }
    }

    @Override
    public void removeScriptLink(CallingContext context, String fromScriptURI) {
        RaptureScript script = getScriptNoFollowLink(fromScriptURI);
        if (script.getPurpose() == RaptureScriptPurpose.LINK) {
            deleteScript(context, fromScriptURI);
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, String.format("Script %s is not a link", fromScriptURI));
        }
    }

    @Override
    public void deleteScript(CallingContext context, String scriptURI) {
        if (scriptURI.endsWith("/")) {
            RaptureScriptStorage.removeFolder(scriptURI);
        } else {
            RaptureURI internalURI = new RaptureURI(scriptURI, Scheme.SCRIPT);
            RaptureScriptStorage.deleteByAddress(internalURI, context.getUser(), Messages.getString("Script.removedScript"));
        }
    }


    @Override
    public Boolean doesScriptExist(CallingContext context, String scriptURI) {
        return getScriptNoFollowLink(scriptURI) != null;
    }

    private RaptureScript getScriptNoFollowLink(String scriptURI) {
        RaptureURI internalURI = new RaptureURI(scriptURI, Scheme.SCRIPT);
        return RaptureScriptStorage.readByAddress(internalURI);
    }

    @Override
    public RaptureScript getScript(CallingContext context, String scriptURI) {
        RaptureScript script = getScriptNoFollowLink(scriptURI);
        if (script != null && script.getPurpose() == RaptureScriptPurpose.LINK) {
            String targetUri = script.getScript().trim();
            script = getScript(context, targetUri);
            if (script != null) {
                script.setScript(String.format("// LINKED TO %s\n%s", targetUri, script.getScript()));
            }
        }
        return script;
    }

    @Override
    public List<String> getScriptNames(CallingContext context, String scriptURI) {
        RaptureURI internalURI = new RaptureURI(scriptURI, Scheme.SCRIPT);
        final List<String> ret = new ArrayList<String>();
        List<RaptureScript> scripts = RaptureScriptStorage.readAll(internalURI.getAuthority());
        for (RaptureScript script : scripts) {
            ret.add(script.getName());
        }
        return ret;
    }

    @Override
    public RaptureScript putScript(CallingContext context, String scriptURI, RaptureScript script) {
        RaptureURI internalURI = new RaptureURI(scriptURI, Scheme.SCRIPT);
        if (internalURI.hasDocPath() && !internalURI.getDocPath().equals(script.getName()))
            throw RaptureExceptionFactory.create(
                    HttpURLConnection.HTTP_BAD_REQUEST,
                    String.format("Supplied URI " + scriptURI + " has a docPath which does not match the script name " + script.getName()));
        script.setAuthority(internalURI.getAuthority());

        RaptureScript currentlyThere = RaptureScriptStorage.readByAddress(script.getAddressURI());
        if (currentlyThere != null && currentlyThere.getPurpose() == RaptureScriptPurpose.LINK) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    String.format("You cannot write directly to a link, use %s instead", currentlyThere.getScript()));
        }

        RaptureScriptStorage.add(script, context.getUser(), Messages.getString("Script.updated")); //$NON-NLS-1$
        return script;
    }

    @Override
    public String runScript(CallingContext context, final String scriptURI, Map<String, String> params) {
        return runScriptStrategy(context, scriptURI, params, "");
    }

    @Override
    public ScriptResult runScriptExtended(CallingContext context, final String scriptURI, Map<String, String> params) {
        return runScriptStrategy(context, scriptURI, params, new ScriptResult());
    }

    @SuppressWarnings("unchecked")
    private <T> T runScriptStrategy(CallingContext context, final String scriptURI, Map<String, String> params, T retVal) {

        RaptureScript script = getScript(context, scriptURI);
        if (script == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, String.format("Script %s does not exists in repository", scriptURI));
        }
        final RaptureURI internalURI = new RaptureURI(scriptURI, Scheme.SCRIPT);
        Kernel.getStackContainer().pushStack(context, internalURI.toString());

        IRaptureScript scriptContext = ScriptFactory.getScript(script);
        Map<String, Object> extraVals = new HashMap<String, Object>();
        if (params != null) {
	        for (Map.Entry<String, String> entry : params.entrySet()) {
	            extraVals.put(entry.getKey(), entry.getValue());
	        }
        }
        Kernel.writeComment(String.format(Messages.getString("Script.running"), scriptURI)); //$NON-NLS-1$
        final String activityId = Kernel.getActivity()
                .createActivity(ContextFactory.getKernelUser(), internalURI.toString(), "Running script", 1L, 100L);

        IActivityInfo activityInfo = new IActivityInfo() {
            @Override
            public String getActivityId() {
                return activityId;
            }

            @Override
            public String getOtherId() {
                return internalURI.toString();
            }
        };

        if (retVal instanceof String) {
            retVal = (T) scriptContext.runProgram(context, activityInfo, script, extraVals);
        } else if (retVal instanceof ScriptResult) {
            retVal = (T) scriptContext.runProgramExtended(context, activityInfo, script, extraVals);
        }

        Kernel.getActivity().finishActivity(ContextFactory.getKernelUser(), activityId, "Ran script");
        Kernel.getKernel().getStat().registerRunScript();
        Kernel.getStackContainer().popStack(context);

        return retVal;
    }

    @Override
    public String checkScript(CallingContext context, String scriptURI) {
        RaptureScript script = getScript(context, scriptURI);
        IRaptureScript scriptContext = ScriptFactory.getScript(script);
        return scriptContext.validateProgram(context, script);
    }

    @Override
    public Map<String, RaptureFolderInfo> listScriptsByUriPrefix(CallingContext context, String uriPrefix, int depth) {
        RaptureURI internalUri = new RaptureURI(uriPrefix, Scheme.SCRIPT);
        Boolean getAll = false;
        String authority = internalUri.getAuthority();
        Map<String, RaptureFolderInfo> ret = new HashMap<String, RaptureFolderInfo>();

        // Schema level is special case.
        if (authority.isEmpty()) {
            --depth;
            try {
                List<RaptureFolderInfo> children = RaptureScriptStorage.getChildren("");
                for (RaptureFolderInfo child : children) {
                    if (child.getName().isEmpty()) continue;
                    String uri = "script://" + child.getName();
                    ret.put(uri, child);
                    if (depth != 0) {
                        ret.putAll(listScriptsByUriPrefix(context, uri, depth));
                    }
                }

            } catch (RaptureException e) {
                // permission denied
                log.debug("No read permission for " + uriPrefix);
            }
            return ret;
        }

        String parentDocPath = internalUri.getShortPath();
        if (parentDocPath.endsWith("/")) parentDocPath = parentDocPath.substring(0, parentDocPath.length() - 1);

        if (log.isDebugEnabled()) {
            log.debug("Loading all children from repo " + internalUri.getAuthority() + " with " + parentDocPath);
        }
        if (depth <= 0) getAll = true;

        Stack<String> parentsStack = new Stack<String>();
        parentsStack.push(parentDocPath);

        int startDepth = StringUtils.countMatches(parentDocPath, "/");

        while (!parentsStack.isEmpty()) {
            String currParentDocPath = parentsStack.pop();
            int currDepth = StringUtils.countMatches(currParentDocPath, "/") - startDepth;
            if (!getAll && currDepth >= depth) continue;

            // Make sure that you have permission to read the folder.
            try {
                GetScriptPayload requestObj = new GetScriptPayload();
                requestObj.setContext(context);
                // Note: Inconsistent
                requestObj.setScriptURI(currParentDocPath);
                ContextValidator.validateContext(context, EntitlementSet.Script_listScriptsByUriPrefix, requestObj); 

            } catch (RaptureException e) {
                // permission denied
                log.debug("No read permission on folder " + currParentDocPath);
                continue;
            }

            boolean top = currParentDocPath.isEmpty();
            List<RaptureFolderInfo> children = RaptureScriptStorage.getChildren(currParentDocPath);
            if ((children == null) || (children.isEmpty()) && (currDepth == 0) && (internalUri.hasDocPath())) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoSuchScript", internalUri.toString())); //$NON-NLS-1$
            } else {
                for (RaptureFolderInfo child : children) {
                    String childDocPath = currParentDocPath + (top ? "" : "/") + child.getName();
                    if (child.getName().isEmpty()) continue;
                    // Special case: for Scripts childDocPath includes the authority

                    RaptureURI childUri = new RaptureURI(childDocPath + (child.isFolder() ? "/" : ""), Scheme.SCRIPT);
                    ret.put(childUri.toString(), child);
                    if (child.isFolder()) {
                        parentsStack.push(childDocPath);
                    }
                }
            }

        }
        return ret;
    }

    @Override
    public List<String> deleteScriptsByUriPrefix(CallingContext context, String uriPrefix) {
        Map<String, RaptureFolderInfo> docs = listScriptsByUriPrefix(context, uriPrefix, Integer.MAX_VALUE);
        List<String> folders = new ArrayList<>();
        Set<String> notEmpty = new HashSet<>();
        List<String> removed = new ArrayList<>();
        
        DeleteScriptPayload requestObj = new DeleteScriptPayload();
        requestObj.setContext(context);
        
        folders.add(uriPrefix.endsWith("/") ? uriPrefix : uriPrefix + "/");
        for (Entry<String, RaptureFolderInfo> entry : docs.entrySet()) {
            String uri = entry.getKey();
            boolean isFolder = entry.getValue().isFolder();
            
            try {
                requestObj.setScriptUri(uri);
                if (isFolder) {
                    ContextValidator.validateContext(context, EntitlementSet.Script_deleteScriptsByUriPrefix, requestObj); 
                    folders.add(0, uri.substring(0, uri.length()-1));
                } else {
                    ContextValidator.validateContext(context, EntitlementSet.Script_deleteScript, requestObj); 
                    deleteScript(context, uri);
                    removed.add(uri);
                }
            } catch (RaptureException e) {
                // permission denied
                log.debug("Unable to delete "+uri+" : " + e.getMessage());
                int colon = uri.indexOf(":") +3;
                while (true) {
                    int slash = uri.lastIndexOf('/');
                    if (slash < colon) break;
                    uri = uri.substring(0, slash);
                    notEmpty.add(uri);
                }
            }
        }
        for (String uri : folders) {
            if (notEmpty.contains(uri)) continue;
            deleteScript(context, uri);
        }
        return removed;
    }

    /**
     * These all manipulate a ReflexREPLSession object
     */
    @Override
    public String createREPLSession(CallingContext context) {
        ReflexREPLSession session = new ReflexREPLSession();
        session.setId(IDGenerator.getUUID());
        session.setLastSeen(new Date());
        session.setVars(new ArrayList<REPLVariable>());
        ReflexREPLSessionStorage.add(session, context.getUser(), "Created session");
        return session.getId();
    }

    @Override
    public void destroyREPLSession(CallingContext context, String sessionId) {
        ReflexREPLSessionStorage.deleteByFields(sessionId, context.getUser(), "Removed session");
    }

    @Override
    public String evaluateREPL(CallingContext context, String sessionId, String line) {
        // Now for the fun bit...
        ReflexREPLSession session = ReflexREPLSessionStorage.readByFields(sessionId);
        if (session == null) {
            throw RaptureExceptionFactory.create("Unknown REPL session");
        }
        // Defreeze the scope, run it against the formatter

        // So we really need to see whether we are still adding to the current
        // line, and just add to that
        // and not parse if so.

        // Then, if the line we are executing contains a leading "def" it is the
        // definition of a function
        // and we should put everything from that to "the closing end or }" into
        // a special area of the session
        // that we can use to prefix the line (so that the functions are defined
        // for each invocation)

        String realLine;

        if (line.endsWith("\\")) {
            // Add to current line (minus the \\)
            // and return
            String newPartial = session.getPartialLine() + "\n" + line.substring(0, line.length() - 2);
            session.setPartialLine(newPartial);
            session.setLastSeen(new Date());
            ReflexREPLSessionStorage.add(session, context.getUser(), "Updated session");
            return "";
        } else {
            // See if we have a current line in the session, append this line to
            // that and then clear the
            // session current line - this is our initial new line.
            realLine = session.getPartialLine() == null || session.getPartialLine().isEmpty() ? line : session.getPartialLine() + " " + line;
            session.setPartialLine("");
            // Does this line contain a def at the start? If it is we make this
            // a functional declaration
            if (realLine.trim().startsWith("def")) {
                if (session.getFunctionDecls() == null) {
                    session.setFunctionDecls(new ArrayList<String>());
                }
                session.getFunctionDecls().add(realLine);
                session.setLastSeen(new Date());
                ReflexREPLSessionStorage.add(session, context.getUser(), "Updated session");
                return "";
            }
        }

        // If we are here, realLine is what was entered, we need to prefix it
        // with the function decls

        if (session.getFunctionDecls() != null) {
            StringBuilder realLineBuilder = new StringBuilder();
            for (String v : session.getFunctionDecls()) {
                realLineBuilder.append(v);
                realLineBuilder.append("\n");
            }
            realLineBuilder.append(realLine);
            realLine = realLineBuilder.toString();
        }

        // Now parse this line *here* for def/end pairs or def name { } - these
        // are function declarations and
        // we need to extract these out of the current line and put them
        // elsewhere (in the session)

        // Then we reform the line as being - (1) function declarations from
        // session, + this line
        // and that is what we parse and evaluate.

        try {
            IReflexHandler handler = new ReflexHandler(context);
            ReflexTreeWalker walker = ReflexExecutor.getWalkerForProgram(realLine, handler);
            walker.setReflexHandler(handler);
            assignSessionToScope(session, walker.currentScope);

            final StringBuilder sb = new StringBuilder();
            walker.getReflexHandler().setOutputHandler(new IReflexOutputHandler() {

                @Override
                public boolean hasCapability() {
                    return true;
                }

                @Override
                public void printLog(String text) {
                    sb.append(text);
                    sb.append("\n");
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
            res.evaluate(new NullDebugger(), walker.currentScope);
            session.setVars(getVarsFromScope(walker.currentScope));
            session.setLastSeen(new Date());
            ReflexREPLSessionStorage.add(session, context.getUser(), "Updated session");
            return sb.toString();
        } catch (Exception e) {
            // Don't throw this as the far end cannot handle it
            return "Unable to execute line " + e.toString();
        }
    }

    private List<REPLVariable> getVarsFromScope(Scope s) {
        List<REPLVariable> ret = new ArrayList<REPLVariable>();
        for (Map.Entry<String, ReflexValue> entry : s.retrieveVariables().entrySet()) {
            REPLVariable repl = new REPLVariable();
            repl.setName(entry.getKey());
            ReflexValue v = entry.getValue();
            repl.setSerializedVar(ValueSerializer.serialize(v));
            ret.add(repl);
        }
        return ret;
    }

    private void assignSessionToScope(ReflexREPLSession session, Scope s) {
        try {
            for (REPLVariable var : session.getVars()) {
                s.assign(var.getName(), ValueSerializer.deserialize(var.getSerializedVar()));
            }
        } catch (ClassNotFoundException e) {
            throw RaptureExceptionFactory.create("Could not determine scope variables");
        }
    }

    @Override
    public void archiveOldREPLSessions(CallingContext context, Long ageInMinutes) {
        List<RaptureFolderInfo> sessions = ReflexREPLSessionStorage.getChildren("");
        for (RaptureFolderInfo sess : sessions) {
            String sessionId = sess.getName();
            // For now, ignore the age/date issue as the session is still in
            // flux and we may have some
            // old junk in there which won't be convertable from the JSON
            // storage
            log.info("Removing " + sessionId);
            ReflexREPLSessionStorage.deleteByFields(sessionId, context.getUser(), "Removed due to age");
        }
    }

    @Override
    public RaptureSnippet createSnippet(CallingContext context, String snippetURI, String snippet) {
        RaptureSnippet rsnippet = new RaptureSnippet();
        RaptureURI internalURI = new RaptureURI(snippetURI, Scheme.SNIPPET);

        rsnippet.setName(internalURI.getDocPath());
        rsnippet.setSnippet(snippet);
        rsnippet.setAuthority(internalURI.getAuthority());
        RaptureSnippetStorage.add(rsnippet, context.getUser(), "Added snippet");
        return rsnippet;
    }

    @Override
    public List<RaptureFolderInfo> getSnippetChildren(CallingContext context, String prefix) {
        return RaptureSnippetStorage.getChildren(prefix);
    }

    @Override
    public void deleteSnippet(CallingContext context, String snippetURI) {
        RaptureURI internalURI = new RaptureURI(snippetURI, Scheme.SNIPPET);
        RaptureSnippetStorage.deleteByAddress(internalURI, context.getUser(), "Removed snippet");
    }

    @Override
    public RaptureSnippet getSnippet(CallingContext context, String snippetURI) {
        RaptureURI internalURI = new RaptureURI(snippetURI, Scheme.SNIPPET);
        return RaptureSnippetStorage.readByAddress(internalURI);
    }

    @Override
    public RaptureScript putRawScript(CallingContext context, String scriptURI, String content, String language, String purpose, List<String> param_types,
                                      List<String> param_names) {
        RaptureURI uri = new RaptureURI(scriptURI, Scheme.SCRIPT);
        RaptureScript script = new RaptureScript();
        RaptureScriptLanguage lang;
        RaptureScriptPurpose purp;
        try {
            lang = RaptureScriptLanguage.valueOf(language);
        } catch (Exception e) {
            throw RaptureExceptionFactory.create("Unknown script language: [" + language + "]");
        }
        try {
            purp = RaptureScriptPurpose.valueOf(purpose);
        } catch (Exception e) {
            throw RaptureExceptionFactory.create("Unknown script purpose: [" + purpose + "]");
        }
        if (param_names.size() != param_types.size()) {
            throw RaptureExceptionFactory.create("Paramter type list must be the same length as parameter name list");
        }
        List<RaptureParameter> parms = Lists.newArrayList();
        Iterator<String> name_iter = param_names.iterator();
        Iterator<String> type_iter = param_types.iterator();
        while (name_iter.hasNext()) {
            parms.add(makeFormalParam(type_iter.next(), name_iter.next()));
        }

        script.setAuthority(uri.getAuthority());
        script.setName(uri.getDocPath());
        script.setLanguage(lang);
        script.setPurpose(purp);
        script.setScript(content);
        script.setParameters(parms);
        RaptureScriptStorage.add(script, context.getUser(), "Update");
        return script;
    }

    private RaptureParameter makeFormalParam(String type, String name) {
        RaptureParameter result = new RaptureParameter();
        result.setName(name);
        RaptureParameterType rpt;
        try {
            rpt = RaptureParameterType.valueOf(type);
        } catch (Exception e) {
            throw RaptureExceptionFactory.create("Unknown type :" + type);
        }
        result.setParameterType(rpt);
        return result;
    }

    @Override
    public ScriptInterface getInterface(CallingContext context, String scriptURI) {
        ScriptInterface result = new ScriptInterface();
        HashMap<String, ScriptParameter> inputs = new HashMap<>();
        ScriptParameter output = new ScriptParameter();
        RaptureScript script = getScriptNoFollowLink(scriptURI);
        if (script == null) {
            log.error("ScriptApiImpl.getInterface, script not found returning NULL, scriptURI: " + scriptURI);
            return result;
        }
        ReflexRaptureScript rrs = new ReflexRaptureScript();
        try {
            if (script.getScript() == null) {
                log.error("ScriptApiImpl.getInterface, script.getScript() returning NULL, scriptURI: " + scriptURI);
                return result;
            }
            ReflexParser parser = rrs.getParser(ContextFactory.getKernelUser(), script.getScript());
            parser.parse();
            if (parser.scriptInfo == null) {
                log.error("ScriptApiImpl.getInterface, parser.scriptInfo NULL, scriptURI: " + scriptURI);
                return result;
            }
            List<MetaParam> parameters = parser.scriptInfo.getParameters();
            for (MetaParam param : parameters) {
                ScriptParameter scriptParam = new ScriptParameter();
                scriptParam.setDescription(param.getDescription());
                String type = param.getParameterType().toUpperCase();
                scriptParam.setParameterType(RaptureParameterType.valueOf(type));
                inputs.put(param.getParameterName(), scriptParam);
            }
            MetaReturn returnInfo = parser.scriptInfo.getReturnInfo();
            
                String type = returnInfo.getType().toUpperCase();
                output.setParameterType(RaptureParameterType.valueOf(type));
                output.setDescription(returnInfo.getDescription());
               
            result.setProperties(parser.scriptInfo.getProperties());
        } catch (Exception e) {
            log.error("ScriptApiImpl.getInterface, scriptURI: " + scriptURI);
            log.log(Level.ERROR, "ScriptApiImpl.getInterface, exception" + e.getMessage(), e);
            throw RaptureExceptionFactory.create("Exception while parsing script parameters: " + e);
        }
        result.setInputs(inputs);
        result.setRet(output);
        return result;
     }
}
