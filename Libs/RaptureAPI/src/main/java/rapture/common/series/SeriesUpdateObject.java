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
package rapture.common.series;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used to update the search repository for new series updates
 * 
 * @author dukenguyen
 *
 */
public class SeriesUpdateObject {

    private String uri;
    private List<String> keys = new ArrayList<>();
    private List<? extends Object> values = new ArrayList<>();

    public SeriesUpdateObject() {
    }

    public SeriesUpdateObject(String uri) {
        this.uri = uri;
    }

    public SeriesUpdateObject(String uri, List<String> keys, List<? extends Object> values) {
        this.uri = uri;
        this.keys = keys;
        this.values = values;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public List<String> getKeys() {
        return keys;
    }

    public void setKeys(List<String> keys) {
        this.keys = keys;
    }

    public List<? extends Object> getValues() {
        return values;
    }

    public void setValues(List<? extends Object> values) {
        this.values = values;
    }

    public Map<String, Object> asMap() {
        Map<String, Object> ret = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            ret.put(keys.get(i), values.get(i));
        }
        return ret;
    }
}
