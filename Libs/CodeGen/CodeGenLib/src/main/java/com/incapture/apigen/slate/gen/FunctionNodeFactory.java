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
package com.incapture.apigen.slate.gen;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.incapture.apigen.SampleCodeFactory;
import com.incapture.apigen.slate.function.Field;
import com.incapture.apigen.slate.function.Function;
import com.incapture.apigen.slate.function.Parameter;
import com.incapture.apigen.slate.type.TypesContainer;
import com.incapture.slate.model.FunctionNode;
import com.incapture.slate.model.node.AsideNode;
import com.incapture.slate.model.node.AsideNodeType;
import com.incapture.slate.model.node.EmptyLineNode;
import com.incapture.slate.model.node.Node;
import com.incapture.slate.model.node.TextNode;
import com.incapture.slate.model.node.WrapperNode;
import com.incapture.slate.model.node.code.CodeLanguageNode;
import com.incapture.slate.model.node.description.HeaderNode;
import com.incapture.slate.model.node.description.table.TableHeaderNode;
import com.incapture.slate.model.node.description.table.TableNode;
import com.incapture.slate.model.node.description.table.TableRowNode;

/**
 * @author bardhi
 * @since 6/5/15.
 */
public class FunctionNodeFactory {
    public static FunctionNode createFunctionNode(String apiName, Function function, TypesContainer typesContainer) {
        String functionName = function.getName();
        FunctionNode functionNode = new FunctionNode(functionName);
        WrapperNode descriptionNode = new WrapperNode();
        List<Node> descriptionNodes = descriptionNode.getNodes();
        if (function.isDeprecated()) {
            if (function.getDeprecatedText() != null) {
                descriptionNodes.add(new AsideNode("This function is deprecated: " + function.getDeprecatedText(), AsideNodeType.WARNING));
            } else {
                descriptionNodes.add(new AsideNode("This function is deprecated.", AsideNodeType.WARNING));
            }
        }

        if (!StringUtils.isBlank(function.getVersionSince())) {
            descriptionNodes.add(new TextNode("`since version " + function.getVersionSince() + "`"));
            descriptionNodes.add(new EmptyLineNode());
        }
        descriptionNodes.add(new TextNode(String.format("**Entitlement**: `%s`", function.getEntitlement() )));
        descriptionNodes.add(new EmptyLineNode());

        descriptionNodes.add(new TextNode(function.getDocumentation()));
        descriptionNodes.add(new EmptyLineNode());

        addFunctionParameters(function, descriptionNodes);

        addReturnValue(function, descriptionNodes);

        addTypeNodes(function, typesContainer, functionNode, descriptionNode, descriptionNodes);

        addCodeNode(apiName, function, functionNode);

        return functionNode;

    }

    private static void addCodeNode(String apiName, Function function, FunctionNode functionNode) {
        WrapperNode codeWrapperNode = new WrapperNode();
        CodeLanguageNode javaNode = new CodeLanguageNode(SampleCodeFactory.INSTANCE.sampleCodeForJava(apiName, function), "java");
        codeWrapperNode.getNodes().add(javaNode);
        CodeLanguageNode pythonNode = new CodeLanguageNode(SampleCodeFactory.INSTANCE.sampleCodeForPython(apiName, function), "python");
        codeWrapperNode.getNodes().add(pythonNode);
        functionNode.setCodeWrapperNode(codeWrapperNode);
    }

    private static void addTypeNodes(Function function, TypesContainer typesContainer, FunctionNode functionNode, WrapperNode descriptionNode,
            List<Node> descriptionNodes) {
        Collection<Node> typesNodes = TypeNodeFactory.createTypesNodes(function, typesContainer);
        if (typesNodes.size() > 0) {
            descriptionNodes.add(new AsideNode("Types used in this function", AsideNodeType.NOTICE));
            descriptionNodes.add(new EmptyLineNode());
            descriptionNodes.addAll(typesNodes);
        }
        functionNode.setDescriptionNode(descriptionNode);
    }

    private static void addReturnValue(Function function, List<Node> descriptionNodes) {
        /**
         * Return value
         */
        descriptionNodes.add(new HeaderNode("Return value"));
        descriptionNodes.add(new EmptyLineNode());
        TableNode tableNode = new TableNode(new TableHeaderNode("Type", "Description"));
        Field returnType = function.getReturnType();
        tableNode.getRows().add(new TableRowNode(returnType.getType(), returnType.getDocumentation()));
        descriptionNodes.add(tableNode);
    }

    private static void addFunctionParameters(Function function, List<Node> descriptionNodes) {
        /**
         * Function parameters
         */
        descriptionNodes.add(new HeaderNode("Function Parameters"));
        descriptionNodes.add(new EmptyLineNode());
        if (function.getParameters().size() > 0) {
            TableNode tableNode = new TableNode(new TableHeaderNode("Parameter", "Type", "Description"));
            for (Parameter parameter : function.getParameters()) {
                tableNode.getRows().add(new TableRowNode(parameter.getName(), parameter.getType(), parameter.getDocumentation()));
            }
            descriptionNodes.add(tableNode);
        } else {
            descriptionNodes.add(new TextNode("This function takes no parameters."));
        }
    }
}
