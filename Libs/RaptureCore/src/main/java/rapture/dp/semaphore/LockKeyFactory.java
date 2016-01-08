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

import java.util.Map;

import rapture.common.dp.PropertyBasedSemaphoreConfig;
import rapture.common.dp.SemaphoreType;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;

/**
 * Allows a given number of simultaneous WorkOrders for each workflow URI
 * 
 * @author bardhi
 * 
 */
public class LockKeyFactory {

    /**
     * Create a lock key based on the {@link SemaphoreType} and the
     * configuration. Note: The contextMap passed in must be the raw map with
     * raw literal property values
     * 
     * @param type
     * @param configString
     * @param workflowURI
     * @param contextMap
     * @return
     */
    public static String createLockKey(SemaphoreType type, String configString, String workflowURI, Map<String, String> contextMap) {
        if (type == SemaphoreType.PROPERTY_BASED) {
            return createLenientPropertyBasedLockKey(configString, workflowURI, contextMap);
        } else if (type == SemaphoreType.UNLIMITED) {
            return null;
        } else if (type == SemaphoreType.WORKFLOW_BASED) {
            return createWorkflowLockKey(workflowURI);
        } else {
            throw RaptureExceptionFactory.create(String.format("Error! Unsupported semaphore type '%s'", type));
        }
    }

    /**
     * Limits the number of Work Orders based on WorkflowURI
     */
    private static String createWorkflowLockKey(String workflowURI) {
        return new LockKeyBuilder().add(workflowURI).build();
    }

    /**
     * Limits the number of Work Orders that can be created for a Workflow if
     * those WorkOrders have one property set the same. E.g. if the
     * maxConcurrent is set to 1, and two Work Orders have a property called
     * "name" set to "bob", only one of them can run. However another Work Order
     * for the same workflow with "name" set to "alice" can run simultaneously
     * with a "bob" Work Order
     * 
     * 
     * Note: This semaphore is lenient. An unlimited amount of WorkOrders with
     * the property not set is allowed. So in the example above, we can have
     * unlimited Work Orders for "name" not set at all.
     */
    private static String createLenientPropertyBasedLockKey(String configString, String workflowURI, Map<String, String> contextMap) {

        PropertyBasedSemaphoreConfig config = JacksonUtil.objectFromJson(configString, PropertyBasedSemaphoreConfig.class);

        String propertyName = config.getPropertyName();
        String propertyValue = contextMap.get(propertyName);
        if (propertyValue == null) {
            return null; // no lock key means unlocked!
        } else {
            return new LockKeyBuilder().add(workflowURI).add(config.getPropertyName()).add(propertyValue).build();
        }
    }

}
