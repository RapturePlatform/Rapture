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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.api.ScriptingApi;

/**
 * Class loader for workflows that uses Rapture's internal Jar system to add dependencies. Dependencies are listed in the workflow definition at either the step
 * or workflow level. Step-level dependencies get priority over workflow-level dependencies. This is an abstract class. The concrete implementations are in
 * ParentFirstClassLoader and ChildFirstClassLoader which differ in terms of where jars are loaded first.
 * 
 * @author dukenguyen
 *
 */
public abstract class AbstractClassLoader extends URLClassLoader {

    private static final Logger log = Logger.getLogger(AbstractClassLoader.class);

    protected ScriptingApi api;
    protected Map<String, String> classNameMap = new HashMap<>();

    public AbstractClassLoader(ClassLoader parent, ScriptingApi api, List<String> jarUris) throws ExecutionException {
        super(new URL[0], parent);
        this.api = api;
        if (CollectionUtils.isNotEmpty(jarUris)) {
            // reverse the list, so that jars given first take precedence since the map will overwrite duplicate names last
            Collections.reverse(jarUris);
            for (String jarUri : jarUris) {
                List<String> expandedUris = JarUtils.expandWildcardUri(api, jarUri);
                for (String expandedUri : expandedUris) {
                    List<String> classNames = JarCache.getInstance().getClassNames(api, expandedUri);
                    for (String className : classNames) {
                        classNameMap.put(className, expandedUri);
                    }
                }
            }
        }
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        log.debug("findClass() for className: " + className);
        String jarUri = classNameMap.get(className);
        if (StringUtils.isBlank(jarUri)) {
            log.debug("Not found, delegating to parent: " + className);
            return super.findClass(className);
        }
        try {
            byte[] classBytes = JarCache.getInstance().getClassBytes(api, jarUri, className);
            return defineClass(className, classBytes, 0, classBytes.length);
        } catch (ExecutionException e) {
            throw new ClassNotFoundException("Could not get class bytes from the cache", e);
        }
    }
}