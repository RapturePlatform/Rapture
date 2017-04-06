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

import org.apache.log4j.Logger;

/**
 * Note that this class is referenced in types.api - any changes to this file should be reflected there.
 **/

public class QueueSubscriber implements QueueEventHandler {
    private static Logger log = Logger.getLogger(QueueSubscriber.class);

    // for testing - remove
    public static int xx = 0;

    String queueName;
    String subscriberId;
    TaskStatus status;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((queueName == null) ? 0 : queueName.hashCode());
        result = prime * result + ((subscriberId == null) ? 0 : subscriberId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        QueueSubscriber other = (QueueSubscriber) obj;
        if (queueName == null) {
            if (other.queueName != null) return false;
        } else if (!queueName.equals(other.queueName)) return false;
        if (subscriberId == null) {
            if (other.subscriberId != null) return false;
        } else if (!subscriberId.equals(other.subscriberId)) return false;
        return true;
    }

    public QueueSubscriber(String queueName, String subscriberId) {
        super();
        setQueueName(queueName);
        setSubscriberId(subscriberId);
    }

    @Override
    public String toString() {
        return "QueueSubscriber [queueName=" + queueName + ", subscriberId=" + subscriberId + "]";
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(String subscriberId) {
        if (!Character.isLetter(subscriberId.codePointAt(0))) {
            throw new RuntimeException("Subscriber ID must start with a letter");
        }
        this.subscriberId = subscriberId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    @Override
    public boolean handleEvent(String queueIdentifier, byte[] data) {
        if ((data == null) || (data.length == 0)) return false;
        if (status == null) status = new TaskStatus();
        status.getOutput().add(new String(data));
        return false;
    }

    @Override
    public boolean handleTask(String queueIdentifier, TaskStatus update) {
        if (update == null) return false;
        String taskId = update.getTaskId();
        if (taskId != status.getTaskId()) {
            return false;
        }
        PipelineTaskState state = update.getCurrentState();
        status.getOutput().addAll(update.getOutput());
        status.setCurrentState(state);
        return state.finished();
    }
}
