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
package rapture.stat.dummy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rapture.stat.BaseStat;
import rapture.stat.CounterStat;
import rapture.stat.IStatStore;
import rapture.stat.PresenceStat;
import rapture.stat.StatType;
import rapture.stat.StringStat;
import rapture.stat.ValueStat;

public class DummyStatStore implements IStatStore {

    @Override
    public boolean calculateStat(String key) {
        return true;
    }

    @Override
    public void defineKey(String key, StatType type) {
    }

    private Map<String, BaseStat> defStat = new HashMap<String, BaseStat>();
    private List<BaseStat> defList = new ArrayList<BaseStat>();
    private List<String> defKeys = new ArrayList<String>();

    @Override
    public Map<String, BaseStat> getCurrentStats() {
        return defStat;
    }

    @Override
    public List<? extends BaseStat> getHistory(String key, int recordCount) {
        return defList;
    }

    @Override
    public List<String> getKeys() {
        return defKeys;
    }

    @Override
    public void purgeOldRecords(String key) {
    }

    @Override
    public void recordCounter(CounterStat value) {
    }

    @Override
    public void recordMessage(StringStat message) {
    }

    @Override
    public void recordPresence(PresenceStat presence) {
    }

    @Override
    public void recordValue(ValueStat value) {
    }

    @Override
    public void setConfig(Map<String, String> config) {
    }

    @Override
    public void setInstance(String instanceName) {
    }

}
