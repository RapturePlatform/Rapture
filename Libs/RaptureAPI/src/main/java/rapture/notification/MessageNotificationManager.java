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
package rapture.notification;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.NotificationInfo;
import rapture.common.NotificationResult;
import rapture.common.api.NotificationApi;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;

/**
 * The base notification manager has the logic for notifications
 * 
 * @author amkimian
 * 
 */
public class MessageNotificationManager {
    private NotificationApi notificationApi;
    private NotificationApiRetriever retriever;
    private Long currentEpoch;
    private String notificationName;
    private CallingContext context;
    private Map<String, Set<RaptureMessageListener<NotificationMessage>>> subscriptions = new HashMap<String, Set<RaptureMessageListener<NotificationMessage>>>();
    private RefreshThread thread;
    private static Logger log = Logger.getLogger(MessageNotificationManager.class);

    public MessageNotificationManager(NotificationApiRetriever retriever, String notificationName) {
        this.retriever = retriever;
        this.notificationName = notificationName;
        resetApi();
        currentEpoch = notificationApi.getLatestNotificationEpoch(context, notificationName);
    }

    private void resetApi() {
        notificationApi = retriever.getNotification();
        context = retriever.getCallingContext();
    }

    public void startNotificationManager() {
        thread = new RefreshThread(notificationName);
        thread.start();
    }

    public void stopNotificationManager() {
        if (thread != null) {
            thread.setQuit();
            // kill it right away.
            thread.interrupt();
            try {
                thread.join();
            } catch (Exception e) {

            }
            thread = null;
        }
    }

    public void registerSubscription(String msgType, RaptureMessageListener<NotificationMessage> listener) {
        if (!subscriptions.containsKey(msgType)) {
            // CopyOnWriteArraySet eliminates a ConcurrentModificationException in handleReference.
            // I would have used ConcurrentSkipListSet, but then the RaptureMessageListener elements
            // would have to support Comparable and it's too big a change
            subscriptions.put(msgType, new CopyOnWriteArraySet<RaptureMessageListener<NotificationMessage>>());
        }
        subscriptions.get(msgType).add(listener);
    }

    private void deregisterSubscription(String notificationName, RaptureMessageListener<NotificationMessage> listener) {
        if (subscriptions.containsKey(notificationName)) {
            subscriptions.get(notificationName).remove(listener);
        }
    }

    public void deregisterAllSubscriptions(RaptureMessageListener<NotificationMessage> listener) {
        for (String key : subscriptions.keySet()) {
            deregisterSubscription(key, listener);
        }
    }

    public void publishMessage(NotificationMessage msg) {
        String content = JacksonUtil.jsonFromObject(msg);
        String reference = msg.getMessageType();
        notificationApi.publishNotification(context, notificationName, reference, content, NotificationType.STRING.toString());
    }

    class RefreshThread extends Thread {
        public RefreshThread(String name) {
            this.setName("MRThread-" + name);
        }

        private boolean shouldQuit = false;

        public void setQuit() {
            shouldQuit = true;
        }

        public void run() {
            while (!shouldQuit) {
                try {
                    NotificationResult result = notificationApi.findNotificationsAfterEpoch(context, notificationName, currentEpoch);
                    currentEpoch = result.getCurrentEpoch();
                    for (String change : result.getReferences()) {
                        handleReference(change);
                    }
                } catch (RaptureException e) {
                    resetApi();
                }

                if (!shouldQuit) {
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {

                    }
                }
            }
        }
    }

    public void handleReference(String change) {
        // This change has occurred, do we need to signal someone?
        if (subscriptions.isEmpty()) {
            return;
        }
        NotificationInfo info = notificationApi.getNotification(context, notificationName, change);
        if (subscriptions.containsKey(info.getReference())) {
            Set<RaptureMessageListener<NotificationMessage>> refSubscriptions = subscriptions.get(info.getReference());
            for (RaptureMessageListener<NotificationMessage> subscription : refSubscriptions) {
                try {
                    NotificationMessage msg = JacksonUtil.objectFromJson(info.getContent(), NotificationMessage.class);
                    subscription.signalMessage(msg);
                } catch (Exception e) {
		    // at least acknowledge it
                    log.debug(ExceptionToString.format(e));
                }
            }

        }
    }
}
