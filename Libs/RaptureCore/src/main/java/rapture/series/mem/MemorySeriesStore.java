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
package rapture.series.mem;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import rapture.common.RaptureFolderInfo;
import rapture.common.SeriesValue;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.dsl.serfun.DecimalSeriesValue;
import rapture.dsl.serfun.LongSeriesValue;
import rapture.dsl.serfun.StringSeriesValue;
import rapture.dsl.serfun.StructureSeriesValueImpl;
import rapture.series.SeriesPaginator;
import rapture.series.SeriesStore;
import rapture.series.children.ChildrenRepo;

/**
 * An in memory version of a series repo, for testing
 *
 * @author amkimian
 */
public class MemorySeriesStore implements SeriesStore {

    private Map<String, SortedMap<String, SeriesValue>> seriesStore = Maps.newHashMap();
    private final ChildrenRepo childrenRepo;

    public MemorySeriesStore() {
        this.childrenRepo = new ChildrenRepo() {

            @Override
            public List<SeriesValue> getPoints(String key) {
                return MemorySeriesStore.this.getPoints(key);
            }

            @Override
            public boolean dropPoints(String key, List<String> points) {
                return MemorySeriesStore.this.deletePointsFromSeriesByPointKey(key, points);
            }

            @Override
            public boolean addPoint(String key, SeriesValue value) {
                MemorySeriesStore.this.addPointToSeries(key, value);
                return true;
            }

            @Override
            public boolean dropRow(String key) {
                return MemorySeriesStore.this.deletePointsFromSeries(key);
            }
        };
    }

    @Override
    public void drop() {
        seriesStore = Maps.newHashMap();
    }

    private SortedMap<String, SeriesValue> getOrMakeSeries(String key) {
        if (!seriesStore.containsKey(key)) {
            SortedMap<String, SeriesValue> data = Maps.newTreeMap();
            seriesStore.put(key, data);
            childrenRepo.registerParentage(key);
            return data;
        }
        return seriesStore.get(key);
    }

    private SortedMap<String, SeriesValue> getSeries(String key) {
        return seriesStore.get(key);
    }

    @Override
    public void addPointToSeries(String key, SeriesValue value) {
        if (value.getColumn() == null) throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Null column not allowed");
        SortedMap<String, SeriesValue> series = getOrMakeSeries(key);
        series.put(value.getColumn(), value);
    }

    @Override
    public void addDoubleToSeries(String key, String column, double value) {
        addPointToSeries(key, new DecimalSeriesValue(value, column));
    }

    @Override
    public void addDoublesToSeries(String key, List<String> columns, List<Double> values) {
        addPointsToSeries(key, DecimalSeriesValue.zip(columns, values));
    }

    @Override
    public void addLongToSeries(String key, String column, long value) {
        addPointToSeries(key, new LongSeriesValue(value, column));
    }

    @Override
    public void addLongsToSeries(String key, List<String> columns, List<Long> values) {
        addPointsToSeries(key, LongSeriesValue.zip(columns, values));
    }

    @Override
    public void addStringToSeries(String key, String column, String value) {
        addPointToSeries(key, new StringSeriesValue(value, column));
    }

    @Override
    public void addStringsToSeries(String key, List<String> columns, List<String> values) {
        addPointsToSeries(key, StringSeriesValue.zip(columns, values));
    }

    @Override
    public void addStructureToSeries(String key, String column, String jsonValue) {
        try {
            addPointToSeries(key, StructureSeriesValueImpl.unmarshal(jsonValue, column));
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Error parsing json value " + jsonValue, e);
        }
    }

    @Override
    public void addStructuresToSeries(String key, List<String> columns, List<String> jsonValues) {
        try {
            addPointsToSeries(key, StructureSeriesValueImpl.zip(columns, jsonValues));
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Error parsing json values", e);
        }
    }

    @Override
    public void addPointsToSeries(String key, List<SeriesValue> values) {
        boolean nullKey = false;
        for (SeriesValue value : values) {
            if (value.getColumn() == null) nullKey = true;
            else addPointToSeries(key, value);
        }
        if (nullKey) throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Column Key may not be null, other values added");
    }

    @Override
    public boolean deletePointsFromSeriesByPointKey(String key, List<String> pointKeys) {
        SortedMap<String, SeriesValue> series = getSeries(key);
        if (series == null) return false;
        for (String column : pointKeys) {
            series.remove(column);
        }
        // Do not drop an empty series here. It's OK to have a series without any points in it.
        return true;
    }

    private boolean dropSeries(String key) {
        SortedMap<String, SeriesValue> val = seriesStore.remove(key);
        if (val != null) {
            // TODO remove empty folder here
            return childrenRepo.dropFileEntry(key);
        }
        return false;
    }

    @Override
    public boolean deletePointsFromSeries(String key) {
        return dropSeries(key);
    }

    @Override
    public List<SeriesValue> getPoints(String key) {
        SortedMap<String, SeriesValue> map = getSeries(key);
        if (map == null) return Lists.newArrayList();
        return Lists.newArrayList(map.values());
    }

    @Override
    public List<SeriesValue> getPointsAfter(String key, String startColumn, int maxNumber) {
        SortedMap<String, SeriesValue> map = getSeries(key);
        if (map == null) return Lists.newArrayList();
        Iterator<SeriesValue> iter = map.tailMap(startColumn).values().iterator();
        List<SeriesValue> result = Lists.newLinkedList();
        for (int i = 0; i < maxNumber; i++) {
            if (!iter.hasNext()) break;
            result.add(iter.next());
        }
        return result;
    }

    @Override
    public List<SeriesValue> getPointsAfterReverse(String key, final String startColumn, int maxNumber) {
        SortedMap<String, SeriesValue> map = getSeries(key);
        if (map == null) return Lists.newArrayList();
        Iterator<Map.Entry<String, SeriesValue>> entryIterator = Iterables.filter(map.entrySet(), new Predicate<Map.Entry<String, SeriesValue>>() {
            @Override
            public boolean apply(Map.Entry<String, SeriesValue> input) {
                return startColumn.compareTo(input.getKey()) >= 0;
            }
        }).iterator();

        List<SeriesValue> vals = Lists.newArrayList();
        while (entryIterator.hasNext()) {
            vals.add(entryIterator.next().getValue());
        }

        Collections.reverse(vals);

        if (maxNumber > vals.size()) {
            return new ArrayList<>(vals);
        }
        return new ArrayList<>(vals.subList(0, maxNumber));
    }

    @Override
    public void setInstanceName(String instanceName) {
    }

    @Override
    public void setConfig(Map<String, String> config) {
        drop();
    }

    @Override
    public List<String> getSeriesLike(String keyPrefix) {
        List<String> ret = Lists.newArrayList();
        for (String key : seriesStore.keySet()) {
            if (key.startsWith(keyPrefix)) {
                ret.add(key);
            }
        }
        return ret;
    }

    @Override
    public List<SeriesValue> getPointsAfter(String key, String startColumn, String endColumn, int maxNumber) {
        SortedMap<String, SeriesValue> map = getSeries(key);
        if (map == null) return Lists.newArrayList();
        Iterator<SeriesValue> iter = map.subMap(startColumn, endColumn.concat("\0")).values().iterator();
        List<SeriesValue> result = Lists.newLinkedList();
        for (int i = 0; i < maxNumber; i++) {
            if (!iter.hasNext()) break;
            result.add(iter.next());
        }
        return result;
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
    public List<RaptureFolderInfo> listSeriesByUriPrefix(String string) {
        return childrenRepo.getChildren(string);
    }

    @Override
    public boolean unregisterKey(String key) {
        return childrenRepo.dropFileEntry(key);
    }

    @Override
    public SeriesValue getLastPoint(String key) {
        SortedMap<String, SeriesValue> map = getSeries(key);
        if (map == null) return null;
        String lastKey = map.lastKey();
        return map.get(lastKey);
    }

    @Override
    public boolean unregisterKey(String key, boolean isFolder) {
        return (isFolder) ? childrenRepo.dropFolderEntry(key) : childrenRepo.dropFileEntry(key);
    }

    @Override
    public void createSeries(String key) {
        getOrMakeSeries(key);
    }

    @Override
    public void deleteSeries(String key) {
        unregisterKey(key);
        deletePointsFromSeries(key);
    }

    /**
     * overflowLimit not used for in-memory implementation
     */
    @Override
    public int getOverflowLimit() {
        return 0;
    }

    /**
     * overflowLimit not used for in-memory implementation
     */
    @Override
    public void setOverflowLimit(int overflowLimit) {
    }

}
