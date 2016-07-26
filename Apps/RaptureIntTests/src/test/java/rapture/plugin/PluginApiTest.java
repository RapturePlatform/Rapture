package rapture.plugin;

import static org.junit.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.common.collect.Maps;

import rapture.common.PluginConfig;
import rapture.common.PluginTransportItem;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.client.HttpDecisionApi;
import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpIndexApi;
import rapture.common.client.HttpPluginApi;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.helper.IntegrationTestHelper;
import rapture.plugin.install.PluginContentReader;
import rapture.plugin.install.PluginSandbox;
import rapture.plugin.install.PluginSandboxItem;


public class PluginApiTest {

    HttpPluginApi pluginApi=null;
    Set <String> installedSet = null;
    IntegrationTestHelper helper=null;
    
    @BeforeClass(groups={"plugin","nightly"})
    @Parameters({"RaptureURL","RaptureUser","RapturePassword"})
    public void setUp(@Optional("http://localhost:8665/rapture")String url, 
                      @Optional("rapture")String username, @Optional("rapture")String password ) {

    	helper = new IntegrationTestHelper(url, username, password);
        pluginApi = helper.getPluginApi();
        installedSet = new HashSet<String> ();
    }
    
    @Test(groups={"plugin","nightly"})
    public void testInstallAndUninstallPlugin () {
    	String pluginName ="testdoc";
    	String zipFileName ="testdoc.zip";
    	String description="Test workflow with docs";
        Reporter.log("Testing plugin: " + pluginName,true);
        //import the zip configuration
        String zipAbsFilePath = System.getProperty("user.dir")+ File.separator+"build"+File.separator+"resources"+File.separator+"test"+File.separator+"plugin"+File.separator+"nightly"+File.separator+zipFileName;
        
        Reporter.log("Reading in file from "+zipAbsFilePath,true);
        
        ZipFile orgZipFile = null;
        try {
        	orgZipFile=	new ZipFile(zipAbsFilePath);
            Assert.assertNotNull(orgZipFile, pluginName);
        } catch (Exception e) {
            Reporter.log("Got error reading zip file " + zipAbsFilePath, true);
            Reporter.log(ExceptionToString.format(e), true);
            Assert.fail("Got error reading zip file " + zipAbsFilePath);
        }
        
        PluginConfig pluginConfig = getPluginConfigFromZip(zipAbsFilePath);
        Assert.assertNotNull(pluginConfig, pluginName);

        //check plugin zip configuration
        Assert.assertEquals(pluginConfig.getPlugin(),pluginName);
        Assert.assertEquals(pluginConfig.getDescription(),description);

        //import (to memory) using plugin sandbox
        PluginSandbox sandbox = new PluginSandbox();
        sandbox.setConfig(pluginConfig);
        sandbox.setStrict(false);
        
        String rootDir = File.separator + "tmp" + File.separator+ "plugin1_" + System.currentTimeMillis();
        Reporter.log("Test for " + zipFileName + ". Dir is " + rootDir,true);
        //add the individual items to sandbox
        sandbox.setRootDir(new File(rootDir, pluginConfig.getPlugin()));
        Enumeration<? extends ZipEntry> entries = orgZipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            try {
            	sandbox.makeItemFromZipEntry(orgZipFile, entry);
            } catch (Exception e) {
            	Reporter.log("Error making sandbox item",true);
            }
        }
        
        Assert.assertEquals(sandbox.getPluginName(),pluginName);
        Assert.assertEquals(sandbox.getDescription(),pluginConfig.getDescription());
       
        //get ready to install plugin
        Map<String, PluginTransportItem> payload = Maps.newHashMap();
        Set <String> itemSet=new HashSet<String>();
        for (PluginSandboxItem item : sandbox.getItems(null)) {
            try {
                PluginTransportItem payloadItem = item.makeTransportItem();
                payload.put(item.getURI().toString(), payloadItem);
                
                itemSet.add(item.getURI().toString());
            } catch (Exception ex) {
                Reporter.log("Exception creating plugin " +ex.getMessage(),true);
            }
        }
        //install the plugin using the http api 
        pluginApi.installPlugin(sandbox.makeManifest(null), payload);
        installedSet.add(pluginName);
        PluginConfig thePlugin=null;
        for (PluginConfig c :pluginApi.getInstalledPlugins())
        	if (c.getPlugin().compareTo(pluginName) ==0)
        		thePlugin=c;
        Assert.assertEquals (thePlugin.getPlugin(),pluginName,"Plugin "+pluginName+" has unexpected name.");
        Assert.assertEquals (thePlugin.getDescription(),description,"Plugin "+pluginName+" has unexpected description.");
        Assert.assertEquals(pluginApi.verifyPlugin(pluginName).keySet(),itemSet,"Problem with installed items");
        
        pluginApi.uninstallPlugin(pluginName);
        
        boolean installed=false;
        for (PluginConfig c :pluginApi.getInstalledPlugins())
        	if (c.getPlugin().compareTo(pluginName) ==0)
        		installed=true;

        Assert.assertFalse(installed,"Plugin "+pluginName + " expected to be uninstalled");
        installedSet.remove(pluginName);
    }
    
    @Test(groups={"plugin","nightly"}, dataProvider="pluginData",description="install a plugin and run work order")
    public void testPluginWithWorkorder(String zipFileName, String pluginName, String expectedDescription, String workflowUri, String ctxString) throws IOException, NoSuchAlgorithmException, InterruptedException, SecurityException, NoSuchMethodException{
        Reporter.log("Testing plugin: " + pluginName,true);
        //import the zip configuration
        String zipAbsFilePath = System.getProperty("user.dir")+ File.separator+"build"+File.separator+"resources"+File.separator+"test"+File.separator+"plugin"+File.separator+"nightly"+File.separator+zipFileName;
        
        Reporter.log("Reading in file from "+zipAbsFilePath,true);
        
        ZipFile orgZipFile = new ZipFile(zipAbsFilePath);
        
        PluginConfig pluginConfig = getPluginConfigFromZip(zipAbsFilePath);
        
        //check plugin zip configuration
        Assert.assertNotNull(pluginConfig, pluginName);
        Assert.assertEquals(pluginConfig.getPlugin(),pluginName);
        Assert.assertEquals(pluginConfig.getDescription(),expectedDescription);

        //import (to memory) using plugin sandbox
        PluginSandbox sandbox = new PluginSandbox();
        sandbox.setConfig(pluginConfig);
        sandbox.setStrict(false);
        
        String rootDir = File.separator + "tmp" + File.separator+ "plugin1_" + System.currentTimeMillis();
        Reporter.log("Test for " + zipFileName + ". Dir is " + rootDir,true);
        //add the individual items to sandbox
        sandbox.setRootDir(new File(rootDir, pluginConfig.getPlugin()));
        Enumeration<? extends ZipEntry> entries = orgZipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            sandbox.makeItemFromZipEntry(orgZipFile, entry);
        }
        
        Assert.assertEquals(sandbox.getPluginName(),pluginName);
        Assert.assertEquals(sandbox.getDescription(),pluginConfig.getDescription());
       
        //get ready to install plugin
        Map<String, PluginTransportItem> payload = Maps.newHashMap();
        for (PluginSandboxItem item : sandbox.getItems(null)) {
            try {
                PluginTransportItem payloadItem = item.makeTransportItem();
                payload.put(item.getURI().toString(), payloadItem);
            } catch (Exception ex) {
                Reporter.log("Exception creating plugin " +ex.getMessage(),true);
            }
        }
        //install the plugin using the http api 
        pluginApi.installPlugin(sandbox.makeManifest(null), payload);
        installedSet.add(pluginName);
        HttpDecisionApi decisionApi = new HttpDecisionApi(helper.getRaptureLogin());
        
        try {
            Thread.sleep(5000);
        } catch (Exception e) {}
        Map <String,Object> ctxMap1=JacksonUtil.getMapFromJson(ctxString);
        Map <String,String> ctxMap2= new HashMap<String,String>();
        for (String currKey : ctxMap1.keySet())
            ctxMap2.put(currKey, ctxMap1.get(currKey).toString());
        String workOrderURI = decisionApi.createWorkOrder(workflowUri,ctxMap2);
        Reporter.log("Creating work order "+workOrderURI + " with context map "+ctxMap2.toString(),true);
   
        int numRetries=0;
        long waitTimeMS=5000;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi,workOrderURI)  && numRetries < 20) {
            Reporter.log("Checking workorder status, retry count="+numRetries+", waiting "+(waitTimeMS/1000)+" seconds...",true);
            try {
                Thread.sleep(waitTimeMS);
            }
            catch (Exception e) {}
            numRetries++;
        }
        Reporter.log("Checking results for work order "+workOrderURI,true);
        Assert.assertEquals(decisionApi.getWorkOrderStatus(workOrderURI).getStatus(),WorkOrderExecutionState.FINISHED);
        String woResult=decisionApi.getContextValue(workOrderURI+"#0", "TEST_RESULTS");
        if (woResult !=null)
            Assert.assertTrue (Boolean.parseBoolean(woResult));
        
    }

    private byte[] getContentFromEntry(ZipFile zip, ZipEntry entry) throws IOException {
        return PluginContentReader.readFromStream(zip.getInputStream(entry));
    }
    
    private PluginConfig getPluginConfigFromZip(String filename) {
        File zip = new File(filename);
        if (!zip.exists() || !zip.canRead()) {
            return null;
        }
        try (ZipFile zipFile = new ZipFile(zip)) {
            ZipEntry configEntry = zipFile.getEntry(PluginSandbox.PLUGIN_TXT);
            
            if (PluginSandbox.PLUGIN_TXT.equals(configEntry.getName())) {                
                try {
                    return JacksonUtil.objectFromJson(getContentFromEntry(zipFile, configEntry), PluginConfig.class);
                } catch (Exception ex) {
                    throw RaptureExceptionFactory.create("pluing.txt manifest corrupt in zip file " + filename);
                }
            } else {
                Reporter.log("No plugin.txt present in root level of zipfile: " + filename,true);
            }
        } catch (IOException e) {
            Reporter.log("Got IOException: "+e.getMessage(),true);
        }
        return null;
    }
    
    @Test
    public void testGetPluginItem() throws IOException {
        String tableUri = "table://foo/bar";
        String indexUri = "index://baz";

        HttpIndexApi index = helper.getIndexApi();
        index.createTable(tableUri, "TABLE {} USING MONGO { prefix=\"foo\"}");
        index.createIndex(indexUri, "field1($1) number");

        String pluginName = "Jeff" + System.currentTimeMillis();

        pluginApi.createManifest(pluginName);
        pluginApi.addManifestItem(pluginName, tableUri);
        pluginApi.addManifestItem(pluginName, indexUri);
        pluginApi.verifyPlugin(pluginName);
        String plug = pluginApi.exportPlugin(pluginName, "/tmp/" + pluginName);
        Map<String, Object> map = JacksonUtil.getMapFromJson(plug);
        ZipFile zip = new ZipFile("/tmp/" + pluginName + "/" + map.get("filePath").toString());

        Enumeration<? extends ZipEntry> ennui = zip.entries();
        while (ennui.hasMoreElements()) {
            ZipEntry ze = ennui.nextElement();
            String name = ze.getName();
            Map<String, Object> contents = null;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(zip.getInputStream(ze)))) {
                contents = JacksonUtil.getMapFromJson(br.lines().collect(Collectors.joining()));
            }
            assertNotNull(contents);

            switch (name) {
            case "plugin.txt":
                assertEquals(pluginName, contents.get("plugin").toString());
                break;
            case "content/foo/bar.table":
                assertEquals("foo", contents.get("authority").toString());
                assertEquals("bar", contents.get("name").toString());
                assertEquals("TABLE {} USING MONGO { prefix=\"foo\"}", contents.get("config").toString());
                break;
            case "content/baz/.index":
                assertEquals("baz", contents.get("name").toString());
                assertEquals("field1($1) number", contents.get("config").toString());
                break;
            default:
                Assert.fail("Unexpected file " + name + " in plugin zip ");
            }
        }

        pluginApi.uninstallPlugin(pluginName);
    }
    
    @Test
    public void testVerifyPlugin() throws IOException {

        RaptureURI docRepo = RaptureURI.builder(Scheme.DOCUMENT, "foo" + System.currentTimeMillis()).build();

        HttpDocApi docApi = helper.getDocApi();
        docApi.createDocRepo(docRepo.toString(), "NREP {} USING MEMORY { prefix=\"foo\"}");
        String doc1 = RaptureURI.builder(docRepo).docPath("foo/doc1").asString();
        String doc2 = RaptureURI.builder(docRepo).docPath("bar/doc2").asString();
        String doc3 = RaptureURI.builder(docRepo).docPath("baz/doc3").asString();
        docApi.putDoc(doc1, "{ \"val\" : \"foo\" }");
        docApi.putDoc(doc2, "{ \"val\" : \"bar\" }");
        docApi.putDoc(doc3, "{ \"val\" : \"baz\" }");

        String pluginName = "Jeff" + System.currentTimeMillis();
        pluginApi.createManifest(pluginName);
        pluginApi.addManifestItem(pluginName, doc1);
        pluginApi.addManifestItem(pluginName, doc2);
        pluginApi.addManifestItem(pluginName, doc3);
        pluginApi.addManifestItem(pluginName, docRepo.toString());
        pluginApi.exportPlugin(pluginName, "/tmp/" + pluginName + ".zip");

        Map<String, String> verify = pluginApi.verifyPlugin(pluginName);
        assertNotNull(verify);
    }

    @AfterClass(groups={"plugin", "nightly"})
    public void AfterTest(){
        //delete all plugins installed during test
    	for (String p :installedSet)
        	pluginApi.uninstallPlugin(p);
    }
    
    
    @DataProvider(name = "pluginData")
    public Object[][] pluginZips() {
        return new Object[][] { 
                {"testseries.zip","testseries","Test workflow with series","workflow://testplugin/testseries/createsquaresandverify","{\"SERIES_SIZE\":\"40\"}"},
                {"testblob.zip","testblob","Test workflow with blobs","workflow://testplugin/testblob/createblobandverify","{\"BLOB_SIZE\":\"40\"}"},
                {"testdoc.zip","testdoc","Test workflow with docs","workflow://testplugin/testdoc/createdocsandverify","{\"DOC_SIZE\":\"40\"}"},
        };
    }

}
