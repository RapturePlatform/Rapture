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
package reflex.cache;

import org.junit.Assert;
import org.junit.Test;

public class CacheElementTest {
    private final String TEST_CONTENT1 = "TEST_CONTENT1";

    @Test
    public void testCreateCacheElement() {
        CacheElement<String> ce = new CacheElement<String>(TEST_CONTENT1, 1000);
        Assert.assertNotNull(ce);
    }

    @Test
    public void testGetContentPass() {
        CacheElement<String> ce = new CacheElement<String>(TEST_CONTENT1, 1000);
        Assert.assertTrue(ce.getContent().compareTo(TEST_CONTENT1) == 0);
    }

    @Test
    public void testGetContentFail() {
        CacheElement<String> ce = new CacheElement<String>(TEST_CONTENT1, 1000);
        String TEST_CONTENT2 = "TEST_CONTENT2";
        Assert.assertFalse(ce.getContent().compareTo(TEST_CONTENT2) == 0);
    }

    @Test
    public void testHasNotExpired() throws InterruptedException {
        CacheElement<String> ce = new CacheElement<String>(TEST_CONTENT1, 1000);
        Thread.sleep(500);
        Assert.assertTrue(!ce.hasExpired());
    }

    @Test
    public void testHasExpired() throws InterruptedException {
        CacheElement<String> ce = new CacheElement<String>(TEST_CONTENT1, 1000);
        Thread.sleep(2000);
        Assert.assertTrue(ce.hasExpired());
    }

}
