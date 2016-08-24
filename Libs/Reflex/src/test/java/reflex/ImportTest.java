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
package reflex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Map;

import org.antlr.runtime.RecognitionException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import rapture.common.BlobContainer;
import rapture.common.RaptureFolderInfo;
import rapture.common.api.ScriptJarApi;
import rapture.common.api.ScriptingApi;
import rapture.common.client.DummyScriptClient;
import reflex.handlers.TestScriptHandler;
import reflex.value.ReflexValue;

public class ImportTest extends ResourceBasedTest {
    @Test
    public void testStandalone() throws RecognitionException {
        String retVal = runTestForWithScriptHandler("/imports/import.rfx", new TestScriptHandler(this, "imports"));
        assertEquals("33", retVal.split("--RETURNS--")[1]);
        retVal = runTestForWithScriptHandler("/imports/returnNull.rfx", new TestScriptHandler(this, "imports"));
        assertEquals(ReflexValue.Internal.NULL.toString(), retVal.split("--RETURNS--")[1]);
        retVal = runTestForWithScriptHandler("/imports/import.rfx", new TestScriptHandler(this, "imports"));
        assertEquals("33", retVal.split("--RETURNS--")[1]);
    }

    @Test
    public void testImportFromJar() throws RecognitionException {
        ScriptingApi api = new DummyScriptClient() {
            @Override
            public ScriptJarApi getJar() {
                return new ScriptJarApi() {

                    @Override
                    public Boolean jarExists(String jarUri) {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public void putJar(String jarUri, byte[] jarContent) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public BlobContainer getJar(String jarUri) {
                        try {
                            byte[] ret;
                            if (jarUri.indexOf("Dynamic") != -1) {
                                ret = IOUtils.toByteArray(this.getClass().getResourceAsStream("/imports/DynamicModule-1.0.0.jar"));
                            } else {
                                ret = IOUtils.toByteArray(this.getClass().getResourceAsStream("/imports/gson-2.6.2.jar"));
                            }
                            BlobContainer b = new BlobContainer();
                            b.setContent(ret);
                            return b;
                        } catch (IOException e) {
                            fail("Failed to load test jars: " + e.getMessage());
                        }
                        return null;
                    }

                    @Override
                    public void deleteJar(String jarUri) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public Long getJarSize(String jarUri) {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public Map<String, String> getJarMetaData(String jarUri) {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public Map<String, RaptureFolderInfo> listJarsByUriPrefix(String uriPrefix, int depth) {
                        // TODO Auto-generated method stub
                        return null;
                    }

                };
            }
        };
        assertEquals("{\n" + "  \"data1\": 100,\n" + "  \"data2\": \"hello\",\n" + "  \"list\": [\n" + "    \"String 1\",\n" + "    \"String 2\",\n"
                + "    \"String 3\"\n" + "  ]\n" + "}", runTestForWithApi("/imports/import2.rfx", api, new ReflexScriptDataHandler(api), null));
    }
}
