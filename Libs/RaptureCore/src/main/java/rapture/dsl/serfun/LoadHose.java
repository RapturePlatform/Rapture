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

import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Preconditions;

import rapture.common.Hose;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.SeriesValue;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.kernel.Kernel;

public class LoadHose extends SimpleHose {
    private List<SeriesValue> page;
    private static final int PAGE_SIZE = 1000;
    private String lastValue = "";
    private boolean terminated = false;
    private RaptureURI uri;  
    private Iterator<SeriesValue> iter;
    
    public LoadHose(RaptureURI seriesURI) {
        this.uri = seriesURI;
        prime(false);
    }
    
    public void prime(boolean chop) {
        page = Kernel.getKernel().getSeriesRepo(uri).getPointsAfter(uri.getDocPath(), lastValue, PAGE_SIZE);
        if (page.size() < 2) {
            terminated = true;
            iter = null;
        }
        else {
            lastValue = page.get(page.size() - 1).getColumn();
            iter = page.iterator();
            if (chop) iter.next(); //chop the head
        }
    }
    
    /**
     * decode external name (format is <code>@authority:path/to/series</code>)
     */
    public static LoadHose make(String in) {
        String parts[] = in.split(":");
        if (parts.length < 2) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Illegal syntax in path description");
        }
        RaptureURI seriesURI = RaptureURI.builder(Scheme.SERIES, parts[0].substring(1)).docPath(parts[1]).build();
        return new LoadHose(seriesURI);
    }
    
    @Override
    public String getName() {
        return "load(seriesURI)";
    }

    @Override
    public void pushValue(SeriesValue v) {
        // do nothing -- there is no input stream
    }

    @Override
    public SeriesValue pullValue() {
        if (terminated) return null;
        if (iter.hasNext()) return iter.next();
        prime(true);
        if (terminated) return null;
        return iter.next();
    }

    public static class Factory implements HoseFactory {
        @Override
        public Hose make(List<HoseArg> args) {
            Preconditions.checkArgument(args.size() == 2, "Wrong number of arguments to load()");
            String authority = args.get(0).asString();
            String path = args.get(1).asString();
            Hose result = new LoadHose(RaptureURI.builder(Scheme.SERIES, authority).docPath(path).build());
            return result;
        }        
    }
}
