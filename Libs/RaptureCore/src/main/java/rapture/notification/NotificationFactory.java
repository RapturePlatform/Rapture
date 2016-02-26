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

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.model.RaptureNotificationConfig;
import rapture.generated.NGenLexer;
import rapture.generated.NGenParser;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

/**
 * Generate NotificationHandler instances from config strings
 * 
 * @author alan
 * 
 */
public final class NotificationFactory {
    private static Logger log = Logger.getLogger(NotificationFactory.class);

    private static Map<String, INotificationHandler> notificationCache = new HashMap<String, INotificationHandler>();

    public static INotificationHandler createNotification(String config) {
        try {
            NGenParser parser = parseConfig(config);
            switch (parser.getStore().getType()) {
            case NGenLexer.FILE:
                return getNotificationStore("rapture.notification.file.FileNotificationHandler", parser.getConfig().getConfig());
            case NGenLexer.MONGODB:
                return getNotificationStore("rapture.notification.mongodb.MongoNotificationHandler", parser.getConfig().getConfig());
            case NGenLexer.MEMORY:
                return getNotificationStore("rapture.notification.memory.MemoryNotificationHandler", parser.getConfig().getConfig());
            case NGenLexer.DUMMY:
                return getNotificationStore("rapture.notification.dummy.DummyNotificationHandler", parser.getConfig().getConfig());
            case NGenLexer.REDIS:
                return getNotificationStore("rapture.notification.redis.RedisNotificationHandler", parser.getConfig().getConfig());
            }
        } catch (RecognitionException e) {
            log.error("Error parsing config - " + e.getMessage());
        }
        return null;
    }

    public static INotificationHandler getNotificationHandler(String name) {
        String keyName = name;
        if (notificationCache.containsKey(keyName)) {
            return notificationCache.get(keyName);
        }
        RaptureNotificationConfig config = Kernel.getNotification().getNotificationManagerConfig(ContextFactory.getKernelUser(), name);
        if (config != null) {
            log.info("Creating notification with config " + config.getConfig());
            INotificationHandler ret = createNotification(config.getConfig());
            if (ret == null) {
                log.info("Could not create notification");
            }
            notificationCache.put(keyName, ret);
            return ret;
        } else {
            log.error("Could not load notification handler, no config for " + name);
            return null;
        }
    }

    private static INotificationHandler getNotificationStore(String className, Map<String, String> config) {
        try {
            Class<?> notClass = Class.forName(className);
            Object fStore;
            fStore = notClass.newInstance();
            if (fStore instanceof INotificationHandler) {
                INotificationHandler ret = (INotificationHandler) fStore;
                ret.setConfig(config);
                return ret;
            } else {
                String message = className + " is not a notification implementation, cannot instantiate";
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, message);
            }
        } catch (InstantiationException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error instantiating notification store", e);
        } catch (IllegalAccessException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error instantiating notification store", e);
        } catch (ClassNotFoundException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error instantiating notification store", e);
        }
    }

    private static NGenParser parseConfig(String config) throws RecognitionException {
        NGenLexer lexer = new NGenLexer();
        lexer.setCharStream(new ANTLRStringStream(config));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        NGenParser parser = new NGenParser(tokens);
        parser.ninfo();
        return parser;
    }

    private NotificationFactory() {
    }

}
