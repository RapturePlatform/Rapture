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
package reflex.node;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.RaptureTransferObject;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import reflex.ReflexException;
import reflex.value.ReflexFileValue;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public final class KernelExecutor {
    protected KernelExecutor() {

    }

    private static final Logger log = Logger.getLogger(KernelExecutor.class);

    public static ReflexValue executeFunction(int lineNumber, Object outerApi, String areaName, String fnName, List<ReflexValue> params) {
        String apiName;
        if (!StringUtils.isEmpty(areaName) && areaName.length() > 1) {
            apiName = areaName.substring(0, 1).toUpperCase() + areaName.substring(1);
        } else {
            apiName = "<api name missing>";
        }
        int numPassedParams = params.size();
        try {
            // Find the method get[AreaName], which will return the area
            Method[] methods = outerApi.getClass().getMethods();
            String getApiMethodName = "get" + apiName;

            for (Method m : methods) {
                if (m.getName().equals(getApiMethodName)) {
                    // Call that method to get the api requested
                    Object api = m.invoke(outerApi);
                    // Now find the method with the fnName
                    Method[] innerMethods = api.getClass().getMethods();
                    for (Method im : innerMethods) {
                        if (im.getName().equals(fnName)) {
                            // the api should just have one entry
                            Type[] types = im.getGenericParameterTypes();
                            int numExpectedParams = types.length;
                            if (numExpectedParams == numPassedParams) {
                                List<Object> callParams = new ArrayList<Object>(types.length);
                                // Now coerce the types...
                                for (int i = 0; i < types.length; i++) {
                                    ReflexValue v = params.get(i);
                                    Object x = convertValueToType(v, types[i]);
                                    callParams.add(x);
                                }

                                // Now invoke
                                Object ret = im.invoke(api, callParams.toArray());
                                ReflexValue retVal = new ReflexNullValue(lineNumber);
                                if (ret != null) {
                                    retVal = new ReflexValue(convertObject(ret));
                                }
                                return retVal;
                            }
                        }
                    }
                    throw new ReflexException(lineNumber, String.format("API call not found: %s.%s (taking %s parameters)", apiName, fnName,
                            numPassedParams));
                }
            }
            throw new ReflexException(lineNumber, "API '" + apiName + "' not found!");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                if (cause instanceof OutOfMemoryError) {
                    log.error(ExceptionToString.format(e));
                    throw (OutOfMemoryError) cause;
                } else if (cause instanceof RaptureException) {
                    log.warn(ExceptionToString.format(e));
                    String message = ((RaptureException) cause).getFormattedMessage();
                    throw new ReflexException(lineNumber, message, e);
                } else {
                    String details = null;
                    if (cause.getMessage() != null) {
                        details = ": " + cause.getMessage();
                    }
                    RaptureException re = RaptureExceptionFactory
                            .create(String.format("Error executing api call: %s.%s (takes %s parameters)%s", apiName, fnName, numPassedParams, details), e);

                    String message = re.getFormattedMessage();
                    throw new ReflexException(lineNumber, message, re);
                }
            }
            throw new ReflexException(lineNumber, e.getTargetException().getMessage(), e);
        } catch (ReflexException e) {
            throw e;
        } catch (Exception e) {
            log.error(ExceptionToString.format(e));
            throw new ReflexException(lineNumber, e.getMessage(), e);
        }
    }

    public static Object convert(List<ReflexValue> asList) {
        List<Object> ret = new ArrayList<Object>(asList.size());
        for (Object o : asList) {
            if (o instanceof ReflexValue) {
                ReflexValue e = (ReflexValue) o;
                if (e.isMap()) {
                    ret.add(convert(e.asMap()));
                } else if (e.isList()) {
                    ret.add(convert(e.asList()));
                } else if (e.isComplex()) {
                    ret.add(convertObject(e.asObject()));
                } else {
                    ret.add(e.asObject());
                }
            } else {
                ret.add(o);
            }
        }
        return ret;
    }

    public static Object convertSimpleList(List<Object> asList) {
        List<Object> ret = new ArrayList<Object>(asList.size());
        for (Object o : asList) {
            if (o instanceof ReflexValue) {
                ReflexValue e = (ReflexValue) o;
                if (e.isMap()) {
                    ret.add(convert(e.asMap()));
                } else if (e.isList()) {
                    ret.add(convert(e.asList()));
                } else {
                    ret.add(e.asObject());
                }
            } else {
                ret.add(o);
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> convert(Map<String, Object> asMap) {
        Map<String, Object> converted = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Object> e : asMap.entrySet()) {
            if (e.getValue() instanceof ReflexValue) {
                ReflexValue val = (ReflexValue) e.getValue();
                if (val.isMap()) {
                    converted.put(e.getKey(), convert(val.asMap()));
                } else if (val.isList()) {
                    converted.put(e.getKey(), convert(val.asList()));
                } else if (val.isString()) {
                    converted.put(e.getKey(), val.asString());
                } else {
                    converted.put(e.getKey(), val.asObject());
                }
            } else if (e.getValue() instanceof List) {
                List<Object> vals = (List<Object>) e.getValue();
                converted.put(e.getKey(), convertSimpleList(vals));
            } else if (e.getValue() instanceof Map) {
                Map<String, Object> vals = (Map<String, Object>) e.getValue();
                converted.put(e.getKey(), convert(vals));
            } else {
                converted.put(e.getKey(), e.getValue());
            }
        }
        return converted;
    }

    public static String convertValueToJson(ReflexValue value, boolean prettyFy) {
        Object toJson = value.asObject();
        if (value.isMap()) {
            toJson = convert(value.asMap());
        } else if (value.isList()) {
            toJson = convert(value.asList());
        } else if (value.isStruct()) {
            toJson = convert(value.asStruct().asMap());
        }
        String ret = JacksonUtil.jsonFromObject(toJson);
        if (prettyFy) {
            ret = JacksonUtil.prettyfy(ret);
        }
        return ret;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object convertValueToType(ReflexValue v, Type type) {

        if (type.equals(byte[].class)) {
            if (v.isFile()) {
                // Load file to bytes and return that
                return getContentFromFile(v.asFile());
            } else if (v.isByteArray()) {
                return v.asByteArray();
            } else if (v.isNull()) {
                return "".getBytes();
            } else {
                return v.toString().getBytes();
            }
        } else if (type.equals(String.class)) {
            if (v.isFile()) {
                return new String(getContentFromFile(v.asFile()));
            } else if (v.isNull()) {
                return null;
            } else {
                return v.toString();
            }
        } else if (type.equals(Double.class)) {
            return v.asDouble();
        } else if (type.equals(Integer.class)) {
            return v.asInt();
        } else if (type.equals(Long.class)) {
            return v.asLong();
        } else if (type.equals(Boolean.class)) {
            return v.asBoolean();
        } else if (type.equals(Map.class)) {
            return v.asMap();
        } else if (type instanceof ParameterizedType) {
            return handleParameterizedType(v, type);
        } else if (v.isMap()) {
            return handleMap(v, type);
        } else if (type.equals(List.class)) {
            if (v.isList()) {
                return handleList(v);
            }
        } else if (type instanceof Class && ((Class<?>) type).isEnum()) {
            return Enum.valueOf((Class<Enum>) type, v.asString());
        }
        return v.asObject();
    }

    private static byte[] getContentFromFile(ReflexFileValue asFile) {
        try {
            return IOUtils.toByteArray(asFile.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Object handleList(ReflexValue v) {
        List<Object> ret = new ArrayList<Object>();
        List<ReflexValue> vals = v.asList();
        for (ReflexValue vi : vals) {
            if (vi.isList()) {
                ret.add(handleList(vi));
            } else if (v.isMap()) {
                ret.add(handleMap(vi, Map.class));
            } else if (v.isComplex()) {
                ret.add(convertObject(v.asObject()));
            } else {
                ret.add(vi.asObject());
            }
        }
        return ret;
    }

    public static ReflexValue reconstructFromObject(Object x) throws ClassNotFoundException {
        if (x instanceof ReflexValue) {
            return (ReflexValue) x;
        }
        if (x instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> theMap = convert((Map<String, Object>) x);
            if (theMap.containsKey("CLASS")) {
                String typeName = theMap.get("CLASS").toString();
                theMap.remove("CLASS");
                String json = JacksonUtil.jsonFromObject(theMap);
                Object realObject = JacksonUtil.objectFromJson(json, Class.forName(typeName));
                return new ReflexValue(realObject);
            } else {
                return new ReflexValue(theMap);
            }
        } else if (x instanceof List) {
            List<?> r = (List<?>) x;
            List<ReflexValue> ret2 = new ArrayList<ReflexValue>(r.size());
            for (Object inner : r) {
                ret2.add(inner == null ? new ReflexNullValue() : reconstructFromObject(inner));
            }
            return new ReflexValue(ret2);
        } else {
            return new ReflexValue(x);
        }
    }

    private static Object handleMap(ReflexValue v, Type type) {
        // If we have a map, we may be able to convert this into the Type by
        // using a JacksonUtil conversion
        // But first we need to walk the map to convert the values to native
        // types
        Map<String, Object> convertedMap = convert(v.asMap());
        if (convertedMap.containsKey("CLASS")) {
            convertedMap.remove("CLASS");
        }
        String json = JacksonUtil.jsonFromObject(convertedMap);
        Object x = JacksonUtil.objectFromJson(json, (Class<?>) type);
        return x;
    }

    private static Object handleParameterizedType(ReflexValue v, Type type) {
        ParameterizedType pType = (ParameterizedType) type;
        if (pType.getRawType().equals(List.class)) {
            Type innerType = pType.getActualTypeArguments()[0];
            if (innerType.equals(String.class)) {
                List<ReflexValue> inner = v.asList();
                List<String> ret = new ArrayList<String>(inner.size());
                for (ReflexValue vi : inner) {
                    ret.add(vi.asString());
                }
                return ret;
            } else if (innerType.getClass().isInstance(ParameterizedType.class)) {
                List<ReflexValue> inner = v.asList();
                List<List<?>> ret = new ArrayList<List<?>>(inner.size());
                for (ReflexValue vi : inner) {
                    ret.add((List<?>) handleList(vi));
                }
                return ret;
            } else {
                // TODO: Do the above, f knows how to test this "List of Lists bull"
                List<ReflexValue> inner = v.asList();
                List<List<?>> ret = new ArrayList<List<?>>(inner.size());
                for (ReflexValue vi : inner) {
                    ret.add((List<?>) handleList(vi));
                }
                return ret;
            }
        }
        return v.asObject();
    }

    static Object convertObject(Object ret) {
        if (ret instanceof Object[]) {
            Object[] r = (Object[]) ret;
            List<ReflexValue> ret2 = new ArrayList<ReflexValue>(r.length);
            for (Object x : r) {
                ret2.add(x == null ? new ReflexNullValue() : new ReflexValue(x));
            }
            return ret2;
        } else if (ret instanceof List) {
            List<?> r = (List<?>) ret;
            List<ReflexValue> ret2 = new ArrayList<ReflexValue>(r.size());
            for (Object x : r) {
                ret2.add(x == null ? new ReflexNullValue() : new ReflexValue(convertObject(x)));
            }
            return ret2;
        } else if (ret instanceof RaptureTransferObject) {
            // RaptureTransferObject is a hint to convert this object from an
            // object
            // to a json doc to a map
            String json = JacksonUtil.jsonFromObject(ret);
            Object retMap = JacksonUtil.getMapFromJson(json);
            @SuppressWarnings("unchecked")
            Map<String, Object> mapAsRet = (Map<String, Object>) retMap;
            mapAsRet.put("CLASS", ret.getClass().getName());
            return retMap;
        }
        return ret;
    }
}
