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
package rapture.dp.semaphore;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.SemaphoreAcquireResponse;
import rapture.common.dp.WorkOrder;
import rapture.kernel.Kernel;

/**
 * Abstract WorkOrder sempahore strategy
 * 
 * @author bardhi
 * 
 */
public class WorkOrderSemaphore {

    @Override
    public String toString() {
        return "WorkOrderSemaphore [callingContext=" + callingContext + ", maxAllowed=" + maxAllowed + ", timeout=" + timeout + "]";
    }

    private CallingContext callingContext;
    private Integer maxAllowed;
    private int timeout;

    public WorkOrderSemaphore(CallingContext callingContext, Integer maxAllowed, Integer timeout) {
        super();
        this.callingContext = callingContext;
        this.maxAllowed = maxAllowed;
        if(timeout != null) {
            this.timeout = timeout;
        }
    }

    public int getTimeout() {
        return timeout;
    }

    /**
     * Attempt to acquire a permit for this WorkOrder to execute, blocking until a permit is acquired, or time out.
     * If the permit is acquired, a {@link WorkOrder} URI is returned; otherwise, null is returned.
     * @param workflowURI
     * @param startInstant
     * @param lockKey
     * @return A URI to be used for the WorkOrder, or null if fail to acquire a permit
     */
    public SemaphoreAcquireResponse acquirePermit(final String workflowURI, final Long startInstant, String lockKey, long timeout) {
        URIGenerator uriGenerator = new URIGenerator() {
            @Override
            public RaptureURI generateStakeholderURI() {
                return Kernel.getDecision().getTrusted().generateWorkOrderURI(callingContext, workflowURI, startInstant);
            }
        };
        if (lockKey == null) {
            // null key means we're not locking on this at all! so we can create
            // unlimited
            SemaphoreAcquireResponse response = new SemaphoreAcquireResponse();
            response.setAcquiredURI(uriGenerator.generateStakeholderURI().toString());
            response.setIsAcquired(true);
            return response;
        } else {
            return Kernel.getLock().getTrusted().acquirePermit(callingContext, maxAllowed, lockKey, uriGenerator, timeout);
        }
    }

    /**
     * Attempt to acquire a permit for this WorkOrder to execute. If the permit
     * is acquired, a {@link WorkOrder} URI is returned, which should be used
     * for the WorkOrder. If the permit is not acquired, null is returned.
     * 
     * @param workflowURI
     * @param startInstant
     * @param lockKey
     * @return A URI to be used for the WorkOrder, or null if impossible to
     *         acquire a permit
     */
    public SemaphoreAcquireResponse tryAcquirePermit(final String workflowURI, final Long startInstant, String lockKey) {
        URIGenerator uriGenerator = new URIGenerator() {
            @Override
            public RaptureURI generateStakeholderURI() {
                return Kernel.getDecision().getTrusted().generateWorkOrderURI(callingContext, workflowURI, startInstant);
            }
        };
        if (lockKey == null) {
            // null key means we're not locking on this at all! so we can create
            // unlimited
            SemaphoreAcquireResponse response = new SemaphoreAcquireResponse();
            response.setAcquiredURI(uriGenerator.generateStakeholderURI().toString());
            response.setIsAcquired(true);
            return response;
        } else {
            return Kernel.getLock().getTrusted().tryAcquirePermit(callingContext, maxAllowed, lockKey, uriGenerator);
        }
    }

    public void releasePermit(String workOrderURI, String lockKey) {
        if (lockKey == null) {
            // null key means no locking!
        } else {
            Kernel.getLock().getTrusted().releasePermit(callingContext, workOrderURI, lockKey);
        }
    }
}
