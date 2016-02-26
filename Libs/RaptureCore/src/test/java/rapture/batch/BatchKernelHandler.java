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
package rapture.batch;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rapture.batch.kernel.ContextCommandExecutor;
import rapture.common.model.DocumentRepoConfig;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class BatchKernelHandler {
    @Before
    public void setup() {
        Kernel.initBootstrap();
    }

    private void testScript(String script) throws IOException {
        ScriptParser parser = new ScriptParser(new ContextCommandExecutor(ContextFactory.getKernelUser()));
        parser.parseScript(new StringReader(script), System.out);
    }

    @Test
    public void testScripts() throws IOException {
        testScript("$$\nDOCREPO\nauthority=testAuthority\nconfig=NREP USING MEMORY {}\n$$");
        List<DocumentRepoConfig> documentRepos = Kernel.getDoc().getDocRepoConfigs(ContextFactory.getKernelUser());
        for (DocumentRepoConfig dr : documentRepos) {
            if (dr.getAuthority().equals("testAuthority")) {
                return;
            }
        }
        assertTrue(false);
    }
}
