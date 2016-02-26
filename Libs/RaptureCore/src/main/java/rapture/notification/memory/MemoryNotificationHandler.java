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
package rapture.notification.memory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.NotificationInfo;
import rapture.common.NotificationResult;
import rapture.notification.INotificationHandler;
import rapture.util.IDGenerator;

public class MemoryNotificationHandler implements INotificationHandler {
    private static Logger log = Logger.getLogger(MemoryNotificationHandler.class);

    private Long currentEpoch = 0L;
    private Map<Long, NotificationInfo> notifications = new HashMap<Long, NotificationInfo>();
    private Map<String, NotificationInfo> notificationsById = new HashMap<String, NotificationInfo>();

    @Override
    public void setConfig(Map<String, String> config) {
        log.info("Setting config for in memory natification handler");
    }

    @Override
    public Long getLatestNotificationEpoch() {
        return currentEpoch;
    }

    @Override
    public String publishNotification(CallingContext context, String referenceId, String content, String contentType) {
        String uuid = IDGenerator.getUUID();
        NotificationInfo info = new NotificationInfo();
        info.setId(uuid);
        info.setWho(context.getUser());
        info.setContent(content);
        info.setReference(referenceId);
        info.setEpoch(currentEpoch);
        info.setWhen(new Date());
        info.setContentType(contentType);
        info.setKernelId(context.getContext());
        currentEpoch++;
        notifications.put(info.getEpoch(), info);
        notificationsById.put(info.getId(), info);
        return uuid;
    }

    @Override
    public NotificationResult findNotificationsAfterEpoch(CallingContext context, Long lastEpochSeen) {
        NotificationResult result = new NotificationResult(currentEpoch);
        for (long epoch = lastEpochSeen; epoch < currentEpoch; epoch++) {
            NotificationInfo info = notifications.get(epoch);
            String originKernelId = info.getKernelId();
            // prevent kernel from receiving notifications from itself, which can cause race conditions
            if (originKernelId != null && originKernelId.equals(context.getContext())) {
                continue;
            }
            result.addId(info.getId());
        }
        return result;
    }

    @Override
    public NotificationInfo getNotification(String id) {
        return notificationsById.get(id);
    }

}
