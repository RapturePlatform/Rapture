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
package com.incapture.rapgen.persistence.generated.factory;

import rapture.common.exception.RaptureExceptionFactory;
import rapture.object.Storable;
import rapture.persistence.storable.mapper.StorableSerDeserHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.log4j.Logger;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.incapture.rapgen.AbstractTTree;
import com.incapture.rapgen.annotations.CacheableAnnotation;
import com.incapture.rapgen.annotations.IndexedAnnotation;
import com.incapture.rapgen.index.IndexTemplateFactory;
import com.incapture.rapgen.persistence.generated.PathAndTemplate;
import com.incapture.rapgen.storable.StorableAttributes;
import com.incapture.rapgen.storable.StorableTemplateFactory;

/**
 * @author bardhi
 * @since 7/21/15.
 */
public class StorableClassesFactory {

    private final StringTemplateGroup templateLib;

    public StorableClassesFactory(StringTemplateGroup templateLib) {
        this.templateLib = templateLib;
    }

    public Map<String, StringTemplate> generateClasses(Class<? extends Storable> storableClass, StorableAttributes storableAttribute,
            Optional<Class<? extends StorableSerDeserHelper>> serDeserClass, Optional<Class<? extends Storable>> extendedClass) {
        Map<String, StringTemplate> map = new HashMap<>();

        PathAndTemplate indexInfo = generateIndexInfoClass(storableAttribute.getTypeName(), storableAttribute.getPackagePath(), storableAttribute.getSdkName(),
                storableAttribute.getPackageName(), storableAttribute.getIndices(), storableAttribute.getFieldNameToType());
        map.put(indexInfo.getPath(), indexInfo.getTemplate());

        PathAndTemplate storableInfo = generateStorableInfoClass(storableAttribute.getTypeName(), storableAttribute.getPackagePath(),
                storableAttribute.getSdkName(),
                storableAttribute.getPackageName(), storableAttribute.getCacheable());
        map.put(storableInfo.getPath(), storableInfo.getTemplate());

        PathAndTemplate storage = generateStorageClass(storableClass, storableAttribute, serDeserClass, extendedClass);
        map.put(storage.getPath(), storage.getTemplate());

        return map;
    }

    private PathAndTemplate generateStorableInfoClass(String typeName, String packagePath, String sdkName, String packageName,
            CacheableAnnotation cacheableAnnotation) {
        StringTemplateGroup templateLib = getTemplateLib();

        StringTemplate template = StorableTemplateFactory.createTemplate(typeName, sdkName, packageName, cacheableAnnotation != null,
                cacheableAnnotation != null && cacheableAnnotation.isShouldCacheNulls(), templateLib);
        String classFullPath = AbstractTTree.GEN_PATH_PREFIX_JAVA + packagePath + "/" + typeName + "StorableInfo.java";
        return new PathAndTemplate(classFullPath, template);
    }

    private PathAndTemplate generateIndexInfoClass(String typeName, String packagePath, String sdkName, String packageName,
            List<IndexedAnnotation> indices, Map<String, String> fieldNameToType) {
        StringTemplateGroup templateLib = getTemplateLib();

        StringTemplate template = IndexTemplateFactory.createIndexTemplate(typeName, sdkName, packageName, indices, fieldNameToType, templateLib);
        String classFullPath = AbstractTTree.GEN_PATH_PREFIX_JAVA + packagePath + "/" + typeName + "IndexInfo.java";
        return new PathAndTemplate(classFullPath, template);

    }

    private PathAndTemplate generateStorageClass(Class<? extends Storable> storableClass, StorableAttributes storableAttributes,
            Optional<Class<? extends StorableSerDeserHelper>> serDeserClass, Optional<Class<? extends Storable>> extendedClass) {
        String storageClassFullPath =
                AbstractTTree.GEN_PATH_PREFIX_JAVA + storableAttributes.getPackagePath() + "/" + storableAttributes.getTypeName() + "Storage.java";

        StringTemplate objectMapperTemplate;
        objectMapperTemplate = templateLib.getInstanceOf("beanStorage_objectMapper");

        ImmutableMap.Builder<String, Object> storageClassAttributes = storableAttributes.getStorageClassAttributes();
        if (serDeserClass.isPresent()) {
            if (extendedClass == null) {
                throw RaptureExceptionFactory.create(String
                        .format("Serializer/deserializer [%s] found for storable [%s], but no extended class found!", serDeserClass.get().getName(),
                                storableClass.getName()));
            }
        }
        storageClassAttributes.put("serDeserCode", createSerDeserTemplate(storableClass, storableAttributes.getTypeName(), serDeserClass));
        storageClassAttributes.put("serDeserImports", createImportsTemplate(serDeserClass, extendedClass, storableAttributes.getSdkName()));

        storageClassAttributes.put("objectMapper", objectMapperTemplate).build();
        StringTemplate storageClass = getTemplateLib().getInstanceOf("beanStorageClass", storageClassAttributes.build());

        return new PathAndTemplate(storageClassFullPath, storageClass);
    }

    private StringTemplate createImportsTemplate(Optional<Class<? extends StorableSerDeserHelper>> serDeserClass,
            Optional<Class<? extends Storable>> extendedClass,
            String sdkName) {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        if (serDeserClass.isPresent()) {
            builder.put("serDeserFullName", serDeserClass.get().getName());
        }
        if (extendedClass.isPresent()) {
            builder.put("extendedName", extendedClass.get().getName());
        }
        String apiVersionName;
        if (sdkName != null) {
            apiVersionName = String.format("rapture.%s.server.ServerApiVersion", sdkName);
        } else {
            apiVersionName = "rapture.server.ServerApiVersion";
        }
        builder.put("apiVersionName", apiVersionName);

        return getTemplateLib().getInstanceOf("beanStorage_serDeserImports", builder.build());
    }

    private StringTemplate createSerDeserTemplate(Class<? extends Storable> storableClass, String typeName,
            Optional<Class<? extends StorableSerDeserHelper>> serDeserClass) {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        builder.put("type", typeName);
        if (serDeserClass.isPresent()) {
            builder.put("serDeserHelper", serDeserClass.get().getSimpleName());
        }
        builder.put("deserSetters", createDeserSetters(storableClass, typeName));

        return templateLib.getInstanceOf("beanStorage_serDeser", builder.build());
    }

    private static final Logger log = Logger.getLogger(StorableClassesFactory.class);
    private List<String> createDeserSetters(Class<? extends Storable> storableClass, String typeName) {
        List<String> deserSetters = new LinkedList<>();

        Set<String> methodNames = new HashSet<>();
        for (Method method : storableClass.getDeclaredMethods()) {
            methodNames.add(method.getName());
        }

        for (Field field : storableClass.getDeclaredFields()) {
            String name = field.getName();
            String capitalizedName = WordUtils.capitalize(name);
            //make sure there's a setter
            if (methodNames.contains("set" + capitalizedName)) {
                deserSetters.add(String.format("%s.set%2$s(extended.get%2s());", WordUtils.uncapitalize(typeName), capitalizedName));
            }
        }
        return deserSetters;
    }

    private StringTemplateGroup getTemplateLib() {
        return templateLib;
    }
}
