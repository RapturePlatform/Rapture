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
package reflex.module;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.debug.IReflexDebugger;
import reflex.importer.Module;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

public class ReflexStatistics implements Module {

    @Override
    public ReflexValue keyholeCall(String name, List<ReflexValue> parameters) {
        return new ReflexVoidValue();
    }

    @Override
    public boolean handlesKeyhole() {
        return false;
    }

    @Override
    public boolean canUseReflection() {
        return true;
    }

    @Override
    public void configure(List<ReflexValue> parameters) {
    }

    @Override
    public void setReflexHandler(IReflexHandler handler) {
    }

    @Override
    public void setReflexDebugger(IReflexDebugger debugger) {

    }

    public ReflexValue statistics(List<ReflexValue> params) {
        if (params.size() != 1) {
            throw new ReflexException(-1, "statistics needs one list parameter");
        }
        if (!params.get(0).isList()) {
            throw new ReflexException(-1, "statistics needs one list parameter");
        }
        DescriptiveStatistics stats = new DescriptiveStatistics();
        List<ReflexValue> values = params.get(0).asList();
        for (ReflexValue v : values) {
            stats.addValue(v.asDouble());
        }
        Map<String, ReflexValue> ret = new HashMap<String, ReflexValue>();
        ret.put("mean", new ReflexValue(stats.getMean()));
        ret.put("std", new ReflexValue(stats.getStandardDeviation()));
        ret.put("median", new ReflexValue(stats.getPercentile(50)));
        return new ReflexValue(ret);
    }

    public ReflexValue frequency(List<ReflexValue> params) {
        if (params.size() != 1) {
            throw new ReflexException(-1, "frequency needs one list parameter");
        }
        if (!params.get(0).isList()) {
            throw new ReflexException(-1, "frequency needs one list parameter");
        }
        Frequency f = new Frequency();
        List<ReflexValue> values = params.get(0).asList();
        for (ReflexValue v : values) {
            f.addValue(v.asDouble());
        }
        return new ReflexValue(f);
    }

    public ReflexValue frequency_count(List<ReflexValue> params) {
        if (params.size() != 2) {
            throw new ReflexException(-1, "frequency_count needs one frequency parameter and one value parameter");
        }
        Frequency f = params.get(0).asObjectOfType(Frequency.class);
        double value = params.get(1).asDouble();
        return new ReflexValue(f.getCount(value));
    }

    public ReflexValue frequency_cum_pct(List<ReflexValue> params) {
        if (params.size() != 2) {
            throw new ReflexException(-1, "frequency_count needs one frequency parameter and one value parameter");
        }
        Frequency f = params.get(0).asObjectOfType(Frequency.class);
        double value = params.get(1).asDouble();
        return new ReflexValue(f.getCumPct(value));
    }
}
