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

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import rapture.common.Hose;
import rapture.common.exception.RaptureExceptionFactory;

import com.google.common.collect.Maps;

public class HoseRegistry {
    private static Map<String,HoseFactory> name2hose = Maps.newHashMap();
    
    static { 
        // built-in functions
        register("load", new LoadHose.Factory());
        register("mavg", new MavgHose.Factory());
        register("sample", new SampleHose.Factory());
        register("split", new SplitterHose.Factory());
        register("total", new RunningTotalHose.Factory());
        register("store", new StoreHose.Factory());
        register("skipNaN", new SkipNaNHose.Factory());
        register("toFile", new FileHose.Factory());
        register("series2csv", new CSVHose.Factory());
    }
    
    // TODO use different registries for different run modes
    public static Hose call(String funcName, List<HoseArg> args) {
        HoseFactory factory = name2hose.get(funcName);
        if (factory == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Unknown series function " + funcName);
        } else {
            return factory.make(args);
        } 
    }
    
    public static void register(String name, HoseFactory factory) {
        if (name2hose.put(name, factory) != null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Duplicate function definition "+name);
        }
    }
}
