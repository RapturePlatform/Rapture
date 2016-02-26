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

import java.util.List;

import org.apache.log4j.Logger;

import rapture.common.WorkOrderExecutionState;
import rapture.common.dp.WorkOrder;
import rapture.common.dp.Worker;
import rapture.kernel.Kernel;

public class WorkOrderStatusUtil {

    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger(WorkOrderStatusUtil.class);

    public static WorkOrderExecutionState computeStatus(WorkOrder workOrder, boolean finishing) {
        List<Worker> workers = Kernel.getDecision().getTrusted().getWorkers(workOrder);
        return computeStatus(workOrder, workers, finishing);
    }

    public static WorkOrderExecutionState computeStatus(WorkOrder workOrder, List<Worker> workers, boolean finishing) {
        boolean isAnyRunning = false;
        boolean isAnyCancelled = false;
        boolean isAnyFinished = false;
        boolean isAnyError = false;
        boolean isAnyReady = false;
        boolean isAnyBlocked = false;
        for (Worker worker : workers) {
            switch (worker.getStatus()) {
                case RUNNING: isAnyRunning = true; break;
                case CANCELLED: isAnyCancelled = true; break;
                case FINISHED: isAnyFinished = true; break;
                case BLOCKED: isAnyBlocked = true; break;
                case ERROR: isAnyError = true; break;
                case READY: isAnyReady = true; break;
            }
        }

        if (finishing) {
            if (isAnyError) return WorkOrderExecutionState.ERROR;
            if (isAnyCancelled) return WorkOrderExecutionState.CANCELLED;
            return WorkOrderExecutionState.FINISHED;
        } else {
            if (isAnyError) return WorkOrderExecutionState.FAILING;
            if (isAnyCancelled) return WorkOrderExecutionState.CANCELLING;
            if (isAnyRunning) return WorkOrderExecutionState.ACTIVE;
            if (isAnyBlocked) return WorkOrderExecutionState.SUSPENDED;
            if (isAnyFinished) return isAnyReady ? WorkOrderExecutionState.SUSPENDED : WorkOrderExecutionState.FINISHED;
            return WorkOrderExecutionState.NEW;
        }
    }
}
