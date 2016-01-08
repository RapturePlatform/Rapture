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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

import com.incapture.rapgen.AbstractTTree;
import com.incapture.rapgen.annotations.IndexedAnnotation;

public class IndexTemplateFactory {
    public static StringTemplate createIndexTemplate(String typeName, String sdkName, String packageName, List<IndexedAnnotation> indices,
            Map<String, String> fieldNameToType, StringTemplateGroup templateLib) {
        AbstractTTree.STAttrMap attrMap = new AbstractTTree.STAttrMap().put("name", typeName).put("sdkName", sdkName).put("package", packageName);

        List<StringTemplate> functionCalls = new LinkedList<StringTemplate>();
        List<StringTemplate> functionDefinitions = new LinkedList<StringTemplate>();
        for (IndexedAnnotation indexedAnnotation : indices) {
            StringTemplate createIndexDefinition = createIndexDefinitionTemplate(templateLib, packageName, typeName, indexedAnnotation, fieldNameToType);
            functionDefinitions.add(createIndexDefinition);

            StringTemplate indexCall = createIndexCallTemplate(templateLib, indexedAnnotation);
            functionCalls.add(indexCall);
        }

        if (indices.size() > 0) {
            attrMap.put("functionDefinitions", functionDefinitions);
            attrMap.put("functionCalls", functionCalls);
        }

        return templateLib.getInstanceOf("beanIndexInfoClass", attrMap);
    }

    private static StringTemplate createIndexDefinitionTemplate(StringTemplateGroup templateLib, String packageName, String className,
            IndexedAnnotation indexedAnnotation,
            Map<String, String> fieldNameToType) {
        List<StringTemplate> fields = createIndexFieldsTemplates(templateLib, packageName, className, indexedAnnotation, fieldNameToType);
        Map<?, ?> definitionMap = new AbstractTTree.STAttrMap().put("name", indexedAnnotation.getName()).put("fields", fields);
        return templateLib.getInstanceOf("createIndexDefinition", definitionMap);
    }

    private static List<StringTemplate> createIndexFieldsTemplates(StringTemplateGroup templateLib, String packageName, String className,
            IndexedAnnotation indexedAnnotation, Map<String, String> fieldNameToType) {
        List<StringTemplate> templates = new LinkedList<>();
        for (String name : indexedAnnotation.getFields()) {
            String type = IndexFieldMapper.fieldToType(packageName, className, indexedAnnotation.getName(), fieldNameToType, name);
            Map<?, ?> attrMap = new AbstractTTree.STAttrMap().put("name", name).put("type", type);
            templates.add(templateLib.getInstanceOf("indexField", attrMap));
        }
        return templates;
    }

    private static StringTemplate createIndexCallTemplate(StringTemplateGroup templateLib, IndexedAnnotation indexedAnnotation) {
        Map<?, ?> callMap = new AbstractTTree.STAttrMap().put("name", indexedAnnotation.getName());
        return templateLib.getInstanceOf("createIndexCall", callMap);
    }
}
