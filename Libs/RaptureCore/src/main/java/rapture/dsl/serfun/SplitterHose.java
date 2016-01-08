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
import java.util.Queue;

import rapture.common.Hose;
import rapture.common.SeriesValue;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class SplitterHose extends ComplexHose {
    private int fan;
    private Queue<SeriesValue> backlog[];
    private static List<String> OUTKEYS = Lists.newArrayList();
    private List<String> outKeys;
    private static boolean TERMINATE = true;
    private static boolean NO_TERMINATE = false;
    
    @SuppressWarnings("unchecked")
    public SplitterHose(HoseArg input, int fan) {
        super(1, fan);
        this.bind(input, 0, 0);
        this.fan = fan;
        for(int i = OUTKEYS.size(); i < fan; i++) {
            OUTKEYS.add("out"+i);
        }
        outKeys = OUTKEYS.subList(0, fan);
        backlog = new Queue[fan];
        for (int i=0; i<fan; i++) {
            backlog[i] = Lists.newLinkedList();
        }
    }
    
    @Override
    public String asString() { return "split()"; }
    
    public static class Factory implements HoseFactory {
        @Override
        public Hose make(List<HoseArg> args) {
            Preconditions.checkArgument(args.size() == 2, "Wrong number of arguments to split()");
            HoseArg input = args.get(0);
            int fan = args.get(1).tryInt("Bad argument to split(stream, fan) -- rate must be an integer");
            Preconditions.checkArgument(fan > 1, "Bad argument to sample(stream, fan) -- rate must be at least 2");
            Hose result = new SplitterHose(input, fan);
            return result;
        }        
    }
    
    @Override
    public void pushValue(SeriesValue v, int index) {
        for(int i=0; i<fan; i++) {
            backlog[i].add(v);
        }
        pushBacklog(NO_TERMINATE);
    }

    @Override
    public List<String> getInputKeys() {
        return SIMPLEKEYLIST;
    }

    @Override
    public List<String> getOutputKeys() {
        return outKeys;
    }

    @Override
    public void terminateStream(int index) {
        terminateStream();
    }

    @Override
    public void terminateStream() {
        terminated = true;
        pushBacklog(TERMINATE);
    }
    
    private void pushBacklog(boolean terminate) {
        for (int i=0; i<fan; i++) {
            Hose h = getDownstreamHose(i);
            int index = getDownstreamIndex(i);
            while (backlog[i].size() > 0) {
                h.pushValue(backlog[i].poll(), index);
            }
            if (terminate) h.terminateStream(index);
        }
    }

    @Override
    public SeriesValue pullValue(int index) {
        if (backlog[index].size() == 0) {
            SeriesValue next = upstream[0].pullValue(upstreamIndex[0]);
            if (next == null) {
                return null;
            }
            for(int i=0; i<fan; i++) {
                backlog[i].add(next);
            }
        }
        return backlog[index].poll();
    }

    @Override
    public String getName() { return "split()"; }
}
