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
package rapture.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import rapture.common.exception.RaptureExceptionFactory;

public class ResourceLoader {

    public static String getResourceAsString(Object context, String path) {
        InputStream is = (context == null ? ResourceLoader.class : context.getClass()).getResourceAsStream(path);
        return getResourceFromInputStream(is);
    }

    public static String getResourceFromInputStream(InputStream is) {
        if (is != null) {
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } catch (IOException e) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error reading resource stream", e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error closing stream", e);
                }
            }
            return writer.toString();
        } else {
            return "";
        }

    }

    public static List<String> getScripts(Object context, String resourcePath) {

        if (resourcePath == null || resourcePath.equals("")) {
            return null;
        }

        URL resourceURL = (context == null ? ResourceLoader.class : context.getClass()).getResource(resourcePath);

        if (resourceURL == null) {
            return null;
        }
        List<String> retVal = new ArrayList<String>();

        if (resourceURL.getProtocol().equals("jar")) {
            loadFromJar(resourcePath, resourceURL, retVal);
        } else {
            loadFromURL(resourceURL, retVal);
        }

        // return the whole list
        return retVal;
    }

    private static void loadFromURL(URL resourceURL, List<String> retVal) {
        InputStreamReader isr = null;
        BufferedReader reader = null;

        try {
            isr = new InputStreamReader(resourceURL.openStream());
            reader = new BufferedReader(isr);

            while (reader.ready()) {
                String val = reader.readLine();
                retVal.add(val);
            }
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error reading resource from URI", e);
        } finally {
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static void loadFromJar(String resourcePath, URL resourceURL, List<String> retVal) {
        int bangLoc = resourceURL.getPath().lastIndexOf('!');
        JarFile jf;
        String uriString = resourceURL.getPath().substring(0, bangLoc);
        try {
            URI uri = new URI(uriString);
            jf = new JarFile(new File(uri));
        } catch (URISyntaxException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error reading URI", e);
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error reading from URI", e);
        }

        try {
            Enumeration<JarEntry> jarEntries = jf.entries();
            if (resourcePath.startsWith("/")) {
                resourcePath = resourcePath.substring(1);
            }
            while (jarEntries.hasMoreElements()) {
                JarEntry entry = jarEntries.nextElement();
                if (entry.getName().matches(resourcePath + ".+")) {
                    String name = entry.getName().replaceFirst(resourcePath, "");
                    retVal.add(name);
                }
            }
        } finally {
            try {
                jf.close();
            } catch (IOException e) {
                // ignore exceptions from read-only close
            }
        }
    }
}
