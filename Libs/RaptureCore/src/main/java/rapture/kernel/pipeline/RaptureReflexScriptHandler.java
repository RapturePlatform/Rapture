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
package rapture.kernel.pipeline;

import rapture.common.RapturePipelineTask;
import rapture.common.RaptureScript;
import rapture.common.exception.ExceptionToString;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.mime.MimeReflexScript;
import rapture.exchange.QueueHandler;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.script.IActivityInfo;
import rapture.script.reflex.ReflexRaptureScript;

import org.apache.log4j.Logger;

// Handles application/vnd.rapture.reflex.script

public class RaptureReflexScriptHandler implements QueueHandler {
    private static final Logger log = Logger.getLogger(RaptureReflexScriptHandler.class);
    private final PipelineTaskStatusManager statusManager;

    public RaptureReflexScriptHandler() {
        statusManager = new PipelineTaskStatusManager();
    }

    @Override
    public boolean handleMessage(String tag, String routing, String contentType, final RapturePipelineTask task) {
        String content = task.getContent();
        log.info("Attempting to run embedded Reflex Script");
        try {
            statusManager.startRunning(task);
            MimeReflexScript info = JacksonUtil.objectFromJson(content, MimeReflexScript.class);
            RaptureScript script = new RaptureScript();
            script.setScript(info.getReflexScript());
            ReflexRaptureScript reflex = new ReflexRaptureScript();
            // Allow this to run with suspend logic, pass in a Pipeline suspend
            // handler
            // that can push the resumption back to the pipeline or onto the
            // scheduler
            // , depending on the value of the suspension time
            PipelineReflexSuspendHandler suspendHandler = new PipelineReflexSuspendHandler();
            TaskReflexOutputHandler outputHandler = new TaskReflexOutputHandler(task);
            final String activityId = Kernel.getActivity().createActivity(ContextFactory.getKernelUser(), task.getTaskId(), "Running script", 1L, 100L);
            String resp = reflex.runProgramWithSuspend(ContextFactory.getKernelUser(), script, new IActivityInfo() {

                @Override
                public String getActivityId() {
                    return activityId;
                }

                @Override
                public String getOtherId() {
                    return task.getTaskId();
                }

            }, info.getParameters(), suspendHandler, outputHandler);
            Kernel.getActivity().finishActivity(ContextFactory.getKernelUser(), activityId, "Ran script");
            if (resp != null) {
                log.info(String.format("Reflex script returned '%s'", resp));
            }
            // Check to see if we suspended
            if (!suspendHandler.getResumePoint().isEmpty()) {
                suspendHandler.handle(info.getReflexScript(), info.getParameters(), resp, task);
            } else {
                statusManager.finishRunningWithSuccess(task);
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error when running Reflex script: \n" + ExceptionToString.format(e));
            statusManager.finishRunningWithFailure(task);
        }
        return true;
    }

}
