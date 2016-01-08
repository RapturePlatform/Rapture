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
package rapture.kernel.dp;

import rapture.common.Activity;
import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.dp.ContextVariables;
import rapture.common.dp.ExecutionContext;
import rapture.common.dp.ExecutionContextField;
import rapture.common.dp.ExecutionContextFieldStorage;
import rapture.common.dp.PassedArgument;
import rapture.common.dp.StepRecord;
import rapture.common.dp.StepRecordDebug;
import rapture.common.dp.WorkOrder;
import rapture.common.dp.WorkOrderArguments;
import rapture.common.dp.WorkOrderArgumentsStorage;
import rapture.common.dp.WorkOrderDebug;
import rapture.common.dp.WorkOrderInitialArgsHash;
import rapture.common.dp.WorkOrderInitialArgsHashStorage;
import rapture.common.dp.WorkOrderStorage;
import rapture.common.dp.Worker;
import rapture.common.dp.WorkerDebug;
import rapture.dp.InvocableUtils;
import rapture.kernel.Kernel;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author bardhi
 * @since 3/18/15.
 */
public class DpDebugReader {
    public WorkOrderDebug getWorkOrderDebug(CallingContext context, String workOrderURI) {
        WorkOrderDebug wod = new WorkOrderDebug();
        WorkOrder order = WorkOrderStorage.readByFields(workOrderURI);
        if (order == null) {
            return null;
        } else {
            wod.setOrder(order);
            // read all the ExecutionContextFields and create an ExecutionContext.
            ExecutionContext exeContext = new ExecutionContext();
            RaptureURI uri = new RaptureURI(workOrderURI, Scheme.WORKORDER);
            List<ExecutionContextField> ecfs = ExecutionContextFieldStorage.readAll(uri.getFullPath());
            HashMap<String, String> data = new HashMap<>();
            for (ExecutionContextField ecf : ecfs) {
                data.put(ecf.getVarName(), ecf.getValue());
            }
            exeContext.setData(data);
            wod.setContext(exeContext);
            List<Worker> workers = Kernel.getDecision().getTrusted().getWorkers(order);
            wod.setWorkerDebugs(getWorkerDebugs(context, workers));
            String appStatusName = InvocableUtils.getAppStatusName(workers.get(0));
            String logURI = InvocableUtils.getWorkflowAuditLog(appStatusName, workOrderURI, null);
            WorkOrderInitialArgsHash argsHash = WorkOrderInitialArgsHashStorage.readByFields(workOrderURI);
            if (argsHash != null) {
                wod.setArgsHashValue(argsHash.getHashValue());
            }
            wod.setParentJobURI(Kernel.getDecision().getContextValue(context, workOrderURI, ContextVariables.PARENT_JOB_URI));
            wod.setLogURI(logURI);

            List<PassedArgument> passedArguments = readPassedArguments(workOrderURI);
            wod.setPassedArguments(passedArguments);

            return wod;
        }
    }

    private List<WorkerDebug> getWorkerDebugs(CallingContext context, List<Worker> workers) {
        List<WorkerDebug> debugs = new LinkedList<>();
        for (Worker worker : workers) {
            //populate step record info for each worker
            WorkerDebug workerDebug = new WorkerDebug();
            workerDebug.setWorker(worker);
            workerDebug.setStepRecordDebugs(getStepRecordDebugs(context, worker));
            debugs.add(workerDebug);
        }
        return debugs;
    }

    private List<StepRecordDebug> getStepRecordDebugs(CallingContext context, Worker worker) {
        List<StepRecord> stepRecords = StepRecordUtil.getStepRecords(worker);
        List<StepRecordDebug> debugs = new LinkedList<>();
        for (StepRecord stepRecord : stepRecords) {
            StepRecordDebug debug = new StepRecordDebug();
            debug.setStepRecord(stepRecord);
            if (stepRecord.getActivityId() != null) {
                Activity activity = Kernel.getActivity().getById(context, stepRecord.getActivityId());
                debug.setActivity(activity);
            }
            debugs.add(debug);
        }
        return debugs;
    }

    private List<PassedArgument> readPassedArguments(String workOrderURI) {
        WorkOrderArguments argsWrapper = WorkOrderArgumentsStorage.readByFields(workOrderURI);
        if (argsWrapper != null) {
            return argsWrapper.getArguments();
        } else {
            return new LinkedList<>();
        }
    }
}
