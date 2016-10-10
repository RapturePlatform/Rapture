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
package rapture.decision;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.client.HttpDecisionApi;
import rapture.common.client.HttpScriptApi;
import rapture.common.dp.SemaphoreType;
import rapture.common.dp.Step;
import rapture.common.dp.StepRecordDebug;
import rapture.common.dp.Transition;
import rapture.common.dp.WorkOrderDebug;
import rapture.common.dp.Worker;
import rapture.common.dp.WorkerDebug;
import rapture.common.dp.WorkerExecutionState;
import rapture.common.dp.Workflow;
import rapture.common.exception.ExceptionToString;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.helper.IntegrationTestHelper;

public class DecisionApiTests {
    private HttpDecisionApi decisionApi = null;
    private Map<String, String> scriptMap = null;
    List<String> workflowList = null;
    IntegrationTestHelper helper = null;
    String workFlowPrefix = "workflow" + System.nanoTime();

    @BeforeClass(groups = { "decision", "nightly" })
    @Parameters({ "RaptureURL", "RaptureUser", "RapturePassword" })
    public void beforeTest(@Optional("http://localhost:8665/rapture") String url, @Optional("rapture") String user, @Optional("rapture") String password) {
       
    	helper = new IntegrationTestHelper(url, user, password);
        decisionApi = new HttpDecisionApi(helper.getRaptureLogin());
        scriptMap = new HashMap<String, String>();
        loadScripts(helper.getRandomAuthority(Scheme.SCRIPT));
        workflowList = new ArrayList<String>();
    }

    @Test(groups = { "decision", "nightly" })
    public void testSplitJoin() throws InterruptedException {

        // setup series repo and inject into workflow via reflex params
        // Setup the overall workflow step object
        List<Step> workflowSteps = new ArrayList<Step>();
        // create steps for the workflow
        // define parent step
        Step s1 = new Step();
        s1.setName("parentStep");
        s1.setDescription("basicSplitJoinTest_step1");
        s1.setExecutable("$SPLIT:stepChild1,stepChild2");
        Transition successTransition = new Transition();
        successTransition.setName("ok");
        successTransition.setTargetStep("stepCleanUpAfterJoin");
        Transition errorTransition = new Transition();
        errorTransition.setName("error");
        errorTransition.setTargetStep("stepToCleanUpFromFailure");
        s1.setTransitions(ImmutableList.of(successTransition, errorTransition));
        workflowSteps.add(s1);
        // define child 1 step
        Step s2 = new Step();
        s2.setName("stepChild1");
        s2.setDescription("basicSplitJoinTest_step2");
        s2.setExecutable(scriptMap.get("child1.rfx"));
        Transition child1Trans = new Transition();
        child1Trans.setName("stepCleanUpAfterJoin");
        child1Trans.setTargetStep("$JOIN");
        s2.setTransitions(ImmutableList.of(child1Trans));
        workflowSteps.add(s2);
        // define child 2 step
        Step s3 = new Step();
        s3.setName("stepChild2");
        s3.setDescription("basicSplitJoinTest_step3");
        s3.setExecutable(scriptMap.get("child2.rfx"));
        Transition child2Trans = new Transition();
        child2Trans.setName("stepCleanUpAfterJoin");
        child2Trans.setTargetStep("$JOIN");
        s3.setTransitions(ImmutableList.of(child2Trans));
        workflowSteps.add(s3);
        // step after join i.e. after child1 and 2 have finished
        Step s4 = new Step();
        s4.setName("stepCleanUpAfterJoin");
        s4.setDescription("basicSplitJoinTest_step4");
        s4.setExecutable(scriptMap.get("cleanupafterjoin.rfx"));
        workflowSteps.add(s4);
        // error step
        Step s5 = new Step();
        s5.setName("stepToCleanUpFromFailure");
        s5.setDescription("basicSplitJoinTest_step5");
        s5.setExecutable(scriptMap.get("cleanuponerror.rfx"));
        workflowSteps.add(s5);

        ////////////////////////////////////////////////////////////////////////
        // create workflow and setup with params
        Workflow simpleSplit = new Workflow();
        simpleSplit.setStartStep("parentStep");
        simpleSplit.setSteps(workflowSteps);

        String wfURI = "workflow://" + workFlowPrefix + "/nightly/parallel/basicSplitJoinTest";
        simpleSplit.setWorkflowURI(wfURI);

        Reporter.log("Creating workflow: " + wfURI, true);
        decisionApi.putWorkflow(simpleSplit);
        workflowList.add(wfURI);
        // not sure we need params for this test.
        Map<String, String> params = new HashMap<String, String>();
        params.put("caller", "basicSplitJoinTest");
        params.put("contextParamTest1", "sample_value_1");

        String createWorkOrder = decisionApi.createWorkOrder(simpleSplit.getWorkflowURI(), params);
        Reporter.log("Created work order uri: " + createWorkOrder, true);

        int numRetries = 0;
        long waitTimeMS = 2000;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi, createWorkOrder) && numRetries < 20) {
            Reporter.log("Checking workorder status, retry count=" + numRetries + ", waiting " + (waitTimeMS / 1000) + " seconds...", true);
            try {
                Thread.sleep(waitTimeMS);
            } catch (Exception e) {
            }
            numRetries++;
        }

        WorkOrderDebug woDebug = decisionApi.getWorkOrderDebug(createWorkOrder);
        Map <String, Object> woMap=JacksonUtil.getMapFromJson(woDebug.toString());
        Assert.assertEquals(((Map <String, Object>)woMap.get("order")).get("workOrderURI").toString(),createWorkOrder);
        Assert.assertNotNull(((Map <String, Object>)woMap.get("order")).get("workerIds"));
        List<WorkerDebug> workerDebugs = woDebug.getWorkerDebugs();
        Reporter.log("Overall runtime : " + (decisionApi.getWorkOrderDebug(createWorkOrder).getOrder().getEndTime()
                - decisionApi.getWorkOrderDebug(createWorkOrder).getOrder().getStartTime()) + "ms", true);
        
        List<WorkerDebug> woDebugsList=woDebug.getWorkerDebugs();
        for (WorkerDebug wd : woDebugsList) {
            Reporter.log(wd.toString(), true);
            Map <String, Object> workerMap=JacksonUtil.getMapFromJson(wd.toString());
            for (Map<String,Object> currStepRecordDebug : (List <Map<String, Object>>)workerMap.get("stepRecordDebugs")) {

            	Object currStepRecord=((Map<String,Object>)currStepRecordDebug).get("stepRecord");
            	Map<String,Object> stepRecordMap =(Map<String,Object>)currStepRecord;
            	Assert.assertTrue(stepRecordMap.keySet().containsAll(Arrays.asList(new String[]{"stepURI","name","startTime","endTime"})));
            }
        }
        Assert.assertEquals(workerDebugs.get(0).getWorker().getStatus().name(), "FINISHED", "worker1 status");
        Assert.assertEquals(workerDebugs.get(1).getWorker().getStatus().name(), "FINISHED", "worker2 status");
        Assert.assertEquals(workerDebugs.get(2).getWorker().getStatus().name(), "FINISHED", "worker2 status");
        Assert.assertEquals(decisionApi.getWorkOrderStatus(createWorkOrder).getStatus().name(), "FINISHED", "Overall work order status");
    }

    @Test(groups = { "decision", "nightly" })
    public void testForkAfterFirstStepReflexInvocableTest() {
        RaptureURI docRepoURI = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(docRepoURI, "MONGODB");
        // step parent sets up repo document and passes repo uri back as a context variable
        List<Step> workflowSteps = new ArrayList<Step>();

        // define parent step
        Step s1 = new Step();
        s1.setName("parentStep");
        s1.setDescription("parent step");
        Transition successTransition = new Transition();
        successTransition.setName("ok");
        successTransition.setTargetStep("step2fork");
        s1.setTransitions(ImmutableList.of(successTransition));
        s1.setExecutable(scriptMap.get("parentFork1.rfx"));
        workflowSteps.add(s1);

        // step 2 is fork
        Step s2 = new Step();
        s2.setName("step2fork");
        s2.setDescription("step2 fork");
        s2.setExecutable("$FORK:forkChild1,forkChild2");
        workflowSteps.add(s2);

        // step 3 and 4 are forked
        Step s3 = new Step();
        s3.setName("forkChild1");
        s3.setDescription("basicForkTest step3");
        s3.setExecutable(scriptMap.get("childForkShareCtxVar1.rfx"));
        workflowSteps.add(s3);

        Step s4 = new Step();
        s4.setName("forkChild2");
        s4.setDescription("basicForkTest step4");
        s4.setExecutable(scriptMap.get("childForkShareCtxVar2.rfx"));
        workflowSteps.add(s4);

        ////////////////////////////////////////////////////////////////////////
        // create workflow and setup with params
        Workflow forkSplit = new Workflow();
        forkSplit.setStartStep("parentStep");
        forkSplit.setSteps(workflowSteps);
        String wfURI = "workflow://" + workFlowPrefix + "/nightly/parallel/basicForkAtStep2Test";
        forkSplit.setWorkflowURI(wfURI);
        forkSplit.setCategory("alpha");

        Reporter.log("Creating workflow: " + wfURI, true);
        decisionApi.putWorkflow(forkSplit);
        workflowList.add(wfURI);
        // pass in a document repo to reflex scripts so they can write to it
        Map<String, String> params = new HashMap<String, String>();
        params.put("caller", "basicForkTest");

        String docURI = "document://" + docRepoURI.getAuthority() + "/doc";

        params.put("docRepoUri", docURI);
        String randInt = new Long(System.nanoTime()).toString();
        params.put("randomNumber", randInt);
        String createWorkOrder = decisionApi.createWorkOrder(forkSplit.getWorkflowURI(), params);
        Reporter.log("Created work order uri: " + createWorkOrder, true);

        int numRetries = 0;
        long waitTimeMS = 2000;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi, createWorkOrder) && numRetries < 20) {
            Reporter.log("Checking workorder status, retry count=" + numRetries + ", waiting " + (waitTimeMS / 1000) + " seconds...", true);
            try {
                Thread.sleep(waitTimeMS);
            } catch (Exception e) {
            }
            numRetries++;
        }
        Assert.assertEquals(decisionApi.getWorkOrderStatus(createWorkOrder).getStatus(), WorkOrderExecutionState.FINISHED);
        /////////////////////////////////////////////////////////////////////////////////////
        // check output in doc repos was as expected + context var is returned from workorder

        String duri = decisionApi.getContextValue(createWorkOrder, "docRepoUri");
        try {
            Thread.sleep(7500);
        } catch (Exception e) {
        }
        Map<String, RaptureFolderInfo> allChildrenMap = helper.getDocApi().listDocsByUriPrefix(duri, 10);
        Assert.assertEquals(allChildrenMap.size(), 3, "Check number of documents created is 3.");

        // check worker 0. this should have 2 set records

        numRetries = 30;
        List<WorkerDebug> woDebugsList = null;
        WorkOrderDebug woDebug = decisionApi.getWorkOrderDebug(createWorkOrder);;
        Map <String, Object> woMap=JacksonUtil.getMapFromJson(woDebug.toString());
        Assert.assertEquals(((Map <String, Object>)woMap.get("order")).get("workOrderURI").toString(),createWorkOrder);
        Assert.assertNotNull(((Map <String, Object>)woMap.get("order")).get("workerIds"));
        do {
            if (woDebugsList != null) try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            
            woDebugsList = woDebug.getWorkerDebugs();
            Reporter.log("+++ Got " + woDebugsList.size() + " WorkerDebugs", true);
            for (WorkerDebug wd : woDebugsList) {
                Reporter.log(wd.toString(), true);
                Map <String, Object> workerMap=JacksonUtil.getMapFromJson(wd.toString());
                for (Map<String,Object> currStepRecordDebug : (List <Map<String, Object>>)workerMap.get("stepRecordDebugs")) {

                	Object currStepRecord=((Map<String,Object>)currStepRecordDebug).get("stepRecord");
                	Map<String,Object> stepRecordMap =(Map<String,Object>)currStepRecord;
                	Assert.assertTrue(stepRecordMap.keySet().containsAll(Arrays.asList(new String[]{"stepURI","name","startTime","endTime"})));
                }
            }
        } while ((numRetries-- > 0) && (woDebugsList.size() != 3));

        Assert.assertEquals(woDebugsList.size(), 3, "Check number of worker ids is 3");

        WorkerDebug woDebug0 = woDebugsList.get(0);
        // Worker worker0 = decision.getWorker(createWorkOrder, "0");
        List<StepRecordDebug> stepRecords0 = woDebug0.getStepRecordDebugs();
        // List<StepRecord> stepRecords0 = worker0.getStepRecords();
        Assert.assertEquals(stepRecords0.size(), 2, "Check number of step records in worker 0.");

        // first step record
        String stepName0_1 = stepRecords0.get(0).getStepRecord().getName();
        String retVal0_1 = stepRecords0.get(0).getStepRecord().getRetVal();
        String stepURI0_1 = stepRecords0.get(0).getStepRecord().getStepURI();
        String executable0_1 = decisionApi.getWorkflowStep(stepURI0_1).getExecutable();
        Assert.assertEquals(executable0_1, scriptMap.get("parentFork1.rfx"), "check step 1 executable for worker 0");
        Assert.assertEquals(stepName0_1, "parentStep", "check step name for worker 0");
        Assert.assertEquals(retVal0_1, "ok", "check ret val for worker 0");
        Assert.assertEquals(stepURI0_1, forkSplit.getWorkflowURI() + "#" + "parentStep", "check step uri for worker 0");
        // second step record
        String stepName0_2 = stepRecords0.get(1).getStepRecord().getName();
        String retVal0_2 = stepRecords0.get(1).getStepRecord().getRetVal();
        String stepURI0_2 = stepRecords0.get(1).getStepRecord().getStepURI();
        String executable0_2 = decisionApi.getWorkflowStep(stepURI0_2).getExecutable();
        Assert.assertEquals(executable0_2, "$FORK:forkChild1,forkChild2", "check step 1 executable for worker 0");
        Assert.assertEquals(stepName0_2, "step2fork", "check step name for worker 0");
        Assert.assertEquals(retVal0_2, "ok", "check ret val for worker 0");
        Assert.assertEquals(stepURI0_2, forkSplit.getWorkflowURI() + "#" + "step2fork", "check step uri for worker 0");

        // check worker 1. this should have 1 record
        WorkerDebug woDebug1 = woDebugsList.get(1);

        List<StepRecordDebug> stepRecords1 = woDebug1.getStepRecordDebugs();
        Assert.assertEquals(stepRecords1.size(), 1, "Check number of step records in worker 1.");
        // first step record
        String stepName1 = stepRecords1.get(0).getStepRecord().getName();
        String retVal1 = stepRecords1.get(0).getStepRecord().getRetVal();
        String stepURI1 = stepRecords1.get(0).getStepRecord().getStepURI();
        String executable1 = decisionApi.getWorkflowStep(stepURI1).getExecutable();
        Assert.assertEquals(executable1, scriptMap.get("childForkShareCtxVar1.rfx"), "check forkstep1 executable for worker 1");
        Assert.assertEquals(stepName1, "forkChild1", "check step name for worker 1");
        Assert.assertEquals(retVal1, "ok", "check ret val for worker 1");
        Assert.assertEquals(stepURI1, forkSplit.getWorkflowURI() + "#" + "forkChild1", "check step uri for worker 1");

        // check worker 1. this should have 1 record
        WorkerDebug woDebug2 = woDebugsList.get(2);
        List<StepRecordDebug> stepRecords2 = woDebug2.getStepRecordDebugs();
        Assert.assertEquals(stepRecords2.size(), 1, "Check number of step records in worker 1.");
        // first step record
        String stepName2 = stepRecords2.get(0).getStepRecord().getName();
        String retVal2 = stepRecords2.get(0).getStepRecord().getRetVal();
        String stepURI2 = stepRecords2.get(0).getStepRecord().getStepURI();
        String executable2 = decisionApi.getWorkflowStep(stepURI2).getExecutable();
        Assert.assertEquals(executable2, scriptMap.get("childForkShareCtxVar2.rfx"), "check forkstep2 executable for worker 2");
        Assert.assertEquals(stepName2, "forkChild2", "check step name for worker 2");
        Assert.assertEquals(retVal2, "ok", "check ret val for worker 2");
        Assert.assertEquals(stepURI2, forkSplit.getWorkflowURI() + "#" + "forkChild2", "check step uri for worker 2");

        Assert.assertEquals(helper.getDocApi().getDoc(docURI + "/" + randInt + "/docFromFork1"), "{\"KeyFromForkChild1\":\"ValueFromForkChild1\"}");
        Assert.assertEquals(helper.getDocApi().getDoc(docURI + "/" + randInt + "/docFromFork2"), "{\"KeyFromChild2\":\"ValueFromChild2\"}");

    }

    @Test(groups = { "decision", "nightly" })
    public void testBasicForkFirstStepReflexInvocableTest() {

        RaptureURI docRepoURI = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(docRepoURI, "MONGODB");
        // Setup the overall workflow step object
        List<Step> workflowSteps = new ArrayList<Step>();
        // define parent step
        Step s1 = new Step();
        s1.setName("parentStep");
        s1.setDescription("basicForkTest_step1");
        s1.setExecutable("$FORK:forkChild1,forkChild2");
        workflowSteps.add(s1);

        // forked step 1
        Step s2 = new Step();
        s2.setName("forkChild1");
        s2.setDescription("basicForkTest_step2");
        s2.setExecutable(scriptMap.get("childFork1.rfx"));
        workflowSteps.add(s2);

        // forked step 2
        Step s3 = new Step();
        s3.setName("forkChild2");
        s3.setDescription("basicForkTest_step3");
        s3.setExecutable(scriptMap.get("childFork2.rfx"));
        workflowSteps.add(s3);

        ////////////////////////////////////////////////////////////////////////
        // create workflow and setup with params
        Workflow forkSplit = new Workflow();
        forkSplit.setStartStep("parentStep");
        forkSplit.setSteps(workflowSteps);
        String wfURI = "workflow://" + workFlowPrefix + "/nightly/parallel/basicForkFirstStepReflexInvocableTest";
        forkSplit.setWorkflowURI(wfURI);

        Reporter.log("Created workflow: " + wfURI, true);
        decisionApi.putWorkflow(forkSplit);
        workflowList.add(wfURI);

        // pass in a document repo to reflex scripts so they can write to it
        Map<String, String> params = new HashMap<String, String>();
        String docURI = "document://" + docRepoURI.getAuthority() + "/doc";

        params.put("caller", "basicForkTest");
        params.put("docRepoUri", docURI);
        String randInt = new Long(System.nanoTime()).toString();
        params.put("randomNumber", randInt.toString());

        String createWorkOrder = decisionApi.createWorkOrder(forkSplit.getWorkflowURI(), params);
        Reporter.log("Work order uri: " + createWorkOrder, true);

        int numRetries = 20;
        long waitTimeMS = 2000;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi, createWorkOrder) && (numRetries-- > 0)) {
            Reporter.log("Checking workorder status, retry count=" + numRetries + ", waiting " + (waitTimeMS / 1000) + " seconds...", true);
            try {
                Thread.sleep(waitTimeMS);
            } catch (InterruptedException e) {
            }
        }

        Reporter.log("Work order no longer running", true);

        Assert.assertEquals(decisionApi.getWorkOrderStatus(createWorkOrder).getStatus(), WorkOrderExecutionState.FINISHED);
        /////////////////////////////////////////////////////////////////////////////////////
        // check output in doc repos was as expected + context var is returned from workorder

        numRetries = 30;
        List<WorkerDebug> woDebugsList = null;
        WorkOrderDebug woDebug = decisionApi.getWorkOrderDebug(createWorkOrder);
        
        Map <String, Object> woMap=JacksonUtil.getMapFromJson(woDebug.toString());
        Assert.assertEquals(((Map <String, Object>)woMap.get("order")).get("workOrderURI").toString(),createWorkOrder);
        Assert.assertNotNull(((Map <String, Object>)woMap.get("order")).get("workerIds"));
        do {
            if (woDebugsList != null) try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            woDebugsList = woDebug.getWorkerDebugs();
            Reporter.log("Got " + woDebugsList.size() + " WorkerDebugs", true);
            for (WorkerDebug wd : woDebugsList) {
                Reporter.log(wd.toString());
            }
        } while ((numRetries-- > 0) && (woDebugsList.size() != 3));

        // get worker threads using getWorkerIds()
        //Assert.assertEquals(woDebugsList.size(), 3, "Check number of worker ids is 3");
        for (WorkerDebug wd : woDebugsList) {
            Reporter.log(wd.toString(), true);
            Map <String, Object> workerMap=JacksonUtil.getMapFromJson(wd.toString());
            for (Map<String,Object> currStepRecordDebug : (List <Map<String, Object>>)workerMap.get("stepRecordDebugs")) {

            	Object currStepRecord=((Map<String,Object>)currStepRecordDebug).get("stepRecord");
            	Map<String,Object> stepRecordMap =(Map<String,Object>)currStepRecord;
            	Assert.assertTrue(stepRecordMap.keySet().containsAll(Arrays.asList(new String[]{"stepURI","name","startTime","endTime"})));
            }
        }
        for (WorkerDebug wo : woDebugsList) {

            List<StepRecordDebug> stepRecordDebugs = wo.getStepRecordDebugs();
            String workerState = wo.getWorker().getStatus().name();
            Assert.assertEquals(stepRecordDebugs.size(), 1, "check number of steprecords in worker.");
            Assert.assertEquals(workerState, "FINISHED", "status check on worker.");
        }
        // check worker 1

        List<StepRecordDebug> stepRecordDebug0 = woDebugsList.get(0).getStepRecordDebugs();
        String stepName1 = stepRecordDebug0.get(0).getStepRecord().getName();
        String retVal1 = stepRecordDebug0.get(0).getStepRecord().getRetVal();
        String stepURI1 = stepRecordDebug0.get(0).getStepRecord().getStepURI();
        String executable1 = decisionApi.getWorkflowStep(stepURI1).getExecutable();
        Assert.assertEquals(executable1, "$FORK:forkChild1,forkChild2", "check step 1 executable for worker 0");
        Assert.assertEquals(stepName1, "parentStep", "check step name for worker 0");
        Assert.assertEquals(retVal1, "ok", "check ret val for worker 0");
        Assert.assertEquals(stepURI1, forkSplit.getWorkflowURI() + "#" + "parentStep", "check step uri for worker 0");

        // check worker 2

        List<StepRecordDebug> stepRecordDebug1 = woDebugsList.get(1).getStepRecordDebugs();
        String stepName2 = stepRecordDebug1.get(0).getStepRecord().getName();
        String retVal2 = stepRecordDebug1.get(0).getStepRecord().getRetVal();
        String stepURI2 = stepRecordDebug1.get(0).getStepRecord().getStepURI();
        String executable2 = decisionApi.getWorkflowStep(stepURI2).getExecutable();
        Assert.assertEquals(executable2, scriptMap.get("childFork1.rfx"), "check step 2 executable for worker 1");
        Assert.assertEquals(stepName2, "forkChild1");
        Assert.assertEquals(retVal2, "ok");
        Assert.assertEquals(stepURI2, forkSplit.getWorkflowURI() + "#" + "forkChild1");

        // check worker 3

        List<StepRecordDebug> stepRecordDebug2 = woDebugsList.get(2).getStepRecordDebugs();
        String stepName3 = stepRecordDebug2.get(0).getStepRecord().getName();
        String retVal3 = stepRecordDebug2.get(0).getStepRecord().getRetVal();
        String stepURI3 = stepRecordDebug2.get(0).getStepRecord().getStepURI();
        String executable3 = decisionApi.getWorkflowStep(stepURI3).getExecutable();
        Assert.assertEquals(executable3, scriptMap.get("childFork2.rfx"), "check step 3 executable for worker 2");
        Assert.assertEquals(stepName3, "forkChild2");
        Assert.assertEquals(retVal3, "ok");
        Assert.assertEquals(stepURI3, forkSplit.getWorkflowURI() + "#" + "forkChild2");

        // read from doc repo written to by forked steps
        String str1 = helper.getDocApi().getDoc(docURI + randInt + "/docFromFork1");
        Assert.assertEquals(str1, "{\"KeyFromChild1\":\"ValueFromChild1\"}", "Check doc written by fork 1.");

        String str2 = helper.getDocApi().getDoc(docURI + randInt + "/docFromFork2");
        Assert.assertEquals(str2, "{\"KeyFromChild2\":\"ValueFromChild2\"}", "Check doc written by fork 1.");

    }

    @Test(groups = { "decision", "nightly" })
    public void testNestedForksBalancedTest() {

        // Setup the overall workflow step object
        List<Step> workflowSteps = new ArrayList<Step>();
        // define parent step
        Step s1 = new Step();
        s1.setName("parentStep");
        s1.setDescription("basicForkTest_step1");
        s1.setExecutable("$FORK:subParentFork1,subParentFork2");
        workflowSteps.add(s1);

        // forked subparent1 step 2
        Step s2 = new Step();
        s2.setName("subParentFork1");
        s2.setDescription("basicForkTest_step2");
        s2.setExecutable("$FORK:forkChild1,forkChild2");
        workflowSteps.add(s2);

        // forked subparent1 step 3
        Step s3 = new Step();
        s3.setName("subParentFork2");
        s3.setDescription("basicForkTest_step3");
        s3.setExecutable("$FORK:forkChild3,forkChild4");
        workflowSteps.add(s3);

        Step s5 = new Step();
        s5.setName("forkChild1");
        s5.setDescription("basicForkTest_childstep4");
        s5.setExecutable(scriptMap.get("childFork1.rfx"));
        workflowSteps.add(s5);

        // forked child2 step 6
        Step s6 = new Step();
        s6.setName("forkChild2");
        s6.setDescription("basicForkTest_childstep5");
        s6.setExecutable(scriptMap.get("childFork2.rfx"));
        workflowSteps.add(s6);

        // forked child3 step 7
        Step s7 = new Step();
        s7.setName("forkChild3");
        s7.setDescription("basicForkTest_childstep6");
        s7.setExecutable(scriptMap.get("childFork3.rfx"));
        workflowSteps.add(s7);

        // forked child4 step 8
        Step s8 = new Step();
        s8.setName("forkChild4");
        s8.setDescription("basicForkTest_childstep7");
        s8.setExecutable(scriptMap.get("childFork4.rfx"));
        workflowSteps.add(s8);

        ////////////////////////////////////////////////////////////////////////
        // create workflow and setup with params
        Workflow forkSplit = new Workflow();
        forkSplit.setStartStep("parentStep");
        forkSplit.setSteps(workflowSteps);

        String wfURI = "workflow://" + workFlowPrefix + "/nightly/parallel/basicForkBalancedTest";

        forkSplit.setWorkflowURI(wfURI);

        Reporter.log("Created workflow: " + wfURI, true);
        decisionApi.putWorkflow(forkSplit);
        workflowList.add(wfURI);

        Map<String, String> params = new HashMap<String, String>();
        params.put("caller", "nestedForksBalancedTest");

        RaptureURI docRepoURI = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(docRepoURI, "MONGODB");

        String docRepoUri = "document://" + docRepoURI.getAuthority();
        params.put("docRepoUri", docRepoUri);
        String randInt = new Long(System.nanoTime()).toString();
        params.put("randomNumber", randInt.toString());
        String createWorkOrder = decisionApi.createWorkOrder(forkSplit.getWorkflowURI(), params);
        Reporter.log("Work order uri: " + createWorkOrder, true);

        int numRetries = 0;
        long waitTimeMS = 2000;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi, createWorkOrder) && numRetries < 20) {
            Reporter.log("Checking workorder status, retry count=" + numRetries + ", waiting " + (waitTimeMS / 1000) + " seconds...", true);
            try {
                Thread.sleep(waitTimeMS);
            } catch (Exception e) {
            }
            numRetries++;
        }
        WorkOrderDebug woDebug = decisionApi.getWorkOrderDebug(createWorkOrder);
        Map <String, Object> woMap=JacksonUtil.getMapFromJson(woDebug.toString());
        Assert.assertEquals(((Map <String, Object>)woMap.get("order")).get("workOrderURI").toString(),createWorkOrder);
        Assert.assertNotNull(((Map <String, Object>)woMap.get("order")).get("workerIds"));
        List<WorkerDebug> woDebugsList = woDebug.getWorkerDebugs();

        for (WorkerDebug wo : woDebugsList) {
            String workerState = wo.getWorker().getStatus().name();
            Assert.assertEquals(workerState, "FINISHED", "status check on worker.");
        }

        for (WorkerDebug wd : woDebugsList) {
            Reporter.log(wd.toString(), true);
            Map <String, Object> workerMap=JacksonUtil.getMapFromJson(wd.toString());
            for (Map<String,Object> currStepRecordDebug : (List <Map<String, Object>>)workerMap.get("stepRecordDebugs")) {

            	Object currStepRecord=((Map<String,Object>)currStepRecordDebug).get("stepRecord");
            	Map<String,Object> stepRecordMap =(Map<String,Object>)currStepRecord;
            	Assert.assertTrue(stepRecordMap.keySet().containsAll(Arrays.asList(new String[]{"stepURI","name","startTime","endTime"})));
            }
        }
        IntegrationTestHelper.isWorkOrderRunning(decisionApi, createWorkOrder);

        Assert.assertEquals(helper.getDocApi().getDoc(docRepoUri + randInt + "/docFromFork1"), "{\"KeyFromChild1\":\"ValueFromChild1\"}");
        Assert.assertEquals(helper.getDocApi().getDoc(docRepoUri + randInt + "/docFromFork2"), "{\"KeyFromChild2\":\"ValueFromChild2\"}");
        Assert.assertEquals(helper.getDocApi().getDoc(docRepoUri + randInt + "/docFromFork3"), "{\"KeyFromChild3\":\"ValueFromChild3\"}");
        Assert.assertEquals(helper.getDocApi().getDoc(docRepoUri + randInt + "/docFromFork4"), "{\"KeyFromChild4\":\"ValueFromChild4\"}");

    }

    @Test(groups = { "decision", "nightly" })
    public void testWorkflowCancelExecution() {

        // Setup the overall workflow step object
        List<Step> workflowSteps = new ArrayList<Step>();
        // define parent step
        Step s1 = new Step();
        s1.setName("step1");
        s1.setDescription("step 1");
        s1.setExecutable(scriptMap.get("sleepStep.rfx"));
        Transition trans1 = new Transition();
        trans1.setName("next");
        trans1.setTargetStep("step2");
        s1.setTransitions(ImmutableList.of(trans1));
        workflowSteps.add(s1);

        // forked subparent1 step 2
        Step s2 = new Step();
        s2.setName("step2");
        s2.setDescription("step 2");
        s2.setExecutable(scriptMap.get("sleepStep.rfx"));
        Transition trans2 = new Transition();
        trans2.setName("next");
        trans2.setTargetStep("step3");
        s2.setTransitions(ImmutableList.of(trans2));
        workflowSteps.add(s2);

        // forked subparent1 step 3
        Step s3 = new Step();
        s3.setName("step3");
        s3.setDescription("step 3");
        s3.setExecutable(scriptMap.get("sleepEnd.rfx"));
        Transition trans3 = new Transition();
        trans3.setName("ok");
        trans3.setTargetStep("$RETURN:done");
        s3.setTransitions(ImmutableList.of(trans3));
        workflowSteps.add(s3);

        ////////////////////////////////////////////////////////////////////////
        // create workflow and setup with params
        Workflow simpleWorkflow = new Workflow();
        simpleWorkflow.setStartStep("step1");
        simpleWorkflow.setSteps(workflowSteps);

        String wfURI = "workflow://" + workFlowPrefix + "/nightly/simpleWorkflow";

        simpleWorkflow.setWorkflowURI(wfURI);

        Reporter.log("Created workflow: " + wfURI, true);
        decisionApi.putWorkflow(simpleWorkflow);
        workflowList.add(wfURI);

        String woUri = decisionApi.createWorkOrder(simpleWorkflow.getWorkflowURI(), new HashMap<String, String>());
        Reporter.log("Work order uri: " + woUri, true);
        int numRetries = 0;
        long waitTimeMS = 1000;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi, woUri) && numRetries < 20) {
            Reporter.log("Checking workorder status, retry count=" + numRetries + ", waiting " + (waitTimeMS / 1000) + " seconds...", true);
            try {
                Thread.sleep(waitTimeMS);
                Reporter.log("Cancelling " + woUri, true);
                decisionApi.cancelWorkOrder(woUri);
            } catch (Exception e) {
            }
            numRetries++;
        }
        Reporter.log("Verifying cancelled status of " + woUri, true);
        Assert.assertEquals(decisionApi.getWorkOrderStatus(woUri).getStatus(), WorkOrderExecutionState.CANCELLED,
                "We did not get a status of cancelled back, we got > " + decisionApi.getWorkOrderStatus(woUri).getStatus());
        

        WorkOrderDebug woDebug = decisionApi.getWorkOrderDebug(woUri);
        Map <String, Object> woMap=JacksonUtil.getMapFromJson(woDebug.toString());
        Assert.assertEquals(((Map <String, Object>)woMap.get("order")).get("workOrderURI").toString(),woUri);
        Assert.assertNotNull(((Map <String, Object>)woMap.get("order")).get("workerIds"));
        
        List<WorkerDebug> woDebugsList=woDebug.getWorkerDebugs();
        for (WorkerDebug wd : woDebugsList) {
            Reporter.log(wd.toString(), true);
            Map <String, Object> workerMap=JacksonUtil.getMapFromJson(wd.toString());
            for (Map<String,Object> currStepRecordDebug : (List <Map<String, Object>>)workerMap.get("stepRecordDebugs")) {

            	Object currStepRecord=((Map<String,Object>)currStepRecordDebug).get("stepRecord");
            	Map<String,Object> stepRecordMap =(Map<String,Object>)currStepRecord;
            	Assert.assertTrue(stepRecordMap.keySet().containsAll(Arrays.asList(new String[]{"stepURI","name","startTime","endTime"})));
            }
        }
    }

    @Test(groups = { "decision", "nightly" })
    public void testResumeExecution() {

        // Setup the overall workflow step object
        List<Step> workflowSteps = new ArrayList<Step>();
        // define parent step
        Step s1 = new Step();
        s1.setName("step1");
        s1.setDescription("step 1");
        s1.setExecutable(scriptMap.get("sleepStep.rfx"));
        Transition trans1 = new Transition();
        trans1.setName("next");
        trans1.setTargetStep("step2");
        s1.setTransitions(ImmutableList.of(trans1));
        workflowSteps.add(s1);

        // forked subparent1 step 2
        Step s2 = new Step();
        s2.setName("step2");
        s2.setDescription("step 2");
        s2.setExecutable(scriptMap.get("sleepStep.rfx"));
        Transition trans2 = new Transition();
        trans2.setName("next");
        trans2.setTargetStep("step3");
        s2.setTransitions(ImmutableList.of(trans2));
        workflowSteps.add(s2);

        // forked subparent1 step 3
        Step s3 = new Step();
        s3.setName("step3");
        s3.setDescription("step 3");
        s3.setExecutable(scriptMap.get("sleepEnd.rfx"));
        Transition trans3 = new Transition();
        trans3.setName("ok");
        trans3.setTargetStep("$RETURN:done");
        s3.setTransitions(ImmutableList.of(trans3));
        workflowSteps.add(s3);

        ////////////////////////////////////////////////////////////////////////
        // create workflow and setup with params
        Workflow simpleWorkflow = new Workflow();
        simpleWorkflow.setStartStep("step1");
        simpleWorkflow.setSteps(workflowSteps);

        String wfURI = "workflow://" + workFlowPrefix + "/nightly/simpleWorkflow";

        simpleWorkflow.setWorkflowURI(wfURI);

        Reporter.log("Created workflow: " + wfURI, true);
        decisionApi.putWorkflow(simpleWorkflow);
        workflowList.add(wfURI);

        String woUri = decisionApi.createWorkOrder(simpleWorkflow.getWorkflowURI(), new HashMap<String, String>());
        Reporter.log("Work order uri: " + woUri, true);
        int numRetries = 0;
        long waitTimeMS = 1000;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi, woUri) && numRetries < 20) {
            Reporter.log("Checking workorder status, retry count=" + numRetries + ", waiting " + (waitTimeMS / 1000) + " seconds...", true);
            try {
                Thread.sleep(waitTimeMS);
                Reporter.log("Cancelling " + woUri, true);
                decisionApi.cancelWorkOrder(woUri);
            } catch (Exception e) {
            }
            numRetries++;
        }

        Assert.assertEquals(decisionApi.getWorkOrderStatus(woUri).getStatus(), WorkOrderExecutionState.CANCELLED,
                "We did not get a status of cancelled back, we got > " + decisionApi.getWorkOrderStatus(woUri).getStatus());
        WorkOrderDebug woDebug = decisionApi.getWorkOrderDebug(woUri);
        Map <String, Object> woMap=JacksonUtil.getMapFromJson(woDebug.toString());
        Assert.assertEquals(((Map <String, Object>)woMap.get("order")).get("workOrderURI").toString(),woUri);
        Assert.assertNotNull(((Map <String, Object>)woMap.get("order")).get("workerIds"));
        List<WorkerDebug> workerList = woDebug.getWorkerDebugs();
        String resumeStepURI = "";
        for (WorkerDebug runWorker : workerList) {
            Reporter.log("Finding step records in " + woUri, true);
            List<StepRecordDebug> stepRecords = runWorker.getStepRecordDebugs();
            for (StepRecordDebug stepRecord : stepRecords) {

                resumeStepURI = stepRecord.getStepRecord().getStepURI().toString();
            }

        }

        // we need to review this process as it is generating a new WO
        Reporter.log("Resuming at " + resumeStepURI, true);
        String resumeURI = decisionApi.resumeWorkOrder(woUri, resumeStepURI).getUri();
        Reporter.log("Created new work order " + resumeURI, true);
        numRetries = 0;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi, resumeURI) && numRetries < 20) {
            Reporter.log("Checking workorder status, retry count=" + numRetries + ", waiting " + (waitTimeMS / 1000) + " seconds...", true);
            try {
                Thread.sleep(waitTimeMS);
            } catch (Exception e) {
            }
            numRetries++;
        }
        woDebug = decisionApi.getWorkOrderDebug(resumeURI);
        List<WorkerDebug> woDebugsList=woDebug.getWorkerDebugs();
        for (WorkerDebug wd : woDebugsList) {
            Reporter.log(wd.toString(), true);
            Map <String, Object> workerMap=JacksonUtil.getMapFromJson(wd.toString());
            for (Map<String,Object> currStepRecordDebug : (List <Map<String, Object>>)workerMap.get("stepRecordDebugs")) {

            	Object currStepRecord=((Map<String,Object>)currStepRecordDebug).get("stepRecord");
            	Map<String,Object> stepRecordMap =(Map<String,Object>)currStepRecord;
            	Assert.assertTrue(stepRecordMap.keySet().containsAll(Arrays.asList(new String[]{"stepURI","name","startTime","endTime"})));
            }
        }
        Assert.assertEquals(decisionApi.getWorkOrderStatus(resumeURI).getStatus(), WorkOrderExecutionState.FINISHED,
                "We did not get a status of finsihed back, we got > " + decisionApi.getWorkOrderStatus(resumeURI).getStatus());

    }

    @Test(groups = { "decision", "nightly" })
    public void testWorkorderWithFailingFirstStep() {

        List<Step> workflowSteps = new ArrayList<Step>();
        // create step 1
        Step s1 = new Step();
        s1.setName("step1");
        s1.setDescription("basicwffail_step1");
        s1.setExecutable(scriptMap.get("step1returnerror.rfx"));
        Transition successTransition = new Transition();
        successTransition.setName("ok");
        successTransition.setTargetStep("step2");
        Transition errorTransition = new Transition();
        errorTransition.setName("error");
        errorTransition.setTargetStep("$FAIL");
        s1.setTransitions(ImmutableList.of(successTransition, errorTransition));
        workflowSteps.add(s1);

        // create step 2
        Step s2 = new Step();
        s2.setName("step2");
        s2.setDescription("basicwffail_step2");
        s2.setExecutable(scriptMap.get("step2returnok.rfx"));
        Transition errorTransition2 = new Transition();
        errorTransition2.setName("error");
        errorTransition2.setTargetStep("$FAIL");
        s2.setTransitions(ImmutableList.of(errorTransition2));
        workflowSteps.add(s2);

        // create workflow and run it
        Workflow wfFailStep1 = new Workflow();
        wfFailStep1.setStartStep("step1");
        wfFailStep1.setSteps(workflowSteps);
        String wfURI = "workflow://" + workFlowPrefix + "/failing/failingWorkflowStep1";
        wfFailStep1.setWorkflowURI(wfURI);
        decisionApi.putWorkflow(wfFailStep1);
        workflowList.add(wfURI);
        Reporter.log("Created workflow: " + wfURI, true);
        // not sure we need params for this test.
        Map<String, String> params = new HashMap<String, String>();
        params.put("caller", "DecisionProcessFailingTests");
        String woUri = decisionApi.createWorkOrder(wfFailStep1.getWorkflowURI(), params);

        Reporter.log("Work order uri: " + woUri, true);
        int numRetries = 0;
        long waitTimeMS = 1000;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi, woUri) && numRetries < 20) {
            Reporter.log("Checking workorder status, retry count=" + numRetries + ", waiting " + (waitTimeMS / 1000) + " seconds...", true);
            try {
                Thread.sleep(waitTimeMS);
            } catch (Exception e) {
            }
            numRetries++;
        }
        WorkOrderDebug woDebug = decisionApi.getWorkOrderDebug(woUri);
        Map <String, Object> woMap=JacksonUtil.getMapFromJson(woDebug.toString());
        Assert.assertEquals(((Map <String, Object>)woMap.get("order")).get("workOrderURI").toString(),woUri);
        Assert.assertNotNull(((Map <String, Object>)woMap.get("order")).get("workerIds"));
        
        List<WorkerDebug> woDebugsList=woDebug.getWorkerDebugs();
        for (WorkerDebug wd : woDebugsList) {
            Reporter.log(wd.toString(), true);
            Map <String, Object> workerMap=JacksonUtil.getMapFromJson(wd.toString());
            for (Map<String,Object> currStepRecordDebug : (List <Map<String, Object>>)workerMap.get("stepRecordDebugs")) {

            	Object currStepRecord=((Map<String,Object>)currStepRecordDebug).get("stepRecord");
            	Map<String,Object> stepRecordMap =(Map<String,Object>)currStepRecord;
            	Assert.assertTrue(stepRecordMap.keySet().containsAll(Arrays.asList(new String[]{"stepURI","name","startTime","endTime"})));
            }
        }

        // Test state of workorder, step and worker
        Assert.assertEquals(woDebug.getWorkerDebugs().get(0).getStepRecordDebugs().size(), 1, "only first step should be exercised.");
        Assert.assertEquals(decisionApi.getWorkOrderStatus(woUri).getStatus(), WorkOrderExecutionState.ERROR, "Overall work order status");
        Assert.assertEquals(woDebug.getWorkerDebugs().get(0).getStepRecordDebugs().get(0).getStepRecord().getStatus(), WorkOrderExecutionState.ERROR,
                "Step1 State");
        Assert.assertEquals(decisionApi.getWorker(woUri, "0").getStatus(), WorkerExecutionState.ERROR, "Worker state");
    }

    @Test(groups = { "decision", "nightly" })
    public void workorderWithReflexErrorStep1Test() {

        List<Step> workflowSteps = new ArrayList<Step>();
        // create step 1
        Step s1 = new Step();
        s1.setName("step1");
        s1.setDescription("wfreflexfail_step1");
        s1.setExecutable(scriptMap.get("step1ReflexIssue.rfx"));
        Transition successTransition = new Transition();
        successTransition.setName("ok");
        successTransition.setTargetStep("step2");
        Transition errorTransition = new Transition();
        errorTransition.setName("error");
        errorTransition.setTargetStep("$FAIL");
        s1.setTransitions(ImmutableList.of(successTransition, errorTransition));
        workflowSteps.add(s1);

        // create step 2
        Step s2 = new Step();
        s2.setName("step2");
        s2.setDescription("wfreflexfail_step2");
        s2.setExecutable(scriptMap.get("step2returnok.rfx"));
        Transition errorTransition2 = new Transition();
        errorTransition2.setName("error");
        errorTransition2.setTargetStep("$FAIL");
        s2.setTransitions(ImmutableList.of(errorTransition2));
        workflowSteps.add(s2);

        // create workflow and run it
        Workflow wfFailStep1 = new Workflow();
        wfFailStep1.setStartStep("step1");
        wfFailStep1.setSteps(workflowSteps);

        String wfURI = "workflow://" + workFlowPrefix + "/failing/failingWorkflowReflexStep1";
        wfFailStep1.setWorkflowURI(wfURI);
        decisionApi.putWorkflow(wfFailStep1);
        workflowList.add(wfURI);
        Reporter.log("Created workflow: " + wfURI, true);
        // not sure we need params for this test.
        Map<String, String> params = new HashMap<String, String>();
        params.put("caller", "DecisionProcessFailingTests");
        String woUri = decisionApi.createWorkOrder(wfFailStep1.getWorkflowURI(), params);

        Reporter.log("Work order uri: " + woUri, true);
        int numRetries = 0;
        long waitTimeMS = 1000;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi, woUri) && numRetries < 20) {
            Reporter.log("Checking workorder status, retry count=" + numRetries + ", waiting " + (waitTimeMS / 1000) + " seconds...", true);
            try {
                Thread.sleep(waitTimeMS);
            } catch (Exception e) {
            }
            numRetries++;
        }

        WorkOrderDebug woDebug = decisionApi.getWorkOrderDebug(woUri);
        Map <String, Object> woMap=JacksonUtil.getMapFromJson(woDebug.toString());
        Assert.assertEquals(((Map <String, Object>)woMap.get("order")).get("workOrderURI").toString(),woUri);
        Assert.assertNotNull(((Map <String, Object>)woMap.get("order")).get("workerIds"));
        
        List<WorkerDebug> woDebugsList=woDebug.getWorkerDebugs();
        for (WorkerDebug wd : woDebugsList) {
            Reporter.log(wd.toString(), true);
            Map <String, Object> workerMap=JacksonUtil.getMapFromJson(wd.toString());
            for (Map<String,Object> currStepRecordDebug : (List <Map<String, Object>>)workerMap.get("stepRecordDebugs")) {

            	Object currStepRecord=((Map<String,Object>)currStepRecordDebug).get("stepRecord");
            	Map<String,Object> stepRecordMap =(Map<String,Object>)currStepRecord;
            	Assert.assertTrue(stepRecordMap.keySet().containsAll(Arrays.asList(new String[]{"stepURI","name","startTime","endTime"})));
            }
        }

        // Test state of workorder, step and worker
        Assert.assertEquals(woDebug.getWorkerDebugs().get(0).getStepRecordDebugs().size(), 1, "only first step should be exercised.");
        Assert.assertEquals(decisionApi.getWorkOrderStatus(woUri).getStatus(), WorkOrderExecutionState.ERROR, "Overall work order status");
        Assert.assertEquals(woDebug.getWorkerDebugs().get(0).getStepRecordDebugs().get(0).getStepRecord().getStatus(), WorkOrderExecutionState.ERROR,
                "Step1 State");
        Assert.assertEquals(decisionApi.getWorker(woUri, "0").getStatus(), WorkerExecutionState.ERROR, "Worker state");
    }

    @Test(groups = { "decision", "nightly" })
    public void testWorkorderWithStep2ErrorTransitionReturnsOk() {

        List<Step> workflowSteps = new ArrayList<Step>();
        // create step 1
        Step s1 = new Step();
        s1.setName("step1");
        s1.setDescription("step1");
        s1.setExecutable(scriptMap.get("step1returnok.rfx"));
        Transition successTransition = new Transition();
        successTransition.setName("ok");
        successTransition.setTargetStep("step2");
        Transition errorTransition1 = new Transition();
        errorTransition1.setName("error");
        errorTransition1.setTargetStep("step3ErrorHandler");
        s1.setTransitions(ImmutableList.of(successTransition, errorTransition1));
        workflowSteps.add(s1);

        // create step 2
        Step s2 = new Step();
        s2.setName("step2");
        s2.setDescription("step2");
        s2.setExecutable(scriptMap.get("step2returnerror.rfx"));
        Transition errorTransition2 = new Transition();
        errorTransition2.setName("error");
        errorTransition2.setTargetStep("step3ErrorHandler");
        s2.setTransitions(ImmutableList.of(errorTransition2));
        workflowSteps.add(s2);

        // create error handling step
        Step s3Error = new Step();
        s3Error.setName("step3ErrorHandler");
        s3Error.setDescription("error handler");
        s3Error.setExecutable(scriptMap.get("stepErrorHandler1ReturnOk.rfx"));
        workflowSteps.add(s3Error);

        Workflow wfFailStep1 = new Workflow();
        wfFailStep1.setStartStep("step1");
        wfFailStep1.setSteps(workflowSteps);

        String wfURI = "workflow://" + workFlowPrefix + "/failing/failingWorkflowReflexStep1";
        wfFailStep1.setWorkflowURI(wfURI);
        decisionApi.putWorkflow(wfFailStep1);
        workflowList.add(wfURI);
        Reporter.log("Created workflow: " + wfURI, true);

        // not sure we need params for this test.
        Map<String, String> params = new HashMap<String, String>();
        params.put("caller", "DecisionProcessFailingTests");
        String woUri = decisionApi.createWorkOrder(wfFailStep1.getWorkflowURI(), params);
        Reporter.log("Created work order: " + woUri);

        int numRetries = 0;
        long waitTimeMS = 2000;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi, woUri) && numRetries < 20) {
            Reporter.log("Checking workorder status, retry count=" + numRetries + ", waiting " + (waitTimeMS / 1000) + " seconds...", true);
            try {
                Thread.sleep(waitTimeMS);
            } catch (Exception e) {
            }
            numRetries++;
        }
        WorkOrderDebug woDebug = decisionApi.getWorkOrderDebug(woUri);
        Map <String, Object> woMap=JacksonUtil.getMapFromJson(woDebug.toString());
        Assert.assertEquals(((Map <String, Object>)woMap.get("order")).get("workOrderURI").toString(),woUri);
        Assert.assertNotNull(((Map <String, Object>)woMap.get("order")).get("workerIds"));
        Worker worker = decisionApi.getWorker(woUri, "0");
        Reporter.log("Checking context value from last step", true);
        String contextValue = decisionApi.getContextValue(worker.getWorkOrderURI(), "retValFromErrorHandler");
        Assert.assertEquals(contextValue, "ugh");
        
        List<WorkerDebug> woDebugsList=woDebug.getWorkerDebugs();
        for (WorkerDebug wd : woDebugsList) {
            Reporter.log(wd.toString(), true);
            Map <String, Object> workerMap=JacksonUtil.getMapFromJson(wd.toString());
            for (Map<String,Object> currStepRecordDebug : (List <Map<String, Object>>)workerMap.get("stepRecordDebugs")) {

            	Object currStepRecord=((Map<String,Object>)currStepRecordDebug).get("stepRecord");
            	Map<String,Object> stepRecordMap =(Map<String,Object>)currStepRecord;
            	Assert.assertTrue(stepRecordMap.keySet().containsAll(Arrays.asList(new String[]{"stepURI","name","startTime","endTime"})));
            }
        }

        // Test state of workorder, step and worker
        Assert.assertEquals(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().size(), 3, "3 steps should be run.");
        Assert.assertEquals(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().get(0).getStepRecord().getStatus(),
                WorkOrderExecutionState.FINISHED, "Step1 State");
        Assert.assertEquals(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().get(0).getStepRecord().getName(), "step1",
                "check correct step was run.");

        Assert.assertEquals(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().get(1).getStepRecord().getStatus(),
                WorkOrderExecutionState.FINISHED, "Step2 State");
        Assert.assertEquals(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().get(1).getStepRecord().getName(), "step2",
                "check correct step was run.");

        Assert.assertEquals(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().get(2).getStepRecord().getStatus(),
                WorkOrderExecutionState.FINISHED, "step3ErrorHandler State");
        Assert.assertEquals(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().get(2).getStepRecord().getName(),
                "step3ErrorHandler", "check correct step was run.");

        Assert.assertEquals(worker.getStatus(), WorkerExecutionState.FINISHED, "Worker state");
        Assert.assertEquals(decisionApi.getWorkOrderStatus(woUri).getStatus(), WorkOrderExecutionState.FINISHED, "Overall work order status");
    }

    @Test(groups = { "decision", "nightly" })
    public void testWorkorderWithStep2ErrorTransitionReturnsFAIL() {

        List<Step> workflowSteps = new ArrayList<Step>();
        // create step 1
        Step s1 = new Step();
        s1.setName("step1");
        s1.setDescription("step1");
        s1.setExecutable(scriptMap.get("step1returnok.rfx"));
        Transition successTransition = new Transition();
        successTransition.setName("ok");
        successTransition.setTargetStep("step2");
        Transition errorTransition1 = new Transition();
        errorTransition1.setName("error");
        errorTransition1.setTargetStep("step3ErrorHandler");
        s1.setTransitions(ImmutableList.of(successTransition, errorTransition1));
        workflowSteps.add(s1);

        // create step 2
        Step s2 = new Step();
        s2.setName("step2");
        s2.setDescription("step2");
        s2.setExecutable(scriptMap.get("step2returnerror.rfx"));
        Transition errorTransition2 = new Transition();
        errorTransition2.setName("error");
        errorTransition2.setTargetStep("step3ErrorHandler");
        s2.setTransitions(ImmutableList.of(errorTransition2));
        workflowSteps.add(s2);

        // create error handling step
        Step s3Error = new Step();
        s3Error.setName("step3ErrorHandler");
        s3Error.setDescription("error handler");
        s3Error.setExecutable(scriptMap.get("stepErrorHandler1ReturnFAIL.rfx"));
        // add a $FAIL transition step that fails the wf on the return "" from reflex script.
        Transition errorTransition3 = new Transition();
        errorTransition3.setName("failworkflowonerror");
        errorTransition3.setTargetStep("$FAIL");
        s3Error.setTransitions(ImmutableList.of(errorTransition3));
        workflowSteps.add(s3Error);

        Workflow wfFailStep1 = new Workflow();
        wfFailStep1.setStartStep("step1");
        wfFailStep1.setSteps(workflowSteps);
        String wfURI = "workflow://" + workFlowPrefix + "/failing/failingStep2WithErrorTransitionReturnFAIL";
        wfFailStep1.setWorkflowURI(wfURI);
        decisionApi.putWorkflow(wfFailStep1);
        workflowList.add(wfURI);
        Reporter.log("Created workflow: " + wfURI, true);

        // not sure we need params for this test.
        Map<String, String> params = new HashMap<String, String>();
        params.put("caller", "DecisionProcessFailingTests");
        String woUri = decisionApi.createWorkOrder(wfFailStep1.getWorkflowURI(), params);
        Reporter.log("Created work order: " + woUri, true);

        int numRetries = 0;
        long waitTimeMS = 2000;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi, woUri) && numRetries < 20) {
            Reporter.log("Checking workorder status, retry count=" + numRetries + ", waiting " + (waitTimeMS / 1000) + " seconds...", true);
            try {
                Thread.sleep(waitTimeMS);
            } catch (Exception e) {
            }
            numRetries++;
        }
        WorkOrderDebug woDebug = decisionApi.getWorkOrderDebug(woUri);
        Map <String, Object> woMap=JacksonUtil.getMapFromJson(woDebug.toString());
        Assert.assertEquals(((Map <String, Object>)woMap.get("order")).get("workOrderURI").toString(),woUri);
        Assert.assertNotNull(((Map <String, Object>)woMap.get("order")).get("workerIds"));
        
        List<WorkerDebug> woDebugsList=woDebug.getWorkerDebugs();
        for (WorkerDebug wd : woDebugsList) {
            Reporter.log(wd.toString(), true);
            Map <String, Object> workerMap=JacksonUtil.getMapFromJson(wd.toString());
            for (Map<String,Object> currStepRecordDebug : (List <Map<String, Object>>)workerMap.get("stepRecordDebugs")) {

            	Object currStepRecord=((Map<String,Object>)currStepRecordDebug).get("stepRecord");
            	Map<String,Object> stepRecordMap =(Map<String,Object>)currStepRecord;
            	Assert.assertTrue(stepRecordMap.keySet().containsAll(Arrays.asList(new String[]{"stepURI","name","startTime","endTime"})));
            }
        }
        Worker worker = decisionApi.getWorker(woUri, "0");

        Reporter.log("Checking workorder results", true);
        // get the context variable set in last step of workflow and ensure its passed correctly back to calling program i.e. this one.
        String contextValue = decisionApi.getContextValue(worker.getWorkOrderURI(), "retValFromErrorHandler");
        Assert.assertEquals(contextValue, "ugh");

        // Test state of workorder, step and worker
        Assert.assertEquals(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().size(), 3, "3 steps should have been run.");
        // check step 1 execution
        Assert.assertEquals(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().get(0).getStepRecord().getStatus(),
                WorkOrderExecutionState.FINISHED, "Step1 State");
        Assert.assertEquals(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().get(0).getStepRecord().getName(), "step1",
                "check correct step was run.");
        Assert.assertEquals(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().get(0).getStepRecord().getStepURI(),
                wfURI + "#step1", "Check step1 uri");
        Assert.assertEquals(decisionApi
                .getWorkflowStep(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().get(0).getStepRecord().getStepURI())
                .getExecutable(), scriptMap.get("step1returnok.rfx"), "check step 0 executable for worker 0");
        // check step 2 execution
        Assert.assertEquals(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().get(1).getStepRecord().getStatus(),
                WorkOrderExecutionState.FINISHED, "Step2 State");
        Assert.assertEquals(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().get(1).getStepRecord().getName(), "step2",
                "check correct step was run.");
        Assert.assertEquals(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().get(1).getStepRecord().getStepURI(),
                wfURI + "#step2", "Check step2 uri");
        Assert.assertEquals(decisionApi
                .getWorkflowStep(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().get(1).getStepRecord().getStepURI())
                .getExecutable(), scriptMap.get("step2returnerror.rfx"), "check step 1 executable for worker 0");
        // check step 3 (error handler) execution
        Assert.assertEquals(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().get(2).getStepRecord().getStatus(),
                WorkOrderExecutionState.ERROR, "step3ErrorHandler State");
        Assert.assertEquals(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().get(2).getStepRecord().getName(),
                "step3ErrorHandler", "check correct step was run.");
        Assert.assertEquals(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().get(2).getStepRecord().getStepURI(),
                wfURI + "#step3ErrorHandler", "Check step2 uri");
        Assert.assertEquals(decisionApi
                .getWorkflowStep(decisionApi.getWorkOrderDebug(woUri).getWorkerDebugs().get(0).getStepRecordDebugs().get(2).getStepRecord().getStepURI())
                .getExecutable(), scriptMap.get("stepErrorHandler1ReturnFAIL.rfx"), "check step 1 executable for worker 0");

        // overall workflow status
        Assert.assertEquals(decisionApi.getWorkOrderStatus(woUri).getStatus().name(), "ERROR", "Check overall work order status");
    }

    @Test(groups = { "decision", "nightly" })
    public void testSimplePropertyBasedLocking() {

        List<Step> workflowSteps = new ArrayList<Step>();
        // create step 1
        Step s1 = new Step();
        s1.setName("step1");
        s1.setDescription("basicwfproplock_step1");
        s1.setExecutable(scriptMap.get("step1.rfx"));
        Transition successTransition = new Transition();
        successTransition.setName("ok");
        successTransition.setTargetStep("step2");
        Transition errorTransition = new Transition();
        errorTransition.setName("error");
        errorTransition.setTargetStep("$FAIL");
        s1.setTransitions(ImmutableList.of(successTransition, errorTransition));
        workflowSteps.add(s1);

        // create step 2
        Step s2 = new Step();
        s2.setName("step2");
        s2.setDescription("basicwfproplock_step2");
        s2.setExecutable(scriptMap.get("step2.rfx"));
        Transition successTransition2 = new Transition();
        successTransition2.setName("ok");
        successTransition2.setTargetStep("$RETURN");
        Transition errorTransition2 = new Transition();
        errorTransition2.setName("error");
        errorTransition2.setTargetStep("$FAIL");
        s2.setTransitions(ImmutableList.of(successTransition2, errorTransition2));
        workflowSteps.add(s2);

        // create workflow and run it
        Workflow wfPropLock = new Workflow();
        wfPropLock.setStartStep("step1");
        wfPropLock.setSteps(workflowSteps);
        String wfURI = "workflow://" + workFlowPrefix + "/locking/simplePropertyBasedLocking1";
        wfPropLock.setWorkflowURI(wfURI);
        wfPropLock.setSemaphoreType(SemaphoreType.PROPERTY_BASED);
        String lockPropName = "WF_LOCK_ID" + System.currentTimeMillis();
        wfPropLock.setSemaphoreConfig("{\"maxAllowed\":1, \"timeout\":5, \"propertyName\":\"" + lockPropName + "\"}");

        decisionApi.putWorkflow(wfPropLock);
        workflowList.add(wfURI);
        Reporter.log("Created workflow: " + wfURI, true);
        Map<String, String> params = new HashMap<String, String>();
        params.put("caller", "simplePropertyBasedLocking1");
        params.put(lockPropName, "QA");
        String woUri = decisionApi.createWorkOrder(wfPropLock.getWorkflowURI(), params);
        Reporter.log("Work order uri: " + woUri, true);
        int numRetries = 0;
        long waitTimeMS = 2000;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi, woUri) && numRetries < 20) {
            Reporter.log("Checking workorder status, retry count=" + numRetries + ", waiting " + (waitTimeMS / 1000) + " seconds...", true);
            try {
                Thread.sleep(waitTimeMS);
            } catch (Exception e) {
            }
            numRetries++;
        }

        // First wf finished
        Assert.assertEquals(decisionApi.getWorkOrderStatus(woUri).getStatus().name(), "FINISHED", "Overall work order status");

        // run it again
        String woUri2 = decisionApi.createWorkOrder(wfPropLock.getWorkflowURI(), params);
        Reporter.log("Work order uri: " + woUri2, true);
        Assert.assertNotNull(woUri2, " should be not be null i.e. the second run of workflow can acquire a lock.");

        // wait for second wf to finish
        numRetries = 0;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi, woUri2) && numRetries < 20) {
            Reporter.log("Checking workorder status, retry count=" + numRetries + ", waiting " + (waitTimeMS / 1000) + " seconds...", true);
            try {
                Thread.sleep(waitTimeMS);
            } catch (Exception e) {
            }
            numRetries++;
        }

        // second wf finished
        Assert.assertEquals(decisionApi.getWorkOrderStatus(woUri2).getStatus().name(), "FINISHED", "Overall work order status");
        
        WorkOrderDebug woDebug = decisionApi.getWorkOrderDebug(woUri2);
        Map <String, Object> woMap=JacksonUtil.getMapFromJson(woDebug.toString());
        Assert.assertEquals(((Map <String, Object>)woMap.get("order")).get("workOrderURI").toString(),woUri2);
        Assert.assertNotNull(((Map <String, Object>)woMap.get("order")).get("workerIds"));
        
        List<WorkerDebug> woDebugsList=woDebug.getWorkerDebugs();
        for (WorkerDebug wd : woDebugsList) {
            Reporter.log(wd.toString(), true);
            Map <String, Object> workerMap=JacksonUtil.getMapFromJson(wd.toString());
            for (Map<String,Object> currStepRecordDebug : (List <Map<String, Object>>)workerMap.get("stepRecordDebugs")) {

            	Object currStepRecord=((Map<String,Object>)currStepRecordDebug).get("stepRecord");
            	Map<String,Object> stepRecordMap =(Map<String,Object>)currStepRecord;
            	Assert.assertTrue(stepRecordMap.keySet().containsAll(Arrays.asList(new String[]{"stepURI","name","startTime","endTime"})));
            }
        }
    }

    @Test(groups = { "decision", "nightly" })
    public void testLiteralContextVariableTest() {

        List<Step> workflowSteps = new ArrayList<Step>();

        Step s1 = new Step();
        s1.setName("startStep");
        s1.setDescription("start workflow step");
        s1.setExecutable(scriptMap.get("contextStartStep"));
        Transition successTransition = new Transition();
        successTransition.setName("ok");
        successTransition.setTargetStep("secondStep");
        s1.setTransitions(ImmutableList.of(successTransition));
        workflowSteps.add(s1);

        // second step
        Step s2 = new Step();
        s2.setName("secondStep");
        s2.setDescription("second workflow step");
        s2.setExecutable(scriptMap.get("contextSecondStep"));
        successTransition = new Transition();
        successTransition.setName("ok");
        successTransition.setTargetStep("thirdStep");
        s2.setTransitions(ImmutableList.of(successTransition));
        workflowSteps.add(s2);

        // third step contains the return; statement
        Step s3 = new Step();
        s3.setName("thirdStep");
        s3.setDescription("third workflow step");
        s3.setExecutable(scriptMap.get("contextThirdStep"));
        successTransition = new Transition();
        successTransition.setName("ok");
        successTransition.setTargetStep("fourthStep");
        s3.setTransitions(ImmutableList.of(successTransition));
        workflowSteps.add(s3);

        Step s4 = new Step();
        s4.setName("fourthStep");
        s4.setDescription("fourth workflow step");
        s4.setExecutable(scriptMap.get("contextFourthStep"));
        successTransition = new Transition();
        successTransition.setName("ok");
        successTransition.setTargetStep("finalStep");
        s4.setTransitions(ImmutableList.of(successTransition));
        workflowSteps.add(s4);

        // final step
        Step s5 = new Step();
        s5.setName("finalStep");
        s5.setDescription("final workflow step");
        s5.setExecutable(scriptMap.get("contextFinalStep"));
        // create workflow
        Workflow complex = new Workflow();
        complex.setStartStep("startStep");
        complex.setSteps(workflowSteps);

        String wfURI = "workflow://" + workFlowPrefix + "/contextTest/test1";
        complex.setWorkflowURI(wfURI);
        decisionApi.putWorkflow(complex);
        workflowList.add(wfURI);

        Map<String, String> params = new HashMap<String, String>();
        params.put("workflowURI", wfURI);
        params.put("caller", "contextTestWorkflow");
        String woUri = decisionApi.createWorkOrder(wfURI, params);
        Reporter.log("Work order uri: " + woUri, true);
        int numRetries = 0;
        long waitTimeMS = 2000;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi, woUri) && numRetries < 20) {
            Reporter.log("Checking workorder status, retry count=" + numRetries + ", waiting " + (waitTimeMS / 1000) + " seconds...", true);
            try {
                Thread.sleep(waitTimeMS);
            } catch (Exception e) {
            }
            numRetries++;
        }
        Assert.assertEquals(decisionApi.getWorkOrderStatus(woUri).getStatus(), WorkOrderExecutionState.ERROR,
                "We did not get a status of ERROR back, we got > " + decisionApi.getWorkOrderStatus(woUri).getStatus());
        
        WorkOrderDebug woDebug = decisionApi.getWorkOrderDebug(woUri);
        Map <String, Object> woMap=JacksonUtil.getMapFromJson(woDebug.toString());
        Assert.assertEquals(((Map <String, Object>)woMap.get("order")).get("workOrderURI").toString(),woUri);
        Assert.assertNotNull(((Map <String, Object>)woMap.get("order")).get("workerIds"));
        
        List<WorkerDebug> woDebugsList=woDebug.getWorkerDebugs();
        for (WorkerDebug wd : woDebugsList) {
            Reporter.log(wd.toString(), true);
            Map <String, Object> workerMap=JacksonUtil.getMapFromJson(wd.toString());
            for (Map<String,Object> currStepRecordDebug : (List <Map<String, Object>>)workerMap.get("stepRecordDebugs")) {

            	Object currStepRecord=((Map<String,Object>)currStepRecordDebug).get("stepRecord");
            	Map<String,Object> stepRecordMap =(Map<String,Object>)currStepRecord;
            	Assert.assertTrue(stepRecordMap.keySet().containsAll(Arrays.asList(new String[]{"stepURI","name","startTime","endTime"})));
            }
        }
    }

    @Test(groups = { "decision", "nightly" })
    public void testWorkordersWithContextLinks() {
        Map<String, Object> lookup = ImmutableMap.of("foo", "bar", "hope", "anchor", "Bolton", "Wanderers", "word", "association", "association", "football");
        RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, "MONGODB");
        String docPath = new RaptureURI.Builder(repo).docPath("doc" + System.nanoTime()).build().toString();

        Step returnStep = new Step();
        returnStep.setName("firstStep");
        returnStep.setTransitions(Lists.<Transition> newArrayList());
        List<Step> steps = Lists.newArrayList(returnStep);
        
        Workflow contextLiteralLinksWF = new Workflow();
        contextLiteralLinksWF.setStartStep("firstStep");
        contextLiteralLinksWF.setSteps(steps);
        String wfURI = "workflow://" + workFlowPrefix + "/nightly/linkTest";
        contextLiteralLinksWF.setWorkflowURI(wfURI);
        decisionApi.putWorkflow(contextLiteralLinksWF);
        workflowList.add(wfURI);
        
        String workOrderUri = decisionApi.createWorkOrder(wfURI, new HashMap<String,String>());
        int numRetries = 0;
        long waitTimeMS = 1000;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi, workOrderUri) && numRetries < 20) {
            Reporter.log("Checking workorder status, retry count=" + numRetries + ", waiting " + (waitTimeMS / 1000) + " seconds...", true);
            try {
                Thread.sleep(waitTimeMS);
            } catch (Exception e) {
            }
            numRetries++;
        }

        helper.getDocApi().putDoc( docPath, JacksonUtil.jsonFromObject(lookup));

        // POSITIVE TESTS
        decisionApi.setContextLink( workOrderUri, "BAR", docPath + "#foo");
        decisionApi.setContextLink( workOrderUri, "FIZZ", docPath + "#Bolton");
        String value = decisionApi.getContextValue(workOrderUri, "BAR");
        Assert.assertEquals(value,"bar");
        value = decisionApi.getContextValue(workOrderUri, "FIZZ");
        Assert.assertEquals(value,"Wanderers");
        
        // NEGATIVE TESTS
        Assert.assertNull(decisionApi.getContextValue(workOrderUri, "LIZZ"));
        decisionApi.setContextLink( workOrderUri, "BIZZ", docPath + "#NONEXIST");
        try {
        	Assert.assertEquals(decisionApi.getContextValue(workOrderUri, "BIZZ"),"");
        } catch (Exception e) {}
        
    }
    
    @Test(groups = { "decision", "nightly" })
    public void testWorkordersWithTemplateTest() {

        Step returnStep = new Step();
        returnStep.setName("firstStep");
        returnStep.setTransitions(Lists.<Transition> newArrayList());
        List<Step> steps = Lists.newArrayList(returnStep);
        
        Workflow contextLiteralLinksWF = new Workflow();
        contextLiteralLinksWF.setStartStep("firstStep");
        contextLiteralLinksWF.setSteps(steps);
        String wfURI = "workflow://" + workFlowPrefix + "/nightly/templateTest";
        contextLiteralLinksWF.setWorkflowURI(wfURI);
        decisionApi.putWorkflow(contextLiteralLinksWF);
        workflowList.add(wfURI);
        
        String workOrderUri = decisionApi.createWorkOrder(wfURI, new HashMap<String,String>());
        int numRetries = 0;
        long waitTimeMS = 1000;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi, workOrderUri) && numRetries < 20) {
            Reporter.log("Checking workorder status, retry count=" + numRetries + ", waiting " + (waitTimeMS / 1000) + " seconds...", true);
            try {
                Thread.sleep(waitTimeMS);
            } catch (Exception e) {
            }
            numRetries++;
        }
         
        decisionApi.setContextLiteral( workOrderUri, "MANCHESTER", "Bolton");
        decisionApi.setContextLiteral( workOrderUri, "UNITED", "Wanderers");
        decisionApi.setContextLiteral( workOrderUri, "TEAM", "${MANCHESTER} ${UNITED} FOREVER");
        String value = decisionApi.getContextValue( workOrderUri, "TEAM");
        Assert.assertEquals(value,"${MANCHESTER} ${UNITED} FOREVER");
        decisionApi.setContextTemplate( workOrderUri, "TEAM", "${MANCHESTER} ${UNITED} FOREVER");
        value = decisionApi.getContextValue( workOrderUri, "TEAM");
        Assert.assertEquals(value,"Bolton Wanderers FOREVER");
        
        decisionApi.setContextTemplate( workOrderUri, "TEAM2", "${DORCHESTER} ${UNITED} FOREVER");
        try {
        	decisionApi.getContextValue( workOrderUri, "TEAM2");
        	Assert.fail();
        } catch (Exception e) {}
        
        RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, "MONGODB");
        String docPath = new RaptureURI.Builder(repo).docPath("doc" + System.nanoTime()).build().toString();
        String testConent="{\"test\":\"test\"}";
        helper.getDocApi().putDoc( docPath, testConent);
        
        decisionApi.setContextLiteral( workOrderUri, "TESTPATH", docPath);
        decisionApi.setContextTemplate( workOrderUri, "TESTLINK", "${TESTPATH}+SOMETEXT");
        value=decisionApi.getContextValue( workOrderUri, "TESTLINK");
        Assert.assertEquals(value, docPath+"+SOMETEXT");

    }
    
    private void loadScripts(RaptureURI tempScripts) {
        HttpScriptApi scriptApi = helper.getScriptApi();
        String rootPath = System.getProperty("user.dir") + File.separator + "build" + File.separator + "resources" + File.separator + "test" + File.separator
                + "reflex" + File.separator + "decision";
        File[] files = new File(rootPath).listFiles();
        if ((files == null) || (files.length == 0)) {
            Reporter.log("No scripts found in directory " + rootPath, true);
            return;
        }
        for (File file : files) {
            try {
                String scriptName = file.getName();
                String subdirName = file.getParent().substring(file.getParent().lastIndexOf('/') + 1);
                String scriptPath = RaptureURI.builder(tempScripts).docPath(subdirName + "/" + scriptName).asString();
                Reporter.log("Reading in file: " + file.getAbsolutePath(), true);
                if (!scriptApi.doesScriptExist(scriptPath)) {
                    byte[] scriptBytes = Files.readAllBytes(file.toPath());
                    scriptApi.createScript(scriptPath, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, new String(scriptBytes));
                }
                scriptMap.put(file.getName(), scriptPath);
            } catch (IOException e) {
                Assert.fail("Failed loading script: " + file.getAbsolutePath() + "\n" + ExceptionToString.format(e));
            }
        }
    }

    @AfterClass
    public void cleanUp() {
        helper.cleanAllAssets();
        for (String workflowURI : workflowList)
            decisionApi.deleteWorkflow(workflowURI);
    }
}
