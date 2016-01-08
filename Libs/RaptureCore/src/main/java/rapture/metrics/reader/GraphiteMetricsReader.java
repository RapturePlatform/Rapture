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
package rapture.metrics.reader;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Period;

import com.incapture.graphite.GraphiteReader;
import com.incapture.graphite.render.Datapoint;
import com.incapture.graphite.render.RenderResponse;

/**
 * @author bardhi
 * @since 1/26/15.
 */
public class GraphiteMetricsReader implements MetricsReader {

    private static final String STATSD_TIMER_PREFIX = "stats.timers.rapture.";
    private static final String MEAN = ".mean";
    private static final String COUNT = ".count";
    final GraphiteReader graphiteReader;

    public GraphiteMetricsReader(URL baseUrl) {
        graphiteReader = new GraphiteReader(baseUrl);
    }

    @Override
    public Double getMetricAverage(String metricName, Period period) throws IOException {
        Long seconds = toSeconds(period);
        String from = String.format("-%ss", seconds);
        List<String> functionArgs = Arrays.asList(STATSD_TIMER_PREFIX + metricName + MEAN, String.format("'%ss'", seconds), "'avg'", "'alignToFrom=true'");
        RenderResponse response = graphiteReader.runRenderFunction(from, "summarize", functionArgs);
        List<Datapoint> datapoints = response.getDatapoints();

        //sometimes this returns more than one result -- always pick the latest timestamp that has a value as that's the most recent
        Double value = null;
        Long maxTimestamp = null;
        for (Datapoint datapoint : datapoints) {
            Long currTs = datapoint.getTimestamp();
            Double currValue = datapoint.getValue();
            if (currTs != null && currValue != null) {
                if (maxTimestamp == null || currTs > maxTimestamp) {
                    maxTimestamp = currTs;
                    value = currValue;
                }
            }
        }
        return value;
    }

    @Override
    public Long getMetricCount(String metricName, Period period) throws IOException {
        Long seconds = toSeconds(period);
        String from = String.format("-%ss", seconds);
        List<String> functionArgs = Arrays.asList(STATSD_TIMER_PREFIX + metricName + COUNT, String.format("'%ss'", seconds), "'sum'", "'alignToFrom=true'");
        RenderResponse response = graphiteReader.runRenderFunction(from, "summarize", functionArgs);
        List<Datapoint> datapoints = response.getDatapoints();

        //sometimes this returns more than one result -- always pick the latest timestamp that has a value as that's the most recent
        Double value = null;
        Long maxTimestamp = null;
        for (Datapoint datapoint : datapoints) {
            Long currTs = datapoint.getTimestamp();
            Double currValue = datapoint.getValue();
            if (currTs != null && currValue != null) {
                if (maxTimestamp == null || currTs > maxTimestamp) {
                    maxTimestamp = currTs;
                    value = currValue;
                }
            }
        }
        if (value == null) {
            return 0L;
        } else {
            return value.longValue();
        }
    }

    private Long toSeconds(Period period) {
        return period.toDurationTo(DateTime.now()).getStandardSeconds();
    }
}
