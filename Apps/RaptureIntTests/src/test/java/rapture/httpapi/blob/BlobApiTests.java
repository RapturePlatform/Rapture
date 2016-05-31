package rapture.httpapi.blob;

import static rapture.common.Scheme.BLOB;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import rapture.common.RaptureURI;
import rapture.common.client.HttpBlobApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.SimpleCredentialsProvider;
import rapture.common.exception.RaptureException;
import rapture.common.model.BlobRepoConfig;
import rapture.util.ResourceLoader;

public class BlobApiTests {
    String raptureUrl = null;
    private String raptureUser = null;
    private String rapturePass = null;
    private HttpLoginApi raptureLogin = null;
    private HttpBlobApi blobApi = null;
    String repoConfigTemplate = "BLOB {} USING %s {grid=\"%s\"}";
    String metaConfigTemplate = "REP {} USING %s {prefix=\"%s.meta\"}";
    String mongoRepoConfigTemplate="BLOB {} USING MONGODB {%s=\"%s\"}";
    String mongoRepoMetaConfigTemplate="REP {} USING MONGODB {prefix=\"%s\"}";
    /** 
     * Creates an instance of HttpAdmin API that will be used in test methods
     * */
    @BeforeClass(groups={"blob","mongo", "smoke"})
    @Parameters({"RaptureURL","RaptureUser","RapturePassword"})
    public void beforeTest(@Optional("http://localhost:8665/rapture")String url, @Optional("rapture")String user, @Optional("rapture")String password)  {
        raptureUrl=url;
        raptureUser=user;
        rapturePass=password;
        raptureLogin = new HttpLoginApi(raptureUrl, new SimpleCredentialsProvider(raptureUser, rapturePass));

        try {
            raptureLogin.login();
            blobApi = new HttpBlobApi(raptureLogin);

        } catch (RaptureException e) {
            e.printStackTrace();
        }   
    }
    
    
    /** 
     * Creates blog using parameter input pointing to a test file. Stores and retrieves blob data and verifies. Delete afterwards
     * @throws IOException 
     * */
    @Test(groups ={"blob", "smoke","mongo"},description="Test basic operations on blob: creation, fetch content, get size, get meta data, and deletion",
          dataProvider = "blobFileScenarios", enabled=true)
    public void testBlobFromFile(String fileName, String contentType) throws IOException {  

        String authorityName = "test.blob" + Thread.currentThread().getId();
        String blobRepoUri=RaptureURI.builder(BLOB, authorityName).build().toString();
        Reporter.log("Created "+blobRepoUri, true);
        if(!blobApi.blobRepoExists(blobRepoUri)){
            String blobConfig = String.format(repoConfigTemplate, "MONGODB", authorityName);
            String metaConfig = String.format(metaConfigTemplate, "MONGODB", authorityName);
            Reporter.log("Creating "+blobRepoUri + " with config "+metaConfig,true);
            try {
                blobApi.createBlobRepo(blobRepoUri, blobConfig,metaConfig);
            }
            catch (Exception e) {
                Assert.fail();
            }
        }
        String blobUri = blobRepoUri + "testblob" + System.nanoTime();
        byte[] bytes = null;
        
        if(contentType == "application/pdf"){
            InputStream resourceAsStream = BlobApiTests.class.getResourceAsStream(fileName);
            bytes = IOUtils.toByteArray(resourceAsStream);       
            blobApi.putBlob(blobUri, bytes, contentType);
        
        } else {
            String resourceAsString = ResourceLoader.getResourceAsString(this,fileName);
            bytes = resourceAsString.getBytes();
            blobApi.putBlob(blobUri,bytes, contentType);
            Assert.assertEquals(new String (blobApi.getBlob(blobUri).getContent()),new String (bytes));
        }
        
        Assert.assertEquals(blobApi.getBlobSize(blobUri),new Long(bytes.length));        
        Map <String,String> metaDataMap = blobApi.getBlobMetaData( blobUri);
        Assert.assertTrue (metaDataMap.containsKey("Content-Type"));
        Assert.assertEquals (metaDataMap.get("Content-Length"),(new Long(bytes.length)).toString());
        Assert.assertTrue (metaDataMap.containsKey("createdTimestamp"));
        Assert.assertTrue (metaDataMap.containsKey("writeTime"));
        Assert.assertTrue (metaDataMap.containsKey("modifiedTimestamp"));
    }
    
    @Test (groups={"blob","mongo", "smoke"},enabled=true, expectedExceptions=RaptureException.class,dataProvider = "configTypes")
    public void testNullBlobContents(String dbType, String configType) {
       
        String blobAuthority="test.blobauthority";
        String metaAuthority="metatest.authority";

        String blobURI = RaptureURI.builder(BLOB, blobAuthority+System.nanoTime()).asString();
        String CONFIG = String.format(mongoRepoConfigTemplate, configType, blobAuthority);
        String CONFIGMETA = String.format(mongoRepoMetaConfigTemplate,metaAuthority);
        Reporter.log("Creating "+blobURI + " with config "+CONFIG,true);
        try {
            blobApi.createBlobRepo(blobURI,CONFIG,CONFIGMETA);
        }
        catch (Exception e) {
            Assert.fail();
        }
               
        blobApi.putBlob(blobURI,null, "application/text");
        
    }
    
    @Test (groups={"blob","mongo", "smoke"},dataProvider = "configTypes")
    public void testBlobAppend (String dbType,String configType){
        String blobAuthority="test.blobauthority";
        String metaAuthority="meta.blobauthority";
        long maxContentSize=10000L;
        
        Random rand = new Random();

        String blobURI=RaptureURI.builder(BLOB, blobAuthority+System.nanoTime()).asString();
       

        String CONFIG = String.format(mongoRepoConfigTemplate, configType, blobAuthority);
        String CONFIGMETA = String.format(mongoRepoMetaConfigTemplate,metaAuthority);
        Reporter.log("Creating repo "+blobURI + " with config "+CONFIG,true);
        try {
            blobApi.createBlobRepo(blobURI,CONFIG,CONFIGMETA);
        }
        catch (Exception e) {
            Assert.fail();
        }
              
        long content_size=Math.abs(rand.nextLong() % maxContentSize);
        String currBlobURI=blobURI + Thread.currentThread().getId() + "_" + content_size + "_" + System.nanoTime();
        Reporter.log("Creating URI "+currBlobURI + " with content size= "+content_size,true);
        String currContent="INITIAL CONTENT";
        Reporter.log("Storing and appending to blob: " + currBlobURI,true);
        try{
            blobApi.putBlob(currBlobURI,currContent.getBytes(), "application/text"); 
        } catch (Exception e) {
            Reporter.log("Exception thrown: " + e,true);
        }
        
        blobApi.addBlobContent(currBlobURI, "MORE CONTENT".getBytes());
        Assert.assertEquals (new String (blobApi.getBlob(currBlobURI).getContent()),currContent+"MORE CONTENT");
        
        blobApi.addBlobContent(currBlobURI, "EVEN MORE CONTENT".getBytes());
        Assert.assertEquals (new String (blobApi.getBlob(currBlobURI).getContent()),currContent+"MORE CONTENT"+"EVEN MORE CONTENT");
    }
    
    
    @Test (groups={"blob","mongo", "smoke"},dataProvider = "configTypes")
    public void testBlobDelete (String dbType,String configType){
        String blobAuthority="test.blobexistauthority";
        String metaAuthority="meta.blobexistauthority";

        String currBlobAuthority=blobAuthority+System.nanoTime();
        String blobURI=RaptureURI.builder(BLOB, currBlobAuthority).asString();
        String CONFIG = String.format(mongoRepoConfigTemplate, configType, blobAuthority);
        String CONFIGMETA = String.format(mongoRepoMetaConfigTemplate,metaAuthority);
        Reporter.log("Creating repo "+blobURI + " with config "+CONFIG,true);
        try {
            blobApi.createBlobRepo(blobURI, CONFIG,CONFIGMETA);
        }
        catch (Exception e) {
            Assert.fail();
        }
        
        Assert.assertEquals(blobApi.getBlobRepoConfig(blobURI).getAuthority(),currBlobAuthority);
        Assert.assertEquals(blobApi.getBlobRepoConfig(blobURI).getConfig(),CONFIG);
        Assert.assertEquals(blobApi.getBlobRepoConfig(blobURI).getMetaConfig(),CONFIGMETA);
        
        // test that blob does not exists and is null before putting content
        String currBlobURI=blobURI + Thread.currentThread().getId() + "_delete_" + System.nanoTime();
        Assert.assertFalse(blobApi.blobExists(currBlobURI));
        Assert.assertNull(blobApi.getBlob(currBlobURI));
        
        // Check that deleting non-exisitng blob returns false
        try {
            blobApi.deleteBlob(currBlobURI); 
        } catch (Exception e) {
            Reporter.log("Exception thrown: "+ e,true);

        }
        
        // test that put then delete content nullifies blob and makes it not exist
        blobApi.putBlob(currBlobURI,"TEST".getBytes(), "application/text");
        blobApi.deleteBlob(currBlobURI);
        Assert.assertFalse(blobApi.blobExists(currBlobURI));
        Assert.assertNull(blobApi.getBlob(currBlobURI));
        
        // test that put then appened then delete content nullifies blob and makes it not exist
        blobApi.putBlob(currBlobURI,"TEST".getBytes(), "application/text");
        blobApi.addBlobContent(currBlobURI, "MORE CONTENT".getBytes());
        blobApi.deleteBlob(currBlobURI);
        Assert.assertFalse(blobApi.blobExists(currBlobURI));
        Assert.assertNull(blobApi.getBlob(currBlobURI));
        
    }
    
    @Test(groups={"blob","mongo", "smoke"},description="overwrite an application/text blob with an application/text blob of same size.",
            dataProvider = "blobOverwriteScenarios",enabled=true)
    public void overwriteExistingTextBlobTest(int originalContentSize, int newContentSize ){

        String authorityName = "test.blob" + System.nanoTime();

        String repoURI=RaptureURI.builder(BLOB, authorityName).build().toString();
        String blobConfig = String.format(repoConfigTemplate, "MONGODB", authorityName);
        String metaConfig = String.format(metaConfigTemplate, "MONGODB", authorityName);
        Reporter.log("Creating repo "+repoURI + " with config "+blobConfig,true);

        try {
            blobApi.createBlobRepo(repoURI, blobConfig,metaConfig);
        }
        catch (Exception e) {
            Assert.fail();
        }
        
        //write original blob to blob store
        String orgContent="";
        for (long j=0;j<originalContentSize;j++){
            orgContent=orgContent+"a";
        }
        //write the blob
        String blobURI=repoURI+"/testoverwrite";
        try {
            blobApi.putBlob(blobURI,orgContent.getBytes(), "application/text");
        } catch (Exception e) {
            Reporter.log("Exception thrown: " +e,true);

        }
        
        //get the blob from store 
        String retrievedOrgContent = new String (blobApi.getBlob(blobURI).getContent());
        Reporter.log("Original blob contents: " + retrievedOrgContent,true);

        Assert.assertEquals(retrievedOrgContent,orgContent, "Compare retrieved blob data to original blob data written to same repo.");
        
        //overwrite the original blob with a new one
        String newContent="";
        for (long j=0;j<newContentSize;j++){
            newContent=newContent+"b";
        }
        Reporter.log("Overwriting original blob with: " + newContent,true);

        try {
            blobApi.putBlob(blobURI,newContent.getBytes(), "application/text");
        } catch (Exception e) {
            Reporter.log("Exception thrown: " +e ,true);

        }
        String retrievedNewContent = new String (blobApi.getBlob(blobURI).getContent());
        Reporter.log("Overwritten blob contents: " + retrievedNewContent,true);

        Assert.assertEquals(retrievedNewContent,newContent, "Blob should be overwritten by newContent bx100");
    }
    
    @Test(groups={"blob","mongo", "smoke"},description="overwrite an application/pdf blob with a different blob type.",enabled=true)
    public void overwriteExistingPDFBlobWithTextBlobTest() throws FileNotFoundException{

        String authorityName = "test.blob" + System.nanoTime();
        String repoURI=RaptureURI.builder(BLOB, authorityName).build().toString();

        String blobConfig = String.format(repoConfigTemplate, "MONGODB", authorityName);
        String metaConfig = String.format(metaConfigTemplate, "MONGODB", authorityName);
        Reporter.log("Creating repo "+repoURI + " with config "+blobConfig,true);
                
        try {
            blobApi.createBlobRepo(repoURI, blobConfig,metaConfig);
        }
        catch (Exception e) {
            Assert.fail();
        }
        
        //load file1 and store in blob store
        String path = getFilePath(this, "/blob/small-pdf-file.pdf");
        Reporter.log("Loading pdf: " + path,true);
        byte[] putOrgData = getFileAsBytes(path);
        String blobURI=repoURI+"/over_write_test";
        
        try {
            blobApi.putBlob(blobURI,putOrgData, "application/pdf");
        } catch (Exception e) {
            Reporter.log("Exception thrown: " +e,true);
        }
        
        byte[] retrievedOrgData = blobApi.getBlob(blobURI).getContent();
        Assert.assertEquals(retrievedOrgData,putOrgData, "Compare retrieved blob data to original blob data written to same repo.");
        
        //load a test file2 and store in same blob store
        String putNewData = ResourceLoader.getResourceAsString(this, "/blob/simple_blob_test.txt");
        
        try {
            blobApi.putBlob(blobURI,putNewData.getBytes(), "application/text");
        } catch (Exception e) {
            Reporter.log("Exception thrown: " +e,true);
        }
        
        byte[] retrievedNewData = blobApi.getBlob(blobURI).getContent();
        
        Assert.assertEquals(retrievedNewData,putNewData.getBytes(), "Compare retrieved blob data to original blob data written to same repo.");
        
    }
    
    
    @Test (groups={"blob","mongo", "smoke"},enabled=true,dataProvider = "configTypes")
    public void testBlobRepositoryCreation(String dbType,String configType) {
        String blobAuthority="test.blobauthority";
        String metaAuthority="metatest.authority";

        String currRepo = RaptureURI.builder(BLOB, blobAuthority+System.nanoTime()).asString();
        String CONFIG = String.format(mongoRepoConfigTemplate, configType, blobAuthority);
        String CONFIGMETA = String.format(mongoRepoMetaConfigTemplate,metaAuthority);
        Reporter.log("Creating "+currRepo + " with config "+CONFIG,true);

        try {
            blobApi.createBlobRepo(currRepo, CONFIG,CONFIGMETA);
        }
        catch (Exception e) {
            Assert.fail();
        }
        try {
            blobApi.deleteBlobRepo(currRepo);
        } catch (Exception e) {
            Reporter.log("Exception thrown: " +e,true);
        }
        
        Assert.assertFalse(blobApi.blobRepoExists(currRepo));
               
    }
    
    private static String getFilePath (Object context, String resourcePath){
        
        String realPath = context.getClass().getResource(resourcePath).getPath();

        return new File(realPath).getPath();
    }
    
    private static byte[] getFileAsBytes(String filePath){
        byte[] returnVal = null;
        FileInputStream fis = null;
                               
        try {
            fis = new FileInputStream(filePath); 
            returnVal = IOUtils.toByteArray(fis);
        } catch (Exception e) {
            throw new RuntimeException("We had problems with " + filePath + " --- " + e);
        } 
        
        return returnVal;
}
    
    @AfterClass(groups={"blob","mongo", "smoke"})
    public void AfterTest(){
        //delete all repos
        List<BlobRepoConfig> blobRepositories = blobApi.getBlobRepoConfigs();
        
        for(BlobRepoConfig repo:blobRepositories ){
            Reporter.log("Blob repo: " + repo.getAuthority(), true);
            if(repo.getAuthority().contains("test.blob") ){
                String uriToDelete = repo.getAuthority();
                Reporter.log("**** Deleting blob repo: " + uriToDelete,true);
                blobApi.deleteBlobRepo(uriToDelete);
                Reporter.log("**** Deleted blob repo: " + uriToDelete,true);
            }
        }
        
    }
    
    @DataProvider
    public Object[][] blobFileScenarios() {
        return new Object[][] {
                new Object[] {File.separator+"blob"+File.separator+"simple_blob_test.txt", "text/plain"},
                new Object[] {File.separator+"blob"+File.separator+"small-pdf-file.pdf", "application/pdf"},
                new Object[] {File.separator+"blob"+File.separator+"small_csv_file.csv", "text/csv"},
                new Object[] {File.separator+"blob"+File.separator+"small-jpg-file.jpg", "image/jpeg"},
        };
    }
    
    @DataProvider
    public Object[][] blobOverwriteScenarios() {
        return new Object[][] {
                new Object[] {100,100},
                new Object[] {50, 100},
                new Object[] {100, 50},
                new Object[] {50, 1},
                new Object[] {1, 50},
        };
    }
    
    @DataProvider
    public Object[][] configTypes() {
        return new Object[][] {
                new Object[] {"MONGODB","prefix"},
                new Object[] {"MONGODB","grid"},
        };
    } 
}
