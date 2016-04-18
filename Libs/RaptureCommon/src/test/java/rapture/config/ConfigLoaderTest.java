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
package rapture.config;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.mockStaticPartial;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import rapture.home.RaptureHomeRetriever;
//junit
//imports to intercept logging
//mock imports

@RunWith(PowerMockRunner.class)
@PrepareForTest({ System.class, ConfigLoader.class, RaptureHomeRetriever.class })
public class ConfigLoaderTest {
    private String resPath = "src/test/resources/etc";
    private String raptureFilename = "/rapturetestconfig.cfg";
    private String corruptRaptureFilename = "/rapturecorrupt.cfg";
    private final Logger log = Logger.getLogger(ConfigLoader.class);
    private final ProxyTestAppender appender = new ProxyTestAppender();

    @Before
    public void setup() {
        Logger.getRootLogger().setLevel(Level.INFO);
        log.addAppender(appender);
        mockStaticPartial(System.class, "getProperty");
        File fs = new File(resPath + raptureFilename);
        String filePath = getFilePath(fs.getAbsolutePath());
        expect(System.getProperty(RaptureHomeRetriever.APP_NAME)).andStubReturn("TestApp1");
        expect(System.getProperty(RaptureHomeRetriever.RAPTURE_HOME)).andStubReturn(filePath);
        expect(System.getProperty(RaptureHomeRetriever.GLOBAL_CONFIG_HOME)).andStubReturn(filePath);
    }

    @Test
    public void loadConfigUsingRaptureHomeTest() {
        // setup expected behavior of System.class (record mode in easymock)
        // use andStubReturn as we are not testing for number of expected calls
        // on method
        expect(System.getProperty("rapture.config")).andStubReturn(null);
        expect(System.getProperty("RAPTURE_CONFIG_FILENAME")).andStubReturn(raptureFilename);
        expect(System.getProperty("os.arch")).andStubReturn("64");
        replay(System.class); // switch from record to replay mode

        ConfigLoader.loadYaml();
        verifyAll(); // easymock function to verify expected behavior of
                     // system.class calls

        List<LoggingEvent> log = appender.getLog();

        // assert on the last log event which should be ""Config loaded from
        // RAPTURE HOME: " + fileName".
        String actualLogMessage = log.get(log.size() - 1).getRenderedMessage();
        Level actualLogLevel = log.get(log.size() - 1).getLevel();
        Assert.assertEquals(Level.INFO, actualLogLevel);
        Assert.assertTrue(actualLogMessage.contains("Successfully"));

        // get the config object
        RaptureConfig rc = ConfigLoader.getConf();
        // assert on all fields in RaptureConfig object
        Assert.assertEquals("REP {} USING MONGODB { prefix=\"rapture.bootstrap\" test }", rc.RaptureRepo);
        Assert.assertEquals("testInstance", rc.AppInstance);
        Assert.assertEquals("testAppName", rc.AppName);
        Assert.assertEquals("1000", rc.CacheExpiry);
        Assert.assertEquals("alpha_test", rc.Categories);
        Assert.assertEquals(
                "{\"testidToHook\":{\"AuditHook\":{\"id\":\"AuditHook\",\"className\":\"rapture.api.hooks.impl.AuditApiHook\",\"hookTypes\":[\"PRE\"]}}}",
                rc.DefaultApiHooks);
        Assert.assertEquals("LOG {} using MEMORY {} test", rc.DefaultAudit);
        Assert.assertEquals("test COMMENTARY {} USING MONGODB { prefix=\"sys.commentary.main\" }", rc.DefaultCommentary);
        Assert.assertEquals("EXCHANGE {} USING RABBITMQ {} test", rc.DefaultExchange);
        Assert.assertEquals("test NOTIFICATION USING MEMORY {}", rc.DefaultNotification);
        Assert.assertEquals("test TABLE {} USING MONGODB { prefix=\"pipelineTaskStatus\" }", rc.DefaultPipelineTaskStatus);
        Assert.assertEquals("test STATUS {} USING MEMORY {}", rc.DefaultStatus);
        Assert.assertEquals("NREP {} USING MONGODB test { prefix=\"sys.config\"}", rc.InitSysConfig);
        Assert.assertEquals("REP {} USING MONGODB { prefix=\"test.emphemeral\"}", rc.InitSysEphemeral);
        Assert.assertEquals("NREP {} USING MONGODB { prefix=test\"sys.settings\"}", rc.InitSysSettings);
        Assert.assertEquals("testuser", rc.KernelUser);
        Assert.assertEquals("testServer", rc.ServerGroup);
        Assert.assertEquals("webserver_test", rc.ServerType);
        // Assert.assertEquals("NREP {} USING MONGODB { prefix=\"${authority}.${test}\"}",
        // rc.StandardTemplate);
        Assert.assertEquals(true, rc.InitConfig);
        Assert.assertEquals(true, rc.WorkflowOnPipeline);
        Assert.assertEquals("8667", rc.web.port);
        Assert.assertEquals(true, RaptureConfig.getLoadYaml());
    }

    // Test: rapture.cfg env variable not set; should default to "rapture.cfg"
    @Test
    public void loadConfigUsingRaptureHomeCfgFileNotSetTest() {
        // load the rapture config file
        // setup system.class to ensure cfg is loaded from url
        // setup expected behavior of System.class (record mode in easymock)
        expect(System.getProperty("rapture.config")).andStubReturn(null);
        expect(System.getProperty("RAPTURE_CONFIG_FILENAME")).andStubReturn(null);
        expect(System.getProperty("os.arch")).andStubReturn("64");
        replay(System.class); // switch from record to replay mode

        ConfigLoader.loadYaml();
        verifyAll();

        List<LoggingEvent> log = appender.getLog();
        // assert on the last log event which should be "loading settings from".
        String actualLogMessage = log.get(log.size() - 1).getRenderedMessage();
        Level actualLogLevel = log.get(log.size() - 1).getLevel();
        Assert.assertEquals(Level.INFO, actualLogLevel);
        Assert.assertTrue(actualLogMessage.contains("Successfully"));

        // get the config object; this should be populated from
        // /src/test/resources/rapture.cfg
        RaptureConfig rc = ConfigLoader.getConf();
        Assert.assertNotNull(rc);
        // test the expected file was loaded; first and last properties verified
        Assert.assertEquals("This cfg is for testing purposes only! %&!@()!_", rc.RaptureRepo);
        Assert.assertEquals(
                "{\"testidToHook\":{\"AuditHook\":{\"id\":\"AuditHook\",\"className\":\"rapture.api.hooks.impl.AuditApiHook\",\"hookTypes\":[\"PRE\"]}}}",
                rc.DefaultApiHooks);
    }

    // Test: Load config file from classpath

    // Test: Load config file from current directory

    // Test: Use a corrupted rapture.cfg to trigger the expected behavior.
    @Test
    public void loadConfigFailedLoadTest() {
        // setup system.class to ensure cfg is loaded from url
        // setup expected behavior of System.class (record mode in easymock)
        expect(System.getProperty("rapture.config")).andStubReturn(null);
        expect(System.getProperty("RAPTURE_CONFIG_FILENAME")).andStubReturn(corruptRaptureFilename);
        // expect(System.getProperty("os.arch")).andReturn("64").times(1);
        replay(System.class); // switch from record to replay mode

        ConfigLoader.loadYaml();
        verifyAll();
        List<LoggingEvent> log = appender.getLog();
        // assert on the last log event which should be "loading settings from".
        String actualLogMessage = log.get(log.size() - 1).getRenderedMessage();
        Level actualLogLevel = log.get(log.size() - 1).getLevel();
        Assert.assertEquals(Level.FATAL, actualLogLevel);
        Assert.assertTrue(actualLogMessage.equals("Unable to load rapture config from any sources."));
    }

    public void setupReaders() {
        MultiValueConfigLoader.setSysPropReader(new ValueReader() {

            @Override
            public String getValue(String property) {
                if (property.startsWith("P")) {
                    return property;
                }
                return null;
            }

        });

        MultiValueConfigLoader.setEnvReader(new ValueReader() {
            @Override
            public String getValue(String property) {
                if (property.startsWith("E")) {
                    return property;
                }
                return null;
            }

        });

    }

    @Test
    public void testClasspathBasedRetrieval() {
        assertTrue(MultiValueConfigLoader.getConfig("GTEST-xyz").equals("XYZ"));
    }

    @After
    public void tearDown() {
        log.removeAppender(appender);
    }

    private String getFilePath(String fullPath) { // returns filename without
                                                  // extension
        int sep = fullPath.lastIndexOf(File.separator);
        return fullPath.substring(0, sep);
    }

    @Test
    public void loadWithGlobalOverrides() {
        expect(System.getProperty("rapture.config")).andStubReturn(null);
        expect(System.getProperty("RAPTURE_CONFIG_FILENAME")).andStubReturn("toOverrideGlobally.cfg");
        expect(System.getProperty("os.arch")).andStubReturn("64");
        replay(System.class); // switch from record to replay mode

        ConfigLoader.loadYaml();
        verifyAll(); // easymock function to verify expected behavior of
                     // system.class calls

        // get the config object
        RaptureConfig rc = ConfigLoader.getConf();
        Assert.assertEquals("catOverrideGlobal", rc.Categories);
        Assert.assertEquals("userOverrideGlobal", rc.KernelUser);
        Assert.assertEquals("888", rc.web.port);
        Assert.assertEquals(new RaptureConfig().RaptureRepo, rc.RaptureRepo);
        Assert.assertEquals(true, RaptureConfig.getLoadYaml());
    }
    
    @Test
    public void loadWithAppOverrides() {
        expect(System.getProperty("rapture.config")).andStubReturn(null);
        expect(System.getProperty("RAPTURE_CONFIG_FILENAME")).andStubReturn("toOverrideWithApp.cfg");
        expect(System.getProperty("os.arch")).andStubReturn("64");
        replay(System.class); // switch from record to replay mode

        ConfigLoader.loadYaml();
        verifyAll(); // easymock function to verify expected behavior of
                     // system.class calls

        // get the config object
        RaptureConfig rc = ConfigLoader.getConf();
        // assert on all fields in RaptureConfig object
        Assert.assertEquals("catOverrideApp", rc.Categories);
        Assert.assertEquals("userOverrideGlobal", rc.KernelUser);
        Assert.assertEquals("888", rc.web.port);
        Assert.assertEquals(new RaptureConfig().RaptureRepo, rc.RaptureRepo);
    }
}

//
class ProxyTestAppender extends AppenderSkeleton {
    private final List<LoggingEvent> log = new ArrayList<LoggingEvent>();

    @Override
    public boolean requiresLayout() {
        return false;
    }

    @Override
    protected void append(final LoggingEvent loggingEvent) {
        log.add(loggingEvent);
    }

    @Override
    public void close() {
    }

    public List<LoggingEvent> getLog() {
        return new ArrayList<LoggingEvent>(log);
    }
}
