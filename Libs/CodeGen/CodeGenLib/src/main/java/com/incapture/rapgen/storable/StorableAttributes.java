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
package com.incapture.rapgen.storable;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.incapture.rapgen.annotations.CacheableAnnotation;
import com.incapture.rapgen.annotations.IndexedAnnotation;

/**
 * A wrapper for attributes parsed from API files that are used to build a storable. Things like class name, package name, member fields, etc. This should be
 * used when building the persistence class
 *
 * @author bardhi
 * @since 7/21/15.
 */
public class StorableAttributes {
    private final String typeName;
    private final String packagePath;
    private final String sdkName;
    private final String packageName;
    private final ImmutableMap.Builder<String, Object> storageClassAttributes;
    private final List<IndexedAnnotation> indices;
    private final Map<String, String> fieldNameToType;
    private final CacheableAnnotation cacheable;

    public StorableAttributes(String typeName, String packagePath, String sdkName, String packageName,
            ImmutableMap.Builder<String, Object> storageClassAttributes, List<IndexedAnnotation> indices, Map<String, String> fieldNameToType,
            CacheableAnnotation cacheable) {
        this.typeName = typeName;
        this.packagePath = packagePath;
        this.packageName = packageName;
        this.sdkName = sdkName;
        this.storageClassAttributes = storageClassAttributes;
        this.indices = indices;
        this.fieldNameToType = fieldNameToType;
        this.cacheable = cacheable;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getPackagePath() {
        return packagePath;
    }

    public String getSdkName() {
        return sdkName;
    }

    public String getPackageName() {
        return packageName;
    }

    public ImmutableMap.Builder<String, Object> getStorageClassAttributes() {
        return storageClassAttributes;
    }

    public List<IndexedAnnotation> getIndices() {
        return indices;
    }

    public Map<String, String> getFieldNameToType() {
        return fieldNameToType;
    }

    public CacheableAnnotation getCacheable() {
        return cacheable;
    }
}
