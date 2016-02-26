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
package reflex.value;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import rapture.common.exception.RaptureExceptionFactory;

/**
 * An instance of this class represents an entry into a file system concept,
 * either a file or a folder
 * 
 * @author amkimian
 * 
 */
public class ReflexFileValue extends ReflexStreamValue {
    private String fileName;
    private File file = null;
    private String encoding = "UTF-8";
    FileInputStream fis = null;

    public ReflexFileValue(String fileName) {
        this.fileName = fileName;
        file = new File(fileName);
    }

    public String getFileName() {
        return fileName;
    }

    public String getNodeName() {
        return file.getName();
    }

    public String getPathName() {
        return file.getAbsolutePath();
    }

    public String toString() {
        return fileName;
    }

    @Override
    public InputStream getInputStream() {
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException e) {
            }
            fis = null;
        }
            
        try {
            fis = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, String.format("File %s not found", fileName), e);
        }
        return fis;
    }

    @Override
    protected void finalize() throws Throwable {
        fis.close();
        fis = null;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getEncoding() {
        return encoding;
    }
}