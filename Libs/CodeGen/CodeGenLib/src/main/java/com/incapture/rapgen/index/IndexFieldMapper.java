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
package com.incapture.rapgen.index;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * @author bardhi
 * @since 4/8/15.
 */
public class IndexFieldMapper {
    private static final Map<String, String> RAW_TO_INDEX_TYPE = createTypeMap();

    private static Map<String, String> createTypeMap() {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        builder.put("Long", "long");
        builder.put("String", "string");
        builder.put("boolean", "boolean");
        builder.put("Integer", "integer");
        builder.put("Double", "double");
        builder.put("WorkerExecutionState", "string");
        builder.put("ReplyProgress", "string");
        return builder.build();
    }

    static String fieldToType(String packageName, String className, String indexName, Map<String, String> fieldNameToType, String name) {
        String rawType = fieldNameToType.get(name);
        if (rawType == null) {
            throw new IllegalArgumentException(String.format("Unknown type in index %s, for field [%s] in %s.%s", indexName, name, packageName, className));
        } else {
            String type = RAW_TO_INDEX_TYPE.get(rawType);
            if (type == null) {
                throw new IllegalArgumentException(
                        String.format("Unsupported type %s in index %s for field [%s] in %s.%s", rawType, indexName, name, packageName, className));
            } else {
                return type;
            }
        }
    }
}
