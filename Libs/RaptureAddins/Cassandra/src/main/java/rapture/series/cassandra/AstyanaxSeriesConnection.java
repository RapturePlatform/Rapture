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
package rapture.series.cassandra;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import rapture.cassandra.AstyanaxCassandraBase;
import rapture.common.RaptureFolderInfo;
import rapture.common.SeriesValue;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.dsl.serfun.SeriesValueCodec;
import rapture.dsl.serfun.StringSeriesValue;
import rapture.series.children.ChildKeyUtil;
import rapture.series.children.ChildrenRepo;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.OperationException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.util.RangeBuilder;

public class AstyanaxSeriesConnection extends AstyanaxCassandraBase {
    private static final String DIRECTORY_KEY = "..directory";
    private final int OVERFLOW_LIMIT;
    private Cache<String, Boolean> keyCache = CacheBuilder.newBuilder().expireAfterAccess(3, TimeUnit.SECONDS).build();
    private final ChildrenRepo childrenRepo;

    public AstyanaxSeriesConnection(String instance, Map<String, String> config, int overflowLimit) {
        super(instance, config);
        OVERFLOW_LIMIT = overflowLimit;
        this.childrenRepo = new ChildrenRepo() {

            @Override
            public List<SeriesValue> getPoints(String key) {
                try {
                    return AstyanaxSeriesConnection.this.getPoints(key);
                } catch (IOException e) {
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), e);
                }
            }

            @Override
            public boolean addPoint(String key, SeriesValue value) {
                AstyanaxSeriesConnection.this.addPoint(key, value);
                return true;
            }

            @Override
            public boolean dropPoints(String key, List<String> points) {
                return AstyanaxSeriesConnection.this.dropPoints(key, points);
            }

            @Override
            public void dropRow(String key) {
                AstyanaxSeriesConnection.this.dropAllPoints(key);
            }
        };
    }

    public void drop() throws OperationException, ConnectionException {
        keyCache.invalidateAll();
        keyspace.truncateColumnFamily(columnFamily);
    }

    private SeriesValue makeSeriesValueFromByteArray(String column, byte[] array) throws IOException {
        return SeriesValueCodec.decode(column, array);
    }

    public void addPoint(String key, SeriesValue value) {
        registerKey(key);
        try {
            keyspace.prepareColumnMutation(columnFamily, key, value.getColumn()).putValue(SeriesValueCodec.encodeValue(value), null).execute();
        } catch (ConnectionException | UnsupportedEncodingException ce) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), ce);
        }
    }

    public Boolean dropPoints(String key, List<String> columns) {
        MutationBatch m = keyspace.prepareMutationBatch();
        for (String columnName : columns) {
            m.withRow(columnFamily, key).deleteColumn(columnName);
        }

        try {
            m.execute();
        } catch (ConnectionException ce) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), ce);
        }
        return true;
    }

    public void dropAllPoints(String key) {
        unregisterKey(key);
        MutationBatch m = keyspace.prepareMutationBatch();
        m.withRow(columnFamily, key).delete();

        try {
            m.execute();
        } catch (ConnectionException ce) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), ce);
        }
    }

    public List<SeriesValue> getPoints(String key) throws IOException {
        String startColumn = "";
        return getPointsAfter(key, startColumn, Integer.MAX_VALUE, false);
    }

    public List<SeriesValue> getPointsAfter(String key, String startColumn, int maxNumber, boolean reverse) throws IOException {
        return getPointsAfter(key, startColumn, "", maxNumber, reverse);
    }

    public List<SeriesValue> getPointsAfter(String key, String startColumn, String endColumn, int maxNumber, boolean reverse) throws IOException {
        List<SeriesValue> ret = new ArrayList<SeriesValue>();
        int limit = (maxNumber > OVERFLOW_LIMIT) ? OVERFLOW_LIMIT + 1 : maxNumber;
        ColumnList<String> result;
        try {
            result = keyspace.prepareQuery(columnFamily).getKey(key)
                    .withColumnRange(new RangeBuilder().setStart(startColumn).setEnd(endColumn).setLimit(limit).setReversed(reverse).build()).execute()
                    .getResult();
        } catch (ConnectionException ce) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), ce);
        }
        if (result.size() > OVERFLOW_LIMIT) {
            throw RaptureExceptionFactory.create(messageCatalog.getMessage("SmallerPages", ""+OVERFLOW_LIMIT));
        }
        for (Column<String> column : result) {
            ret.add(makeSeriesValue(column));
        }
        return ret;
    }

    public List<String> getSeriesLike(String keyPrefix) throws IOException {
        if (Strings.isNullOrEmpty(keyPrefix)) {
            List<SeriesValue> listings = getPoints(DIRECTORY_KEY);
            return Lists.transform(listings, colFunc);
        } else {
            int lastCharValue = keyPrefix.charAt(keyPrefix.length() - 1) + 1;
            String endPrefix = keyPrefix.substring(0, keyPrefix.length() - 1) + ((char) lastCharValue);
            List<SeriesValue> listings = getPointsAfter(DIRECTORY_KEY, keyPrefix, endPrefix, Integer.MAX_VALUE, false);
            List<String> result = Lists.transform(listings, colFunc);
            if (result.size() > 0 && endPrefix.equals(result.get(result.size() - 1))) {
                result.subList(result.size() - 1, result.size()).clear();
            }
            return result;
        }
    }

    private static Function<SeriesValue, String> colFunc = new Function<SeriesValue, String>() {
        @Override
        public String apply(SeriesValue v) {
            return v.getColumn();
        }
    };

    public void addPoint(String key, List<SeriesValue> values) {
        boolean nullKey = false;
        try {
            registerKey(key);
            MutationBatch m = keyspace.prepareMutationBatch();
            ColumnListMutation<String> mut = m.withRow(columnFamily, key);
            for (SeriesValue value : values) {
                if (value.getColumn() == null) nullKey = true;
                else mut.putColumn(value.getColumn(), SeriesValueCodec.encodeValue(value), null);
            }
            m.execute();
        } catch (ConnectionException ce) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), ce);
        } catch (UnsupportedEncodingException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("BadSeriesValue"), e);
        }
        if (nullKey) throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, messageCatalog.getMessage("BadKey"));
    }

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
                addPoint(DIRECTORY_KEY, new StringSeriesValue(".", key));
                childrenRepo.registerParentage(key);
                keyCache.put(key, true);
            }
        } catch (ExecutionException e) {
            // this should never ever happen
            throw RaptureExceptionFactory.create("Severe: the line 'return false;' just failed");
        }
    }

    void unregisterKey(String key) {
        unregisterKey(key, false);
    }

    void unregisterKey(String key, boolean isFolder) {
        if (DIRECTORY_KEY.equals(key)) {
            return;
        }
        dropPoints(DIRECTORY_KEY, ImmutableList.of(key));
        if (isFolder) childrenRepo.dropFolderEntry(key);
        else childrenRepo.dropFileEntry(key);
        keyCache.invalidate(key);
    }

    private final SeriesValue makeSeriesValue(Column<String> column) throws IOException {
        return makeSeriesValueFromByteArray(column.getName(), column.getByteArrayValue());
    }

    public List<RaptureFolderInfo> getChildren(String folderName) {
        return childrenRepo.getChildren(folderName);
    }
}
