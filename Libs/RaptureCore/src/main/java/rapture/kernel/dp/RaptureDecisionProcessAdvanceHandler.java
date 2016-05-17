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
package rapture.kernel.dp;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.AppStatus;
import rapture.common.AppStatusGroup;
import rapture.common.AppStatusGroupStorage;
import rapture.common.CallingContext;
import rapture.common.RapturePipelineTask;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.dp.WorkOrder;
import rapture.common.dp.WorkOrderStorage;
import rapture.common.dp.Worker;
import rapture.common.dp.WorkerExecutionState;
import rapture.common.dp.WorkerStorage;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.dp.DecisionProcessExecutor;
import rapture.dp.DecisionProcessExecutorFactory;
import rapture.dp.WorkOrderFactory;
import rapture.exchange.QueueHandler;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.pipeline.PipelineTaskStatusManager;
import rapture.log.MDCService;

/**
 * Advance a decision process to the next stage
 *
 * @author amkimian
 */
public class RaptureDecisionProcessAdvanceHandler implements QueueHandler {

    @Override
    public String toString() {
        return "RaptureDecisionProcessAdvanceHandler [statusManager=" + statusManager + "]";
    }

    private static final Logger log = Logger.getLogger(RaptureDecisionProcessAdvanceHandler.class);
    private final PipelineTaskStatusManager statusManager;

    public RaptureDecisionProcessAdvanceHandler() {
        statusManager = new PipelineTaskStatusManager();
    }

    @Override
    public boolean handleMessage(String tag, String routing, String contentType, RapturePipelineTask task) {
        log.debug("Attempting to advance a decision process");
        CallingContext kernelUser = ContextFactory.getKernelUser();
        statusManager.startRunning(task);
        Worker worker = JacksonUtil.objectFromJson(task.getContent(), Worker.class);
        DecisionProcessExecutor dpe = DecisionProcessExecutorFactory.getDefault();
        String workOrderURI = new RaptureURI(worker.getWorkOrderURI(), Scheme.WORKORDER).withoutElement().toString();
        if (Kernel.getDecision().wasCancelCalled(kernelUser, workOrderURI)) {
            Kernel.getActivity().finishActivity(worker.getCallingContext(), worker.getActivityId(), "Cancelled");

            worker.setStatus(WorkerExecutionState.CANCELLED);
            WorkerStorage.add(worker, ContextFactory.getKernelUser().getUser(), "Cancel Worker");

            String id = worker.getId();
            WorkOrder workOrder = WorkOrderFactory.loadWorkOrder(worker);
            workOrder.getPendingIds().remove(id);
            workOrder.setStatus(WorkOrderStatusUtil.computeStatus(workOrder, workOrder.getPendingIds().size() == 0));
            workOrder.setEndTime(System.currentTimeMillis());
            WorkOrderStorage.add(new RaptureURI(workOrderURI, Scheme.WORKORDER), workOrder, ContextFactory.getKernelUser().getUser(),
                    "Updating status for cancel");

            try {
                String appStatusURI = (worker.getAppStatusNameStack().isEmpty()) ? "" : worker.getAppStatusNameStack().get(0);
                log.debug("appStatusUri = " + appStatusURI);
                if (!StringUtils.isEmpty(appStatusURI)) {
                    AppStatusGroup group = getAppStatusGroup(appStatusURI, workOrderURI);
                    AppStatus appStatus = group.getIdToStatus().get(workOrderURI);
                    appStatus.setOverallStatus(workOrder.getStatus());
                    appStatus.setLastUpdated(System.currentTimeMillis());
                    AppStatusGroupStorage.add(group, ContextFactory.getKernelUser().getUser(), "WorkOrder ended");
                }
            } finally {
                Kernel.getDecision().getTrusted().releaseWorkOrderLock(kernelUser, workOrder);
            }
        } else {
            MDCService.INSTANCE.setWorkOrderMDC(worker.getWorkOrderURI(), worker.getId());
            try {
                dpe.executeStep(worker);
            } finally {
                MDCService.INSTANCE.clearWorkOrderMDC();
            }
        }
        // cancel case seems like failure, but we mark success to avoid retry
        // and such.
        statusManager.finishRunningWithSuccess(task);
        return true;
    }

    private AppStatusGroup getAppStatusGroup(String appStatusName, String workOrderURI) {
        AppStatusGroup group = AppStatusGroupStorage.readByFields(appStatusName);
        if (group == null) {
            group = new AppStatusGroup();
            group.setName(appStatusName);
        }
        AppStatus status = group.getIdToStatus().get(workOrderURI);
        if (status == null) {
            status = new AppStatus();
            status.setName(appStatusName);
            group.getIdToStatus().put(workOrderURI, status);
            status.setWorkOrderURI(workOrderURI);
        }
        return group;
    }
}
