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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.jar.JarUtils;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.script.KernelScript;

public class JarUtilsTest {

    @Test
    public void testGetClassNamesAndBytesFromJar() throws IOException {
        byte[] jarBytes = IOUtils.toByteArray(this.getClass().getResourceAsStream("/workflowTestJars/xmlunit-1.6.jar"));
        Map<String, byte[]> ret = JarUtils.getClassNamesAndBytesFromJar(jarBytes);
        assertFalse(ret.isEmpty());
        assertTrue(ret.containsKey("org.custommonkey.xmlunit.XMLUnit"));
        assertTrue(ret.get("org.custommonkey.xmlunit.XMLUnit").length > 0);
    }

    @Test
    public void testConvertJarPathToFullyQualifiedClassName() {
        assertEquals("com.x.y.z.Test", JarUtils.convertJarPathToFullyQualifiedClassName("com/x/y/z/Test.class"));
        assertEquals("com.x.y.z.Test", JarUtils.convertJarPathToFullyQualifiedClassName("com/x/y/z/Test"));
        assertEquals("com.x.y.z.Test", JarUtils.convertJarPathToFullyQualifiedClassName("com/x/y/z.Test.class"));
    }

    @Test
    public void testExpandWildcardUriWithScheme() {
        testExpandWildcardUri("jar://someJars/");
    }

    @Test
    public void testExpandWildcardUriWithoutScheme() {
        testExpandWildcardUri("//jarswithoutscheme/xsdf/df/");
    }

    private void testExpandWildcardUri(String uriPrefix) {
        Kernel.getKernel().restart();
        Kernel.initBootstrap();
        int num = 9;
        for (int i = 0; i < num; i++) {
            Kernel.getJar().putJar(ContextFactory.getKernelUser(), uriPrefix + i, new byte[0]);
        }
        KernelScript ks = new KernelScript();
        ks.setCallingContext(ContextFactory.getKernelUser());
        List<String> ret = JarUtils.expandWildcardUri(ks, uriPrefix + "*");
        assertFalse(ret.isEmpty());
        for (int i = 0; i < num; i++) {
            assertEquals(new RaptureURI(uriPrefix + i, Scheme.JAR).toString(), ret.get(i));
        }
    }
}
