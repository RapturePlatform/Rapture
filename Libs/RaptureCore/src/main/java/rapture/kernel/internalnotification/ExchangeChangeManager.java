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
public class ExchangeChangeManager {
    private static final String EXCHANGE_NAME = "exchangeName";
    private MessageNotificationManager manager;
    private static final String EXCHANGECHANGE = "kernel-exchangeChange";

    public ExchangeChangeManager(MessageNotificationManager notificationManager) {
        manager = notificationManager;
    }

    /**
     * Signal that an exchange has changed
     * 
     * @param exchangeName
     */
    public void setExchangeChanged(String exchangeName) {
        NotificationMessage msg = new NotificationMessage();
        msg.setMessageType(EXCHANGECHANGE);
        msg.getAttributes().put(EXCHANGE_NAME, exchangeName);
        manager.publishMessage(msg);
    }

    /**
     * Register this listener for exchange updates
     * 
     * @param listener
     */
    public void registerExchangeListener(RaptureMessageListener<NotificationMessage> listener) {
        manager.registerSubscription(EXCHANGECHANGE, listener);
    }

    public static String getExchangeFromMessage(NotificationMessage msg) {
        return (String) msg.getAttributes().get(EXCHANGE_NAME);
    }

    public static boolean isExchangeMessage(NotificationMessage msg) {
        return msg.getMessageType().equals(EXCHANGECHANGE);
    }
}
