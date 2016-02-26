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
package rapture.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.testng.collections.Maps;

public class TreeCacheTest {
    @Test
    public void testSimpleRegister() {
        Callback c = new Callback();
        TreeCache tc = new TreeCache(c);
        tc.registerKey("/alpha/bravo/charlie");
        assertTrue(c.isPresent("alpha/bravo/charlie"));
        assertFalse(c.isFolder("alpha/bravo/charlie"));
        assertFalse(c.isPresent("alpha/bravo/delta"));
        assertFalse(c.isPresent("whatever"));
        assertTrue(c.isPresent("alpha/bravo"));
        assertTrue(c.isPresent("alpha"));
        assertTrue(c.isFolder("alpha/bravo"));
        assertTrue(c.isFolder("alpha"));
    }

    @Test
    public void testSlashless() {
        Callback c = new Callback();
        TreeCache tc = new TreeCache(c);
        tc.registerKey("solo");
        assertTrue(c.isPresent("solo"));
        assertFalse(c.isFolder("solo"));
    }

    @Test
    public void testShadow() {
        Callback c = new Callback();
        TreeCache tc = new TreeCache(c);
        tc.registerKey("/first/push");
        assertTrue(c.isPresent("first/push"));
        assertFalse(c.isFolder("first/push"));
        tc.registerKey("first/push/adjusted");
        assertTrue(c.isPresent("first/push"));
        assertTrue(c.isFolder("first/push"));
        assertTrue(c.isPresent("first/push/adjusted"));
        assertFalse(c.isFolder("first/push/adjusted"));
    }

    @Test
    public void testReverseShadow() {
        Callback c = new Callback();
        TreeCache tc = new TreeCache(c);
        tc.registerKey("first/push/adjusted");
        assertTrue(c.isPresent("first/push"));
        assertTrue(c.isFolder("first/push"));
        assertTrue(c.isPresent("first/push/adjusted"));
        assertFalse(c.isFolder("first/push/adjusted"));
        tc.registerKey("/first/push");
        assertTrue(c.isPresent("first/push"));
        assertFalse(c.isFolder("first/push"));
    }

    @Test
    public void testDelete() {
        Callback c = new Callback();
        TreeCache tc = new TreeCache(c);
        tc.registerKey("first/push/elder");
        tc.registerKey("first/push/younger");
        assertTrue(c.isPresent("first/push"));
        assertTrue(c.isFolder("first/push"));
        assertTrue(c.isPresent("first/push/elder"));
        assertFalse(c.isFolder("first/push/elder"));
        assertTrue(c.isPresent("first/push/younger"));
        assertFalse(c.isFolder("first/push/younger"));
        tc.unregisterKey("first/push/elder");
        assertFalse(c.isPresent("first/push/elder"));
        assertTrue(c.isPresent("first/push/younger"));
        tc.registerKey("first/push/elder");
        assertTrue(c.isPresent("first/push/elder"));
        assertTrue(c.isPresent("first/push/younger"));
    }

    public static class Callback implements TreeCache.Callback {
        Map<String, Boolean> map = Maps.newHashMap();

        @Override
        public void register(String key, boolean isFolder) {
            map.put(key, isFolder);
        }

        boolean isPresent(String key) {
            return map.containsKey(key);
        }

        boolean isFolder(String key) {
            Boolean result = map.get(key);
            if (result == null) throw new NullPointerException();
            return result;
        }

        @Override
        public void unregister(String key, boolean isFolder) {
            map.remove(key);
        }
    }
}
