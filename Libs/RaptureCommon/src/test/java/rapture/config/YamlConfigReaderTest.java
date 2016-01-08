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
package rapture.config;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

public class YamlConfigReaderTest {
    private static final Logger log = Logger.getLogger(YamlConfigReaderTest.class);

    @Test
    public void testYamlGood1() throws IOException {

        URL sourceURL = getClass().getClassLoader().getResource("rapture/config/source.yaml");
        URL overridesURL = getClass().getClassLoader().getResource("rapture/config/overrides.yaml");
        File sourceFile = new File(sourceURL.getFile());
        File overridesFile = new File(overridesURL.getFile());
        String sourceString = FileUtils.readFileToString(sourceFile);
        String overridesString = FileUtils.readFileToString(overridesFile);

        String overridenString = YamlConfigReader.overlayYaml(sourceString, overridesString);
        log.info(overridenString);

        @SuppressWarnings("unchecked")
        Map<String, String> results = new Yaml().loadAs(overridenString, Map.class);
        assertEquals(4, results.size());
        assertEquals("overriden", results.get("RaptureRepo"));
        assertEquals("webserverz", results.get("ServerTypez"));
        assertEquals("webserver", results.get("ServerType"));
        assertEquals("runners", results.get("Categories"));
    }

    @Test
    public void testYamlGood2() throws IOException {
        YamlConfigReader reader = new YamlConfigReader("yamlTest.cfg");
        RaptureConfig config = reader.findAndRead();
        
        assertEquals("overridenRepo", config.RaptureRepo);
        assertEquals("overridenServer", config.ServerType);
        assertEquals("originalCat", config.Categories);
    }

    @Test
    public void testYaml2() throws IOException {
        assertNoOverrides(null);
    }

    private void assertNoOverrides(String overridesString) throws IOException {
        URL sourceURL = getClass().getClassLoader().getResource("rapture/config/source.yaml");
        File sourceFile = new File(sourceURL.getFile());
        String sourceString = FileUtils.readFileToString(sourceFile);

        String overridenString = YamlConfigReader.overlayYaml(sourceString, overridesString);
        log.info(overridenString);

        @SuppressWarnings("unchecked")
        Map<String, String> results = new Yaml().loadAs(overridenString, Map.class);
        assertEquals(3, results.size());
        assertEquals("REP {} USING MONGODB { prefix=\"rapture.bootstrap\" }", results.get("RaptureRepo"));
        assertEquals("webserver", results.get("ServerType"));
        assertEquals("runners", results.get("Categories"));
    }

    @Test
    public void testYaml3() throws IOException {
        assertNoOverrides("");
    }

    @Test
    public void testYaml4() throws IOException {
        assertNoOverrides("this is not yaml");
    }

}
