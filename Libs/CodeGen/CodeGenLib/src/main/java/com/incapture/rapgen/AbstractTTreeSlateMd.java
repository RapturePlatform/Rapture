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

import rapture.common.exception.ExceptionToString;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.TreeNodeStream;
import org.apache.log4j.Logger;

import com.incapture.apigen.SampleCodeFactory;
import com.incapture.apigen.slate.Api;
import com.incapture.apigen.slate.ApiNodeWrapper;
import com.incapture.apigen.slate.Rapture2SlateGenerator;
import com.incapture.apigen.slate.function.Field;
import com.incapture.apigen.slate.function.Function;
import com.incapture.apigen.slate.function.Parameter;
import com.incapture.apigen.slate.type.TypeDefinition;
import com.incapture.rapgen.docString.DocumentationParser;
import com.incapture.rapgen.parser.SampleCodeParser;
import com.incapture.rapgen.parser.SampleCodeParserException;
import com.incapture.slate.DocGenerator;

public abstract class AbstractTTreeSlateMd extends AbstractTTree {
    private List<Api> apis = new LinkedList<>();
    private String version;
    private String minVersion;
    private List<TypeDefinition> typeDefinitions = new LinkedList<>();

    /**
     * Here we store type information (name and package really)
     */

    protected String getFileNameForApiDoc(String apiName, String string) {
        return apiName + ".md";
    }

    /**
     * Create a new parser instance, pre-supplying the input token stream.
     *
     * @param input The stream of tokens that will be pulled from the lexer
     */
    protected AbstractTTreeSlateMd(TreeNodeStream input) {
        super(input);
    }

    /**
     * Create a new parser instance, pre-supplying the input token stream and
     * the shared state.
     *
     * This is only used when a grammar is imported into another grammar, but we
     * must supply this constructor to satisfy the super class contract.
     *
     * @param input The stream of tokesn that will be pulled from the lexer
     * @param state The shared state object created by an interconnectd grammar
     */
    protected AbstractTTreeSlateMd(TreeNodeStream input, RecognizerSharedState state) {
        super(input, state);
    }

    /**
     * Creates the error/warning message that we need to show users/IDEs when
     * ANTLR has found a parsing error, has recovered from it and is now telling
     * us that a parsing exception occurred.
     *
     * @param tokenNames token names as known by ANTLR (which we ignore)
     * @param e          The exception that was thrown
     */
    @Override
    public void displayRecognitionError(String[] tokenNames, RecognitionException e) {

        // This is just a place holder that shows how to override this method
        //
        super.displayRecognitionError(tokenNames, e);
    }

    private static final Logger log = Logger.getLogger(AbstractTTreeSlateMd.class);

    protected void addType(String packageName, String typeName, List<Field> fields, String documentation, boolean isDeprecated, String deprecatedText) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("addType: packageName=[%s], typeName=[%s], fields.size()=[%s], documentation=[%s], isDeprecated=[%s], deprecatedText=[%s]",
                    packageName, typeName, fields.size(), documentation, isDeprecated, deprecatedText));
        }
        TypeDefinition typeDefinition = new TypeDefinition();
        typeDefinition.setPackageName(packageName);
        typeDefinition.setName(typeName);
        typeDefinition.setFields(fields);
        typeDefinition.setDocumentation(documentation);
        typeDefinition.setIsDeprecated(isDeprecated);
        typeDefinition.setDeprecatedText(deprecatedText);
        typeDefinitions.add(typeDefinition);
    }

    protected void addApi(String apiName, List<Function> functions, String apiDoc, boolean isDeprecated, String deprecatedText) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("addApi: apiName=[%s], functions=[%s], apiDoc=[%s]", apiName, functions, apiDoc));
        }

        updateFunctionDoc(functions);

        Api api = new Api();
        api.setName(apiName);
        api.setFunctions(functions);
        api.setDocumentation(apiDoc);
        api.setIsDeprecated(isDeprecated);
        api.setIsDeprecated(isDeprecated);
        api.setDeprecatedText(deprecatedText);
        apis.add(api);
    }

    private void updateFunctionDoc(List<Function> functions) {
        for (Function function : functions) {
            String raw = function.getDocumentation();
            if (raw != null && raw.length() > 0) {
                String description = DocumentationParser.retrieveDescription(raw);
                function.setDocumentation(description);

                Map<String, String> paramToDoc = DocumentationParser.retrieveParams(raw);
                for (Parameter parameter : function.getParameters()) {
                    String paramDoc = paramToDoc.get(parameter.getName());
                    if (paramDoc != null) {
                        parameter.setDocumentation(paramDoc);
                    }
                }
                String since = DocumentationParser.retrieveSince(raw);
                function.setVersionSince(since);
            } else {
                log.warn(String.format("Function %s has no description.", function.getName()));
            }
        }
    }

    protected void addVersionInfo(int major, int minor, int micro) {
        version = String.format("%s.%s.%s", major, minor, micro);
    }

    protected void addMinVersionInfo(int major, int minor, int micro) {
        minVersion = String.format("%s.%s.%s", major, minor, micro);
    }

    @Override
    public void dumpFiles(String outputKernelFolder, String outputApiFolder) {
        updateParameterPackages();
        ApiNodeWrapper wrapper = new Rapture2SlateGenerator().generate(version, minVersion, apis, typeDefinitions);
        File rootDir = new File(outputApiFolder);
        try {
            log.info(String.format("dumping api files to %s", rootDir.getAbsolutePath()));
            new DocGenerator().generateOutput(rootDir, wrapper.getIndexNode(), wrapper.getApiNodes());
        } catch (IOException e) {
            log.error(ExceptionToString.format(e));
        }
    }

    protected void updateParameterPackages() {
        Map<String, String> typeNameToPackage = new HashMap<>();
        for (TypeDefinition typeDefinition : typeDefinitions) {
            typeNameToPackage.put(typeDefinition.getName(), typeDefinition.getPackageName());
        }
        for (Api api : apis) {
            for (Function function : api.getFunctions()) {
                List<Field> toUpdate = new LinkedList<>();
                toUpdate.addAll(function.getParameters());
                toUpdate.add(function.getReturnType());
                for (Field parameter : toUpdate) {
                    if (parameter.getPackageName() == null) {
                        String packageName = typeNameToPackage.get(parameter.getType());
                        parameter.setPackageName(packageName);
                    }
                }
            }
        }
    }

    public void setupCodeParser(String codeSamplesJava, String codeSamplesPython) {
        SampleCodeParser parser = new SampleCodeParser();
        if (codeSamplesJava != null) {
            File file = new File(codeSamplesJava);
            parser.addSourcePath(SampleCodeParser.SourceType.java, file.getAbsolutePath());
        }
        if (codeSamplesPython != null) {
            File file = new File(codeSamplesPython);
            parser.addSourcePath(SampleCodeParser.SourceType.py, file.getAbsolutePath());
        }
        try {
            parser.parse();
        } catch (SampleCodeParserException e) {
            throw new RuntimeException("Error setting up sample code parser", e);
        }
        SampleCodeFactory.INSTANCE.setParser(parser);
    }
}
