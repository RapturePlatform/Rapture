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

import static rapture.common.Scheme.SERIES;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.SeriesDouble;
import rapture.common.SeriesString;
import rapture.common.client.HttpSeriesApi;
import rapture.helper.IntegrationTestHelper;

public class SeriesApiTest {
    private HttpSeriesApi seriesApi = null;
    IntegrationTestHelper helper = null;

    @BeforeClass(groups = { "series", "cassandra", "nightly" })
    @Parameters({ "RaptureURL", "RaptureUser", "RapturePassword" })
    public void beforeTest(@Optional("http://localhost:8665/rapture") String url, @Optional("rapture") String user, @Optional("rapture") String password) {
        helper = new IntegrationTestHelper(url, user, password);
        seriesApi = helper.getSeriesApi();
    }

    @Test(groups = { "series", "cassandra", "nightly" })
    public void testAddStringsToSeries() {
        int MAX_VALUES = 50;

        RaptureURI repo = helper.getRandomAuthority(Scheme.SERIES);
        helper.configureTestRepo(repo, "CASSANDRA");
        String repoName = new RaptureURI.Builder(repo).docPath("").build().toString();
        List<String> pointKeys = new ArrayList<String>();
        List<String> pointValues = new ArrayList<String>();
        for (int i = 0; i < MAX_VALUES; i++) {
            pointKeys.add(new Integer(i).toString());
            pointValues.add("testValue" + i);
        }
        String newSeries = repoName + "addStrings" + System.nanoTime();
        Reporter.log("Adding " + pointKeys.size() + " points to " + newSeries, true);
        seriesApi.addStringsToSeries(newSeries, pointKeys, pointValues);
        Reporter.log("Checking points in " + newSeries, true);
        List<SeriesString> seriesList = seriesApi.getPointsAsStrings(newSeries);
        Assert.assertTrue(seriesList.size() > 0);
        Set<String> keySet = new HashSet<String>();
        Set<String> valueSet = new HashSet<String>();
        for (SeriesString s : seriesList) {
            keySet.add(s.getKey());
            valueSet.add(s.getValue());
        }
        Assert.assertEquals(keySet, new HashSet<String>(pointKeys));
        Assert.assertEquals(valueSet, new HashSet<String>(pointValues));
    }

    @Test(groups = { "series", "cassandra", "nightly" })
    public void testSeriesListByUriPrefix() {
        RaptureURI repo = helper.getRandomAuthority(Scheme.SERIES);
        helper.configureTestRepo(repo, "MONGODB");

        Reporter.log("Create some test series", true);
        String seriesURIf1d1 = RaptureURI.builder(SERIES, repo.getAuthority()).docPath("folder1/doc1").build().toString();
        String seriesURIf1d2 = RaptureURI.builder(SERIES, repo.getAuthority()).docPath("folder1/doc2").build().toString();
        String seriesURIf1d3 = RaptureURI.builder(SERIES, repo.getAuthority()).docPath("folder1/doc3").build().toString();
        String seriesURIf2f21d1 = RaptureURI.builder(SERIES, repo.getAuthority()).docPath("folder2/folder21/doc1").build().toString();
        String seriesURIf2f21d2 = RaptureURI.builder(SERIES, repo.getAuthority()).docPath("folder2/folder21/doc2").build().toString();
        String seriesURIf3d1 = RaptureURI.builder(SERIES, repo.getAuthority()).docPath("folder3/doc1").build().toString();

        seriesApi.addLongToSeries(seriesURIf1d1, "key1",new Long (1));
        seriesApi.addLongToSeries(seriesURIf1d2, "key1",new Long (1));
        seriesApi.addLongToSeries(seriesURIf1d3,"key1",new Long (1));
        seriesApi.addLongToSeries(seriesURIf2f21d1, "key1",new Long (1));
        seriesApi.addLongToSeries(seriesURIf2f21d2, "key1",new Long (1));
        seriesApi.addLongToSeries(seriesURIf3d1,"key1",new Long (1));

        Reporter.log("Check folder contents using different depths", true);
        Assert.assertEquals(seriesApi.listSeriesByUriPrefix(RaptureURI.builder(SERIES, repo.getAuthority()).docPath("folder1").build().toString(), 2).size(), 3);
        Assert.assertEquals(seriesApi.listSeriesByUriPrefix(RaptureURI.builder(SERIES, repo.getAuthority()).docPath("folder1").build().toString(), 1).size(), 3);
        Assert.assertEquals(seriesApi.listSeriesByUriPrefix(RaptureURI.builder(SERIES, repo.getAuthority()).docPath("folder2").build().toString(), 2).size(), 3);
        Assert.assertEquals(seriesApi.listSeriesByUriPrefix(RaptureURI.builder(SERIES, repo.getAuthority()).docPath("folder2").build().toString(), 1).size(), 1);
        Assert.assertEquals(seriesApi.listSeriesByUriPrefix(RaptureURI.builder(SERIES, repo.getAuthority()).docPath("folder2").build().toString(), 0).size(), 3);
        Assert.assertEquals(seriesApi.listSeriesByUriPrefix(RaptureURI.builder(SERIES, repo.getAuthority()).docPath("folder3").build().toString(), 0).size(), 1);

        Reporter.log("Delete some series and check folder contents", true);
        seriesApi.deleteSeries(seriesURIf1d1);
        seriesApi.deleteSeries(seriesURIf3d1);
        Assert.assertEquals(seriesApi.listSeriesByUriPrefix(RaptureURI.builder(SERIES, repo.getAuthority()).docPath("folder1").build().toString(), 2).size(), 2);
        Assert.assertEquals(seriesApi.listSeriesByUriPrefix(RaptureURI.builder(SERIES, repo.getAuthority()).docPath("folder1").build().toString(), 1).size(), 2);
        Assert.assertEquals(seriesApi.listSeriesByUriPrefix(RaptureURI.builder(SERIES, repo.getAuthority()).docPath("folder3").build().toString(), 0).size(), 0);
      
        Reporter.log("Recreated some series and check folder contents", true);
        seriesApi.addLongToSeries(seriesURIf3d1, "key2",new Long (1));
        Assert.assertEquals(seriesApi.listSeriesByUriPrefix(RaptureURI.builder(SERIES, repo.getAuthority()).docPath("folder3").build().toString(), 1).size(), 1);
    }
    
    @Test(groups = { "series", "cassandra", "nightly" })
    public void testAddLongsToSeries() {
        int MAX_VALUES = 50;
        RaptureURI repo = helper.getRandomAuthority(Scheme.SERIES);
        helper.configureTestRepo(repo, "CASSANDRA");
        String repoName = new RaptureURI.Builder(repo).docPath("").build().toString();
        List<String> pointKeys = new ArrayList<String>();
        List<Long> pointValues = new ArrayList<Long>();
        for (int i = 0; i < MAX_VALUES; i++) {
            pointKeys.add(new Integer(i).toString());
            pointValues.add(new Long(i));
        }
        String newSeries = repoName + "addLongs" + System.nanoTime();
        Reporter.log("Adding " + pointKeys.size() + " points to " + newSeries, true);
        seriesApi.addLongsToSeries(newSeries, pointKeys, pointValues);
        Reporter.log("Checking points in " + newSeries, true);
        List<SeriesDouble> seriesList = seriesApi.getPointsAsDoubles(newSeries);
        Assert.assertTrue(seriesList.size() > 0);
        Set<String> keySet = new HashSet<String>();
        Set<Long> valueSet = new HashSet<Long>();
        for (SeriesDouble s : seriesList) {
            keySet.add(s.getKey());
            valueSet.add(new Long(s.getValue().longValue()));
        }
        Assert.assertEquals(keySet, new HashSet<String>(pointKeys));
        Assert.assertEquals(valueSet, new HashSet<Long>(pointValues));
    }

    @Test(groups = { "series", "cassandra", "nightly" })
    public void testAddDoublesToSeries() {
        int MAX_VALUES = 50;
        RaptureURI repo = helper.getRandomAuthority(Scheme.SERIES);
        helper.configureTestRepo(repo, "CASSANDRA");
        String repoName = new RaptureURI.Builder(repo).docPath("").build().toString();
        List<String> pointKeys = new ArrayList<String>();
        List<Double> pointValues = new ArrayList<Double>();
        for (int i = 0; i < MAX_VALUES; i++) {
            pointKeys.add(new Integer(i).toString());
            pointValues.add(new Double(i));
        }
        String newSeries = repoName + "addDoubles" + System.nanoTime();
        Reporter.log("Adding " + pointKeys.size() + " points to " + newSeries, true);
        seriesApi.addDoublesToSeries(newSeries, pointKeys, pointValues);
        Reporter.log("Checking points in " + newSeries, true);
        List<SeriesDouble> seriesList = seriesApi.getPointsAsDoubles(newSeries);
        Set<String> keySet = new HashSet<String>();
        Set<Double> valueSet = new HashSet<Double>();
        for (SeriesDouble s : seriesList) {
            keySet.add(s.getKey());
            valueSet.add(s.getValue());
        }
        Assert.assertEquals(keySet, new HashSet<String>(pointKeys));
        Assert.assertEquals(valueSet, new HashSet<Double>(pointValues));
    }

    @Test(groups = { "series", "cassandra", "nightly" })
    public void testAddDoublesToSeriesAndUpdate() {
        int MAX_VALUES = 100;
        RaptureURI repo = helper.getRandomAuthority(Scheme.SERIES);
        helper.configureTestRepo(repo, "CASSANDRA");
        String repoName = new RaptureURI.Builder(repo).docPath("").build().toString();
        List<String> pointKeys = new ArrayList<String>();
        List<Double> pointValues = new ArrayList<Double>();
        for (int i = 0; i < MAX_VALUES; i++) {
            pointKeys.add(new Integer(i).toString());
            pointValues.add(new Double(i));
        }
        String newSeries = repoName + "updateDoubles" + System.nanoTime();
        Reporter.log("Adding " + pointKeys.size() + " points to " + newSeries, true);
        seriesApi.addDoublesToSeries(newSeries, pointKeys, pointValues);
        Reporter.log("Checking points in " + newSeries, true);
        List<SeriesDouble> seriesList = seriesApi.getPointsAsDoubles(newSeries);
        Set<String> keySet = new HashSet<String>();
        Set<Double> valueSet = new HashSet<Double>();
        for (SeriesDouble s : seriesList) {
            keySet.add(s.getKey());
            valueSet.add(s.getValue());
        }
        Assert.assertEquals(keySet, new HashSet<String>(pointKeys));
        Assert.assertEquals(valueSet, new HashSet<Double>(pointValues));

        Reporter.log("Updating " + pointKeys.size() + " points to " + newSeries, true);
        Map<String, Double> checkMap = new HashMap<String, Double>();
        Reporter.log("Checking points in " + newSeries, true);
        for (int i = 0; i < MAX_VALUES; i++) {
            seriesApi.addDoubleToSeries(newSeries, new Integer(i).toString(), new Double(i + MAX_VALUES));
            checkMap.put(new Integer(i).toString(), new Double(i + MAX_VALUES));
        }
        seriesList = seriesApi.getPointsAsDoubles(newSeries);
        for (SeriesDouble s : seriesList) {
            Assert.assertEquals(s.getValue(), checkMap.get(s.getKey()));
        }
    }

    @Test(groups = { "series", "cassandra", "nightly" })
    public void testAddStringsToSeriesAndUpdate() {
        int MAX_VALUES = 100;
        RaptureURI repo = helper.getRandomAuthority(Scheme.SERIES);
        helper.configureTestRepo(repo, "CASSANDRA");
        String repoName = new RaptureURI.Builder(repo).docPath("").build().toString();
        List<String> pointKeys = new ArrayList<String>();
        List<String> pointValues = new ArrayList<String>();
        for (int i = 0; i < MAX_VALUES; i++) {
            pointKeys.add(new Integer(i).toString());
            pointValues.add("point" + i);
        }
        String newSeries = repoName + "updateStrings" + System.nanoTime();
        Reporter.log("Adding " + pointKeys.size() + " points to " + newSeries, true);
        seriesApi.addStringsToSeries(newSeries, pointKeys, pointValues);
        Reporter.log("Checking points in " + newSeries, true);
        List<SeriesString> seriesList = seriesApi.getPointsAsStrings(newSeries);
        Set<String> keySet = new HashSet<String>();
        Set<String> valueSet = new HashSet<String>();
        for (SeriesString s : seriesList) {
            keySet.add(s.getKey());
            valueSet.add(s.getValue());
        }
        Assert.assertEquals(keySet, new HashSet<String>(pointKeys));
        Assert.assertEquals(valueSet, new HashSet<String>(pointValues));

        Reporter.log("Updating " + pointKeys.size() + " points to " + newSeries, true);
        Map<String, String> checkMap = new HashMap<String, String>();
        Reporter.log("Checking points in " + newSeries, true);
        for (int i = 0; i < MAX_VALUES; i++) {
            seriesApi.addStringToSeries(newSeries, new Integer(i).toString(), "point" + (i + MAX_VALUES));
            checkMap.put(new Integer(i).toString(), "point" + (i + MAX_VALUES));
        }
        seriesList = seriesApi.getPointsAsStrings(newSeries);
        for (SeriesString s : seriesList) {
            Assert.assertEquals(s.getValue(), checkMap.get(s.getKey()));
        }
    }

    @Test(groups = { "series", "cassandra", "nightly" })
    public void testDoubleSeriesRange() {
        int MAX_VALUES = 200;
        int OFFSET = 1000;
        int LOW_VALUE = MAX_VALUES / 4;
        int HIGH_VALUE = 3 * (MAX_VALUES / 4);
        RaptureURI repo = helper.getRandomAuthority(Scheme.SERIES);
        helper.configureTestRepo(repo, "CASSANDRA");
        String repoName = new RaptureURI.Builder(repo).docPath("").build().toString();
        List<String> pointKeys = new ArrayList<String>();
        List<Double> pointValues = new ArrayList<Double>();
        for (int i = OFFSET; i < MAX_VALUES + OFFSET; i++) {
            pointKeys.add(new Integer(i).toString());
            pointValues.add(new Double(i));
        }
        String newSeries = repoName + "getDoublesRanges" + System.nanoTime();
        Reporter.log("Adding " + pointKeys.size() + " points to " + newSeries, true);
        seriesApi.addDoublesToSeries(newSeries, pointKeys, pointValues);
        Reporter.log("Checking points in " + newSeries, true);
        List<SeriesDouble> seriesList = seriesApi.getPointsInRangeAsDoubles(newSeries, new Integer(LOW_VALUE + OFFSET).toString(),
                new Integer(HIGH_VALUE + OFFSET).toString(), MAX_VALUES);
        Assert.assertTrue(seriesList.size() > 0);
        for (SeriesDouble s : seriesList) {
            Assert.assertTrue(Integer.parseInt(s.getKey()) >= (LOW_VALUE + OFFSET) && Integer.parseInt(s.getKey()) <= (HIGH_VALUE + OFFSET),
                    "Key " + s.getKey() + " not in range");
        }
    }

    @Test(groups = { "series", "cassandra", "nightly" })
    public void testDeleteSeriesByKey() {
        int MAX_VALUES = 200;
        RaptureURI repo = helper.getRandomAuthority(Scheme.SERIES);
        helper.configureTestRepo(repo, "CASSANDRA");
        String repoName = new RaptureURI.Builder(repo).docPath("").build().toString();
        List<String> pointKeys = new ArrayList<String>();
        List<Double> pointValues = new ArrayList<Double>();
        for (int i = 0; i < MAX_VALUES; i++) {
            pointKeys.add(new Integer(i).toString());
            pointValues.add(new Double(i));
        }
        String newSeries = repoName + "deleteSeriesByKey" + System.nanoTime();
        Reporter.log("Adding " + pointKeys.size() + " points to " + newSeries, true);
        seriesApi.addDoublesToSeries(newSeries, pointKeys, pointValues);
        Reporter.log("Deleting " + (pointKeys.size() / 2) + " points to " + newSeries, true);
        for (int i = 0; i < MAX_VALUES / 2; i++)
            pointKeys.remove(0);
        seriesApi.deletePointsFromSeriesByPointKey(newSeries, pointKeys);
        Reporter.log("Checking " + newSeries, true);
        List<SeriesDouble> seriesList = seriesApi.getPointsAsDoubles(newSeries);
        for (SeriesDouble s : seriesList) {
            Assert.assertTrue(Integer.parseInt(s.getKey()) < (MAX_VALUES / 2));
        }
        Reporter.log("Deleting remaining points to " + newSeries, true);
        pointKeys = new ArrayList<String>();
        for (SeriesDouble s : seriesList)
            pointKeys.add(s.getKey());
        seriesApi.deletePointsFromSeriesByPointKey(newSeries, pointKeys);
        Reporter.log("Checking " + newSeries + " has size of 0", true);
        Assert.assertEquals(seriesApi.getPointsAsDoubles(newSeries).size(), 0);
    }

    @Test(groups = { "series", "cassandra", "nightly" })
    public void testDeleteAndUpdateSeries() {
        int MAX_VALUES = 200;
        RaptureURI repo = helper.getRandomAuthority(Scheme.SERIES);
        helper.configureTestRepo(repo, "CASSANDRA");
        String repoName = new RaptureURI.Builder(repo).docPath("").build().toString();
        List<String> pointKeys = new ArrayList<String>();
        List<Double> pointValues = new ArrayList<Double>();
        for (int i = 0; i < MAX_VALUES; i++) {
            pointKeys.add(new Integer(i).toString());
            pointValues.add(new Double(i));
        }
        String newSeries = repoName + "deleteUpdateSeries" + System.nanoTime();
        Reporter.log("Adding " + pointKeys.size() + " points to " + newSeries, true);
        seriesApi.addDoublesToSeries(newSeries, pointKeys, pointValues);
        Reporter.log("Deleting " + (pointKeys.size() / 2) + " points to " + newSeries, true);
        for (int i = 0; i < MAX_VALUES / 2; i++)
            pointKeys.remove(0);
        seriesApi.deletePointsFromSeriesByPointKey(newSeries, pointKeys);
        Reporter.log("Checking " + newSeries, true);
        List<SeriesDouble> seriesList = seriesApi.getPointsAsDoubles(newSeries);
        for (SeriesDouble s : seriesList) {
            Assert.assertTrue(Integer.parseInt(s.getKey()) < (MAX_VALUES / 2));
        }
        Reporter.log("Updating remaining points to " + newSeries, true);
        Map<String, Double> updateMap = new HashMap<String, Double>();
        for (SeriesDouble s : seriesList) {
            seriesApi.addDoubleToSeries(newSeries, s.getKey(), new Double(s.getValue().doubleValue() + MAX_VALUES));
            updateMap.put(s.getKey(), new Double(s.getValue().doubleValue() + MAX_VALUES));
        }
        Reporter.log("Check points in " + newSeries, true);
        seriesList = seriesApi.getPointsAsDoubles(newSeries);
        for (SeriesDouble s : seriesList) {
            Assert.assertEquals(s.getValue(), updateMap.get(s.getKey()));
        }

    }

    @Test(groups = { "series", "cassandra", "nightly" })
    public void testDeleteAllSeriesPoints() {
        int MAX_VALUES = 200;
        RaptureURI repo = helper.getRandomAuthority(Scheme.SERIES);
        helper.configureTestRepo(repo, "CASSANDRA");
        String repoName = new RaptureURI.Builder(repo).docPath("").build().toString();
        List<String> pointKeys = new ArrayList<String>();
        List<Double> pointValues = new ArrayList<Double>();
        for (int i = 0; i < MAX_VALUES; i++) {
            pointKeys.add(new Integer(i).toString());
            pointValues.add(new Double(i));
        }
        String newSeries = repoName + "deleteSeries" + System.nanoTime();
        Reporter.log("Adding " + pointKeys.size() + " points to " + newSeries, true);
        seriesApi.addDoublesToSeries(newSeries, pointKeys, pointValues);
        seriesApi.deletePointsFromSeries(newSeries);
        Assert.assertEquals(seriesApi.getPoints(newSeries).size(), 0);
    }

    @AfterClass(groups = { "series", "cassandra", "nightly" })
    public void AfterTest() {
        helper.cleanAllAssets();
    }
}
