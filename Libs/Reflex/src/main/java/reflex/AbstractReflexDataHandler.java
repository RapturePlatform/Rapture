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
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import rapture.common.BlobContainer;
import rapture.common.RaptureURI;
import rapture.common.SeriesPoint;
import rapture.common.SheetAndMeta;
import rapture.common.api.ScriptingApi;
import rapture.common.impl.jackson.JacksonUtil;
import reflex.node.KernelExecutor;
import reflex.util.MapConverter;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

import com.google.common.net.MediaType;

public abstract class AbstractReflexDataHandler implements IReflexDataHandler {

    private static Logger log = Logger.getLogger(AbstractReflexDataHandler.class);
    protected ScriptingApi api;
    protected ScriptingApi originalApi;

    public static final String CSV = MediaType.CSV_UTF_8.toString().split(";")[0].trim();

    public AbstractReflexDataHandler(ScriptingApi scriptClient) {
        this.api = scriptClient;
        this.originalApi = scriptClient;
    }

    public ReflexValue pullData(RaptureURI uri) {
        switch (uri.getScheme()) {
            case DOCUMENT:
                String strContent = api.getDoc().getDoc(uri.toString());
                return (strContent == null)
                        ? new ReflexNullValue()
                        : new ReflexValue(MapConverter.convertMap(JacksonUtil.getMapFromJson(strContent)));

            case SERIES:
                List<SeriesPoint> series = api.getSeries().getPoints(uri.toString());
                ReflexValue retSeries = new ReflexValue(series);
                return retSeries;

            case SHEET:
                SheetAndMeta sheet = api.getSheet().getSheetAndMeta(uri.toString());
                ReflexValue retSheet = new ReflexValue(sheet.getCells());
                return retSheet;

            case BLOB:
                BlobContainer blobContainer = api.getBlob().getBlob(uri.toString());
                if (blobContainer == null) return null;
                String input = new String(blobContainer.getContent());
                if (input.isEmpty()) return new ReflexNullValue();

                String contentType = blobContainer.getHeaders().get("Content-Type");
                if (contentType != null) {
                    if (contentType.startsWith(CSV)) {
                        List<ReflexValue> linesArray = new ArrayList<ReflexValue>();
                        String[] rows = input.split("\n");
                        for (String row : rows) {
                            List<ReflexValue> lineArray = new ArrayList<ReflexValue>();
                            for (String value : row.split(",")) {
                                lineArray.add(new ReflexValue(value));
                            }
                            linesArray.add(new ReflexValue(lineArray));
                        }
                        return new ReflexValue(linesArray);
                    }
                }
                return new ReflexValue(input);

            default:
                throw new ReflexException(-1, "Pull from " + uri + " not supported yet");
        }

    }

    @Override
    public void pushData(RaptureURI uri, ReflexValue data, MediaType metadata) {
        String uriStr = uri.toString(); // sigh
        switch (uri.getScheme()) {
            case SHEET:
                // SheetApi shap = Kernel.;
                List<ReflexValue> rows = data.asList();
                if (rows == null) throw new ReflexException(-1, "Cannot write " + data.getTypeAsString() + " to " + uri);
                if (!api.getSheet().sheetExists(uriStr)) api.getSheet().createSheet(uriStr);
                int row = 0;
                for (ReflexValue rv : rows) {
                    if (rv != null) {
                        List<ReflexValue> columns = ((ReflexValue) rv).asList();
                        int column = 0;
                        for (ReflexValue col : columns) {
                            api.getSheet().setSheetCell(uriStr, row, column++, col.asString(), 0);
                        }
                    }
                    row++;
                }
                break;

            case SERIES:
                List<ReflexValue> series = data.asList();
                if (series == null) throw new ReflexException(-1, "Cannot write " + data.getTypeAsString() + " to " + uri);

                if (series.size() == 2) {
                    List<ReflexValue> l0 = series.get(0).asList();
                    List<ReflexValue> l1 = series.get(1).asList();
                    if ((l0 == null) || (l1 == null) || (l0.size() != l1.size())) {
                        throw new ReflexException(-1, "Cannot write " + data.getTypeAsString() + " to " + uri + " - illegal format");
                    }
                    for (int i = 0; i < l0.size(); i++) {
                        ReflexValue val = l1.get(1);
                        if (val.isString()) api.getSeries().addStringToSeries(uriStr, l0.get(i).toString(), val.toString());
                        else if (val.isNumber()) api.getSeries().addDoubleToSeries(uriStr, l0.get(i).toString(), val.asDouble());
                    }
                } else {
                    ReflexValue first = series.get(0);
                    if (first.isComplex()) {
                        Object val = first.getValue();
                        if (val instanceof SeriesPoint) {
                            for (ReflexValue rv : series) {
                                SeriesPoint sp = (SeriesPoint) rv.getValue();
                                api.getSeries().addStringToSeries(uriStr, sp.getColumn(), sp.getValue());
                            }
                        } else {
                            throw new ReflexException(-1, "Cannot add " + val.getClass().getSimpleName() + " to series");
                        }
                    } else {
                        List<ReflexValue> firstEntry = first.asList();
                        if (firstEntry == null) {
                            throw new ReflexException(-1, "Expected a list for " + series.get(0));
                        }
                        int lengthExpect = firstEntry.size();
                        if (lengthExpect < 2) {
                            throw new ReflexException(-1, "Expected at least two values " + series.get(0));
                        }
                        for (Object obj : series) {
                            if (!(obj instanceof ReflexValue)) obj = new ReflexValue(obj);
                            ReflexValue rv = (ReflexValue) obj;
                            List<ReflexValue> entry = rv.asList();
                            if (entry == null) {
                                log.error("Ignoring illegal entry " + rv + " in list");
                                continue;
                            }
                            if (entry.size() != lengthExpect) {
                                log.error("Ignoring " + entry + " as it has size " + entry.size() + " - expected " + lengthExpect);
                                continue;
                            }
                            String newUri = uriStr;
                            for (int i = 0; i < lengthExpect - 2; i++) {
                                newUri = newUri + "/" + entry.get(i).toString();
                            }
                            ReflexValue val = entry.get(lengthExpect - 1);
                            if (val.isString()) api.getSeries().addStringToSeries(newUri, entry.get(lengthExpect - 2).toString(), val.toString());
                            else if (val.isNumber()) api.getSeries().addDoubleToSeries(newUri, entry.get(lengthExpect - 2).toString(), val.asDouble());
                        }
                    }
                }
                break;

            case DOCUMENT:
                Map<String, Object> map;

                if (data.isMap()) {
                    map = KernelExecutor.convert(data.asMap());
                } else if (data.isList()) {
                    List<ReflexValue> list = data.asList();
                    map = rvListToMap(list);
                } else throw new ReflexException(-1, "Cannot write " + data.getTypeAsString() + " to " + uri);

                api.getDoc().putDoc(uriStr, JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(map)));
                break;

            case BLOB:
                byte[] bytes = data.asByteArray();
                if (bytes == null) throw new ReflexException(-1, "Cannot write " + data.getTypeAsString() + " to " + uri);
                api.getBlob().putBlob(uriStr, bytes, metadata.toString());
                break;

            default:
                throw new ReflexException(-1, "Cannot write " + data.getTypeAsString() + " to " + uri);
        }
    }

    @SuppressWarnings("unchecked")
    private static void listToMap(Map<String, Object> map, List<ReflexValue> list) {
        int size = list.size();
        if (size == 0) return;
        if (size == 1) {
            map.put("value", list.get(0).getValue());
        } else if (size == 2) {
            map.put(list.get(0).toString(), list.get(1).getValue());
        } else {
            String key = list.get(0).toString();
            Map<String, Object> childMap;
            Object child = map.get(key);
            if (child != null) {
                if (!(child instanceof Map)) {
                    throw new ReflexException(-1, "Internal error: " + child.getClass().getSimpleName() + " is not a Map");
                } else {
                    childMap = (Map<String, Object>) child;
                }
            } else {
                childMap = new HashMap<>();
                map.put(key, childMap);
            }
            listToMap(childMap, list.subList(1, size));
        }
    }

    public static Map<String, Object> rvListToMap(List<ReflexValue> list) {
        Map<String, Object> ret = new HashMap<>();
        for (ReflexValue rvElem : list) {
            if (rvElem.isList()) {
                listToMap(ret, rvElem.asList());
            }
        }
        return ret;
    }

    public static List<ReflexValue> mapToRVList(Map<String, Object> map) {
        List<ReflexValue> list = new ArrayList<>();
        for (Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                List<ReflexValue> sublist = mapToRVList((Map<String, Object>) entry.getValue());
                for (ReflexValue val : sublist) {
                    val.asList().add(0, new ReflexValue(entry.getKey()));
                }
                list.addAll(sublist);
            } else {
                List<ReflexValue> sublist = new ArrayList<>();
                sublist.add(new ReflexValue(entry.getKey()));
                sublist.add(new ReflexValue(entry.getValue()));
                list.add(new ReflexValue(sublist));
            }
        }
        return list;
    }

}
