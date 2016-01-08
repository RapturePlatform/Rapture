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
package rapture.audit.mongodb;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.audit.AuditLog;
import rapture.audit.AuditUtil;
import rapture.common.RaptureURI;
import rapture.common.exception.ExceptionToString;
import rapture.common.model.AuditLogEntry;
import rapture.mongodb.MongoDBFactory;
import rapture.mongodb.MongoRetryWrapper;
import rapture.util.IDGenerator;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

public class MongoDBAuditLog implements AuditLog {
    private static Logger log = Logger.getLogger(MongoDBAuditLog.class);
    private static final String CATEGORY = "category";
    private static final String ENTRY_ID = "entryId";
    private static final String SOURCE = "source";
    private static final String WHEN = "when";
    private static final String LOG_ID = "logId";
    private static final String USER = "user";
    private static final String MESSAGE = "message";
    private static final String LEVEL = "level";
    private String tableName;
    private String instanceName = "default";
    private static final String TABLE_NAME = "prefix";

    private DBCollection getAuditCollection() {
        return MongoDBFactory.getCollection(instanceName, tableName);
    }

    private String logId;

    @Override
    public List<AuditLogEntry> getRecentEntries(final int count) {
        final BasicDBObject sort = getSortObject();

        MongoRetryWrapper<List<AuditLogEntry>> wrapper = new MongoRetryWrapper<List<AuditLogEntry>> () {
            public DBCursor makeCursor() {
                return getAuditCollection().find().sort(sort).limit(count);
            }
            
            public List<AuditLogEntry> action(DBCursor cursor) {
                List<AuditLogEntry> ret = new ArrayList<AuditLogEntry>();
                fillFromCursor(ret, cursor);
                return ret;
            }
        };
        
        return wrapper.doAction();
    }

    private String safeString(DBObject obj, String fieldName) {
        if (obj.containsField(fieldName)) {
            Object val = obj.get(fieldName);
            if (val != null) {
                return val.toString();
            }
        }
        return "";
    }

    private void fillFromCursor(List<AuditLogEntry> ret, DBCursor cursor) throws MongoException {
        while (cursor.hasNext()) {
            DBObject obj = cursor.next();
            AuditLogEntry entry = new AuditLogEntry();
            entry.setLevel((Integer) obj.get(LEVEL));
            entry.setMessage(safeString(obj, MESSAGE));
            entry.setUser(safeString(obj, USER));
            entry.setLogId(safeString(obj, LOG_ID));
            entry.setWhen((Date) obj.get(WHEN));
            entry.setSource(safeString(obj, SOURCE));
            entry.setEntryId(safeString(obj, ENTRY_ID));
            entry.setCategory(safeString(obj, CATEGORY));
            ret.add(entry);
        }
    }

    @Override
    public List<AuditLogEntry> getEntriesSince(AuditLogEntry when) {
        final BasicDBObject sort = getSortObject();
        final BasicDBObject query = new BasicDBObject();
        BasicDBObject test = new BasicDBObject();
        test.put("$gt", when.getWhen());
        query.put(WHEN, test);
        MongoRetryWrapper<List<AuditLogEntry>> wrapper = new MongoRetryWrapper<List<AuditLogEntry>> () {
            public DBCursor makeCursor() {
                return getAuditCollection().find(query).sort(sort);
            }
            
            public List<AuditLogEntry> action(DBCursor cursor) {
                List<AuditLogEntry> ret = new ArrayList<AuditLogEntry>();
                fillFromCursor(ret, cursor);
                return ret;
            }
        };
        return wrapper.doAction();
    }

    @Override
    public void setConfig(String logId, Map<String, String> config) {
        tableName = config.get(TABLE_NAME);
        this.logId = logId;
        try {
            getAuditCollection().ensureIndex(WHEN);
        } catch (MongoException e) {
            log.info("setConfig failed on "+tableName+": "+e.getMessage());
            log.debug(ExceptionToString.format(e));
        }
    }

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public Boolean writeLog(String category, int level, String message, String user) {
        log.debug("Mongo write audit log - " + message);
        BasicDBObject obj = new BasicDBObject();
        obj.append(LEVEL, level);
        obj.append(MESSAGE, message);
        obj.append(USER, user);
        obj.append(WHEN, new Date());
        obj.append(LOG_ID, logId);
        obj.append(SOURCE, "");
        obj.append(CATEGORY, category);
        obj.append(ENTRY_ID, IDGenerator.getUUID());
        try {
            getAuditCollection().insert(obj);
        } catch (MongoException e) {
            System.err.println("Cannot log "+message+": "+e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * For now
     */
    
    @Override
    public Boolean writeLogData(String category, int level, String message, String user, Map<String, Object> data) {
        return writeLog(category, level, message + " " + AuditUtil.getStringRepresentation(data), user );
    }
    
    private BasicDBObject getSortObject() {
        BasicDBObject ret = new BasicDBObject();
        ret.append(WHEN, 1);
        return ret;
    }

    @Override
    public void setContext(RaptureURI internalURI) {
    }

}
