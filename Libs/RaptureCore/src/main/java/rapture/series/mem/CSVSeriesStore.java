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

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;

import rapture.common.SeriesValue;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.exception.RaptureExceptionFormatter;
import rapture.parser.CSVExtractor;
import rapture.util.ResourceLoader;

public class CSVSeriesStore extends MemorySeriesStore {

    private static final Logger log = Logger.getLogger(CSVSeriesStore.class);
    boolean live = false;

    @Override
    public void addDoubleToSeries(String key, String column, double value) {
        if (live) throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Read Only Repo");
        else super.addDoubleToSeries(key, column, value);
    }

    @Override
    public void addLongToSeries(String key, String column, long value) {
        if (live) throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Read Only Repo");
        else super.addDoubleToSeries(key, column, value);
    }

    @Override
    public void addStringToSeries(String key, String column, String value) {
        if (live) throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Read Only Repo");
        else super.addStringToSeries(key, column, value);
    }

    @Override
    public void addStructureToSeries(String key, String column, String json) {
        if (live) throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Read Only Repo");
        else super.addStructureToSeries(key, column, json);
    }

    @Override
    public void addPointToSeries(String key, SeriesValue value) {
        if (live) throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Read Only Repo");
        else super.addPointToSeries(key, value);
    }

    @Override
    public void addDoublesToSeries(String key, List<String> columns, List<Double> values) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Read Only Repo");
    }

    @Override
    public void addLongsToSeries(String key, List<String> columns, List<Long> values) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Read Only Repo");
    }

    @Override
    public void addStringsToSeries(String key, List<String> columns, List<String> values) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Read Only Repo");
    }

    @Override
    public void addStructuresToSeries(String key, List<String> columns, List<String> values) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Read Only Repo");
    }

    @Override
    public void addPointsToSeries(String key, List<SeriesValue> value) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Read Only Repo");
    }

    @Override
    public boolean deletePointsFromSeriesByPointKey(String key, List<String> pointKeys) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Read Only Repo");
    }

    @Override
    public boolean deletePointsFromSeries(String key) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Read Only Repo");
    }

    @Override
    public void setConfig(Map<String, String> config) {
        boolean useType = "true".equals(config.get("typerow"));
        String filePath = config.get("filename");
        Preconditions.checkArgument(filePath != null, "Mandatory argument filename missing");
        String prefix = config.get("prefix");
        if (prefix == null) prefix = "";

        String content = ResourceLoader.getResourceAsString(null, filePath);
        CSVSeriesCallback callback = new CSVSeriesCallback(this, useType, prefix);
        try {
            CSVExtractor.getCSV(content, callback);
        } catch (RecognitionException e) {
            RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error parsing csv config");
            String message = RaptureExceptionFormatter.getExceptionMessage(raptException, e);
            log.error(String.format("Error parsing csv content %s: %s", content, message));
            throw raptException;
        }
        live = true;
    }
}
