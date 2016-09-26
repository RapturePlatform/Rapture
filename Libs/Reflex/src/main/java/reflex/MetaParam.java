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
package reflex;

import org.apache.log4j.Logger;

import rapture.common.RaptureParameterType;

public class MetaParam {
   
    @Override
    public String toString() {
        return "MetaParam [parameterName=" + parameterName + ", parameterType=" + parameterType + ", description=" + description + "]";
    }

    private String parameterName;
    private RaptureParameterType parameterType;
    private String description;
    private static final Logger log = Logger.getLogger(MetaParam.class);

    public MetaParam(String parameterName, String parameterType, String description) {
        try {
            this.parameterType = RaptureParameterType.valueOf(parameterType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unsupported parameter type " + parameterType);
            this.parameterType = RaptureParameterType.VOID;
        }
        this.parameterName = parameterName;
        this.description = description;
    }
    
    public String getParameterName() {
        return parameterName;
    }
    
    public RaptureParameterType getParameterType() {
        return parameterType;
    }

    public String getDescription() {
        return description;
    }
}
