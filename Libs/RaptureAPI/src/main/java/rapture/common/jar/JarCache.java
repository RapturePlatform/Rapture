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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import rapture.common.BlobContainer;
import rapture.common.api.ScriptingApi;

/**
 * Cache all the jar entries by jarUri --> map of (className to className bytes). Classloaders require access to individual classnames so have them ready for
 * speedier lookup
 * 
 * @author dukenguyen
 *
 */
public enum JarCache {
    INSTANCE;

    private static final Logger log = Logger.getLogger(JarCache.class);

    private Cache<String, Map<String, byte[]>> cache = CacheBuilder.newBuilder().maximumSize(10000).build();

    public static JarCache getInstance() {
        return INSTANCE;
    }

    public Map<String, byte[]> get(ScriptingApi api, String jarUri) throws ExecutionException {
        return cache.get(jarUri, new Callable<Map<String, byte[]>>() {
            @Override
            public Map<String, byte[]> call() throws Exception {
                log.info(String.format("Cache miss, loading classes for jar [%s]", jarUri));
                BlobContainer bc = api.getJar().getJar(jarUri);
                if (bc == null) {
                    throw new ExecutionException(String.format("No jar found at uri [%s]", jarUri), null);
                }
                try {
                    return JarUtils.getClassNamesAndBytesFromJar(bc.getContent());
                } catch (IOException e) {
                    throw new ExecutionException("IOException extracting classes and bytes from jar", e);
                }
            }
        });
    }

    public byte[] getClassBytes(ScriptingApi api, String jarUri, String className) throws ExecutionException {
        return get(api, jarUri).get(className);
    }

    public List<String> getClassNames(ScriptingApi api, String jarUri) throws ExecutionException {
        return new ArrayList<>(get(api, jarUri).keySet());
    }

    public void put(String jarUri, Map<String, byte[]> classNamesToBytes) {
        cache.put(jarUri, classNamesToBytes);
    }

    public void invalidate(String jarUri) {
        cache.invalidate(jarUri);
    }
}