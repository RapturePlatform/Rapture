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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.jmx.JmxApp;
import rapture.jmx.JmxAppCache;
import rapture.jmx.JmxServer;

public class EnvironmentApiImplTest {

    private static final Logger log = Logger.getLogger(EnvironmentApiImplTest.class);

    private CallingContext ctx = ContextFactory.getKernelUser();

    private JmxApp app;

    @Before
    public void setup() {
        Kernel.getKernel().restart();
        Kernel.initBootstrap();
        if (!JmxServer.getInstance().isStarted()) {
            JmxServer.getInstance().start("jmxServerTest");
        }
        app = JmxServer.getInstance().getJmxApp();
    }

    @Test
    public void testGetAppNames() {
        List<String> result = Kernel.getEnvironment().getAppNames(ctx);
        assertTrue(result.size() >= 1);
        boolean found = false;
        for (String name : result) {
            if (name.startsWith("jmxServerTest")) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testGetOperatingSystemInfo() {
        Map<String, String> result = Kernel.getEnvironment().getOperatingSystemInfo(ctx, Arrays.asList(app.toString()));
        assertEquals(1, result.size());
        Map<String, Object> value = getValueMap(result, app.toString());
        assertTrue(value.containsKey("FreePhysicalMemorySize"));
        assertStatus(result, app.toString());
    }

    @Test
    public void testReadByPath() {
        Map<String, String> result = Kernel.getEnvironment().readByPath(ctx, Arrays.asList(app.toString()), "java.lang:type=Memory/HeapMemoryUsage");
        assertEquals(1, result.size());
        Map<String, Object> value = getValueMap(result, app.toString());
        assertTrue(value.containsKey("used"));
        assertStatus(result, app.toString());
    }

    @Test
    public void testWriteByPath() {
        Map<String, String> result = Kernel.getEnvironment().writeByPath(ctx, Arrays.asList(app.toString()), "java.lang:type=Memory/Verbose/true");
        assertEquals(1, result.size());
        assertStatus(result, app.toString());
        result = Kernel.getEnvironment().writeByPath(ctx, Arrays.asList(app.toString()), "java.lang:type=Memory/Verbose/false");
        assertEquals(1, result.size());
        assertStatus(result, app.toString());
    }

    @Test
    public void testExecByPath() {
        Map<String, String> result = Kernel.getEnvironment().execByPath(ctx, Arrays.asList(app.toString()),
                "rapture.jmx.beans:type=RaptureLogging/setLevel/rapture.kernel.EnvironmentApiImplTest/TRACE");
        assertEquals(1, result.size());
        assertStatus(result, app.toString());
        assertEquals(Level.TRACE, log.getLevel());
    }

    @Test
    public void testReadByJson() {
        Map<String, String> result = Kernel.getEnvironment().readByJson(ctx, Arrays.asList(app.toString()), "{\n" +
                "   \"type\":\"read\",\n" +
                "   \"mbean\":\"java.lang:type=Threading\",\n" +
                "   \"attribute\":\"ThreadCount\"\n" +
                "}");
        assertEquals(1, result.size());
        assertStatus(result, app.toString());
    }

    @Test
    public void testWriteByJson() {
        Map<String, String> result = Kernel.getEnvironment().writeByJson(ctx, Arrays.asList(app.toString()), "{\n" +
                "   \"type\":\"write\",\n" +
                "   \"mbean\":\"java.lang:type=Memory\",\n" +
                "   \"attribute\":\"Verbose\",\n" +
                "   \"value\":\"true\"\n" +
                "}");
        assertEquals(1, result.size());
        assertStatus(result, app.toString());
        result = Kernel.getEnvironment().writeByJson(ctx, Arrays.asList(app.toString()), "{\n" +
                "   \"type\":\"write\",\n" +
                "   \"mbean\":\"java.lang:type=Memory\",\n" +
                "   \"attribute\":\"Verbose\",\n" +
                "   \"value\":\"false\"\n" +
                "}");
        assertEquals(1, result.size());
        assertStatus(result, app.toString());
    }

    @Test
    public void testExecByJson() {
        Map<String, String> result = Kernel.getEnvironment().writeByJson(ctx, Arrays.asList(app.toString()), "{\n" +
                "   \"type\":\"exec\",\n" +
                "   \"mbean\":\"rapture.jmx.beans:type=RaptureLogging\",\n" +
                "   \"operation\":\"setLevel\",\n" +
                "   \"arguments\":[\"rapture.kernel.EnvironmentApiImplTest\",\"DEBUG\"]\n" +
                "}");
        assertEquals(1, result.size());
        assertStatus(result, app.toString());
        assertEquals(Level.DEBUG, log.getLevel());
    }

    @Test
    public void testJmxAppCacheExpiryWrite() {
        assertEquals(1, JmxAppCache.getInstance().getCacheExpiry());
        Map<String, String> result = Kernel.getEnvironment().writeByJson(ctx, Arrays.asList(app.toString()), "{\n" +
                "   \"type\":\"write\",\n" +
                "   \"mbean\":\"rapture.jmx.beans:type=JmxAppCache\",\n" +
                "   \"attribute\":\"CacheExpiry\",\n" +
                "   \"value\":3\n" +
                "}");
        assertEquals(1, result.size());
        assertStatus(result, app.toString());
        assertEquals(3, JmxAppCache.getInstance().getCacheExpiry());
        result = Kernel.getEnvironment().writeByJson(ctx, Arrays.asList(app.toString()), "{\n" +
                "   \"type\":\"write\",\n" +
                "   \"mbean\":\"rapture.jmx.beans:type=JmxAppCache\",\n" +
                "   \"attribute\":\"CacheExpiry\",\n" +
                "   \"value\":1\n" +
                "}");
        assertEquals(1, result.size());
        assertStatus(result, app.toString());
        assertEquals(1, JmxAppCache.getInstance().getCacheExpiry());
    }

    @Test
    public void testJmxAppCacheExpiryRead() {
        assertEquals(1, JmxAppCache.getInstance().getCacheExpiry());
        Map<String, String> result = Kernel.getEnvironment().writeByJson(ctx, Arrays.asList(app.toString()), "{\n" +
                "   \"type\":\"read\",\n" +
                "   \"mbean\":\"rapture.jmx.beans:type=JmxAppCache\",\n" +
                "   \"attribute\":\"CacheExpiry\",\n" +
                "}");
        assertEquals(1, result.size());
        assertStatus(result, app.toString());
        assertEquals(1, JmxAppCache.getInstance().getCacheExpiry());
    }

    @Test
    public void testUnknown() {
        Map<String, String> result = Kernel.getEnvironment().execByPath(ctx, Arrays.asList(app.toString()), "rapture.jmx:type=UnknownBean/someMethod/someArg");
        assertEquals(1, result.size());
        Map<String, Object> jsonMap = JacksonUtil.getMapFromJson(result.get(app.toString()));
        assertEquals(404, (int) jsonMap.get("status"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getValueMap(Map<String, String> result, String app) {
        Map<String, Object> jsonMap = JacksonUtil.getMapFromJson(result.get(app.toString()));
        return (Map<String, Object>) jsonMap.get("value");
    }

    private void assertStatus(Map<String, String> result, String app) {
        Map<String, Object> jsonMap = JacksonUtil.getMapFromJson(result.get(app.toString()));
        assertEquals(200, (int) jsonMap.get("status"));
    }
}