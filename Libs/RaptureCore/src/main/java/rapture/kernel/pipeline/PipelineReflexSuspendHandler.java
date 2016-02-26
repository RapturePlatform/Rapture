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
package rapture.kernel.pipeline;

import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.RapturePipelineTask;
import rapture.common.mime.MimeReflexScriptResume;
import rapture.kernel.ContextFactory;
import reflex.IReflexSuspendHandler;
import reflex.suspend.SuspendContext;

public class PipelineReflexSuspendHandler implements IReflexSuspendHandler {
    private static final Logger log = Logger.getLogger(PipelineReflexSuspendHandler.class);
    private final PipelineTaskStatusManager statusManager;

    private int nextNodeId = 0;
    private int suspendTime = 0;
    private SuspendContext context = new SuspendContext();

    public PipelineReflexSuspendHandler() {
        statusManager = new PipelineTaskStatusManager();
    }

    @Override
    public String getNewNodeId() {
        nextNodeId++;
        return "" + nextNodeId;
    }

    @Override
    public void suspendTime(Integer suspendTimeInSeconds) {
        this.suspendTime = suspendTimeInSeconds;
    }

    @Override
    public void addResumeContext(String nodeId, String contextName, Object value) {
        context.addResumeContext(nodeId, contextName, value);
    }

    @Override
    public void addResumePoint(String nodeId) {
        context.addResumePoint(nodeId);
    }

    @Override
    public boolean containsResume(String nodeId, String contextName) {
        return context.containsResume(nodeId, contextName);
    }

    @Override
    public Object getResumeContext(String nodeId, String string) {
        return context.retrieveResumeContext(nodeId, string);
    }

    @Override
    public String getResumePoint() {
        return context.getResumePoint();
    }

    @Override
    public int getSuspendTime() {
        return suspendTime;
    }

    public void setResumeContext(Map<String, Map<String, Object>> context) {
        this.context.setContexts(context);
    }

    public Map<String, Map<String, Object>> getContext() {
        return context.getContexts();
    }

    @Override
    public boolean hasCapability() {
        return true;
    }

    public void handle(String reflexScript, Map<String, Object> parameters, String resp, RapturePipelineTask task) {
        log.info("Script was suspended, determining next steps");
        statusManager.suspendedRunning(task);
        /*
         * TODO Submit a resume Pipleline Task regardless of what happens.
         * Ideally we would schedule this if the suspendTime >
         * MAXTIMEBEFORESCHEDULE, need to fix
         */
        MimeReflexScriptResume scriptExec = new MimeReflexScriptResume();
        scriptExec.setReflexScript(reflexScript);
        scriptExec.setParameters(parameters);
        scriptExec.setResumePoint(this.getResumePoint());
        scriptExec.setContext(this.getContext());
        scriptExec.setScopeContext(resp);
        TaskSubmitter.submitReply(ContextFactory.getKernelUser(), scriptExec, MimeReflexScriptResume.getMimeType(), task);

        // TODO: this is the old code that used to create a suspended task
        // if (suspendTime < MAXTIMEBEFORESCHEDULE) {
        // } else {
        // // Submit a resume Script Schedule Task
        // log.info("Long running task, will submit via scheduler");
        // TaskSubmitter
        // .submitTaskOnScheduler(task, suspendTime, info.getReflexScript(),
        // info.getParameters(), this.getResumePoint(), this.getContext(),
        // resp);
        // }
    }

}
