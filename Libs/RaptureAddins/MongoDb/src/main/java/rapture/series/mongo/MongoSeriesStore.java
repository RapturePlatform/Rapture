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

import rapture.common.RaptureFolderInfo;
import rapture.common.SeriesValue;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.dsl.serfun.DecimalSeriesValue;
import rapture.dsl.serfun.LongSeriesValue;
import rapture.dsl.serfun.StringSeriesValue;
import rapture.dsl.serfun.StructureSeriesValueImpl;
import rapture.mongodb.MongoDBFactory;
import rapture.mongodb.MongoRetryWrapper;
import rapture.series.SeriesPaginator;
import rapture.series.SeriesStore;
import rapture.series.children.ChildKeyUtil;
import rapture.series.children.ChildrenRepo;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

/**
 * Mongo implementation of the storage for series api.
 *
 * @author mel
 */
public class MongoSeriesStore implements SeriesStore {
    private String instanceName = "default";
    private String tableName;
    public static final String ROWKEY = "row";
    public static final String COLKEY = "col";
    public static final String VALKEY = "val";
    private Cache<String, Boolean> keyCache = CacheBuilder.newBuilder().expireAfterAccess(3, TimeUnit.SECONDS).build();
    private final ChildrenRepo childrenRepo;
    private static Logger log = Logger.getLogger(MongoSeriesStore.class);

    public MongoSeriesStore() {
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
        DBCollection collection = getCollection(null);
        collection.drop();
        keyCache.invalidateAll();
    }

    @Override
    public void addDoubleToSeries(String key, String column, double value) {
        saveDBObject(key, column, value);
    }

    @Override
    public void addLongToSeries(String key, String column, long value) {
        saveDBObject(key, column, value);
    }

    @Override
    public void addStringToSeries(String key, String column, String value) {
        saveDBObject(key, column, "'" + value);
    }

    @Override
    public void addStructureToSeries(String key, String column, String json) {
        saveDBObject(key, column, json);
    }

    @Override
    public void addPointToSeries(String key, SeriesValue value) {
        if (value.isDouble()) addDoubleToSeries(key, value.getColumn(), value.asDouble());
        else if (value.isLong()) addLongToSeries(key, value.getColumn(), value.asLong());
        else if (value.isString()) addStringToSeries(key, value.getColumn(), value.asString());
        else if (value.isStructure()) addStructureToSeries(key, value.getColumn(), value.asString());
        else throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Value has no encoder in MongoSeriesStore: " + value.asString());
    }

    private void saveDBObject(String key, String column, Object val) {
        registerKey(key);
        DBCollection collection = getCollection(key);
        DBObject dbkey = new BasicDBObject(ROWKEY, key).append(COLKEY, column);
        DBObject dbval = new BasicDBObject(ROWKEY, key).append(COLKEY, column).append(VALKEY, val);
        WriteResult result = collection.update(dbkey, dbval, true, false);
        if (!result.getCachedLastError().ok()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, result.getCachedLastError().getErrorMessage());
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
            throw RaptureExceptionFactory.create("Severe: 'return false' failed in key cache");
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
        boolean result = true;
        while (col.hasNext()) {
            String column = col.next();
            T value = val.next();
            if (column == null) nullKey = true;
            else c.go(key, column, value);
        }
        if (nullKey) throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Column Key may not be null, other values added");
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
        DBCollection collection = getCollection(key);
        for (String pointKey : pointKeys) {
            DBObject victim = new BasicDBObject(ROWKEY, key).append(COLKEY, pointKey);
            WriteResult result = collection.remove(victim);
            if (!result.getCachedLastError().ok()) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, result.getCachedLastError().getErrorMessage());
            }
        }
        return true;
    }

    @Override
    public void deletePointsFromSeries(String key) {
        unregisterKey(key);
        DBCollection collection = getCollection(key);
        DBObject victim = new BasicDBObject(ROWKEY, key);
        WriteResult result = collection.remove(victim);
        if (!result.getCachedLastError().ok()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, result.getCachedLastError().getErrorMessage());
        }
    }

    @Override
    public List<SeriesValue> getPoints(final String key) {

        MongoRetryWrapper<List<SeriesValue>> wrapper = new MongoRetryWrapper<List<SeriesValue>>() {

            public DBCursor makeCursor() {
                DBObject query = new BasicDBObject(ROWKEY, key);
                DBCollection collection = getCollection(key);
                return collection.find(query);
            }

            public List<SeriesValue> action(DBCursor cursor) {
                List<SeriesValue> result = Lists.newArrayList();
                for (DBObject entry : cursor) {
                    SeriesValue bolt = makeSeriesValue(entry);
                    result.add(bolt);
                }
                return result;
            }
        };
        return wrapper.doAction();
    }

    private SeriesValue makeSeriesValue(DBObject entry) {
        Object val = entry.get(VALKEY);
        String col = (String) entry.get(COLKEY);
        if (val instanceof Double) return new DecimalSeriesValue(((Double) val).doubleValue(), col);
        if (val instanceof String) return decodeString((String) val, col);
        if (val instanceof Long) return new LongSeriesValue(((Long) val).longValue(), col);
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Unkown type in MongoSeriesStore");
    }

    private SeriesValue decodeString(String val, String col) {
        if (val.startsWith("'")) return new StringSeriesValue(val.substring(1), col);
        try {
            return StructureSeriesValueImpl.unmarshal(val, col);
        } catch (JsonParseException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Error parsing json value " + val, e);

        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Error parsing json value " + val, e);
        }
    }

    @Override
    public List<SeriesValue> getPointsAfter(final String key, final String startColumn, final int maxNumber) {
        MongoRetryWrapper<List<SeriesValue>> wrapper = new MongoRetryWrapper<List<SeriesValue>>() {

            public DBCursor makeCursor() {
                DBCollection collection = getCollection(key);
                DBObject query = new BasicDBObject(ROWKEY, key).append(COLKEY, new BasicDBObject("$gte", startColumn));
                return collection.find(query);
            }

            public List<SeriesValue> action(DBCursor cursor) {
                List<SeriesValue> result = Lists.newArrayList();
                int count = 0;
                for (DBObject entry : cursor) {
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

            public DBCursor makeCursor() {
                DBCollection collection = getCollection(key);
                DBObject query = new BasicDBObject(ROWKEY, key).append(COLKEY, new BasicDBObject("$lte", startColumn));
                BasicDBObject sort = new BasicDBObject(COLKEY, -1);
                return collection.find(query).sort(sort);
            }

            public List<SeriesValue> action(DBCursor cursor) {
                int count = 0;
                List<SeriesValue> result = Lists.newArrayList();
                for (DBObject entry : cursor) {
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

            public DBCursor makeCursor() {
                DBCollection collection = getCollection(key);
                DBObject query = new BasicDBObject(ROWKEY, key).append(COLKEY, new BasicDBObject("$gte", startColumn).append("$lte", endColumn));
                return collection.find(query);
            }

            public List<SeriesValue> action(DBCursor cursor) {
                int count = 0;
                List<SeriesValue> result = Lists.newArrayList();
                for (DBObject entry : cursor) {
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

    @Override
    public void setConfig(Map<String, String> config) {
        // TODO MEL No options for you! We will choose everything.
        tableName = config.get("prefix");
        log.debug("Table name is " + tableName + ", instance name is " + instanceName);
        if (tableName == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Madatory config 'prefix' missing for MongoSeriesStore");
        }
        // WARNING: Fragile code assumes setInstanceName is called BEFORE
        // setConfig
        MongoDBFactory.getCollection(instanceName, tableName).ensureIndex(INDEX_KEYS, INDEX_OPTS);
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

    private static final DBObject INDEX_KEYS = new BasicDBObject(ROWKEY, 1).append(COLKEY, 1);
    private static final DBObject INDEX_OPTS = new BasicDBObject("unique", true);

    private DBCollection getCollection(String checkAllTheCallersIfYouStopIgnoringThisParameter) {
        DBCollection result = MongoDBFactory.getCollection(instanceName, tableName);
        return result;
    }

    @Override
    public SeriesValue getLastPoint(final String key) {

        MongoRetryWrapper<SeriesValue> wrapper = new MongoRetryWrapper<SeriesValue>() {

            public DBCursor makeCursor() {
                DBCollection collection = getCollection(key);
                DBObject query = new BasicDBObject(ROWKEY, key);
                BasicDBObject sort = new BasicDBObject(COLKEY, -1);
                return collection.find(query).sort(sort).limit(1);
            }

            public SeriesValue action(DBCursor cursor) {
                return cursor.hasNext() ? makeSeriesValue(cursor.next()) : null;
            }
        };
        return wrapper.doAction();
    }
}
