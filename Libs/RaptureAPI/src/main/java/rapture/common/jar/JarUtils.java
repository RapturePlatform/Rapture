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
package rapture.common.jar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.ScriptingApi;

/**
 * General purpose jar-related methods go in here
 * 
 * @author dukenguyen
 *
 */
public class JarUtils {

    public static Map<String, byte[]> getClassNamesAndBytesFromJar(byte[] jarBytes) throws IOException {
        Map<String, byte[]> ret = new LinkedHashMap<>();
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                // load the class name
                String name = entry.getName();
                if (!name.endsWith("/")) {
                    // load the class bytes
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    int numRead;
                    byte[] data = new byte[4096];
                    while ((numRead = jis.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, numRead);
                    }
                    ret.put(convertJarPathToFullyQualifiedClassName(name), buffer.toByteArray());
                }
            }
        }
        return ret;
    }

    public static String convertJarPathToFullyQualifiedClassName(String name) {
        int index = name.indexOf(".class");
        if (index != -1) {
            name = name.substring(0, index);
        }
        return name.replace("/", ".");
    }

    public static List<String> expandWildcardUri(ScriptingApi api, String jarUriStr) {
        String jarUri = new RaptureURI(jarUriStr, Scheme.JAR).toString();
        if (!jarUri.endsWith("*")) {
            return Arrays.asList(jarUri);
        }
        List<String> ret = new ArrayList<>();
        String prefix = jarUri.substring(0, jarUri.indexOf("*"));
        Map<String, RaptureFolderInfo> jars = api.getJar().listJarsByUriPrefix(prefix, 1);
        for (Map.Entry<String, RaptureFolderInfo> entry : jars.entrySet()) {
            if (entry.getValue().isFolder()) {
                continue;
            }
            ret.add(prefix + entry.getValue().getName());
        }
        Collections.sort(ret);
        return ret;
    }

}
