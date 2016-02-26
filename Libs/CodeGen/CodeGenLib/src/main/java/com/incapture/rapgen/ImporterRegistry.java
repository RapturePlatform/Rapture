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
package com.incapture.rapgen;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class ImporterRegistry {

    private static final Map<String, String> IMPORTS_MAP;
    static {
        Builder<String, String> mapBuilder = ImmutableMap.<String, String> builder();
        mapBuilder.put("List", "java.util.List");
        mapBuilder.put("Map", "java.util.Map");
        mapBuilder.put("Set", "java.util.Set");
        mapBuilder.put("Date", "java.util.Date");
        mapBuilder.put("ArrayList", "java.util.ArrayList");
        mapBuilder.put("HashSet", "java.util.HashSet");
        mapBuilder.put("HashMap", "java.util.HashMap");
        mapBuilder.put("UUID", "java.util.UUID");
        IMPORTS_MAP = mapBuilder.build();
    }

    public static Collection<String> getImportList(Collection<String> varTypes) {
        Set<String> importSet = new HashSet<String>();
        for (String varType : varTypes) {
            for (String key : IMPORTS_MAP.keySet()) {
                if (varType.contains(key)) {
                    String importValue = IMPORTS_MAP.get(key);
                    if (importValue != null) {
                        importSet.add("import " + importValue + ";");
                    }
                }
            }
        }

        return importSet;
    }
}
