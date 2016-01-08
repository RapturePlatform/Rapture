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

import rapture.common.CallingContext;
import rapture.common.NotificationInfo;
import rapture.common.NotificationResult;
import rapture.common.RaptureActivity;
import rapture.common.RaptureActivityStorage;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.NotificationApi;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JsonContent;
import rapture.common.model.RaptureNotificationConfig;
import rapture.common.model.RaptureNotificationConfigStorage;
import rapture.notification.INotificationHandler;
import rapture.notification.NotificationFactory;
import rapture.repo.RepoVisitor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * @author amkimian
 */
public class NotificationApiImpl extends KernelBase implements NotificationApi {
    private static Logger log = Logger.getLogger(NotificationApiImpl.class);

    public NotificationApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    @Override
    public List<RaptureNotificationConfig> findNotificationManagerConfigsByPurpose(CallingContext context, final String purpose) {
        final List<RaptureNotificationConfig> ret = new ArrayList<RaptureNotificationConfig>();
        RaptureNotificationConfigStorage.visitAll(new RepoVisitor() {

            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                if (!isFolder) {
                    log.info("Visiting " + name);
                    RaptureNotificationConfig notification;
                    try {
                        notification = RaptureNotificationConfigStorage.readFromJson(content);
                        if (notification.getPurpose().equals(purpose)) {
                            ret.add(notification);
                        }
                    } catch (RaptureException e) {
                        log.error("Could not load document " + name + ", continuing anyway");
                    }
                }
                return true;
            }

        });
        return ret;
    }

    @Override
    public List<RaptureNotificationConfig> getNotificationManagerConfigs(CallingContext context) {
        return RaptureNotificationConfigStorage.readAll();
    }

    @Override
    public RaptureNotificationConfig createNotificationManager(CallingContext context, String notificationManagerUri, String cfg, String purpose) {
        RaptureNotificationConfig config = new RaptureNotificationConfig();
        config.setConfig(cfg);
        config.setName(notificationManagerUri);
        config.setPurpose(purpose);
        RaptureNotificationConfigStorage.add(config, context.getUser(), "Create notification provider");
        return config;
    }

    @Override
    public RaptureNotificationConfig getNotificationManagerConfig(CallingContext context, String notificationManagerUri) {
        // RaptureURI addressURI = new RaptureURI(notificationManagerUri,
        // Scheme.NOTIFICATION);
        return RaptureNotificationConfigStorage.readByFields(notificationManagerUri);
    }

    @Override
    public Boolean notificationManagerExists(CallingContext context, String notificationManagerUri) {
        RaptureNotificationConfig config = getNotificationManagerConfig(context, notificationManagerUri);
        if (config == null) return false;
        else return true;
    }

    @Override
    public void deleteNotificationManager(CallingContext context, String notificationManagerUri) {
        RaptureURI addressURI = new RaptureURI(notificationManagerUri, Scheme.NOTIFICATION);
        RaptureNotificationConfigStorage.deleteByAddress(addressURI, context.getUser(), "Deleting notification provider");
    }

    @Override
    public Long getLatestNotificationEpoch(CallingContext context, String notificationManagerUri) {
        INotificationHandler handler = NotificationFactory.getNotificationHandler(notificationManagerUri);
        return handler.getLatestNotificationEpoch();
    }

    @Override
    public String publishNotification(CallingContext context, String notificationManagerUri, String referenceId, String content, String contentType) {
        INotificationHandler handler = NotificationFactory.getNotificationHandler(notificationManagerUri);
        if (handler != null) {
            return handler.publishNotification(context, referenceId, content, contentType);
        } else {
            return "";
        }
    }

    @Override
    public NotificationResult findNotificationsAfterEpoch(CallingContext context, String notificationManagerUri, Long epoch) {
        INotificationHandler handler = NotificationFactory.getNotificationHandler(notificationManagerUri);
        return handler.findNotificationsAfterEpoch(context, epoch);
    }

    @Override
    public NotificationInfo getNotification(CallingContext context, String notificationUri, String id) {
        INotificationHandler handler = NotificationFactory.getNotificationHandler(notificationUri);
        return handler.getNotification(id);
    }

    // Activity handling

    private Long nextPredictedSlot = 0L;
    private static final Long MAXSLOT = 100L;

    private RaptureActivity getAvailableSlot() {
        boolean found = false;
        nextPredictedSlot++;
        long rotateSlot = nextPredictedSlot;
        Date now = new Date();
        RaptureActivity activity = null;
        while (!found) {
            activity = RaptureActivityStorage.readByFields("" + nextPredictedSlot);
            if (activity == null || now.getTime() > activity.getExpiresAt()) {
                found = true;
            } else {
                nextPredictedSlot++;
                if (nextPredictedSlot > MAXSLOT) {
                    nextPredictedSlot = 0L;
                }
                if (rotateSlot == nextPredictedSlot) {
                    found = true;
                }
            }
        }
        if (activity == null) {
            activity = new RaptureActivity();
            activity.setId("" + nextPredictedSlot);
        }
        return activity;
    }

    @Override
    public List<RaptureFolderInfo> listNotificationsByUriPrefix(CallingContext context, String uriPrefix) {
        return RaptureNotificationConfigStorage.getChildren(uriPrefix);
    }

}
