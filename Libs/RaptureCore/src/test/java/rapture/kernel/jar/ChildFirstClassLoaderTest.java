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
package rapture.kernel.jar;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import rapture.common.exception.ExceptionToString;
import rapture.common.jar.ChildFirstClassLoader;
import rapture.kernel.ContextFactory;
import rapture.kernel.script.KernelScript;

public class ChildFirstClassLoaderTest extends AbstractClassLoaderTest {

    @Test
    public void testClassLoader() throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException, ExecutionException {
        KernelScript ks = new KernelScript();
        ks.setCallingContext(ContextFactory.getKernelUser());
        ChildFirstClassLoader cl1 = new ChildFirstClassLoader(this.getClass().getClassLoader(), ks, Arrays.asList(JAR_URI1, JAR_URI2));
        ChildFirstClassLoader cl2 = new ChildFirstClassLoader(this.getClass().getClassLoader(), ks, Arrays.asList(JAR_URI1, JAR_URI2));
        testClassLoader(cl1, cl2);
    }

    @SuppressWarnings({ "resource", "rawtypes" })
    @Test
    public void testGetResourceAsStream() {
        KernelScript ks = new KernelScript();
        ks.setCallingContext(ContextFactory.getKernelUser());

        try {
            ChildFirstClassLoader cl1 = new ChildFirstClassLoader(this.getClass().getClassLoader(), ks, Arrays.asList(JAR_URI1, JAR_URI2, JAR_URI3));
            Class class1 = cl1.loadClass("biz.c24.api.License");
            assertNotNull(class1);
            cl1.close();

            Object a1 = class1.getConstructor(boolean.class).newInstance(true);
            assertNotNull(a1);

            InputStream is = a1.getClass().getResourceAsStream("IO.version");
            assertNotNull(is);
            Properties properties = new Properties();
            properties.load(is);
            is.close();
            assertNotNull(properties.get("build.version"));

        } catch (ClassNotFoundException | ExecutionException | IOException | InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            fail(ExceptionToString.summary(e));
        }
    }

}
