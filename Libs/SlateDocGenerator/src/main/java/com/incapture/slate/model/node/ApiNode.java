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
package com.incapture.slate.model.node;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

import com.google.common.base.Optional;
import com.incapture.slate.model.FunctionNode;
import com.incapture.slate.template.TemplateRepo;

/**
 * @author bardhi
 * @since 6/2/15.
 */
public class ApiNode extends FunctionNode implements FileNode {

    public ApiNode(String functionName) {
        super(functionName);
    }

    public String getTrimmedTitle() {
        return StringUtils.deleteWhitespace(getTitle()).toLowerCase();
    }

    private List<FunctionNode> functions = new LinkedList<>();

    public List<FunctionNode> getFunctions() {
        return functions;
    }

    @Override
    public String getContent() {
        String template = TemplateRepo.INSTANCE.getTemplate("apiNode");
        CompiledTemplate compiled = TemplateCompiler.compileTemplate(template);
        Map<String, Object> vars = new HashMap<>();
        vars.put("title", getTitle());
        vars.put("descriptionNode", getDescriptionNode());
        vars.put("codeNode", getCodeWrapperNode());
        vars.put("functionNodes", functions);
        return StringUtils.trim(TemplateRuntime.execute(compiled, vars).toString());
    }

    @Override
    public Optional<String> getDirName() {
        return Optional.of("includes");
    }

    public String getFilename() {
        return "_" + getTrimmedTitle() + ".md";
    }

}
