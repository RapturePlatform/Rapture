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
package rapture.kernel.notification;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.NotificationResult;
import rapture.common.api.NotificationApi;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.notification.INotificationHandler;
import rapture.notification.MessageNotificationManager;
import rapture.notification.NotificationApiRetriever;
import rapture.notification.NotificationFactory;
import rapture.notification.NotificationMessage;
import rapture.notification.NotificationType;
import rapture.notification.RaptureMessageListener;

public class NotificationTest {
    @Test
    public void testSimpleNotification() {
        CallingContext user = new CallingContext();
        user.setUser("nonkerneluser");
        user.setValid(true);
        Kernel.getNotification().createNotificationManager(user, "Test", "NOTIFICATION USING MEMORY {}", "Sample");
        INotificationHandler handler = NotificationFactory.getNotificationHandler("Test");
        Long startEpoch = handler.getLatestNotificationEpoch();
        handler.publishNotification(user, "MyId", "My Content", NotificationType.STRING.toString());
        NotificationResult result = handler.findNotificationsAfterEpoch(user, startEpoch);
        System.out.println("New epoch is " + result.getCurrentEpoch());
        System.out.println("References are " + result.getReferences());
        for (String ref : result.getReferences()) {
            System.out.println("Reference data for " + ref + " is " + handler.getNotification(ref).getContent());
        }
        assertEquals(1, result.getReferences().size());
    }
    
    @Test
    public void testSelfNotifications() {
        CallingContext user = ContextFactory.getKernelUser();
        Kernel.getNotification().createNotificationManager(user, "Test", "NOTIFICATION USING MEMORY {}", "Sample");
        INotificationHandler handler = NotificationFactory.getNotificationHandler("Test");
        Long startEpoch = handler.getLatestNotificationEpoch();
        handler.publishNotification(user, "MyId", "My Content", NotificationType.STRING.toString());
        NotificationResult result = handler.findNotificationsAfterEpoch(user, startEpoch);
        assertEquals(0, result.getReferences().size()); 
    }

    @Test
    public void testNotificationManager() {
        Kernel.getNotification().createNotificationManager(ContextFactory.getKernelUser(), "Test", "NOTIFICATION USING MEMORY {}", "Sample");
        MessageNotificationManager mgr = new MessageNotificationManager(new NotificationApiRetriever() {

            @Override
            public NotificationApi getNotification() {
                return Kernel.getNotification();
            }

            @Override
            public CallingContext getCallingContext() {
                CallingContext context = new CallingContext();
                context.setUser("nonkerneluser");
                context.setValid(true);
                return context;
            }

        }, "Test");
        mgr.startNotificationManager();
        final Object lockObj = new Object();
        final AtomicBoolean done = new AtomicBoolean(false);
        mgr.registerSubscription("MyMessage", new RaptureMessageListener<NotificationMessage>() {
            @Override
            public void signalMessage(NotificationMessage msg) {
                System.out.println("Received message with special field " + msg.getAttributes().get("special"));
                synchronized (lockObj) {
                    done.set(true);
                    lockObj.notifyAll();
                }
            }

        });
        NotificationMessage message = new NotificationMessage();
        message.setMessageType("MyMessage");
        message.getAttributes().put("special", "Alan is special");
        mgr.publishMessage(message);
        synchronized (lockObj) {
            try {
                lockObj.wait(1300); // notification manager runs every 1s
                assertTrue("Did not receive notification in timely manner", done.get());
            } catch (InterruptedException e) {
                fail("Notification not received--interrupted: " + e.toString());
            }
        }
        mgr.stopNotificationManager();
    }

}
