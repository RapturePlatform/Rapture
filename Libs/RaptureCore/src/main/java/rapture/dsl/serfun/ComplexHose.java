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

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableList;

public abstract class ComplexHose extends StreamHose {
    public static final List<String> SIMPLEKEYLIST = ImmutableList.of("");
    protected Hose upstream[];
    protected int upstreamIndex[];
    protected Hose downstream[];
    protected int downstreamIndex[];
    
    public ComplexHose(int inputCount, int outputCount) {
        upstream = new Hose[inputCount];
        upstreamIndex = new int[inputCount];
        downstream = new Hose[outputCount];
        downstreamIndex = new int[outputCount];
    }
    
    @Override 
    public void bindSender(Hose sender, int senderIndex, int receiverIndex) {
        checkArgument(receiverIndex >= 0 && receiverIndex <= upstream.length, asString() + " has no such input " + receiverIndex);
        upstream[receiverIndex] = sender;
        upstreamIndex[receiverIndex] = senderIndex;
    }

    @Override
    public void bindReceiver(Hose receiver, int senderIndex, int receiverIndex) {
        downstream[senderIndex] = receiver;
        downstreamIndex[senderIndex] = receiverIndex;
    }

    protected int getDownstreamIndex(int i) { return downstreamIndex[i]; }

    protected Hose getDownstreamHose(int i) { return downstream[i]; }    
    
    @Override
    public SeriesValue pullValue() { return pullValue(0); }
    
    @Override
    public void pushValue(SeriesValue v) { pushValue(v, 0); }
}
