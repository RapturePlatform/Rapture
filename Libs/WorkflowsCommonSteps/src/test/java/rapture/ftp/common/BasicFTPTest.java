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
package rapture.ftp.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.DocApi;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.ftp.common.FTPRequest.Action;
import rapture.ftp.common.FTPRequest.Status;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class BasicFTPTest {

    static String saveInitSysConfig;
    static String saveRaptureRepo;
    private static final String auth = "test" + System.currentTimeMillis();
    private static final String REPO_USING_MEMORY = "REP {} USING MEMORY {prefix=\"/tmp/" + auth + "\"}";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        RaptureConfig.setLoadYaml(false);
        RaptureConfig config = ConfigLoader.getConf();
        saveRaptureRepo = config.RaptureRepo;
        saveInitSysConfig = config.InitSysConfig;

        config.RaptureRepo = REPO_USING_MEMORY;
        config.InitSysConfig = "NREP {} USING MEMORY { prefix=\"/tmp/" + auth + "/sys.config\"}";

        System.setProperty("LOGSTASH-ISENABLED", "false");
        Kernel.initBootstrap();

        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {prefix=\"/tmp/" + auth + "\"}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Read single file via FTP
     */
    @Test
    public void testSingleFTPReadWithConfigUri() {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("speedtest.tele2.net").setLoginId("ftp").setPassword("foo@bar")
                .setUseSFTP(false);

        CallingContext context = ContextFactory.getKernelUser();
        DocApi dapi = Kernel.getDoc();
        String configRepo = "document://test" + System.currentTimeMillis();
        dapi.createDocRepo(context, configRepo, "NREP {} USING MEMORY {}");
        dapi.putDoc(context, configRepo + "/Config", JacksonUtil.jsonFromObject(ftpConfig));

        Connection connection = new SFTPConnection(configRepo + "/Config");
        connection.connectAndLogin();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FTPRequest read = new FTPRequest(FTPRequest.Action.READ).setDestination(baos).setRemoteName("1KB.zip");
        connection.doAction(read);
        assertEquals(1024, baos.size());
        for (byte b : baos.toByteArray()) {
            assertEquals(0, b);
        }
        connection.logoffAndDisconnect();
    }

    /**
     * Read single file via FTP
     */
    @Test
    public void testSingleFTPRead() {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("speedtest.tele2.net").setLoginId("ftp").setPassword("foo@bar")
                .setUseSFTP(false);
        Connection connection = new SFTPConnection(ftpConfig);
        connection.connectAndLogin();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FTPRequest read = new FTPRequest(FTPRequest.Action.READ).setDestination(baos).setRemoteName("1KB.zip");
        connection.doAction(read);
        assertEquals(1024, baos.size());
        for (byte b : baos.toByteArray()) {
            assertEquals(0, b);
        }
        connection.logoffAndDisconnect();
    }

    /**
     * Read single file via SFTP
     */
    @Test
    public void testSingleSFTPRead() {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig();
        ftpConfig.setAddress("test.rebex.net").setPort(22).setLoginId("demo").setPassword("password").setUseSFTP(true);
        Connection connection = new SFTPConnection(ftpConfig);
        connection.connectAndLogin();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FTPRequest read = new FTPRequest(FTPRequest.Action.READ).setDestination(baos).setRemoteName("readme.txt");
        connection.doAction(read);
        connection.logoffAndDisconnect();
        assertEquals(403, baos.size());
        String[] data = baos.toString().split("\r");
        assertEquals("Welcome,", data[0]);
    }

    /**
     * Check existence of file via FTP
     */
    @Test
    public void testFTPExists() {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("speedtest.tele2.net").setLoginId("ftp").setPassword("foo@bar")
                .setUseSFTP(false);
        Connection connection = new SFTPConnection(ftpConfig);
        connection.connectAndLogin();
        List<FTPRequest> reads = new ArrayList<>();
        reads.add(new FTPRequest(FTPRequest.Action.EXISTS).setRemoteName("1KB.zip"));
        reads.add(new FTPRequest(FTPRequest.Action.EXISTS).setRemoteName("2KB.zip"));
        connection.doActions(reads);
        assertEquals(reads.get(0).getStatus(), Status.SUCCESS);
        assertEquals(reads.get(1).getStatus(), Status.ERROR);
        connection.logoffAndDisconnect();
    }

    /**
     * Check existence of file via SFTP
     */
    @Test
    public void testSFTPExists() {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig();
        ftpConfig.setAddress("test.rebex.net").setPort(22).setLoginId("demo").setPassword("password").setUseSFTP(true);
        Connection connection = new SFTPConnection(ftpConfig);
        connection.connectAndLogin();
        List<FTPRequest> reads = new ArrayList<>();
        reads.add(new FTPRequest(FTPRequest.Action.EXISTS).setRemoteName("readme.txt"));
        reads.add(new FTPRequest(FTPRequest.Action.EXISTS).setRemoteName("readyou.txt"));
        connection.doActions(reads);
        assertEquals(reads.get(0).getStatus(), Status.SUCCESS);
        assertEquals(reads.get(1).getStatus(), Status.ERROR);
        connection.logoffAndDisconnect();
    }

    /**
     * Read multiple files via FTP
     */
    @Test
    public void testMultiFTPRead() throws IOException {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig();
        ftpConfig.setAddress("speedtest.tele2.net").setLoginId("ftp").setPassword("foo@bar").setUseSFTP(false);
        Connection connection = new SFTPConnection(ftpConfig);
        connection.connectAndLogin();

        List<String> files = ImmutableList.of("1KB.zip", "100KB.zip");
        assertEquals(2, files.size());
        List<FTPRequest> reads = new ArrayList<>();
        Path tmpDir = Files.createTempDirectory("test");
        for (String file : files) {
            reads.add(new FTPRequest(Action.READ).setRemoteName(file).setLocalName(tmpDir + "/" + file.substring(file.lastIndexOf('/') + 1)));
        }
        connection.doActions(reads);
        connection.logoffAndDisconnect();
        File f = new File(reads.get(0).getLocalName());
        assertTrue(f.exists());
        assertTrue(f.canRead());
        assertEquals(1024, f.length());

        f = new File(reads.get(1).getLocalName());
        assertTrue(f.exists());
        assertTrue(f.canRead());
        assertEquals(102400, f.length());
    }

    /**
     * Read multiple files via SFTP
     */
    @Test
    public void testMultiSFTPRead() throws IOException {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig();
        ftpConfig.setAddress("test.rebex.net").setPort(22).setLoginId("demo").setPassword("password").setUseSFTP(true);
        Connection connection = new SFTPConnection(ftpConfig);
        connection.connectAndLogin();

        List<String> files = connection.listFiles("pub/example/*");
        assertEquals(19, files.size());
        List<FTPRequest> reads = new ArrayList<>();
        // Path tmpDir = Files.createTempDirectory("test");
        File tmpDir = new File("/tmp/test");
        tmpDir.mkdirs();
        for (String file : files) {
            reads.add(new FTPRequest(Action.READ).setRemoteName("pub/example/" + file).setLocalName(tmpDir + "/" + file.substring(file.lastIndexOf('/') + 1)));
        }
        connection.doActions(reads);
        connection.logoffAndDisconnect();
        File f = new File(reads.get(18).getLocalName());
        assertTrue(f.exists());
        assertTrue(f.canRead());
        assertEquals(17911, f.length());
    }

    /**
     * write single file via FTP -
     */
    @Test
    public void testSingleFTPWrite() {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig();
        ftpConfig.setAddress("speedtest.tele2.net").setLoginId("ftp").setPassword("foo@bar").setUseSFTP(false);
        Connection connection = new SFTPConnection(ftpConfig);
        connection.connectAndLogin();
        ByteArrayInputStream stream = new ByteArrayInputStream("Get loose, get loose!".getBytes());
        FTPRequest write = new FTPRequest(FTPRequest.Action.WRITE).setSource(stream).setRemoteName("upload/junk.dummyfile");
        connection.doAction(write);
        connection.logoffAndDisconnect();
    }

    /**
     * write single file via SFTP - need a SFTP serverw ith write access to test
     */
    @Test
    @Ignore
    public void testSingleSFTPWrite() {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig();
        ftpConfig.setAddress("test.rebex.net").setPort(22).setLoginId("demo").setPassword("password").setUseSFTP(true);
        Connection connection = new SFTPConnection(ftpConfig);
        connection.connectAndLogin();
        ByteArrayInputStream stream = new ByteArrayInputStream("Get loose, get loose!".getBytes());
        FTPRequest read = new FTPRequest(FTPRequest.Action.WRITE).setSource(stream).setRemoteName("junk");
        connection.doAction(read);
        connection.logoffAndDisconnect();
    }

    /**
     * Check for sensible error messages
     */
    @Test
    public void errorMessage1() {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("non.ext.iste.nt").setLoginId("ftp").setPassword("foo@bar").setUseSFTP(true);
        Connection connection = new SFTPConnection(ftpConfig);
        try {
            connection.connectAndLogin();
        } catch (Exception e) {
            Assert.assertEquals("Unknown host non.ext.iste.nt", e.getMessage());
        }
    }

    @Test
    public void errorMessage2() {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("localhost").setLoginId("ftp").setPassword("foo@bar").setUseSFTP(true);
        Connection connection = new SFTPConnection(ftpConfig);
        try {
            connection.connectAndLogin();
        } catch (Exception e) {
            Assert.assertEquals("Unable to establish secure FTP connection to localhost", e.getMessage());
        }
    }

    @Test
    public void errorMessage3() {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("non.ext.iste.nt").setLoginId("ftp").setPassword("foo@bar").setUseSFTP(false);
        Connection connection = new SFTPConnection(ftpConfig);
        try {
            connection.connectAndLogin();
        } catch (Exception e) {
            Assert.assertEquals("Unknown host non.ext.iste.nt", e.getMessage());
        }
    }

    @Test
    public void errorMessage4() {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("localhost").setLoginId("ftp").setPassword("foo@bar").setUseSFTP(false);
        Connection connection = new SFTPConnection(ftpConfig);
        try {
            connection.connectAndLogin();
        } catch (Exception e) {
            Assert.assertEquals("Could not login to localhost as ftp", e.getMessage());
        }
    }

}
