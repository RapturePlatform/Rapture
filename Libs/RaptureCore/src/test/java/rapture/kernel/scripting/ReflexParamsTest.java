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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.SeriesPoint;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class ReflexParamsTest {

    private static CallingContext ctx = ContextFactory.getKernelUser();

    private String auth = "reflexParamsTest";
    RaptureURI uri;

    @After
    public void cleanUp() {
        Kernel.getDoc().deleteDocRepo(ctx, uri.toAuthString());
    }

    @Before
    public void setup() {
        Kernel.initBootstrap();
        uri = new RaptureURI(auth, Scheme.SCRIPT);

        if (!Kernel.getDoc().docRepoExists(ctx, uri.toAuthString())) {
            Kernel.getDoc().createDocRepo(ctx, uri.toAuthString(), "NREP {} USING MEMORY {}");
        }
    }

    public String makeScript(String name, String script) {
        String scriptUri = RaptureURI.builder(uri).docPath(name).asString();
        
        if (Kernel.getScript().doesScriptExist(ctx, scriptUri)) {
            Kernel.getScript().deleteScript(ctx, scriptUri);
        }

        Kernel.getScript().createScript(ctx, scriptUri, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, script);
        return scriptUri;
    }

    @Test
    public void testParamsNotDefined() {
        String scriptUri = makeScript("testScript1", "if defined(_params) do \n" + "  println(\"params is defined \" + _params); return \"def\";\n"
                + "else do \n" + "  println(\"params is undefined\"); return \"notdef\";\n" + "end");
        String retval = Kernel.getScript().runScript(ctx, scriptUri, new HashMap<String, String>());
        assertEquals("notdef", retval);

        scriptUri = makeScript("rap4090a", "if (!defined(foo)) do \n return 'undefined'; end ");
        retval = Kernel.getScript().runScript(ctx, scriptUri, new HashMap<String, String>());
        assertEquals("undefined", retval);

        scriptUri = makeScript("rap4090b", "if (!defined(foo.bar)) do \n return 'undefined'; end ");
        retval = Kernel.getScript().runScript(ctx, scriptUri, new HashMap<String, String>());
        assertEquals("undefined", retval);

        scriptUri = makeScript("rap4090c", "foo = [];\n if (!defined(foo.bar)) do \n return 'undefined'; end \n return('fail');");
        retval = Kernel.getScript().runScript(ctx, scriptUri, new HashMap<String, String>());
        assertEquals("undefined", retval);

        scriptUri = makeScript("rap4090d", "foo = {};\n if (!defined(foo.bar)) do \n return 'undefined'; \n end \n return('fail');");
        retval = Kernel.getScript().runScript(ctx, scriptUri, new HashMap<String, String>());
        assertEquals("undefined", retval);

        scriptUri = makeScript("rap4090e", "foo = 'foo';\n if (!defined(foo.bar)) do \n return 'undefined'; end \n return('fail');");
        retval = Kernel.getScript().runScript(ctx, scriptUri, new HashMap<String, String>());
        assertEquals("undefined", retval);

        scriptUri = makeScript("rap4090f", "foo = {};\n if (!defined(foo.bar.baz)) do \n return 'undefined'; \n end \n return('fail');");
        retval = Kernel.getScript().runScript(ctx, scriptUri, new HashMap<String, String>());
        assertEquals("undefined", retval);

        scriptUri = makeScript("rap4090g", "if (!defined(foo['bar'])) do \n return 'undefined'; end \n return('fail');");
        retval = Kernel.getScript().runScript(ctx, scriptUri, new HashMap<String, String>());
        assertEquals("undefined", retval);

        scriptUri = makeScript("rap4090h", "foo = [];\n if (!defined(foo['bar'])) do \n return 'undefined'; end \n return('fail');");
        retval = Kernel.getScript().runScript(ctx, scriptUri, new HashMap<String, String>());
        assertEquals("undefined", retval);

        scriptUri = makeScript("rap4090i", "foo = {};\n if (!defined(foo['bar'])) do \n return 'undefined'; end \n return('fail');");
        retval = Kernel.getScript().runScript(ctx, scriptUri, new HashMap<String, String>());
        assertEquals("undefined", retval);

        scriptUri = makeScript("rap4090j", "foo = 'foo';\n if (!defined(foo['bar'])) do \n return 'undefined'; end \n return('fail');");
        retval = Kernel.getScript().runScript(ctx, scriptUri, new HashMap<String, String>());
        assertEquals("undefined", retval);
    }

    @Test
    public void testListsOfDoublesViaCasting() {
        final String script1 = "script://myscriptsReflexParamsTest/tx1";
        Kernel.getScript().putRawScript(ctx, script1,
                "today = date();\n" + "TODAY = cast(today, 'string');\n" + "seriesPrefix='reflexKeySpace.CfReflexTest';\n" + "keyspace='reflexCassKeySpace';\n"
                        + "colFamily='CassfReflexTest';\n" + "\n" + "CONFIG = 'SREP {} USING MEMORY {prefix=\"' + seriesPrefix + '\"}';\n"
                        + "SERIES_REPO_URI = 'series://myseriesTest';\n" + "\n" + "// Series Calls\n" + "\n"
                        + "if (!(#series.seriesRepoExists(SERIES_REPO_URI))) do\n" + "        #series.createSeriesRepo(SERIES_REPO_URI, CONFIG);\n" + "end\n"
                        + "\n" + "SERIES_URI = SERIES_REPO_URI + '/testReflexSeriesXYZ';\n" + "\n" + "// how many points you want\n" + "MAX = 10;\n" + "\n"
                        + "seriesKeys = [];\n" + "seriesValues = [];\n" + "\n" + "println ('Test add and retrieve doubles from series');\n"
                        + "for currPoint = 1 to MAX do\n" + "\n" + "        now = time();\n" + "        nowNum = cast(now, 'number');\n"
                        + "        nowString = cast(nowNum, 'string');\n" + "        valueInt = rand(50);\n" + "        valueNum = cast(valueInt, 'number');\n"
                        + "        valueString = cast(valueNum, 'string');\n" + "\n" + "        seriesKeys = seriesKeys + nowString;\n"
                        + "        seriesValues = seriesValues + valueNum;\n" + "\n" + "        sleep(100);\n" + "end\n" + "\n"
                        + "println ('keys: ' + seriesKeys);\n" + "println ('values: ' + seriesValues);\n" + "println ('seriesURI: ' + SERIES_URI);\n"
                        + "#series.addDoublesToSeries(SERIES_URI, seriesKeys, seriesValues);\n" + "\n" + "println('done');",
                RaptureScriptLanguage.REFLEX.toString(), RaptureScriptPurpose.PROGRAM.toString(), new ArrayList<String>(), new ArrayList<String>());
        Kernel.getScript().runScript(ctx, script1, new HashMap<String, String>());
        List<SeriesPoint> pts = Kernel.getSeries().getPoints(ctx, "series://myseriesTest/testReflexSeriesXYZ");
        assertEquals(10, pts.size());
    }

    @Test
    public void testSizeWithMap() {
        String scriptUri = makeScript("testScript1", 
                "foo.w = 'x'; \n foo['y'] = 'z'; \n bar=[ 'a', 'b', 'c'];\nprintln(foo.size()); \n println(bar.size()); \n println(size(foo)); \n println(size(bar)); \n return size(foo) + size(bar);\n");
        String retval = Kernel.getScript().runScript(ctx, scriptUri, new HashMap<String, String>());
        assertEquals("5", retval);
    }

    @Test
    public void testCheckVsRun() {
        String scriptUri = makeScript("testScript1",
                "foo.w = 'x'; \n foo['y'] = 'z'; \n bar=[ 'a', 'b', 'c'];\nprintln(foo.size()); \n println(bar.size()); \n println(size(foo)); \n println(size(bar)); \n error;  \n return size(foo) + size(bar);\n");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));

        String runval = "";
        try {
            runval = Kernel.getScript().runScript(ctx, scriptUri, new HashMap<String, String>());
        } catch (Exception e) {
        }

        String checkval = "";
        try {
            checkval = Kernel.getScript().checkScript(ctx, scriptUri);
        } catch (Exception e) {
        }
        assertTrue(err.toString().startsWith(checkval));

        System.setOut(null);
        System.setErr(null);

    }

}
