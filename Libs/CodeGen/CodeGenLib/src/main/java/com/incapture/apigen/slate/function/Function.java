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
package com.incapture.apigen.slate.function;

import java.util.List;

public class Function {
    private Field returnType;
    private String name;
    private List<Parameter> parameters;
    private String documentation;
    private String entitlement;
    private boolean isDeprecated;
    private String deprecatedText;
    private String versionSince;

    public void setReturnType(String type, String documentation) {
        this.returnType = new Field();
        returnType.setName("retVal");
        returnType.setType(type);
        returnType.setDocumentation(documentation);
    }

    public void setName(String name) {

        this.name = name;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public void setEntitlement(String entitlement) {
        this.entitlement = entitlement;
    }

    public void setIsDeprecated(boolean isDeprecated) {
        this.isDeprecated = isDeprecated;
    }

    public void setDeprecatedText(String deprecatedText) {
        this.deprecatedText = deprecatedText;
    }

    @Override
    public String toString() {
        return "Function{" +
                "returnType='" + returnType + '\'' +
                ", name='" + name + '\'' +
                ", parameters=" + parameters +
                ", documentation='" + documentation + '\'' +
                ", entitlement='" + entitlement + '\'' +
                ", isDeprecated=" + isDeprecated +
                ", deprecatedText='" + deprecatedText + '\'' +
                '}';
    }

    public Field getReturnType() {
        return returnType;
    }

    public String getName() {
        return name;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public String getVersionSince() {
        return versionSince;
    }

    public String getDocumentation() {
        return documentation;
    }

    public String getEntitlement() {
        return entitlement;
    }

    public boolean isDeprecated() {
        return isDeprecated;
    }

    public String getDeprecatedText() {
        return deprecatedText;
    }

    public void setVersionSince(String versionSince) {
        this.versionSince = versionSince;
    }
}
