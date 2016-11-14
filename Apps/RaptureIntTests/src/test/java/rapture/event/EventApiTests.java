package rapture.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpEventApi;
import rapture.common.client.HttpNotificationApi;
import rapture.common.client.HttpScriptApi;
import rapture.common.model.RaptureEvent;
import rapture.common.model.RaptureEventScript;
import rapture.helper.IntegrationTestHelper;

public class EventApiTests {

	HttpEventApi eventApi = null;
	HttpScriptApi scriptApi = null;
	HttpDocApi docApi = null;
	HttpNotificationApi notificationApi = null;
	IntegrationTestHelper helper = null;
	RaptureURI eventRepoUri = null;

	@BeforeClass(groups = { "document", "mongo", "nightly" })
	@Parameters({ "RaptureURL", "RaptureUser", "RapturePassword" })
	public void beforeTest(@Optional("http://localhost:8665/rapture") String url, @Optional("raptureApi") String user,
			@Optional("raptivating") String password) {
		helper = new IntegrationTestHelper(url, user, password);
		eventApi = helper.getEventApi();
		eventRepoUri = helper.getRandomAuthority(Scheme.EVENT);
		scriptApi = helper.getScriptApi();
		docApi = helper.getDocApi();

	}

	@AfterClass
	public void cleanUp() {
		helper.cleanAllAssets();
	}

	@Test(groups = { "event", "mongo", "nightly" })
	public void testFireScriptEvent() {

		String eventScriptText = "println('Hello from a test script');\n";
		String eventScriptName = "eventScript" + System.nanoTime();

		String eventURI = RaptureURI.builder(eventRepoUri).docPath(eventScriptName).asString();

		String scriptPath = RaptureURI.builder(helper.getRandomAuthority(Scheme.SCRIPT)).docPath(eventScriptName)
				.asString();
		Reporter.log("Created script " + scriptPath, true);
		scriptApi.createScript(scriptPath, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, eventScriptText);
		eventApi.addEventScript(eventURI, scriptPath, true);

		RaptureEvent currEvent = eventApi.getEvent(eventURI);
		Assert.assertEquals("event://" + currEvent.getUriFullPath(), eventURI);
		Assert.assertEquals("event://" + currEvent.getStoragePath(), eventURI);

		Assert.assertTrue(eventApi.runEvent(eventURI, "eventName", "eventContext"));
		Assert.assertTrue(eventApi.eventExists(eventURI));
	}

	@Test(groups = { "event", "mongo", "nightly" })
	public void testRunEventFromRepo() throws InterruptedException {

		RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
		helper.configureTestRepo(repo, "MONGODB");
		String docURI = RaptureURI.builder(repo).docPath("folder1/documenttest").build().toString();

		String testEventTargetDoc = RaptureURI.builder(repo).docPath("folder1/targetDoc").build().toString();

		String docToWrite = "{\\\"key1\\\":\\\"val1 written by event\\\"}\"";
		String scriptText = "#doc.putDoc(\"" + testEventTargetDoc + "\"" + ",\"" + docToWrite + ");\n"; //

		String scriptName = "script" + System.nanoTime();
		String scriptURI = RaptureURI.builder(helper.getRandomAuthority(Scheme.SCRIPT)).docPath(scriptName).asString();
		String eventName = "data/update"; // this is a special keyword/fixed uri
											// which fires an event when data is
											// written to repo
		Reporter.log("Created script " + scriptURI, true);
		scriptApi.createScript(scriptURI, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, scriptText);
		String eventURI = RaptureURI.builder(Scheme.EVENT, repo.getAuthority()).docPath(eventName).build().toString();
		Reporter.log("Created event " + eventURI, true);
		// associate event with script to run test once only
		eventApi.addEventScript(eventURI, scriptURI, true);

		Reporter.log("Write to " + docURI + " to trigger event", true);
		docApi.putDoc(docURI, "{\"key1\":\"value1\"}");
		String s2 = docApi.getDoc(docURI);
		Assert.assertEquals(s2, "{\"key1\":\"value1\"}");
		Thread.sleep(1000);

		// check if the doc was written from the reflex script
		String s3 = docApi.getDoc(testEventTargetDoc);
		Reporter.log("Check contents of " + testEventTargetDoc, true);
		Assert.assertEquals(s3, "{\"key1\":\"val1 written by event\"}");

	}

	@Test(groups = { "event", "mongo", "nightly" })
	public void testNullEventScriptURI() {

		String eventName = "event" + System.nanoTime();
		String eventURI = RaptureURI.builder(eventRepoUri).docPath(eventName).asString();
		eventApi.addEventScript(eventURI, null, true);
		eventApi.runEvent(eventURI, "eventName", "eventContext");
	}

	@Test(groups = { "event", "mongo", "nightly" })
	public void testDeleteEvent() {

		int MAX_EVENTS = 15;

		for (int i = 0; i < MAX_EVENTS; i++) {
			String eventScriptText = "println('Hello from a test script');\n";

			String eventName = "event" + System.nanoTime();
			String eventURI = RaptureURI.builder(eventRepoUri).docPath(eventName).asString();

			String eventScriptName = "eventScript" + System.nanoTime();
			String scriptURI = RaptureURI.builder(helper.getRandomAuthority(Scheme.SCRIPT)).docPath(eventScriptName)
					.asString();
			Reporter.log("Created script " + scriptURI, true);
			scriptApi.createScript(scriptURI, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM,
					eventScriptText);

			eventApi.addEventScript(eventURI, scriptURI, true);

			// set up event with 1 script
			RaptureEvent newEvent = new RaptureEvent();
			RaptureEventScript newScript = new RaptureEventScript();
			newScript.setScriptURI(scriptURI);
			Set<RaptureEventScript> scriptsSet = new HashSet<RaptureEventScript>();
			scriptsSet.add(newScript);
			newEvent.setScripts(scriptsSet);

			eventApi.addEventScript(eventURI + "/f1", scriptURI, true);
			newEvent = new RaptureEvent();
			scriptsSet = new HashSet<RaptureEventScript>();
			scriptsSet.add(newScript);
			newEvent.setScripts(scriptsSet);

			// Test that invalid event uri is not retrieved
			eventApi.putEvent(newEvent);

			// Test that invalid event uri is not removed
			try {
				eventApi.deleteEventScript(eventURI + "test", scriptURI);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Reporter.log("Deleting events in repo " + "event://" + eventRepoUri.getAuthority(), true);
		eventApi.deleteEventsByUriPrefix("//" + eventRepoUri.getAuthority());
		Assert.assertEquals(eventApi.listEventsByUriPrefix("//" + eventRepoUri.getAuthority()).size(), 0);
	}

	@Test(groups = { "event", "mongo", "nightly" })
	public void testEventNotification() {

		List<String> addedEventList = new ArrayList<String>();
		List<String> removedEventList = new ArrayList<String>();
		int MAX_EVENTS = 20;

		for (int i = 0; i < MAX_EVENTS; i++) {
			String eventScriptText = "println('Hello from a test script');\n";
			String eventName = "event" + System.nanoTime();
			String eventURI = RaptureURI.builder(eventRepoUri).docPath(eventName).asString();

			String eventScriptName = "eventScript" + System.nanoTime();
			String scriptURI = RaptureURI.builder(helper.getRandomAuthority(Scheme.SCRIPT)).docPath(eventScriptName)
					.asString();
			scriptApi.createScript(scriptURI, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM,
					eventScriptText);

			eventApi.addEventScript(eventURI, scriptURI, true);
			String eventNotificationName = "eventNotification" + System.nanoTime();
			eventApi.addEventNotification(eventURI, eventNotificationName, "//system/kernel",
					new HashMap<String, String>());
			if (i % 2 == 0) {
				eventApi.addEventScript(eventURI, scriptURI, true);
				addedEventList.add(eventURI);
				// set up event with 1 script
				RaptureEvent newEvent = new RaptureEvent();
				RaptureEventScript newScript = new RaptureEventScript();
				newScript.setScriptURI(scriptURI);
				HashSet<RaptureEventScript> scriptsSet = new HashSet<RaptureEventScript>();
				scriptsSet.add(newScript);
				newEvent.setScripts(scriptsSet);
				Reporter.log("Running event "+eventURI, true);
				Assert.assertTrue(eventApi.runEvent(eventURI, "eventName", "eventContext"));
			} else {
				eventApi.deleteEventNotification(eventURI, eventNotificationName);
				removedEventList.add(eventURI);
			}
		}
	}
	
	@Test(groups = { "event", "mongo", "nightly" })
    public void testBasicEventGetChildren () {
        
        int MAX_EVENTS=15;
        
        for (int i=0;i<MAX_EVENTS; i++) {
            String eventScriptText = "println('Hello from a test script');\n";
            String eventName = "event" + System.nanoTime();
			String eventURI = RaptureURI.builder(eventRepoUri).docPath(eventName).asString();

			String eventScriptName = "eventScript" + System.nanoTime();
			String scriptURI = RaptureURI.builder(helper.getRandomAuthority(Scheme.SCRIPT)).docPath(eventScriptName)
					.asString();

            scriptApi.createScript( "//test/" + eventScriptName, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM,
                    eventScriptText);

            eventApi.addEventScript(eventURI+"/"+i, scriptURI, true);
            
            RaptureEvent newEvent= new RaptureEvent();
            RaptureEventScript newScript= new RaptureEventScript();
            newScript.setScriptURI(scriptURI);
            HashSet <RaptureEventScript> scriptsSet = new HashSet <RaptureEventScript> ();
            scriptsSet.add(newScript);
            newEvent.setScripts(scriptsSet);
          
        }  
        Assert.assertEquals(eventApi.listEventsByUriPrefix("//" + eventRepoUri.getAuthority()).size(),MAX_EVENTS);
	}

}
