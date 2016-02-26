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
package rapture.series.cassandra;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ColumnPath;
import org.apache.cassandra.thrift.Deletion;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.thrift.Mutation;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.SliceRange;
import org.apache.cassandra.thrift.TimedOutException;
import org.apache.cassandra.thrift.UnavailableException;
import org.apache.thrift.TException;

import rapture.cassandra.CassandraBase;
import rapture.common.RaptureFolderInfo;
import rapture.common.SeriesValue;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.dsl.serfun.SeriesValueCodec;
import rapture.dsl.serfun.StringSeriesValue;
import rapture.series.children.ChildKeyUtil;
import rapture.series.children.ChildrenRepo;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @deprecated - Use AstyanaxSeriesConnection instead
 * @author dukenguyen
 * 
 */
@Deprecated
public class CassSeriesConnection extends CassandraBase {
    private static final String DIRECTORY_KEY = "..directory";

    private final ChildrenRepo childrenRepo;

    public CassSeriesConnection(String instance, Map<String, String> config) {
        super(instance, config);
        this.childrenRepo = new ChildrenRepo() {

            @Override
            public List<SeriesValue> getPoints(String key) {
                try {
                    return CassSeriesConnection.this.getPoints(key);
                } catch (InvalidRequestException | UnavailableException | TimedOutException | TException | IOException e) {
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), e);  //$NON-NLS-1$
                }
            }

            @Override
            public boolean addPoint(String key, SeriesValue value) {
                return CassSeriesConnection.this.addPoint(key, value);

            }

            @Override
            public boolean dropPoints(String key, List<String> points) {
                return CassSeriesConnection.this.dropPoints(key, points);
            }

            @Override
            public void dropRow(String key) {
                try {
                    CassSeriesConnection.this.dropAllPoints(key);
                } catch (Exception e) {
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), e);
                }
            }
        };
    }

    public void drop() throws InvalidRequestException, UnavailableException, TimedOutException, TException {
        client.truncate(columnFamily);
    }

    private SeriesValue makeSeriesValueFromByteArray(String column, byte[] array) throws IOException {
        return SeriesValueCodec.decode(column, array);
    }

    private static final String UTF8 = "UTF-8";

    public Boolean addPoint(String key, SeriesValue value) throws RaptureException {
        try {
            registerKey(key);
            ColumnParent colPathName = new ColumnParent(columnFamily);
            Column valColumn = new Column();
            valColumn.setName(value.getColumn().getBytes(UTF8));
            valColumn.setValue(SeriesValueCodec.encodeValue(value));
            valColumn.setTimestamp(System.currentTimeMillis());
            client.insert(ByteBuffer.wrap(key.getBytes(UTF8)), colPathName, valColumn, getWriteCL());
            return true;
        } catch (TException | UnsupportedEncodingException | InvalidRequestException | UnavailableException | TimedOutException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), e);
        }
    }

    public Boolean dropPoints(String key, List<String> columns) {
        try {
            // Similar to addPoints, use a mutation, but in this case use a
            // delete
            // mutation
            List<Mutation> removal_list = new ArrayList<Mutation>();
            List<ByteBuffer> cols = new ArrayList<ByteBuffer>();
            for (String column : columns) {
                cols.add(ByteBuffer.wrap(column.getBytes(UTF8)));
            }
            SlicePredicate sp = new SlicePredicate();
            sp.setColumn_names(cols);
            Mutation mut = new Mutation();
            Deletion deletion = new Deletion();
            deletion.setPredicate(sp);
            deletion.setTimestamp(System.currentTimeMillis());
            mut.setDeletion(deletion);
            removal_list.add(mut);
            Map<String, List<Mutation>> columnFamilyValues = new HashMap<String, List<Mutation>>();
            columnFamilyValues.put(columnFamily, removal_list);

            Map<ByteBuffer, Map<String, List<Mutation>>> rowDefinition = new HashMap<ByteBuffer, Map<String, List<Mutation>>>();
            rowDefinition.put(ByteBuffer.wrap((key).getBytes(UTF8)), columnFamilyValues);
            client.batch_mutate(rowDefinition, getWriteCL());
            return true;
        } catch (UnsupportedEncodingException | InvalidRequestException | UnavailableException | TimedOutException | TException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), e);

        }
    }

    public Boolean dropAllPoints(String key) throws Exception {
        unregisterKey(key);
        ColumnPath columnPath = new ColumnPath();
        columnPath.setColumn_family(columnFamily);
        client.remove(ByteBuffer.wrap(key.getBytes(UTF8)), columnPath, System.currentTimeMillis(), getWriteCL());
        return true;
    }

    public List<SeriesValue> getPoints(String key) throws InvalidRequestException, UnavailableException, TimedOutException, TException, IOException {
        List<SeriesValue> ret = new ArrayList<SeriesValue>();

        boolean done = false;
        String startColumn = "";
        while (!done) {
            List<SeriesValue> inner = getPointsAfter(key, startColumn, 100);
            if (inner.isEmpty() || inner.size() < 100) {
                done = true;
            }
            ret.addAll(inner);
            if (!inner.isEmpty()) {
                startColumn = inner.get(inner.size() - 1).getColumn();
            }
        }
        return ret;
    }

    public List<SeriesValue> getPointsAfter(String key, String startColumn, int maxNumber) throws InvalidRequestException, UnavailableException, TimedOutException,
            TException, IOException {
        return getPointsAfter(key, startColumn, "", maxNumber);
    }

    public List<SeriesValue> getPointsAfter(String key, String startColumn, String endColumn, int maxNumber) throws InvalidRequestException, UnavailableException,
            TimedOutException, TException, IOException {
        List<SeriesValue> ret = new ArrayList<SeriesValue>();
        ByteBuffer start = startColumn.isEmpty() ? ByteBuffer.wrap(new byte[0]) : ByteBuffer.wrap(startColumn.getBytes(UTF8));
        ByteBuffer end = endColumn.isEmpty() ? ByteBuffer.wrap(new byte[0]) : ByteBuffer.wrap(endColumn.getBytes(UTF8));
        SliceRange range = new SliceRange(start, end, false, maxNumber);
        SlicePredicate predicate = new SlicePredicate();
        predicate.setSlice_range(range);
        ColumnParent columnParent = new ColumnParent();
        columnParent.setColumn_family(columnFamily);
        List<ColumnOrSuperColumn> results = client.get_slice(ByteBuffer.wrap(key.getBytes(UTF8)), columnParent, predicate, getReadCL());
        for (ColumnOrSuperColumn result : results) {
            ret.add(makeSeriesValue(result.column));
        }
        return ret;
    }

    public List<String> getSeriesLike(String keyPrefix) throws InvalidRequestException, UnavailableException, TimedOutException, TException, IOException {
        if (Strings.isNullOrEmpty(keyPrefix)) {
            List<SeriesValue> listings = getPoints(DIRECTORY_KEY);
            return Lists.transform(listings, colFunc);
        } else {
            int lastCharValue = keyPrefix.charAt(keyPrefix.length() - 1) + 1;
            String endPrefix = keyPrefix.substring(0, keyPrefix.length() - 1) + ((char) lastCharValue);
            List<SeriesValue> listings = getPointsAfter(DIRECTORY_KEY, keyPrefix, endPrefix, Integer.MAX_VALUE);
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

    public Boolean addPoint(String key, List<SeriesValue> values) throws Exception {
        registerKey(key);
        List<Mutation> insertion_list = new ArrayList<Mutation>();
        for (SeriesValue value : values) {
            Column c = new Column();
            c.setName(value.getColumn().getBytes(UTF8));
            c.setValue(SeriesValueCodec.encodeValue(value));
            c.setTimestamp(System.currentTimeMillis());
            Mutation mut = new Mutation();
            mut.setColumn_or_supercolumn(new ColumnOrSuperColumn().setColumn(c));
            insertion_list.add(mut);
        }
        Map<String, List<Mutation>> columnFamilyValues = new HashMap<String, List<Mutation>>();
        columnFamilyValues.put(columnFamily, insertion_list);

        Map<ByteBuffer, Map<String, List<Mutation>>> rowDefinition = new HashMap<ByteBuffer, Map<String, List<Mutation>>>();
        rowDefinition.put(ByteBuffer.wrap((key).getBytes(UTF8)), columnFamilyValues);
        client.batch_mutate(rowDefinition, getWriteCL());
        return true;
    }

    private Set<String> seriesKeys = Sets.newHashSet();

    private void registerKey(String key) {
        if (DIRECTORY_KEY.equals(key) || ChildKeyUtil.isRowKey(key)) {
            return;
        } else if (!seriesKeys.contains(key)) {
            addPoint(DIRECTORY_KEY, new StringSeriesValue(".", key));
            childrenRepo.registerParentage(key);
            seriesKeys.add(key);
        }
    }

    private void unregisterKey(String key) throws Exception {
        if (DIRECTORY_KEY.equals(key)) {
            return;
        }
        dropPoints(DIRECTORY_KEY, ImmutableList.of(key));
        childrenRepo.dropFileEntry(key);
        seriesKeys.remove(key);
    }

    private final SeriesValue makeSeriesValue(Column column) throws UnsupportedEncodingException, IOException {
        return makeSeriesValueFromByteArray(new String(column.getName(), UTF8), column.getValue());
    }

    public List<RaptureFolderInfo> getChildren(String folderName) throws Exception {
        return childrenRepo.getChildren(folderName);
    }
}
