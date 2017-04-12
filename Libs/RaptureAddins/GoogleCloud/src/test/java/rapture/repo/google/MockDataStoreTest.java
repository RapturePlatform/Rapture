package rapture.repo.google;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.joda.time.Duration;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.google.cloud.datastore.testing.LocalDatastoreHelper;

public class MockDataStoreTest {

    final static LocalDatastoreHelper helper = LocalDatastoreHelper.create();

    @BeforeClass
    public static void setupLocalDatastore() throws IOException, InterruptedException {
        helper.start(); // Starts the local Datastore emulator in a separate process
        GoogleDatastoreKeyStore.setDatastoreOptionsForTesting(helper.getOptions());
        GoogleIndexHandler.setDatastoreOptionsForTesting(helper.getOptions());
    }

    @AfterClass
    public static void cleanupLocalDatastore() throws IOException, InterruptedException, TimeoutException {
        try {
            if (helper != null) helper.stop(new Duration(6000L));
        } catch (Exception e) {
            System.out.println("Exception shutting down LocalDatastoreHelper: " + e.getMessage());
        }
    }

}