package rapture.api.checkout;

import java.util.UUID;

import org.testng.Assert;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.client.HttpBlobApi;
import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.HttpSeriesApi;
import rapture.common.client.SimpleCredentialsProvider;

public class IntegrationTestHelper {
    
    HttpLoginApi raptureLogin = null;
    HttpSeriesApi seriesApi = null;
    HttpDocApi docApi = null;
    HttpBlobApi blobApi = null;

    public HttpLoginApi getRaptureLogin() {
        return raptureLogin;
    }

    public HttpSeriesApi getSeriesApi() {
        return seriesApi;
    }

    public HttpDocApi getDocApi() {
        return docApi;
    }

    public HttpBlobApi getBlobApi() {
        return blobApi;
    }

    public IntegrationTestHelper(String url, String username, String password) {
        raptureLogin = new HttpLoginApi(url, new SimpleCredentialsProvider(username, password));
        raptureLogin.login();
        seriesApi = new HttpSeriesApi(raptureLogin);
        docApi = new HttpDocApi(raptureLogin);
        blobApi = new HttpBlobApi(raptureLogin);
    }

    public RaptureURI getRandomAuthority(Scheme scheme) {
        return new RaptureURI.Builder(scheme, UUID.randomUUID().toString()).build();
    }

    public void configureTestRepo(RaptureURI repo, String storage) {
        Assert.assertFalse(repo.hasDocPath(), "Doc path not allowed");
        String authString = repo.toAuthString();
    
        switch (repo.getScheme()) {
        case BLOB:
            if (blobApi.blobRepoExists(repo.toAuthString())) blobApi.deleteBlobRepo(authString);
            blobApi.createBlobRepo(authString, "BLOB {} USING " + storage + " {prefix=\"B_" + repo.getAuthority() + "\"}",
                    "NREP {} USING " + storage + " {prefix=\"M_" + repo.getAuthority() + "\"}");
            Assert.assertTrue(blobApi.blobRepoExists(authString), authString + " Create failed");
            break;
    
        case DOCUMENT:
            if (docApi.docRepoExists(repo.toAuthString())) docApi.deleteDocRepo(authString);
            docApi.createDocRepo(authString, "NREP {} USING " + storage + " {prefix=\"D_" + repo.getAuthority() + "\"}");
            Assert.assertTrue(docApi.docRepoExists(authString), authString + " Create failed");
            break;
    
        case SERIES:
            if (seriesApi.seriesRepoExists(repo.toAuthString())) seriesApi.deleteSeriesRepo(authString);
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
    
        default:
            Assert.fail(repo.toString() + " not supported");
        }
    }

}