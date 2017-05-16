package rapture.repo.google;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.threeten.bp.Duration;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;

import rapture.common.CallingContext;
import rapture.common.exception.ExceptionToString;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.kernel.AbstractFileTest;
import rapture.pipeline2.gcp.PubsubPipeline2Handler;

public class LocalDataStoreTest {

    private final static LocalDatastoreHelper helper = LocalDatastoreHelper.create();
    static Datastore localDatastore;

    public static String saveInitSysConfig;
    public static String saveRaptureRepo;
    public static final String auth = "test" + System.currentTimeMillis();
    public static List<File> files = new ArrayList<>();
    public static String[] suffixes = new String[] { "", "_meta", "_attribute", "-2d1", "-2d1_meta", "-2d1_attribute", "_blob", "_sheet", "_sheetmeta",
            "_series" };
    public static RaptureConfig config;
    public static CallingContext callingContext;

    @BeforeClass
    public static void setupLocalDatastore() throws IOException, InterruptedException {
        helper.start(); // Starts the local Datastore emulator in a separate process
        localDatastore = helper.getOptions().getService();

        GoogleDatastoreKeyStore.setDatastoreOptionsForTesting(helper.getOptions());
        GoogleIndexHandler.setDatastoreOptionsForTesting(helper.getOptions());

        for (String s : suffixes) {
            File temp = new File("/tmp/" + auth + s);
            temp.deleteOnExit();
            files.add(temp);
        }
        RaptureConfig.setLoadYaml(false);
        config = ConfigLoader.getConf();
        saveRaptureRepo = config.RaptureRepo;
        saveInitSysConfig = config.InitSysConfig;
        System.setProperty("LOGSTASH-ISENABLED", "false");
        callingContext = new CallingContext();
        callingContext.setUser("dummy");
    }

    @AfterClass
    public static void cleanupLocalDatastore() throws IOException, InterruptedException, TimeoutException {
        try {
            for (File temp : files)
                try {
                    if (temp.isDirectory()) FileUtils.deleteDirectory(temp);
                } catch (Exception e) {
                    Logger.getLogger(AbstractFileTest.class).warn("Cannot delete " + temp);
                    // Unable to clean up properly
                }
            ConfigLoader.getConf().InitSysConfig = saveInitSysConfig;
            ConfigLoader.getConf().RaptureRepo = saveRaptureRepo;

            PubsubPipeline2Handler.cleanUp();
            if (helper != null) helper.stop(Duration.ofSeconds(6000L));
        } catch (Exception e) {
            System.out.println("Exception shutting down LocalDatastoreHelper: " + ExceptionToString.format(e));
        }
    }
}
