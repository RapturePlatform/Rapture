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
package rapture.jmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;

public class JmxAppCacheTest {

    @Before
    public void setup() {
        if (!JmxServer.getInstance().isStarted()) {
            JmxServer.getInstance().start("jmxServerTest");
        }
    }

    @Test
    public void testCache() throws InterruptedException, ExecutionException {
        Map<String, JmxApp> apps = JmxAppCache.getInstance().get();
        JmxApp localApp = JmxServer.getInstance().getJmxApp();
        assertNotNull(localApp);
        JmxApp fetchedApp = apps.get(localApp.toString());
        assertNotNull(fetchedApp);
        assertEquals(localApp.getName(), fetchedApp.getName());
        assertEquals(localApp.getPort(), fetchedApp.getPort());
        assertEquals(localApp.getHost(), fetchedApp.getHost());
        assertEquals(localApp.getUrl(), fetchedApp.getUrl());
    }

}
