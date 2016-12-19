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

package rapture.ftp.step;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import rapture.common.Activity;
import rapture.common.ActivityStatus;
import rapture.common.CreateResponse;
import rapture.common.RaptureScript;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.common.WorkOrderExecutionState;
import rapture.common.client.HttpBlobApi;
import rapture.common.client.HttpDecisionApi;
import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.HttpScriptApi;
import rapture.common.client.SimpleCredentialsProvider;
import rapture.common.dp.Step;
import rapture.common.dp.StepRecord;
import rapture.common.dp.StepRecordDebug;
import rapture.common.dp.Steps;
import rapture.common.dp.Transition;
import rapture.common.dp.WorkOrderDebug;
import rapture.common.dp.WorkerDebug;
import rapture.common.dp.Workflow;
import rapture.common.impl.jackson.JacksonUtil;

public class ConfigurationStepIntTest {

    private static final String auth = "test" + System.currentTimeMillis();
    private static final String BLOB_USING_MONGODB = "BLOB {} USING MONGODB {prefix=\"/tmp/B" + auth + "\"}";
    private static final String META_USING_MONGODB = "REP {} USING MONGODB {prefix=\"/tmp/M" + auth + "\"}";

    private static HttpLoginApi raptureLogin = null;
    private static HttpDocApi docApi = null;
    private static HttpBlobApi blobApi = null;
    private static HttpDecisionApi decisionApi = null;
    private static HttpScriptApi scriptApi = null;
    private static final String testPrefix = "__RESERVED__";
    private static RaptureURI repoUri = null;

    public static void configureTestRepo(RaptureURI repo, String storage) {
        Assert.assertFalse(repo.hasDocPath(), "Doc path not allowed");
        String authString = repo.toAuthString();
        boolean versioned = false;

        switch (repo.getScheme()) {
        case BLOB:
            if (blobApi.blobRepoExists(repo.toAuthString())) blobApi.deleteBlobRepo(authString);
            blobApi.createBlobRepo(authString, "BLOB {} USING " + storage + " {prefix=\"B_" + repo.getAuthority() + "\"}",
                    "NREP {} USING " + storage + " {prefix=\"M_" + repo.getAuthority() + "\"}");
            Assert.assertTrue(blobApi.blobRepoExists(authString), authString + " Create failed");
            break;

        case DOCUMENT:
            if (docApi.docRepoExists(repo.toAuthString())) docApi.deleteDocRepo(authString);
            docApi.createDocRepo(authString,
                    "NREP {} USING " + storage + " {prefix=\"D_" + repo.getAuthority() + (versioned ? "\", separateVersion=\"true" : "") + "\"}");
            Assert.assertTrue(docApi.docRepoExists(authString), authString + " Create failed");
            break;

        default:
            Assert.fail(repo.toString() + " not supported");
        }
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // String url = "http://localhost:8664/rapture";
        // String url = "http://165.193.214.92:8664/rapture";
        String url = "http://192.168.99.100:8665/rapture";

        try {
            raptureLogin = new HttpLoginApi(url, new SimpleCredentialsProvider("rapture", "rapture"));
            raptureLogin.login();
            docApi = new HttpDocApi(raptureLogin);
            blobApi = new HttpBlobApi(raptureLogin);
            decisionApi = new HttpDecisionApi(raptureLogin);
            scriptApi = new HttpScriptApi(raptureLogin);

            defineWorkflow();
        } catch (Exception e) {
            Assume.assumeNoException(e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (blobApi != null) blobApi.deleteBlobRepo("blob://tmp");
        if (docApi != null) docApi.deleteDoc("document://tmp/elp");
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    static String workflowUri = "workflow://workflows/core/testing";

    static private void defineWorkflow() {
        String docUri = "//test/configuration";
        docApi.putDoc(docUri, JacksonUtil.jsonFromObject(ImmutableMap.of("FOO", "BAR")));
        Workflow workflow = new Workflow();
        workflow.setJarUriDependencies(ImmutableList.of("jar://workflows/dynamic/*"));
        workflow.setStartStep("step1");
        String step1Script = "script://matrix/step1";
        if (scriptApi.doesScriptExist(step1Script)) scriptApi.deleteScript(step1Script);

        RaptureScript script1 = new RaptureScript();
        script1.setLanguage(RaptureScriptLanguage.REFLEX);
        script1.setPurpose(RaptureScriptPurpose.PROGRAM);
        script1.setName("step1");
        script1.setScript("workerURI = _params['$DP_WORKER_URI']; \n foo = #decision.getContextValue(workerURI, 'FOO');\n" + "println('foo = '+foo);\n"
                + "return'next';");

        script1.setAuthority("matrix");
        scriptApi.putScript(step1Script, script1);

        String step3Script = "script://matrix/step3";
        if (scriptApi.doesScriptExist(step3Script)) scriptApi.deleteScript(step3Script);

        RaptureScript script3 = new RaptureScript();
        script3.setLanguage(RaptureScriptLanguage.REFLEX);
        script3.setPurpose(RaptureScriptPurpose.PROGRAM);
        script3.setName("step3");
        script3.setScript("workerURI = _params['$DP_WORKER_URI']; \n foo = #decision.getContextValue(workerURI, 'FOO');\n" + "println('now foo = '+foo);\n"
                + "return foo;");

        script3.setAuthority("matrix");
        scriptApi.putScript(step3Script, script3);

        Step step1 = new Step();
        step1.setExecutable(step1Script);
        step1.setName("step1");

        Transition next1 = new Transition();
        next1.setName(Steps.NEXT.toString());
        next1.setTargetStep("step2");
        step1.setTransitions(ImmutableList.of(next1));

        Step step2 = new Step();
        step2.setExecutable("dp_java_invocable://configuration.steps.ConfigurationStep");
        step2.setName("step2");

        Transition next2 = new Transition();
        next2.setName(Steps.NEXT.toString());
        next2.setTargetStep("step3");
        step2.setTransitions(ImmutableList.of(next2));

        Step step3 = new Step();
        step3.setExecutable(step3Script);
        step3.setName("step3");

        Transition next3 = new Transition();
        next3.setName("BAR");
        next3.setTargetStep("$RETURN");
        step3.setTransitions(ImmutableList.of(next3));


        workflow.setSteps(ImmutableList.of(step1, step2, step3));
        workflow.setView(ImmutableMap.of("CONFIGURATION", "#" + docUri));
        workflow.setWorkflowURI(workflowUri);
        decisionApi.putWorkflow(workflow);
    }

    @Test
    public void testConfigurationStep() {

        Map<String, String> args = new HashMap<>();
        CreateResponse response = decisionApi.createWorkOrderP(workflowUri, args, null);
        assertTrue(response.getIsCreated());
        WorkOrderDebug debug;
        WorkOrderExecutionState state = WorkOrderExecutionState.NEW;
        long timeout = System.currentTimeMillis() + 60000;
        do {
            debug = decisionApi.getWorkOrderDebug(response.getUri());
            state = debug.getOrder().getStatus();
        } while (((state == WorkOrderExecutionState.NEW) || (state == WorkOrderExecutionState.ACTIVE)) && (System.currentTimeMillis() < timeout));

        // If anything went wrong

        for (WorkerDebug db : debug.getWorkerDebugs()) {
            for (StepRecordDebug srd : db.getStepRecordDebugs()) {
                StepRecord sr = srd.getStepRecord();
                if (sr.getExceptionInfo() != null) System.err.println(sr.getExceptionInfo().getStackTrace());
            }
        }

        WorkerDebug worker = debug.getWorkerDebugs().get(0);
        List<StepRecordDebug> dbgs = worker.getStepRecordDebugs();
        for (StepRecordDebug dbg : dbgs) {
            Activity activity = dbg.getActivity();
            if (activity != null) {
                assertEquals(10, activity.getMax().longValue());
                assertEquals(ActivityStatus.FINISHED, activity.getStatus());
            }
        }
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
    }
}
