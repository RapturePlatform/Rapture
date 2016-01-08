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
package rapture.dp.semaphore;

import rapture.common.CallingContext;
import rapture.common.dp.PropertyBasedSemaphoreConfig;
import rapture.common.dp.SemaphoreType;
import rapture.common.dp.WorkflowBasedSemaphoreConfig;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;

public class WorkOrderSemaphoreFactory {

    
    
    public static WorkOrderSemaphore create(CallingContext callingContext, SemaphoreType type, String config) {
        int maxAllowed;
        Integer timeout;
        if (SemaphoreType.UNLIMITED == type) {
            maxAllowed = -1;
            timeout = -1;
        } else if (SemaphoreType.WORKFLOW_BASED == type) {
            WorkflowBasedSemaphoreConfig configObject = JacksonUtil.objectFromJson(config, WorkflowBasedSemaphoreConfig.class);
            maxAllowed = configObject.getMaxAllowed();
            timeout = configObject.getTimeout();
        } else if (SemaphoreType.PROPERTY_BASED == type) {
            PropertyBasedSemaphoreConfig configObject = JacksonUtil.objectFromJson(config, PropertyBasedSemaphoreConfig.class);
            maxAllowed = configObject.getMaxAllowed();
            timeout = configObject.getTimeout();
        } else {
            throw RaptureExceptionFactory.create(String.format("Error! Unsupported semaphore type '%s'", type));
        }
        return new WorkOrderSemaphore(callingContext, maxAllowed, timeout);
    }

}
