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
package reflex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import rapture.common.exception.RaptureExceptionFactory;
import reflex.value.ReflexArchiveFileValue;
import reflex.value.ReflexFileValue;
import reflex.value.ReflexStreamValue;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;
import reflex.value.internal.ReflexVoidValue;
import reflex.zip.ReadZipHandler;
import reflex.zip.WriteZipHandler;

import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;

public class DefaultReflexIOHandler implements IReflexIOHandler {

	public static String getStreamAsString(InputStream is) throws FileNotFoundException {
		return getStreamAsString(is, "UTF-8");
	}
	
    public static String getStreamAsString(InputStream is, String encoding) throws FileNotFoundException {
        String ret = null;
        InputStreamReader isr = null;
        try {
        	isr = new InputStreamReader(is, encoding);
        	ret = CharStreams.toString(isr);
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error with Reflex stream", e);
        } finally {
            try {
            	if (isr != null) {
            	   isr.close();
            	}
                is.close();
            } catch (IOException e) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error closing stream", e);
            }
        }
        return ret;
    }

    @Override
    public ReflexValue forEachLine(ReflexStreamValue fileValue, final IReflexLineCallback iReflexLineCallback) {
        InputStream is = null;
        InputStreamReader isr = null;
        final ReflexValue ret = new ReflexVoidValue();
        try {
            is = fileValue.getInputStream();
            isr = new InputStreamReader(is, fileValue.getEncoding());
            CharStreams.readLines(isr, new LineProcessor<String>() {

				@Override
				public boolean processLine(String line) throws IOException {
	                ReflexValue v = iReflexLineCallback.callback(line);
	                if (v.getValue() == ReflexValue.Internal.BREAK) {                  
	                    return false;
	                }
	                return true;
				}

				@Override
				public String getResult() {
					return null;
				}
            	
            });
            
        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        } finally {
            if (is != null) {
                try {
                    is.close();
                    isr.close();
                } catch (Exception e) {

                }
            }
        }
        return ret;
    }

    @Override
    public ReflexValue getContent(ReflexStreamValue fileValue) {
        try {
            String content = getStreamAsString(fileValue.getInputStream(), fileValue.getEncoding());
            return new ReflexValue(content);
        } catch (FileNotFoundException e) {
            return new ReflexNullValue();
        }
    }

    /**
     * Return a boolean value indicating whether this file is a file
     * 
     * @return
     */
    @Override
    public ReflexValue isFile(ReflexFileValue fileValue) {
        if (fileValue == null) {
            return new ReflexValue(Boolean.FALSE);
        }
        File f = new File(fileValue.getFileName());
        return new ReflexValue(f.isFile());
    }

    /**
     * Return a boolean value indicating whether this file is a folder
     * 
     * @return
     */
    @Override
    public ReflexValue isFolder(ReflexFileValue fileValue) {
        File f = new File(fileValue.getFileName());
        return new ReflexValue(f.isDirectory());
    }

    @Override
    public ReflexValue readdir(ReflexFileValue fileValue) {
        File f = new File(fileValue.getFileName());
        String[] res = f.list();
        if (res == null) {
            return new ReflexNullValue();
        }
        List<ReflexValue> ret = new ArrayList<ReflexValue>();
        for (String file : res) {
            String fullName = fileValue.getFileName() + "/" + file;
            ret.add(new ReflexValue(new ReflexFileValue(fullName)));
        }
        return new ReflexValue(ret);
    }

    @Override
    public void mkdir(ReflexFileValue asFile) {
        File f = new File(asFile.getFileName());
        if (!f.mkdirs()) {
            throw new ReflexException(-1, "Could not create directories in " + asFile.getFileName());
        }
    }
    
    @Override
    public void writeFile(ReflexFileValue asFile, String data) {
        // Given the fileName, write the contents to the file
        try {
            FileUtils.writeStringToFile(new File(asFile.getFileName()), data);
        } catch (IOException e) {
            throw new ReflexException(-1, "Could not write to file " + asFile.getFileName(), e);
        }
    }

    // These three methods assume the passed file is a ZipArchive

    private Map<String, ReadZipHandler> readZipHandler = new HashMap<String, ReadZipHandler>();
    private Map<String, WriteZipHandler> writeZipHandler = new HashMap<String, WriteZipHandler>();

    @Override
    public ReflexValue getArchiveEntry(ReflexArchiveFileValue fVal) {
        String name = fVal.getFileName();
        if (!readZipHandler.containsKey(name)) {
            ReadZipHandler rzh = new ReadZipHandler(name);
            readZipHandler.put(name, rzh);
        }
        return readZipHandler.get(name).getArchiveEntry();
    }

    @Override
    public void writeArchiveEntry(ReflexArchiveFileValue fVal, ReflexValue value) {
        String name = fVal.getFileName();
        if (!writeZipHandler.containsKey(name)) {
            WriteZipHandler rzh = new WriteZipHandler(name);
            writeZipHandler.put(name, rzh);
        }
        writeZipHandler.get(name).writeArchiveEntry(value);
    }

    @Override
    public void close(ReflexArchiveFileValue fVal) {
        String name = fVal.getFileName();
        if (readZipHandler.containsKey(name)) {
            readZipHandler.get(name).close();
            readZipHandler.remove(name);
        }
        if (writeZipHandler.containsKey(name)) {
            writeZipHandler.get(name).close();
            writeZipHandler.remove(name);
        }
    }

    @Override
    public void remove(ReflexFileValue toDelete) {
        File f = new File(toDelete.getFileName());
        f.delete();
    }

    @Override
    public boolean hasCapability() {
        return true;
    }

 

}
