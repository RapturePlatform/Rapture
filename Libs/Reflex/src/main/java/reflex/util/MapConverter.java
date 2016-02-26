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
package reflex.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import reflex.value.ReflexValue;

public class MapConverter {
    private MapConverter() {

    }

    /**
     * Make sure the members of this map are all ReflexValues of some shape or
     * form
     * 
     * @param map
     * @return
     */
    @SuppressWarnings("unchecked")
	public static Map<String, Object> convertMap(Map<String, Object> asMap) {
    	if (asMap == null) {
    		return null;
    	}
        Map<String, Object> converted = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Object> e : asMap.entrySet()) {
            if (e.getValue() instanceof ReflexValue) {
                ReflexValue val = (ReflexValue) e.getValue();
                if (val.isMap()) {
                    converted.put(e.getKey(), convertMap(val.asMap()));
                } else if (val.isList()) {
                    converted.put(e.getKey(), convert(val.asList()));
                } else if (val.isString()) {
                	converted.put(e.getKey(), val.asString());
                } else {                
                    converted.put(e.getKey(), val.asObject());
                }
            } else if (e.getValue() instanceof List) {
                List<Object> vals = (List<Object>) e.getValue();
                converted.put(e.getKey(), convertSimpleList(vals));
            } else if (e.getValue() instanceof Map) {
                Map<String, Object> vals = (Map<String, Object>) e.getValue();
                converted.put(e.getKey(), convertMap(vals));
            } else {
                converted.put(e.getKey(), e.getValue());
            }
        }
        return converted;
    }

	public static Object convertSimpleList(List<Object> asList) {
        List<Object> ret = new ArrayList<Object>(asList.size());
        for (Object o : asList) {
            if (o instanceof ReflexValue) {
                ReflexValue e = (ReflexValue) o;
                if (e.isMap()) {
                    ret.add(convertMap(e.asMap()));
                } else if (e.isList()) {
                    ret.add(convert(e.asList()));
                } else {
                    ret.add(e.asObject());
                }
            } else {
                ret.add(o);
            }
        }
        return ret;
    }
	
	public static Object convert(List<ReflexValue> asList) {
        List<Object> ret = new ArrayList<Object>(asList.size());
        for (Object o : asList) {
            if (o instanceof ReflexValue) {
                ReflexValue e = (ReflexValue) o;
                if (e.isMap()) {
                    ret.add(convertMap(e.asMap()));
                } else if (e.isList()) {
                    ret.add(convert(e.asList()));
                } else {
                    ret.add(e.asObject());
                }
            } else {
                ret.add(o);
            }
        }
        return ret;
    }
}
