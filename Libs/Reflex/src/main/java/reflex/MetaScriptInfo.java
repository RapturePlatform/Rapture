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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetaScriptInfo {
    @Override
    public String toString() {
        return "MetaScriptInfo [returnInfo=" + returnInfo + ", params=" + params + ", properties=" + properties + "]";
    }

    private MetaReturn returnInfo = null;
    private List<MetaParam> params = new ArrayList<MetaParam>();
    private Map<String, String> properties = new HashMap<String, String>();
    
    
    public void setReturn(String retType, String meta) {
        returnInfo = new MetaReturn(retType, meta);
    }
    
    public void addParameter(String parameterName, String parameterType, String description) {
        params.add(new MetaParam(parameterName, parameterType, description));
    }
    
    public MetaReturn getReturnInfo() {
        return returnInfo;
    }
    
    public List<MetaParam> getParameters() {
        return params;
    }
    
    public void setProperty(String name, String value) {
        properties.put(name, value);
    }
    
    public String getProperty(String name) {
        return properties.get(name);
    }

	public Map<String, String> getProperties() {
		return properties;
	}

    public MetaParam getParameter(String paramName) {
        if (paramName != null) {
            for (MetaParam param : params) {
                if (paramName.equals(param.getParameterName())) return param;
            }
        }
        return null;
    }
}
