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
package rapture.stat.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;

import rapture.stat.BaseStat;
import rapture.stat.ValueOperation;
import rapture.stat.ValueStat;

/**
 * A value memory type holds a current set of "working" records, and a history
 * of old records.
 * 
 * @author amkimian
 * 
 */
public class ValueMemoryType extends BaseMemoryType {
    private ValueOperation operation;
    private long expireSeconds;
    private List<ValueStat> workingValues = Collections.synchronizedList(new LinkedList<ValueStat>());
    private List<ValueStat> history = Collections.synchronizedList(new ArrayList<ValueStat>());
    private String key;
    private long nextRecord;

    public ValueMemoryType(String key, ValueOperation operation, long seconds) {
        this.operation = operation;
        this.expireSeconds = seconds;
        this.key = key;
        this.nextRecord = nextExtractionTime();
    }

    public void addValue(ValueStat value) {
        workingValues.add(value);
    }

    @Override
    public boolean calculate() {
        if (System.currentTimeMillis() > nextRecord) {
            ValueStat v = new ValueStat();
            v.setKey(key);
            if (workingValues.isEmpty()) {
                v.setValue(0.0);
            } else {
                SummaryStatistics summary = new SummaryStatistics();
                synchronized (workingValues) {
                    for (ValueStat vs : workingValues) {
                        summary.addValue(vs.getValue());
                    }
                }
                switch (operation) {
                case AVERAGE:
                    v.setValue(summary.getMean());
                    break;
                case SUM:
                    v.setValue(summary.getSum());
                    break;
                }
            }
            history.add(v);
            nextRecord = nextExtractionTime();
            return true;
        }
        return false;
    }

    @Override
    public BaseStat getCurrentStats() {
        if (!history.isEmpty()) {
            return history.get(history.size() - 1);
        }
        return null;
    }

    @Override
    public List<? extends BaseStat> getHistory(int recordCount) {
        // records are stored in oldest first, so we want to go back
        if (recordCount >= history.size()) {
            recordCount = history.size();
        }
        List<ValueStat> ret = new ArrayList<ValueStat>();
        synchronized (history) {
            for (int i = history.size() - recordCount; i < history.size(); i++) {
                ret.add(history.get(i));
            }
        }
        return ret;
    }

    private long nextExtractionTime() {
        return System.currentTimeMillis() + 1000 * expireSeconds;
    }

    @Override
    public void purgeOldRecords() {
        List<ValueStat> toRemove = new ArrayList<ValueStat>();
        synchronized (workingValues) {
            for (ValueStat s : workingValues) {
                if (s.expiredInSeconds(expireSeconds)) {
                    toRemove.add(s);
                }
            }
        }
        workingValues.removeAll(toRemove);
    }

}
