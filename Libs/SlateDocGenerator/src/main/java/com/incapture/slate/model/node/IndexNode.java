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
import com.incapture.slate.template.TemplateRepo;

/**
 * @author bardhi
 * @since 6/2/15.
 */
public class IndexNode implements FileNode {
    private String title;

    private boolean isSearchable;

    private List<LanguageNode> languages = new LinkedList<>();
    private List<Node> tocFooters = new LinkedList<>();
    private List<String> sectionsNames = new LinkedList<>();

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public List<LanguageNode> getLanguages() {
        return languages;
    }

    public List<Node> getTocFooters() {
        return tocFooters;
    }

    public void setSearchable(boolean searchable) {
        this.isSearchable = searchable;
    }

    public List<String> getSectionsNames() {
        return sectionsNames;
    }

    @Override
    public String getContent() {
        String template = TemplateRepo.INSTANCE.getTemplate("indexNode");
        CompiledTemplate compiled = TemplateCompiler.compileTemplate(template);
        Map<String, Object> vars = new HashMap<>();
        vars.put("title", getTitle());
        vars.put("languages", languages);
        vars.put("tocFooters", tocFooters);
        vars.put("includeFiles", createIncludeFilesList());
        vars.put("isSearchable", isSearchable);
        return StringUtils.trim(TemplateRuntime.execute(compiled, vars).toString());
    }

    private List<String> createIncludeFilesList() {
        List<String> list = new LinkedList<>();
        for (String sectionName : sectionsNames) {
            list.add(sectionName);
        }
        return list;
    }

    @Override
    public Optional<String> getDirName() {
        return Optional.absent();
    }

    @Override
    public String getFilename() {
        return "index.md";
    }

    @Override
    public String getTrimmedTitle() {
        return "index";
    }
}
