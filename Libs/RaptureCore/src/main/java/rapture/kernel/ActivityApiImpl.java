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

import rapture.common.Activity;
import rapture.common.ActivityQueryResponse;
import rapture.common.ActivityStatus;
import rapture.common.ActivityStorage;
import rapture.common.CallingContext;
import rapture.common.api.ActivityApi;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JsonContent;
import rapture.idgen.SystemIdGens;
import rapture.repo.RepoVisitor;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.mutable.MutableObject;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

/**
 * @author bardhi
 * @since 3/17/15.
 */
public class ActivityApiImpl extends KernelBase implements ActivityApi {
    private static final long MAX_BATCH_SIZE = 10000;
    private static Logger log = Logger.getLogger(ActivityApiImpl.class);

    public ActivityApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    @Override
    public String createActivity(CallingContext context, String description, String message, Long progress, Long max) {
        String id = createNextId(context);

        Activity activity = new Activity();
        activity.setId(id);
        activity.setDescription(description);
        activity.setMessage(message);
        activity.setProgress(progress);
        activity.setMax(max);
        long now = System.currentTimeMillis();
        activity.setLastSeen(now);
        activity.setStatus(ActivityStatus.ACTIVE);

        ActivityStorage.add(activity, context.getUser(), "Record activity");
        return "" + activity.getId();
    }

    @Override
    public Boolean updateActivity(CallingContext context, String activityId, String message, Long progress, Long max) {
        Activity activity = getActivityNotNull(activityId);
        if (isActive(activity)) {
            activity.setMessage(message);
            activity.setProgress(progress);
            activity.setMax(max);
            long now = System.currentTimeMillis();
            activity.setLastSeen(now);
            ActivityStorage.add(activity, context.getUser(), "update activity");
            return true;
        } else {
            return false;
        }
    }

    private boolean isActive(Activity activity) {
        return activity.getStatus() == ActivityStatus.ACTIVE;
    }

    private Activity getActivityNotNull(String activityId) {
        Activity activity = ActivityStorage.readByFields(activityId);
        if (activity == null) {
            throw RaptureExceptionFactory.create(HttpStatus.SC_BAD_REQUEST, String.format("Activity with id [%s] does not exist!", activityId));
        } else {
            return activity;
        }
    }

    @Override
    public Boolean finishActivity(CallingContext context, String activityId, String message) {
        Activity activity = getActivityNotNull(activityId);
        if (isActive(activity)) {
            activity.setMessage(message);
            long now = System.currentTimeMillis();
            activity.setLastSeen(now);
            activity.setStatus(ActivityStatus.FINISHED);
            ActivityStorage.add(activity, context.getUser(), "finishActivity");
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Boolean requestAbortActivity(CallingContext context, String activityId, String message) {
        Activity activity = getActivityNotNull(activityId);
        if (isActive(activity)) {
            activity.setMessage(message);
            long now = System.currentTimeMillis();
            activity.setLastSeen(now);
            activity.setStatus(ActivityStatus.ABORTED);
            ActivityStorage.add(activity, context.getUser(), "request abort activity");
            return true;
        } else {
            return false;
        }
    }

    @Override
    public ActivityQueryResponse queryByExpiryTime(CallingContext context, final String nextBatchId, Long batchSize, final Long lastSeen) {
        if (batchSize > MAX_BATCH_SIZE) {
            log.warn(String.format("Batch size %s is too large, resetting to maximum of %s", batchSize, MAX_BATCH_SIZE));
            batchSize = MAX_BATCH_SIZE;
        }

        final MutableObject lastId = new MutableObject();
        final long finalBatchSize = batchSize;

        final List<Activity> activities = new LinkedList<>();
        ActivityStorage.visitAll(new RepoVisitor() {
            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                if (activities.size() < finalBatchSize) {
                    Activity activity = ActivityStorage.readFromJson(content);
                    String currentId = activity.getId();
                    if ((nextBatchId.length() == 0 || currentId.compareTo(nextBatchId) > 0) && activity.getLastSeen() > lastSeen) {
                        activities.add(activity);
                        lastId.setValue(currentId);
                    }
                    return true;
                } else {
                    return false;
                }
            }
        });

        ActivityQueryResponse response = new ActivityQueryResponse();
        response.setActivities(activities);
        boolean isLast = activities.size() < batchSize;
        response.setIsLast(isLast);
        if (!isLast) {
            String responseNextId = lastId.getValue().toString();
            response.setNextBatchId(responseNextId);
        }
        return response;
    }

    @Override
    public Activity getById(CallingContext context, String activityId) {
        return ActivityStorage.readByFields(activityId);
    }

    public String createNextId(CallingContext context) {
        return Kernel.getIdGen().nextIds(context, SystemIdGens.ACTIVITY_IDGEN_URI, 1L);
    }
}
