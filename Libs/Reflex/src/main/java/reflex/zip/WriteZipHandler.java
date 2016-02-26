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
package reflex.zip;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.exception.RaptureExceptionFormatter;
import rapture.common.impl.jackson.JacksonUtil;
import reflex.value.ReflexValue;

public class WriteZipHandler extends ZipHandler {
    private FileOutputStream dest;
    private ZipOutputStream out;
    private int recordNumber;
    
    private final static Logger log = Logger.getLogger(WriteZipHandler.class);

    public WriteZipHandler(String name) {
        super(name);
        try {
            recordNumber = 0;
            dest = new FileOutputStream(getName());
            out = new ZipOutputStream(new BufferedOutputStream(dest));
        } catch (FileNotFoundException e) {
            RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error creating zip file");
            log.error(RaptureExceptionFormatter.getExceptionMessage(raptException, e));
            throw raptException;
        }
    }

    public void writeArchiveEntry(ReflexValue value) {
        try {
            String content = "";
            String entryName = "";
            if (value.isMap()) {
                content = JacksonUtil.jsonFromObject(value.asMap());
                entryName = "" + recordNumber + ".txt";
            } else if (value.isList()) {
                List<ReflexValue> list = value.asList();
                if (list.size() == 2) {
                    entryName = list.get(0).asString();
                    content = JacksonUtil.jsonFromObject(list.get(1).asMap());
                } else {
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Need just two entries - name and data - for push");
                }
            }

            ZipEntry entry = new ZipEntry(entryName);
            recordNumber++;
            out.putNextEntry(entry);

            out.write(content.getBytes("UTF-8"));

        } catch (IOException e) {
            RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error writing data");
            log.error(RaptureExceptionFormatter.getExceptionMessage(raptException, e));
            throw raptException;
        }

    }

    @Override
    public void close() {
        try {
            out.close();
        } catch (IOException e) {
            log.error("Error closing file:" + ExceptionToString.format(e));
        }
    }

}
