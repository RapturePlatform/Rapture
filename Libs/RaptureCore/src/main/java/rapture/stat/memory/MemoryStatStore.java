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
import rapture.stat.CounterStat;
import rapture.stat.CounterStatType;
import rapture.stat.IStatStore;
import rapture.stat.PresenceStat;
import rapture.stat.PresenceStatType;
import rapture.stat.StatType;
import rapture.stat.StringStat;
import rapture.stat.StringStatType;
import rapture.stat.ValueStat;
import rapture.stat.ValueStatType;

/**
 * A MemoryStatStore is intended to be used for testing only.
 * 
 * @author amkimian
 * 
 */
public class MemoryStatStore implements IStatStore {
    private Map<String, StatType> keyDefs = new HashMap<String, StatType>();
    private Map<String, StringMemoryType> stringMemoryTypes = new HashMap<String, StringMemoryType>();
    private Map<String, PresenceMemoryType> presenceMemoryTypes = new HashMap<String, PresenceMemoryType>();
    private Map<String, ValueMemoryType> valueMemoryTypes = new HashMap<String, ValueMemoryType>();
    private Map<String, CounterMemoryType> counterMemoryTypes = new HashMap<String, CounterMemoryType>();

    public MemoryStatStore() {
        // Nothing to do here.
    }

    @Override
    public boolean calculateStat(String key) {
        if (stringMemoryTypes.containsKey(key)) {
            return stringMemoryTypes.get(key).calculate();
        } else if (presenceMemoryTypes.containsKey(key)) {
            return presenceMemoryTypes.get(key).calculate();
        } else if (valueMemoryTypes.containsKey(key)) {
            return valueMemoryTypes.get(key).calculate();
        } else if (counterMemoryTypes.containsKey(key)) {
            return counterMemoryTypes.get(key).calculate();
        }
        return false;
    }

    @Override
    public void defineKey(String key, StatType type) {
        keyDefs.put(key, type);
        if (type instanceof StringStatType) {
            StringStatType stype = (StringStatType) type;
            StringMemoryType smem = new StringMemoryType(stype.getMessages());
            stringMemoryTypes.put(key, smem);
        } else if (type instanceof PresenceStatType) {
            PresenceStatType pType = (PresenceStatType) type;
            PresenceMemoryType pmem = new PresenceMemoryType(key, pType.getSeconds());
            presenceMemoryTypes.put(key, pmem);
        } else if (type instanceof ValueStatType) {
            ValueStatType vType = (ValueStatType) type;
            ValueMemoryType tmem = new ValueMemoryType(key, vType.getOperation(), vType.getSeconds());
            valueMemoryTypes.put(key, tmem);
        } else if (type instanceof CounterStatType) {
            CounterMemoryType cmem = new CounterMemoryType(key);
            counterMemoryTypes.put(key, cmem);
        }
    }

    @Override
    public Map<String, BaseStat> getCurrentStats() {
        // For each key we have, get the *current stats* for the record
        // The current stat depends on the type, for most it is the latest
        // recorded
        // (capture) value
        Map<String, BaseStat> ret = new HashMap<String, BaseStat>();
        for (String key : stringMemoryTypes.keySet()) {
            StringMemoryType t = stringMemoryTypes.get(key);
            ret.put(key, t.getCurrentStats());
        }
        for (String key : presenceMemoryTypes.keySet()) {
            ret.put(key, presenceMemoryTypes.get(key).getCurrentStats());
        }
        for (String key : valueMemoryTypes.keySet()) {
            ret.put(key, valueMemoryTypes.get(key).getCurrentStats());
        }
        for (String key : counterMemoryTypes.keySet()) {
            ret.put(key, counterMemoryTypes.get(key).getCurrentStats());
        }
        return ret;
    }

    @Override
    public List<? extends BaseStat> getHistory(String key, int recordCount) {
        if (stringMemoryTypes.containsKey(key)) {
            return stringMemoryTypes.get(key).getHistory(recordCount);
        } else if (presenceMemoryTypes.containsKey(key)) {
            return presenceMemoryTypes.get(key).getHistory(recordCount);
        } else if (valueMemoryTypes.containsKey(key)) {
            return valueMemoryTypes.get(key).getHistory(recordCount);
        } else if (counterMemoryTypes.containsKey(key)) {
            return counterMemoryTypes.get(key).getHistory(recordCount);
        }
        return null;
    }

    @Override
    public List<String> getKeys() {
        List<String> ret = new ArrayList<String>();
        ret.addAll(keyDefs.keySet());
        return ret;
    }

    @Override
    public void purgeOldRecords(String key) {
        if (stringMemoryTypes.containsKey(key)) {
            stringMemoryTypes.get(key).purgeOldRecords();
        } else if (presenceMemoryTypes.containsKey(key)) {
            presenceMemoryTypes.get(key).purgeOldRecords();
        } else if (valueMemoryTypes.containsKey(key)) {
            valueMemoryTypes.get(key).purgeOldRecords();
        } else if (counterMemoryTypes.containsKey(key)) {
            counterMemoryTypes.get(key).purgeOldRecords();
        }
    }

    @Override
    public void recordCounter(CounterStat value) {
        CounterMemoryType cmem = counterMemoryTypes.get(value.getKey());
        if (cmem != null) {
            cmem.addValue(value);
        }
    }

    @Override
    public void recordMessage(StringStat message) {
        StringMemoryType smem = stringMemoryTypes.get(message.getKey());
        if (smem != null) {
            smem.addMessage(message);
        }
    }

    @Override
    public void recordPresence(PresenceStat presence) {
        PresenceMemoryType pmem = presenceMemoryTypes.get(presence.getKey());
        if (pmem != null) {
            pmem.addPresence(presence);
        }
    }

    @Override
    public void recordValue(ValueStat value) {
        ValueMemoryType vmem = valueMemoryTypes.get(value.getKey());
        if (vmem != null) {
            vmem.addValue(value);
        }
    }

    @Override
    public void setConfig(Map<String, String> config) {
    }

    @Override
    public void setInstance(String instanceName) {
    }

}
