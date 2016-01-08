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
package rapture.dsl.serfun;

import java.util.List;

import rapture.common.Hose;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.SeriesValue;
import rapture.kernel.Kernel;
import rapture.repo.SeriesRepo;

import com.google.common.base.Preconditions;

public class StoreHose extends SimpleHose {
    public final String path;
    public final SeriesRepo repo;

    public StoreHose(HoseArg outhose, RaptureURI seriesURI) {
        super(outhose);
        this.path = seriesURI.getDocPath();
        this.repo = Kernel.getKernel().getSeriesRepo(seriesURI);
    }
    
    @Override
    public String getName() {
        return "store()";
    }

    @Override
    public void pushValue(SeriesValue v) {
        repo.addPointToSeries(path, v);
    }

    @Override
    public SeriesValue pullValue() {
        SeriesValue v = upstream.pullValue();
        if (v==null) return null;
        repo.addPointToSeries(path, v);
        return v;
    }

    static class Factory implements HoseFactory {

        @Override
        public Hose make(List<HoseArg> args) {
            Preconditions.checkArgument(args.size() == 3, "Wrong number of arguments to store()");
            HoseArg outhose = args.get(0);
            String authority = args.get(1).asString();
            String path = args.get(2).asString();
            Hose result = new StoreHose(outhose, RaptureURI.builder(Scheme.SERIES, authority).docPath(path).build());
            return result;
        }
        
    }
}
