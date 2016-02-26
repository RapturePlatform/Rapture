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
package rapture.stat.memory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import rapture.stat.BaseStat;
import rapture.stat.StringStat;

/**
 * A StringMemory type is used to hold a series of messages in a loop
 * 
 * @author amkimian
 * 
 */
public class StringMemoryType extends BaseMemoryType {
    private Queue<StringStat> messages;
    private int queueSize;

    public StringMemoryType(int queueSize) {
        messages = new LinkedList<StringStat>();
        this.queueSize = queueSize;
    }

    public void addMessage(StringStat message) {
        messages.add(message);
        if (messages.size() > queueSize) {
            messages.remove();
        }
    }

    @Override
    public boolean calculate() {
        return false;
    }

    @Override
    public BaseStat getCurrentStats() {
        if (messages.isEmpty()) {
            return null;
        } else {
            return ((LinkedList<StringStat>) messages).get(messages.size() - 1);
        }
    }

    @Override
    public List<? extends BaseStat> getHistory(int recordCount) {
        List<StringStat> ret = new ArrayList<StringStat>();
        LinkedList<StringStat> history = (LinkedList<StringStat>) messages;
        // records are stored in oldest first, so we want to go back
        if (recordCount >= history.size()) {
            recordCount = history.size();
        }
        for (int i = history.size() - recordCount; i < history.size(); i++) {
            ret.add(history.get(i));
        }
        return ret;
    }

    @Override
    public void purgeOldRecords() {
        // Nothing to do, it happens automatically
    }

}
