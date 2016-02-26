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
package com.incapture.apigen.slate.gen;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Optional;
import com.incapture.apigen.slate.function.Field;
import com.incapture.apigen.slate.function.Function;
import com.incapture.apigen.slate.type.TypeDefinition;
import com.incapture.apigen.slate.type.TypesContainer;
import com.incapture.slate.model.node.EmptyLineNode;
import com.incapture.slate.model.node.AsideNode;
import com.incapture.slate.model.node.AsideNodeType;
import com.incapture.slate.model.node.Node;
import com.incapture.slate.model.node.TextNode;
import com.incapture.slate.model.node.description.HeaderNode;
import com.incapture.slate.model.node.description.table.TableHeaderNode;
import com.incapture.slate.model.node.description.table.TableNode;
import com.incapture.slate.model.node.description.table.TableRowNode;

public class TypeNodeFactory {

    public static Collection<Node> createTypesNodes(Function function, TypesContainer typesContainer) {
        List<Node> nodes = new LinkedList<>();

        List<Field> fields = new LinkedList<>();
        fields.addAll(function.getParameters());
        fields.add(function.getReturnType());

        for (Field parameter : fields) {
            Optional<TypeDefinition> typeOptional = typesContainer.get(parameter.getPackageName(), parameter.getType());
            if (typeOptional.isPresent()) {
                //add some description
                TypeDefinition typeDefinition = typeOptional.get();
                nodes.add(new HeaderNode(typeDefinition.getName(), 4));
                nodes.add(new TextNode(String.format("*%s*", typeDefinition.getDocumentation())));
                if (typeDefinition.isDeprecated()) {
                    if (typeDefinition.getDeprecatedText() != null) {
                        nodes.add(new AsideNode("This type is deprecated: " + typeDefinition.getDeprecatedText(), AsideNodeType.WARNING));
                    } else {
                        nodes.add(new AsideNode("This type is deprecated.", AsideNodeType.WARNING));
                    }
                }
                nodes.add(new EmptyLineNode());

                //add all the fields
                TableHeaderNode headerNode = new TableHeaderNode("Field", "Type");
                TableNode tableNode = new TableNode(headerNode);
                for (Field field : typeDefinition.getFields()) {
                    String text = field.getType(); //maybe add <a href=''... link here, to link to other types?
                    tableNode.getRows().add(new TableRowNode(field.getName(), text));
                }
                nodes.add(tableNode);
                nodes.add(new EmptyLineNode());
            }
        }
        return nodes;
    }
}
