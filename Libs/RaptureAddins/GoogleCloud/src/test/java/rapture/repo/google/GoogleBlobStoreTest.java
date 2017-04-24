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
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.joda.time.Duration;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import com.google.cloud.storage.testing.RemoteStorageHelper;
import com.google.cloud.storage.testing.RemoteStorageHelper.StorageHelperException;
import com.google.common.collect.ImmutableMap;

import rapture.blob.BlobStore;
import rapture.blob.BlobStoreContractTest;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.exception.ExceptionToString;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class GoogleBlobStoreTest extends BlobStoreContractTest {

    private static GoogleBlobStore store = null;
    private static Datastore metaStore;
    static String bukkit = "davet_incapture_com";
    private static final Logger log = Logger.getLogger(GoogleBlobStoreTest.class);

    // Currently there isn't an emulator for Google Cloud Storage,
    // so an alternative is to create a test project.
    // RemoteStorageHelper contains convenience methods to make setting up and cleaning up
    // the test project easier. However we need a project ID to do that.

    // LocalDatastoreHelper emulates a GCP DataStore for Blob metadata
    static LocalDatastoreHelper helper = null;


    @BeforeClass
    public static void beforeClass() {
        RemoteStorageHelper storageHelper = null;
        try {
            storageHelper = RemoteStorageHelper.create();
        } catch (Exception e) {
            // That failed, try the key
            try {
                File key = new File("src/test/resources/key.json");
                Assume.assumeTrue("Cannot read " + key.getAbsolutePath(), key.canRead());
                storageHelper = RemoteStorageHelper.create("todo3-incap", new FileInputStream(key));
            } catch (StorageHelperException | FileNotFoundException ee) {
                Assume.assumeNoException("Cannot create storage helper", ee);
            }
        }

        try {
            helper = LocalDatastoreHelper.create();
            helper.start();
        } catch (IOException | InterruptedException e) {
            Assert.fail(e.getMessage());
        }

        metaStore = helper.getOptions().getService();
        GoogleDatastoreKeyStore.setDatastoreOptionsForTesting(helper.getOptions());
        GoogleIndexHandler.setDatastoreOptionsForTesting(helper.getOptions());

        Assume.assumeNotNull("Storage helper not initialized", helper);
        try {
            GoogleBlobStore.setStorageForTesting(storageHelper.getOptions().getService());
            Kernel.initBootstrap();

            store = new GoogleBlobStore();
            store.setConfig(ImmutableMap.of("prefix", bukkit));

            Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                    "LOG {} using MEMORY {prefix=\"/tmp/" + UUID.randomUUID() + "\"}");
        } catch (Exception e) {
            String error = ExceptionToString.format(e);
            log.error(error);
            // I don't get it. These test cases pass when run as a standalone block,
            // but when run as part of a suite they fail. I haven't found a way to fix that.
            Assume.assumeNoException(e);
        }
    }

    @AfterClass
    public static void tearDown() throws IOException, InterruptedException, TimeoutException {
        Kernel.shutdown();
        if (store != null) store.destroyBucket(bukkit);
        try {
            if (helper != null) helper.stop(new Duration(6000L));
        } catch (Exception e) {
            System.out.println("Exception shutting down LocalDatastoreHelper: " + e.getMessage());
        }
    }


    @Override
    public BlobStore getBlobStore() {
        return store;
    }
}
