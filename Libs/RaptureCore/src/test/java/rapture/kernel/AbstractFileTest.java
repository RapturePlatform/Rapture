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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import rapture.common.CallingContext;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;

public class AbstractFileTest {
    
    static String saveInitSysConfig;
    static String saveRaptureRepo;
    static final String auth = "test" + System.currentTimeMillis();
    static List<File> files = new ArrayList<>();
    static String[] suffixes = new String[] { "", "_meta", "_attribute", "-2d1", "-2d1_meta", "-2d1_attribute", "_blob", "_sheet", "_sheetmeta", "_series"} ;
    static RaptureConfig config;
    static CallingContext callingContext;

    @BeforeClass
    static public void setUp() {
        for ( String s : suffixes) {
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

    public CallingContext getCallingContext() {
        return callingContext;
    }

    @AfterClass
    static public void after() throws IOException {
        for (File temp : files) 
            try {
                if (temp.isDirectory()) 
                    FileUtils.deleteDirectory(temp);
            } catch (Exception e) {
                Logger.getLogger(AbstractFileTest.class).warn("Cannot delete "+temp);
                // Unable to clean up properly
            }
        ConfigLoader.getConf().InitSysConfig = saveInitSysConfig;
        ConfigLoader.getConf().RaptureRepo = saveRaptureRepo;
    }

    public AbstractFileTest() {
        super();
    }

}
