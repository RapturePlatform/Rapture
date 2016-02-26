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
import java.util.Queue;

import rapture.common.Hose;
import rapture.common.SeriesValue;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class MavgHose extends SimpleHose {
    private int range = 0;
    private double sum = 0.0;
    private String col;
    private Queue<SeriesValue> window = Lists.newLinkedList();
    
    public MavgHose(HoseArg input, int range) {
        super(input);
        this.range = range;
    }
    
    @Override 
    public String getName() { return "mavg()"; }
    
    public static class Factory implements HoseFactory {
        @Override
        public Hose make(List<HoseArg> args) {
            Preconditions.checkArgument(args.size() == 2, "Wrong number of arguments to mavg()");
            HoseArg input = args.get(0);
            int range = args.get(1).tryInt("Bad argument to mavg(stream, range) -- range must be an Integer");
            Hose result = new MavgHose(input, range);
            return result;
        }        
    }

    @Override
    public SeriesValue pullValue() {
        while(window.size() < range) {
            SeriesValue v = pullFromUpstream();
            if (v == null) {
                return null;
            }
            sum += v.asDouble();
            col = v.getColumn();
            window.add(v);
        }
        SeriesValue result = new DecimalSeriesValue(sum / range, col);
        SeriesValue dead = window.poll();
        sum -= dead.asLong();
        return result;
    }

    @Override
    public void pushValue(SeriesValue v) {
        sum += v.asDouble();
        col = v.getColumn();
        window.add(v);
        if(window.size() > range) {
            SeriesValue dead = window.poll();
            sum -= dead.asDouble();
        }
        if(window.size() == range) {
            pushValue(new DecimalSeriesValue(sum / range, col));
        }
    }
}
