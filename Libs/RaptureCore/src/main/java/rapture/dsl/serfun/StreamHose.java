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

import rapture.common.Hose;
import rapture.common.StructureSeriesValue;

public abstract class StreamHose implements Hose {
    protected boolean terminated = false;

    @Override
    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public String getColumn() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String asString() {
        return "<?>";
    }

    @Override
    public long asLong() {
        throw new UnsupportedOperationException("Attempt to use function as integer");
    }

    @Override
    public double asDouble() {
        throw new UnsupportedOperationException("Attempt to use function as decimal");
    }    
    
    @Override
    public boolean asBoolean() {
        throw new UnsupportedOperationException("Attempt to use function as boolean");
    }
    
    public StructureSeriesValue asStructure() {
        throw new UnsupportedOperationException("Attempt to use function as structure");
    }

    @Override
    public Hose asStream() {
        return this;
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public boolean isLong() {
        return false;
    }

    @Override
    public boolean isDouble() {
        return false;
    }
    
    @Override
    public boolean isBoolean() {
        return false;
    }
    
    @Override
    public boolean isString() {
        return false;
    }
    
    @Override
    public boolean isStructure() {
        return false;
    }

    @Override
    public boolean isSeries() {
        return true;
    }

    @Override
    public void bind(Hose receiver, int senderIndex, int receiverIndex) {
        bindReceiver(receiver, senderIndex, receiverIndex);
        receiver.bindSender(this, senderIndex, receiverIndex);
    }

    @Override
    public String getInputKey(int index) {
        return getInputKeys().get(index);
    }

    @Override
    public String getOutputKey(int index) {
        return getOutputKeys().get(index);
    }

    @Override
    public int getInputIndex(String key) {
        return getInputKeys().indexOf(key);
    }

    @Override
    public int getOutputIndex(String key) {
        return getOutputKeys().indexOf(key);
    }
}
