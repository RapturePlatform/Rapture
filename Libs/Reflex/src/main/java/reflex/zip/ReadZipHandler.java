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
package reflex.zip;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class ReadZipHandler extends ZipHandler {

    private static final int BUFFER = 2048;
    ZipFile zipfile;
    Enumeration<ZipEntry> enumer;

    @SuppressWarnings("unchecked")
    public ReadZipHandler(String name) {
        super(name);
        try {
            zipfile = new ZipFile(name);
        } catch (IOException e1) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error reading zip file", e1);
        }
        enumer = (Enumeration<ZipEntry>) zipfile.entries();
    }

    public ReflexValue getArchiveEntry() {
        if (!enumer.hasMoreElements()) {
            return new ReflexNullValue();
        }
        ZipEntry entry = enumer.nextElement();
        BufferedInputStream is = null;
        ByteArrayOutputStream bos = null;
        try {
            is = new BufferedInputStream(zipfile.getInputStream(entry));
            bos = new ByteArrayOutputStream();

            int count;
            byte data[] = new byte[BUFFER];
            while ((count = is.read(data, 0, BUFFER)) != -1) {
                bos.write(data, 0, count);
            }
            bos.close();
            String value = bos.toString();
            if (value.isEmpty()) {
                return new ReflexNullValue();
            }
            Map<String, Object> outerMap = new HashMap<String, Object>();
            outerMap.put("data", new ReflexValue(JacksonUtil.getMapFromJson(value)));
            outerMap.put("name", new ReflexValue(entry.getName()));
            return new ReflexValue(outerMap);
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error reading zip file", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {

                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {

                }
            }
        }
    }

    @Override
    public void close() {
        try {
            zipfile.close();
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error closing zip file stream", e);
        }
    }

}
