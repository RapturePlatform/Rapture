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
package rapture.kernel;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;

public class BlobApiTest {

    private CallingContext ctx = ContextFactory.getKernelUser();

    @Before
    public void setup() {
        Kernel.INSTANCE.restart();
        Kernel.initBootstrap();
        if (!Kernel.getBlob().blobRepoExists(ctx, "//blobapitestrepo")) {
            Kernel.getBlob().createBlobRepo(ctx, "//blobapitestrepo", "BLOB {} USING MEMORY {}", "REP {} USING MEMORY {}");
        }
    }

    @Test
    public void testPutBlob() {
        Kernel.getBlob().putBlob(ctx, "//blobapitestrepo/x", "xy".getBytes(), "text/plain");
        Kernel.getBlob().putBlob(ctx, "blob://blobapitestrepo/y", "zz".getBytes(), "text/plain");
        assertNotNull(Kernel.getBlob().getBlob(ctx, "//blobapitestrepo/x"));
        assertNotNull(Kernel.getBlob().getBlob(ctx, "blob://blobapitestrepo/x"));
        assertNotNull(Kernel.getBlob().getBlob(ctx, "//blobapitestrepo/y"));
        assertNotNull(Kernel.getBlob().getBlob(ctx, "blob://blobapitestrepo/y"));
    }
}