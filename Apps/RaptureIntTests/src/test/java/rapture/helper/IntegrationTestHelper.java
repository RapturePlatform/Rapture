/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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

package rapture.helper;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.testng.Assert;

import com.google.common.collect.ImmutableList;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.client.HttpAdminApi;
import rapture.common.client.HttpBlobApi;
import rapture.common.client.HttpDecisionApi;
import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpEntitlementApi;
import rapture.common.client.HttpEventApi;
import rapture.common.client.HttpIndexApi;
import rapture.common.client.HttpLockApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.HttpOperationApi;
import rapture.common.client.HttpPluginApi;
import rapture.common.client.HttpScriptApi;
import rapture.common.client.HttpSearchApi;
import rapture.common.client.HttpSeriesApi;
import rapture.common.client.HttpStructuredApi;
import rapture.common.client.HttpUserApi;
import rapture.common.client.SimpleCredentialsProvider;

public class IntegrationTestHelper {
    
    HttpLoginApi raptureLogin = null;
    HttpLockApi lockApi = null;
    HttpIndexApi indexApi = null;
    HttpSeriesApi seriesApi = null;
    HttpScriptApi scriptApi = null;
    HttpSearchApi searchApi = null;
    HttpDocApi docApi = null;
    HttpBlobApi blobApi = null;
    HttpUserApi userApi = null;
    HttpAdminApi adminApi = null;
    HttpEntitlementApi entApi = null;
    HttpEventApi eventApi = null;
    HttpPluginApi pluginApi = null;
    HttpDecisionApi decisionApi = null;
    HttpStructuredApi structApi = null;
    HttpOperationApi operationApi = null;

    static final String testPrefix = "__RESERVED__";

    Set<RaptureURI> uriCache;

    public HttpLoginApi getRaptureLogin() {
        return raptureLogin;
    }

    public HttpEventApi getEventApi() {
        return eventApi;
    }
    
    public HttpIndexApi getIndexApi() {
        return indexApi;
    }

    public HttpPluginApi getPluginApi() {
        return pluginApi;
    }
    
    public HttpSeriesApi getSeriesApi() {
        return seriesApi;
    }

    public HttpSearchApi getSearchApi() {
        return searchApi;
    }

    public HttpScriptApi getScriptApi() {
        return scriptApi;
    }

    public HttpDocApi getDocApi() {
        return docApi;
    }

    public HttpOperationApi getOperationApi() {
        return operationApi;
    }

    public HttpBlobApi getBlobApi() {
        return blobApi;
    }

    public HttpUserApi getUserApi() {
        return userApi;
    }

    public HttpAdminApi getAdminApi() {
        return adminApi;
    }

    public HttpEntitlementApi getEntApi() {
        return entApi;
    }

    public HttpDecisionApi getDecisionApi() {
        return decisionApi;
    }

    public HttpStructuredApi getStructApi() {
        return structApi;
    }

    String user = null;

    public String getUser() {
        return user;
    }

    public IntegrationTestHelper(String url, String username, String password) {
        user = username;
        raptureLogin = new HttpLoginApi(url, new SimpleCredentialsProvider(username, password));
        raptureLogin.login();
        searchApi = new HttpSearchApi(raptureLogin);
        seriesApi = new HttpSeriesApi(raptureLogin);
        scriptApi = new HttpScriptApi(raptureLogin);
        lockApi = new HttpLockApi(raptureLogin);
        indexApi = new HttpIndexApi(raptureLogin);
        docApi = new HttpDocApi(raptureLogin);
        blobApi = new HttpBlobApi(raptureLogin);
        userApi = new HttpUserApi(raptureLogin);
        adminApi = new HttpAdminApi(raptureLogin);
        entApi = new HttpEntitlementApi(raptureLogin);
        eventApi = new HttpEventApi(raptureLogin);
        pluginApi = new HttpPluginApi(raptureLogin);
        decisionApi = new HttpDecisionApi(raptureLogin);
        structApi = new HttpStructuredApi(raptureLogin);
        operationApi = new HttpOperationApi(raptureLogin);
        uriCache = new HashSet<>();
    }

    public RaptureURI getRandomAuthority(Scheme scheme) {
        RaptureURI gagarin = new RaptureURI.Builder(scheme, testPrefix + UUID.randomUUID().toString().replaceAll("-", "")).build();
        uriCache.add(gagarin);
        return gagarin;
    }

    public void configureTestRepo(RaptureURI repo, String storage) {
    	configureTestRepo(repo, storage, false);
    }
    
    public void configureTestRepo(RaptureURI repo, String storage, boolean versioned) {
        Assert.assertFalse(repo.hasDocPath(), "Doc path not allowed");
        String authString = repo.toAuthString();
        uriCache.add(repo);

        switch (repo.getScheme()) {
        case BLOB:
            if (blobApi.blobRepoExists(repo.toAuthString())) blobApi.deleteBlobRepo(authString);
            	blobApi.createBlobRepo(authString, "BLOB {} USING " + storage + " {prefix=\"B_" + repo.getAuthority()+ "\"}",
                "NREP {} USING " + storage + " {prefix=\"M_" + repo.getAuthority() + "\"}");
            Assert.assertTrue(blobApi.blobRepoExists(authString), authString + " Create failed");
            break;
    
        case DOCUMENT:
            if (docApi.docRepoExists(repo.toAuthString())) docApi.deleteDocRepo(authString);
            	docApi.createDocRepo(authString, "NREP {} USING " + storage + " {prefix=\"D_" + repo.getAuthority()+ (versioned? ", separateVersion=\"true\"}":"") + "\"}");
            Assert.assertTrue(docApi.docRepoExists(authString), authString + " Create failed");
            break;
    
        case SCRIPT:
            // Scripts use an existing repo
            break;

        case SERIES:
            if (seriesApi.seriesRepoExists(repo.toAuthString())) seriesApi.deleteSeriesRepo(authString);
            
            if (storage.equalsIgnoreCase("CASSANDRA"))
            	seriesApi.createSeriesRepo(authString, "SREP {} USING " + storage + " {keyspace=\""+"s"+repo.getAuthority().substring(repo.getAuthority().length()-8, repo.getAuthority().length())+"KS\", cf=\""+"s"+repo.getAuthority().substring(repo.getAuthority().length()-8, repo.getAuthority().length())+"CF\"}");
            else
            	seriesApi.createSeriesRepo(authString, "SREP {} USING " + storage + " {prefix=\"S_" + repo.getAuthority() + "\"}");
            Assert.assertTrue(seriesApi.seriesRepoExists(authString), authString + " Create failed");
            break;
    
        default:
            Assert.fail(repo.toString() + " not supported");
        }
    }

    public void cleanTestRepo(RaptureURI repo) {
        Assert.assertFalse(repo.hasDocPath());
        String authString = repo.toAuthString();
    
        switch (repo.getScheme()) {
        case BLOB:
            blobApi.deleteBlobRepo(authString);
            Assert.assertFalse(blobApi.blobRepoExists(authString), authString + " Delete failed");
            break;
    
        case DOCUMENT:
            docApi.deleteDocRepo(authString);
            Assert.assertFalse(docApi.docRepoExists(authString), authString + " Delete failed");
            break;
    
        case SERIES:
            seriesApi.deleteSeriesRepo(authString);
            Assert.assertFalse(seriesApi.seriesRepoExists(authString), authString + " Delete failed");
            break;

        case SCRIPT:
            scriptApi.deleteScriptsByUriPrefix(authString);
            scriptApi.deleteScript(authString);
            break;
        case STRUCTURED:
        	if (structApi.structuredRepoExists(authString))
        		structApi.deleteStructuredRepo(authString);
            Assert.assertFalse(structApi.structuredRepoExists(authString), authString + " Delete failed");
            break;
        case WORKFLOW:
            // TODO

        default:
            System.out.println(repo.toString() + " not supported");
        }
        uriCache.remove(repo);
    }

    public static boolean isWorkOrderRunning (HttpDecisionApi decisionApi,String workOrderURI ) {
        WorkOrderExecutionState state = decisionApi.getWorkOrderStatus(workOrderURI).getStatus();
        return !(state == WorkOrderExecutionState.FINISHED || state == WorkOrderExecutionState.CANCELLED || state == WorkOrderExecutionState.ERROR ||  state == WorkOrderExecutionState.FAILING);
    }
    
    /**
     * Delete any created assets that we know about
     */
    public void cleanAllAssets() {
        for (RaptureURI gagarin : ImmutableList.copyOf(uriCache)) {
            cleanTestRepo(gagarin);
        }
    }
}
