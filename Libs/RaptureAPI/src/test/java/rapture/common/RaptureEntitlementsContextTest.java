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
package rapture.common;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import rapture.common.model.BasePayload;

public class RaptureEntitlementsContextTest {
    
    class DocPathPayload extends BasePayload {
        public String getDocPath() {
            return DOCPATH;
        }
    }

    class AuthorityPayload extends BasePayload {
        public String getAuthority() {
            return AUTHORITY;
        }
    }

    class FullPathPayload extends BasePayload {
        public String getFullPath() {
            return FULLPATH;
        }
    }

    class NoPayload extends BasePayload {

    }

    private static final String DOCPATH = "D";

    private static final String AUTHORITY = "A";

    private static final String FULLPATH = "F";

    @Test
    public void testDocumentPath() {
        RaptureEntitlementsContext ctx = new RaptureEntitlementsContext(new DocPathPayload());
        assertTrue(ctx.getDocPath().equals(DOCPATH));
        assertTrue(ctx.getAuthority().isEmpty());
        assertTrue(ctx.getFullPath().isEmpty());
    }

    @Test
    public void testAuthority() {
        RaptureEntitlementsContext ctx = new RaptureEntitlementsContext(new AuthorityPayload());
        assertTrue(ctx.getDocPath().isEmpty());
        assertTrue(ctx.getAuthority().equals(AUTHORITY));
        assertTrue(ctx.getFullPath().isEmpty());
    }

    @Test
    public void testLockName() {
        RaptureEntitlementsContext ctx = new RaptureEntitlementsContext(new FullPathPayload());
        assertTrue(ctx.getDocPath().isEmpty());
        assertTrue(ctx.getAuthority().isEmpty());
        assertTrue(ctx.getFullPath().equals(FULLPATH));
    }

    @Test
    public void testNone() {
        RaptureEntitlementsContext ctx = new RaptureEntitlementsContext(new NoPayload());
        assertTrue(ctx.getDocPath().isEmpty());
        assertTrue(ctx.getAuthority().isEmpty());
        assertTrue(ctx.getFullPath().isEmpty());
    }
}
