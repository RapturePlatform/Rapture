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
package rapture.common;

import java.util.List;

/**
 * This is a pull-based interface for use with stream functions.  I'm calling it Hose instead of "SeriesFunction"
 * to avoid confusion with the existing SeriesFunction implementation.  We can refactor it to a less painful name
 * after we decide which DSL should live and which should die.
 * 
 * A Hose takes series data as input in sorted order through named streams and outputs them to any number of named
 * streams. The balance between push and pull is is intentionally not decided in the interface so that we can later
 * make multiple implementations for different behaviors such as lazy evaluation, incremental update, and suspend.
 * 
 * Each instance has some number of inputs and outputs.  Inputs and outputs can be referred to by either their
 * zero-based index or by an InputKey/OutputKey (i.e. an optional String alias for a stream)
 *  
 * @author mel
 */
public interface Hose extends SeriesValue {
    public String getName();
    public List<String> getInputKeys();
    public List<String> getOutputKeys();
    public String getInputKey(int index);
    public String getOutputKey(int index);
    public int getOutputIndex(String key);
    public int getInputIndex(String key);
    public void bind(Hose receiver, int senderIndex, int receiverIndex);
    public void bindSender(Hose sender, int senderIndex, int receiverIndex);
    public void bindReceiver(Hose receiver, int senderIndex, int receiverIndex);
  
    /**
     * Push a value to the default stream (key = "")
     * @param v the datum
     */
    public void pushValue(SeriesValue v);

    /**
     * push a value to the specified output 
     * @param v the datum
     * @param index the index of the output stream
     */
    public void pushValue(SeriesValue v, int index);
    
    /**
     * Push an "end of stream" signal to the named input
     * @param key
     */
    public void terminateStream(int index);
    
    /**
     * Push an "end of stream" signal to the default output (key = "")
     * @return
     */
    public void terminateStream();
    
    public boolean isTerminated();
}
