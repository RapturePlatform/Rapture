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
package rapture.series;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import rapture.common.SeriesValue;

public class SeriesChecks {
    @Test
    public void testMemorySeriesStore() {
        SeriesStore repo = SeriesFactory.createStore("SREP {} USING MEMORY {}", "testSeries");
        assertNotNull(repo);
        for (int i = 0; i < 1000; i++) {
            repo.addDoubleToSeries("test", "" + i, (double) i * i);
        }
        dumpKey("test", repo);
    }

    @Test
    public void testGetPointsAfterReverse() {
        SeriesStore repo = SeriesFactory.createStore("SREP {} USING MEMORY {}", "testSeries2");
        assertNotNull(repo);
        for (int i = 100; i < 1000; i++) {
            repo.addDoubleToSeries("test2", "" + i, (double) i);
        }
        List<SeriesValue> points = repo.getPointsAfterReverse("test2", "500", 5);
        assertEquals(500.0, points.get(0).asDouble(), 0);
        assertEquals(499.0, points.get(1).asDouble(), 0);
        assertEquals(498.0, points.get(2).asDouble(), 0);
        assertEquals(497.0, points.get(3).asDouble(), 0);
        assertEquals(496.0, points.get(4).asDouble(), 0);
    }

    private void dumpKey(String key, SeriesStore repo) {
        List<SeriesValue> points = repo.getPoints(key);
        System.out.println("Key = " + key);
        for (SeriesValue p : points) {
            System.out.println(String.format("%s - %f", p.getColumn(), p.asDouble()));
        }
    }
}
