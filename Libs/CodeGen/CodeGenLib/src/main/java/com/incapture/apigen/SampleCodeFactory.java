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
package com.incapture.apigen;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.incapture.apigen.slate.function.Parameter;
import com.incapture.rapgen.parser.SampleCodeParser;

/**
 * @author bardhi
 * @since 6/3/15.
 */
public enum SampleCodeFactory {
    INSTANCE;

    private SampleCodeParser parser;

    public void setParser(SampleCodeParser parser) {
        this.parser = parser;
    }

    public String sampleCodeForJava(String apiName, com.incapture.apigen.slate.function.Function function) {
        return getCode(apiName, function, "/sample/code/java.mvel", SampleCodeParser.SourceType.java);
    }

    public String sampleCodeForPython(String apiName, com.incapture.apigen.slate.function.Function function) {
        return getCode(apiName, function, "/sample/code/python.mvel", SampleCodeParser.SourceType.py);

    }

    private static final Logger log = Logger.getLogger(SampleCodeFactory.class);

    private String getCode(String apiName, com.incapture.apigen.slate.function.Function function, String name, SampleCodeParser.SourceType sourceType) {
        String sampleCode = null;
        String apiLowerCase = apiName.toLowerCase();
        if (parser != null) {
            sampleCode = parser.getSampleCode(sourceType, apiLowerCase, function.getName());
        }

        if (sampleCode == null) {
            return createForTemplate(apiName, function, name);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("sample code found for %s, %s", apiName, function.getName()));
            }
            return sampleCode;
        }
    }

    private String createForTemplate(String apiName, com.incapture.apigen.slate.function.Function function, String templatePath) {
        String template = getResource(templatePath);
        CompiledTemplate compiled = TemplateCompiler.compileTemplate(template);
        Map<String, Object> vars = new HashMap<>();
        vars.put("apiUpper", upperFirst(apiName));
        vars.put("apiLower", apiName.toLowerCase());
        String functionName = function.getName();
        vars.put("functionUpper", upperFirst(functionName));
        vars.put("functionName", functionName);
        vars.put("retType", function.getReturnType());
        List<String> paramNames = Lists.newArrayList(Iterables.transform(function.getParameters(), new Function<Parameter, String>() {
            @Override
            public String apply(Parameter input) {
                return input.getName();
            }
        }));
        vars.put("parameters", StringUtils.join(paramNames, ", "));
        return StringUtils.trim(TemplateRuntime.execute(compiled, vars).toString());
    }

    private static String upperFirst(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    private static String getResource(String name) {
        try (InputStream stream = SampleCodeFactory.class.getResourceAsStream(name)) {
            if (stream == null) {
                throw new IllegalArgumentException(String.format("resource name not found %s", name));
            }
            return IOUtils.toString(stream);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error while reading resource %s", name), e);
        }

    }
}
