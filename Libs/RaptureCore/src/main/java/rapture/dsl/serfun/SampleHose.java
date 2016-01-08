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
import rapture.common.SeriesValue;

import com.google.common.base.Preconditions;

/**
 * A simple stream function to pick every nth element from a stream.
 * @author mel
 *
 */
public class SampleHose extends SimpleHose {
    private int rate;
    private int pushCount = 0;
    
    public SampleHose(HoseArg input, int rate) {
        this.bind(input, 0, 0);
        this.rate = rate;
    }
    
    @Override
    public String getName() { return "sample()"; }
    
    public static class Factory implements HoseFactory {
        @Override
        public Hose make(List<HoseArg> args) {
            Preconditions.checkArgument(args.size() == 2, "Wrong number of arguments to sample()");
            HoseArg input = args.get(0);
            int rate = args.get(1).tryInt("Bad argument to sample(stream, rate) -- rate must be an integer");
            Preconditions.checkArgument(rate > 1, "Bad argument to sample(stream, rate) -- rate must be at least 2");
            Hose result = new SampleHose(input, rate);
            return result;
        }        
    }
    
    @Override
    public SeriesValue pullValue() {
        for (int i = pushCount + 1; i<rate; i++) upstream.pullValue();
        pushCount = 0;
        return upstream.pullValue(upstreamIndex);
    }

    @Override
    public void pushValue(SeriesValue v) {
        pushCount++;
        if (pushCount >= rate) {
            pushCount = 0;
            downstream.pushValue(v);
        }
    }
}
