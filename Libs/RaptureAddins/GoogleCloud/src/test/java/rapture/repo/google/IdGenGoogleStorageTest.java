package rapture.repo.google;


import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.testing.RemoteStorageHelper;
import com.google.cloud.storage.testing.RemoteStorageHelper.StorageHelperException;
import com.google.common.collect.ImmutableMap;

import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.dsl.idgen.RaptureIdGen;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class IdGenGoogleStorageTest {
    UUID uuid = UUID.randomUUID();

    static RemoteStorageHelper storageHelper = null;
    static Storage storage;

    // Make sure
    // * that the google SDK is installed
    // * that $PATH includes google-cloud-sdk/bin
    // that gcloud version returns a value for cloud-datastore-emulator
    //
    // You can install cloud-datastore-emulator with the command gcloud beta emulators datastore start

    @BeforeClass
    static public void setUp() {
        String namespace = UUID.randomUUID().toString();
        try {
            storageHelper = RemoteStorageHelper.create();
            storage = storageHelper.getOptions().getService();
            IdGenGoogleStorage.setStorageForTesting(storage);
        } catch (Exception e1) {
            try {
                File key = new File("src/test/resources/key.json");
                Assume.assumeTrue("Cannot read " + key.getAbsolutePath(), key.canRead());
                storageHelper = RemoteStorageHelper.create("todo3-incap", new FileInputStream(key));
            } catch (StorageHelperException | FileNotFoundException e) {
                Assume.assumeNoException("Cannot create storage helper", e);
            }
        }
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {prefix=\"/tmp/" + UUID.randomUUID() + "\"}");
    }

    @Test
    public void testIdGen1() {
        try {
            RaptureIdGen f = new RaptureIdGen();
            IdGenGoogleStorage iggs = new IdGenGoogleStorage();
            iggs.setConfig(ImmutableMap.of("projectid", "todo3-incap", "prefix", "TST"));
            f.setIdGenStore(iggs);
            f.setProcessorConfig(ImmutableMap.of("initial", "10", "base", "26", "length", "8", "prefix", "TST"));
            String result = f.incrementIdGen(10L);
            assertEquals("TST0000000K", result);
        } catch (Exception e) {
            String error = ExceptionToString.format(e);
            if (error.contains("com.google.cloud.storage.StorageException: 401 Unauthorized")) Assume.assumeNoException(e);
            throw e;
        }
    }

    @Test
    public void testIdGen2() {
        try {
            RaptureIdGen f = new RaptureIdGen();
            IdGenGoogleStorage iggs = new IdGenGoogleStorage();
            iggs.setConfig(ImmutableMap.of("projectid", "todo3-incap", "prefix", "TST"));
            f.setIdGenStore(iggs);
            f.setProcessorConfig(ImmutableMap.of("initial", "706216874", "base", "36", "length", "6", "prefix", "OI-"));

            String result = f.incrementIdGen(1L);
            assertEquals("OI-BOGOFF", result);
            result = f.incrementIdGen(735953563L);
            assertEquals("OI-NUMPTY", result);
            result = f.incrementIdGen(655030524L);
            assertEquals("OI-YOMAMA", result);
            f.invalidate();
            try {
                result = f.incrementIdGen(2L);
                Assert.fail("ID Generator was invalidated");
            } catch (RaptureException e) {
                // assertEquals("IdGenerator has been deleted", e.getMessage());
            }
        } catch (Exception e) {
            String error = ExceptionToString.format(e);
            if (error.contains("com.google.cloud.storage.StorageException: 401 Unauthorized")) Assume.assumeNoException(e);
            throw e;
        }
    }

}
