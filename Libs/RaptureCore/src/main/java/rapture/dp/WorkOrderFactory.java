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

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.dp.WorkOrder;
import rapture.common.dp.WorkOrderStorage;
import rapture.common.dp.Worker;
import rapture.common.exception.RaptureExceptionFactory;

public class WorkOrderFactory {

    public static WorkOrder loadWorkOrder(Worker worker) {
        WorkOrder workOrder = WorkOrderStorage.readByAddress(new RaptureURI(worker.getWorkOrderURI(), Scheme.WORKORDER));
        return workOrder;
    }

    public static WorkOrder getWorkOrderNotNull(CallingContext context, String workOrderURI) {
        RaptureURI addressURI = new RaptureURI(workOrderURI, Scheme.WORKORDER);
        WorkOrder wo = WorkOrderStorage.readByAddress(addressURI);
        if (wo == null) {
            throw RaptureExceptionFactory.create("No work order found for URI " + workOrderURI);
        }
        return wo;
    }

}
