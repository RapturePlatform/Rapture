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
import rapture.common.RaptureLockConfig;
import rapture.common.RaptureScript;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.client.HttpAdminApi;
import rapture.common.client.HttpDecisionApi;
import rapture.common.client.HttpLockApi;
import rapture.common.client.HttpScriptApi;
import rapture.common.dp.SemaphoreType;
import rapture.common.dp.Step;
import rapture.common.dp.Transition;
import rapture.common.dp.WorkOrderDebug;
import rapture.common.dp.Workflow;
import rapture.common.impl.jackson.MD5Utils;
import rapture.helper.IntegrationTestHelper;
import rapture.kernel.LockApiImpl;

public class LockWorkflowTest {
    
    private IntegrationTestHelper helper;
    private HttpLockApi lockApi = null;
    private HttpAdminApi admin = null;

    private static final String user = "User";
    private IntegrationTestHelper helper2;
    private RaptureURI repoUri = null;

    @BeforeClass(groups = { "nightly" })
    @Parameters({ "RaptureURL", "RaptureUser", "RapturePassword" })
    public void setUp(@Optional("http://localhost:8665/rapture") String url, @Optional("rapture") String username, @Optional("rapture") String password) {


        helper = new IntegrationTestHelper(url, username, password);
        lockApi = helper.getLockApi();
        admin = helper.getAdminApi();
        if (!admin.doesUserExist(user)) {
            admin.addUser(user, "Another User", MD5Utils.hash16(user), "user@incapture.net");
        }

        helper2 = new IntegrationTestHelper(url, user, user);

        repoUri = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repoUri, "MONGODB"); // TODO Make this configurable
    }

    @AfterClass(groups = { "nightly" })
    public void tearDown() {
    }

    @Test(groups = { "locking", "nightly" })
    public void lockWorkflowTest() {
        RaptureLockConfig lockConfig = lockApi.createLockManager(LockApiImpl.SEMAPHORE_MANAGER_URI.toString(), "LOCKING USING ZOOKEEPER {}", "");
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
        Reporter.log(order2.getMessage());
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

        Reporter.log("Work order " + orderUri + " has status " + decisionApi.getWorkOrderStatus(orderUri).getStatus());
        WorkOrderDebug details = decisionApi.getWorkOrderDebug(orderUri);
        Reporter.log(details.toString());
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
    @Test(groups = { "locking", "nightly" }, enabled = false)
    public void lockWorkflowTest2() {
        RaptureLockConfig lockConfig = lockApi.createLockManager(LockApiImpl.SEMAPHORE_MANAGER_URI.toString(), "LOCKING USING ZOOKEEPER {}", "");
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
        Reporter.log(order2.getMessage());
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

        Reporter.log("Work order " + orderUri + " has status " + decisionApi.getWorkOrderStatus(orderUri).getStatus());
        WorkOrderDebug details = decisionApi.getWorkOrderDebug(orderUri);
        Reporter.log(details.toString());
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
}
