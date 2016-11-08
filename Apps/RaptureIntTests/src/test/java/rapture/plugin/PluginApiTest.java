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
package rapture.plugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import rapture.common.BlobContainer;
import rapture.common.PluginConfig;
import rapture.common.PluginTransportItem;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.client.HttpBlobApi;
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

    HttpPluginApi pluginApi = null;
    Set<String> installedSet = null;
    IntegrationTestHelper helper = null;

    @BeforeClass(groups = { "plugin", "nightly" })
    @Parameters({ "RaptureURL", "RaptureUser", "RapturePassword" })
    public void setUp(@Optional("http://localhost:8665/rapture") String url, @Optional("rapture") String username, @Optional("rapture") String password) {

        helper = new IntegrationTestHelper(url, username, password);
        pluginApi = helper.getPluginApi();
        installedSet = new HashSet<String>();
    }
    
    @Test(groups={"plugin","nightly"})
    public void testInstallStructuredPlugin () {
    	String pluginName="teststruct";
    	String description="Test structured plugin";
    	String zipFileName="teststructcreate.zip";
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
        Assert.assertEquals(helper.getStructApi().describeTable("structured://structtest/testtable"),ImmutableMap.<String, String>builder()
        	    .put("address","text")
        	    .put("name", "text")
        	    .put("company", "text")
        	    .put("id", "integer")
        	    .put("email", "text")
        	    .put("verification", "text")
        	    .put("username", "text")
        	    .build());
        

    	zipFileName="teststructalter.zip";
        Reporter.log("Testing plugin: " + pluginName,true);
        //import the zip configuration
        zipAbsFilePath = System.getProperty("user.dir")+ File.separator+"build"+File.separator+"resources"+File.separator+"test"+File.separator+"plugin"+File.separator+"nightly"+File.separator+zipFileName;
        
        Reporter.log("Reading in file from "+zipAbsFilePath,true);

        try {
        	orgZipFile=	new ZipFile(zipAbsFilePath);
            Assert.assertNotNull(orgZipFile, pluginName);
        } catch (Exception e) {
            Reporter.log("Got error reading zip file " + zipAbsFilePath, true);
            Reporter.log(ExceptionToString.format(e), true);
            Assert.fail("Got error reading zip file " + zipAbsFilePath);
        }
        
        pluginConfig = getPluginConfigFromZip(zipAbsFilePath);
        Assert.assertNotNull(pluginConfig, pluginName);

        //check plugin zip configuration
        Assert.assertEquals(pluginConfig.getPlugin(),pluginName);
        Assert.assertEquals(pluginConfig.getDescription(),description);

        //import (to memory) using plugin sandbox
        sandbox = new PluginSandbox();
        sandbox.setConfig(pluginConfig);
        sandbox.setStrict(false);
        
        rootDir = File.separator + "tmp" + File.separator+ "plugin1_" + System.currentTimeMillis();
        Reporter.log("Test for " + zipFileName + ". Dir is " + rootDir,true);
        //add the individual items to sandbox
        sandbox.setRootDir(new File(rootDir, pluginConfig.getPlugin()));
        entries = orgZipFile.entries();
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
        payload = Maps.newHashMap();
        itemSet=new HashSet<String>();
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
        Assert.assertEquals(helper.getStructApi().describeTable("structured://structtest/testtable"),ImmutableMap.<String, String>builder()
        	    .put("address","text")
        	    .put("name", "text")
        	    .put("company", "text")
        	    .put("id", "integer")
        	    .put("email", "text")
        	    .put("verification", "text")
        	    .put("username", "text")
        	    .put("bar", "text")
        	    .put("foo", "text")
        	    .build());
    
        pluginApi.uninstallPlugin(pluginName);
        installedSet.remove(pluginName);
        boolean installed=false;
        for (PluginConfig c :pluginApi.getInstalledPlugins())
        	if (c.getPlugin().compareTo(pluginName) ==0)
        		installed=true;

        Assert.assertFalse(installed,"Plugin did not uninstall");


    }
    @Test(groups = { "plugin", "nightly" }, dataProvider = "pluginZips")
    public void testInstallAndUninstallPlugin(String pluginName, String zipFileName, String description) {

        Reporter.log("Testing plugin: " + pluginName, true);
        // import the zip configuration
        String zipAbsFilePath = System.getProperty("user.dir") + File.separator + "build" + File.separator + "resources" + File.separator + "test"
                + File.separator + "plugin" + File.separator + "nightly" + File.separator + zipFileName;

        Reporter.log("Reading in file from " + zipAbsFilePath, true);

        ZipFile orgZipFile = null;
        try {
            orgZipFile = new ZipFile(zipAbsFilePath);
            Assert.assertNotNull(orgZipFile, pluginName);
        } catch (Exception e) {
            Reporter.log("Got error reading zip file " + zipAbsFilePath, true);
            Reporter.log(ExceptionToString.format(e), true);
            Assert.fail("Got error reading zip file " + zipAbsFilePath);
        }

        PluginConfig pluginConfig = getPluginConfigFromZip(zipAbsFilePath);
        Assert.assertNotNull(pluginConfig, pluginName);

        // check plugin zip configuration
        Assert.assertEquals(pluginConfig.getPlugin(), pluginName);
        Assert.assertEquals(pluginConfig.getDescription(), description);

        // import (to memory) using plugin sandbox
        PluginSandbox sandbox = new PluginSandbox();
        sandbox.setConfig(pluginConfig);
        sandbox.setStrict(false);

        String rootDir = File.separator + "tmp" + File.separator + "plugin1_" + System.currentTimeMillis();
        Reporter.log("Test for " + zipFileName + ". Dir is " + rootDir, true);
        // add the individual items to sandbox
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
                Reporter.log("Error making sandbox item", true);
            }
        }

        Assert.assertEquals(sandbox.getPluginName(), pluginName);
        Assert.assertEquals(sandbox.getDescription(), pluginConfig.getDescription());

        // get ready to install plugin
        Map<String, PluginTransportItem> payload = Maps.newHashMap();
        Set<String> itemSet = new HashSet<String>();
        for (PluginSandboxItem item : sandbox.getItems(null)) {
            try {
                PluginTransportItem payloadItem = item.makeTransportItem();
                payload.put(item.getURI().toString(), payloadItem);

                itemSet.add(item.getURI().toString().contains("$") ? item.getURI().toString().substring(0, item.getURI().toString().indexOf("$") - 1)
                        : item.getURI().toString());
            } catch (Exception ex) {
                Reporter.log("Exception creating plugin " + ex.getMessage(), true);
            }
        }
        // install the plugin using the http api
        pluginApi.installPlugin(sandbox.makeManifest(null), payload);
        installedSet.add(pluginName);
        PluginConfig thePlugin = null;
        for (PluginConfig c : pluginApi.getInstalledPlugins())
            if (c.getPlugin().compareTo(pluginName) == 0) thePlugin = c;
        Assert.assertEquals(thePlugin.getPlugin(), pluginName, "Plugin " + pluginName + " has unexpected name.");
        Assert.assertEquals(thePlugin.getDescription(), description, "Plugin " + pluginName + " has unexpected description.");
        Assert.assertEquals(pluginApi.verifyPlugin(pluginName).keySet(), itemSet, "Problem with installed items");

        pluginApi.uninstallPlugin(pluginName);

        boolean installed = false;
        for (PluginConfig c : pluginApi.getInstalledPlugins())
            if (c.getPlugin().compareTo(pluginName) == 0) installed = true;

        Assert.assertFalse(installed, "Plugin " + pluginName + " expected to be uninstalled");
        installedSet.remove(pluginName);
    }

    @Test(groups = { "plugin", "nightly" }, dataProvider = "workflowPluginZips", description = "install a plugin and run work order")
    public void testPluginWithWorkorder(String zipFileName, String pluginName, String expectedDescription, String workflowUri, String ctxString)
            throws IOException, NoSuchAlgorithmException, InterruptedException, SecurityException, NoSuchMethodException {
        Reporter.log("Testing plugin: " + pluginName, true);
        // import the zip configuration
        String zipAbsFilePath = System.getProperty("user.dir") + File.separator + "build" + File.separator + "resources" + File.separator + "test"
                + File.separator + "plugin" + File.separator + "nightly" + File.separator + zipFileName;

        Reporter.log("Reading in file from " + zipAbsFilePath, true);

        ZipFile orgZipFile = new ZipFile(zipAbsFilePath);

        PluginConfig pluginConfig = getPluginConfigFromZip(zipAbsFilePath);

        // check plugin zip configuration
        Assert.assertNotNull(pluginConfig, pluginName);
        Assert.assertEquals(pluginConfig.getPlugin(), pluginName);
        Assert.assertEquals(pluginConfig.getDescription(), expectedDescription);

        // import (to memory) using plugin sandbox
        PluginSandbox sandbox = new PluginSandbox();
        sandbox.setConfig(pluginConfig);
        sandbox.setStrict(false);

        String rootDir = File.separator + "tmp" + File.separator + "plugin1_" + System.currentTimeMillis();
        Reporter.log("Test for " + zipFileName + ". Dir is " + rootDir, true);
        // add the individual items to sandbox
        sandbox.setRootDir(new File(rootDir, pluginConfig.getPlugin()));
        Enumeration<? extends ZipEntry> entries = orgZipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            sandbox.makeItemFromZipEntry(orgZipFile, entry);
        }

        Assert.assertEquals(sandbox.getPluginName(), pluginName);
        Assert.assertEquals(sandbox.getDescription(), pluginConfig.getDescription());

        // get ready to install plugin
        Map<String, PluginTransportItem> payload = Maps.newHashMap();
        for (PluginSandboxItem item : sandbox.getItems(null)) {
            try {
                PluginTransportItem payloadItem = item.makeTransportItem();
                payload.put(item.getURI().toString(), payloadItem);
            } catch (Exception ex) {
                Reporter.log("Exception creating plugin " + ex.getMessage(), true);
            }
        }
        // install the plugin using the http api
        pluginApi.installPlugin(sandbox.makeManifest(null), payload);
        installedSet.add(pluginName);
        HttpDecisionApi decisionApi = new HttpDecisionApi(helper.getRaptureLogin());

        try {
            Thread.sleep(5000);
        } catch (Exception e) {
        }
        Map<String, Object> ctxMap1 = JacksonUtil.getMapFromJson(ctxString);
        Map<String, String> ctxMap2 = new HashMap<String, String>();
        for (String currKey : ctxMap1.keySet())
            ctxMap2.put(currKey, ctxMap1.get(currKey).toString());
        String workOrderURI = decisionApi.createWorkOrder(workflowUri, ctxMap2);
        Reporter.log("Creating work order " + workOrderURI + " with context map " + ctxMap2.toString(), true);

        int numRetries = 0;
        long waitTimeMS = 5000;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi, workOrderURI) && numRetries < 20) {
            Reporter.log("Checking workorder status, retry count=" + numRetries + ", waiting " + (waitTimeMS / 1000) + " seconds...", true);
            try {
                Thread.sleep(waitTimeMS);
            } catch (Exception e) {
            }
            numRetries++;
        }
        Reporter.log("Checking results for work order " + workOrderURI, true);
        Assert.assertEquals(decisionApi.getWorkOrderStatus(workOrderURI).getStatus(), WorkOrderExecutionState.FINISHED);
        String woResult = decisionApi.getContextValue(workOrderURI + "#0", "TEST_RESULTS");
        if (woResult != null) Assert.assertTrue(Boolean.parseBoolean(woResult));

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
                Reporter.log("No plugin.txt present in root level of zipfile: " + filename, true);
            }
        } catch (IOException e) {
            Reporter.log("Got IOException: " + e.getMessage(), true);
        }
        return null;
    }

    @Test(groups = { "plugin", "nightly" })
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
        Map<String, String> verify = pluginApi.verifyPlugin(pluginName);
        Assert.assertEquals(verify.size(), 2);
        for (String key : verify.keySet()) {
            Assert.assertEquals(verify.get(key), "Verified", key + " was not verified");
        }

        RaptureURI blobRepo = helper.getRandomAuthority(Scheme.BLOB);
        helper.configureTestRepo(blobRepo, "MONGODB", true);
        String blobUri = RaptureURI.builder(blobRepo).docPath(pluginName).asString();
        String plug = pluginApi.exportPlugin(pluginName, blobUri);
        Map<String, Object> map = JacksonUtil.getMapFromJson(plug);
        Assert.assertEquals(map.get("objectCount"), new Integer(2));

        BlobContainer blob = helper.getBlobApi().getBlob(blobUri);
        ByteArrayInputStream bais = new ByteArrayInputStream(blob.getContent());
        ZipInputStream zis = new ZipInputStream(bais);
        Assert.assertEquals(zis.getNextEntry().getName(), "plugin.txt");

        byte[] contents = new byte[256];

        ZipEntry ze = zis.getNextEntry();
        zis.read(contents, 0, 255);
        map = JacksonUtil.getMapFromJson(new String(contents));
        Assert.assertEquals(ze.getName(), "content/foo/bar.table");
        Assert.assertEquals("foo", map.get("authority").toString());
        Assert.assertEquals("bar", map.get("name").toString());
        Assert.assertEquals("TABLE {} USING MONGO { prefix=\"foo\"}", map.get("config").toString());

        ze = zis.getNextEntry();
        zis.read(contents, 0, 255);
        map = JacksonUtil.getMapFromJson(new String(contents));
        Assert.assertEquals(ze.getName(), "content/baz/.index");
        Assert.assertEquals("baz", map.get("name").toString());
        Assert.assertEquals("field1($1) number", map.get("config").toString());

        pluginApi.uninstallPlugin(pluginName);
    }

    @Test(groups = { "plugin", "nightly" })
    public void testVerifyPlugin() throws IOException {

        RaptureURI docRepo = helper.getRandomAuthority(Scheme.DOCUMENT);
        HttpDocApi docApi = helper.getDocApi();
        HttpBlobApi blobApi = helper.getBlobApi();
        helper.configureTestRepo(docRepo, "MEMORY", true);
        String doc1 = RaptureURI.builder(docRepo).docPath("foo/doc1").asString();
        String doc2 = RaptureURI.builder(docRepo).docPath("bar/doc2").asString();
        String doc3 = RaptureURI.builder(docRepo).docPath("baz/doc3").asString();
        docApi.putDoc(doc1, "{ \"val\" : \"foo\" }");
        docApi.putDoc(doc2, "{ \"val\" : \"bar\" }");
        docApi.putDoc(doc3, "{ \"val\" : \"baz\" }");

        String pluginName = "Jeff" + System.currentTimeMillis();
        pluginApi.createManifest(pluginName);
        pluginApi.addManifestItem(pluginName, docRepo.toString());
        pluginApi.addManifestItem(pluginName, doc1);
        pluginApi.addManifestItem(pluginName, doc2);
        pluginApi.addManifestItem(pluginName, doc3);

        RaptureURI blobRepo = helper.getRandomAuthority(Scheme.BLOB);
        helper.configureTestRepo(blobRepo, "MONGODB", true);
        String blobUri = RaptureURI.builder(blobRepo).docPath(pluginName).asString();
        String metadata = pluginApi.exportPlugin(pluginName, blobUri);
        Map<String, Object> meta = JacksonUtil.getMapFromJson(metadata);
        BlobContainer blob = blobApi.getBlob(blobUri);
        Assert.assertEquals(blob.getContent().length, ((Integer) meta.get("fileSize")).intValue());

        try (FileOutputStream fos = new FileOutputStream(new File("/tmp/foo.zip"))) {
            fos.write(blob.getContent());
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(blob.getContent());
        ZipInputStream zis = new ZipInputStream(bais);
        Assert.assertEquals(zis.getNextEntry().getName(), "plugin.txt");
        Assert.assertEquals(zis.getNextEntry().getName(), docRepo.toString().replace("document:/", "content") + ".rdoc");
        Assert.assertEquals(zis.getNextEntry().getName(), doc1.replace("document:/", "content") + ".rdoc");
        Assert.assertEquals(zis.getNextEntry().getName(), doc2.replace("document:/", "content") + ".rdoc");
        Assert.assertEquals(zis.getNextEntry().getName(), doc3.replace("document:/", "content") + ".rdoc");

        Map<String, String> verify = pluginApi.verifyPlugin(pluginName);
        Assert.assertNotNull(verify);
    }

    @AfterClass(groups = { "plugin", "nightly" })
    public void AfterTest() {
    	helper.cleanAllAssets();
        // delete all plugins installed during test
        for (String p : installedSet)
            pluginApi.uninstallPlugin(p);
    }

    @DataProvider(name = "workflowPluginZips")
    public Object[][] workflowPluginZips() {
        return new Object[][] {
                { "testseries.zip", "testseries", "Test workflow with series", "workflow://testplugin/testseries/createsquaresandverify",
                        "{\"SERIES_SIZE\":\"40\"}" },
                { "testblob.zip", "testblob", "Test workflow with blobs", "workflow://testplugin/testblob/createblobandverify", "{\"BLOB_SIZE\":\"40\"}" },
                { "testdoc.zip", "testdoc", "Test workflow with docs", "workflow://testplugin/testdoc/createdocsandverify", "{\"DOC_SIZE\":\"40\"}" }, };
    }

    @DataProvider(name = "pluginZips")
    public Object[][] pluginZips() {
        return new Object[][] { { "testdoc", "testdoc.zip", "Test workflow with docs" },
                { "testdocplugin", "testdocplugin.zip", "Test document plugin with data" },
                { "testplugin", "testplugin.zip", "Test plugin with different types" }, };
    }

}
