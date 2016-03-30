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
package rapture.repo.mongodb;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bson.Document;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.util.JSON;

import rapture.common.Messages;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureNativeQueryResult;
import rapture.common.RaptureNativeRow;
import rapture.common.RaptureQueryResult;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptNotSupportedException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.JsonContent;
import rapture.index.IndexHandler;
import rapture.index.IndexProducer;
import rapture.kernel.Kernel;
import rapture.mongodb.MongoDBFactory;
import rapture.mongodb.MongoRetryWrapper;
import rapture.notification.NotificationMessage;
import rapture.notification.RaptureMessageListener;
import rapture.repo.AbstractKeyStore;
import rapture.repo.KeyStore;
import rapture.repo.RepoLockHandler;
import rapture.repo.RepoVisitor;
import rapture.repo.StoreKeyVisitor;
import rapture.series.mongo.MongoSeriesStore;
import rapture.table.mongodb.MongoIndexHandler;

/**
 * The MongoDBDataStore is configured with a table name and then the content
 * follows a normal K=V type approach to store the data.
 * <p>
 * The MongoDB connection URL is normally a setting in the environment
 * <p>
 * tableName = 'table'
 *
 * @author alan
 */
public class MongoDbDataStore extends AbstractKeyStore implements KeyStore, RaptureMessageListener<NotificationMessage> {
    private static final Logger log = Logger.getLogger(MongoDbDataStore.class);
    private static final String MONGODB = "MONGODB";
    private static final String $IN = "$in";
    private static final String SORT = "sort";
    private static final String LIMIT = "limit";
    private static final String SKIP = "skip";
    public static final String PREFIX = "prefix";
    private static final String VALUE = "value";
    private static final String KEY = "key";
    private static final String VERSION = "version";
    private static final String SEPARATE_VERSION = "separateVersion";
    private String tableName;
    private String instanceName = "default";
    private boolean separateVersion = false;
    private boolean needsFolderHandling = true;
    private Messages mongoMsgCatalog;
    private final MongoChildrenRepo dirRepo = new MongoChildrenRepo(new MongoSeriesStore());

    public MongoDbDataStore() {
        super();
        mongoMsgCatalog = new Messages("Mongo");
        Kernel.getKernel().registerTypeListener(this.getClass().getName(), this);
        log.trace("Registered as a listener with TypeChangeManager ");
    }

    private void resetNeedsFolderHandling() {
        needsFolderHandling = false;
    }

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public boolean containsKey(String ref) {
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        BasicDBObject query = new BasicDBObject();
        query.put(KEY, ref);
        BasicDBObject obj = (BasicDBObject) collection.findOne(query);
        return obj != null;
    }

    @Override
    public long countKeys() throws RaptNotSupportedException {
        return MongoDBFactory.getDB(instanceName).getCollection(tableName).count();
    }

    /**
     * A related key store does not have the special folder handling
     */
    @Override
    public KeyStore createRelatedKeyStore(String relation) {
        Map<String, String> config = new HashMap<String, String>();
        config.put(PREFIX, tableName + "_" + relation);
        MongoDbDataStore related = new MongoDbDataStore();
        related.resetNeedsFolderHandling();
        related.setInstanceName("version".equalsIgnoreCase(relation) && separateVersion ? VERSION : instanceName);
        related.setConfig(config);
        return related;
    }

    @Override
    public boolean delete(String key) {
        DBCollection collection = getCollection();
        BasicDBObject query = new BasicDBObject(KEY, key);
        boolean deleted = null != collection.findAndRemove(query);
        if (deleted && needsFolderHandling) {
            dirRepo.dropFileEntry(key);
        }
        return deleted;
    }

    @Override
    public void setRepoLockHandler(RepoLockHandler repoLockHandler) {
        dirRepo.setRepoLockHandler(repoLockHandler);
    }

    @Override
    public boolean delete(List<String> keys) {
        BasicDBObject query = new BasicDBObject();
        BasicDBObject inClause = new BasicDBObject();
        inClause.append($IN, keys);
        query.append(KEY, inClause);
		try {
			WriteResult result = getCollection().remove(query);
			log.info("Remove "+(result.wasAcknowledged() ? "was" : "was not")+" acknowledged");

	        int deleted = result.getN();
	        if ((deleted != 0) && needsFolderHandling) {
	            for (String key : keys) {
	                dirRepo.dropFileEntry(key);
	            }
	        }
	        return deleted > 0;
		} catch (MongoException me) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, new ExceptionToString(me));
        }

    }

    @Override
    public boolean dropKeyStore() {
        MongoDBFactory.getDB(instanceName).getCollection(tableName).drop();
        dirRepo.drop();
        return true;
    }

    @Override
    public String get(String k) {
        if (log.isDebugEnabled()) {
            log.debug("Get " + k); // Temporary to see what's up
        }
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        BasicDBObject query = new BasicDBObject();
        query.put(KEY, k);
        BasicDBObject obj = (BasicDBObject) collection.findOne(query);
        if (obj != null) {
            Object v = obj.get(VALUE);
            return v.toString();
        }
        return null;
    }

    @Override
    public boolean matches(String key, String value) {
        String val = get(key);
        if (val != null) {
            String tester = StringUtils.deleteWhitespace(JacksonUtil.jsonFromObject(JacksonUtil.getMapFromJson(val)));
            return tester.equals(StringUtils.deleteWhitespace(value));
        } else {
            return false;
        }
    }

    @Override
    public List<String> getBatch(final List<String> keys) {

        MongoRetryWrapper<List<String>> wrapper = new MongoRetryWrapper<List<String>>() {

            public DBCursor makeCursor() {
                DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
                BasicDBObject query = new BasicDBObject();
                BasicDBObject inClause = new BasicDBObject();
                inClause.append($IN, keys);
                query.append(KEY, inClause);
                return collection.find(query);
            }

            public List<String> action(DBCursor cursor) {
                List<String> ret = new ArrayList<String>();
                // We may not get them all, we need to match those that work
                Map<String, String> retMap = new HashMap<String, String>();

                while (cursor.hasNext()) {
                    BasicDBObject obj = (BasicDBObject) cursor.next();
                    if (obj != null) {
                        String v = obj.get(VALUE).toString();
                        String k = obj.get(KEY).toString();
                        retMap.put(k, v);
                    }
                }

                for (String key : keys) {
                    if (retMap.containsKey(key)) {
                        ret.add(retMap.get(key));
                    } else {
                        ret.add(null);
                    }
                }
                return ret;
            }
        };
        return wrapper.doAction();
    }

    private DBObject getFieldObjFromQueryParams(List<String> queryParams) {
        DBObject fieldObj = null;
        // (2) the return fields
        String field = queryParams.get(1);
        if (field != null && !field.isEmpty()) {
            fieldObj = (DBObject) JSON.parse(field);
        }
        return fieldObj;
    }

    private DBObject getQueryObjFromQueryParams(List<String> queryParams) {
        return (DBObject) JSON.parse(queryParams.get(0));
    }

    @Override
    public String getStoreId() {
        return tableName;
    }

    @Override
    public void put(String key, String value) {
        DBCollection collection = getCollection();
        BasicDBObject query = new BasicDBObject(KEY, key);
        BasicDBObject toPut = new BasicDBObject(KEY, key);

        toPut.put(VALUE, value);

        DBObject result = collection.findAndModify(query, null, null, false, toPut, false, true);
        if (needsFolderHandling && result == null) {
            dirRepo.registerParentage(key);
        }
    }

    @Override
    public RaptureQueryResult runNativeQuery(final String repoType, final List<String> queryParams) {
        if (repoType.toUpperCase().equals(MONGODB)) {

            MongoRetryWrapper<RaptureQueryResult> wrapper = new MongoRetryWrapper<RaptureQueryResult>() {

                public DBCursor makeCursor() {
                    // Here we go, the queryParams are basically
                    // (1) the searchCriteria
                    DBObject queryObj = getQueryObjFromQueryParams(queryParams);
                    DBObject fieldObj = getFieldObjFromQueryParams(queryParams);
                    DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
                    return collection.find(queryObj, fieldObj);
                }

                public RaptureQueryResult action(DBCursor cursor) {
                    int skip = 0;
                    int batch = 100;
                    Map<String, Object> options = null;
                    if (queryParams.size() > 2) {
                        options = JacksonUtil.getMapFromJson(queryParams.get(2));
                        if (options.containsKey(SKIP)) {
                            skip = (Integer) options.get(SKIP);
                        }
                        if (options.containsKey(LIMIT)) {
                            batch = (Integer) options.get(LIMIT);
                        }
                    }

                    RaptureQueryResult res = new RaptureQueryResult();
                    // (3) optional extended stuff (such as sort, etc.)
                    if (options != null && options.containsKey(SORT)) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> sortInfo = (Map<String, Object>) options.get(SORT);
                        DBObject sortInfoObject = new BasicDBObject();
                        sortInfoObject.putAll(sortInfo);
                        cursor = cursor.sort(sortInfoObject);
                    }
                    cursor = cursor.skip(skip).limit(batch);
                    // And now setup the values

                    for (DBObject d : cursor.toArray()) {
                        res.addRowContent(new JsonContent(d.toString()));
                    }

                    return res;
                }
            };
            return wrapper.doAction();
        } else {
			throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, mongoMsgCatalog.getMessage("Mismatch", repoType));
        }
    }

    @Override
    public RaptureNativeQueryResult runNativeQueryWithLimitAndBounds(String repoType, final List<String> queryParams, final int limit, final int offset) {
        if (repoType.toUpperCase().equals(MONGODB)) {

            MongoRetryWrapper<RaptureNativeQueryResult> wrapper = new MongoRetryWrapper<RaptureNativeQueryResult>() {

                public DBCursor makeCursor() {
                    // This is like a native query, except that we need to (a)
                    // add in
                    // the displayname (the key) into the
                    // results, and force a limit and offset (so the queryParams
                    // will
                    // only be of size 2).
                    DBObject queryObj = getQueryObjFromQueryParams(queryParams);
                    DBObject fieldObj = getFieldObjFromQueryParams(queryParams);
                    if (!fieldObj.keySet().isEmpty()) {
                        fieldObj.put(KEY, "1");
                    }

                    DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
                    DBCursor cursor = collection.find(queryObj, fieldObj);
                    cursor = cursor.skip(offset).limit(limit);
                    return cursor;
                }

                public RaptureNativeQueryResult action(DBCursor cursor) {
                    RaptureNativeQueryResult res = new RaptureNativeQueryResult();
                    for (DBObject d : cursor.toArray()) {
                        RaptureNativeRow row = new RaptureNativeRow();
                        row.setName(d.get(KEY).toString());
                        row.setContent(new JsonContent(d.get(VALUE).toString()));
                        res.addRowContent(row);
                    }
                    return res;
                }
            };
            return wrapper.doAction();
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "RepoType mismatch. Repo is of type MONGODB, asked for " + repoType);
        }
    }

    @Override
    public void setConfig(Map<String, String> config) {
        tableName = config.get(PREFIX);
        if (config.containsKey(SEPARATE_VERSION)) {
            separateVersion = Boolean.valueOf(config.get(SEPARATE_VERSION));
        }
        String dirName;
        if (tableName == null || tableName.isEmpty()) {
            tableName = "__data__" + instanceName;
            dirName = "__dir__" + instanceName;
        } else {
            dirName = "__dir__" + tableName;
        }

        DBCollection collection = getCollection();
        collection.createIndex(KEY);
        dirRepo.setInstanceName(instanceName);
        Map<String, String> dirConfig = ImmutableMap.of(PREFIX, dirName);
        dirRepo.setConfig(dirConfig);
        dirRepo.setRepoDescription(String.format("Mongo - %s", tableName));
    }

    private DBCollection getCollection() {
        return MongoDBFactory.getDB(instanceName).getCollection(tableName);
    }

    @Override
    public void visit(final String folderPrefix, final RepoVisitor iRepoVisitor) {

        MongoRetryWrapper<Object> wrapper = new MongoRetryWrapper<Object>() {

            public DBCursor makeCursor() {
                DBCollection collection = getCollection();
                BasicDBObject query = new BasicDBObject();
                query.put(KEY, Pattern.compile(folderPrefix));

                return collection.find(query);
            }

            public Object action(DBCursor cursor) {
                while (cursor.hasNext()) {
                    BasicDBObject d = (BasicDBObject) cursor.next();
                    if (!d.getString(KEY).startsWith("$")) {
                        if (!iRepoVisitor.visit(d.getString(KEY), new JsonContent(d.getString(VALUE)), false)) {
                            break;
                        }
                    }
                }
                return null;
            }
        };

        wrapper.doAction();
    }

    @Override
    public void visitKeys(final String prefix, final StoreKeyVisitor iStoreKeyVisitor) {
        MongoRetryWrapper<Object> wrapper = new MongoRetryWrapper<Object>() {

            public DBCursor makeCursor() {
                DBCollection collection = getCollection();
                BasicDBObject query = new BasicDBObject();
                // REGEX on key name, surrounding prefix with \Q and \E means
                // dont interpret any regex chars
                String regex = "^\\Q" + prefix + "\\E";
                query.put(KEY, Pattern.compile(regex));
                return collection.find(query);
            }

            public Object action(DBCursor cursor) {
                while (cursor.hasNext()) {
                    BasicDBObject d = (BasicDBObject) cursor.next();
                    if (!iStoreKeyVisitor.visit(d.getString(KEY), d.getString(VALUE))) {
                        break;
                    }
                }
                return null;
            }
        };

        wrapper.doAction();
    }

    @Override
    public void visitKeysFromStart(final String startPoint, final StoreKeyVisitor iStoreKeyVisitor) {
        MongoRetryWrapper<Object> wrapper = new MongoRetryWrapper<Object>() {

            public DBCursor makeCursor() {
                DBCollection collection = getCollection();
                BasicDBObject query = new BasicDBObject();
                // REGEX on key name?
                String regex = ".*";
                query.put(KEY, Pattern.compile(regex));

                return collection.find(query);
            }

            public Object action(DBCursor cursor) {
                boolean canStart = startPoint == null;
                while (cursor.hasNext()) {
                    BasicDBObject d = (BasicDBObject) cursor.next();
                    if (canStart) {
                        if (!iStoreKeyVisitor.visit(d.getString(KEY), d.getString(VALUE))) {
                            break;
                        }
                    } else {
                        if (d.getString(KEY).equals(startPoint)) {
                            canStart = true;
                        }
                    }
                }
                return null;
            }
        };

        wrapper.doAction();
    }

    @Override
    public List<RaptureFolderInfo> getSubKeys(String prefix) {
        if (needsFolderHandling) {
            return dirRepo.getChildren(prefix);
        } else {
            return null;
        }
    }

    private void removeEntries(List<RaptureFolderInfo> ret, String prefix) {
        List<RaptureFolderInfo> entries = getSubKeys(prefix);
        for (RaptureFolderInfo i : entries) {
            String nextLevel = prefix + "/" + i.getName();
            if (i.isFolder()) {
                removeEntries(ret, nextLevel);
            } else {
                delete(nextLevel);
                RaptureFolderInfo nextRfi = new RaptureFolderInfo();
                nextRfi.setName(nextLevel);
                nextRfi.setFolder(false);
                ret.add(nextRfi);
            }
        }
        RaptureFolderInfo topRfi = new RaptureFolderInfo();
        topRfi.setFolder(true);
        topRfi.setName(prefix);
        ret.add(topRfi);
        dirRepo.dropFolderEntry(prefix);
    }

    @Override
    public List<RaptureFolderInfo> removeSubKeys(String folder, Boolean force) {
        List<RaptureFolderInfo> ret = new ArrayList<RaptureFolderInfo>();
        removeEntries(ret, folder);
        return ret;
    }

    @Override
    public void resetFolderHandling() {
        resetNeedsFolderHandling();
    }

    @Override
    public List<String> getAllSubKeys(String displayNamePart) {
        if (needsFolderHandling) {
            List<RaptureFolderInfo> base = getSubKeys(displayNamePart);
            List<String> result = Lists.newArrayList();
            expandTree(base, result, null);
            return result;
        } else {
            return null;
        }
    }

    @Override
    public IndexHandler createIndexHandler(IndexProducer indexProducer) {
        Map<String, String> config = new HashMap<String, String>();
        config.put(PREFIX, tableName + "_index_index");
        MongoIndexHandler mongoIndexHandler = new MongoIndexHandler();

        mongoIndexHandler.setInstanceName(instanceName);
        mongoIndexHandler.setIndexProducer(indexProducer);
        mongoIndexHandler.setConfig(config);

        mongoIndexHandler.initialize();

        return mongoIndexHandler;
    }

    private void expandTree(List<RaptureFolderInfo> base, List<String> result, String prefix) {
        for (RaptureFolderInfo info : base) {
            String name = prefix == null ? info.getName() : (prefix + "/" + info.getName());
            result.add(name);
            expandTree(getSubKeys(name), result, name);
        }
    }

    @Override
    public Boolean validate() {
        return getCollection() != null;
    }

    @Override
    public long getSize() {
        return getCollection().getStats().getLong("size");
    }

    /**
     * Called when we get a message to indicate that the cache needs updating
     * Since we don't know whether the notification is about a leaf or folder,
     * unregister both. At worst this will cause a redundant save later.
     */
    @Override
    public void signalMessage(NotificationMessage message) {
        // this implementation no longer has state, so we can completely ignore
        // peer notifications on the subject
    }
}
