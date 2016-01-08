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
package rapture.kernel.schedule;

import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import rapture.common.JobExecStatus;
import rapture.common.WorkOrderExecutionState;
import rapture.common.WorkflowJobExecDetails;

public class ExecStatusHelper {

    public static String prettyDuration(Long millis) {
        Duration duration = new Duration(millis);
        PeriodFormatter formatter = new PeriodFormatterBuilder().appendDays().appendSuffix("D ").appendHours().appendSuffix("h ").appendMinutes()
                .appendSuffix("m").toFormatter();
        return formatter.print(duration.toPeriod());
    }

    public static boolean isSuccess(WorkOrderExecutionState workOrderStatus, JobExecStatus jobStatus) {
        return jobStatus == JobExecStatus.FINISHED && ExecStatusHelper.isWorkOrderSuccess(workOrderStatus);
    }

    public static boolean isFailed(WorkOrderExecutionState workOrderStatus, JobExecStatus jobStatus) {
        return jobStatus == JobExecStatus.FAILED || workOrderStatus == WorkOrderExecutionState.ERROR || jobStatus == null;
    }

    public static boolean isOverrun(WorkflowJobExecDetails exec) {
        return exec.getOverrunMillis() > 0;
    }

    public static boolean isWorkOrderSuccess(WorkOrderExecutionState workOrderStatus) {
        return workOrderStatus == WorkOrderExecutionState.FINISHED || workOrderStatus == WorkOrderExecutionState.CANCELLED;
    }

    public static boolean isWorkOrderOk(WorkOrderExecutionState workOrderStatus, WorkflowJobExecDetails exec) {
        return isWorkOrderSuccess(workOrderStatus)
                || (!isOverrun(exec) && (workOrderStatus == WorkOrderExecutionState.NEW || workOrderStatus == WorkOrderExecutionState.ACTIVE));
    }

    public static boolean isJobOk(JobExecStatus jobStatus) {
        return jobStatus == JobExecStatus.WAITING || jobStatus == JobExecStatus.SCHEDULED || jobStatus == JobExecStatus.RUNNING;
    }

    public static boolean isOk(WorkOrderExecutionState workOrderStatus, JobExecStatus jobStatus, WorkflowJobExecDetails exec) {
        return isJobOk(jobStatus) || (jobStatus == JobExecStatus.FINISHED && isWorkOrderOk(workOrderStatus, exec));
    }

    public static boolean isLate(Long lateThreshold, WorkflowJobExecDetails current) {
        return current.getStartDate() < lateThreshold;
    }

}
