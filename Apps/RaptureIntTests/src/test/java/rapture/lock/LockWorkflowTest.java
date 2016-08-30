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
package rapture.lock;

import static org.testng.AssertJUnit.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

import rapture.common.CreateResponse;
import rapture.common.LockHandle;
import rapture.common.RaptureLockConfig;
import rapture.common.RaptureScript;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.client.HttpDecisionApi;
import rapture.common.client.HttpLockApi;
import rapture.common.client.HttpScriptApi;
import rapture.common.dp.SemaphoreType;
import rapture.common.dp.Step;
import rapture.common.dp.Transition;
import rapture.common.dp.WorkOrderDebug;
import rapture.common.dp.Workflow;
import rapture.helper.IntegrationTestHelper;

public class LockWorkflowTest {
    
    private IntegrationTestHelper helper;
    private RaptureURI repoUri = null;
    private HttpLockApi lockApi = null;

    @BeforeClass(groups = { "nightly","lock" })
    @Parameters({ "RaptureURL", "RaptureUser", "RapturePassword" })
    public void setUp(@Optional("http://localhost:8665/rapture") String url, @Optional("rapture") String username, @Optional("rapture") String password) {
        helper = new IntegrationTestHelper(url, username, password);
        repoUri = helper.getRandomAuthority(Scheme.DOCUMENT);
        lockApi = helper.getLockApi();
        helper.configureTestRepo(repoUri, "MONGODB"); // TODO Make this configurable
    }

    @AfterClass(groups = { "nightly","lock" })
    public void tearDown() {
    	helper.cleanAllAssets();
    }

    @Test(groups = { "lock", "nightly" })
    public void testTwoWorkflowsWithLocks() {
        RaptureURI workflowRepo = helper.getRandomAuthority(Scheme.WORKFLOW);
        HttpDecisionApi decisionApi = helper.getDecisionApi();
        HttpScriptApi scriptApi = helper.getScriptApi();
        List<Step> thirtyNine = new ArrayList<Step>();
        String scriptUri = new RaptureURI("script://sleep").toString();

        if (!scriptApi.doesScriptExist(scriptUri)) {
            @SuppressWarnings("unused")
            RaptureScript executable = scriptApi.createScript(scriptUri, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM,
                    "println(\"Thread sleeping\");\nsleep(5000);\nreturn 'next';");
        }

        Step s1 = new Step();
        s1.setName("first");
        s1.setExecutable(scriptUri);
        s1.setDescription("first sleep step");

        Transition trans1 = new Transition();
        trans1.setName("next");
        trans1.setTargetStep("$RETURN");

        Transition error = new Transition();
        error.setName("error");
        error.setTargetStep("$FAIL");

        s1.setTransitions(ImmutableList.of(trans1, error));
        thirtyNine.add(s1);

        String semaphoreConfig = "{\"maxAllowed\":1, \"timeout\":5 }";

        // Begin by creating a couple of workflows.
        // In the real world these would be created by two different threads/users/applications,
        // so they use the same URI and semaphore config.

        Workflow flow = new Workflow();
        flow.setStartStep("first");
        flow.setSteps(thirtyNine);
        String flowUri = RaptureURI.builder(workflowRepo).docPath("lockTest").asString();
        flow.setWorkflowURI(flowUri);
        flow.setSemaphoreType(SemaphoreType.WORKFLOW_BASED);
        flow.setSemaphoreConfig(semaphoreConfig);
        decisionApi.putWorkflow(flow);

        Workflow flow2 = new Workflow();
        flow2.setStartStep("first");
        flow2.setSteps(thirtyNine);
        flow2.setWorkflowURI(flowUri);
        flow2.setSemaphoreType(SemaphoreType.WORKFLOW_BASED);
        flow2.setSemaphoreConfig(semaphoreConfig);
        decisionApi.putWorkflow(flow2);

        // Start the first workflow. It will sleep for 5s then exit
        Map<String, String> params = new HashMap<String, String>();
        String orderUri = decisionApi.createWorkOrder(flow.getWorkflowURI(), params);
        assertNotNull(orderUri);

        // Make sure that it's running.
        int numRetries = 20;
        while (decisionApi.getWorkOrderStatus(orderUri).getStatus().equals(WorkOrderExecutionState.NEW) && (--numRetries > 0)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        Assert.assertNotEquals(decisionApi.getWorkOrderStatus(orderUri).getStatus(), WorkOrderExecutionState.NEW, "Workflow failed to start");

        // Now start the second workflow. Use createWorkOrderP to get the full response info
        CreateResponse order2 = decisionApi.createWorkOrderP(flow.getWorkflowURI(), params, null);
        Reporter.log(order2.getMessage(), true);
        // Expect null because of the lock
        Assert.assertNull(order2.getUri());

        // Wait up to 10 seconds for flow to complete
        numRetries = 20;
        while (decisionApi.getWorkOrderStatus(orderUri).getStatus().equals(WorkOrderExecutionState.ACTIVE) && (--numRetries > 0)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }

        Reporter.log("Work order " + orderUri + " has status " + decisionApi.getWorkOrderStatus(orderUri).getStatus(), true);
        WorkOrderDebug details = decisionApi.getWorkOrderDebug(orderUri);
        Reporter.log(details.toString(), true);
        Assert.assertEquals(decisionApi.getWorkOrderStatus(orderUri).getStatus(), WorkOrderExecutionState.FINISHED, "Workflow failed to complete");

        order2 = decisionApi.createWorkOrderP(flow.getWorkflowURI(), params, null);
        String uri2 = order2.getUri();
        assertNotNull(uri2);

        // Wait up to 10 seconds for flow to complete
        numRetries = 20;
        while (!decisionApi.getWorkOrderStatus(uri2).getStatus().equals(WorkOrderExecutionState.FINISHED) && (--numRetries > 0)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }
        Assert.assertEquals(decisionApi.getWorkOrderStatus(uri2).getStatus(), WorkOrderExecutionState.FINISHED, "Workflow failed to complete");
        scriptApi.deleteScript(scriptUri);
    }

    // Cover bad cases
    @Test(groups = { "lock", "nightly" }, enabled = false)
    public void testTwoWorkflowsWithLocksFail() {
        RaptureURI workflowRepo = helper.getRandomAuthority(Scheme.WORKFLOW);
        HttpDecisionApi decisionApi = helper.getDecisionApi();
        HttpScriptApi scriptApi = helper.getScriptApi();
        List<Step> thirtyNine = new ArrayList<Step>();
        String scriptUri = new RaptureURI("script://sleep").toString();

        if (!scriptApi.doesScriptExist(scriptUri)) {
            @SuppressWarnings("unused")
            RaptureScript executable = scriptApi.createScript(scriptUri, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM,
                    "println(\"Thread sleeping\");\nsleep(5000);\nreturn 'next';");
        }

        Step s1 = new Step();
        s1.setName("first");
        s1.setExecutable(scriptUri);
        s1.setDescription("first sleep step");

        Transition trans1 = new Transition();
        trans1.setName("next");
        trans1.setTargetStep("$RETURN");

        Transition error = new Transition();
        error.setName("error");
        error.setTargetStep("$FAIL");

        s1.setTransitions(ImmutableList.of(trans1, error));
        thirtyNine.add(s1);

        String semaphoreConfig1 = "{\"maxAllowed\":1, \"timeout\":5 }";
        String semaphoreConfig2 = "{\"maxAllowed\":2, \"timeout\":10 }";

        // This time let'st happens when the user specifies a different semaphore config

        Workflow flow = new Workflow();
        flow.setStartStep("first");
        flow.setSteps(thirtyNine);
        String flowUri = RaptureURI.builder(workflowRepo).docPath("lockTest").asString();
        flow.setWorkflowURI(flowUri);
        flow.setSemaphoreType(SemaphoreType.WORKFLOW_BASED);
        flow.setSemaphoreConfig(semaphoreConfig1);
        decisionApi.putWorkflow(flow);

        Workflow flow2 = new Workflow();
        flow2.setStartStep("first");
        flow2.setSteps(thirtyNine);
        flow2.setWorkflowURI(flowUri);
        flow2.setSemaphoreType(SemaphoreType.WORKFLOW_BASED);
        flow2.setSemaphoreConfig(semaphoreConfig2);
        decisionApi.putWorkflow(flow2);

        // Start the first workflow. It will sleep for 5s then exit
        Map<String, String> params = new HashMap<String, String>();
        String orderUri = decisionApi.createWorkOrder(flow.getWorkflowURI(), params);
        assertNotNull(orderUri);

        // Make sure that it's running.
        int numRetries = 20;
        while (decisionApi.getWorkOrderStatus(orderUri).getStatus().equals(WorkOrderExecutionState.NEW) && (--numRetries > 0)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        Assert.assertNotEquals(decisionApi.getWorkOrderStatus(orderUri).getStatus(), WorkOrderExecutionState.NEW, "Workflow failed to start");

        // Now start the second workflow. Use createWorkOrderP to get the full response info
        CreateResponse order2 = decisionApi.createWorkOrderP(flow.getWorkflowURI(), params, null);
        Reporter.log(order2.getMessage(), true);
        // Expect null because of the lock
        Assert.assertNull(order2.getUri());

        // Wait up to 10 seconds for flow to complete
        numRetries = 20;
        while (decisionApi.getWorkOrderStatus(orderUri).getStatus().equals(WorkOrderExecutionState.ACTIVE) && (--numRetries > 0)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }

        Reporter.log("Work order " + orderUri + " has status " + decisionApi.getWorkOrderStatus(orderUri).getStatus(), true);
        WorkOrderDebug details = decisionApi.getWorkOrderDebug(orderUri);
        Reporter.log(details.toString(), true);
        Assert.assertEquals(decisionApi.getWorkOrderStatus(orderUri).getStatus(), WorkOrderExecutionState.FINISHED, "Workflow failed to complete");

        order2 = decisionApi.createWorkOrderP(flow.getWorkflowURI(), params, null);
        String uri2 = order2.getUri();
        assertNotNull(uri2);

        // Wait up to 10 seconds for flow to complete
        numRetries = 20;
        while (!decisionApi.getWorkOrderStatus(uri2).getStatus().equals(WorkOrderExecutionState.FINISHED) && (--numRetries > 0)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }
        Assert.assertEquals(decisionApi.getWorkOrderStatus(uri2).getStatus(), WorkOrderExecutionState.FINISHED, "Workflow failed to complete");
        scriptApi.deleteScript(scriptUri);
    }
    
    @Test(groups = { "lock", "nightly" })
    public void testMultipleWorkflowsWithLocks() {
        RaptureURI workflowRepo = helper.getRandomAuthority(Scheme.WORKFLOW);
        HttpDecisionApi decisionApi = helper.getDecisionApi();
        HttpScriptApi scriptApi = helper.getScriptApi();
        List<Step> thirtyNine = new ArrayList<Step>();
        String scriptUri = RaptureURI.builder(helper.getRandomAuthority(Scheme.SCRIPT)).docPath("sleep").asString();
        long scriptSleepTime=10000;
        long timeoutValueSeconds=2;
        int numWorkflows=15;
        
        if (!scriptApi.doesScriptExist(scriptUri)) {
            @SuppressWarnings("unused")
            RaptureScript executable = scriptApi.createScript(scriptUri, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM,
                    "println(\"Thread sleeping\");\nsleep("+scriptSleepTime+");\nreturn 'next';");
        }

        Step s1 = new Step();
        s1.setName("first");
        s1.setExecutable(scriptUri);
        s1.setDescription("first sleep step");

        Transition trans1 = new Transition();
        trans1.setName("next");
        trans1.setTargetStep("$RETURN");

        Transition error = new Transition();
        error.setName("error");
        error.setTargetStep("$FAIL");

        s1.setTransitions(ImmutableList.of(trans1, error));
        thirtyNine.add(s1);

        String semaphoreConfig = "{\"maxAllowed\":1, \"timeout\":"+timeoutValueSeconds+" }";

        // Begin by creating a couple of workflows.
        // In the real world these would be created by two different threads/users/applications,
        // so they use the same URI and semaphore config.

        Workflow flow = new Workflow();
        flow.setStartStep("first");
        flow.setSteps(thirtyNine);
        String flowUri = RaptureURI.builder(workflowRepo).docPath("lockTest").asString();
        flow.setWorkflowURI(flowUri);
        flow.setSemaphoreType(SemaphoreType.WORKFLOW_BASED);
        flow.setSemaphoreConfig(semaphoreConfig);
        decisionApi.putWorkflow(flow);

        
        RaptureURI lockUri = RaptureURI.builder(helper.getRandomAuthority(Scheme.DOCUMENT)).build();
		RaptureLockConfig lockConfig = lockApi.createLockManager(lockUri.toString(), "LOCKING USING ZOOKEEPER {}", "");
        // Start the first workflow. It will sleep for 5s then exit
        Map<String, String> params = new HashMap<String, String>();
        CreateResponse orderUri = decisionApi.createWorkOrderP(flow.getWorkflowURI(), params, null);
        assertNotNull(orderUri);

        List<String>woList = new ArrayList<String> ();
        for (int woCount =0; woCount<=numWorkflows; woCount++) {
        	CreateResponse woUri = decisionApi.createWorkOrderP(flow.getWorkflowURI(), params, null);
        	
        	try {
        		Thread.sleep(200);
            } catch (InterruptedException e) {
            }
        	Reporter.log("Checking work order attempt "+woCount,true);
        	long successfulWoNumber=scriptSleepTime/(timeoutValueSeconds*1000);
        	if (woCount % successfulWoNumber ==0 && woCount >0) {
        		Assert.assertNotNull(woUri.getUri());
        		woList.add(woUri.getUri());
        	}
        	else
        		Assert.assertNull(woUri.getUri());
        	
        }
        try {
        	Thread.sleep(10000);
        } catch (Exception e) {}
        for (String wo : woList) {
        	Reporter.log ("Checking status of "+wo,true);
        	Assert.assertEquals(decisionApi.getWorkOrderStatus(wo).getStatus(),WorkOrderExecutionState.FINISHED);
        }
    }
       
    @Test(groups = { "lock", "nightly" })
    public void testAcquireReleaseLockWithinWorkflows() {
        RaptureURI workflowRepo = helper.getRandomAuthority(Scheme.WORKFLOW);
        HttpDecisionApi decisionApi = helper.getDecisionApi();
        HttpScriptApi scriptApi = helper.getScriptApi();
        List<Step> thirtyNine = new ArrayList<Step>();
        String scriptUri = RaptureURI.builder(helper.getRandomAuthority(Scheme.SCRIPT)).docPath("acquire").asString();
        int numOrders=15;
        
        if (!scriptApi.doesScriptExist(scriptUri)) {
            @SuppressWarnings("unused")
            RaptureScript executable = scriptApi.createScript(scriptUri, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM,
                   "workOrderURI = _params['$DP_WORK_ORDER_URI'];\nworkerURI =  _params['$DP_WORKER_URI'];\n"
                   + "docRepoUri=#decision.getContextValue(workerURI, 'docRepoUri');\n"
                   + "lockUri=#decision.getContextValue(workerURI, 'lockUri');\n"
                   + "lockConfig=#decision.getContextValue(workerURI, 'lockConfig');\n"
            	   + "lockHandle=#lock.acquireLock(lockUri, lockConfig, 2, 10);\n"
            	   + "if (lockHandle != null) do\n"
                   + "l=workOrderURI.split('/');\n"
                   + "woId=l[size(l)-1];\nwinningPath=docRepoUri+'doc/'+woId;\nwinningContent='{\"key\":\"'+woId+'\"}';\n"
                   + "#doc.putDoc(winningPath,winningContent);\n"
                   + "sleep(1000);\n"
                   + "#decision.setContextLiteral(workerURI,'winningPath',winningPath);\n"
            		+"#decision.setContextLiteral(workerURI,'winningContent',winningContent);\n"
            		+ "#lock.releaseLock(lockUri, lockConfig, lockHandle);\n"
            		+ "end\n"
            		+ "return 'next';");
        }
        Reporter.log("Creating script "+scriptUri, true);
        Step s1 = new Step();
        s1.setName("first");
        s1.setExecutable(scriptUri);
        s1.setDescription("first acquire step");

        Transition trans1 = new Transition();
        trans1.setName("next");
        trans1.setTargetStep("$RETURN");

        Transition error = new Transition();
        error.setName("error");
        error.setTargetStep("$FAIL");

        s1.setTransitions(ImmutableList.of(trans1, error));
        thirtyNine.add(s1);

        Workflow flow = new Workflow();
        flow.setStartStep("first");
        flow.setSteps(thirtyNine);
        String flowUri = RaptureURI.builder(workflowRepo).docPath("lockTest").asString();
        flow.setWorkflowURI(flowUri);
        
        Reporter.log("Creating workflow "+flowUri, true);
        decisionApi.putWorkflow(flow);

        // Start the first workflow. It will sleep for 5s then exit
        Map<String, String> params = new HashMap<String, String>();
        RaptureURI docRepoUri =helper.getRandomAuthority(Scheme.DOCUMENT);
		helper.configureTestRepo(docRepoUri, "MONGODB", false);
		RaptureURI lockUri = RaptureURI.builder(helper.getRandomAuthority(Scheme.DOCUMENT)).docPath("locktest"+System.nanoTime()).build();
		RaptureLockConfig lockConfig = lockApi.createLockManager(lockUri.toString(), "LOCKING USING ZOOKEEPER {}", "");

		params.put("docRepoUri", docRepoUri.toString());
		params.put("lockUri", lockUri.toString());
		params.put("lockConfig", lockConfig.getName());

		List<String> woList = new ArrayList<String> ();
		for (int i = 0; i <numOrders; i++) {
			try {
	        	Thread.sleep(500);
	        } catch (Exception e) {}
			CreateResponse orderUri = decisionApi.createWorkOrderP(flow.getWorkflowURI(), params, null);
			assertNotNull(orderUri);
			Reporter.log("Creating workorder "+orderUri.getUri(), true);
			woList.add(orderUri.getUri());
		}
		try {
        	Thread.sleep(500);
        } catch (Exception e) {}
		String winningPath="";
		String winningContent="";

		Map <String,String> resultsMap = new HashMap<String,String> ();
        for (String orderUri:woList) {
        	String currPath=decisionApi.getContextValue(orderUri+"#0", "winningPath");
	       	if (currPath != null) {
	       		Reporter.log("Found workorder winning path: "+orderUri, true);
		       	winningPath=currPath;
		       	winningContent=decisionApi.getContextValue(orderUri+"#0", "winningContent");
		        resultsMap.put(winningPath, winningContent);
	       	}
        }
        try {
        	Thread.sleep(1000);
        } catch (Exception e) {
        	e.printStackTrace();
        }

        for (String currKey : resultsMap.keySet()) {
        	Reporter.log("Checking data "+currKey,true);
        	Assert.assertEquals(helper.getDocApi().getDoc(currKey),resultsMap.get(currKey));
        }
    }
    
    @Test(groups = { "lock", "nightly" })
    public void testBlockingLockWithinWorkflows() {
        RaptureURI workflowRepo = helper.getRandomAuthority(Scheme.WORKFLOW);
        HttpDecisionApi decisionApi = helper.getDecisionApi();
        HttpScriptApi scriptApi = helper.getScriptApi();
        List<Step> thirtyNine = new ArrayList<Step>();
        String scriptUri = RaptureURI.builder(helper.getRandomAuthority(Scheme.SCRIPT)).docPath("acquire").asString();
        int numOrders=10;
        
        if (!scriptApi.doesScriptExist(scriptUri)) {
            @SuppressWarnings("unused")
            RaptureScript executable = scriptApi.createScript(scriptUri, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM,
                   "workOrderURI = _params['$DP_WORK_ORDER_URI'];\nworkerURI =  _params['$DP_WORKER_URI'];\n"
                   + "docRepoUri=#decision.getContextValue(workerURI, 'docRepoUri');\n"
                   + "lockUri=#decision.getContextValue(workerURI, 'lockUri');\n"
                   + "lockConfig=#decision.getContextValue(workerURI, 'lockConfig');\n"
            	   + "lockHandle=#lock.acquireLock(lockUri, lockConfig, 2, 10);\n"
            	   + "if (lockHandle != null) do\n"
                   + "l=workOrderURI.split('/');\n"
                   + "woId=l[size(l)-1];\nwinningPath=docRepoUri+'doc/'+woId;\nwinningContent='{\"key\":\"'+woId+'\"}';\n"
                   + "#doc.putDoc(winningPath,winningContent);\n"
                   + "#decision.setContextLiteral(workerURI,'winningPath',winningPath);\n"
            		+"#decision.setContextLiteral(workerURI,'winningContent',winningContent);\n"
            		+"#decision.setContextLiteral(workerURI,'lockHolder',lockHandle['lockHolder']);\n"
            		+"#decision.setContextLiteral(workerURI,'lockName',lockHandle['lockName']);\n"
            		+ "end\n"
            		+ "return 'next';");
        }
        Reporter.log("Creating script "+scriptUri, true);
        Step s1 = new Step();
        s1.setName("first");
        s1.setExecutable(scriptUri);
        s1.setDescription("first acquire step");

        Transition trans1 = new Transition();
        trans1.setName("next");
        trans1.setTargetStep("$RETURN");

        Transition error = new Transition();
        error.setName("error");
        error.setTargetStep("$FAIL");

        s1.setTransitions(ImmutableList.of(trans1, error));
        thirtyNine.add(s1);

        Workflow flow = new Workflow();
        flow.setStartStep("first");
        flow.setSteps(thirtyNine);
        String flowUri = RaptureURI.builder(workflowRepo).docPath("lockTest").asString();
        flow.setWorkflowURI(flowUri);
        
        Reporter.log("Creating workflow "+flowUri, true);
        decisionApi.putWorkflow(flow);

        // Start the first workflow. It will sleep for 5s then exit
        Map<String, String> params = new HashMap<String, String>();
        RaptureURI docRepoUri =helper.getRandomAuthority(Scheme.DOCUMENT);
		helper.configureTestRepo(docRepoUri, "MONGODB", false);
		RaptureURI lockUri = RaptureURI.builder(helper.getRandomAuthority(Scheme.DOCUMENT)).docPath("locktest"+System.nanoTime()).build();
		RaptureLockConfig lockConfig = lockApi.createLockManager(lockUri.toString(), "LOCKING USING ZOOKEEPER {}", "");

		params.put("docRepoUri", docRepoUri.toString());
		params.put("lockUri", lockUri.toString());
		params.put("lockConfig", lockConfig.getName());

		List<String> woList = new ArrayList<String> ();
		for (int i = 0; i <numOrders; i++) {
			try {
	        	Thread.sleep(1000);
	        } catch (Exception e) {}
			CreateResponse orderUri = decisionApi.createWorkOrderP(flow.getWorkflowURI(), params, null);
			assertNotNull(orderUri);
			Reporter.log("Creating workorder "+orderUri.getUri(), true);
			woList.add(orderUri.getUri());
		}
        
		LockHandle lockHandle= new LockHandle();
		String winningPath="";
		String winningContent="";
        for (String orderUri:woList) {
        	String currPath=decisionApi.getContextValue(orderUri+"#0", "winningPath");
	       	if (currPath != null) {
	       		Reporter.log("Found workorder winning path: "+orderUri, true);
		       	winningPath=currPath;
		       	winningContent=decisionApi.getContextValue(orderUri+"#0", "winningContent");
		       	lockHandle.setLockHolder(decisionApi.getContextValue(orderUri+"#0", "lockHolder"));
		       	lockHandle.setLockName(decisionApi.getContextValue(orderUri+"#0", "lockName"));	
	       	}
        }
        Assert.assertTrue(lockApi.releaseLock(lockUri.toString(), lockConfig.getName(), lockHandle));
        Assert.assertEquals(helper.getDocApi().getDoc(winningPath),winningContent);
    }
}
