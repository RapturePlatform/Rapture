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
import java.util.List;

import rapture.stat.BaseStat;
import rapture.stat.CounterStat;

/**
 * A value memory type holds a current set of "working" records, and a history
 * of old records.
 * 
 * @author amkimian
 * 
 */
public class CounterMemoryType extends BaseMemoryType {
    private long currentValue = 0;
    private String key;

    public CounterMemoryType(String key) {
        this.key = key;
    }

    public void addValue(CounterStat value) {
        currentValue += value.getValue();
    }

    @Override
    public boolean calculate() {
        return false;
    }

    @Override
    public BaseStat getCurrentStats() {
        CounterStat s = new CounterStat();
        s.setKey(key);
        s.setValue(currentValue);
        return s;
    }

    @Override
    public List<? extends BaseStat> getHistory(int recordCount) {
        ArrayList<BaseStat> ret = new ArrayList<BaseStat>();
        ret.add(getCurrentStats());
        return ret;
    }

    @Override
    public void purgeOldRecords() {
    }

}
