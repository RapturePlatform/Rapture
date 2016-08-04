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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.SemaphoreAcquireResponse;
import rapture.common.SemaphoreLockStorage;
import rapture.common.dp.PropertyBasedSemaphoreConfig;
import rapture.common.dp.SemaphoreType;
import rapture.common.dp.WorkflowBasedSemaphoreConfig;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class SemaphoreTest {

    private static final Integer MAX_ALLOWED = 15;
    private static final Integer NUM_REMOVE = 5;

    private static final String WORKFLOW_URI = new RaptureURI("//auth/test", Scheme.WORKFLOW).toString();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Kernel.initBootstrap();
    }

    private LinkedList<RaptureURI> workOrderURIs;

    @Before
    public void setUp() throws Exception {
        workOrderURIs = new LinkedList<RaptureURI>();
    }

    @Test
    public void workOrderBased() {
        SemaphoreType type = SemaphoreType.WORKFLOW_BASED;
        WorkflowBasedSemaphoreConfig config = new WorkflowBasedSemaphoreConfig();
        config.setMaxAllowed(MAX_ALLOWED);
        String configString = JacksonUtil.jsonFromObject(config);
        String lockKey = LockKeyFactory.createLockKey(SemaphoreType.WORKFLOW_BASED, configString, WORKFLOW_URI, null);
        WorkOrderSemaphore semaphore = WorkOrderSemaphoreFactory.create(ContextFactory.getKernelUser(), type, configString);
        SemaphoreLockStorage.deleteByFields(lockKey, "user", "test");

        /*
         * Acquire all permits
         */
        for (int i = 0; i < MAX_ALLOWED; i++) {
            acquireGood(semaphore, i, lockKey);
        }

        /*
         * Make sure we can't acquire more
         */
        for (int i = 0; i < MAX_ALLOWED; i++) {
            acquireBad(semaphore, i, lockKey);
        }

        /*
         * Release some
         */
        for (int i = 0; i < NUM_REMOVE; i++) {
            release(semaphore, lockKey);
        }

        /*
         * Acquire again to the max
         */
        for (int i = 0; i < NUM_REMOVE; i++) {
            acquireGood(semaphore, i, lockKey);
        }

        /*
         * Make sure we can't acquire again
         */
        acquireBad(semaphore, 0, lockKey);

    }

    private void release(WorkOrderSemaphore semaphore, String lockKey) {
        RaptureURI uri = workOrderURIs.remove(0);
        semaphore.releasePermit(uri.toString(), lockKey);
    }

    private void acquireBad(WorkOrderSemaphore semaphore, int count, String lockKey) {
        Long startInstant = System.currentTimeMillis();
        SemaphoreAcquireResponse response = semaphore.tryAcquirePermit(WORKFLOW_URI, startInstant, lockKey);
        String uri = response.getAcquiredURI();
        assertNull("Null for attempt # " + count, uri);
        assertFalse("False attempt # " + count, response.getIsAcquired());
    }

    private void acquireGood(WorkOrderSemaphore semaphore, int count, String lockKey) {
        Long startInstant = System.currentTimeMillis();
        SemaphoreAcquireResponse response = semaphore.tryAcquirePermit(WORKFLOW_URI, startInstant, lockKey);
        RaptureURI uri = new RaptureURI(response.getAcquiredURI());
        assertTrue("True for attempt # " + count, response.getIsAcquired());
        assertNotNull("Not null for attempt # " + count, uri);
        workOrderURIs.add(uri);
    }

    @Test
    public void propertyBased() {
        SemaphoreType type = SemaphoreType.PROPERTY_BASED;
        PropertyBasedSemaphoreConfig config = new PropertyBasedSemaphoreConfig();
        config.setMaxAllowed(MAX_ALLOWED);
        config.setPropertyName("strategy");
        String configString = JacksonUtil.jsonFromObject(config);
        WorkOrderSemaphore semaphore = WorkOrderSemaphoreFactory.create(ContextFactory.getKernelUser(), type, configString);

        Map<String, String> map = new HashMap<String, String>();
        String lockKey;
        try {
            lockKey = LockKeyFactory.createLockKey(SemaphoreType.PROPERTY_BASED, configString, WORKFLOW_URI, map);
            fail("You must specify the property name in the map");
        } catch (RaptureException e) {
            // TODO Auto-generated catch block
            assertEquals("Property strategy is not defined and property based locking is in force", e.getMessage());
        }

        for (int strategy = 0; strategy < 5; strategy++) {
            workOrderURIs.clear();
            map.put("strategy", "myStrategy" + strategy);
            lockKey = LockKeyFactory.createLockKey(SemaphoreType.PROPERTY_BASED, configString, WORKFLOW_URI, map);
            SemaphoreLockStorage.deleteByFields(lockKey, "user", "test");
            /*
             * Acquire all permits
             */
            for (int i = 0; i < MAX_ALLOWED; i++) {
                acquireGood(semaphore, i, lockKey);
            }

            /*
             * Make sure we can't acquire more
             */
            for (int i = 0; i < MAX_ALLOWED; i++) {
                acquireBad(semaphore, i, lockKey);
            }

            /*
             * Release some
             */
            for (int i = 0; i < NUM_REMOVE; i++) {
                release(semaphore, lockKey);
            }

            /*
             * Acquire again to the max
             */
            for (int i = 0; i < NUM_REMOVE; i++) {
                acquireGood(semaphore, i, lockKey);
            }

            /*
             * Make sure we can't acquire again
             */
            acquireBad(semaphore, 0, lockKey);
        }
    }

}
