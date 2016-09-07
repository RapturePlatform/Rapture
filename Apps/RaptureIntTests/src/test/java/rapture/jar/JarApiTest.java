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
package rapture.jar;

import static rapture.common.Scheme.JAR;

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
import org.testng.annotations.DataProvider;
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

    IntegrationTestHelper helper = null;
    HttpJarApi jarApi = null;
    List<String> workflowList = null;
    List<String> jarList = null;

    @BeforeClass(groups = { "jar", "nightly" })
    @Parameters({ "RaptureURL", "RaptureUser", "RapturePassword" })
    public void setUp(@Optional("http://localhost:8665/rapture") String url,
            @Optional("rapture") String username, @Optional("rapture") String password) {

        helper = new IntegrationTestHelper(url, username, password);
        jarApi = new HttpJarApi(helper.getRaptureLogin());
        workflowList = new ArrayList<String>();
        jarList = new ArrayList<String>();

    }

    @Test(groups = { "jar", "nightly" })
    public void testPutJar() {

        String jarUri = "jar://test/jar" + System.nanoTime();
        byte[] jarContent = "TEST_CONTENT".getBytes();
        jarApi.putJar(jarUri, jarContent);
        Reporter.log("Verifying " + jarUri + " after putJar", true);
        Assert.assertTrue(jarApi.jarExists(jarUri));
        Assert.assertEquals(jarApi.getJarSize(jarUri).longValue(), jarContent.length);
        Assert.assertEquals(jarApi.getJar(jarUri).getContent(), jarContent);
        Assert.assertEquals(jarApi.getJarMetaData(jarUri).get("Content-Type"), "application/java-archive");
        jarApi.deleteJar(jarUri);
    }

    @Test(groups = { "jar", "nightly" })
    public void testJarListByUriPrefixFolderDepths() {
        String jarAuthority="jar" + System.nanoTime();
        byte[] jarContent = "TEST_CONTENT".getBytes();

        Reporter.log("Create some test jars", true);
        String jarURIf1d1 = RaptureURI.builder(JAR, jarAuthority).docPath("folder1/doc1").build().toString();
        String jarURIf1d2 = RaptureURI.builder(JAR, jarAuthority).docPath("folder1/doc2").build().toString();
        String jarURIf1d3 = RaptureURI.builder(JAR, jarAuthority).docPath("folder1/doc3").build().toString();
        String jarURIf2f21d1 = RaptureURI.builder(JAR,jarAuthority).docPath("folder2/folder21/doc1").build().toString();
        String jarURIf2f21d2 = RaptureURI.builder(JAR, jarAuthority).docPath("folder2/folder21/doc2").build().toString();
        String jarURIf3d1 = RaptureURI.builder(JAR, jarAuthority).docPath("folder3/doc1").build().toString();

        jarApi.putJar(jarURIf1d1, jarContent);
        jarList.add(jarURIf1d1);
        jarApi.putJar(jarURIf1d2, jarContent);
        jarList.add(jarURIf1d2);
        jarApi.putJar(jarURIf1d3,jarContent);
        jarList.add(jarURIf1d3);
        jarApi.putJar(jarURIf2f21d1,jarContent);
        jarList.add(jarURIf2f21d1);
        jarApi.putJar(jarURIf2f21d2,jarContent);
        jarList.add(jarURIf2f21d2);
        jarApi.putJar(jarURIf3d1,jarContent);

        Reporter.log("Check folder contents using different depths", true);
        Assert.assertEquals(jarApi.listJarsByUriPrefix(RaptureURI.builder(JAR, jarAuthority).docPath("folder1").build().toString(), 2).size(), 3);
        Assert.assertEquals(jarApi.listJarsByUriPrefix(RaptureURI.builder(JAR, jarAuthority).docPath("folder1").build().toString(), 1).size(), 3);
        Assert.assertEquals(jarApi.listJarsByUriPrefix(RaptureURI.builder(JAR, jarAuthority).docPath("folder2").build().toString(), 2).size(), 3);
        Assert.assertEquals(jarApi.listJarsByUriPrefix(RaptureURI.builder(JAR, jarAuthority).docPath("folder2").build().toString(), 1).size(), 1);
        Assert.assertEquals(jarApi.listJarsByUriPrefix(RaptureURI.builder(JAR, jarAuthority).docPath("folder2").build().toString(), 0).size(), 3);
        Assert.assertEquals(jarApi.listJarsByUriPrefix(RaptureURI.builder(JAR, jarAuthority).docPath("folder3").build().toString(), 0).size(), 1);

        Reporter.log("Delete some series and check folder contents", true);
        jarApi.deleteJar(jarURIf1d1);
        jarList.remove(jarURIf1d1);
        jarApi.deleteJar(jarURIf3d1);
        jarList.remove(jarURIf3d1);
        Assert.assertEquals(jarApi.listJarsByUriPrefix(RaptureURI.builder(JAR, jarAuthority).docPath("folder1").build().toString(), 2).size(), 2);
        Assert.assertEquals(jarApi.listJarsByUriPrefix(RaptureURI.builder(JAR, jarAuthority).docPath("folder1").build().toString(), 1).size(), 2);
        Assert.assertEquals(jarApi.listJarsByUriPrefix(RaptureURI.builder(JAR, jarAuthority).docPath("folder3").build().toString(), 0).size(), 0);
      
        Reporter.log("Recreated some series and check folder contents", true);
        jarApi.putJar(jarURIf3d1, jarContent);
        Assert.assertEquals(jarApi.listJarsByUriPrefix(RaptureURI.builder(JAR, jarAuthority).docPath("folder3").build().toString(), 1).size(), 1);
    }
    
    @Test(groups = { "jar", "nightly" })
    public void testDeleteJar() {
        String jarUri = "jar://test/jar" + System.nanoTime();
        byte[] jarContent = "TEST_CONTENT".getBytes();
        jarApi.putJar(jarUri, jarContent);
        Assert.assertTrue(jarApi.jarExists(jarUri));
        jarApi.deleteJar(jarUri);
        Reporter.log("Verifying " + jarUri + " after deleteJar", true);
        Assert.assertFalse(jarApi.jarExists(jarUri));
    }

    @Test(groups = { "jar", "nightly" }, dataProvider="jarCounts")
    public void testJarListByUriPrefixJarCounts(Integer jarCount) {
        String jarPrefix = "jar://test/jar" + System.nanoTime();
        Set<String> jarSet = new HashSet<String>();
        for (int i = 0; i < jarCount; i++) {
            String jarUri = jarPrefix + "/jar" + System.nanoTime();
            byte[] jarContent = "TEST_CONTENT".getBytes();
            jarApi.putJar(jarUri, jarContent);
            Assert.assertTrue(jarApi.jarExists(jarUri));
            jarSet.add(jarUri);
        }
        jarList.addAll(jarSet);
        Assert.assertEquals(jarApi.listJarsByUriPrefix(jarPrefix, 2).keySet(), jarSet);

    }

    @Test(groups = { "jar", "nightly" })
    public void testWorkOrderFromJar() {
        String jarUri = "jar://test/jar" + System.nanoTime();
        String jarPath = System.getProperty("user.dir") + File.separator + "build" + File.separator + "resources" + File.separator + "test" + File.separator
                + "jar" + File.separator + "nightly" + File.separator + "testJar.jar";
        Reporter.log("Creating " + jarUri + " from " + jarPath, true);
        byte[] jarContent = null;
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
        } catch (Exception e) {
        }
        jarApi.putJar(jarUri, jarContent);
        jarList.add(jarUri);
        // Create workflow definition for test
        List<Step> steps = new LinkedList<Step>();
        Workflow testWf = new Workflow();
        testWf.setSemaphoreType(SemaphoreType.UNLIMITED);
        // create step
        Step s = new Step();
        s.setName("step1");
        s.setDescription("step 1 for a java invocable to write to a doc repo.");
        String stepUri = new RaptureURI("//testjar.steps.CreateData", Scheme.DP_JAVA_INVOCABLE).toString();
        s.setExecutable(stepUri);
        // add to steps
        steps.add(s);
        // add all steps to workflow
        testWf.setSteps(steps);
        testWf.setStartStep("step1");
        List<String> localJarList = new ArrayList<String>();
        localJarList.add(jarUri);
        testWf.setJarUriDependencies(localJarList);
        String wfURI = "workflow://workflow" + System.nanoTime() + "/jar/CreateDataTest";
        Reporter.log("Creating workflow definition " + wfURI, true);
        testWf.setWorkflowURI(wfURI);

        HttpDecisionApi decisionApi = helper.getDecisionApi();
        decisionApi.putWorkflow(testWf);
        workflowList.add(wfURI);

        RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, "MONGODB");
        String docPath = new RaptureURI.Builder(repo).docPath("doc" + System.nanoTime()).build().toString();
        Map<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("DOC_URI", docPath);

        String content = "{\"test\":\"test" + System.nanoTime() + "\"}";
        paramMap.put("DOC_CONTENT", content);
        String workOrderURI = decisionApi.createWorkOrder(testWf.getWorkflowURI(), paramMap);
        Reporter.log("Creating work order " + workOrderURI, true);
        int numRetries = 0;
        long waitTimeMS = 2000;
        while (IntegrationTestHelper.isWorkOrderRunning(decisionApi, workOrderURI) && numRetries < 20) {
            Reporter.log("Checking workorder status, retry count=" + numRetries + ", waiting " + (waitTimeMS / 1000) + " seconds...", true);
            try {
                Thread.sleep(waitTimeMS);
            } catch (Exception e) {
            }
            numRetries++;
        }

        Reporter.log("Checking workorder status and document output", true);
        Assert.assertEquals(decisionApi.getWorkOrderStatus(workOrderURI).getStatus(), WorkOrderExecutionState.FINISHED);
        Assert.assertEquals(helper.getDocApi().getDoc(docPath), content);
    }

    @DataProvider
    public Object[][] jarCounts() {
        return new Object[][] {
                new Object[] {new Integer(5)},
                new Object[] {new Integer(20)},
                new Object[] {new Integer(150)},
        };
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
