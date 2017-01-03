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
package rapture.common.impl.jackson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Charsets;

import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureExceptionFactory;

/**
 * Copied from standard jackson utility classes
 * <p>
 * and work http://www.mkyong.com/java/how-to-enable-pretty-print-json-output-jackson/
 *
 * This class wraps any exceptions thrown by Jackson into RaptureException (unchecked) and throws that. If you need checked exceptions, use JacksonUtilChecked
 *
 * @author alan
 */
@SuppressWarnings("deprecation")
public final class JacksonUtil {
    private static final Logger log = Logger.getLogger(JacksonUtil.class);

    public static final ObjectMapper DEFAULT_MAPPER = MapperFactory.createDefault().enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
    private static final ObjectWriter PRETTY_PRINTER = DEFAULT_MAPPER.writerWithDefaultPrettyPrinter();

    public static Map<String, Object> getHashFromObject(Object obj) {
        return getMapFromJson(jsonFromObject(obj));
    }

    public static Map<String, Object> getMapFromJson(String json) {
        try {
            JsonFactory factory = new JsonFactory();
            TypeReference<LinkedHashMap<String, Object>> typeRef = new TypeReference<LinkedHashMap<String, Object>>() {
            };
            return DEFAULT_MAPPER.<LinkedHashMap<String, Object>>readValue(factory.createJsonParser(json), typeRef);
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error parsing json content " + json, e);
        }
    }

    public static Map<String, Object> getMapFromJson(InputStream json) {
        try {
            JsonFactory factory = new JsonFactory();
            TypeReference<LinkedHashMap<String, Object>> typeRef = new TypeReference<LinkedHashMap<String, Object>>() {
            };
            return DEFAULT_MAPPER.<LinkedHashMap<String, Object>>readValue(factory.createJsonParser(json), typeRef);
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error parsing json content " + json, e);
        }
    }

    public static String jsonFromObject(Object object) {
        StringWriter writer = new StringWriter();
        try {
            DEFAULT_MAPPER.writeValue(writer, object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return writer.toString();
    }

    public static byte[] bytesJsonFromObject(Object object) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            DEFAULT_MAPPER.writeValue(os, object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return os.toByteArray();
    }

    public static <T> T objectFromJson(JsonNode json, Class<T> klass) {
        T object;
        if (klass == null) {
            throw RaptureExceptionFactory.create("Null class given to objectFromJson");
        }
        try {
            object = DEFAULT_MAPPER.convertValue(json, klass);
        } catch (RuntimeException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error making " + klass.getName() + " object from json " + json, e);
        }
        return object;
    }

    public static <T> T objectFromJson(String json, Class<T> klass) {
        T object;
        try {
            object = DEFAULT_MAPPER.readValue(json, klass);
        } catch (IOException | RuntimeException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error making " + klass.getName() + " object from json " + json, e);
        }
        return object;
    }

    public static <T> T objectFromJson(String json, TypeReference<T> tr) {
        T object;
        try {
            object = DEFAULT_MAPPER.readValue(json, tr);
        } catch (IOException | RuntimeException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error parsing json content from TR " + json, e);
        }
        return object;
    }

    public static String prettyfy(String json) {
        JsonNode node = treeFromJson(json);
        try {
            return PRETTY_PRINTER.writeValueAsString(node);
        } catch (Exception e) {
            log.error("error printing json: " + ExceptionToString.format(e));
        }
        return json;
    }

    public static JsonNode treeFromJson(String json) {
        try {
            return DEFAULT_MAPPER.readTree(json);
        } catch (IOException | RuntimeException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error parsing json content " + json, e);
        }
    }

    public static <T> T objectFromJson(byte[] jsonBytes, Class<T> klass) {
        return objectFromJson(new String(jsonBytes, Charsets.UTF_8), klass);
    }

    private JacksonUtil() {

    }
}
