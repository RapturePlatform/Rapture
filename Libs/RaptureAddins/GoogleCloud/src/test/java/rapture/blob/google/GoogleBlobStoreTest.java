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
import org.junit.Assume;
import org.junit.Before;

import com.google.common.collect.ImmutableMap;

import rapture.blob.BlobStore;
import rapture.blob.BlobStoreContractTest;
import rapture.field.FieldTransformLoader;

public class GoogleBlobStoreTest extends BlobStoreContractTest {

    private GoogleBlobStore store = null;
    String bukkit = "dave_incapture_com";

    // Currently there isn't an emulator for Google Cloud Storage,
    // so an alternative is to create a test project.
    // RemoteStorageHelper contains convenience methods to make setting up and cleaning up
    // the test project easier. However we need a project ID to do that.

    @Before
    public void setUp() {
        String projectId = System.getenv("PROJECT_ID"); // "high-plating-157918"
        Assume.assumeNotNull(projectId);

        Map<String, String> config = ImmutableMap.of("prefix", bukkit, "projectid", projectId);
        this.store = new GoogleBlobStore();
        store.setConfig(config);
        FieldTransformLoader ftl;
    }

    @After
    public void tearDown() throws IOException {
        if (store != null) store.destroyBucket(bukkit);
    }

    @Override
    public BlobStore getBlobStore() {
        return store;
    }

}
