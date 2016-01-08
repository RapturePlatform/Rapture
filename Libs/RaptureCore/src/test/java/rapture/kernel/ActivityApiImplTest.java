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
package rapture.kernel;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import rapture.common.Activity;
import rapture.common.ActivityQueryResponse;
import rapture.common.ActivityStatus;
import rapture.common.CallingContext;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ActivityApiImplTest {

    private static final long PROGRESS1 = 1L;
    private static final long PROGRESS2 = 2L;
    private static final long MAX1 = 5L;
    private static final long MAX2 = 6L;
    private static final String MESSAGE1 = "message1";
    private static final String MESSAGE2 = "message2";
    private static ActivityApiImplWrapper api;
    private static CallingContext context;

    @Before
    public void setUp() throws Exception {
        Kernel.INSTANCE.restart();
        Kernel.initBootstrap();
        api = Kernel.getActivity();
        context = ContextFactory.getKernelUser();
    }

    @Test
    public void testCreateActivity() throws Exception {
        long before = System.currentTimeMillis();
        String id = api.createActivity(context, "test", MESSAGE1, PROGRESS1, MAX1);
        long after = System.currentTimeMillis();

        Activity activity = api.getById(context, id);

        assertEquals(id, activity.getId());
        assertEquals(ActivityStatus.ACTIVE, activity.getStatus());
        assertEquals("test", activity.getDescription());
        assertEquals(MESSAGE1, activity.getMessage());
        assertEquals(PROGRESS1, activity.getProgress().longValue());
        assertEquals(MAX1, activity.getMax().longValue());
        assertBeforeAndAfter(before, after, activity);
    }

    private void assertBeforeAndAfter(long before, long after, Activity activity) {

        String message = String
                .format("lastSeen=[%s], before=[%s], after=[%s]", activity.getLastSeen(), before, after);
        assertTrue(message, activity.getLastSeen() >= before);
        assertTrue(message, activity.getLastSeen() <= after);
    }

    @Test
    public void testUpdateActivity() throws Exception {
        String id = api.createActivity(context, "test", MESSAGE1, PROGRESS1, MAX1);

        long before = System.currentTimeMillis() - 1;
        assertTrue(api.updateActivity(context, id, MESSAGE2, PROGRESS2, MAX1));
        long after = System.currentTimeMillis() + 1;

        Activity activity = api.getById(context, id);
        assertEquals(ActivityStatus.ACTIVE, activity.getStatus());
        assertEquals(MESSAGE2, activity.getMessage());
        assertEquals(PROGRESS2, activity.getProgress().longValue());
        assertEquals(MAX1, activity.getMax().longValue());
        assertBeforeAndAfter(before, after, activity);

        assertTrue(api.updateActivity(context, id, MESSAGE2, PROGRESS2, MAX2));
        activity = api.getById(context, id);
        assertEquals(ActivityStatus.ACTIVE, activity.getStatus());
        assertEquals(MESSAGE2, activity.getMessage());
        assertEquals(PROGRESS2, activity.getProgress().longValue());
        assertEquals(MAX2, activity.getMax().longValue());

    }

    @Test
    public void testFinishActivity() throws Exception {
        String id = api.createActivity(context, "test", MESSAGE1, PROGRESS1, MAX1);

        long before = System.currentTimeMillis() - 1;
        assertTrue(api.finishActivity(context, id, MESSAGE2));
        long after = System.currentTimeMillis() + 1;

        Activity activity = api.getById(context, id);
        assertEquals(ActivityStatus.FINISHED, activity.getStatus());
        assertEquals(MESSAGE2, activity.getMessage());
        assertEquals(PROGRESS1, activity.getProgress().longValue());
        assertEquals(MAX1, activity.getMax().longValue());
        assertBeforeAndAfter(before, after, activity);

        //These guys should return false
        assertFalse(api.finishActivity(context, id, MESSAGE1));
        assertFalse(api.updateActivity(context, id, MESSAGE1, PROGRESS2, MAX2));
        assertFalse(api.requestAbortActivity(context, id, MESSAGE1));

        //Activity should be same as right after finish
        Activity activity2 = api.getById(context, id);
        assertEquals(activity, activity2);
    }

    @Test
    public void testRequestAbortActivity() throws Exception {
        String id = api.createActivity(context, "test", MESSAGE1, PROGRESS1, MAX1);

        long before = System.currentTimeMillis() - 1;
        assertTrue(api.requestAbortActivity(context, id, MESSAGE2));
        long after = System.currentTimeMillis() + 1;

        Activity activity = api.getById(context, id);
        assertEquals(ActivityStatus.ABORTED, activity.getStatus());
        assertEquals(MESSAGE2, activity.getMessage());
        assertEquals(PROGRESS1, activity.getProgress().longValue());
        assertEquals(MAX1, activity.getMax().longValue());
        assertBeforeAndAfter(before, after, activity);

        //These guys should return false
        assertFalse(api.finishActivity(context, id, MESSAGE1));
        assertFalse(api.updateActivity(context, id, MESSAGE1, PROGRESS2, MAX2));
        assertFalse(api.requestAbortActivity(context, id, MESSAGE1));

        //Activity should be same as right after abort
        Activity activity2 = api.getById(context, id);
        assertEquals(activity, activity2);

    }

    @Test
    public void testQueryByExpiryTime() throws Exception {
        long before = System.currentTimeMillis() - 1;
        String id1 = create();
        String id2 = create();
        String id3 = create();

        Thread.sleep(1);
        long cutoff = System.currentTimeMillis();
        Thread.sleep(1);
        touch(id1);
        String id4 = create();
        String id5 = create();

        ActivityQueryResponse response1 = api.queryByExpiryTime(context, "", 100L, before);
        List<Activity> activities = response1.getActivities();
        assertEquals(String.format("found %s:\n%s\n\nexpected %s-%s", activities.size(), activities, id1, id5), 5, activities.size());

        assertTrue(contains(response1, id1));
        assertTrue(contains(response1, id2));
        assertTrue(contains(response1, id3));
        assertTrue(contains(response1, id4));
        assertTrue(contains(response1, id5));

        ActivityQueryResponse response2 = api.queryByExpiryTime(context, "", 100L, cutoff);
        assertEquals(3, response2.getActivities().size());
        assertFalse(contains(response2, id2));
        assertFalse(contains(response2, id3));

        assertTrue(contains(response2, id1));
        assertTrue(contains(response2, id4));
        assertTrue(contains(response2, id5));

    }

    private void touch(String id) {
        api.updateActivity(context, id, MESSAGE2, PROGRESS1, MAX2);
    }

    private boolean contains(ActivityQueryResponse response, String id) {
        for (Activity activity : response.getActivities()) {
            if (activity.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private String create() {
        return api.createActivity(context, "test", MESSAGE1, PROGRESS1, MAX1);
    }

    @Test
    public void testQueryByExpiryTimeBatches() throws Exception {
        String id1 = create();
        assertNotNull(create());
        Assert.assertNotNull(create());

        Thread.sleep(1);
        long cutoff = System.currentTimeMillis();
        Thread.sleep(1);
        touch(id1);
        String id4 = create();
        String id5 = create();

        long batchSize = 1;
        ActivityQueryResponse response = api.queryByExpiryTime(context, "", batchSize, cutoff);
        int count = 0;
        while (!response.getIsLast()) {
            assertEquals(1, response.getActivities().size());
            Activity activity = response.getActivities().get(0);
            String id = activity.getId();
            assertTrue(String.format("id is [%s]", id), id.equals(id1) || id.equals(id4) || id.equals(id5));
            response = api.queryByExpiryTime(context, response.getNextBatchId(), batchSize, cutoff);

            count++;
            if (count > 5) {
                fail(String.format("Count is %s, id is [%s], response is %s", count, id, response));
            }
        }
    }

}