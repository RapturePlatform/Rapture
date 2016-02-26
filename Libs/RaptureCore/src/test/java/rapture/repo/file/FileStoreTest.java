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
package rapture.repo.file;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import rapture.common.RaptureFolderInfo;

public class FileStoreTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    FileDataStore store = new FileDataStore();

    @Before
    public void setup() throws IOException {
        Map<String, String> config = new HashMap<String, String>();
        config.put("prefix", folder.newFolder("test1").getAbsolutePath());
        store.setConfig(config);
    }

    @Test
    public void testSimpleStore() {
        store.put("a/b/c", "Hello there");
        String val = store.get("a/b/c");
        System.out.println(val);
    }

    @Test
    public void testFolders() {
        store.put("a/b/c", "Hello there");
        store.put("a/b/d", "Hello there");
        List<RaptureFolderInfo> ret = store.getSubKeys("");
        show(ret);
        ret = store.getSubKeys("a");
        show(ret);
        ret = store.getSubKeys("a/b");
        show(ret);
        ret = store.getSubKeys("c");
        show(ret);
    }

    @Test
    public void showAll() {
        store.put("a/b/c", "Hello there");
        store.put("a/b/d", "Hello there");
        List<String> allKeys = store.getAllSubKeys("");
        for (String a : allKeys) {
            System.out.println(a);
        }
    }

    private void show(List<RaptureFolderInfo> ret) {
        for (RaptureFolderInfo r : ret) {
            System.out.println("Name=" + r.getName() + ",folder=" + r.isFolder());
        }
    }
}
