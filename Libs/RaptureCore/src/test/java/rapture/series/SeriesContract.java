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
package rapture.series;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import rapture.common.RaptureFolderInfo;
import rapture.common.SeriesValue;
import rapture.repo.SeriesRepo;

import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * This class defines the contract for a SeriesStore and defines UnitTests
 * for an implementation thereof. Each Implementation (e.g.
 * CassandraSeriesStore) should extend this class and implement the abstract
 * methods to initialize the type of store under test.
 * 
 * @author mel
 */
public abstract class SeriesContract {
    SeriesRepo repo = createRepo();

    /**
     * Create and initialize the series repo to be used by the test. Run
     * once per suite.
     */
    public abstract SeriesRepo createRepo();

    @Test
    public void testGetPoint() {
        // make a series with one point
        // make a search (same column)
        // make a search (different column)
        repo.addDoubleToSeries("GetPoint", "197307012350", 1.41);
        List<SeriesValue> pointList = repo.getPointsAfter("GetPoint", "197307010000", 2);
        assertEquals(1, pointList.size());
        assertEquals(1.41, pointList.get(0).asDouble(), 0.0);
        assertEquals("197307012350", pointList.get(0).getColumn());
        pointList = repo.getPointsAfter("GetPoint", "197307012351", 2);
        assertEquals(0, pointList.size());
        pointList = repo.getPointsAfter("GetPoints", "197307012350", 2);
        assertEquals(0, pointList.size());
    }

    @Test
    public void testGetPointsAfterReverse() {
        String key = "GetPointsAfterReverse";
        for (int i = 100; i < 200; i++) {
            repo.addDoubleToSeries(key, String.valueOf(i), i);
        }
        List<SeriesValue> points = repo.getPointsAfterReverse(key, "199", 100);
        assertEquals(100, points.size());
        assertEquals(199.0, points.get(0).asDouble(), 0);
        assertEquals(100.0, points.get(99).asDouble(), 0);
        points = repo.getPointsAfterReverse(key, "145", 13);
        assertEquals(13, points.size());
        assertEquals(145.0, points.get(0).asDouble(), 0);
        assertEquals(133.0, points.get(12).asDouble(), 0);


        points = repo.getPointsAfterReverse(key, "199", 5);
        assertEquals(5, points.size());
        assertEquals(199.0, points.get(0).asDouble(), 0);
    }

    @Test
    public void testGetLastPoint() {
        String key = "GetLastPoint";
        for (int i = 100; i < 200; i++) {
            repo.addDoubleToSeries(key, String.valueOf(i), i);
        }
        SeriesValue lastPoint = repo.getLastPoint(key);
        assertEquals("199", lastPoint.getColumn());
        assertEquals(199.0, lastPoint.asDouble(), 0);
    }
    
    @Test
    public void testGetPointsOverflow() {
        repo.addDoubleToSeries("GetPointOver", "197307012340", 1.41);
        repo.addDoubleToSeries("GetPointOver", "197307012351", 1.41);
        repo.addDoubleToSeries("GetPointOver", "197307012352", 1.41);
        repo.addDoubleToSeries("GetPointOver", "197307012363", 1.41);
        List<SeriesValue> pointList = repo.getPointsAfter("GetPointOver", "197307012345", 2);
        assertEquals(2, pointList.size());
        assertEquals("197307012351", pointList.get(0).getColumn());
        assertEquals("197307012352", pointList.get(1).getColumn());
    }

    @Test
    public void testGetAll() {
        // make a series with a few points
        // get them as a list
        // verify list contents

        // Note: first two intentionally added out of order
        repo.addDoubleToSeries("GetAll", "199306011454", 1.41);
        repo.addDoubleToSeries("GetAll", "199306011444", 1.42);
        repo.addDoubleToSeries("GetAll", "199306011520", 1.43);
        repo.addDoubleToSeries("GetAll", "199306011523", 1.44);
        List<SeriesValue> pointList = repo.getPoints("GetAll");
        assertEquals(4, pointList.size());
        assertEquals(1.42, pointList.get(0).asDouble(), 0.0);
        assertEquals(1.41, pointList.get(1).asDouble(), 0.0);
        assertEquals(1.43, pointList.get(2).asDouble(), 0.0);
        assertEquals(1.44, pointList.get(3).asDouble(), 0.0);
    }

    @Test
    public void testGetRangeIterator() {
        final String key = "GetRange";

        // make a series with many points
        // search for a range that only gets some of the points
        List<String> colList = ImmutableList.of("199701172103", "199701172105", "199701172107", "199701172108", "199701172109", "199701172111");
        List<Double> valList = ImmutableList.of(0.61, 0.69, 0.58, 0.73, 1.3, 9.2);
        repo.addDoublesToSeries(key, colList, valList);
        int index = 0;
        for (SeriesValue point : repo.getRangeAsIteration(key, "199701172106", "199701172109", 5)) {
            assertEquals(colList.get(index + 2), point.getColumn());
            assertEquals(valList.get(index + 2).doubleValue(), point.asDouble(), 0.0);
            index++;
        }
        assertEquals(3, index);
        index = 0;
        for (SeriesValue point : repo.getRangeAsIteration(key, "199701172104", "199701172110", 2)) {
            assertEquals(colList.get(index + 1), point.getColumn());
            assertEquals(valList.get(index + 1).doubleValue(), point.asDouble(), 0.0);
            index++;
        }
        assertEquals(4, index);
    }

    @Test
    public void testDropPoints() {
        final String key = "Drop";

        // search for presence of four points
        // drop two of them
        // verify that only two are left
        List<String> colList = ImmutableList.of("199701172103", "199701172105", "199701172107", "199701172108", "199701172109", "199701172111");
        List<Double> valList = ImmutableList.of(0.61, 0.69, 0.58, 0.73, 1.3, 9.2);
        List<String> dropList = ImmutableList.of("199701172107", "199701172109");
        repo.addDoublesToSeries(key, colList, valList);

        int index = 0;
        for (SeriesValue point : repo.getRangeAsIteration(key, "199701172104", "199701172110", 10)) {
            assertEquals(colList.get(index + 1), point.getColumn());
            assertEquals(valList.get(index + 1).doubleValue(), point.asDouble(), 0.0);
            index++;
        }
        assertEquals(4, index);

        repo.deletePointsFromSeriesByColumn(key, dropList);

        Iterator<SeriesValue> iter = repo.getRangeAsIteration(key, "199701172104", "199701172110", 10).iterator();
        SeriesValue point = iter.next();
        assertEquals("199701172105", point.getColumn());
        assertEquals(valList.get(1).doubleValue(), point.asDouble(), 0.0);

        point = iter.next();
        assertEquals("199701172108", point.getColumn());
        assertEquals(valList.get(3).doubleValue(), point.asDouble(), 0.0);

        assertFalse(iter.hasNext());
    }

    @Test
    public void testJson() {
        String path = "struct/series";
        String json = "{ \"a\":5, \"nest\": { \"b\": 8 } }";
        repo.addStructureToSeries(path, "whatever", json);
        List<SeriesValue> sv = repo.getPointsAfter(path, "whatever", 1);
        assertEquals(1, sv.size());
        assertEquals(5, sv.get(0).asStructure().getField("a").asLong());
        assertEquals(8, sv.get(0).asStructure().getField("nest").asStructure().getField("b").asLong());
    }
    
    @Test
    public void testFancyJson() {
        String path = "struct/series2";
        String json = "{ \"a\":5, \"nest\": { \"b\": 8 }, \"arr\": [ 1, 2, 3] }";
        repo.addStructureToSeries(path, "whatever", json);
        List<SeriesValue> sv = repo.getPointsAfter(path, "whatever", 1);
        assertEquals(1, sv.size());
        assertEquals("[1,2,3]", sv.get(0).asStructure().getField("arr").asString());
    }
    
    @Test
    public void testFindSeriesByUriPrefix() {
        String ab1 = "a/b/s1";
        String ab2 = "a/b/s2";
        String ac1 = "a/c/s1";
        String acd1 = "a/c/d/s1";
        String acd2 = "a/c/d/s2";
        String acd3 = "a/c/d/s3";

        repo.addDoubleToSeries(ab1, "197307012340", 1.41);
        repo.addDoubleToSeries(ab2, "197307012351", 1.41);
        repo.addDoubleToSeries(ac1, "197307012352", 1.41);
        repo.addDoubleToSeries(acd1, "197307012363", 1.41);
        repo.addDoubleToSeries(acd2, "197307012363", 1.41);
        repo.addDoubleToSeries(acd3, "197307012363", 1.41);

        assertTrue(repo.listSeriesByUriPrefix("").size() >= 1); // at least this, but
                                                      // could have more from
                                                      // other tests
        assertEquals(2, repo.listSeriesByUriPrefix("a").size());

        assertEquals(2, repo.listSeriesByUriPrefix("a/b").size());
        assertEquals(2, repo.listSeriesByUriPrefix("a/c").size());
        assertEquals(3, repo.listSeriesByUriPrefix("a/c/d").size());

        List<RaptureFolderInfo> children = repo.listSeriesByUriPrefix("a/b");
        assertEquals("s1", children.get(0).getName());
        assertEquals("s2", children.get(1).getName());
        assertFalse(children.get(0).isFolder());
        assertFalse(children.get(1).isFolder());

        children = repo.listSeriesByUriPrefix("a/c");
        assertEquals("d", children.get(1).getName());
        assertEquals("s1", children.get(0).getName());
        assertTrue(children.get(1).isFolder());
        assertFalse(children.get(0).isFolder());
        
        
        repo.deletePointsFromSeries("a/b/s1");
        assertEquals(1, repo.listSeriesByUriPrefix("a/b").size());
        repo.deletePointsFromSeries("a/b/s2");
        assertEquals(0, repo.listSeriesByUriPrefix("a/b").size());
    }
}
