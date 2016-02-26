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
package rapture.object.storage;

import static org.junit.Assert.assertEquals;

import rapture.common.RaptureScript;
import rapture.common.RaptureScriptPathBuilder;
import rapture.common.RaptureURI;
import rapture.common.Scheme;

import org.junit.Test;

public class StorageLocationFactoryTest {

    @Test
    public void testAddressToStorage() {
        RaptureURI addressURI = new RaptureURI("script://auth/my/path/filename");
        RaptureURI storageLocation = StorageLocationFactory
                .createStorageURI(RaptureScriptPathBuilder.getRepoName(), RaptureScriptPathBuilder.getPrefix(), addressURI);
        assertEquals("document://sys.RaptureConfig/script/auth/my/path/filename", storageLocation.toString());
    }

    @Test
    public void testObjectToStorage() {
        RaptureScript script = new RaptureScript();
        script.setAuthority("auth");
        script.setName("my/path/filename");

        RaptureURI storageLocation = RaptureURI.builder(Scheme.DOCUMENT, RaptureScriptPathBuilder.getRepoName())
                .docPath(RaptureScriptPathBuilder.getPrefix() + script.getStoragePath()).build();
        assertEquals("document://sys.RaptureConfig/script/auth/my/path/filename", storageLocation.toString());
    }
}
