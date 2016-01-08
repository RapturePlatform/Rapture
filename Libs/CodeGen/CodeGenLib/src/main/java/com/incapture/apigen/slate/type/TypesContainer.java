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
package com.incapture.apigen.slate.type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;

public class TypesContainer {
    private final Map<String, Map<String, TypeDefinition>> packageToNameToType;

    public TypesContainer(List<TypeDefinition> typeDefinitions) {
        packageToNameToType = new HashMap<>();
        for (TypeDefinition typeDefinition : typeDefinitions) {
            Map<String, TypeDefinition> nameToType = packageToNameToType.get(typeDefinition.getPackageName());
            if (nameToType == null) {
                nameToType = new HashMap<>();
                packageToNameToType.put(typeDefinition.getPackageName(), nameToType);
            }
            nameToType.put(typeDefinition.getName(), typeDefinition);
        }
    }

    public Optional<TypeDefinition> get(String packageName, String typeName) {
        Map<String, TypeDefinition> nameToType = packageToNameToType.get(packageName);
        if (nameToType != null) {
            return Optional.fromNullable(nameToType.get(typeName));
        } else {
            return Optional.absent();
        }
    }
}
