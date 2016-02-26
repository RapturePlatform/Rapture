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
package rapture.kernel.scripting;

import rapture.common.CallingContext;
import rapture.common.RaptureScript;
import rapture.common.RaptureScriptLanguage;
import rapture.config.ConfigLoader;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.script.IRaptureScript;
import rapture.script.ScriptFactory;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests use of Rapture Reflex
 *
 * @author amkimian
 */
public class ReflexSupportTest {

    @Before
    public void setUp() throws Exception {

        Kernel.INSTANCE.restart();
        Kernel.initBootstrap();
        CallingContext context = ContextFactory.getKernelUser();
        String systemBlobRepo = "//sys.blob";
        if (Kernel.getBlob().blobRepoExists(context, systemBlobRepo)) {
            Kernel.getBlob().deleteBlobRepo(context, systemBlobRepo);
        }
        Kernel.getBlob().createBlobRepo(context, systemBlobRepo, ConfigLoader.getConf().DefaultSystemBlobConfig,
                ConfigLoader.getConf().DefaultSystemBlobFoldersConfig);

    }

    @Test
    public void testSimpleProgram() {
        ScriptFactory.init();
        RaptureScript script = new RaptureScript();
        script.setLanguage(RaptureScriptLanguage.REFLEX);
        script.setName("//test/1");
        script.setScript("println('hello');println(_params['hello']);return 5;\n");
        IRaptureScript scr = ScriptFactory.getScript(script);
        Map<String, Object> extra = new HashMap<String, Object>();
        extra.put("hello", "world");
        String res = scr.runProgram(ContextFactory.getKernelUser(), null, script, extra);
        System.out.println(res);
    }
}
