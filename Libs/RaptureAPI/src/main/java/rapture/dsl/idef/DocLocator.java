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
package rapture.dsl.idef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rapture.common.model.DocumentMetadata;

public class DocLocator extends FieldLocator {
    private List<String> fields = new ArrayList<String>();

    public void addField(String field) {
        fields.add(field);
    }

    @Override
    public String toString() {
        StringBuffer ret = new StringBuffer();
        for (String f : fields) {
            ret.append(f);
            ret.append(".");
        }
        return ret.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object value(String key, Map<String, Object> mappedContent, DocumentMetadata meta) {
        Map<String, Object> current = mappedContent;
        Object val = null;
        boolean allInList = false;
        for (String field : fields) {
            if (allInList) {
                // everything in list assumes the next field is the end and
                // doesn't go deeper
                List<Object> vals = new ArrayList<Object>();
                for (int i = 0; i < current.size(); i++) {
                    Map<String, Object> listObj = (Map<String, Object>) current.get(String.valueOf(i));
                    vals.add(listObj.get(field));
                }
                return vals;
            }
            if (field.equals("*")) {
                // star field means they want everything in a list, so skip to
                // next field
                allInList = true;
                continue;
            }
            val = current.get(field);
            if (val instanceof Map<?, ?>) {
                current = (Map<String, Object>) val;
            } else if (val instanceof List<?>) {
                current = listToMap((List<?>) val);
            }
        }
        return val;
    }

    /**
     * Support for lists, so we create a map where they key is the index of the
     * object in the list, and the value is the object itself
     * 
     * @param l
     *            - the list to convert
     * @return - a map with key being list index and value being object
     */
    private Map<String, Object> listToMap(List<?> l) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (int i = 0; i < l.size(); i++) {
            map.put(String.valueOf(i), l.get(i));
        }
        return map;
    }

}
