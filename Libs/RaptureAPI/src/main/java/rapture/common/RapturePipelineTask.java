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
package rapture.common;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import rapture.common.impl.jackson.JacksonUtil;
import rapture.util.IDGenerator;

/**
 * Note that this class is referenced in types.api - any changes to this file should be reflected there.
 **/

@JsonIgnoreProperties(ignoreUnknown = true)
public class RapturePipelineTask implements RaptureTransferObject {
    @Override
    public String toString() {
        return "RapturePipelineTask [status=" + status + ", taskType=" + taskType + ", priority=" + priority + ", categoryList=" + categoryList + ", taskId="
                + taskId + ", content=" + content + ", contentType=" + contentType + ", epoch=" + epoch + "]";
    }

    public PipelineTaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(PipelineTaskType taskType) {
        this.taskType = taskType;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public List<String> getCategoryList() {
        return categoryList;
    }

    public void setCategoryList(List<String> categoryList) {
        this.categoryList = categoryList;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
        if (status == null) {
            status = new PipelineTaskStatus();
        }
        status.setTaskId(taskId);
    }

    public Long getEpoch() {
        return epoch;
    }

    public void setEpoch(Long epoch) {
        this.epoch = epoch;
    }

    private PipelineTaskStatus status = new PipelineTaskStatus();
    private PipelineTaskType taskType;
    private int priority;
    private List<String> categoryList;
    private String taskId;
    private String content;
    private String contentType = "text/plain";
    private Long epoch;
    private Boolean statusEnabled = true;

    public RapturePipelineTask() {
        initTask();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void addMimeObject(RaptureTransferObject transferObject) {
        setContent(JacksonUtil.jsonFromObject(transferObject));
    }

    public PipelineTaskStatus getStatus() {
        return status;
    }

    public void setStatus(PipelineTaskStatus status) {
        this.status = status;
        if (taskId != null) {
            status.setTaskId(taskId);
        }
    }

    public void initTask() {
        status = new PipelineTaskStatus();
        this.taskId = IDGenerator.getUUID();
        status.setTaskId(taskId);
    }

    public Boolean isStatusEnabled() {
        return statusEnabled;
    }

    public void setStatusEnabled(Boolean statusEnabled) {
        this.statusEnabled = statusEnabled;
    }
}
