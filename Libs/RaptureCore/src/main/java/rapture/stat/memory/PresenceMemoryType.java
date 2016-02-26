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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rapture.stat.BaseStat;
import rapture.stat.PresenceStat;
import rapture.stat.ValueStat;

public class PresenceMemoryType extends BaseMemoryType {
    private Map<String, PresenceStat> stats = new HashMap<String, PresenceStat>();
    private List<ValueStat> history = new ArrayList<ValueStat>();
    private String key;
    private long nextRecord;

    private long ageInSeconds;

    public PresenceMemoryType(String key, Long age) {
        this.key = key;
        this.ageInSeconds = age;
        this.nextRecord = nextExtractionTime(); // Try and record every minute
    }

    public void addPresence(PresenceStat presence) {
        synchronized (stats) {
            stats.put(presence.getInstance(), presence);
        }
    }

    @Override
    public boolean calculate() {
        synchronized (stats) {
            if (System.currentTimeMillis() > nextRecord) {
                ValueStat v = new ValueStat();
                v.setKey(key);
                v.setValue(new Double(stats.size()));
                history.add(v);
                nextRecord = nextExtractionTime();
                return true;
            }
            return false;
        }
    }

    @Override
    public BaseStat getCurrentStats() {
        if (!history.isEmpty()) {
            return history.get(history.size() - 1);
        } else {
            return null;
        }
    }

    @Override
    public List<? extends BaseStat> getHistory(int recordCount) {
        // records are stored in oldest first, so we want to go back
        if (recordCount >= history.size()) {
            recordCount = history.size();
        }
        List<ValueStat> ret = new ArrayList<ValueStat>();
        for (int i = history.size() - recordCount; i < history.size(); i++) {
            ret.add(history.get(i));
        }
        return ret;
    }

    private long nextExtractionTime() {
        return System.currentTimeMillis() + 1000 * ageInSeconds;
    }

    @Override
    public void purgeOldRecords() {
        synchronized (stats) {
            List<String> toRemove = new ArrayList<String>();
            for (PresenceStat s : stats.values()) {
                if (s.expiredInSeconds(ageInSeconds)) {
                    toRemove.add(s.getInstance());
                }
            }
            for (String k : toRemove) {
                stats.remove(k);
            }
            history.clear();
        }
    }

}
