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
package rapture.repo.google;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.joda.time.Duration;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;

import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import com.google.cloud.storage.testing.RemoteStorageHelper;
import com.google.cloud.storage.testing.RemoteStorageHelper.StorageHelperException;
import com.google.common.collect.ImmutableMap;

import rapture.blob.BlobStore;
import rapture.blob.BlobStoreContractTest;

public class GoogleBlobStoreTest extends BlobStoreContractTest {

    private static GoogleBlobStore store = null;
    static String bukkit = "davet_incapture_com";
    private static LocalDatastoreHelper helper = LocalDatastoreHelper.create();

    // Currently there isn't an emulator for Google Cloud Storage,
    // so an alternative is to create a test project.
    // RemoteStorageHelper contains convenience methods to make setting up and cleaning up
    // the test project easier. However we need a project ID to do that.

    @BeforeClass
    public static void setUp() {
        GoogleDatastoreKeyStore.setDatastoreOptionsForTesting(helper.getOptions());
        try {
            helper.start();
        } catch (IOException | InterruptedException e) {
            Assert.fail(e.getMessage());
        } // Starts the local Datastore emulator in a separate process

        RemoteStorageHelper helper = null;
        try {
            helper = RemoteStorageHelper.create();
        } catch (Exception e) {
            // That failed, try the key
            try {
                File key = new File("src/test/resources/key.json");
                Assume.assumeTrue("Cannot read " + key.getAbsolutePath(), key.canRead());
                helper = RemoteStorageHelper.create("todo3-incap", new FileInputStream(key));
            } catch (StorageHelperException | FileNotFoundException ee) {
                Assume.assumeNoException("Cannot create storage helper", ee);
            }
        }
        Assume.assumeNotNull("Storage helper not initialized", helper);
        GoogleBlobStore.setStorageForTesting(helper.getOptions().getService());
        store = new GoogleBlobStore();
        store.setConfig(ImmutableMap.of("prefix", bukkit));

    }

    @AfterClass
    public static void tearDown() throws IOException, InterruptedException, TimeoutException {
        if (store != null) store.destroyBucket(bukkit);
        try {
            helper.stop(new Duration(6000L));
        } catch (Exception e) {
            System.out.println("Exception shutting down LocalDatastoreHelper: " + e.getMessage());
        }
    }

    @Override
    public BlobStore getBlobStore() {
        return store;
    }

}
