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
package rapture.server;

import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtilChecked;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * @author bardhi
 * @since 10/28/14.
 */
public class ArgumentParser {
    private static final Logger log = Logger.getLogger(ArgumentParser.class);

    public static <T> T parsePayload(String json, Class<T> tClass) {
        final int errorCode = HttpURLConnection.HTTP_BAD_REQUEST;

        try {
            return JacksonUtilChecked.objectFromJson(json, tClass);
        } catch (UnrecognizedPropertyException e) {
            List<String> references = getReferences(e);
            if (references.size() > 1) {
                String referencesString = StringUtils.join(references.subList(0, references.size()), "->");
                throw RaptureExceptionFactory.create(errorCode,
                        String.format("Bad value for argument \"%s\". Child field \"%s\" not recognized.",
                                references.get(0),
                                referencesString), e);
            } else {
                throw RaptureExceptionFactory
                        .create(errorCode, String.format("Unrecognized parameter \"%s\" passed to API call", e.getUnrecognizedPropertyName()), e);
            }
        } catch (JsonMappingException e) {
            List<String> references = getReferences(e);
            if (references.size() > 1) {
                String referencesString = StringUtils.join(references.subList(0, references.size()), "->");
                throw RaptureExceptionFactory.create(errorCode,
                        String.format("Bad value for argument \"%s\". The problem is in the child field \"%s\": %s", references.get(0), referencesString,
                                getReadableMessage(e)), e);
            } else if (references.size() > 0) {
                throw RaptureExceptionFactory
                        .create(errorCode, String.format("Bad value for argument \"%s\": %s", references.get(0), getReadableMessage(e)), e);
            } else {
                RaptureException raptureException = RaptureExceptionFactory.create(errorCode, "Error reading arguments: " + e.getMessage(), e);
                log.error(String.format("exception id [%s]; json [%s]", raptureException.getId(), json));
                throw raptureException;
            }
        } catch (JsonParseException e) {
            RaptureException raptureException = RaptureExceptionFactory.create(errorCode, "Bad/incomplete arguments passed in: " + e.getMessage(), e);
            log.error(String.format("exception id [%s]; json [%s]", raptureException.getId(), json));
            throw raptureException;
        } catch (IOException e) {
            RaptureException raptureException = RaptureExceptionFactory.create(errorCode, "Error reading arguments: " + e.getMessage(), e);
            log.error(String.format("exception id [%s]; json [%s]", raptureException.getId(), json));
            throw raptureException;
        }
    }

    /**
     * Having to do this kind of sucks, but we need to: the getMessage() method of JsonMappingException appends some information to the message that is useful,
     * but not very legible and exposes Java internals. We need to have as much of a language-agnostic message as possible here, since different languages will
     * be using the API and we should not expose Java errors. To do this, we need to get the plain error message that happens to be stored in the private field
     * detailMessage of the Throwable class (parent of JsonMappingException)
     *
     * @param e
     * @return
     */
    private static String getReadableMessage(JsonMappingException e) {
        Field detailMessageField = null;
        try {
            detailMessageField = Throwable.class.getDeclaredField("detailMessage");
        } catch (NoSuchFieldException e1) {
            log.error("Error getting readable message from exception: " + ExceptionToString.format(e1));
        }
        Object value = null;
        if (detailMessageField != null) {
            detailMessageField.setAccessible(true);
            try {
                value = detailMessageField.get(e);
            } catch (IllegalAccessException e1) {
                log.error("Error getting readable message from exception: " + ExceptionToString.format(e1));
            }
        }

        if (value != null) {
            return value.toString();
        } else {
            return e.getMessage(); // fallback value
        }
    }

    private static List<String> getReferences(JsonMappingException e) {
        List<JsonMappingException.Reference> references = e.getPath();
        List<String> ret = new LinkedList<>();
        if (references != null) {
            for (JsonMappingException.Reference reference : references) {
                ret.add(reference.getFieldName());
            }
        }
        return ret;
    }
}
