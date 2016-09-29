package rapture.calendar;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import junit.framework.Assert;
import org.junit.AfterClass;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.client.HttpDecisionApi;
import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpJarApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.SimpleCredentialsProvider;
import rapture.common.dp.SemaphoreType;
import rapture.common.dp.Step;
import rapture.common.dp.StepRecord;
import rapture.common.dp.WorkOrderDebug;
import rapture.common.dp.Workflow;

/**
 * @author      Jeremy Flieder <jeremy.flieder@incapturetechnologies.com>
 * @version     1.0                 intial version
 */
public class CalendarTests {
	
	static String url="http://localhost:8665/rapture";
	static String user="rapture";
	static String password="rapture";
	static HttpLoginApi raptureLogin=null;
	static HttpJarApi jarApi=null;
	static HttpDecisionApi decisionApi=null;
	static List<String> jarList=null;
	static List<String> workflowList=null;
	
	@BeforeClass
    public static void setUpBeforeClass() {
		raptureLogin = new HttpLoginApi(url, new SimpleCredentialsProvider(user, password));
        raptureLogin.login();
        jarApi=new HttpJarApi(raptureLogin);
        decisionApi=new HttpDecisionApi(raptureLogin);
        jarList = new ArrayList<String> ();
        workflowList = new ArrayList<String> ();
        
        HttpDocApi docApi = new HttpDocApi(raptureLogin);
        String config="NREP {} USING MONGODB {prefix=\"testCalendar\"}";
        String docRepoUri="//testCalendar";
        if (!docApi.docRepoExists(docRepoUri))
        	docApi.createDocRepo(docRepoUri, config);
	}
	
    /**
     * Tests different scenarios for calendar step:
     * 1. Loads jar file w/ step class
     * 2. Creates work flow definition for calendar
     * 3. Updates calendar and translator definition document 
     * 4. Creates a work order using calendar and translator documents, a specific date, and locale if applicable (default to en-US)
     * 5. Run work order
     * 6. Check expected results
     */
    @Test
    public void testCalendarWorkOrder() {
        String jarUri = "jar://test/testCalendar";

        if (!jarApi.jarExists(jarUri)) {
        	addJar(jarUri);
        }
        // Create workflow definition for test
        List<Step> steps = new LinkedList<Step>();
        Workflow calendarWf = new Workflow();
        calendarWf.setSemaphoreType(SemaphoreType.UNLIMITED);
        // create step
        Step s = new Step();
        s.setName("step1");
        s.setDescription("calendar workflow");
        String stepUri = new RaptureURI("//calendar.steps.CalendarLookupStep", Scheme.DP_JAVA_INVOCABLE).toString();
        s.setExecutable(stepUri);
        // add to steps
        steps.add(s);
        // add all steps to workflow
        calendarWf.setSteps(steps);
        calendarWf.setStartStep("step1");
        List<String> localJarList = new ArrayList<String>();
        localJarList.add(jarUri);
        calendarWf.setJarUriDependencies(localJarList);
        String calwfURI = "workflow://workflow" + System.nanoTime() + "/CalendarTest";
        calendarWf.setWorkflowURI(calwfURI);

        decisionApi.putWorkflow(calendarWf);
        workflowList.add(calwfURI);

        
        HttpDocApi docApi = new HttpDocApi(raptureLogin);
        String translationUri = "document://testCalendar/translation";
        String calendarUri = "document://testCalendar/calendar";
        for (Map<String,String> currItem : createTestCaseList()) {
        	docApi.deleteDoc(translationUri);
        	docApi.deleteDoc(calendarUri);
	        docApi.putDoc(translationUri, currItem.get("TRANSLATE_DATA"));
	        docApi.putDoc(calendarUri, currItem.get("CALENDAR_DATA"));

	        Map<String, String> paramMap=new HashMap<String, String>();
	        System.out.println ("Test case: "+currItem.get("NAME"));
	        
		    paramMap.put("DATE",currItem.get("DATE_TO_TEST"));
		    paramMap.put("CALENDAR",calendarUri);
		    paramMap.put("TRANSLATOR",translationUri);
		    String locale=currItem.get("LOCALE");
		    if (locale!=null)
		    	paramMap.put("LOCALE",locale);
		    String woUri=decisionApi.createWorkOrder(calwfURI, paramMap);
		    System.out.println("Created work order:"+woUri);
		    WorkOrderDebug debug=null;
		    WorkOrderExecutionState state=null;
		    long timeout = System.currentTimeMillis() + 60000;
		    do {
		    	debug=decisionApi.getWorkOrderDebug(woUri);
		        state = debug.getOrder().getStatus();
		    } while (((state == WorkOrderExecutionState.NEW) || (state == WorkOrderExecutionState.ACTIVE)) && (System.currentTimeMillis() < timeout));
		    System.out.println("Checking results");
		    StepRecord sr = debug.getWorkerDebugs().get(0).getStepRecordDebugs().get(0).getStepRecord();
		    Assert.assertEquals("FAILED TEST: "+currItem.get("NAME"),currItem.get("EXPECTED_RETURN"), sr.getRetVal());

        }
    }
    
    /**
     * Private method to add calendar jar file
     */
    private void addJar(String jarUri) {
        String jarPath = System.getProperty("user.dir") + File.separator + "build" + File.separator + "libs" + File.separator + "Calendar.jar";

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
        jarApi.deleteJar(jarUri);
        jarApi.putJar(jarUri, jarContent);
        jarList.add(jarUri);
    }
    
    /**
     * Private method to create list of scenarios. Data for a scenario is stored in map of strings
     */
    private List<Map<String,String>> createTestCaseList () {
    	List<Map<String,String>> retList = new ArrayList<Map<String,String>> ();
    	
    	Map <String, String> testCaseMap = new HashMap<String,String>();
    	testCaseMap.put("NAME", "CHRISTMAS AS WEEKEND");
    	testCaseMap.put("TRANSLATE_DATA","{\"18Jan2016\":\"Martin Luther King Day\",\"25Dec2016\":\"Christmas\",\"09Feb2016\":[\"Shrove Tuesday\",\"Mardi Gras\"],\"01Jan\":\"New Year's Day\",\"02Feb\":\"Groundhog Day\"}");
    	testCaseMap.put("CALENDAR_DATA","{\"Saturday\":\"$WEEKEND\",\"Sunday\":\"$WEEKEND\",\"New Year's Day\":\"$HOLIDAY\",\"Christmas\":\"$HOLIDAY\",\"25Jan2016\":\"$HOLIDAY\",\"Mardi Gras\":\"$HOLIDAY\"}");
    	testCaseMap.put("DATE_TO_TEST", "2016-12-25");
    	testCaseMap.put("EXPECTED_RETURN", "$WEEKEND");
    	retList.add(testCaseMap);
    	
    	testCaseMap = new HashMap<String,String>();
    	testCaseMap.put("NAME", "CHRISTMAS AS WEEKEND");
    	testCaseMap.put("TRANSLATE_DATA","{\"18Jan2016\":\"Martin Luther King Day\",\"25Dec2016\":\"Christmas\",\"09Feb2016\":[\"Shrove Tuesday\",\"Mardi Gras\"],\"01Jan\":\"New Year's Day\",\"02Feb\":\"Groundhog Day\"}");
    	testCaseMap.put("CALENDAR_DATA","{\"Saturday\":\"$WEEKEND\",\"Sunday\":\"$WEEKEND\",\"New Year's Day\":\"$HOLIDAY\",\"Christmas\":\"$HOLIDAY\",\"25Jan2016\":\"$HOLIDAY\",\"Mardi Gras\":\"$HOLIDAY\"}");
    	testCaseMap.put("DATE_TO_TEST", "2016-12-25");
    	testCaseMap.put("EXPECTED_RETURN", "$WEEKEND");
    	retList.add(testCaseMap);
    	
    	testCaseMap = new HashMap<String,String>();
    	testCaseMap.put("NAME", "NON-HOLIDAY WEEKEND");
    	testCaseMap.put("TRANSLATE_DATA","{\"18Jan2016\":\"Martin Luther King Day\",\"25Dec2016\":\"Christmas\",\"09Feb2016\":[\"Shrove Tuesday\",\"Mardi Gras\"],\"01Jan\":\"New Year's Day\",\"02Feb\":\"Groundhog Day\"}");
    	testCaseMap.put("CALENDAR_DATA","{\"Saturday\":\"$WEEKEND\",\"Sunday\":\"$WEEKEND\",\"New Year's Day\":\"$HOLIDAY\",\"Christmas\":\"$HOLIDAY\",\"25Jan2016\":\"$HOLIDAY\",\"Mardi Gras\":\"$HOLIDAY\"}");
    	testCaseMap.put("DATE_TO_TEST", "2016-09-03");
    	testCaseMap.put("EXPECTED_RETURN", "$WEEKEND");
    	retList.add(testCaseMap);
    	
    	testCaseMap = new HashMap<String,String>();
    	testCaseMap.put("NAME", "CHRISTMAS AS HOLIDAY");
    	testCaseMap.put("TRANSLATE_DATA","{\"18Jan2016\":\"Martin Luther King Day\",\"25Dec2016\":\"Christmas\",\"09Feb2016\":[\"Shrove Tuesday\",\"Mardi Gras\"],\"01Jan\":\"New Year's Day\",\"02Feb\":\"Groundhog Day\"}");
    	testCaseMap.put("CALENDAR_DATA","{\"Christmas\":\"$HOLIDAY\",\"New Year's Day\":\"$HOLIDAY\",\"25Jan2016\":\"$HOLIDAY\",\"Mardi Gras\":\"$HOLIDAY\",\"Saturday\":\"$WEEKEND\",\"Sunday\":\"$WEEKEND\"}");
    	testCaseMap.put("DATE_TO_TEST", "2016-12-25");
    	testCaseMap.put("EXPECTED_RETURN", "$HOLIDAY");
    	retList.add(testCaseMap);
    	
    	testCaseMap = new HashMap<String,String>();
    	testCaseMap.put("NAME", "BUSINESS DAY");
    	testCaseMap.put("TRANSLATE_DATA","{\"18Jan2016\":\"Martin Luther King Day\",\"25Dec2016\":\"Christmas\",\"09Feb2016\":[\"Shrove Tuesday\",\"Mardi Gras\"],\"01Jan\":\"New Year's Day\",\"02Feb\":\"Groundhog Day\"}");
    	testCaseMap.put("CALENDAR_DATA","{\"Christmas\":\"$HOLIDAY\",\"New Year's Day\":\"$HOLIDAY\",\"25Jan2016\":\"$HOLIDAY\",\"Mardi Gras\":\"$HOLIDAY\",\"Saturday\":\"$WEEKEND\",\"Sunday\":\"$WEEKEND\"}");
    	testCaseMap.put("DATE_TO_TEST", "2016-11-25");
    	testCaseMap.put("EXPECTED_RETURN", "next");
    	retList.add(testCaseMap);
    	
    	testCaseMap = new HashMap<String,String>();
    	testCaseMap.put("NAME", "GERMAN LOCALE WEEKEND");
    	testCaseMap.put("TRANSLATE_DATA","{\"18Jan2016\":\"Martin Luther King Day\",\"25Dec2016\":\"Christmas\",\"09Feb2016\":[\"Shrove Tuesday\",\"Mardi Gras\"],\"01Jan\":\"New Year's Day\",\"02Feb\":\"Groundhog Day\"}");
    	testCaseMap.put("CALENDAR_DATA","{\"Christmas\":\"$HOLIDAY\",\"New Year's Day\":\"$HOLIDAY\",\"25Jan2016\":\"$HOLIDAY\",\"Mardi Gras\":\"$HOLIDAY\",\"Samstag\":\"$WEEKEND\",\"Sonntag\":\"$WEEKEND\"}");
    	testCaseMap.put("DATE_TO_TEST", "2016-09-11");
    	testCaseMap.put("EXPECTED_RETURN", "$WEEKEND");
    	testCaseMap.put("LOCALE", "de-DE");
    	retList.add(testCaseMap);
    	
    	testCaseMap = new HashMap<String,String>();
    	testCaseMap.put("NAME", "GERMAN LOCALE HOLIDAY");
    	testCaseMap.put("TRANSLATE_DATA","{\"18Jan2016\":\"Martin Luther King Day\",\"25Dez2016\":\"Christmas\",\"09Feb2016\":[\"Shrove Tuesday\",\"Mardi Gras\"],\"01Jan\":\"New Year's Day\",\"02Feb\":\"Groundhog Day\"}");
    	testCaseMap.put("CALENDAR_DATA","{\"Christmas\":\"$HOLIDAY\",\"New Year's Day\":\"$HOLIDAY\",\"25Jan2016\":\"$HOLIDAY\",\"Mardi Gras\":\"$HOLIDAY\",\"Samstag\":\"$WEEKEND\",\"Sonntag\":\"$WEEKEND\"}");
    	testCaseMap.put("DATE_TO_TEST", "2016-12-25");
    	testCaseMap.put("EXPECTED_RETURN", "$HOLIDAY");
    	testCaseMap.put("LOCALE", "de-DE");
    	retList.add(testCaseMap);
    	
    	testCaseMap = new HashMap<String,String>();
    	testCaseMap.put("NAME", "JAPANESE LOCALE WEEKEND");
    	testCaseMap.put("TRANSLATE_DATA","{\"18Jan2016\":\"Martin Luther King Day\",\"25Dez2016\":\"Christmas\",\"09Feb2016\":[\"Shrove Tuesday\",\"Mardi Gras\"],\"01Jan\":\"New Year's Day\",\"02Feb\":\"Groundhog Day\"}");
    	testCaseMap.put("CALENDAR_DATA","{\"Christmas\":\"$HOLIDAY\",\"New Year's Day\":\"$HOLIDAY\",\"25Jan2016\":\"$HOLIDAY\",\"Mardi Gras\":\"$HOLIDAY\",\"土曜日\":\"$WEEKEND\",\"日曜日\":\"$WEEKEND\"}");
    	testCaseMap.put("DATE_TO_TEST", "2016-09-11");
    	testCaseMap.put("EXPECTED_RETURN", "$WEEKEND");
    	testCaseMap.put("LOCALE", "ja-JP");
    	retList.add(testCaseMap);
    	
    	return retList;
    }
    
    @AfterClass
    public static void cleanUp() {
        for (String workflowURI : workflowList)
            decisionApi.deleteWorkflow(workflowURI);
        for (String jarURI : jarList)
            jarApi.deleteJar(jarURI);
    }
    
}
