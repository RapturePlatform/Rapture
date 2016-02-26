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
package com.incapture.slate.model.node.description.table;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

import com.incapture.slate.model.node.Node;
import com.incapture.slate.template.TemplateRepo;

/**
 * @author bardhi
 * @since 6/2/15.
 */
public class TableNode implements Node {
    private final TableHeaderNode tableHeaderNode;
    private List<TableRowNode> rows;

    public TableNode(TableHeaderNode tableHeaderNode) {
        this.tableHeaderNode = tableHeaderNode;
        rows = new LinkedList<>();
    }

    public List<TableRowNode> getRows() {
        return rows;
    }

    @Override
    public String getContent() {
        String template = TemplateRepo.INSTANCE.getTemplate("tableNode");
        CompiledTemplate compiled = TemplateCompiler.compileTemplate(template);
        Map<String, Object> vars = new HashMap<>();
        vars.put("header", tableHeaderNode);
        vars.put("separator", new TableSeparatorNode(tableHeaderNode));
        vars.put("rows", rows);
        return TemplateRuntime.execute(compiled, vars).toString();

    }
}
