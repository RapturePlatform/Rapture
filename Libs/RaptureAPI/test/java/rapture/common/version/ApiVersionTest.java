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
package rapture.common.version;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author bardhi
 * @since 7/24/15.
 */
public class ApiVersionTest {

    @Test
    public void testCompareEquals() throws Exception {
        ApiVersion v1 = new ApiVersion(1, 0, 0);
        ApiVersion v2 = new ApiVersion(1, 0, 0);
        assertEquals(0, v1.compareTo(v2));
        assertTrue(v1.equals(v2));
    }
    @Test
    public void testCompareGtMicro() throws Exception {
        ApiVersion v1 = new ApiVersion(1, 0, 1);
        ApiVersion v2 = new ApiVersion(1, 0, 0);
        assertTrue(v1.compareTo(v2) > 0);
    }

    @Test
    public void testCompareGtMinor() throws Exception {
        ApiVersion v1 = new ApiVersion(1, 5, 0);
        ApiVersion v2 = new ApiVersion(1, 0, 0);
        assertTrue(v1.compareTo(v2) > 0);
    }

    @Test
    public void testCompareGtMajor() throws Exception {
        ApiVersion v1 = new ApiVersion(5, 0, 0);
        ApiVersion v2 = new ApiVersion(1, 0, 0);
        assertTrue(v1.compareTo(v2) > 0);
    }

    @Test
    public void testCompareLtMicro() throws Exception {
        ApiVersion v1 = new ApiVersion(1, 0, 0);
        ApiVersion v2 = new ApiVersion(1, 0, 31);
        assertTrue(v1.compareTo(v2) < 0);
    }

    @Test
    public void testCompareLtMinor() throws Exception {
        ApiVersion v1 = new ApiVersion(1, 0, 0);
        ApiVersion v2 = new ApiVersion(1, 1, 0);
        assertTrue(v1.compareTo(v2) < 0);
    }

    @Test
    public void testCompareLtMajor() throws Exception {
        ApiVersion v1 = new ApiVersion(10, 0, 0);
        ApiVersion v2 = new ApiVersion(53, 0, 0);
        assertTrue(v1.compareTo(v2) < 0);
    }
}