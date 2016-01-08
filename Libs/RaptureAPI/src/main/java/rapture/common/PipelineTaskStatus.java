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
package rapture.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Note that this class is referenced in types.api - any changes to this file should be reflected there.
**/

public class PipelineTaskStatus implements RaptureTransferObject {

    private String taskId;
    private String relatedTaskId = "";
    private Date creationTime;
    private Date startExecutionTime;
    private Date endExecutionTime;
    private int suspensionCount = 0;
    private List<String> output = new ArrayList<String>();
    
    /*
     * store unique states mapped to the server (hostname/IP) since different rapture instances can be handling the task at different states.
     */
    private Map<PipelineTaskState, String> states = new HashMap<PipelineTaskState, String>();
    
    private PipelineTaskState currentState = PipelineTaskState.UNKNOWN;

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public Date getStartExecutionTime() {
        return startExecutionTime;
    }

    public void setStartExecutionTime(Date startExecutionTime) {
        this.startExecutionTime = startExecutionTime;
    }

    public Date getEndExecutionTime() {
        return endExecutionTime;
    }

    public void setEndExecutionTime(Date endExecutionTime) {
        this.endExecutionTime = endExecutionTime;
    }

    public int getSuspensionCount() {
        return suspensionCount;
    }

    public void setSuspensionCount(int suspensionCount) {
        this.suspensionCount = suspensionCount;
    }

    public List<String> getOutput() {
        return output;
    }

    public void setOutput(List<String> output) {
        this.output = output;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public Map<PipelineTaskState, String> getStates() {
        return states;
    }

    public void beginCreation(String serverIdentifier) {
        creationTime = new Date();
        changeState(PipelineTaskState.SUBMITTED, serverIdentifier);
    }

    public void beginRunning(String serverIdentifier) {
        startExecutionTime = new Date();
        changeState(PipelineTaskState.RUNNING, serverIdentifier);
    }

    public void endRunning(String serverIdentifier, boolean ok) {
        endExecutionTime = new Date();
        if (ok) {
            changeState(PipelineTaskState.COMPLETED, serverIdentifier);

        } else {
            changeState(PipelineTaskState.FAILED, serverIdentifier);
        }
    }

    public String getRelatedTaskId() {
        return relatedTaskId;
    }

    public void setRelatedTaskId(String relatedTaskId) {
        this.relatedTaskId = relatedTaskId;
    }

    public void addToOutput(String line) {
        output.add(line);
    }

    public void suspended(String serverIdentifier) {
        suspensionCount++;
        changeState(PipelineTaskState.SUSPENDED, serverIdentifier);

    }
    
    private void changeState(PipelineTaskState state, String serverIdentifier) {
        states.put(state, serverIdentifier);
        setCurrentState(state);
    }

    public PipelineTaskState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(PipelineTaskState currentState) {
        this.currentState = currentState;
    }
}
