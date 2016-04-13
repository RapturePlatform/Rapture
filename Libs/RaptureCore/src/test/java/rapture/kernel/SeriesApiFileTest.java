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
package rapture.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.SeriesPoint;
import rapture.common.SeriesRepoConfig;
import rapture.common.api.SeriesApi;
import rapture.common.exception.RaptureException;
import rapture.series.SeriesFactory;

public class SeriesApiFileTest extends AbstractFileTest {

    private static CallingContext callingContext;
    private static SeriesApiImpl seriesImpl;
    private static final String auth = "test" + System.currentTimeMillis();
    private static final String SERIES_USING_FILE = "SREP {} USING FILE {prefix=\"/tmp/" + auth + "\"}";
    private static final String REP_USING_FILE = "REP {} USING FILE {prefix=\"/tmp/" + auth + "\"}";

    static String seriesAuthorityURI = "series://" + auth;
    static String seriesURI = seriesAuthorityURI + "/SwampThing";

    @BeforeClass
    static public void setUp() {
        AbstractFileTest.setUp();
        config.RaptureRepo = REP_USING_FILE;
        config.InitSysConfig = "NREP {} USING FILE { prefix=\"/tmp/" + auth + "/sys.config\"}";
        // config.DefaultPipelineTaskStatus = "TABLE {} USING FILE {prefix=\"/tmp/" + auth + "\"}";

        Kernel.initBootstrap();
        callingContext = ContextFactory.getKernelUser();

        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");
        seriesImpl = new SeriesApiImpl(Kernel.INSTANCE);
    }

    @Test
    public void testDeleteRepo() {
        String thing = "SwampThing";
        ensureSeries(seriesAuthorityURI, thing);
        assertTrue(seriesImpl.seriesExists(callingContext, seriesURI));
        seriesImpl.deleteSeriesRepo(callingContext, seriesAuthorityURI);
        assertFalse(seriesImpl.seriesRepoExists(callingContext, seriesAuthorityURI));
        assertFalse(seriesImpl.seriesExists(callingContext, seriesURI));
        testCreateAndGetRepo();
        assertTrue(seriesImpl.seriesRepoExists(callingContext, seriesAuthorityURI));
        assertFalse(seriesImpl.seriesExists(callingContext, seriesURI));
        ensureSeries(seriesAuthorityURI, thing);
        assertTrue(seriesImpl.seriesExists(callingContext, seriesURI));
    }

    @Test
    public void testIllegalRepoPaths() {
        String repo = "series://";
        try {
            seriesImpl.createSeriesRepo(ContextFactory.getKernelUser(), repo, "SREP {} using MEMORY {}");
            fail(repo + " is not a valid Repo URI");
        } catch (RaptureException e) {
            assertEquals("Cannot create a repository without an authority", e.getMessage());
        }
        try {
            seriesImpl.createSeriesRepo(ContextFactory.getKernelUser(), "", "SREP {} using MEMORY {}");
            fail(" empty is not a valid Repo URI");
        } catch (RaptureException e) {
            assertEquals("Argument Repository URI cannot be null or empty", e.getMessage());
        }
        try {
            seriesImpl.createSeriesRepo(ContextFactory.getKernelUser(), null, "SREP {} using MEMORY {}");
            fail(" null is not a valid Repo URI");
        } catch (RaptureException e) {
            assertEquals("Argument Repository URI cannot be null or empty", e.getMessage());
        }
        try {
            seriesImpl.createSeriesRepo(ContextFactory.getKernelUser(), "document://x/x", "SREP {} using MEMORY {}");
            fail(repo + "x/x is not a valid URI");
        } catch (RaptureException e) {
            assertEquals("A Repository URI may not have a document path component", e.getMessage());
        }
    }

    @Test
    public void testThatWhichShouldNotBe() {
        String dummyAuthorityURI = "series://dummy";
        String dummyURI = dummyAuthorityURI + "/dummy";
        try {
            seriesImpl.createSeriesRepo(callingContext, dummyAuthorityURI, "SREP {} USING FILE { }");
            seriesImpl.addStringToSeries(callingContext, dummyURI, "duran", "duran");
            fail("You can't create a repo without a prefix");
        } catch (RaptureException e) {
            assertEquals("Repository configuration string is not valid: SREP {} USING FILE { }", e.getMessage());
        }

        // because the config gets stored even though it's not valid
        seriesImpl.deleteSeriesRepo(callingContext, dummyAuthorityURI);

        try {
            seriesImpl.createSeriesRepo(callingContext, dummyAuthorityURI, "SREP {} USING FILE { prefix=\"\" }"); //$NON-NLS-1$
            seriesImpl.addStringToSeries(callingContext, dummyURI, "duran", "duran");
            fail("You can't create a repo without a prefix");
        } catch (RaptureException e) {
            assertEquals("Repository configuration string is not valid: SREP {} USING FILE { prefix=\"\" }", e.getMessage());
        }

        // because the config gets stored even though it's not valid
        seriesImpl.deleteSeriesRepo(callingContext, dummyAuthorityURI);

        try {
            seriesImpl.createSeriesRepo(callingContext, dummyAuthorityURI, "SREP {} USING FILE {}");
            seriesImpl.addStringToSeries(callingContext, dummyURI, "duran", "duran");
            fail("You can't create a repo without a prefix");
        } catch (RaptureException e) {
            assertEquals("Repository configuration string is not valid: SREP {} USING FILE {}", e.getMessage());
        }

        // because the config gets stored even though it's not valid
        seriesImpl.deleteSeriesRepo(callingContext, dummyAuthorityURI);

        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("prefix", "");
        try {
            seriesImpl.createSeriesRepo(callingContext, dummyAuthorityURI, "SREP {} USING FILE { prefix=\"\" }");
            seriesImpl.addStringToSeries(callingContext, dummyURI, "duran", "duran");
            fail("You can't create a repo without a prefix");
        } catch (RaptureException e) {
            assertEquals("Repository configuration string is not valid: SREP {} USING FILE { prefix=\"\" }", e.getMessage());
        }
    }

    @Test
    public void testValidDocStore() {
        if (seriesImpl.seriesRepoExists(callingContext, seriesAuthorityURI)) seriesImpl.deleteSeriesRepo(callingContext, seriesAuthorityURI);
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("prefix", "/tmp/foo");
        seriesImpl.createSeriesRepo(callingContext, seriesAuthorityURI, "SREP {} USING FILE { prefix=\"/tmp/foo\"}");
    }

    @Ignore // This doesn't get caught correctly
    @Test
    public void testInvalidDocStore() {
        if (seriesImpl.seriesRepoExists(callingContext, seriesAuthorityURI)) seriesImpl.deleteSeriesRepo(callingContext, seriesAuthorityURI);
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("prefix", "/tmp/foo");
        try {
            seriesImpl.createSeriesRepo(callingContext, seriesAuthorityURI, "SREP {} USING FILE { prefix=\"/tmp/foo\"");
            fail("That config isn't valid");
        } catch (Exception e) {
            assertEquals("TBD", e.getMessage());
        }
    }

    @Test
    public void testCreateAndGetRepo() {
        if (seriesImpl.seriesRepoExists(callingContext, seriesAuthorityURI)) seriesImpl.deleteSeriesRepo(callingContext, seriesAuthorityURI);

        seriesImpl.createSeriesRepo(callingContext, seriesAuthorityURI, SERIES_USING_FILE);
        SeriesRepoConfig repoConfig = seriesImpl.getSeriesRepoConfig(callingContext, seriesAuthorityURI);
        assertNotNull(repoConfig);
        assertEquals(SERIES_USING_FILE, repoConfig.getConfig());
        assertEquals(auth, repoConfig.getAuthority());
    }

    @Test
    public void testGetSeriesRepositories() {
        // There may already be some existing repos
        List<SeriesRepoConfig> seriesRepositories = seriesImpl.getSeriesRepoConfigs(callingContext);
        ensureRepo(seriesAuthorityURI);
        int before = seriesRepositories.size();
        seriesImpl.createSeriesRepo(callingContext, "series://somewhereelse/", SERIES_USING_FILE);
        seriesRepositories = seriesImpl.getSeriesRepoConfigs(callingContext);
        assertEquals(before + 1, seriesRepositories.size());
    }

    /************************/

    @Test
    public void testLastPoint() {
        String seriesName = "ticker";
        String uri = String.format("%s/%s", seriesAuthorityURI, seriesName);
        ensureRepo(seriesAuthorityURI);
        // add data points to series
        int total = 30;
        for (int i = 1; i <= total; i++) {
            seriesImpl.addStringToSeries(callingContext, uri, getColumn(i), getValue(i));
        }

        // check last column
        SeriesPoint lastPoint = seriesImpl.getLastPoint(callingContext, uri);
        assertEquals(getColumn(total), lastPoint.getColumn());
    }

    @Test
    public void testMultiAdd() {
        String seriesName = "foo";
        String uri = String.format("%s/%s", seriesAuthorityURI, seriesName);

        testCreateAndGetRepo();

        List<String> columns = Arrays.asList(new String[] { "19700101", "19700102", "19700103" });
        List<String> stringValues = Arrays.asList(new String[] { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday" });
        List<String> jsonValues = Arrays.asList(new String[] { "{}", "{}", "{}", "{}" });
        List<Double> doubleValues = Arrays.asList(new Double[] { 1.0d, 2.0d, 3.0d, 4.0d });
        List<Long> longValues = Arrays.asList(new Long[] { 1l, 2l, 3l, 4l });
        List<Double> nullDoubleValues = Arrays.asList(new Double[] { null, null, null });
        List<String> nullStringValues = Arrays.asList(new String[] { null, null, null });
        List<Long> nullLongValues = Arrays.asList(new Long[] { null, null, null });

        try {
            seriesImpl.addStringsToSeries(callingContext, uri, columns, stringValues);
            Assert.fail("Expected exception due to mismatched list sizes");
        } catch (RaptureException e) {
            // Expected
        }

        try {
            seriesImpl.addStructuresToSeries(callingContext, uri, columns, jsonValues);
            Assert.fail("Expected exception due to mismatched list sizes");
        } catch (RaptureException e) {
            // Expected
        }

        try {
            seriesImpl.addLongsToSeries(callingContext, uri, columns, longValues);
            Assert.fail("Expected exception due to mismatched list sizes");
        } catch (RaptureException e) {
            // Expected
        }

        try {
            seriesImpl.addDoublesToSeries(callingContext, uri, columns, doubleValues);
            Assert.fail("Expected exception due to mismatched list sizes");
        } catch (RaptureException e) {
            // Expected
        }

        try {
            seriesImpl.addStringsToSeries(callingContext, uri, columns, nullStringValues);
        } catch (RaptureException e) {
            Assert.fail("Unexpected exception adding list of Null strings" + e);
        }

        try {
            seriesImpl.addStructuresToSeries(callingContext, uri, columns, nullStringValues);
            Assert.fail("Expected exception due to null values");
        } catch (RaptureException e) {
            // Expected
        }

        try {
            seriesImpl.addLongsToSeries(callingContext, uri, columns, nullLongValues);
            Assert.fail("Expected exception due to null values");
        } catch (RaptureException e) {
            // Expected
        }

        try {
            seriesImpl.addDoublesToSeries(callingContext, uri, columns, nullDoubleValues);
            Assert.fail("Expected exception due to null values");
        } catch (RaptureException e) {
            // Expected
        }

        try {
            seriesImpl.addStringsToSeries(callingContext, uri, new ArrayList<String>(), new ArrayList<String>());
            seriesImpl.addDoublesToSeries(callingContext, uri, new ArrayList<String>(), new ArrayList<Double>());
            seriesImpl.addLongsToSeries(callingContext, uri, new ArrayList<String>(), new ArrayList<Long>());
            seriesImpl.addStructuresToSeries(callingContext, uri, new ArrayList<String>(), new ArrayList<String>());
        } catch (RaptureException e) {
            Assert.fail("Unexpected exception - empty list is silly but legal " + e);
        }
    }

    private String getColumn(int index) {
        return "date_" + formatInt(index);
    }

    private String getValue(int index) {
        return "value_" + formatInt(index);
    }

    private String formatInt(int index) {
        return String.format("%03d", index);
    }

    private void deleteRepo(String repo) {
        seriesImpl.deleteSeriesRepo(callingContext, repo);
    }

    @Test
    public void deleteSeriesByUriPrefixTest() {
        ensureRepo(seriesAuthorityURI);
        ensureSeries(seriesAuthorityURI, "top");
        ensureSeries(seriesAuthorityURI, "live/series");
        ensureSeries(seriesAuthorityURI, "die/series");
        ensureSeries(seriesAuthorityURI, "nested/die/series");
        ensureSeries(seriesAuthorityURI, "die/nested/series");

        Map<String, RaptureFolderInfo> resultsMap;
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, seriesAuthorityURI, 0);
        assertEquals(10, resultsMap.size());

        List<String> removed = seriesImpl.deleteSeriesByUriPrefix(callingContext, seriesAuthorityURI + "/die");
        assertEquals(2, removed.size());
        removed = seriesImpl.deleteSeriesByUriPrefix(callingContext, seriesAuthorityURI + "/die");
        assertEquals(0, removed.size());

        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, seriesAuthorityURI, 0);
        // SeriesApi doesn't delete empty folders
        assertEquals(8, resultsMap.size());
    }

    private void ensureSeries(String repo, String name) {
        String uri = repo + (name.startsWith("/") ? "" : "/") + name;
        seriesImpl.addDoubleToSeries(callingContext, uri, "mel", 45.0);
        Object o = seriesImpl.getPoints(callingContext, uri);
    }

    private void ensureRepo(String repo) {
        if (seriesImpl.seriesRepoExists(callingContext, repo)) deleteRepo(repo);
        seriesImpl.createSeriesRepo(callingContext, repo, SERIES_USING_FILE);
    }

    @Test
    // also tests listSeriesByUriPrefix indirectly - seriesExists uses it
    public void createExistTest() {
        ensureRepo(seriesAuthorityURI);
        String name1 = "/Cheers/foo";
        String name2 = "/Cheers/bar";
        String series1 = seriesAuthorityURI + name1;
        String series2 = seriesAuthorityURI + name2;
        SeriesApi api = seriesImpl;

        ensureSeries(seriesAuthorityURI, name1);
        ensureSeries(seriesAuthorityURI, name2);

        assertTrue(series1 + " doesn't seem to exist", api.seriesExists(callingContext, series1));
        api.deleteSeries(callingContext, series1);
        assertTrue(series2 + " should still exist", api.seriesExists(callingContext, series2));
        assertFalse(series1 + " won't go away", api.seriesExists(callingContext, series1));
        api.addStringToSeries(callingContext, series1, "column-key", "column-value");
        assertTrue(series1 + " doesn't seem to exist", api.seriesExists(callingContext, series1));
        api.deleteSeries(callingContext, series1);
        assertTrue(series2 + " should still exist", api.seriesExists(callingContext, series2));
        assertFalse(series1 + " won't go away", api.seriesExists(callingContext, series1));
    }

    @Test()
    public void testDeleteSeriesRepo() {
        String series = auth + "/TheSweeney"; // It was a good series
        ensureRepo(seriesAuthorityURI);
        ensureSeries(seriesAuthorityURI, series);
        seriesImpl.deleteSeriesRepo(callingContext, seriesAuthorityURI);
        assertFalse(seriesImpl.seriesExists(callingContext, series));
    }

    @Test
    public void seriesPutPointsTest() {
        String wibble = "/tmp/wibble" + System.currentTimeMillis();
        String config = "SREP {} USING FILE {prefix=\"" + wibble + "\"}";

        // build uri and create repo
        String seriesURI = RaptureURI.builder(Scheme.SERIES, "foo").asString();
        System.out.println("The seriesURI: " + seriesURI);

        if (!seriesImpl.seriesRepoExists(callingContext, seriesURI)) {
            seriesImpl.createSeriesRepo(callingContext, seriesURI, config);
            SeriesRepoConfig seriesRepoConfig = seriesImpl.getSeriesRepoConfig(callingContext, seriesURI);
            System.out.println("Series repo config: " + seriesRepoConfig.toString());
        }

        seriesImpl.addDoubleToSeries(callingContext, seriesURI, "12345678", 54.321);
        List<SeriesPoint> pointsAsDoubles = seriesImpl.getPoints(callingContext, seriesURI);
        System.out.println(pointsAsDoubles.toString());
        new File(wibble + "_series/").deleteOnExit();
    }

    @Test
    public void testListByUriPrefix() {
        CallingContext callingContext = getCallingContext();
        for (SeriesRepoConfig src : seriesImpl.getSeriesRepoConfigs(callingContext))
            try {
                seriesImpl.deleteSeriesRepo(callingContext, src.getAddressURI().toString());
            } catch (Exception e) {
            }

        List<SeriesRepoConfig> legacy = seriesImpl.getSeriesRepoConfigs(callingContext);
        for (SeriesRepoConfig src : legacy) {
            seriesImpl.deleteSeriesRepo(callingContext, src.getAuthority());
        }
        legacy = seriesImpl.getSeriesRepoConfigs(callingContext);
        assertTrue(legacy.isEmpty());
        
        ensureRepo(seriesAuthorityURI);

        String uriPrefix = seriesAuthorityURI + "/uriFragment/";
        ensureRepo(seriesAuthorityURI);
        ensureSeries(seriesAuthorityURI, "uriFragment/series1");
        ensureSeries(seriesAuthorityURI, "uriFragment/series2");
        ensureSeries(seriesAuthorityURI, "uriFragment/folder1/series3");
        ensureSeries(seriesAuthorityURI, "uriFragment/folder1/series4");
        ensureSeries(seriesAuthorityURI, "uriFragment/folder1/folder2/series5");
        ensureSeries(seriesAuthorityURI, "uriFragment/folder1/folder2/series6");

        Map<String, RaptureFolderInfo> resultsMap;
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, uriPrefix, -1);
        assertEquals(8, resultsMap.size());
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, uriPrefix, 0);
        assertEquals(8, resultsMap.size());
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, uriPrefix, 4);
        assertEquals(8, resultsMap.size());
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, uriPrefix, 3);
        assertEquals(8, resultsMap.size());

        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, uriPrefix, 2);
        assertEquals(6, resultsMap.size());
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, uriPrefix, 1);
        assertEquals(3, resultsMap.size());
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, uriPrefix + "/folder1", 1);
        assertEquals(3, resultsMap.size());
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, uriPrefix + "/folder1", 2);
        assertEquals(5, resultsMap.size());

        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, seriesAuthorityURI, -1);
        assertEquals(9, resultsMap.size());
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, seriesAuthorityURI, 0);
        assertEquals(9, resultsMap.size());
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, seriesAuthorityURI, 4);
        assertEquals(9, resultsMap.size());
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, seriesAuthorityURI, 3);
        assertEquals(7, resultsMap.size());
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, seriesAuthorityURI, 2);
        assertEquals(4, resultsMap.size());
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, seriesAuthorityURI, 1);
        assertEquals(1, resultsMap.size());
        String str = resultsMap.keySet().toArray(new String[1])[0];
        assertEquals(uriPrefix, str);

        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, "series://", 1);
        assertEquals(1, resultsMap.size());
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, "series://", 2);
        assertEquals(2, resultsMap.size());
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, "series://", 3);
        assertEquals(5, resultsMap.size());
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, "series://", 4);
        assertEquals(8, resultsMap.size());
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, "series://", 5);
        assertEquals(10, resultsMap.size());
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, "series://", 6);
        assertEquals(10, resultsMap.size());
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, "series://", 0);
        assertEquals(10, resultsMap.size());
        resultsMap = seriesImpl.listSeriesByUriPrefix(callingContext, "series://", -1);
        assertEquals(10, resultsMap.size());

    }

    @Test
    public void testListByPrefix() {
        String fileRepo = "//fileBasedSeriesRepo";
        String memRepo = "//memoryBasedSeriesRepo";
        SeriesApi api = Kernel.getSeries();

        if (api.seriesRepoExists(callingContext, fileRepo)) api.deleteSeriesRepo(callingContext, fileRepo);
        if (api.seriesRepoExists(callingContext, memRepo)) api.deleteSeriesRepo(callingContext, memRepo);

        api.createSeriesRepo(callingContext, memRepo, "SREP {} USING MEMORY { }");
        api.createSeriesRepo(callingContext, fileRepo, "SREP {} USING FILE { prefix=\"/tmp/" + auth + "\"}");

        ensureSeries(memRepo, "foo/bar/baz");
        ensureSeries(fileRepo, "foo/bar/baz");

        Map<String, RaptureFolderInfo> memList = api.listSeriesByUriPrefix(callingContext, memRepo + "/foo", -1);
        Map<String, RaptureFolderInfo> fileList = api.listSeriesByUriPrefix(callingContext, fileRepo + "/foo", -1);

        assertEquals(memList.size(), fileList.size());

        for (String mem : memList.keySet()) {
            String file = mem.replaceAll(memRepo, fileRepo);
            assertEquals(memList.get(mem).getName(), fileList.get(file).getName());
            assertEquals(memList.get(mem).isFolder(), fileList.get(file).isFolder());
        }

        if (api.seriesRepoExists(callingContext, fileRepo)) api.deleteSeriesRepo(callingContext, fileRepo);
        if (api.seriesRepoExists(callingContext, memRepo)) api.deleteSeriesRepo(callingContext, memRepo);
    }

    @Test
    public void testRAP3643() {
        // RAP-3643 - Create Series Store errors and creates a malformed/unremovable repo (Series API)

        try {
            SeriesFactory.createStore(new RaptureURI("series://bogus"), "SREP { } using FILE { x=\"y\" } ");
        } catch (RaptureException e) {
            // expected because no prefix
        }
        // this should be OK
        SeriesFactory.createStore(new RaptureURI("series://bogus"), "SREP { } using FILE { prefix=\"y\" } ");
    }

}
