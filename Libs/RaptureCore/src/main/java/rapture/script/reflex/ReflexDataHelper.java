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
package rapture.script.reflex;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rapture.common.CallingContext;
import rapture.common.api.ScriptingApi;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentWithMeta;
import rapture.common.uri.URIParser;
import rapture.kernel.script.KernelScript;
import rapture.util.TypeUtil;
import reflex.IReflexDataHandler;

/**
 * An instance of this class is installed into the Reflex environment when
 * running a script. It is locked to a RaptureCallingContext so that is passed
 * to the api function to use it.
 * 
 * @author amkimian
 * 
 */
public class ReflexDataHelper implements IReflexDataHandler {

    public ReflexDataHelper(CallingContext ctx) {
        KernelScript sc = new KernelScript();
        sc.setCallingContext(ctx);
        this.api = sc;
    }

    @Override
    public List<String> batchPushData(List<String> displayNames, List<Map<String, Object>> datas) {
        if (displayNames.size() != datas.size()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Batch push data and display different sizes. Data is " + datas.size()
                    + ", displays are " + displayNames.size());
        }
        List<List<String>> displayBatches = TypeUtil.getBatches(displayNames, 50);
        List<List<Map<String, Object>>> dataBatches = TypeUtil.getBatches(datas, 50);
        List<String> ret = new ArrayList<String>(displayBatches.size());
        for (int i = 0; i < displayBatches.size(); i++) {
            List<String> batchDisplayNames = displayBatches.get(i);
            List<Map<String, Object>> dataBatch = dataBatches.get(i);
            List<String> parsedDisplayNames = new ArrayList<String>(batchDisplayNames.size());
            List<String> contents = new ArrayList<String>(batchDisplayNames.size());
            int point = 0;
            for (String disp : batchDisplayNames) {
                disp = URIParser.convertDocURI(disp);
                parsedDisplayNames.add(disp);
                contents.add(JacksonUtil.jsonFromObject(dataBatch.get(point)));
                point++;
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
        List<List<String>> batches = getBatches(displayNames, 50);
        for (List<String> batch : batches) {
            List<String> batchDisplayNames = new ArrayList<String>();
            for (String disp : batch) {
                disp = URIParser.convertDocURI(disp);
                batchDisplayNames.add(disp);
            }
            Map<String, String> contents = api.getDoc().getDocs(batchDisplayNames);
            content.addAll(contents.values());
        }
        List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
        for (String c : content) {
            ret.add((Map<String, Object>) JacksonUtil.getMapFromJson(c));
        }
        return ret;
    }

    private List<List<String>> getBatches(List<String> displayNames, int batchSize) {
        List<List<String>> ret = new ArrayList<List<String>>();
        List<String> current = new ArrayList<String>();
        for (String display : displayNames) {
            current.add(display);
            if (current.size() >= batchSize) {
                ret.add(current);
                current.clear();
            }
        }
        if (!current.isEmpty()) {
            ret.add(current);
        }
        return ret;
    }

    @Override
    public Map<String, Object> pullData(String raptureURI) {
        raptureURI = URIParser.convertDocURI(raptureURI);
        String content = api.getDoc().getDoc(raptureURI);
        if (content != null) {
            return (Map<String, Object>) JacksonUtil.getMapFromJson(content);
        } else {
            return null;
        }
    }

    @Override
    public String pushData(String raptureURI, Map<String, Object> data) {
        raptureURI = URIParser.convertDocURI(raptureURI);
        String content = JacksonUtil.jsonFromObject(data);
        return api.getDoc().putDoc(raptureURI, content);
    }

    @Override
    public void remove(String raptureURI) {
        raptureURI = URIParser.convertDocURI(raptureURI);
        api.getDoc().deleteDoc(raptureURI);
    }

    @Override
    public Map<String, Object> metaPullData(String raptureURI) {
        raptureURI = URIParser.convertDocURI(raptureURI);
        DocumentWithMeta content = api.getDoc().getDocAndMeta(raptureURI);
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
    public String rawPushData(String raptureURI, String content) {
        raptureURI = URIParser.convertDocURI(raptureURI);
        String ret = api.getDoc().putDoc(raptureURI, content);
        return ret;

    }

    @Override
    public List<String> batchPushRawData(List<String> displayNames, List<String> datas) {
        if (displayNames.size() != datas.size()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Batch push raw data and display different sizes. Data is " + datas.size()
                    + ", displays are " + displayNames.size());
        }
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
            api.getDoc().putDocs( parsedDisplayNames, dataBatch);
            ret.addAll(parsedDisplayNames);
        }
        return ret;
    }

    @Override
    public boolean hasCapability() {
        return true;
    }

    private ScriptingApi api;
    private ScriptingApi originalApi;
    
    @Override
    public void switchApi(ScriptingApi api) {
       this.api = api;
    }

    @Override
    public void resetApi() {
       this.api = originalApi;
    }
}
