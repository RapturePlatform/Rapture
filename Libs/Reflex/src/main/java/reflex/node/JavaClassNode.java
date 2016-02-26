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
package reflex.node;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import org.apache.log4j.Logger;

import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureExceptionFactory;
import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;

/**
 * Reflex node that instantiates a Java object. Note that we may need to have
 * safeguards here - is it wise to allow people to arbitrarily create Objects
 * with no regard to type safety? This is a useful feature, but there need to be
 * controls
 * 
 * @author David Tong <dave.tong@incapturetechnologies.com>
 * 
 */
public class JavaClassNode extends BaseNode {

    private ReflexNode node;
    private static final Logger log = Logger.getLogger(JavaClassNode.class);

    public JavaClassNode(int lineNumber, IReflexHandler handler, Scope scope, ReflexNode node) {
        super(lineNumber, handler, scope);
        this.node = node;
    }

    /**
     * Find the Class. If the class name is not fully qualified look in the
     * class path for a matching name.
     * 
     * @param className
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static Class<?> findClass(String className) {
        // If class name is fully qualified this is easy
        Class<?> retClass = null;
        try {
            retClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            // That's OK, we'll try again
        }
        if (retClass == null) {
            // OK now we need to look for it.
            ClassLoader c = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> manifests;
            try {
                manifests = c.getResources(JarFile.MANIFEST_NAME);
                while (manifests.hasMoreElements()) {
                    URL url = manifests.nextElement();
                    String jarName = url.toString().replace("!/META-INF/MANIFEST.MF", "");
                    // System.out.println("Jar " + jarName);
                    JarInputStream jarFile = new JarInputStream(new FileInputStream(jarName.substring(jarName.lastIndexOf(':') + 1)));
                    JarEntry jarEntry;
                    while (true) {
                        jarEntry = jarFile.getNextJarEntry();
                        if (jarEntry == null) {
                            break;
                        }

                        String cName = jarEntry.getName().replaceAll("/", "\\.");
                        if (cName.contains("." + className + ".class")) {
                            cName = cName.substring(0, cName.length() - 6);
                            System.out.println("Jar " + jarName + " contains " + cName);
                            jarFile.close();
                            retClass = Class.forName(cName);
                            break;
                        }
                    }
                    jarFile.close();
                }
            } catch (FileNotFoundException e) {
                log.debug(ExceptionToString.format(e));
                throw RaptureExceptionFactory.create("Cannot find class "+className, e);
            } catch (ClassNotFoundException e) {
                log.debug(ExceptionToString.format(e));
                throw RaptureExceptionFactory.create("Cannot find class "+className, e);
            } catch (IOException e) {
                log.debug(ExceptionToString.format(e));
                throw RaptureExceptionFactory.create("Cannot find class "+className, e);
            }
        }
        return retClass;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue rv = node.evaluate(debugger, scope);
        String className = rv.toString();
        Class<?> retClass = null;
        Object instance = null;
        /**
         * How do we want to implement whitelisting/blacklisting?
         */
        try {
            retClass = findClass(className);
            if (retClass != null) {
                instance = retClass.newInstance();
                /**
                Method[] methods = instance.getClass().getMethods();
                for (Method m : methods) {
                    System.out.println(instance.getClass().getName() + " " + m.getName());
                }
                **/
            }
        } catch (InstantiationException e) {
            log.debug(ExceptionToString.format(e));
            throw RaptureExceptionFactory.create("Cannot create instance of class "+className, e);
        } catch (IllegalAccessException e) {
            log.debug(ExceptionToString.format(e));
            throw RaptureExceptionFactory.create("Cannot create instance of class "+className, e);
        } catch (SecurityException e) {
            log.debug(ExceptionToString.format(e));
            throw RaptureExceptionFactory.create("Cannot create instance of class "+className, e);
        }
        rv = new ReflexValue(instance);
        debugger.stepEnd(this, rv, scope);
        return rv;
    }
}
