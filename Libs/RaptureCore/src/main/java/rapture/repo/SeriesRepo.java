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
package rapture.repo;

import rapture.common.RaptureFolderInfo;
import rapture.common.SeriesValue;
import rapture.series.SeriesStore;

import java.util.List;
import java.util.Map;

/**
 * Created by seanchen on 7/2/15.
 */
public class SeriesRepo {

    private SeriesStore store;

    public SeriesRepo(SeriesStore store) {
        this.store = store;
    }

    public void drop() {
        store.drop();
    }

    public void addDoubleToSeries(String key, String column, double value) {
        // TODO RAP-3141 Add graphite code here/figure out how to measure data size
        store.addDoubleToSeries(key, column, value);
    }

    public void addLongToSeries(String key, String column, long value) {
        // TODO RAP-3141 Add graphite code here/figure out how to measure data size
        store.addLongToSeries(key, column, value);
    }

    public void addStringToSeries(String key, String column, String value) {
        // TODO RAP-3141 Add graphite code here/figure out how to measure data size
        store.addStringToSeries(key, column, value);
    }

    public void addStructureToSeries(String key, String column, String json) {
        // TODO RAP-3141 Add graphite code here/figure out how to measure data size
        store.addStructureToSeries(key, column, json);
    }

    public void addPointToSeries(String key, SeriesValue value) {
        // TODO RAP-3141 Add graphite code here/figure out how to measure data size
        store.addPointToSeries(key, value);
    }

    public void addDoublesToSeries(String key, List<String> columns, List<Double> values) {
        // TODO RAP-3141 Add graphite code here/figure out how to measure data size
        store.addDoublesToSeries(key, columns, values);
    }

    public void addLongsToSeries(String key, List<String> columns, List<Long> values) {
        // TODO RAP-3141 Add graphite code here/figure out how to measure data size
        store.addLongsToSeries(key, columns, values);
    }

    public void addStringsToSeries(String key, List<String> columns, List<String> values) {
        // TODO RAP-3141 Add graphite code here/figure out how to measure data size
        store.addStringsToSeries(key, columns, values);
    }

    public void addStructuresToSeries(String key, List<String> columns, List<String> values) {
        // TODO RAP-3141 Add graphite code here/figure out how to measure data size
        store.addStructuresToSeries(key, columns, values);
    }

    public void addPointsToSeries(String key, List<SeriesValue> values) {
        // TODO RAP-3141 Add graphite code here/figure out how to measure data size
        store.addPointsToSeries(key, values);
    }

    public Boolean deletePointsFromSeriesByColumn(String key, List<String> pointKeys) {
        return store.deletePointsFromSeriesByPointKey(key, pointKeys);
    }

    /**
     * Drop all points from a series and delete it.
     *
     * @param key
     * @return
     */
    public void deletePointsFromSeries(String key) {
        store.deletePointsFromSeries(key);
    }

    public List<SeriesValue> getPoints(String key) {
        return store.getPoints(key);
    }

    public List<SeriesValue> getPointsAfter(String key, String startColumn, int maxNumber) {
        return store.getPointsAfter(key, startColumn, maxNumber);
    }

    public List<SeriesValue> getPointsAfterReverse(String key, String startColumn, int maxNumber) {
        return store.getPointsAfterReverse(key, startColumn, maxNumber);
    }

    public List<SeriesValue> getPointsAfter(String key, String startColumn, String endColumn, int maxNumber) {
        return store.getPointsAfter(key, startColumn, endColumn, maxNumber);
    }

    public void setInstanceName(String instanceName) {
        store.setInstanceName(instanceName);
    }

    public void setConfig(Map<String, String> config) {
        store.setConfig(config);
    }

    public List<String> getSeriesLike(String keyPrefix) {
        return store.getSeriesLike(keyPrefix);
    }

    public Iterable<SeriesValue> getRangeAsIteration(String key, String startCol, String endCol, int pageSize) {
        return store.getRangeAsIteration(key, startCol, endCol, pageSize);
    }

    List<SeriesValue> getRangeAsList(String key, String startCol, String endCol) {
        return store.getRangeAsList(key, startCol, endCol);
    }

    public List<RaptureFolderInfo> listSeriesByUriPrefix(String uriPrefix) {
        return store.listSeriesByUriPrefix(uriPrefix);
    }

    public void unregisterKey(String key) {
        store.unregisterKey(key);
    }

    public void unregisterKey(String key, boolean isFolder) {
        store.unregisterKey(key, isFolder);
    }

    public SeriesValue getLastPoint(String key) {
        return store.getLastPoint(key);
    }
}
