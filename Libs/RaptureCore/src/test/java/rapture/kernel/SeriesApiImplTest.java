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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.SeriesPoint;
import rapture.common.api.SeriesApi;
import rapture.common.exception.RaptureException;

public class SeriesApiImplTest {
    private CallingContext ctx = ContextFactory.getKernelUser();
    private final static String REPO = "//removeFolderRepo";

    @Before
    public void init() {
        Kernel.initBootstrap();
        ensureRepo(REPO);
    }

    @After
    public void tearDown() {
        deleteRepo(REPO);
    }

    @Test
    public void testLastPoint() {
        String seriesName = "ticker";
        String uri = String.format("%s/%s", REPO, seriesName);

        // add data points to series
        int total = 30;
        for (int i = 1; i <= total; i++) {
            Kernel.getSeries().addStringToSeries(ctx, uri, getColumn(i), getValue(i));
        }

        // check last column
        SeriesPoint lastPoint = Kernel.getSeries().getLastPoint(ctx, uri);
        assertEquals(getColumn(total), lastPoint.getColumn());
    }

    @Test
    public void testMultiAdd() {
        String seriesName = "foo";
        String uri = String.format("%s/%s", REPO, seriesName);

        List<String> columns = Arrays.asList(new String[] { "19700101", "19700102", "19700103" });
        List<String> stringValues = Arrays.asList(new String[] { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday" });
        List<String> jsonValues = Arrays.asList(new String[] { "{}", "{}", "{}", "{}" });
        List<Double> doubleValues = Arrays.asList(new Double[] { 1.0d, 2.0d, 3.0d, 4.0d });
        List<Long> longValues = Arrays.asList(new Long[] { 1l, 2l, 3l, 4l });
        List<Double> nullDoubleValues = Arrays.asList(new Double[] { null, null, null });
        List<String> nullStringValues = Arrays.asList(new String[] { null, null, null });
        List<Long> nullLongValues = Arrays.asList(new Long[] { null, null, null });

        try {
            Kernel.getSeries().addStringsToSeries(ctx, uri, columns, stringValues);
            Assert.fail("Expected exception due to mismatched list sizes");
        } catch (RaptureException e) {
            // Expected
        }

        try {
            Kernel.getSeries().addStructuresToSeries(ctx, uri, columns, jsonValues);
            Assert.fail("Expected exception due to mismatched list sizes");
        } catch (RaptureException e) {
            // Expected
        }

        try {
            Kernel.getSeries().addLongsToSeries(ctx, uri, columns, longValues);
            Assert.fail("Expected exception due to mismatched list sizes");
        } catch (RaptureException e) {
            // Expected
        }

        try {
            Kernel.getSeries().addDoublesToSeries(ctx, uri, columns, doubleValues);
            Assert.fail("Expected exception due to mismatched list sizes");
        } catch (RaptureException e) {
            // Expected
        }

        try {
            Kernel.getSeries().addStringsToSeries(ctx, uri, columns, nullStringValues);
        } catch (RaptureException e) {
            Assert.fail("Unexpected exception adding list of Null strings" + e);
        }

        try {
            Kernel.getSeries().addStructuresToSeries(ctx, uri, columns, nullStringValues);
            Assert.fail("Expected exception due to null values");
        } catch (RaptureException e) {
            // Expected
        }

        try {
            Kernel.getSeries().addLongsToSeries(ctx, uri, columns, nullLongValues);
            Assert.fail("Expected exception due to null values");
        } catch (RaptureException e) {
            // Expected
        }

        try {
            Kernel.getSeries().addDoublesToSeries(ctx, uri, columns, nullDoubleValues);
            Assert.fail("Expected exception due to null values");
        } catch (RaptureException e) {
            // Expected
        }

        try {
            Kernel.getSeries().addStringsToSeries(ctx, uri, new ArrayList<String>(), new ArrayList<String>());
            Kernel.getSeries().addDoublesToSeries(ctx, uri, new ArrayList<String>(), new ArrayList<Double>());
            Kernel.getSeries().addLongsToSeries(ctx, uri, new ArrayList<String>(), new ArrayList<Long>());
            Kernel.getSeries().addStructuresToSeries(ctx, uri, new ArrayList<String>(), new ArrayList<String>());
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
        Kernel.getSeries().deleteSeriesRepo(ctx, repo);
    }

    @Test
    public void deleteSeriesByUriPrefixTest() {
        ensureRepo(REPO);
        ensureSeries(REPO, "top");
        ensureSeries(REPO, "live/series");
        ensureSeries(REPO, "die/series");
        ensureSeries(REPO, "nested/die/series");
        ensureSeries(REPO, "die/nested/series");
        Kernel.getSeries().deleteSeriesByUriPrefix(ctx, REPO + "/die");
        try {
			Kernel.getSeries().deleteSeriesByUriPrefix(ctx, REPO + "/die");
			fail("Exception expected");
		} catch (Exception e) {
			assertEquals("Folder series://removeFolderRepo/die does not exist", e.getMessage());
		}
    }

    private void ensureSeries(String repo, String name) {
        String uri = repo + (name.startsWith("/") ? "" : "/") + name;
        Kernel.getSeries().addDoubleToSeries(ctx, uri, "mel", 45.0);
    }

    private void ensureRepo(String repo) {
        if (!Kernel.getSeries().seriesRepoExists(ctx, repo)) {
            Kernel.getSeries().createSeriesRepo(ctx, repo, "SREP {} USING MEMORY { }");
        }
    }

    @Test
    // also tests listSeriesByUriPrefix indirectly - seriesExists uses it
    public void createExistTest() {
        ensureRepo(REPO);
        String name1 = "/Cheers/foo";
        String name2 = "/Cheers/bar";
        String series1 = REPO + name1;
        String series2 = REPO + name2;
        SeriesApi api = Kernel.getSeries();

        ensureSeries(REPO, name1);
        ensureSeries(REPO, name2);

        assertTrue(series1 + " should exist", api.seriesExists(ctx, series1));

        api.deleteSeries(ctx, series1);
        assertTrue(series2 + " should still exist", api.seriesExists(ctx, series2));
        assertFalse(series1 + " should have been deleted", api.seriesExists(ctx, series1));

        api.addStringToSeries(ctx, series1, "column-key", "column-value");
        assertTrue(series2 + " should still exist", api.seriesExists(ctx, series2));
        assertTrue(series1 + " Should be back", api.seriesExists(ctx, series1));
    }

    @Test
    public void testIllegalRepoPaths() {
        String repo = "series://";
        String docPath = repo + "x/x";

        try {
            Kernel.getSeries().createSeriesRepo(ContextFactory.getKernelUser(), repo, "SERIES {} using MEMORY {}");
            fail("Cannot create a repository without an authority");
        } catch (RaptureException e) {
            assertEquals("Cannot create a repository without an authority", e.getMessage());
        }
        try {
            Kernel.getSeries().createSeriesRepo(ContextFactory.getKernelUser(), "", "SERIES {} using MEMORY {}");
            fail("URI cannot be null or empty");
        } catch (RaptureException e) {
            assertEquals("Argument Repository URI cannot be null or empty", e.getMessage());
        }
        try {
            Kernel.getSeries().createSeriesRepo(ContextFactory.getKernelUser(), null, "SERIES {} using MEMORY {}");
            fail("URI cannot be null or empty");
        } catch (RaptureException e) {
            assertEquals("Argument Repository URI cannot be null or empty", e.getMessage());
        }
        try {
            Kernel.getSeries().createSeriesRepo(ContextFactory.getKernelUser(), docPath, "SERIES {} using MEMORY {}");
            fail("Repository Uri can't have a document path component");
        } catch (RaptureException e) {
            assertEquals("A Repository URI may not have a document path component", e.getMessage());
        }
    }
}
