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
package rapture.reflex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.client.HttpAdminApi;
import rapture.common.client.HttpScriptApi;
import rapture.common.exception.ExceptionToString;
import rapture.helper.IntegrationTestHelper;

public class ReflexTestRunner {
    String raptureUrl = null;
    private HttpScriptApi scriptApi = null;

    private List<String> scriptList = new ArrayList<>();
    IntegrationTestHelper helper;

    @BeforeClass(groups = { "script", "mongo", "nightly" })
    @Parameters({ "RaptureURL", "RaptureUser", "RapturePassword" })
    public void beforeTest(@Optional("http://localhost:8665/rapture") String url, @Optional("rapture") String user, @Optional("rapture") String password) {
        helper = new IntegrationTestHelper(url, user, password);
        scriptApi = helper.getScriptApi();
        loadScripts(helper.getRandomAuthority(Scheme.SCRIPT));

        HttpAdminApi admin = helper.getAdminApi();
    }

    Map<String, String> getParams() {
        Map<String, String> paramMap = new HashMap<>();

        RaptureURI blobRepo = helper.getRandomAuthority(Scheme.BLOB);
        helper.configureTestRepo(blobRepo, "MEMORY");
        paramMap.put("blobRepoUri", blobRepo.toString());

        RaptureURI docRepo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(docRepo, "MEMORY");
        paramMap.put("docRepoUri", docRepo.toString());

        RaptureURI seriesRepo = helper.getRandomAuthority(Scheme.SERIES);
        helper.configureTestRepo(seriesRepo, "MEMORY");
        paramMap.put("seriesRepoUri", seriesRepo.toString());

        RaptureURI scriptRepo = helper.getRandomAuthority(Scheme.SCRIPT);
        paramMap.put("scriptRepoUri", scriptRepo.toString());

        RaptureURI structuredRepo = helper.getRandomAuthority(Scheme.STRUCTURED);
        paramMap.put("structuredRepoUri", structuredRepo.toString());

        paramMap.put("user", helper.getUser());

        return paramMap;
    }

    // Checks all scripts for syntax and then attempts to run
    @Test(groups = { "script", "nightly", "search" }, dataProvider = "allScripts")
    public void runAllScripts(String scriptName) {
        String checkStr = scriptApi.checkScript(scriptName);
        Assert.assertEquals(0, checkStr.length(), "Found error in script " + scriptName + ": " + checkStr);
        Reporter.log("Running script: " + scriptName, true);
        String scriptResult = "";
        try {
            scriptResult = scriptApi.runScript(scriptName, getParams());
        } catch (Exception e) {
            Assert.fail("Failed running script: " + scriptName + "\n" + ExceptionToString.format(e));
        }
        Assert.assertTrue(Boolean.parseBoolean(scriptResult), "Script result was not true for " + scriptName);

    }

    // Checks all non search scripts for syntax and then attempts to run
    @Test(groups = { "script", "nightly", "nosearch" }, dataProvider = "nonSearchScripts")
    public void runNonSearchScripts(String scriptName) {
        Assert.assertEquals(0, scriptApi.checkScript(scriptName).length(), "Found error in script " + scriptName);
        Reporter.log("Running script: " + scriptName, true);
        String scriptResult = "";
        try {
            scriptResult = scriptApi.runScript(scriptName, getParams());
        } catch (Exception e) {
            Assert.fail("Failed running script: " + scriptName + "\n" + ExceptionToString.format(e));
        }
        Assert.assertTrue(Boolean.parseBoolean(scriptResult), "Script result was not true for " + scriptName);
    }

    List<File> recursiveList(File file) {
        if (file.isDirectory()) {
            List<File> ret = new ArrayList<>();
            File[] list = file.listFiles();
            if (list != null) {
                for (File f : list) {
                    ret.addAll(recursiveList(f));
                }
            }
            return ret;
        }
        return (file.exists() && file.getName().endsWith("rfx")) ? ImmutableList.of(file) : ImmutableList.of();
    }

    // DO NOT CHECK THIS IN WITH A VALUE OTHER THAN NULL. IT IS FOR DEBUGGING PURPOSES ONLY
    String testOnly = null;

    // Read in all reflex scripts in all subdirs of ($HOME)/bin/reflex/nightly and creates scripts in Rapture
    private void loadScripts(RaptureURI tempScripts) {
        String rootPath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator
                + "reflex" + File.separator + "nightly";
        List<File> files = recursiveList(new File(rootPath));
        if (files.isEmpty()) {
            Reporter.log("No tests found in directory " + rootPath, true);
            return;
        }
        for (File file : files) {
            if ((testOnly == null) || file.getName().contains(testOnly)) try {
                String scriptName = file.getName();
                String subdirName = file.getParent().substring(file.getParent().lastIndexOf('/') + 1);
                String scriptPath = RaptureURI.builder(tempScripts).docPath(subdirName + "/" + scriptName).asString();
                Reporter.log("Reading in file: " + file.getAbsolutePath(), true);

                if (!scriptApi.doesScriptExist(scriptPath)) {
                    byte[] scriptBytes = Files.readAllBytes(file.toPath());
                    scriptApi.createScript(scriptPath, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, new String(scriptBytes));
                }
                scriptList.add(scriptPath);
            } catch (IOException e) {
                Assert.fail("Failed loading script: " + file.getAbsolutePath() + "\n" + ExceptionToString.format(e));
            }
        }
    }

    // Returns all scripts
    @DataProvider
    public Object[][] allScripts() {
        List<Object[]> result = Lists.newArrayList();
        for (String s : scriptList) {
            Object[] o = new Object[1];
            o[0] = s;
            result.add(o);
        }
        return result.toArray(new Object[result.size()][]);
    }

    // Returns all scripts except ones for search
    @DataProvider
    public Object[][] nonSearchScripts() {
        List<Object[]> result = Lists.newArrayList();
        for (String s : scriptList)
            if (!s.contains("search")) {
                Object[] o = new Object[1];
                o[0] = s;
                result.add(o);
            }
        return result.toArray(new Object[result.size()][]);
    }

    @AfterClass
    public void cleanUp() {
        helper.cleanAllAssets();
    }
}
