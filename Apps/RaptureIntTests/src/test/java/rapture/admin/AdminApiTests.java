package rapture.admin;

import static rapture.common.Scheme.DOCUMENT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import rapture.common.client.HttpAdminApi;
import rapture.common.impl.jackson.MD5Utils;
import rapture.common.model.RaptureUser;
import rapture.common.model.RepoConfig;
import rapture.helper.IntegrationTestHelper;

public class AdminApiTests {
	
	IntegrationTestHelper helper=null;
	HttpAdminApi adminApi=null;
	String raptureUrl=null;
	
	@BeforeClass(groups={"admin","nightly"})
    @Parameters({"RaptureURL","RaptureUser","RapturePassword"})
    public void beforeTest(@Optional("http://localhost:8665/rapture")String url, @Optional("rapture")String user, @Optional("rapture")String password)  {  
        helper = new IntegrationTestHelper(url, user, password);
        adminApi=helper.getAdminApi();
        raptureUrl=url;
    }
	
	@Test(groups={"admin","nightly"})
	public void testGetUsers() {
		int MAX_USERS=10;
		Set <String> userSet = new HashSet<String>();
		Reporter.log("Adding "+MAX_USERS+" users", true);
		for (int i = 0; i < MAX_USERS; i++) {
			String userName="testuser"+System.nanoTime();
			String description = "This is Test User";
			String pwd="testpassword";
			String email=userName+"@test.com";
		
			adminApi.addUser(userName, description, MD5Utils.hash16(pwd), email);
			userSet.add(userName);
		}
		Set <String> raptureUserSet = new HashSet<String>();
		for (RaptureUser ru : adminApi.getAllUsers())
			raptureUserSet.add(ru.getUsername());
		Reporter.log("Checking users exist",true);
		Assert.assertTrue(raptureUserSet.containsAll(userSet));
	}
	
	@Test(groups={"admin","nightly"})
	public void testGetUser() {
		String userName="testuser"+System.nanoTime();
		String description = "This is Test User";
		String pwd="testpassword";
		String email=userName+"@test.com";
		Reporter.log("Adding user "+userName, true);
		adminApi.addUser(userName, description, MD5Utils.hash16(pwd), email);

		RaptureUser user = adminApi.getUser(userName);
		Reporter.log("Checking user "+userName+" exists",true);
		Assert.assertEquals(user.getUsername(), userName);
	}
	
	@Test(groups={"admin","nightly"})
	public void testDestroyUser() {
		String userName="testuser"+System.nanoTime();
		String description = "This is Test User";
		String pwd="testpassword";
		String email=userName+"@test.com";
		Reporter.log("Adding user "+userName, true);
		adminApi.addUser(userName, description, MD5Utils.hash16(pwd), email);

		RaptureUser user = adminApi.getUser(userName);
		Reporter.log("Checking user "+userName+" exists",true);
		Assert.assertEquals(user.getUsername(), userName);
		
		Reporter.log("Destroying user "+userName,true);
		adminApi.destroyUser(userName);
		
		Assert.assertNull(adminApi.getUser(userName));
	}

	@Test(groups={"admin","nightly"})
	public void testDeleteUser() {
		String userName="testuser"+System.nanoTime();
		String description = "This is Test User";
		String pwd="testpassword";
		String email=userName+"@test.com";
		Reporter.log("Adding user "+userName, true);
		adminApi.addUser(userName, description, MD5Utils.hash16(pwd), email);

		RaptureUser user = adminApi.getUser(userName);
		Reporter.log("Checking user "+userName+" exists",true);
		Assert.assertEquals(user.getUsername(), userName);
		
		Reporter.log("Deleting user "+userName,true);
		adminApi.deleteUser(userName);
		
		Assert.assertTrue(adminApi.getUser(userName).getInactive());

	}
	
	@Test(groups={"admin","nightly"})
	public void testPasswordResetToken() {
		String userName="testuser"+System.nanoTime();
		String description = "This is Test User";
		String pwd="testpassword";
		String email=userName+"@test.com";
		Reporter.log("Adding user "+userName, true);
		adminApi.addUser(userName, description, MD5Utils.hash16(pwd), email);
		Assert.assertNotNull(adminApi.createPasswordResetToken(userName));
		adminApi.cancelPasswordResetToken(userName);
	}
	
	
	@Test(groups={"admin","nightly"})
	public void testUpdateUserEmail() {
		String userName="testuser"+System.nanoTime();
		String description = "This is Test User";
		String pwd="testpassword";
		String email=userName+"@test.com";
		Reporter.log("Adding user "+userName, true);
		adminApi.addUser(userName, description, MD5Utils.hash16(pwd), email);
		String newEmail=userName+"@testnew.com";
		adminApi.updateUserEmail(userName, newEmail);
		Assert.assertEquals(adminApi.getUser(userName).getEmailAddress(),newEmail);
		
	}

	
	@Test(groups={"admin","nightly"})
	public void testCopyDocumentRepo() {
		RaptureURI srcRepo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(srcRepo, "MONGODB");
        RaptureURI targetRepo = helper.getRandomAuthority(Scheme.DOCUMENT);
        int MAX_DOC=20;
        Reporter.log("Adding "+MAX_DOC + " documents to source repo");
        for (int i = 0; i < MAX_DOC;i++){
        	String docUri = RaptureURI.builder(DOCUMENT, srcRepo.getAuthority()).docPath("docTest" + System.nanoTime()).asString();
            helper.getDocApi().putDoc(docUri, "{\"testkey\":\"testvalue"+System.nanoTime()+"\"}");
		}

        Reporter.log("Copying "+srcRepo.getAuthority() +" to " +targetRepo.getAuthority());
		adminApi.copyDocumentRepo(srcRepo.getAuthority(), targetRepo.getAuthority(), false);
		Map<String,String> srcMap= new HashMap<String,String> ();
		Map<String,String> targetMap= new HashMap<String,String> ();
		for (String uri:helper.getDocApi().listDocsByUriPrefix(RaptureURI.builder(DOCUMENT, srcRepo.getAuthority()).asString(), 2).keySet()) {
			srcMap.put(uri.replace(RaptureURI.builder(DOCUMENT, srcRepo.getAuthority()).asString(), RaptureURI.builder(DOCUMENT, targetRepo.getAuthority()).asString()),helper.getDocApi().getDoc(uri));
		}
		for (String uri:helper.getDocApi().listDocsByUriPrefix(RaptureURI.builder(DOCUMENT, targetRepo.getAuthority()).asString(), 2).keySet()) {
			targetMap.put(uri,helper.getDocApi().getDoc(uri));
		}
		Reporter.log("Checking all data in "+srcRepo.getAuthority() +" is in " +targetRepo.getAuthority());
		Assert.assertEquals(srcMap, targetMap);
	}
	
	@Test(groups={"admin","nightly"})
    public void testGetSystemProperties() {
		Reporter.log("Checking for some keys in system properties");
        List<String> keys = new ArrayList<String>();
        keys.add("PATH");
        keys.add("SHELL");
        keys.add("LOGNAME");
        keys.add("HOME");
        
        Map<String, String> propMap=adminApi.getSystemProperties(keys);
        for (String propKey : propMap.keySet()) {
            Assert.assertNotNull(propMap.get(propKey));
        }
        
    }
	
    @Test (groups= {"admin","nightly"})
    public void getRepoConfigTest() {
    	Reporter.log("Checking default repo names");
        List<RepoConfig> repoConfigs = adminApi.getRepoConfig();
        Set<String> repoNameSet=new HashSet<String> ();
        for(RepoConfig repoConfig: repoConfigs ) {
            repoNameSet.add(repoConfig.getName());
            
        } 
        Assert.assertTrue(repoNameSet.contains("RaptureConfig"), "RaptureConfig is not present");
        Assert.assertTrue(repoNameSet.contains("RaptureEphemeral"), "RaptureEphemeral is not present");
        Assert.assertTrue(repoNameSet.contains("RaptureSettings"), "RaptureSettings is not present");       
    }
	
    @AfterClass(groups={"admin", "nightly"})
    public void AfterTest(){
    	helper.cleanAllAssets(); 
    }
}
