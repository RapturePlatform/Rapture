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
package com.incapture.rapgen;

import rapture.util.ResourceLoader;

import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.Map;
import java.util.TreeMap;

import org.antlr.stringtemplate.StringTemplateGroup;

public class TemplateRepo {
    private static final Map<String, Boolean> MULTI_FILE_LANGS = createMultiFileLangs();

    private static Map<String, Boolean> createMultiFileLangs() {
        TreeMap<String, Boolean> map = new TreeMap<String, Boolean>(String.CASE_INSENSITIVE_ORDER);
        map.put("Java", true);
        map.put("DotNet", true);
        map.put("CurtisDoc", true);
        return map;
    }

    public static StringTemplateGroup getApiTemplates(String language) {
        language = language.toLowerCase();
        return new ResourceBasedStringTemplateGroup(language, "/" + language + "/api/");
    }

    public static StringTemplateGroup getSdkTemplates(String language) {
        language = language.toLowerCase();
        return new ResourceBasedStringTemplateGroup(language, "/" + language + "/sdk/", "/" + language + "/api/");
    }

    public static StringTemplateGroup getApiSingleFileTemplates(String language) throws FileNotFoundException {
        String name = "/template/" + language + ".stg";
        String guts = ResourceLoader.getResourceAsString(new Object(), name);
        if (guts == null) {
            throw new FileNotFoundException("Template file not found: " + name);
        }
        StringReader reader = new StringReader(guts);
        return new StringTemplateGroup(reader);
    }

    public static StringTemplateGroup getSdkSingleFileTemplates(String language) throws FileNotFoundException {
        String name = "/template/" + language + "Addon.stg";
        String guts = ResourceLoader.getResourceAsString(new Object(), name);
        if (guts == null) {
            throw new FileNotFoundException("Template file not found: " + name);
        }
        StringReader reader = new StringReader(guts);
        return new StringTemplateGroup(reader);
    }

    public static StringTemplateGroup getTemplatesFromFile(String fileName) throws FileNotFoundException {
        String guts = ResourceLoader.getResourceAsString(new Object(), "/" + fileName);
        if (guts == null) {
            throw new FileNotFoundException("Template file not found: " + fileName);
        }
        StringReader reader = new StringReader(guts);
        return new StringTemplateGroup(reader);
    }

    public static StringTemplateGroup getTemplates(String language, GenType genType) throws FileNotFoundException {
        if (genType == GenType.SDK) {
            if (isMultiFile(language)) {
                return getSdkTemplates(language);
            } else {
                return getSdkSingleFileTemplates(language);
            }
        } else {
            if (isMultiFile(language)) {
                return getApiTemplates(language);
            } else {
                return getApiSingleFileTemplates(language);
            }
        }
    }

    private static boolean isMultiFile(String language) {
        return MULTI_FILE_LANGS.containsKey(language);
    }
}
