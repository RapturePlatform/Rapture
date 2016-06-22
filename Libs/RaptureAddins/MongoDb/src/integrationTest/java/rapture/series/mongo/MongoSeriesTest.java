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

import java.util.Map;

import org.junit.Before;

import com.google.common.collect.ImmutableMap;

import rapture.config.MultiValueConfigLoader;
import rapture.config.ValueReader;
import rapture.repo.SeriesRepo;
import rapture.series.SeriesContract;

public class MongoSeriesTest extends SeriesContract {

    @Override
    public SeriesRepo createRepo() {
        Map<String, String> config = ImmutableMap.of("prefix", "testalicious");
        MongoSeriesStore store = new MongoSeriesStore();
        store.setInstanceName("default");
        store.setConfig(config);
        return new SeriesRepo(store);
    }

    @Before
    public void setup() {
        MultiValueConfigLoader.setEnvReader(new ValueReader() {
            @Override
            public String getValue(String property) {
                if (property.equals("MONGODB-DEFAULT")) {
                    return "mongodb://test:test@localhost/test";
                }
                return null;
            }
        });
    }
}
