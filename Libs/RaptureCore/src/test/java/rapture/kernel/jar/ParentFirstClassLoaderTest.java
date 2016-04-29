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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

public class ParentFirstClassLoaderTest extends AbstractClassLoaderTest {

    @Test
    public void testClassLoader() throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException, ExecutionException {
        ParentFirstClassLoader cl1 = new ParentFirstClassLoader(this.getClass().getClassLoader(), ctx, Arrays.asList(JAR_URI1, JAR_URI2));
        ParentFirstClassLoader cl2 = new ParentFirstClassLoader(this.getClass().getClassLoader(), ctx, Arrays.asList(JAR_URI1, JAR_URI2));
        testClassLoader(cl1, cl2);
    }
}