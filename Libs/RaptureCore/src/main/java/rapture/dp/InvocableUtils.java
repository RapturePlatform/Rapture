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
package rapture.dp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.AppStatusGroup;
import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.dp.StepRecord;
import rapture.common.dp.Worker;
import rapture.common.dp.WorkerStorage;
import rapture.common.dp.Workflow;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.kernel.Kernel;
import rapture.kernel.dp.ExecutionContextUtil;
import rapture.kernel.dp.StepRecordUtil;
import rapture.util.RaptureURLCoder;

/**
 * Class containing some common utility type calls that java invocables can use
 *
 * @author alanmoore
 */
public class InvocableUtils {
    private static Logger logger = Logger.getLogger(InvocableUtils.class.getName());
    static final String WORKFLOWLOG = "//workflow/";

    public static void writeWorkflowAuditEntry(CallingContext ctx, String workerURI, String message, Boolean error) {
        Worker worker = getWorker(workerURI);
        String appStatusURI = worker.getAppStatusNameStack().get(0);
        if (appStatusURI != null && !appStatusURI.isEmpty()) {
            logger.trace("Worker URI is " + workerURI + "APPSTATUS is " + appStatusURI);
            List<StepRecord> steps = StepRecordUtil.getStepRecords(worker);
            String currStep = steps.get(steps.size() - 1).getName();
            String auditUri = getWorkflowAuditLog(appStatusURI, worker.getWorkOrderURI(), currStep);
            logger.debug(String.format("audit uri is %s, appStatus uri %s", auditUri, appStatusURI));
            Kernel.getAudit().writeAuditEntry(ctx, auditUri, error ? "ERROR" : "workflow", error ? 2 : 1, message);
        } else {
            logger.info("No app status, cannot write audit entry: " + message);
        }
    }

    /**
     * Return a worker when given a workerURI
     *
     * @param workerURI
     * @return
     */
    private static Worker getWorker(String workerURI) {
        if (workerURI == null) {
            throw RaptureExceptionFactory.create("workerURI cannot be null");
        }
        RaptureURI uri = new RaptureURI(workerURI, Scheme.WORKORDER);
        String workOrderURI = uri.toShortString();
        if (workOrderURI == null) {
            throw RaptureExceptionFactory.create(String.format("Bad workerURI '%s': cannot extract workOrderURI from it", workerURI));
        }
        String id = uri.getElement();
        if (id == null) {
            throw RaptureExceptionFactory.create(String.format("Bad workerURI '%s': cannot extract worker id from it", workerURI));
        }
        return WorkerStorage.readByFields(workOrderURI, id);
    }

    public static String getWorkflowAuditLog(String appStatusName) {
        return getWorkflowAuditLog(appStatusName, null, null);
    }

    public static String getWorkflowAuditLog(String appStatusName, String workOrderUri, String currStep) {
        StringBuilder sb = new StringBuilder(InvocableUtils.WORKFLOWLOG);
        if(!StringUtils.isEmpty(appStatusName)) {
            AppStatusGroup group = new AppStatusGroup();
            group.setName(appStatusName);
            RaptureURI internalURI = new RaptureURI(group.getStoragePath(), Scheme.DOCUMENT);
            sb.append(internalURI.getShortPath());
        }
        if(!StringUtils.isEmpty(workOrderUri)) {
            sb.append("/").append(getWorkOrderNumber(workOrderUri));
        }
        if(!StringUtils.isEmpty(currStep)) {
            sb.append("/").append(RaptureURLCoder.encode(currStep));
        }
        return sb.toString();
    }

    private static String getWorkOrderNumber(String workOrderUri) {
        int index = workOrderUri.lastIndexOf("/");
        if(index > 0) {
            return workOrderUri.substring(index + 1, workOrderUri.length());
        } else {
            return "";
        }
    }

    public static boolean updateActivity(CallingContext ctx, String workerURI, String message, Long percentComplete) {
        RaptureURI uri = new RaptureURI(workerURI, Scheme.WORKORDER);
        String workOrderURI = uri.toShortString();
        String workerId = uri.getElement();
        Worker found = WorkerStorage.readByFields(workOrderURI, workerId);
        if (found != null) {
            Kernel.getActivity().updateActivity(ctx, found.getActivityId(), message, percentComplete, 100L);
        }
        return false;
    }

    public static Map<String, String> getLocalViewOverlay(Worker worker) {
        if (!worker.getLocalView().isEmpty()) {
            Map<String, String> ret = new HashMap<String, String>();
            Map<String, String> viewOverlay = worker.getViewOverlay();
            Map<String, String> localOverlay = worker.getLocalView().get(0);
            ret.putAll(localOverlay);
            ret.putAll(viewOverlay);
            return ret;
        } else {
            return worker.getViewOverlay();
        }
    }

    public static String createAppStatusName(CallingContext context, Workflow workflow, Worker worker, String appStatusNamePattern) {
        Map<String, String> view = InvocableUtils.getLocalViewOverlay(worker);
        if (appStatusNamePattern == null || appStatusNamePattern.length() == 0) {
            appStatusNamePattern = workflow.getDefaultAppStatusNamePattern();
        }
        if (appStatusNamePattern != null && appStatusNamePattern.length() == 1) {
            logger.error("appStatusNamePattern needs to start with '%' and have length of more than 1, but is " + appStatusNamePattern);
            appStatusNamePattern = null;
        }
        // default to %authority/${$__date_string}/doc path
        if(appStatusNamePattern == null) {
            RaptureURI uri = workflow.getAddressURI();
            appStatusNamePattern = String.format("%%%s/${$__date_string}/%s", uri.getAuthority(), uri.getDocPath());
        }
        String appStatusURI = ExecutionContextUtil.evalTemplateECF(context, worker.getWorkOrderURI(), appStatusNamePattern.substring(1), view);
        appStatusURI = appStatusURI.replaceAll("^\\/*", ""); // remove leading slashes
        return appStatusURI;
    }

    public static String getAppStatusName(Worker worker) {
        if (worker == null || worker.getAppStatusNameStack().size() == 0) return null;
        return worker.getAppStatusNameStack().get(0);
    }

    public static String getWorkflowAuditUri(Worker worker) {
        String appStatusURI = getAppStatusName(worker);
        if (appStatusURI == null || appStatusURI.isEmpty()) {
            return null;
        }
        String workflowLogUri = getWorkflowAuditLog(appStatusURI);
        return workflowLogUri;
    }

    public static String getWorkflowAuditUri(String workerURI) {
        return getWorkflowAuditUri(getWorker(workerURI));
    }
}
