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
package rapture.plugin.install;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import rapture.common.PluginManifest;
import rapture.common.PluginManifestItem;
import rapture.common.RaptureURI;
import rapture.common.Scheme;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PluginSandboxTest {

    private PluginSandbox ps;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setupPS() throws IOException {
        ps = new PluginSandbox();
        ps.setRootDir(tempFolder.newFile());
    }

    @Test
    public void testMakeManifest() {
        RaptureURI docURI = new RaptureURI("//authority/folder/document", Scheme.DOCUMENT);
        ps.addURI(null, docURI);
        ps.addURI("variant1", docURI);

        PluginManifest manifest = ps.makeManifest(null);
        assertNotNull(manifest);
        List<PluginManifestItem> contents = manifest.getContents();
        assertEquals(1, contents.size());

        manifest = ps.makeManifest("variant1");
        assertNotNull(manifest);
        contents = manifest.getContents();
        assertEquals(1, contents.size());
    }

    @Test
    public void testGetItems() {
        RaptureURI docURI = new RaptureURI("//authority/folder/document", Scheme.DOCUMENT);
        ps.addURI(null, docURI);
        ps.addURI("variant1", docURI);

        Iterable<PluginSandboxItem> items = ps.getItems(null);
        assertNotNull(items);
        Iterator<PluginSandboxItem> iterator = items.iterator();
        assertTrue("Should be exactly 1 item", iterator.hasNext());
        iterator.next();
        assertFalse("Should be exactly 1 item", iterator.hasNext());

        items = ps.getItems("variant1");
        assertNotNull(items);
        iterator = items.iterator();
        assertTrue("Should be exactly 1 item", iterator.hasNext());
        iterator.next();
        assertFalse("Should be exactly 1 item", iterator.hasNext());
    }
}