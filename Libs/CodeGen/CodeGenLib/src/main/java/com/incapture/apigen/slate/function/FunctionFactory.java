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
package com.incapture.apigen.slate.function;

import java.util.List;

import org.apache.log4j.Logger;

import com.incapture.rapgen.docString.DocumentationParser;

public class FunctionFactory {
    private static final Logger log = Logger.getLogger(FunctionFactory.class);

    public static Function createFunction(String apiName, String returnType, String functionName, List<Parameter> parameters, String documentation,
            String entitlement, boolean isDeprecated, String deprecatedText) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("createFunction: apiName=[%s], returnType=[%s], functionName=[%s], parameters=[%s], documentation=[%s], entitlement=[%s]",
                    apiName, returnType, functionName, parameters, documentation, entitlement));
        }

        Function function = new Function();
        String returnTypeDocumentation;
        if (documentation != null) {
            returnTypeDocumentation = DocumentationParser.retrieveReturn(documentation);
        } else {
            returnTypeDocumentation = "";
        }
        function.setReturnType(returnType, returnTypeDocumentation);
        function.setName(functionName);
        function.setParameters(parameters);
        function.setDocumentation(documentation);
        function.setEntitlement(entitlement);
        function.setIsDeprecated(isDeprecated);
        function.setDeprecatedText(deprecatedText);
        return function;
    }

}
