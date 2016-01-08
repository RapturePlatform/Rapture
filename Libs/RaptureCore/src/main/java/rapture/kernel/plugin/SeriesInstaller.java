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
package rapture.kernel.plugin;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;

import rapture.common.CallingContext;
import rapture.common.PluginTransportItem;
import rapture.common.RaptureURI;
import rapture.common.api.SeriesApi;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.kernel.Kernel;

import com.google.common.base.Charsets;
import com.google.common.io.LineReader;

public class SeriesInstaller implements RaptureInstaller {
    @Override
    public void install(CallingContext ctx, RaptureURI uri, PluginTransportItem item) {
        SeriesApi api = Kernel.getSeries();
        try {
            LineReader reader = new LineReader(new StringReader(new String(item.getContent(), Charsets.UTF_8)));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                String part[] = line.split(",", 2);
                if (part.length != 2) {
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Malformed Series Descption: " + uri);
                }
                String column = part[0];
                String val = part[1];
                // TODO MEL find a way to bypass the wrapper so we don't reparse
                // URI and entitle/audit each point
                if (val.startsWith("'")) {
                    api.addStringToSeries(ctx, uri.toString(), column, val.substring(1));
                } else if (val.startsWith("{")) {
                    api.addStructureToSeries(ctx, uri.toString(), column, val);
                } else if (val.contains(".")) {
                    api.addDoubleToSeries(ctx, uri.toString(), column, Double.parseDouble(val));
                } else {
                    api.addLongToSeries(ctx, uri.toString(), column, Long.parseLong(val));
                }
            }
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error installing series at uri " + uri.toString(), e);
        }
    }

    @Override
    public void remove(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        Kernel.getSeries().deletePointsFromSeries(context, uri.toShortString());
    }
}
