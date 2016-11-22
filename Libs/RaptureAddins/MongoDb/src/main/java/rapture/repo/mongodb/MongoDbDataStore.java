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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bson.BsonInt32;
import org.bson.Document;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
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
    private static final String $SET = "$set";
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
        MongoCollection<Document> collection = MongoDBFactory.getCollection(instanceName, tableName);
        return collection.count(new Document(KEY, ref)) > 0;
    }

    @Override
    public long countKeys() throws RaptNotSupportedException {
        return MongoDBFactory.getCollection(instanceName, tableName).count();
    }

    /**
     * A related key store does not have the special folder handling
     */
    @Override
    public KeyStore createRelatedKeyStore(String relation) {
        Map<String, String> config = new HashMap<>();
        config.put(PREFIX, tableName + "_" + relation);
        MongoDbDataStore related = new MongoDbDataStore();
        related.resetNeedsFolderHandling();
        related.setInstanceName("version".equalsIgnoreCase(relation) && separateVersion ? VERSION : instanceName);
        related.setConfig(config);
        return related;
    }

    @Override
    public boolean delete(String key) {
        MongoCollection<Document> collection = getCollection();
        Document query = new Document(KEY, key);
        boolean deleted = null != collection.findOneAndDelete(query);
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
        Document inClause = new Document($IN, keys);
        Document query = new Document(KEY, inClause);
        try {
            Document result = getCollection().findOneAndDelete(query);
            if ((result != null) && needsFolderHandling) {
                for (String key : keys) {
                    dirRepo.dropFileEntry(key);
                }
            }
            return result != null;
        } catch (MongoException me) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, new ExceptionToString(me));
        }

    }

    @Override
    public boolean dropKeyStore() {
        MongoDBFactory.getCollection(instanceName, tableName).drop();
        dirRepo.drop();
        return true;
    }

    @Override
    public String get(String k) {
        if (log.isDebugEnabled()) {
            log.debug("Get " + k); // Temporary to see what's up
        }
        MongoCollection<Document> collection = MongoDBFactory.getCollection(instanceName, tableName);
        Document query = new Document();
        query.put(KEY, k);
        FindIterable<Document> obj = collection.find(query).limit(1);
        if (obj != null) {
            for (Document doc : obj) {
                Object v = doc.get(VALUE);
                if (v != null) return v.toString();
            }
        }
        return null;
    }

    @Override
    public boolean matches(String key, String value) {
        String val = get(key);
        if (val == null) return false;
        String valspc = StringUtils.deleteWhitespace(value);

        try {
            return StringUtils.deleteWhitespace(JacksonUtil.jsonFromObject(JacksonUtil.getMapFromJson(val))).equals(valspc);
        } catch (Exception e) {
        }

        return (StringUtils.deleteWhitespace(val).equals(valspc));
    }

    @Override
    public List<String> getBatch(final List<String> keys) {

        MongoRetryWrapper<List<String>> wrapper = new MongoRetryWrapper<List<String>>() {

            @Override
            public FindIterable<Document> makeCursor() {
                MongoCollection<Document> collection = MongoDBFactory.getCollection(instanceName, tableName);
                Document inClause = new Document($IN, keys);
                Document query = new Document(KEY, inClause);
                return collection.find(query);
            }

            @Override
            public List<String> action(FindIterable<Document> cursor) {
                List<String> ret = new ArrayList<>();
                // We may not get them all, we need to match those that work
                Map<String, String> retMap = new HashMap<>();

                for (Document obj : cursor) {
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

    private Document getQueryObjFromQueryParams(List<String> queryParams) {
        return (Document) JSON.parse(queryParams.get(0));
    }

    @Override
    public String getStoreId() {
        return tableName;
    }

    @Override
    public void put(String key, String value) {
        MongoCollection<Document> collection = getCollection();
        Document query = new Document(KEY, key);
        Document toPut = new Document($SET, new Document(KEY, key).append(VALUE, value));

        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.BEFORE);
        Document result = collection.findOneAndUpdate(query, toPut, options);
                
        if (needsFolderHandling && result == null) {
            dirRepo.registerParentage(key);
        }
    }

    @Override
    public RaptureQueryResult runNativeQuery(final String repoType, final List<String> queryParams) {
        if (repoType.toUpperCase().equals(MONGODB)) {

            MongoRetryWrapper<RaptureQueryResult> wrapper = new MongoRetryWrapper<RaptureQueryResult>() {

                @Override
                public FindIterable<Document> makeCursor() {
                    // Here we go, the queryParams are basically
                    // (1) the searchCriteria
                    Document queryObj = getQueryObjFromQueryParams(queryParams);
                    // Document fieldObj =
                    // getFieldObjFromQueryParams(queryParams);
                    MongoCollection<Document> collection = MongoDBFactory.getCollection(instanceName, tableName);
                    FindIterable<Document> find = collection.find(queryObj).batchSize(100);
                    if (queryParams.size() > 2) {
                        Map<String, Object> options = JacksonUtil.getMapFromJson(queryParams.get(2));
                        if (options.containsKey(SKIP)) {
                            find.skip((Integer) options.get(SKIP));
                        }
                        if (options.containsKey(LIMIT)) {
                            find.limit((Integer) options.get(LIMIT));
                        }
                        if (options.containsKey(SORT)) {
                            Map<String, Object> sortInfo = JacksonUtil.getMapFromJson(options.get(SORT).toString());
                            Document sortInfoObject = new Document();
                            sortInfoObject.putAll(sortInfo);
                            find.sort(sortInfoObject);
                        }
                    }
                    return find;
                }

                @Override
                public RaptureQueryResult action(FindIterable<Document> iterable) {
                    RaptureQueryResult res = new RaptureQueryResult();
                    for (Document d : iterable) {
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

                @Override
                public FindIterable<Document> makeCursor() {
                    // This is like a native query, except that we need to (a)
                    // add in
                    // the displayname (the key) into the
                    // results, and force a limit and offset (so the queryParams
                    // will
                    // only be of size 2).
                    Document queryObj = getQueryObjFromQueryParams(queryParams);
                    // Document fieldObj =
                    // getFieldObjFromQueryParams(queryParams);
                    // if (!fieldObj.keySet().isEmpty()) {
                    // fieldObj.put(KEY, "1");
                    // }

                    MongoCollection<Document> collection = MongoDBFactory.getCollection(instanceName, tableName);
                    FindIterable<Document> cursor = collection.find(queryObj);
                    cursor = cursor.skip(offset).limit(limit);
                    return cursor;
                }

                @Override
                public RaptureNativeQueryResult action(FindIterable<Document> iterable) {
                    RaptureNativeQueryResult res = new RaptureNativeQueryResult();
                    for (Document d : iterable) {
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
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, mongoMsgCatalog.getMessage("Mismatch", repoType));
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

        MongoCollection<Document> collection = getCollection();
        collection.createIndex(new Document(KEY, new BsonInt32(1)));
        dirRepo.setInstanceName(instanceName);
        Map<String, String> dirConfig = ImmutableMap.of(PREFIX, dirName);
        dirRepo.setConfig(dirConfig);
        dirRepo.setRepoDescription(String.format("Mongo - %s", tableName));
    }

    private MongoCollection<Document> getCollection() {
        return MongoDBFactory.getCollection(instanceName, tableName);
    }

    @Override
    public void visit(final String folderPrefix, final RepoVisitor iRepoVisitor) {

        MongoRetryWrapper<Object> wrapper = new MongoRetryWrapper<Object>() {

            @Override
            public FindIterable<Document> makeCursor() {
                MongoCollection<Document> collection = getCollection();
                Document query = new Document(KEY, Pattern.compile(folderPrefix));

                return collection.find(query);
            }

            @Override
            public Object action(FindIterable<Document> iterable) {
                Iterator<Document> cursor = iterable.iterator();
                while (cursor.hasNext()) {
                    Document d = cursor.next();
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

            @Override
            public FindIterable<Document> makeCursor() {
                MongoCollection<Document> collection = getCollection();
                // REGEX on key name, surrounding prefix with \Q and \E means
                // dont interpret any regex chars
                String regex = "^\\Q" + prefix + "\\E";
                Document query = new Document(KEY, Pattern.compile(regex));
                return collection.find(query);
            }

            @Override
            public Object action(FindIterable<Document> iterable) {
                Iterator<Document> cursor = iterable.iterator();
                while (cursor.hasNext()) {
                    Document d = cursor.next();
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

            @Override
            public FindIterable<Document> makeCursor() {
                MongoCollection<Document> collection = getCollection();
                // REGEX on key name?
                String regex = ".*";
                Document query = new Document(KEY, Pattern.compile(regex));
                return collection.find(query);
            }

            @Override
            public Object action(FindIterable<Document> iterable) {
                Iterator<Document> cursor = iterable.iterator();
                boolean canStart = startPoint == null;
                while (cursor.hasNext()) {
                    Document d = cursor.next();
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
        List<RaptureFolderInfo> ret = new ArrayList<>();
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
        Map<String, String> config = new HashMap<>();
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
        return getCollection().count();
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
