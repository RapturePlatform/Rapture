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
package rapture.series.mongo;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.bson.Document;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;

import rapture.common.Messages;
import rapture.common.RaptureFolderInfo;
import rapture.common.SeriesValue;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.dsl.serfun.DecimalSeriesValue;
import rapture.dsl.serfun.LongSeriesValue;
import rapture.dsl.serfun.StringSeriesValue;
import rapture.dsl.serfun.StructureSeriesValueImpl;
import rapture.mongodb.MongoRetryWrapper;
import rapture.mongodb.MongoDBFactory;
import rapture.series.SeriesPaginator;
import rapture.series.SeriesStore;
import rapture.series.children.ChildKeyUtil;
import rapture.series.children.ChildrenRepo;

/**
 * Mongo implementation of the storage for series api.
 *
 * @author mel
 */
public class MongoSeriesStore implements SeriesStore {
    private String instanceName = "default";
    private String tableName;
    private static final String $SET = "$set";
    public static final String ROWKEY = "row";
    public static final String COLKEY = "col";
    public static final String VALKEY = "val";
    private Cache<String, Boolean> keyCache = CacheBuilder.newBuilder().expireAfterAccess(3, TimeUnit.SECONDS).build();
    private final ChildrenRepo childrenRepo;
    private static Logger log = Logger.getLogger(MongoSeriesStore.class);

    private Messages mongoMsgCatalog;

    public MongoSeriesStore() {
        mongoMsgCatalog = new Messages("Mongo");
        this.childrenRepo = new ChildrenRepo() {
            @Override
            public List<SeriesValue> getPoints(String key) {
                return MongoSeriesStore.this.getPoints(key);
            }

            @Override
            public boolean addPoint(String key, SeriesValue value) {
                addPointToSeries(key, value);
                return true;

            }

            @Override
            public boolean dropPoints(String key, List<String> points) {
                return deletePointsFromSeriesByPointKey(key, points);
            }

            @Override
            public void dropRow(String key) {
                deletePointsFromSeries(key);
            }
        };
    }

    @Override
    public void drop() {
        MongoCollection<Document> collection = getCollection(null);
        collection.drop();
        keyCache.invalidateAll();
    }

    @Override
    public void addDoubleToSeries(String key, String column, double value) {
        saveDocument(key, column, value);
    }

    @Override
    public void addLongToSeries(String key, String column, long value) {
        saveDocument(key, column, value);
    }

    @Override
    public void addStringToSeries(String key, String column, String value) {
        saveDocument(key, column, "'" + value);
    }

    @Override
    public void addStructureToSeries(String key, String column, String json) {
        saveDocument(key, column, json);
    }

    @Override
    public void addPointToSeries(String key, SeriesValue value) {
        if (value.isDouble()) addDoubleToSeries(key, value.getColumn(), value.asDouble());
        else if (value.isLong()) addLongToSeries(key, value.getColumn(), value.asLong());
        else if (value.isString()) addStringToSeries(key, value.getColumn(), value.asString());
        else if (value.isStructure()) addStructureToSeries(key, value.getColumn(), value.asString());
        else throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, mongoMsgCatalog.getMessage("NoEncoder", value.asString()));
    }

    private void saveDocument(String key, String column, Object val) {
        registerKey(key);
        MongoCollection<Document> collection = getCollection(key);
        Document dbkey = new Document(ROWKEY, key).append(COLKEY, column);
        Document dbval = new Document($SET, new Document(ROWKEY, key).append(COLKEY, column).append(VALKEY, val));
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER);
        try {
            @SuppressWarnings("unused")
            Document ret = collection.findOneAndUpdate(dbkey, dbval, options);
        } catch (MongoException me) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, new ExceptionToString(me));
        }
    }

    private static final String DIRECTORY_KEY = "..directory";
    private Callable<Boolean> FALSE_CALL = new Callable<Boolean>() {
        public Boolean call() {
            return false;
        }
    };

    private void registerKey(String key) {
        if (DIRECTORY_KEY.equals(key) || ChildKeyUtil.isRowKey(key)) {
            return;
        } else try {
            if (keyCache.get(key, FALSE_CALL) == false) {
                addPointToSeries(DIRECTORY_KEY, new StringSeriesValue(".", key));
                childrenRepo.registerParentage(key);
                keyCache.put(key, true);
            }
        } catch (ExecutionException e) {
            // this should be impossible
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, new ExceptionToString(e));
        }
    }

    public void unregisterKey(String key) {
        unregisterKey(key, false);
    }

    public void unregisterKey(String key, boolean isFolder) {
        if (DIRECTORY_KEY.equals(key)) {
            return;
        }
        deletePointsFromSeriesByPointKey(DIRECTORY_KEY, ImmutableList.of(key));
        if (isFolder) childrenRepo.dropFolderEntry(key);
        else childrenRepo.dropFileEntry(key);
        keyCache.invalidate(key);
    }

    private <T> void multiAdd(String key, List<String> columns, List<T> values, Callback<T> c) {
        boolean nullKey = false;
        Preconditions.checkArgument(columns.size() == values.size());
        Iterator<String> col = columns.iterator();
        Iterator<T> val = values.iterator();
        while (col.hasNext()) {
            String column = col.next();
            T value = val.next();
            if (column == null) nullKey = true;
            else c.go(key, column, value);
        }
        if (nullKey) throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, mongoMsgCatalog.getMessage("NullKey"));
    }

    @Override
    public void addDoublesToSeries(String key, List<String> columns, List<Double> values) {
        multiAdd(key, columns, values, multiDouble);
    }

    @Override
    public void addLongsToSeries(String key, List<String> columns, List<Long> values) {
        multiAdd(key, columns, values, multiLong);
    }

    @Override
    public void addStringsToSeries(String key, List<String> columns, List<String> values) {
        multiAdd(key, columns, values, multiString);
    }

    @Override
    public void addStructuresToSeries(String key, List<String> columns, List<String> values) {
        multiAdd(key, columns, values, multiStruct);
    }

    @Override
    public void addPointsToSeries(String key, List<SeriesValue> values) {
        Iterator<SeriesValue> val = values.iterator();
        while (val.hasNext()) {
            multiPoint.go(key, null, val.next());
        }
    }

    private interface Callback<T> {
        public void go(String key, String column, T value);
    }

    private Callback<Double> multiDouble = new Callback<Double>() {
        public void go(String key, String column, Double value) {
            addDoubleToSeries(key, column, value);
        }
    };

    private Callback<Long> multiLong = new Callback<Long>() {
        public void go(String key, String column, Long value) {
            addLongToSeries(key, column, value);
        }
    };

    private Callback<String> multiString = new Callback<String>() {
        public void go(String key, String column, String value) {
            addStringToSeries(key, column, value);
        }
    };

    private Callback<String> multiStruct = new Callback<String>() {
        public void go(String key, String column, String value) {
            addStructureToSeries(key, column, value);
        }
    };

    private Callback<SeriesValue> multiPoint = new Callback<SeriesValue>() {
        public void go(String key, String column, SeriesValue value) {
            if (value.isDouble()) addDoubleToSeries(key, value.getColumn(), value.asDouble());
            else if (value.isLong()) addLongToSeries(key, value.getColumn(), value.asLong());
            else if (value.isString()) addStringToSeries(key, value.getColumn(), value.asString());
            else if (value.isStructure()) addStructureToSeries(key, value.getColumn(), value.asString());
        }
    };

    @Override
    public Boolean deletePointsFromSeriesByPointKey(String key, List<String> pointKeys) {
        MongoCollection<Document> collection = getCollection(key);
        for (String pointKey : pointKeys) {
            Document victim = new Document(ROWKEY, key).append(COLKEY, pointKey);
            try {
                DeleteResult result = collection.deleteMany(victim);
                log.info("Remove " + (result.wasAcknowledged() ? "was" : "was not") + " acknowledged");
            } catch (MongoException me) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, new ExceptionToString(me));
            }
        }
        return true;
    }

    @Override
    public void deletePointsFromSeries(String key) {
        unregisterKey(key);
        MongoCollection<Document> collection = getCollection(key);
        Document victim = new Document(ROWKEY, key);
        try {
            DeleteResult result = collection.deleteMany(victim);
            log.info("Remove " + (result.wasAcknowledged() ? "was" : "was not") + " acknowledged");
        } catch (MongoException me) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, new ExceptionToString(me));
        }
    }

    @Override
    public List<SeriesValue> getPoints(final String key) {

        MongoRetryWrapper<List<SeriesValue>> wrapper = new MongoRetryWrapper<List<SeriesValue>>() {

            public FindIterable<Document> makeCursor() {
                Document query = new Document(ROWKEY, key);
                MongoCollection<Document> collection = getCollection(key);
                FindIterable<Document> cursor = collection.find(query);
                if (cursor == null)
                    log.info("No points found for "+key);
                return cursor;
            }

            public List<SeriesValue> action(FindIterable<Document> cursor) {
                if (cursor == null) return null;
                List<SeriesValue> result = Lists.newArrayList();
                for (Document entry : cursor) {
                    SeriesValue bolt = makeSeriesValue(entry);
                    result.add(bolt);
                }
                return result;
            }
        };
        return wrapper.doAction();
    }

    private SeriesValue makeSeriesValue(Document entry) {
        Object val = entry.get(VALKEY);
        String col = (String) entry.get(COLKEY);
        if (val instanceof Double) return new DecimalSeriesValue(((Double) val).doubleValue(), col);
        if (val instanceof String) return decodeString((String) val, col);
        if (val instanceof Long) return new LongSeriesValue(((Long) val).longValue(), col);
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, mongoMsgCatalog.getMessage("UnknownType"));
    }

    private SeriesValue decodeString(String val, String col) {
        if (val.startsWith("'")) return new StringSeriesValue(val.substring(1), col);
        try {
            return StructureSeriesValueImpl.unmarshal(val, col);
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, mongoMsgCatalog.getMessage("JsonError", val));
        }
    }

    @Override
    public List<SeriesValue> getPointsAfter(final String key, final String startColumn, final int maxNumber) {
        MongoRetryWrapper<List<SeriesValue>> wrapper = new MongoRetryWrapper<List<SeriesValue>>() {

            public FindIterable<Document> makeCursor() {
                MongoCollection<Document> collection = getCollection(key);
                Document query = new Document(ROWKEY, key).append(COLKEY, new Document("$gte", startColumn));
                FindIterable<Document> cursor = collection.find(query);
                if (cursor == null)
                    log.info("No points found for "+key);
                return cursor;
            }

            public List<SeriesValue> action(FindIterable<Document> cursor) {
                if (cursor == null) return null;
                List<SeriesValue> result = Lists.newArrayList();
                int count = 0;
                for (Document entry : cursor) {
                    if (count >= maxNumber) break;
                    SeriesValue bolt = makeSeriesValue(entry);
                    result.add(bolt);
                    count++;
                }
                return result;
            }
        };
        return wrapper.doAction();
    }

    @Override
    public List<SeriesValue> getPointsAfterReverse(final String key, final String startColumn, final int maxNumber) {
        MongoRetryWrapper<List<SeriesValue>> wrapper = new MongoRetryWrapper<List<SeriesValue>>() {

            public FindIterable<Document> makeCursor() {
                MongoCollection<Document> collection = getCollection(key);
                Document query = new Document(ROWKEY, key).append(COLKEY, new Document("$lte", startColumn));
                Document sort = new Document(COLKEY, -1);
                FindIterable<Document> cursor = collection.find(query).sort(sort);
                if (cursor == null)
                    log.info("No points found for "+key);
                return cursor;
            }

            public List<SeriesValue> action(FindIterable<Document> cursor) {
                if (cursor == null) return null;
                int count = 0;
                List<SeriesValue> result = Lists.newArrayList();
                for (Document entry : cursor) {
                    if (count >= maxNumber) break;
                    SeriesValue bolt = makeSeriesValue(entry);
                    result.add(bolt);
                    count++;
                }
                return result;
            }
        };
        return wrapper.doAction();
    }

    @Override
    public List<SeriesValue> getPointsAfter(final String key, final String startColumn, final String endColumn, final int maxNumber) {
        MongoRetryWrapper<List<SeriesValue>> wrapper = new MongoRetryWrapper<List<SeriesValue>>() {

            public FindIterable<Document> makeCursor() {
                MongoCollection<Document> collection = getCollection(key);
                Document query = new Document(ROWKEY, key).append(COLKEY, new Document("$gte", startColumn).append("$lte", endColumn));
                FindIterable<Document> cursor = collection.find(query);
                if (cursor == null)
                    log.info("No points found for "+key);
                return cursor;
            }

            public List<SeriesValue> action(FindIterable<Document> cursor) {
                if (cursor == null) return null;
                int count = 0;
                List<SeriesValue> result = Lists.newArrayList();
                for (Document entry : cursor) {
                    if (count >= maxNumber) break;
                    SeriesValue bolt = makeSeriesValue(entry);
                    result.add(bolt);
                    count++;
                }
                return result;
            }
        };
        return wrapper.doAction();
    }

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    private static final Document INDEX_KEYS = new Document(ROWKEY, 1).append(COLKEY, 1);
    private static final IndexOptions INDEX_OPTS = new IndexOptions().unique(true);

    @Override
    public void setConfig(Map<String, String> config) {
        // TODO MEL No options for you! We will choose everything.
        tableName = config.get("prefix");
        log.debug("Table name is " + tableName + ", instance name is " + instanceName);
        if (tableName == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, mongoMsgCatalog.getMessage("NoPrefix"));
        }
        // WARNING: Fragile code assumes setInstanceName is called BEFORE
        // setConfig
        MongoDBFactory.getCollection(instanceName, tableName).createIndex(INDEX_KEYS, INDEX_OPTS);
    }

    @Override
    public List<String> getSeriesLike(String keyPrefix) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<SeriesValue> getRangeAsIteration(String key, String startCol, String endCol, int pageSize) {
        return new SeriesPaginator(key, startCol, endCol, pageSize, this);
    }

    @Override
    public List<SeriesValue> getRangeAsList(String key, String startCol, String endCol) {
        return getPointsAfter(key, startCol, endCol, Integer.MAX_VALUE);
    }

    @Override
    public List<RaptureFolderInfo> listSeriesByUriPrefix(String folderName) {
        return childrenRepo.getChildren(folderName);
    }

    private MongoCollection<Document> getCollection(String checkAllTheCallersIfYouStopIgnoringThisParameter) {
        MongoCollection<Document> result = MongoDBFactory.getCollection(instanceName, tableName);
        return result;
    }

    @Override
    public SeriesValue getLastPoint(final String key) {

        MongoRetryWrapper<SeriesValue> wrapper = new MongoRetryWrapper<SeriesValue>() {

            public FindIterable<Document> makeCursor() {
                MongoCollection<Document> collection = getCollection(key);
                Document query = new Document(ROWKEY, key);
                Document sort = new Document(COLKEY, -1);
                FindIterable<Document> cursor = collection.find(query).sort(sort).limit(1);
                if (cursor == null)
                    log.info("No points found for "+key);
                return cursor;
            }

            public SeriesValue action(FindIterable<Document> cursor) {
                if (cursor == null) return null;
                Iterator<Document> iterator = cursor.iterator();
                return iterator.hasNext() ? makeSeriesValue(iterator.next()) : null;
            }
        };
        return wrapper.doAction();
    }

    @Override
    public void createSeries(String key) {
        registerKey(key);
    }

    @Override
    public void deleteSeries(String key) {
        unregisterKey(key);
        deletePointsFromSeries(key);
    }
}
