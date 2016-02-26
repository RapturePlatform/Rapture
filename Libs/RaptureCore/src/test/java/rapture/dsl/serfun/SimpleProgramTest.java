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
package rapture.dsl.serfun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static rapture.common.Scheme.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.RecognitionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import rapture.common.CallingContext;
import rapture.common.Hose;
import rapture.common.RaptureURI;
import rapture.common.SeriesValue;
import rapture.common.SeriesPoint;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class SimpleProgramTest {
    public static final List<HoseArg> NULL_INPUT = Lists.newArrayList();
    public static final String AUTHORITY = "test";
    public static final String REPO = "series";
    public static final String CONFIG = "SREP {} USING MEMORY {}";
    private static FakeHose.Factory faker = new FakeHose.Factory();

    @Test
    public void testLoad() throws RecognitionException {
        String prog = "(stream out) <- x(stream in)\nout <- load('test', 'test/loader');";
        // Map<String,String> config = ImmutableMap.of("keyspace",
        // "__series_store", "cf", "__reserved_series_table");

        addDouble("test/loader", "alpha1", 1.5);
        addDouble("test/loader", "alpha2", 2.1);
        addDouble("test/loader", "alpha3", 1.2);
        addDouble("test/loader", "alpha4", 13.0);
        addDouble("test/loader", "alpha5", 1.5);
        HoseFactory factory = HoseProgram.compile(prog);
        Hose hose = factory.make(NULL_INPUT);

        checkDoubleValue(hose, "alpha1", 1.5);
        checkDoubleValue(hose, "alpha2", 2.1);
        checkDoubleValue(hose, "alpha3", 1.2);
        checkDoubleValue(hose, "alpha4", 13.0);
        checkDoubleValue(hose, "alpha5", 1.5);
        assertNull(hose.pullValue());
    }

    @Test
    public void testStore() throws RecognitionException {
        String prog = "(stream out) <- s(stream in)\nout <- store(in, 'test', 'test/storage');";
        HoseArg arg = HoseArg.make(faker.make("foo"), null, null);
        HoseFactory factory = HoseProgram.compile(prog);
        Hose hose = factory.make(ImmutableList.of(arg));
        checkDoubleValue(hose, "beta1", 239.5);
        checkDoubleValue(hose, "beta2", -2947.2);
        checkDoubleValue(hose, "beta3", 4817.2);
        checkDoubleValue(hose, "beta4", -293);
        checkDoubleValue(hose, "beta5", 44.3);
        checkDoubleValue(hose, "beta6", 113.2);
        assertNull(hose.pullValue());
        List<SeriesPoint> reloaded = Kernel.getSeries().getPoints(ContextFactory.getKernelUser(),
                RaptureURI.builder(SERIES, AUTHORITY).docPath("test/storage").asString());
        assertNotNull(reloaded);
        assertEquals(6, reloaded.size());
        checkDoubleInList(reloaded, 0, "beta1", 239.5);
        checkDoubleInList(reloaded, 1, "beta2", -2947.2);
        checkDoubleInList(reloaded, 2, "beta3", 4817.2);
        checkDoubleInList(reloaded, 3, "beta4", -293);
        checkDoubleInList(reloaded, 4, "beta5", 44.3);
        checkDoubleInList(reloaded, 5, "beta6", 113.2);
    }

    /**
     * Just a quick validation to see how much of the load time above is
     * one-time startup expenses
     * 
     * @throws RecognitionException
     */
    @Test
    public void testLoadAgain() throws RecognitionException {
        testLoad();
    }

    private static final void addDouble(String key, String column, double d) {
        Kernel.getSeries()
                .getTrusted()
                .addDoubleToSeries(ContextFactory.getKernelUser(),
                        RaptureURI.builder(SERIES, AUTHORITY).docPath(key).asString(), column, d);
    }

    private static final void checkDoubleInList(List<SeriesPoint> in, int index, String column, double d) {
        SeriesPoint value = in.get(index);
        assertEquals(column, value.getColumn());
        assertEquals("" + d, value.getValue());
    }

    private static final void checkDoubleValue(Hose hose, String column, double d) {
        SeriesValue v = hose.pullValue();
        assertEquals(column, v.getColumn());
        assertEquals(d, v.asDouble(), 0.0);
    }

    @Before
    public void setup() {
        CallingContext ctx = ContextFactory.getKernelUser();
        Kernel.initBootstrap();
        if (!Kernel.getSeries().seriesRepoExists(ctx, RaptureURI.builder(SERIES, AUTHORITY).asString())) {
            Kernel.getSeries().createSeriesRepo(ctx, RaptureURI.builder(SERIES, AUTHORITY).asString(), CONFIG);
        }
    }

    @After
    public void breakdown() {
        Kernel.getKernel().restart();
    }

    public static class FakeHose extends SimpleHose {
        private Iterator<SeriesValue> iter;

        FakeHose(List<SeriesValue> values) {
            iter = values.iterator();
        }

        @Override
        public String getName() {
            return "fake()";
        }

        @Override
        public void pushValue(SeriesValue v) {
            // do nothing -- no input so no pushing
        }

        @Override
        public SeriesValue pullValue() {
            return iter.hasNext() ? iter.next() : null;
        }

        public static class Factory implements HoseFactory {
            private static Map<String, List<SeriesValue>> dict = Maps.newHashMap();

            static {
                dict.put("foo", ImmutableList.of((SeriesValue) new DecimalSeriesValue(239.5, "beta1"),
                        new DecimalSeriesValue(-2947.2, "beta2"), new DecimalSeriesValue(4817.2, "beta3"),
                        new DecimalSeriesValue(-293, "beta4"), new DecimalSeriesValue(44.3, "beta5"),
                        new DecimalSeriesValue(113.2, "beta6")));

                dict.put("hi",
                        ImmutableList.of((SeriesValue) new StringSeriesValue("Hello"), new StringSeriesValue("World")));
            }

            @Override
            public Hose make(List<HoseArg> args) {
                return new FakeHose(dict.get(args.get(0).asString()));
            }

            public Hose make(String key) {
                return new FakeHose(dict.get(key));
            }
        }
    }
}
