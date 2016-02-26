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
package rapture.common.apigen;

import rapture.common.exception.ExceptionToString;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * @author bardhi
 * @since 4/22/15.
 */
public abstract class ListToClassReader<T> {
    private static final Logger log = Logger.getLogger(ListToClassReader.class);

    private final String resourceName;

    protected ListToClassReader(String resourceName) {
        this.resourceName = resourceName;
    }

    public List<Class<? extends T>> readAll() {

        Enumeration<URL> resourcesEnum = null;
        List<Class<? extends T>> all = new LinkedList<>();
        try {
            resourcesEnum = getClass().getClassLoader().getResources(resourceName);
        } catch (IOException e) {
            log.error(ExceptionToString.format(e));
        }

        List<URL> resourcesList = new LinkedList<>();
        if (resourcesEnum != null) {
            while (resourcesEnum.hasMoreElements()) {
                URL url = resourcesEnum.nextElement();
                resourcesList.add(url);
            }
        }

        log.info(String.format("Found %s files that contain the purge list: %s", resourcesList.size(), resourcesList));
        Set<String> nameSet = new HashSet<>();
        for (URL url : resourcesList) {
            nameSet.addAll(getStringList(url));
        }
        for (String className : nameSet) {
            Class<? extends T> theClass = nameToClass(className);
            if (theClass != null) {
                all.add(theClass);
            }
        }
        return all;
    }

    private List<String> getStringList(URL url) {
        InputStream input = null;
        try {
            input = url.openStream();
        } catch (IOException e) {
            log.error(ExceptionToString.format(e));
        }
        List<String> nameList = new LinkedList<>();
        if (input != null) {
            try {
                List<String> contents = IOUtils.readLines(input);
                nameList.addAll(contents);
            } catch (IOException e) {
                log.error(ExceptionToString.format(e));
            } finally {
                try {
                    input.close();
                } catch (IOException e) {
                    log.error(ExceptionToString.format(e));
                }
            }
        }
        return nameList;
    }

    private Class<? extends T> nameToClass(String className) {
        log.trace("Class is " + className);
        try {
            return (Class<? extends T>) Class.forName(className);
        } catch (Exception e) {
            log.error(ExceptionToString.format(e));
            return null;
        }
    }

}
