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
package rapture.dp.metrics;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.dp.WorkOrder;
import rapture.common.dp.WorkOrderInitialArgsHash;
import rapture.common.dp.Worker;
import rapture.metrics.MetricsService;

/**
 * @author bardhi
 * @since 1/27/15.
 */
public class WorkflowMetricsService {
    private static final String WORKORDER_EXECUTION_CLASS = "WORKORDER_EXECUTION_CLASS";

    public static void startMonitoring(MetricsService metricsService, Worker worker) {
        metricsService.startMonitoring(WORKORDER_EXECUTION_CLASS, worker.getWorkOrderURI());

    }

    public static void workOrderFinished(MetricsService metricsService, WorkOrder workOrder, WorkOrderInitialArgsHash argsHash, RaptureURI jobURI,
            WorkOrderExecutionState status) {
        RaptureURI workflowURI = new RaptureURI(workOrder.getWorkflowURI(), Scheme.WORKFLOW);
        String workOrderURI = workOrder.getWorkOrderURI();

        //1. workflow metric, only workflow name + status
        String workflowMetric = WorkflowMetricsFactory.createWorkflowMetricName(workflowURI, status);
        metricsService.recordTimeDifference(WORKORDER_EXECUTION_CLASS, workOrderURI, workflowMetric);

        //2. if is job, use job uri in metric (does not include workflow)
        if (jobURI != null) {
            String jobMetric = WorkflowMetricsFactory.createJobMetricName(jobURI, status);
            metricsService.recordTimeDifference(WORKORDER_EXECUTION_CLASS, workOrderURI, jobMetric);
        }

        //3. if we have args hash value, record hash value metric too
        if (argsHash != null) {
            String argsMetric = WorkflowMetricsFactory.createWorkflowWithArgsMetric(workflowURI, status, argsHash.getHashValue());
            metricsService.recordTimeDifference(WORKORDER_EXECUTION_CLASS, workOrderURI, argsMetric);
        }

    }
}
