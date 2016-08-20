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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.RaptureJobExec;
import rapture.common.RapturePipelineTask;
import rapture.common.RaptureScript;
import rapture.common.exception.ExceptionToString;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.mime.MimeScheduleReflexScriptRef;
import rapture.exchange.QueueHandler;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.schedule.ScheduleManager;
import rapture.script.IActivityInfo;
import rapture.script.reflex.ReflexRaptureScript;

// Handles application/vnd.rapture.reflex.script

public class RaptureScheduleReflexScriptRefHandler implements QueueHandler {

    @Override
    public String toString() {
        return "RaptureScheduleReflexScriptRefHandler [statusManager=" + statusManager + "]";
    }

    private static final Logger log = Logger.getLogger(RaptureScheduleReflexScriptRefHandler.class);
    private final PipelineTaskStatusManager statusManager;

    public RaptureScheduleReflexScriptRefHandler() {
        statusManager = new PipelineTaskStatusManager();
    }

    @Override
    public boolean handleMessage(String tag, String routing, String contentType, RapturePipelineTask task) {
        String content = task.getContent();
        log.info("Attempting to run linked Reflex Script with task id " + task.getTaskId() + " and then update scheduler...");
        final MimeScheduleReflexScriptRef info = JacksonUtil.objectFromJson(content, MimeScheduleReflexScriptRef.class);
        statusManager.startRunning(task);
        RaptureScript script = Kernel.getScript().getScript(ContextFactory.getKernelUser(), info.getScriptURI());

        ReflexRaptureScript reflex = new ReflexRaptureScript();
        final String activityId = Kernel.getActivity().createActivity(ContextFactory.getKernelUser(), task.getTaskId(), "Running schedule script", 1L, 100L);
        try {
            Object resp = reflex.runProgram(ContextFactory.getKernelUser(), new IActivityInfo() {

                @Override
                public String getActivityId() {
                    return activityId;
                }

                @Override
                public String getOtherId() {
                    return info.getScriptURI();
                }

            }, script, info.getParameters());
            if (resp != null) {
                log.info("Reflex script returned " + resp.toString());
            }
            statusManager.finishRunningWithSuccess(task);
            // And update job using the schedule manager
            RaptureJobExec exec = Kernel.getSchedule().getNextExec(ContextFactory.getKernelUser(), info.getJobURI());
            Map<String, String> updatedVars = new HashMap<String, String>();
            for (Map.Entry<String, Object> entry : info.getParameters().entrySet()) {
                updatedVars.put(entry.getKey(), entry.getValue().toString());
            }
            exec.setPassedParams(updatedVars);
            ScheduleManager.handleJobExecutionCompleted(exec);
        } catch (Exception e) {
            log.error("Error when running Reflex script:\n" + ExceptionToString.format(e));
            statusManager.finishRunningWithFailure(task);
            RaptureJobExec exec = Kernel.getSchedule().getNextExec(ContextFactory.getKernelUser(), info.getJobURI());
            ScheduleManager.handleJobExecutionFailed(exec);
        } finally {
            Kernel.getActivity().finishActivity(ContextFactory.getKernelUser(), activityId, "Ran schedule script");
        }
        return true;
    }

}
