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
package rapture.dp.metrics;

import rapture.common.RaptureURI;
import rapture.common.WorkOrderExecutionState;
import rapture.metrics.MetricsSanitizer;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * @author bardhi
 * @since 1/27/15.
 */
public class WorkflowMetricsFactory {

    public static final String GENERIC_WORKFLOW_PREFIX = "dp.workflow.generic";

    public static String createWorkflowMetricName(RaptureURI workflowURI, WorkOrderExecutionState state) {
        String legiblePart = MetricsSanitizer.sanitizeParameterName(workflowURI.getAuthority()) + "." + MetricsSanitizer
                .sanitizeParameterName(workflowURI.getDocPath());
        String checksumPart = DigestUtils.sha256Hex(workflowURI.getShortPath());
        return String.format("%s.%s.%s.%s", GENERIC_WORKFLOW_PREFIX, state, legiblePart, checksumPart);
    }

    public static final String JOB_WORKFLOW_PREFIX = "dp.workflow.job";

    public static String createJobMetricName(RaptureURI jobURI, WorkOrderExecutionState state) {
        String legiblePart = MetricsSanitizer.sanitizeParameterName(jobURI.getAuthority()) + "." + MetricsSanitizer.sanitizeParameterName(jobURI.getDocPath());
        String checksumPart = DigestUtils.sha256Hex(jobURI.getShortPath());
        return String.format("%s.%s.%s.%s", JOB_WORKFLOW_PREFIX, state, legiblePart, checksumPart);
    }

    public static final String ARGS_WORKFLOW_PREFIX = "ephemeral.dp.workflow.argsHash";

    public static String createWorkflowWithArgsMetric(RaptureURI workflowURI, WorkOrderExecutionState state,
            String argsHashValue) {
        String legiblePart = MetricsSanitizer.sanitizeParameterName(workflowURI.getAuthority()) + "." + MetricsSanitizer
                .sanitizeParameterName(workflowURI.getDocPath());
        String checksumPart = DigestUtils.sha256Hex(workflowURI.getShortPath());
        return String.format("%s.%s.%s.%s.%s", ARGS_WORKFLOW_PREFIX, state, legiblePart, checksumPart, argsHashValue);
    }

}
