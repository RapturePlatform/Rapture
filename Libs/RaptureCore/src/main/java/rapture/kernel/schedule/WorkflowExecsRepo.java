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

import rapture.common.CallingContext;
import rapture.common.JobErrorAck;
import rapture.common.JobErrorAckStorage;
import rapture.common.JobErrorType;
import rapture.common.JobExecStatus;
import rapture.common.JobType;
import rapture.common.LastJobExecStorage;
import rapture.common.RaptureJob;
import rapture.common.RaptureJobExec;
import rapture.common.RaptureJobExecStorage;
import rapture.common.RaptureJobStorage;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.UpcomingJobExecStorage;
import rapture.common.WorkOrderExecutionState;
import rapture.common.WorkflowJobDetails;
import rapture.common.WorkflowJobExecDetails;
import rapture.common.dp.StepRecord;
import rapture.common.dp.WorkOrderDebug;
import rapture.common.dp.WorkerDebug;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.JsonContent;
import rapture.kernel.Kernel;
import rapture.kernel.dp.StepRecordUtil;
import rapture.repo.RepoVisitor;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * @author bardhi
 * @since 3/9/15.
 */
public class WorkflowExecsRepo {

    private static final Logger log = Logger.getLogger(WorkflowExecsRepo.class);
    private static final long OLD_EXEC_THRESHOLD_DELTA = 10L * 24 * 3600 * 1000;
    private static final long OLD_ACK_THRESHOLD_DELTA = 24L * 3600 * 1000;

    public List<WorkflowJobExecDetails> getLastWorkflowJobExecs(final CallingContext context) {
        final List<WorkflowJobExecDetails> ret = new LinkedList<WorkflowJobExecDetails>();
        Long lateThreshold = createLateThreshold();

        LastJobExecStorage.visitAll(createRepoVisitor(context, ret, lateThreshold, false));
        return ret;
    }

    private RepoVisitor createRepoVisitor(final CallingContext context, final List<WorkflowJobExecDetails> execDetailsList, final Long lateThreshold,
            final Boolean isUpcoming) {
        return new RepoVisitor() {
            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                if (!isFolder) {
                    RaptureJobExec exec = RaptureJobExecStorage.readFromJson(content);
                    if (exec.getJobType() == JobType.WORKFLOW) {
                        WorkflowJobExecDetails current = new WorkflowJobExecDetails();
                        current.setJobStatus(exec.getStatus());
                        String jobURI = exec.getJobURI();
                        current.setJobURI(jobURI);
                        Long execTime = exec.getExecTime();
                        current.setStartDate(execTime);
                        RaptureJob job = RaptureJobStorage.readByFields(jobURI);
                        if (job != null) {
                            current.setParameters(job.getParams());
                            current.setPassedParams(exec.getPassedParams());

                            String sc = job.getScriptURI();
                            String workflowURI = RaptureURI.createFromFullPath(new RaptureURI(sc, Scheme.WORKFLOW).getFullPath(), Scheme.WORKFLOW).toString();

                            current.setWorkflowURI(workflowURI);
                            current.setMaxRuntimeMinutes(job.getMaxRuntimeMinutes());
                            String additionalDetails = exec.getExecDetails();
                            if (additionalDetails != null && additionalDetails.length() > 0) {

                                WorkflowJobDetails workflowJobDetails = JacksonUtil.objectFromJson(additionalDetails, WorkflowJobDetails.class);
                                String workOrderURI = workflowJobDetails.getWorkOrderURI();

                                if (workOrderURI != null) {
                                    current.setWorkOrderURI(workOrderURI);
                                    int afterSlash = workOrderURI.lastIndexOf("/") + 1;
                                    if (workOrderURI.length() > afterSlash) {
                                        String workOrderID = workOrderURI.substring(afterSlash);
                                        current.setWorkOrderID(workOrderID);
                                    }
                                }

                                WorkOrderDebug workOrderDebug = Kernel.getDecision().getWorkOrderDebug(context, workOrderURI);
                                if (workOrderDebug != null) {
                                    WorkOrderExecutionState workOrderStatus = workOrderDebug.getOrder().getStatus();
                                    current.setWorkOrderStatus(workOrderStatus);
                                    Long lastUpdated = 0L;
                                    for (WorkerDebug workerDebug : workOrderDebug.getWorkerDebugs()) {
                                        for (StepRecord record : StepRecordUtil.getStepRecords(workerDebug.getWorker())) {
                                            Long endTime = record.getEndTime();
                                            if (endTime != null && endTime > lastUpdated) {
                                                lastUpdated = endTime;
                                            } else {
                                                Long startTime = record.getStartTime();
                                                if (startTime != null && startTime > lastUpdated) {
                                                    lastUpdated = startTime;
                                                }
                                            }
                                        }

                                    }
                                    current.setLastUpdated(lastUpdated);
                                    if (workOrderStatus == WorkOrderExecutionState.NEW || workOrderStatus == WorkOrderExecutionState.ACTIVE) {
                                        Long now = System.currentTimeMillis();
                                        if (job.getMaxRuntimeMinutes() != -1) {
                                            long totalMillis = now - execTime;
                                            long overrunMillis = totalMillis - job.getMaxRuntimeMinutes() * 60 * 1000;
                                            if (overrunMillis > 0) {
                                                current.setOverrunMillis(overrunMillis);
                                            }
                                        }
                                    }
                                }
                            }

                            WorkOrderExecutionState workOrderStatus = current.getWorkOrderStatus();
                            JobExecStatus jobStatus = current.getJobStatus();

                            if (isUpcoming) {
                                if (ExecStatusHelper.isLate(lateThreshold, current)) {
                                    current.setPrettyStatus("Late");
                                    current.setNotes(String.format("Should have started %s ago",
                                            ExecStatusHelper.prettyDuration(lateThreshold - current.getStartDate())));
                                    JobErrorAck errorAck = JobErrorAckStorage.readByFields(jobURI, execTime);
                                    current.setErrorAck(errorAck);
                                } else {
                                    current.setPrettyStatus("Not yet");
                                }
                            } else {
                                JobErrorAck errorAck = JobErrorAckStorage.readByFields(jobURI, execTime);
                                if (ExecStatusHelper.isSuccess(workOrderStatus, jobStatus)) {
                                    current.setPrettyStatus("Finished");
                                } else {
                                    current.setPrettyStatus("Running");
                                    if (ExecStatusHelper.isOk(workOrderStatus, jobStatus, current)) {
                                        current.setNotes("Still running");
                                    }
                                }
                                if (ExecStatusHelper.isFailed(workOrderStatus, jobStatus)) {
                                    if (workOrderStatus != null) {
                                        current.setPrettyStatus(workOrderStatus.toString());
                                    } else {
                                        current.setPrettyStatus("Failed to start");
                                    }
                                    if (errorAck != null && errorAck.getErrorType() == JobErrorType.FAILED) {
                                        current.setErrorAck(errorAck);
                                    }

                                } else if (ExecStatusHelper.isOverrun(current)) {
                                    current.setPrettyStatus(workOrderStatus.toString());
                                    String prettyOverrun = ExecStatusHelper.prettyDuration(current.getOverrunMillis());
                                    current.setNotes(String.format("Over by %s", prettyOverrun));

                                    if (errorAck != null && errorAck.getErrorType() == JobErrorType.OVERRUN) {
                                        current.setErrorAck(errorAck);
                                    }
                                }
                            }
                            JobErrorAck errorAck = current.getErrorAck();
                            if (errorAck != null && isExecOld(current) && isAckOld(errorAck)) {
                                if (log.isTraceEnabled()) {
                                    log.trace(String.format("Ignoring old acked exec, workflow=[%s], workorder=[%s], time=[%s]", current.getWorkflowURI(),
                                            current.getWorkOrderURI(), current.getStartDate()));
                                }
                            } else if (isOldDisabledOkJob(current, job)) {
                                if (log.isTraceEnabled()) {
                                    log.trace(String.format("Ignoring old OK workflow workflow=[%s], workorder=[%s], time=[%s]", current.getWorkflowURI(),
                                            current.getWorkOrderURI(), current.getStartDate()));
                                }
                            } else {
                                execDetailsList.add(current);
                            }
                        } else {
                            log.error(String.format("Unable to find job %s for execution %s ", jobURI, execTime));
                        }
                    }
                }
                return true;
            }

        };
    }

    /**
     * Returns true if an execution is old, the corresponding job is now disabled, and this execution suceeded
     *
     * @param exec
     * @param job
     * @return
     */
    private boolean isOldDisabledOkJob(WorkflowJobExecDetails exec, RaptureJob job) {
        boolean isOkAndNotLate =
                ExecStatusHelper.isOk(exec.getWorkOrderStatus(), exec.getJobStatus(), exec) && !ExecStatusHelper.isLate(createLateThreshold(), exec);

        boolean isSuccess = ExecStatusHelper.isSuccess(exec.getWorkOrderStatus(), exec.getJobStatus());
        boolean statusGreen = isOkAndNotLate || isSuccess;

        return !job.getActivated() && statusGreen && isExecOld(exec);
    }

    /**
     * Filter out execs execs that are too old
     *
     * @param execDetails
     * @return
     */
    private boolean isExecOld(WorkflowJobExecDetails execDetails) {
        Long timestamp;
        if (execDetails.getLastUpdated() != null) {
            timestamp = execDetails.getLastUpdated();
        } else {
            timestamp = execDetails.getStartDate();
        }
        long now = System.currentTimeMillis();
        Long earliestExecThreshold = now - OLD_EXEC_THRESHOLD_DELTA;
        return timestamp == null || timestamp < earliestExecThreshold;
    }

    /**
     * Filter out acks that are old
     *
     * @param errorAck
     * @return
     */
    private boolean isAckOld(JobErrorAck errorAck) {
        long now = System.currentTimeMillis();
        if (errorAck != null) {
            Long ackTime = errorAck.getTimestamp();
            Long earliestAckThreshold = now - OLD_ACK_THRESHOLD_DELTA;
            return ackTime == null || ackTime < earliestAckThreshold;
        } else {
            return true;
        }
    }

    public Long createLateThreshold() {
        // give the scheduler 1 minute leeway
        return System.currentTimeMillis() - 60 * 1000;
    }

    public List<WorkflowJobExecDetails> getUpcomingWorkflowJobExecs(final CallingContext context) {
        final List<WorkflowJobExecDetails> ret = new LinkedList<WorkflowJobExecDetails>();
        Long lateThreshold = createLateThreshold();

        UpcomingJobExecStorage.visitAll(createRepoVisitor(context, ret, lateThreshold, true));
        return ret;
    }
}
