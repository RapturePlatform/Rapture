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
package rapture.stat;

import java.util.List;
import java.util.Map;

import rapture.dsl.metricgen.MetricFactory;

/**
 * A standard implementation of a stat api We define the stat implementation in
 * two parts - what is the config for the StatStore (which is key
 * settings/stat/store ) and the config for each key (which is the folder
 * traverse of settings/stat/keys/* )
 * 
 * When we see a key, we see if we've seen it before. If not, we load its config
 * and pass it to the store Almost everything else goes to the store.
 * 
 * Periodically we need to run the update() method to generate the history
 * 
 * @author amkimian
 * 
 */
public class StandardStatImpl implements IRaptureStatApi {
    private IStatStore statStore;

    @Override
    public void computeRecord(String key) {
        if (statStore.calculateStat(key)) {
            statStore.purgeOldRecords(key);
        }
    }

    @Override
    public void defineStat(String key, String config) {
        StatType st = MetricFactory.getStatType(config);
        if (st != null) {
            statStore.defineKey(key, st);
        }
    }

    @Override
    public Map<String, BaseStat> getCurrentStat() {
        return statStore.getCurrentStats();
    }

    @Override
    public List<? extends BaseStat> getHistory(String key) {
        return statStore.getHistory(key, 100);
    }

    @Override
    public List<String> getStatKeys() {
        return statStore.getKeys();
    }

    @Override
    public void recordCounter(String key, long delta) {
        CounterStat s = new CounterStat();
        s.setKey(key);
        s.setValue(new Long(delta));
        statStore.recordCounter(s);
    }

    @Override
    public void recordPresence(String key, String instanceName) {
        PresenceStat s = new PresenceStat();
        s.setInstance(instanceName);
        s.setKey(key);
        statStore.recordPresence(s);
    }

    @Override
    public void recordStringStat(String key, String value) {
        StringStat s = new StringStat();
        s.setKey(key);
        s.setMessage(value);
        statStore.recordMessage(s);
    }

    @Override
    public void recordValue(String key, long value) {
        ValueStat s = new ValueStat();
        s.setKey(key);
        s.setValue(new Double(value));
        statStore.recordValue(s);
    }

    public void setStatStore(IStatStore statStore) {
        this.statStore = statStore;
    }

}
