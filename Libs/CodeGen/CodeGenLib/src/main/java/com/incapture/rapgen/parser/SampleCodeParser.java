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
package com.incapture.rapgen.parser;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.MemberValuePair;
import japa.parser.ast.expr.NormalAnnotationExpr;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.annotations.SampleCode;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Given a set of sourcePaths, parse all the *.type files and find the functions that have our custom {@literal @}SampleCode annotation. Provides a
 * getSampleCode function to get the source code, if given the type, api name, and function name.
 * 
 * Example usage:
 * 
 * <pre>
 * {
 *     &#064;code
 *     SampleCodeParser p = new SampleCodeParser();
 *     p.addSourcePath(SampleCodeParser.SourceType.java, &quot;/path/to/java/srcfiles&quot;);
 *     p.parse();
 *     String sampleCode = p.getSampleCode(SampleCodeParser.SourceType.java, &quot;doc&quot;, &quot;getContent&quot;);
 * }
 * </pre>
 * 
 * @author dukenguyen
 * 
 */
public class SampleCodeParser {

    private static final Logger log = Logger.getLogger(SampleCodeParser.class);

    private Map<String, SourceType> sourcePaths = new HashMap<>();
    private Table<String, String, String> cache = HashBasedTable.create();

    public enum SourceType {
        java, py
    };

    /**
     * Load a sourcePath and its corresponding source code type for parsing later on.
     * 
     * @param type
     *            Must be one of the SampleCodeParser.SourceType enumerated values. Specifies the extension of the files to search as well as how to process
     *            them.
     * @param sourcePath
     *            A folder on the filesystem to search for *.type files to find the annotated functions
     */
    public void addSourcePath(SourceType type, String sourcePath) {
        if (StringUtils.isBlank(sourcePath) || type == null) {
            return;
        }
        sourcePaths.put(sourcePath, type);
    }

    /**
     * Remove a sourcePath from the pre-loaded paths. This path will not be parsed after successful removal.
     * 
     * @param sourcePath
     *            the sourcePath to remove
     */
    public void removeSourcePath(String sourcePath) {
        sourcePaths.remove(sourcePath);
    }

    /**
     * Get the source code for the given type, api, and function name
     * 
     * @param type
     *            Must be one of the enumerated values in SampleCodeParser.SourceType
     * @param api
     *            API name e.g. doc, structured, series, sheet
     * @param functionName
     *            The name of the function e.g. getContent
     * @return String representing the body of the associated function that was annotated with {@literal @}SampleCode, or null if not found
     */
    public String getSampleCode(SourceType type, String api, String functionName) {
        if (type == null) {
            return null;
        }
        return cache.get(getCacheRowKey(type, api), functionName);
    }

    /**
     * Parse all the pre-loaded sourcePaths for all *.type files that have a {@literal @}SampleCode annotated function.
     * 
     * @throws SampleCodeParserException
     *             if there was a problem parsing the source
     */
    public void parse() throws SampleCodeParserException {
        cache.clear();
        for (Map.Entry<String, SourceType> entry : sourcePaths.entrySet()) {
            Collection<File> files = FileUtils.listFiles(new File(entry.getKey()), new String[] { entry.getValue().name() }, true);
            switch (entry.getValue()) {
                case java:
                    for (File file : files) {
                        FileInputStream in = null;
                        CompilationUnit cu = null;
                        try {
                            in = new FileInputStream(file);
                            cu = JavaParser.parse(in);
                        } catch (ParseException | FileNotFoundException e) {
                            throw new SampleCodeParserException(e);
                        } finally {
                            if (in != null) {
                                try {
                                    in.close();
                                } catch (IOException e) {
                                    log.warn("Failed to close FileInputStream for file: " + e.toString());
                                }
                            }
                        }
                        new MethodVisitor<>(entry.getValue()).visit(cu, cache);
                    }
                    break;
                case py:
                    break;
            }
        }
    }

    private class MethodVisitor<A extends Table<String, String, String>> extends VoidVisitorAdapter<A> {

        private SourceType type;

        public MethodVisitor(SourceType type) {
            this.type = type;
        }

        @Override
        public void visit(MethodDeclaration method, A result) {
            if (CollectionUtils.isEmpty(method.getAnnotations())) {
                return;
            }
            for (AnnotationExpr annotation : method.getAnnotations()) {
                if (!annotation.getClass().equals(NormalAnnotationExpr.class)) {
                    continue;
                }
                NormalAnnotationExpr annot = (NormalAnnotationExpr) annotation;
                if (annot.getName().toString().equals(SampleCode.class.getSimpleName())
                        && !CollectionUtils.isEmpty(annot.getPairs())) {
                    for (MemberValuePair pair : annot.getPairs()) {
                        // get the 'api' parameter from the annotation to let us know which api this function belongs to
                        if (StringUtils.equals(pair.getName(), "api") && !StringUtils.isBlank(pair.getValue().toString())) {
                            result.put(getCacheRowKey(type, pair.getValue().toString().replace("\"", "")), stripTestPrefix(method.getName()),
                                    stripCurlyBrackets(method.getBody().toString()));
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Strip leading 'test' from any function names and apply proper capitalization
     * 
     * @param functionName
     *            The function to be adjusted
     * @return functionName without leading 'test' and proper capitalization
     */
    String stripTestPrefix(String functionName) {
        if (StringUtils.isBlank(functionName)) {
            return null;
        }
        if (StringUtils.startsWithIgnoreCase(functionName, "test")) {
            return StringUtils.uncapitalize(functionName.substring(4));
        }
        return functionName;
    }

    /**
     * Remove leading and trailing curly brackets from source code
     * 
     * @param sourceCode
     *            The source code to be modified
     * @return sourceCode with leading and trailing curly bracket removed
     */
    String stripCurlyBrackets(String sourceCode) {
        if (StringUtils.isBlank(sourceCode)) {
            return null;
        }
        String ret = sourceCode.trim();
        if (ret.startsWith("{")) {
            ret = ret.substring(1);
        }
        if (ret.endsWith("}")) {
            ret = ret.substring(0, ret.length() - 1);
        }
        return ret;
    }

    private String getCacheRowKey(SourceType type, String api) {
        return String.format("%s.%s", type.name(), api);
    }

}