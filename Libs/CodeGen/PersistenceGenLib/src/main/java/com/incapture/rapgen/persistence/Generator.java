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
package com.incapture.rapgen.persistence;

import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.object.Storable;
import rapture.persistence.storable.mapper.StorableSerDeserHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

import com.google.common.base.Optional;
import com.incapture.rapgen.persistence.generated.factory.StorableClassesFactory;
import com.incapture.rapgen.storable.StorableAttributes;

/**
 * @author bardhi
 * @since 7/21/15.
 */
public class Generator {
    private final StringTemplateGroup templateLib;

    public Generator(StringTemplateGroup templateLib) {
        this.templateLib = templateLib;
    }

    public Map<String, StringTemplate> generatePersistenceFiles(List<StorableAttributes> storableAttributes, StorableSerDeserRepo storableMappersRepo) {
        StorableClassesFactory factory = new StorableClassesFactory(templateLib);

        Map<String, StringTemplate> pathToTemplate = new HashMap<>();

        for (StorableAttributes storableAttribute : storableAttributes) {
            String typeName = storableAttribute.getTypeName();
            String packageName = storableAttribute.getPackageName();
            Class<? extends Storable> storableClass;
            try {
                //noinspection unchecked
                storableClass = (Class<? extends Storable>) Class.forName(String.format("%s.%s", packageName, typeName));
            } catch (ClassNotFoundException e) {
                throw RaptureExceptionFactory.create(ExceptionToString.format(e));
            }
            Class<? extends StorableSerDeserHelper> serDeserHelper = storableMappersRepo.getSerDeserHelper(storableClass);

            pathToTemplate.putAll(factory
                    .generateClasses(storableClass, storableAttribute, Optional.<Class<? extends StorableSerDeserHelper>>fromNullable(serDeserHelper),
                            Optional.<Class<? extends Storable>>fromNullable(storableMappersRepo.getExtendedClass(storableClass))));

        }
        return pathToTemplate;
    }
}
