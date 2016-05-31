package rapture.httpapi.script;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
import rapture.common.client.HttpLoginApi;
import rapture.common.client.HttpScriptApi;
import rapture.common.client.SimpleCredentialsProvider;
import rapture.common.exception.RaptureException;

public class ReflexTestRunner {
    String raptureUrl = null;
    private String raptureUser = null;
    private String rapturePass = null;
    private HttpLoginApi raptureLogin = null;
    private HttpScriptApi scriptApi=null;
    private String scriptPrefix="";
    private String testRepoName="";
    private List<String> scriptList= new ArrayList<String>();
    
    @BeforeClass(groups={"blob","mongo", "smoke"})
    @Parameters({"RaptureURL","RaptureUser","RapturePassword"})
    public void beforeTest(@Optional("http://localhost:8665/rapture")String url, @Optional("rapture")String user, @Optional("rapture")String password)  {
        raptureUrl=url;
        raptureUser=user;
        rapturePass=password;
        raptureLogin = new HttpLoginApi(raptureUrl, new SimpleCredentialsProvider(raptureUser, rapturePass));
        scriptPrefix="testNightly"+System.nanoTime();
        testRepoName="//testNightlyRepo"+System.nanoTime();
        try {
            raptureLogin.login();
            scriptApi = new HttpScriptApi(raptureLogin);

        } catch (RaptureException e) {
            e.printStackTrace();
        }   
        loadScripts();
    }
    
    // Checks all scripts for syntax and then attempts to run
    @Test (groups ={"script", "smoke","search"},dataProvider = "allScripts")
    public void runAllScripts (String scriptName) {
        Assert.assertEquals(0,scriptApi.checkScript(scriptName).length(),"Found error in script "+scriptName);
        Reporter.log("Running script: " +scriptName,true);
        Map <String, String> paramMap=new HashMap<String,String>();
        paramMap.put("repoURI", testRepoName);
        try {
            scriptApi.runScript(scriptName, paramMap);
        } catch (Exception e) {
            Assert.fail("Failed running script: "+scriptName);
        }
    }

    // Checks all non search scripts for syntax and then attempts to run
    @Test (groups ={"script", "smoke"},dataProvider = "nonSearchScripts")
    public void runNonSearchScripts (String scriptName) {
        Assert.assertEquals(0,scriptApi.checkScript(scriptName).length(),"Found error in script "+scriptName);
        Reporter.log("Running script: " +scriptName,true);
        Map <String, String> paramMap=new HashMap<String,String>();
        paramMap.put("repoURI", testRepoName);
        try {
            scriptApi.runScript(scriptName, paramMap);
        } catch (Exception e) {
            Assert.fail("Failed running script: "+scriptName);
        }
    }
    
    // Read in all reflex scripts in all subdirs of ($HOME)/bin/reflex/smoke and creates scripts in Rapture
    private void loadScripts () {
        String rootPath = System.getProperty("user.dir")+ File.separator+"bin"+File.separator+"reflex"+File.separator+"smoke";
        File dir = new File(rootPath);
        for (File subdir : dir.listFiles()) {
            String subdirName=subdir.getName();
            for (File scriptFile : subdir.listFiles()) {
                String scriptName = scriptFile.getName();
                String scriptPath = "script://"+scriptPrefix+ "/"+subdirName + "/" +scriptName;
                String scriptText="";
                Reporter.log("Reading in file: " +scriptFile.getAbsolutePath(),true);
                try {
                    BufferedReader bufferReader = new BufferedReader( new FileReader(scriptFile.getAbsolutePath()));
                    String line;
                    while ((line = bufferReader.readLine()) != null)   {
                        scriptText= scriptText + line;
                    }
                    bufferReader.close();
                 }catch(Exception e){
                     Reporter.log("Error while reading file: " + e.getMessage(),true);                      
                 }
                Reporter.log("Loading script: " +scriptPath,true); 
                scriptApi.createScript(scriptPath, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, scriptText);
                scriptList.add(scriptPath);              
                
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
