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
package rapture.kernel.plugin;

import org.antlr.runtime.RecognitionException;

import com.google.common.base.Charsets;

import rapture.common.CallingContext;
import rapture.common.PluginTransportItem;
import rapture.common.RaptureURI;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.kernel.Kernel;
import rapture.parser.CSVCallback;
import rapture.parser.CSVExtractor;

public class RawSheetInstaller implements RaptureInstaller {

    @Override
    public void install(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        remove(context, uri, item);
        create(context, uri, item);
        try {
            CSVExtractor.getCSV(new String(item.getContent(), Charsets.UTF_8), new Callback(context, uri.toShortString()));
        } catch (RecognitionException e) {
            throw RaptureExceptionFactory.create("Error: CSV is not properly formatted");
        }
    }

    private void create(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        Kernel.getSheet().createSheet(context, uri.toShortString());
    }

    @Override
    public void remove(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        String s = uri.toShortString();
        if (Kernel.getSheet().sheetExists(context, s)) {
            Kernel.getSheet().deleteSheet(context, s);
        }
    }

    static class Callback implements CSVCallback {
        private int row = -1, col = -1, dimension = 0;
        private final String uri;
        private final CallingContext context;

        Callback(CallingContext context, String uri) {
            this.uri = uri;
            this.context = context;
        }

        @Override
        public void startNewLine() {
            row++;
            col = -1;
        }

        @Override
        public void addCell(String cell) {
            col++;
            Kernel.getSheet().setSheetCell(context, uri, row, col, cell, dimension);
        }
    }
}
