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
package rapture.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rapture.common.api.ScriptingApi;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentWithMeta;
import rapture.common.uri.URIParser;
import rapture.util.DisplayNameParts;
import rapture.util.StringUtil;
import rapture.util.TypeUtil;
import reflex.IReflexDataHandler;

public class ReflexDataHandler implements IReflexDataHandler {
    private ScriptingApi api;

    public ReflexDataHandler(ScriptingApi scriptClient) {
        this.api = scriptClient;
    }

    @Override
    public List<String> batchPushData(List<String> displayNames, List<Map<String, Object>> datas) {
        List<List<String>> displayBatches = TypeUtil.getBatches(displayNames, 50);
        List<List<Map<String, Object>>> dataBatches = TypeUtil.getBatches(datas, 50);
        List<String> ret = new ArrayList<String>();
        for (int i = 0; i < displayBatches.size(); i++) {
            List<String> batchDisplayNames = displayBatches.get(i);
            List<Map<String, Object>> dataBatch = dataBatches.get(i);
            List<String> parsedDisplayNames = new ArrayList<String>();
            List<String> contents = new ArrayList<String>();
            for (String disp : batchDisplayNames) {
                disp = URIParser.convertDocURI(disp);
                parsedDisplayNames.add(disp);
                contents.add(JacksonUtil.jsonFromObject(dataBatch.get(i)));
            }
            api.getDoc().putDocs(parsedDisplayNames, contents);
            ret.addAll(parsedDisplayNames);
        }
        return ret;
    }

    @Override
    public List<Map<String, Object>> batchPullData(List<String> displayNames) {
        // Run in batches of 50
        List<String> content = new ArrayList<String>();
        List<List<String>> batches = StringUtil.getBatches(displayNames, 50);
        for (List<String> batch : batches) {
            List<String> batchDisplayNames = new ArrayList<String>();
            for (String disp : batch) {
                DisplayNameParts p = new DisplayNameParts(disp);
                batchDisplayNames.add(p.getDisplay());
            }
            Map<String, String> contents = api.getDoc().getDocs(batchDisplayNames);
            content.addAll(contents.values());
        }
        List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
        for (String c : content) {
            if (c == null) {
                ret.add(null);
            } else {
                ret.add((Map<String, Object>) JacksonUtil.getMapFromJson(c));
            }
        }
        return ret;
    }

    @Override
    public Map<String, Object> pullData(String fullName) {
        String content = api.getDoc().getDoc(fullName);
        if (content == null) {
            return null;
        } else {
            return (Map<String, Object>) JacksonUtil.getMapFromJson(content);
        }
    }

    @Override
    public String pushData(String fullName, Map<String, Object> data) {
        String content = JacksonUtil.jsonFromObject(data);
        String ret = api.getDoc().putDoc(fullName, content);
        return ret;
    }

    @Override
    public void remove(String displayName) {
        api.getDoc().deleteDoc(displayName);
    }

    @Override
    public Map<String, Object> metaPullData(String fullName) {
        DocumentWithMeta content = api.getDoc().getDocAndMeta(fullName);
        if (content == null) {
            return null;
        } else {
            Map<String, Object> ret = new HashMap<String, Object>();
            ret.put("meta", JacksonUtil.getHashFromObject(content.getMetaData()));
            ret.put("content", (Map<String, Object>) JacksonUtil.getMapFromJson(content.getContent()));
            return ret;
        }
    }

    @Override
    public String rawPushData(String displayName, String content) {
        String ret = api.getDoc().putDoc(displayName, content);
        return ret;

    }

    @Override
    public List<String> batchPushRawData(List<String> displayNames, List<String> datas) {
        List<List<String>> displayBatches = TypeUtil.getBatches(displayNames, 50);
        List<List<String>> dataBatches = TypeUtil.getBatches(datas, 50);
        List<String> ret = new ArrayList<String>();
        for (int i = 0; i < displayBatches.size(); i++) {
            List<String> batchDisplayNames = displayBatches.get(i);
            List<String> dataBatch = dataBatches.get(i);
            List<String> parsedDisplayNames = new ArrayList<String>();
            for (String disp : batchDisplayNames) {
                disp = URIParser.convertDocURI(disp);
                parsedDisplayNames.add(disp);
            }
            api.getDoc().putDocs(parsedDisplayNames, dataBatch);
            ret.addAll(parsedDisplayNames);
        }
        return ret;
    }

    @Override
    public boolean hasCapability() {
        return true;
    }

    @Override
    public void switchApi(ScriptingApi api) {
        this.api = api;
    }

    private ScriptingApi originalApi;
    @Override
    public void resetApi() {
        
        this.api = originalApi;
    }

}
