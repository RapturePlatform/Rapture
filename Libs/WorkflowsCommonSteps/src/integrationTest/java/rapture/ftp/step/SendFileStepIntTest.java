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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.MediaType;

import rapture.common.Activity;
import rapture.common.ActivityStatus;
import rapture.common.CreateResponse;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.client.HttpBlobApi;
import rapture.common.client.HttpDecisionApi;
import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpJarApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.SimpleCredentialsProvider;
import rapture.common.dp.Step;
import rapture.common.dp.StepRecord;
import rapture.common.dp.StepRecordDebug;
import rapture.common.dp.Transition;
import rapture.common.dp.WorkOrderDebug;
import rapture.common.dp.WorkerDebug;
import rapture.common.dp.Workflow;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.ftp.common.FTPConnectionConfig;

public class SendFileStepIntTest {

    private static final String auth = "test" + System.currentTimeMillis();
    private static final String BLOB_USING_MONGODB = "BLOB {} USING MONGODB {prefix=\"/tmp/B" + auth + "\"}";
    private static final String META_USING_MONGODB = "REP {} USING MONGODB {prefix=\"/tmp/M" + auth + "\"}";

    private static HttpLoginApi raptureLogin = null;
    private static HttpDocApi docApi = null;
    private static HttpBlobApi blobApi = null;
    private static HttpDecisionApi decisionApi = null;
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
        String url = "http://localhost:8664/rapture";
        // String url = "http://165.193.214.92:8664/rapture";
        // String url = "http://192.168.99.100:8665/rapture";

        raptureLogin = new HttpLoginApi(url, new SimpleCredentialsProvider("rapture", "rapture"));
        raptureLogin.login();
        docApi = new HttpDocApi(raptureLogin);
        blobApi = new HttpBlobApi(raptureLogin);
        HttpJarApi jarApi = new HttpJarApi(raptureLogin);
        decisionApi = new HttpDecisionApi(raptureLogin);

        repoUri = new RaptureURI.Builder(Scheme.DOCUMENT, testPrefix + UUID.randomUUID().toString().replaceAll("-", "")).build();
        configureTestRepo(repoUri, "MONGODB");

        if (!blobApi.blobRepoExists("blob://tmp")) blobApi.createBlobRepo("blob://tmp", BLOB_USING_MONGODB, META_USING_MONGODB);
        blobApi.putBlob("blob://tmp/blobby", "And did those feet in ancient time walk upon England's crowded hills?".getBytes(),
                MediaType.ANY_TEXT_TYPE.toString());
        docApi.putDoc("document://tmp/elp",
                "{ \"Band\" : \"Emerson, Lake and Palmer\", \"Album\" : \"Brain Salad Surgery\", \"Track\" : \"Jerusalem\" }");

        if (jarApi.jarExists("jar://workflows/dynamic/WorkflowsCommonSteps.jar")) jarApi.deleteJar("jar://workflows/dynamic/WorkflowsCommonSteps.jar");

        try {
            Path WorkflowsCommonSteps = Paths.get("../../../Rapture/Libs/WorkflowsCommonSteps/build/libs/WorkflowsCommonSteps-1.0.0.jar");
            if (!WorkflowsCommonSteps.toFile().exists()) {
                System.err.println("Cannot access " + WorkflowsCommonSteps.normalize().toString());
            } else {
                jarApi.putJar("jar://workflows/dynamic/WorkflowsCommonSteps.jar", Files.readAllBytes(WorkflowsCommonSteps));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        defineWorkflow();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (blobApi != null) blobApi.deleteBlobRepo("blob://tmp");
        if (docApi != null) docApi.deleteDoc("document://tmp/elp");
        // decisionApi.deleteWorkflow(workflowUri);
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    static String workflowUri = "workflow://workflows/core/testing";

    static private void defineWorkflow() {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("64.124.8.105").setPort(22).setLoginId("ftpsssnyqa").setPassword("Fuj!n5kewrux")
                .setUseSFTP(true);
        // FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("192.168.42.82").setPort(22).setLoginId("rapture").setPassword("rapture")
        // .setUseSFTP(true);


        String configRepo = "document://test" + System.currentTimeMillis();
        String configUri = configRepo + "/Config";

        docApi.createDocRepo(configRepo, "NREP {} USING MONGODB {}");
        docApi.putDoc(configUri, JacksonUtil.jsonFromObject(ftpConfig));

        Workflow w = new Workflow();
        w.setJarUriDependencies(ImmutableList.of("jar://workflows/dynamic/*"));
        w.setStartStep("step1");
        List<Step> steps = new LinkedList<>();
        Step step = new Step();
        step.setExecutable("dp_java_invocable://ftp.steps.SendFileStep");
        step.setName("step1");
        step.setDescription("description");
        Transition tran = new Transition();
        tran.setName("next");
        tran.setTargetStep("$RETURN");
        Transition tran2 = new Transition();
        tran2.setName("error");
        tran2.setTargetStep("$FAIL");
        step.setTransitions(ImmutableList.of(tran, tran2));
        steps.add(step);

        Map<String, String> viewMap = new HashMap<>();
        viewMap.put("FTP_CONFIGURATION", "#" + configUri);
        w.setSteps(steps);
        w.setView(viewMap);
        w.setWorkflowURI(workflowUri);
        decisionApi.putWorkflow(w);
    }

    @Test
    public void testSendFileStep() {

        Map<String, String> args = new HashMap<>();
        args.put("COPY_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("/bin/ls", "incapture.test.dummyfile1")));

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

    @Test
    public void testSendBlobStep() {
        Map<String, String> args = new HashMap<>();
        args.put("COPY_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("blob://tmp/blobby", "incapture.test.dummyfile2")));

        Workflow wf = decisionApi.getWorkflow(workflowUri);

        CreateResponse response = decisionApi.createWorkOrderP(workflowUri, args, "Test");
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

    @Test
    public void testSendDocStep() {
        Map<String, String> args = new HashMap<>();
        args.put("COPY_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("document://tmp/elp", "incapture.test.dummyfile3")));

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
