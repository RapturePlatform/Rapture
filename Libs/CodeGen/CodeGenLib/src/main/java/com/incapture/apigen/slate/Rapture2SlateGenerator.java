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
package com.incapture.apigen.slate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.incapture.apigen.slate.function.Function;
import com.incapture.apigen.slate.gen.FunctionNodeFactory;
import com.incapture.apigen.slate.type.TypeDefinition;
import com.incapture.apigen.slate.type.TypesContainer;
import com.incapture.slate.model.node.EmptyLineNode;
import com.incapture.slate.model.node.ApiNode;
import com.incapture.slate.model.node.AsideNode;
import com.incapture.slate.model.node.AsideNodeType;
import com.incapture.slate.model.node.HrefNode;
import com.incapture.slate.model.node.IndexNode;
import com.incapture.slate.model.node.LanguageNode;
import com.incapture.slate.model.node.TextNode;
import com.incapture.slate.model.node.WrapperNode;
import com.incapture.slate.model.node.code.CodeAnnotationNode;
import com.incapture.slate.model.node.code.CodeLanguageNode;

public class Rapture2SlateGenerator {
    public ApiNodeWrapper generate(String version, String minVersion, List<Api> apis, List<TypeDefinition> typeDefinitions) {
        ApiNodeWrapper wrapper = new ApiNodeWrapper();
        IndexNode indexNode = createIndexNode();
        wrapper.setIndexNode(indexNode);
        List<ApiNode> apiNodes = new LinkedList<>();
        apiNodes.add(createIntroNode(version, minVersion));
        try {
            apiNodes.add(createAuthNode());
        } catch (IOException e) {
            throw new RuntimeException("Error creating auth node", e);
        }
        for (Api api : sortByName(apis)) {
            apiNodes.add(createApiNode(api, new TypesContainer(typeDefinitions)));
        }
        wrapper.setApiNodes(apiNodes);
        return wrapper;
    }

    private List<Api> sortByName(List<Api> apis) {
        ArrayList<Api> sortedApis = new ArrayList<>(apis);
        Collections.sort(sortedApis, new ApiNameComparator());
        return sortedApis;
    }

    private ApiNode createApiNode(Api api, TypesContainer typesContainer) {
        ApiNode apiNode = new ApiNode(api.getName() + " API");
        WrapperNode descriptionNode = new WrapperNode();
        apiNode.setDescriptionNode(descriptionNode);
        if (api.getDocumentation() != null) {
            descriptionNode.getNodes().add(new TextNode(api.getDocumentation()));
            descriptionNode.getNodes().add(new EmptyLineNode());
        }

        if (api.isDeprecated()) {
            if (api.getDeprecatedText() != null) {
                descriptionNode.getNodes().add(new AsideNode("This api is deprecated: " + api.getDeprecatedText(), AsideNodeType.WARNING));
            } else {
                descriptionNode.getNodes().add(new AsideNode("This api is deprecated.", AsideNodeType.WARNING));
            }
        }
        for (Function function : api.getFunctions()) {
            apiNode.getFunctions().add(FunctionNodeFactory.createFunctionNode(api.getName(), function, typesContainer));
        }
        return apiNode;

    }

    private ApiNode createAuthNode() throws IOException {
        ApiNode api = new ApiNode("Authentication");

        WrapperNode descriptionNode = new WrapperNode();
        descriptionNode.getNodes().add(new TextNode("Rapture uses username and password to log in. Look at the code samples on the right for a how-to."));
        descriptionNode.getNodes().add(new EmptyLineNode());
        descriptionNode.getNodes().add(new TextNode("`Host: localhost`"));
        descriptionNode.getNodes().add(new EmptyLineNode());
        descriptionNode.getNodes().add(new TextNode("`Username: rapture`"));
        descriptionNode.getNodes().add(new EmptyLineNode());
        descriptionNode.getNodes().add(new TextNode("`Password: password`"));
        descriptionNode.getNodes().add(new EmptyLineNode());
        descriptionNode.getNodes()
                .add(new AsideNode("You must replace <code>localhost</code> as well as username and password with your own values.", AsideNodeType.NOTICE));
        api.setDescriptionNode(descriptionNode);

        WrapperNode codeWrapperNode = new WrapperNode();
        codeWrapperNode.getNodes().add(new CodeAnnotationNode("To log in and get authenticated, use this code:"));
        codeWrapperNode.getNodes().add(new EmptyLineNode());
        CodeLanguageNode javaNode = new CodeLanguageNode(readResource("/slate/code/auth/java.txt"), "java");
        codeWrapperNode.getNodes().add(javaNode);
        CodeLanguageNode pythonNode = new CodeLanguageNode(readResource("/slate/code/auth/python.txt"), "python");
        codeWrapperNode.getNodes().add(pythonNode);
        codeWrapperNode.getNodes().add(new CodeAnnotationNode("You must replace <code>localhost</code> with your URI."));
        api.setCodeWrapperNode(codeWrapperNode);

        return api;
    }

    private ApiNode createIntroNode(String version, String minVersion) {
        ApiNode api = new ApiNode("Introduction");
        WrapperNode descriptionNode = new WrapperNode();
        descriptionNode.getNodes().add(new TextNode("**API Version: " + version +"**"));
        descriptionNode.getNodes().add(new EmptyLineNode());
        try {
            descriptionNode.getNodes().add(new TextNode(readResource("/slate/description/intro.txt")));
        } catch (IOException e) {
            throw new RuntimeException("Error getting resource for intro node", e);
        }
        api.setDescriptionNode(descriptionNode);
        return api;

    }

    private IndexNode createIndexNode() {
        IndexNode indexNode = new IndexNode();
        indexNode.setTitle("Rapture API Docs");
        indexNode.getLanguages().add(new LanguageNode("java"));
        indexNode.getLanguages().add(new LanguageNode("python"));
        indexNode.getTocFooters().add(new TextNode("by Incapture Technologies"));
        indexNode.getTocFooters().add(new HrefNode("Documentation Powered by Slate", "http://github.com/tripit/slate"));
        indexNode.setSearchable(true);
        return indexNode;
    }

    private String readResource(String resource) throws IOException {
        InputStream stream = getClass().getResourceAsStream(resource);
        String data = StringUtils.strip(IOUtils.toString(stream));
        stream.close();
        return data;
    }
}
