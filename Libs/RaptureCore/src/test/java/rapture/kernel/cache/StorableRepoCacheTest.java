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
package rapture.kernel.cache;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import rapture.common.CallingContext;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.repo.Repository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;

/**
 * @author bardhi
 * @since 4/23/15.
 */
public class StorableRepoCacheTest {
    private static final CallingContext CONTEXT = ContextFactory.getKernelUser();

    private static final String URI = "//someRepo";

    @Before
    public void setUp() throws Exception {
        Kernel.initBootstrap();
        Kernel.getDoc().createDocRepo(CONTEXT, URI, "NREP {} USING MEMORY {}");
        Kernel.getIndex().createIndex(CONTEXT, URI, "field1($0) string, field2(test) string");
    }

    @After
    public void tearDown() throws Exception {
        Kernel.getDoc().deleteDocRepo(CONTEXT, URI);
        Kernel.getIndex().deleteIndex(CONTEXT, URI);
    }

    @Test
    public void testWithIndex() throws Exception {
        Repository repo = Kernel.INSTANCE.getRepo(URI);
        assertTrue(repo.hasIndexProducer());

    }

    @Test
    public void testNoIndex() throws Exception {
        Optional<Repository> repo = Kernel.INSTANCE.getStorableRepo(URI, null);
        assertFalse(repo.get().hasIndexProducer());

    }
}