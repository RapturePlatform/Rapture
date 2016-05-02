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

import java.util.List;
import java.util.Map;

import rapture.common.SeriesValue;
import rapture.series.children.ChildrenRepo;
import rapture.series.mongo.MongoSeriesStore;

public class MongoChildrenRepo extends ChildrenRepo {
    private final MongoSeriesStore seriesStore;

    public MongoChildrenRepo(MongoSeriesStore seriesStore) {
        this.seriesStore = seriesStore;
    }

    public void setInstanceName(String instanceName) {
        seriesStore.setInstanceName(instanceName);
    }

    public void setConfig(Map<String, String> dirConfig) {
        seriesStore.setConfig(dirConfig);
    }

    @Override
    public List<SeriesValue> getPoints(String key) {
        return seriesStore.getPoints(key);
    }

    @Override
    public boolean addPoint(String key, SeriesValue value) {
        seriesStore.addPointToSeries(key, value);
        return true;
    }

    @Override
    public boolean dropPoints(String key, List<String> points) {
        return seriesStore.deletePointsFromSeriesByPointKey(key, points);
    }

    @Override
    public boolean dropRow(String key) {
        return seriesStore.deletePointsFromSeries(key);
    }

    @Override
    public void drop() {
        super.drop();
        seriesStore.drop();
    }

}
