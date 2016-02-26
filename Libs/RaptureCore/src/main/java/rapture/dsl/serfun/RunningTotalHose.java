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

import java.util.List;

import rapture.common.Hose;
import rapture.common.SeriesValue;

import com.google.common.base.Preconditions;

public class RunningTotalHose extends SimpleHose {
    private static double total = 0.0;
    
    public RunningTotalHose(HoseArg input) {
        super(input);
    }
    
    @Override
    public String getName() {
        return "total()";
    }

    @Override
    public void pushValue(SeriesValue v) {
        total += v.asDouble();
        downstream.pushValue(new DecimalSeriesValue(total, v.getColumn()));
    }

    @Override
    public SeriesValue pullValue() {
        SeriesValue v = upstream.pullValue();
        if (v == null) {
            return null;
        }
        total += v.asDouble();
        return new DecimalSeriesValue(total, v.getColumn());
    }

    public static class Factory implements HoseFactory {
        @Override
        public Hose make(List<HoseArg> args) {
            Preconditions.checkArgument(args.size() == 1, "Wrong number of arguments to total()");
            HoseArg input = args.get(0);
            Hose result = new RunningTotalHose(input);
            return result;
        }       
    }
}
