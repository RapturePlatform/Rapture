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
package rapture.blob.google;

import java.io.IOException;
import java.util.Map;

import org.junit.After;
import org.junit.Before;

import com.google.common.collect.ImmutableMap;

import rapture.blob.BlobStore;
import rapture.blob.BlobStoreContractTest;

public class GoogleBlobStoreTest extends BlobStoreContractTest {

    private GoogleBlobStore store;
    String bukkit = "dave_incapture_com";

    @Before
    public void setUp() {
        Map<String, String> config = ImmutableMap.of("prefix", bukkit, "projectid", "high-plating-157918");
        this.store = new GoogleBlobStore();
        store.setConfig(config);
    }

    @After
    public void tearDown() throws IOException {
        store.destroyBucket(bukkit);
    }

    @Override
    public BlobStore getBlobStore() {
        return store;
    }

}
