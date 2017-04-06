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

import java.util.ArrayList;

import com.google.common.collect.ImmutableList;

import rapture.common.CallingContext;
import rapture.common.RapturePipelineTask;
import rapture.common.RaptureTransferObject;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.Kernel;
import rapture.kernel.Pipeline2ApiImpl;

public class TaskSubmitter {

    private static PipelineTaskStatusManager statusManager;

    private TaskSubmitter() {}
    
    static {
        statusManager = new PipelineTaskStatusManager();
    }

    public static String submitBroadcastToAll(CallingContext context, RaptureTransferObject mimeObject, String mimeType, String category) {
        RapturePipelineTask task = createTask(mimeObject, mimeType, category);
        if (Pipeline2ApiImpl.usePipeline2) {
            Kernel.getPipeline2().getTrusted().broadcastMessageToAll(context, task);
        } else {
            Kernel.getPipeline().broadcastMessageToAll(context, task);
        }
        return task.getTaskId();
    }

    public static String submitBroadcastToCategory(CallingContext context, RaptureTransferObject mimeObject, String mimeType, String category) {
        RapturePipelineTask task = createTask(mimeObject, mimeType, category);
        if (Pipeline2ApiImpl.usePipeline2) {
            Kernel.getPipeline2().broadcastMessage(context, category, JacksonUtil.jsonFromObject(task));
        } else {
            Kernel.getPipeline().broadcastMessageToCategory(context, task);
        }
        return task.getTaskId();
    }

    public static String submitLoadBalancedToCategory(CallingContext context, RaptureTransferObject mimeObject, String mimeType, String category) {
        RapturePipelineTask task = createTask(mimeObject, mimeType, category);
        statusManager.initialCreation(task);
        if (Pipeline2ApiImpl.usePipeline2) {
            Kernel.getPipeline2().broadcastMessage(context, category, JacksonUtil.jsonFromObject(task));
        } else {
            Kernel.getPipeline().broadcastMessageToCategory(context, task);
        }
        return task.getTaskId();
    }

    private static RapturePipelineTask createTask(RaptureTransferObject mimeObject, java.lang.String mimeType, String category) {
        RapturePipelineTask pTask = new RapturePipelineTask();
        if (category != null) {
            pTask.setCategoryList(ImmutableList.<String> of(category));
        } else {
            pTask.setCategoryList(new ArrayList<String>());
        }
        pTask.setPriority(1);
        pTask.initTask();

        pTask.addMimeObject(mimeObject);
        pTask.setContentType(mimeType);
        
        
        return pTask;

    }

    public static String submitReply(CallingContext context, RaptureTransferObject mimeObject, String mimeType, RapturePipelineTask originalMessage) {
        RapturePipelineTask pTask = new RapturePipelineTask();
        pTask.setStatus(originalMessage.getStatus());
        pTask.setPriority(originalMessage.getPriority());
        pTask.setCategoryList(originalMessage.getCategoryList());

        pTask.addMimeObject(mimeObject);
        pTask.setContentType(mimeType);
        if (Pipeline2ApiImpl.usePipeline2) {
            // This is the reply
            Kernel.getPipeline2().publishTask(context, originalMessage.getCategoryList().get(0), JacksonUtil.jsonFromObject(pTask), 0L, null);
        } else {
            Kernel.getPipeline().publishMessageToCategory(context, pTask);
        }
        return pTask.getTaskId();
    }

}
