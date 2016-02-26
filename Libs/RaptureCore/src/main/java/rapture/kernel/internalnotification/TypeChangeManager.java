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
package rapture.kernel.internalnotification;

import java.util.Map;

import rapture.notification.MessageNotificationManager;
import rapture.notification.NotificationMessage;
import rapture.notification.RaptureMessageListener;

/**
 * The typeChangeManager is used to coordinate changes of types (removals,
 * updates etc.) between instances of Rapture. It is held within the Kernel but
 * shared with the RepoCache
 * 
 * @author amkimian
 * 
 */
public class TypeChangeManager {
    private static final String MSG_TYPE = "kernel-typeChange";
    private static final String AUTHORITY = "authority";

    private MessageNotificationManager manager;

    public TypeChangeManager(MessageNotificationManager notificationManager) {
        manager = notificationManager;
    }

    /**
     * Signal that a type has changed
     * @param authority
     */
    public void setTypeChanged(String typeName, String authority) {
        NotificationMessage msg = new NotificationMessage();
        msg.setMessageType(getFullMsgType(typeName));
        msg.getAttributes().put(AUTHORITY, authority);
        manager.publishMessage(msg);
    }

    /**
     * Used to signal other changes
     */
    public void publishMessage(String typeName, Map<String, Object> attributes) {
        NotificationMessage msg = new NotificationMessage();
        msg.setMessageType(getFullMsgType(typeName));
        msg.getAttributes().putAll(attributes);
        manager.publishMessage(msg);
    }

    /**
     * Register this listener for type updates
     * 
     * @param listener
     */
    public void registerTypeListener(String typeName, RaptureMessageListener<NotificationMessage> listener) {
        manager.registerSubscription(getFullMsgType(typeName), listener);
    }

    public static String getAuthorityFromMessage(NotificationMessage msg) {
        return (String) msg.getAttributes().get(AUTHORITY);
    }

    private String getFullMsgType(String typeName) {
        return MSG_TYPE + "-" + typeName;
    }

}
