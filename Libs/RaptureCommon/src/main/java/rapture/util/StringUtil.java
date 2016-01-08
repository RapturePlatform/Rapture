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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Base64;

import rapture.common.exception.RaptureExceptionFactory;

public final class StringUtil {
    public static String[] firstSplit(String displayNamePart) {
        int firstPoint = displayNamePart.indexOf('/');
        String[] ret = new String[2];
        if (firstPoint == -1) {
            ret[0] = displayNamePart;
            ret[1] = null;
        } else {
            ret[0] = displayNamePart.substring(0, firstPoint);
            ret[1] = displayNamePart.substring(firstPoint + 1);
        }
        return ret;
    }

    public static String[] lastSplit(String displayNamePart) {
        int lastPoint = displayNamePart.lastIndexOf('/');
        String[] ret = new String[2];
        if (lastPoint == -1) {
            ret[0] = displayNamePart;
            ret[1] = null;
        } else {
            ret[0] = displayNamePart.substring(0, lastPoint);
            ret[1] = displayNamePart.substring(lastPoint + 1);
        }
        return ret;
    }

    public static List<List<String>> getBatches(List<String> displayNames, int batchSize) {
        List<List<String>> ret = new ArrayList<List<String>>();
        List<String> current = new ArrayList<String>();
        for (String display : displayNames) {
            current.add(display);
            if (current.size() >= batchSize) {
                ret.add(current);
                current = new ArrayList<String>();
            }
        }
        if (!current.isEmpty()) {
            ret.add(current);
        }
        return ret;
    }

    public static Map<String, Object> getMapFromString(String val) {
        Map<String, Object> ret = new HashMap<String, Object>();
        if (val == null || val.isEmpty()) {
            return ret;
        }
        // The string will be x=y,a=b

        String[] parts = val.split(",");
        for (String p : parts) {
            String[] lr = p.split("=");
            if (lr.length == 2) {
                if (!lr[0].isEmpty() && !lr[1].isEmpty()) {
                    ret.put(lr[0], lr[1]);
                }
            }
        }
        return ret;
    }

    public static Map<String, String> getStringMapFromString(String val) {
        Map<String, String> ret = new HashMap<String, String>();
        if (val == null || val.isEmpty()) {
            return ret;
        }
        // The string will be x=y,a=b

        String[] parts = val.split(",");
        for (String p : parts) {
            String[] lr = p.split("=");
            if (lr.length == 2) {
                if (!lr[0].isEmpty() && !lr[1].isEmpty()) {
                    ret.put(lr[0], lr[1]);
                }
            }
        }
        return ret;
    }

    public static List<String> list(String val) {
        return list(val, ",");
    }

    public static List<String> list(String val, String string) {
        List<String> ret = new ArrayList<String>();
        if (val == null || val.isEmpty()) {
            return ret;
        }
        String[] parts = val.split(string);
        for (String p : parts) {
            ret.add(p);
        }
        return ret;
    }

    private StringUtil() {

    }

    /**
     * Compress the passed in string and base64 encode it
     * @param content
     * @return
     */
    public static String base64Compress(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip;
        try {
            gzip = new GZIPOutputStream(out);
            gzip.write(content.getBytes("UTF-8"));
            gzip.close();
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error compressing content", e);
        }
 
        
        // Now with out, base64
       byte[] encoding =  Base64.encodeBase64(out.toByteArray());
       return new String(encoding);
    }
    
    public static String base64Decompress(String content) {
        byte[] decodedBytes = Base64.decodeBase64(content.getBytes());
        ByteArrayInputStream is = new ByteArrayInputStream(decodedBytes);
        try {
            GZIPInputStream gzip = new GZIPInputStream(is);
            BufferedReader bf = new BufferedReader(new InputStreamReader(gzip, "UTF-8"));
            StringBuilder ret = new StringBuilder();
            String line;
            while((line = bf.readLine()) != null) {
                ret.append(line);
                ret.append("\n");
            }
            return ret.toString().substring(0, ret.length()-1);
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error decompressing content", e);
        }
    }
}
