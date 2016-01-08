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
package rapture.batch;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Map;

import org.junit.Test;

public class BatchScriptTester {
    @Test
    public void runSimpleTest() throws IOException {
        String script = "$$\nCOMMAND\none=1\ntwo=2\n$$";
        testScript(script);
    }

    @Test
    public void testBadParam() throws IOException {
        testScript("$$\nCOMMAND\nfred\n$$");
    }

    @Test
    public void testLongParam() throws IOException {
        testScript("$$\nCOMMAND\none=1\ntwo=\nxxx\nA\nB\nC\nxxx\n$$");
    }

    @Test
    public void testMalformedScript1() throws IOException {
        testScript("\n\n\n$$$$\n$$\nCOMMAND\n$$");
    }

    @Test
    public void testMultiCommand() throws IOException {
        testScript("$$\nCOMM1\n$$\nCOMM2\n$$");
    }

    @Test
    public void testNoEndToParam() throws IOException {
        testScript("$$\nCOMMAND\ntwo=\nxxx\n$$");
    }

    @Test
    public void testNullScript() throws IOException {
        testScript("");
    }

    private void testScript(String script) throws IOException {
        ScriptParser parser = new ScriptParser(new IBatchExecutor() {

            @Override
            public void runGeneralCommand(String commandName, Map<String, String> params, OutputStream output) {
                System.out.println("Run command " + commandName + " with params " + params.toString());
            }

        });
        parser.parseScript(new StringReader(script), System.out);
    }
}
