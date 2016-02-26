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
package rapture.notification.mongodb;

import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.NotificationInfo;
import rapture.common.NotificationResult;
import rapture.mongodb.EpochManager;
import rapture.mongodb.MongoDBFactory;
import rapture.mongodb.MongoRetryWrapper;
import rapture.notification.INotificationHandler;
import rapture.util.IDGenerator;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * Implementation of Notification Handler in MongoDB
 * 
 * We store it all in one table, with an epoch number held (like a idGen)
 * 
 * And entries based on epochs stored as first class records (so we can query by
 * epoch and record)
 * 
 * @author amkimian
 * 
 */
public class MongoNotificationHandler implements INotificationHandler {
    private static Logger log = Logger.getLogger(MongoNotificationHandler.class);
    private static final Object PREFIX_NAME = "prefix";
    private String tableName;
    private String instanceName = "default";
    private static final String RECORDTYPE = "recordType";
    private static final String NOTIFICATION = "notification";

    @Override
    public void setConfig(Map<String, String> config) {
        tableName = config.get(PREFIX_NAME);
    }

    @Override
    public Long getLatestNotificationEpoch() {
        return EpochManager.getLatestEpoch(getNotificationCollection());
    }

    @Override
    public String publishNotification(CallingContext context, String referenceId, String content, String contentType) {
        log.debug("Mongo push notification - content");
        BasicDBObject recordObject = new BasicDBObject();
        recordObject.put(RECORDTYPE, NOTIFICATION);
        recordObject.put("epoch", EpochManager.nextEpoch(getNotificationCollection()));
        String id = IDGenerator.getUUID();
        recordObject.put("id", id);
        recordObject.put("reference", referenceId);
        recordObject.put("content", content);
        recordObject.put("when", new Date());
        recordObject.put("contentType", contentType);
        recordObject.put("who", context.getUser());
        recordObject.put("kernelId", context.getContext());
        getNotificationCollection().insert(recordObject);
        return id;
    }

    @Override
    public NotificationResult findNotificationsAfterEpoch(final CallingContext context, Long lastEpochSeen) {
        // Find all records where the recordtype is NOTIFICATION and
        // the epoch is gt lastEpochSeen, returning just the ids
        // Return the current epoch and these ids.

        final BasicDBObject queryObject = new BasicDBObject();
        queryObject.put(RECORDTYPE, NOTIFICATION);
        final BasicDBObject greaterEpochObject = new BasicDBObject();
        greaterEpochObject.put("$gt", lastEpochSeen);
        queryObject.put("epoch", greaterEpochObject);

        final BasicDBObject fields = new BasicDBObject();
        fields.put("id", 1);
        fields.put("kernelId", 1);

        MongoRetryWrapper<NotificationResult> wrapper = new MongoRetryWrapper<NotificationResult>() {

            public DBCursor makeCursor() {
                return getNotificationCollection().find(queryObject, fields);
            }
            
            public NotificationResult action(DBCursor cursor) {
                NotificationResult res = new NotificationResult(getLatestNotificationEpoch());
                while (cursor.hasNext()) {
                    DBObject dobj = cursor.next();
                    String kernelId = (String) dobj.get("kernelId");
                    // ignore any notifications that came from this same kernel,
                    // which can cause race conditions
                    if (kernelId != null && kernelId.equals(context.getContext())) {
                        continue;
                    }
                    res.addId((String) dobj.get("id"));
                }
                return res;
            }
        };
        return wrapper.doAction();
    }

    @Override
    public NotificationInfo getNotification(String id) {
        // Find the one record that has this as the id, that is a NOTIFICATION
        BasicDBObject queryObject = new BasicDBObject();
        queryObject.put(RECORDTYPE, NOTIFICATION);
        queryObject.put("id", id);
        DBObject one = getNotificationCollection().findOne(queryObject);
        if (one != null) {
            NotificationInfo info = new NotificationInfo();
            info.setContent((String) one.get("content"));
            info.setEpoch((Long) one.get("epoch"));
            info.setId(id);
            info.setReference((String) one.get("reference"));
            info.setWhen((Date) one.get("when"));
            info.setContentType((String) one.get("contentType"));
            if (one.get("who") != null) {
                info.setWho((String) one.get("who"));
            }
            info.setKernelId((String) one.get("kernelId"));
            return info;
        }
        return null;
    }

    private DBCollection getNotificationCollection() {
        return MongoDBFactory.getCollection(instanceName, tableName);
    }

}
