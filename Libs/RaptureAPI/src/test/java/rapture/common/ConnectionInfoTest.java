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

import org.junit.Test;

import static org.junit.Assert.*;

public class ConnectionInfoTest {

    @Test
    public void testEqual() {
        ConnectionInfo info1 = createInfo();
        assertEquals(info1, createInfo());

        ConnectionInfo info2 = new ConnectionInfo();
        assertNotEquals(info1, info2);

        info2.setHost(info1.getHost());
        assertNotEquals(info1, info2);

        info2.setPort(info1.getPort());
        assertNotEquals(info1, info2);

        info2.setDbName(info1.getDbName());
        assertEquals(info1, info2);

        info2.setDbName("newdb");
        assertNotEquals(info1, info2);
    }

    private ConnectionInfo createInfo() {
        ConnectionInfo info = new ConnectionInfo();
        info.setHost("localhost");
        info.setUsername("rapture");
        info.setPassword("rapture");
        info.setDbName("RaptureDB");
        info.setInstanceName("default");
        return info;
    }

}