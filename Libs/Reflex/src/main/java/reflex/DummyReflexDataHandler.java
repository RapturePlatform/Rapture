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

import rapture.common.RaptureURI;
import rapture.common.api.ScriptingApi;
import reflex.value.ReflexValue;

import com.google.common.net.MediaType;

public class DummyReflexDataHandler implements IReflexDataHandler {

    @Override
    public List<Map<String, Object>> batchPullData(List<String> displayNames) {
        return null;
    }

    @Override
    public Map<String, Object> pullData(String displayName) {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("_id", displayName);
        return ret;
    }
    
    @Override
    public ReflexValue pullData(RaptureURI displayName) {
        return new ReflexValue(pullData(displayName.toString()));
    }

    @Override
    public void pushData(RaptureURI displayName, ReflexValue data, MediaType type) {
    }

    @Override
    public String pushData(String displayName, Map<String, Object> data) {
        return displayName;
    }

    @Override
    public void remove(String displayName) {
    }

    @Override
    public Map<String, Object> metaPullData(String asString) {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("meta", new HashMap<String, Object>());
        ret.put("content", new HashMap<String, Object>());
        return ret;
    }

    @Override
    public List<String> batchPushData(List<String> displayNames, List<Map<String, Object>> datas) {
        List<String> ret = new ArrayList<String>();
        for (int i = 0; i < displayNames.size(); i++) {
            ret.add(pushData(displayNames.get(i), datas.get(i)));
        }
        return ret;
    }

    @Override
    public List<String> batchPushRawData(List<String> displayNames, List<String> datas) {
        List<String> ret = new ArrayList<String>();
        for (int i = 0; i < displayNames.size(); i++) {
            ret.add(rawPushData(displayNames.get(i), datas.get(i)));
        }
        return ret;
    }

    @Override
    public String rawPushData(String displayName, String content) {
        return displayName;
    }

    @Override
    public boolean hasCapability() {
        return false;
    }

    @Override
    public void switchApi(ScriptingApi api) {
    }

    @Override
    public void resetApi() {
    }

}
