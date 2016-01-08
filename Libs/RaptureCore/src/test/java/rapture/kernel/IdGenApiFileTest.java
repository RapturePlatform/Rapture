/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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
package rapture.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.impl.jackson.MD5Utils;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.dsl.idgen.IdGenFactory;
import rapture.dsl.idgen.RaptureIdGen;

public class IdGenApiFileTest {
    private static final String auth = "test" + System.currentTimeMillis();
    private static final String REPO_USING_FILE = "REP {} USING FILE {prefix=\"/tmp/" + auth + "\"}";
    
    String saveRaptureRepo;
    String saveInitSysConfig;

    @Before
    public void setUp() {
        new File("/tmp/" + auth).mkdir();
        new File("/tmp/" + auth + "_meta").mkdir();
        RaptureConfig.setLoadYaml(false);
        RaptureConfig config = ConfigLoader.getConf();
        
        saveRaptureRepo = config.RaptureRepo;
        saveInitSysConfig = config.InitSysConfig;

        config.RaptureRepo = REPO_USING_FILE;
        config.InitSysConfig = "NREP {} USING FILE { prefix=\"/tmp/" + auth + "/sys.config\"}";
        // config.DefaultPipelineTaskStatus = "TABLE {} USING FILE {prefix=\"/tmp/" + auth + "\"}";

        System.setProperty("LOGSTASH-ISENABLED", "false");

        Kernel.initBootstrap();
        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");
    }

    @After
    public void after() throws IOException {
        try {
            FileUtils.deleteDirectory(new File("/tmp/" + auth));
        } catch (Exception e) {
            // Unable to clean up
        }
        try {
            FileUtils.deleteDirectory(new File("/tmp/" + auth + "_meta"));
        } catch (Exception e) {
            // Unable to clean up  
        }
        try {
            FileUtils.deleteDirectory(new File("/tmp/" + auth + "_sheet"));
        } catch (Exception e) {
            // Unable to clean up
        }
        
        ConfigLoader.getConf().InitSysConfig = saveInitSysConfig;
        ConfigLoader.getConf().RaptureRepo = saveRaptureRepo;

    }

    @Test
    public void testThatWhichShouldNotBe() {
        try {
            IdGenFactory.getIdGen("IDGEN {} USING FILE { }").incrementIdGen(1L);
            fail("You can't create a repo without a prefix");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("prefix"));
        }

        try {
            IdGenFactory.getIdGen("IDGEN {} USING FILE { prefix = \" \" }").incrementIdGen(1L);
            fail("You can't create a repo without a valid prefix");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("prefix"));
        }
    }

    @Test
    public void testIdGen() {
        RaptureIdGen f = IdGenFactory.getIdGen("IDGEN { initial=\"143\", base=\"36\", length=\"8\", prefix=\"FOO\" } USING FILE { prefix=\"/tmp/"+auth+"\"}");
        String result = f.incrementIdGen(14500L);
        assertEquals("FOO00000BAR",result);
    }
}
