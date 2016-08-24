package rapture.jar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.client.HttpDecisionApi;
import rapture.common.client.HttpJarApi;
import rapture.common.dp.SemaphoreType;
import rapture.common.dp.Step;
import rapture.common.dp.Workflow;
import rapture.helper.IntegrationTestHelper;


public class JarApiTest {
	
	IntegrationTestHelper helper=null;
	HttpJarApi jarApi = null;
	List<String> workflowList = null;
	List<String> jarList = null;
	
    @BeforeClass(groups={"jar","nightly"})
    @Parameters({"RaptureURL","RaptureUser","RapturePassword"})
    public void setUp(@Optional("http://localhost:8665/rapture")String url, 
                      @Optional("rapture")String username, @Optional("rapture")String password ) {

    	helper = new IntegrationTestHelper(url, username, password);
    	jarApi= new HttpJarApi (helper.getRaptureLogin());
    	workflowList=new ArrayList<String> ();
    	jarList=new ArrayList<String> ();
        
    }
    
    @Test (groups = {"jar","nightly"})
    public void testPutJar () {

        String jarUri="jar://test/jar"+System.nanoTime();
        byte [] jarContent = "TEST_CONTENT".getBytes();
        jarApi.putJar(jarUri, jarContent);
        Reporter.log("Verifying "+jarUri + " after putJar",true);
        Assert.assertTrue (jarApi.jarExists(jarUri));
        Assert.assertEquals (jarApi.getJarSize(jarUri).longValue(),jarContent.length);
        Assert.assertEquals (jarApi.getJar(jarUri).getContent(),jarContent);
        Assert.assertEquals(jarApi.getJarMetaData(jarUri).get("Content-Type"),"application/java-archive");
        jarApi.deleteJar(jarUri);
    }
    
    
    @Test (groups = {"jar","nightly"})
    public void testDeleteJar () {
        String jarUri="jar://test/jar"+System.nanoTime();
        byte [] jarContent = "TEST_CONTENT".getBytes();
        jarApi.putJar(jarUri, jarContent);
        Assert.assertTrue (jarApi.jarExists(jarUri));
        jarApi.deleteJar(jarUri);
        Reporter.log("Verifying "+jarUri + " after deleteJar",true);
        Assert.assertFalse (jarApi.jarExists(jarUri));
    }
    
    @Test (groups = {"jar","nightly"})
    public void testListJarByUriPrefix () {
    	int MAX_JAR=20;
    	String jarPrefix="jar://test/jar"+System.nanoTime();
    	Set<String> jarSet=new HashSet<String>();
    	for (int i =0; i< MAX_JAR;i++) {
	        String jarUri=jarPrefix+"/jar"+System.nanoTime();
	        byte [] jarContent = "TEST_CONTENT".getBytes();
	        jarApi.putJar(jarUri, jarContent);
	        Assert.assertTrue (jarApi.jarExists(jarUri));
	        jarSet.add(jarUri);
    	}
    	jarList.addAll(jarSet);
    	Assert.assertEquals(jarApi.listJarsByUriPrefix(jarPrefix, 2).keySet(), jarSet);
    	
    }
    
    @Test (groups = {"jar","nightly"})
    public void testWorkOrderFromJar () {
        String jarUri="jar://test/jar"+System.nanoTime();
        String jarPath = System.getProperty("user.dir") + File.separator + "build" + File.separator + "resources" + File.separator + "test" + File.separator
                + "jar" + File.separator + "nightly" + File.separator +"testJar.jar";
        Reporter.log("Creating "+jarUri + " from "+jarPath,true);
        byte[] jarContent=null;
        try {
            InputStream is = new FileInputStream(jarPath);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int next = is.read();
            while (next > -1) {
                bos.write(next);
                next = is.read();
            }
            bos.flush();
            jarContent = bos.toByteArray();
            is.close();
        }
        catch (Exception e) {}
        jarApi.putJar(jarUri, jarContent);
        jarList.add(jarUri);
        // Create workflow definition for test
        List<Step> steps = new LinkedList<Step>();   
        Workflow testWf = new Workflow();
        testWf.setSemaphoreType(SemaphoreType.UNLIMITED);
        //create step
        Step s = new Step();
        s.setName("step1");
        s.setDescription("step 1 for a java invocable to write to a doc repo.");
        String stepUri = new RaptureURI("//testjar.steps.CreateData", Scheme.DP_JAVA_INVOCABLE).toString();
        s.setExecutable(stepUri);
        //add to steps
        steps.add(s);
        //add all steps to workflow
        testWf.setSteps(steps);
        testWf.setStartStep("step1");
        List<String> localJarList=new ArrayList<String>();
        localJarList.add(jarUri);
        testWf.setJarUriDependencies(localJarList);
        String wfURI = "workflow://workflow"+System.nanoTime()+"/jar/CreateDataTest";
        Reporter.log("Creating workflow definition "+wfURI,true);
        testWf.setWorkflowURI(wfURI);

        HttpDecisionApi decisionApi=helper.getDecisionApi();
        decisionApi.putWorkflow(testWf);
        workflowList.add(wfURI);
        
        RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, "MONGODB");
        String docPath= new RaptureURI.Builder(repo).docPath("doc"+System.nanoTime()).build().toString();
        Map<String, String> paramMap= new HashMap<String,String>();
        paramMap.put("DOC_URI", docPath);
        
        String content="{\"test\":\"test"+System.nanoTime()+"\"}";
        paramMap.put("DOC_CONTENT",content);
        String workOrderURI = decisionApi.createWorkOrder(testWf.getWorkflowURI(),paramMap);
        Reporter.log("Creating work order "+workOrderURI,true);
        int numRetries=0;
        long waitTimeMS=2000;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi,workOrderURI)  && numRetries < 20) {
            Reporter.log("Checking workorder status, retry count="+numRetries+", waiting "+(waitTimeMS/1000)+" seconds...",true);
            try {
                Thread.sleep(waitTimeMS);
            }
            catch (Exception e) {}
            numRetries++;
        }

        Reporter.log("Checking workorder status and document output",true);
        Assert.assertEquals(decisionApi.getWorkOrderStatus(workOrderURI).getStatus(),WorkOrderExecutionState.FINISHED);
        Assert.assertEquals(helper.getDocApi().getDoc(docPath),content);
    }
    
    @AfterClass
    public void cleanUp() {
    	helper.cleanAllAssets();
    	for (String workflowURI : workflowList)
    		helper.getDecisionApi().deleteWorkflow(workflowURI);
    	for (String jarURI : jarList)
    		jarApi.deleteJar(jarURI);
    }
}
