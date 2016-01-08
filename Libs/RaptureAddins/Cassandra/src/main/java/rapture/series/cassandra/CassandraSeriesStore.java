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
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import rapture.cassandra.CassandraConstants;
import rapture.common.Messages;
import rapture.common.RaptureFolderInfo;
import rapture.common.SeriesValue;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.dsl.serfun.DecimalSeriesValue;
import rapture.dsl.serfun.LongSeriesValue;
import rapture.dsl.serfun.StringSeriesValue;
import rapture.dsl.serfun.StructureSeriesValueImpl;
import rapture.series.SeriesPaginator;
import rapture.series.SeriesStore;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

/**
 * A Cassandra implementation of a series store
 *
 * @author amkimian
 */
public class CassandraSeriesStore implements SeriesStore {
    private AstyanaxSeriesConnection cass;

    Messages messageCatalog = new Messages("Cassandra");
    
    @Override
    public void drop() {
        try {
            cass.drop();
        } catch (ConnectionException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), e);
        }
    }

    @Override
    public void addPointToSeries(String key, SeriesValue value) {
        if (value.getColumn() == null) throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, messageCatalog.getMessage("NoColumnNull"));
        cass.addPoint(key, value);
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
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, messageCatalog.getMessage("JsonParseError"), e);
        }
    }

    @Override
    public void addStructuresToSeries(String key, List<String> columns, List<String> jsonValues) {
        try {
            addPointsToSeries(key, StructureSeriesValueImpl.zip(columns, jsonValues));
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, messageCatalog.getMessage("JsonParseError"), e);
        }

    }

    @Override
    public void addPointsToSeries(String key, List<SeriesValue> values) {
        cass.addPoint(key, values);
    }

    @Override
    public Boolean deletePointsFromSeriesByPointKey(String key, List<String> pointKeys) {
        return cass.dropPoints(key, pointKeys);
    }

    @Override
    public void deletePointsFromSeries(String key) {
        cass.dropAllPoints(key);
    }

    @Override
    public List<SeriesValue> getPoints(String key) {
        try {
            return cass.getPoints(key);
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), e);
        }
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
    public List<SeriesValue> getPointsAfter(String key, String startColumn, int maxNumber) {
        try {
            return cass.getPointsAfter(key, startColumn, maxNumber, false);
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), e);
        }
    }

    @Override
    public List<SeriesValue> getPointsAfterReverse(String docPath, String startColumn, int maxNumber) {
        try {
            return cass.getPointsAfter(docPath, startColumn, maxNumber, true);
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), e);
        }
    }

    @Override
    public List<SeriesValue> getPointsAfter(String key, String startColumn, String endColumn, int maxNumber) {
        try {
            return cass.getPointsAfter(key, startColumn, endColumn, maxNumber, false);
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), e);
        }
    }

    private String instance = "default";
    private Map<String, String> config;

    @Override
    public void setInstanceName(String instanceName) {
        this.instance = instanceName;
    }

    @Override
    public void setConfig(Map<String, String> config) {
        this.config = config;
        if (!config.containsKey(CassandraConstants.KEYSPACECFG)) {
            config.put(CassandraConstants.KEYSPACECFG, instance);
        }
        if (!config.containsKey(CassandraConstants.CFCFG)) {
            config.put(CassandraConstants.CFCFG, instance);
        }
        int overflowLimit = CassandraConstants.DEFAULT_OVERFLOW;
        if (config.containsKey(CassandraConstants.OVERFLOWLIMITCFG)) {
            try {
                overflowLimit = Integer.valueOf(config.get(CassandraConstants.OVERFLOWLIMITCFG));
            } catch (NumberFormatException ex) {
                throw RaptureExceptionFactory.create("Overflow limit must be an integer value");
            }
        }
        cass = new AstyanaxSeriesConnection(instance, this.config, overflowLimit);
    }

    @Override
    public List<String> getSeriesLike(String keyPrefix) {
        try {
            return cass.getSeriesLike(keyPrefix);
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), e);
        }
    }

    @Override
    public List<RaptureFolderInfo> listSeriesByUriPrefix(String dirName) {
        return cass.getChildren(dirName);
    }

    @Override
    public void unregisterKey(String key) {
        cass.unregisterKey(key, false);
    }

    @Override
    public void unregisterKey(String key, boolean isFolder) {
        cass.unregisterKey(key, isFolder);
    }

    @Override
    public SeriesValue getLastPoint(String key) {
        List<SeriesValue> points = getPointsAfterReverse(key, "", 1);
        return points.isEmpty() ? null : points.get(0);
    }
}
