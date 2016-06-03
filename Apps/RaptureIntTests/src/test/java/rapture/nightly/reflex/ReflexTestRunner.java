package rapture.nightly.reflex;

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

import com.google.common.collect.Lists;

import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.client.HttpScriptApi;
import rapture.common.exception.ExceptionToString;
import rapture.nightly.IntegrationTestHelper;

public class ReflexTestRunner {
    String raptureUrl = null;
    private HttpScriptApi scriptApi = null;
    private RaptureURI scriptRepo = null;
    private List<String> scriptList = new ArrayList<String>();
    
    IntegrationTestHelper helper;

    @BeforeClass(groups = { "script", "mongo", "nightly" })
    @Parameters({"RaptureURL","RaptureUser","RapturePassword"})
    public void beforeTest(@Optional("http://localhost:8665/rapture")String url, @Optional("rapture")String user, @Optional("rapture")String password)  {
        helper = new IntegrationTestHelper(url, user, password);
        scriptApi = helper.getScriptApi();
        scriptRepo = helper.getRandomAuthority(Scheme.SCRIPT);
        loadScripts();
    }
    
    // Checks all scripts for syntax and then attempts to run
    @Test(groups = { "script", "nightly", "search" }, dataProvider = "allScripts")
    public void runAllScripts (String scriptName) {
        Assert.assertEquals(0,scriptApi.checkScript(scriptName).length(),"Found error in script "+scriptName);
        Reporter.log("Running script: " +scriptName,true);
        Map <String, String> paramMap=new HashMap<String,String>();

        RaptureURI blobRepo = helper.getRandomAuthority(Scheme.BLOB);
        helper.configureTestRepo(blobRepo, "MEMORY");
        paramMap.put("blobRepoUri", blobRepo.toString());

        RaptureURI docRepo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(docRepo, "MEMORY");
        paramMap.put("docRepoUri", docRepo.toString());

        RaptureURI seriesRepo = helper.getRandomAuthority(Scheme.SERIES);
        helper.configureTestRepo(seriesRepo, "MEMORY");
        paramMap.put("seriesRepoUri", seriesRepo.toString());

        paramMap.put("scriptRepoUri", scriptRepo.toString());
        try {
            scriptApi.runScript(scriptName, paramMap);
        } catch (Exception e) {
            Reporter.log(e.getMessage());
            Assert.fail("Failed running script: " + scriptName + " : " + e.getMessage());
        } finally {
            helper.cleanAllAssets();
        }
    }

    // Checks all non search scripts for syntax and then attempts to run
    @Test(groups = { "script", "nightly" }, dataProvider = "nonSearchScripts")
    public void runNonSearchScripts (String scriptName) {
        Assert.assertEquals(0,scriptApi.checkScript(scriptName).length(),"Found error in script "+scriptName);
        Reporter.log("Running script: " +scriptName,true);
        Map <String, String> paramMap=new HashMap<String,String>();
        paramMap.put("repoURI", scriptRepo.toString());
        try {
            scriptApi.runScript(scriptName, paramMap);
        } catch (Exception e) {
            Assert.fail("Failed running script: " + scriptName + "\n" + ExceptionToString.format(e));
        }
    }
    
    // Read in all reflex scripts in all subdirs of ($HOME)/bin/reflex/nightly and creates scripts in Rapture
    private void loadScripts () {
        String rootPath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator
                + "reflex" + File.separator + "nightly";
        File[] files = new File(rootPath).listFiles();
        if (files == null) {
            Reporter.log("No tests found in directory " + rootPath, true);
            return;
        }
        for (File subdir : files) {
            String subdirName=subdir.getName();
            for (File scriptFile : subdir.listFiles()) {
                try {
                    String scriptName = scriptFile.getName();
                    String scriptPath = RaptureURI.builder(scriptRepo).docPath(subdirName + "/" + scriptName).asString();
                    Reporter.log("Reading in file: " + scriptFile.getAbsolutePath(), true);
                    if (!scriptApi.doesScriptExist(scriptPath)) {
                        byte[] scriptBytes = Files.readAllBytes(scriptFile.toPath());
                        scriptApi.createScript(scriptPath, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, new String(scriptBytes));
                    }
                    scriptList.add(scriptPath);
                } catch (IOException e) {
                    Assert.fail("Failed loading script: " + scriptFile.getAbsolutePath() + "\n" + ExceptionToString.format(e));
                }
            }
        }
    }
    
    // Returns all scripts
    @DataProvider
    public Object[][] allScripts() {
        List<Object[]> result = Lists.newArrayList();
        for (String s : scriptList) {
            Object [] o = new Object[1];
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
                Object [] o = new Object[1];
                o[0] = s;
                result.add(o);
            }
        return result.toArray(new Object[result.size()][]);
    }
    
    
    @AfterClass
    public void cleanUp () {
        for (String scriptPath:scriptList) {
            scriptApi.deleteScript(scriptPath);
        } 
    }
}
