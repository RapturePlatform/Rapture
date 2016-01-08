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
package rapture.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class MimeTypeResolver {
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(MimeTypeResolver.class.getName());
    private Map<String, String> mimeTypes;

    public MimeTypeResolver() {
        mimeTypes = new HashMap<String, String>();
        processResource("/mimeTypes.txt");
    }

    public String getExtensionFromPath(String path) {
        int indexPoint = path.lastIndexOf('.');
        String extension = path;
        if (indexPoint != -1) {
            extension = path.substring(indexPoint+1);
        }
        return extension;
    }
    
    public String getMimeTypeFromPath(String path) {
        return getMimeType(getExtensionFromPath(path));
    }
    
    private void processResource(String path) {
        InputStream is = MimeTypeResolver.class.getResourceAsStream(path);
        if (is != null) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split("\\s+");
                    for(int i=1; i< parts.length; i++) {
                        mimeTypes.put(parts[i], parts[0]);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
        }
    }

    public String getMimeType(String extension) {
        if (mimeTypes.containsKey(extension)) {
            return mimeTypes.get(extension);
        } else {
            return "application/unknown";
        }
    }
}
