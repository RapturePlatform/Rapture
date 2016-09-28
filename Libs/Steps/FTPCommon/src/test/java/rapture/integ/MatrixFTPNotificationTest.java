package rapture.integ;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import rapture.common.RaptureScript;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.client.HttpAdminApi;
import rapture.common.client.HttpBlobApi;
import rapture.common.client.HttpDecisionApi;
import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpJarApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.HttpScriptApi;
import rapture.common.client.HttpSysApi;
import rapture.common.client.SimpleCredentialsProvider;
import rapture.common.dp.Step;
import rapture.common.dp.Steps;
import rapture.common.dp.Transition;
import rapture.common.dp.WorkOrderDebug;
import rapture.common.dp.WorkerDebug;
import rapture.common.dp.Workflow;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.ftp.common.FTPConnectionConfig;
import rapture.mail.Mailer;
import rapture.mail.SMTPConfig;

public class MatrixFTPNotificationTest {

	
	
	Workflow copyFileAndNotifyWorkflow=null;
	static final String auth = "test" + System.currentTimeMillis();
    static String configRepoName="test" + System.currentTimeMillis();
    static String configRepo = "document://"+configRepoName;
    static String translationUri = configRepo + "/translation";
    static String calendarCMIUri = configRepo + "/calendarCMI";
    static String calendarSSSUri = configRepo + "/calendarSSS";
    static String calendarSCBUri = configRepo + "/calendarSCB";
    static String calendarMGCUri = configRepo + "/calendarMGC";


    static final String area = "CONFIG";
    static final String templateName = "TESTING";
	
	String fileExistsWorkflowUri = "workflow://matrix/fileExistsFlow";
	String fileFetchWorkflowUri = "workflow://matrix/fileFetchFlow";
	String untilMidnightWorkflowUri = "workflow://matrix/untilMidnightWorkflow";
	String untilTimeoutWorkflowUri = "workflow://matrix/untilTimeoutWorkflow";
    
    static String url = "http://192.168.99.100:8665/rapture";
	static String user="rapture";
	static String password="rapture";
	static HttpLoginApi raptureLogin=null;
	static HttpJarApi jarApi=null;
	static HttpDecisionApi decisionApi=null;
	static HttpScriptApi scriptApi=null;
	static HttpDocApi docApi=null;
	static HttpSysApi sysApi= null;
	static HttpAdminApi adminApi= null;
	static HttpBlobApi blobApi= null;
    
    static String ftpConfigUri = configRepo + "/FTPConfiguration";
    static String sftpConfigUri = configRepo + "/SFTPConfiguration";
	
	@BeforeClass
    public static void setUpBeforeClass() {
		raptureLogin = new HttpLoginApi(url, new SimpleCredentialsProvider(user, password));
        raptureLogin.login();
        jarApi=new HttpJarApi(raptureLogin);
        decisionApi=new HttpDecisionApi(raptureLogin);
        scriptApi=new HttpScriptApi(raptureLogin);
        docApi= new HttpDocApi(raptureLogin);
        sysApi=new HttpSysApi(raptureLogin);
        adminApi=new HttpAdminApi(raptureLogin);
        blobApi=new HttpBlobApi (raptureLogin);
        
        sysApi.writeSystemConfig( area, Mailer.EMAIL_TEMPLATE_DIR + successTemplateName, successTemplate);
        sysApi.writeSystemConfig( area, Mailer.EMAIL_TEMPLATE_DIR + FILEEXISTSSTEP, fileExistsError);
        sysApi.writeSystemConfig( area, Mailer.EMAIL_TEMPLATE_DIR + GETFILESTEP, getFileError);
        try {
        	Path calendarJar = Paths.get("./build/libs/Calendar.jar");
	        jarApi.putJar("jar://workflows/dynamic/Calendar.jar", Files.readAllBytes(calendarJar));
	        Path ftpcommonJar = Paths.get("../FTPCommon/build/libs/FTPCommon-1.0.0.jar");
	        jarApi.putJar("jar://workflows/dynamic/FTPCommon.jar", Files.readAllBytes(ftpcommonJar));
        } catch (Exception e) {}
        
        String config = "NREP {} USING MONGODB {prefix=\""+configRepoName+"\"}"; 
        docApi.createDocRepo(configRepo, config);
        System.out.println ("Creating doc repo:"+configRepo);
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("speedtest.tele2.net").setPort(23).setLoginId("ftp").setPassword("foo@bar")
                .setUseSFTP(false);

        docApi.putDoc(ftpConfigUri, JacksonUtil.jsonFromObject(ftpConfig));

        FTPConnectionConfig sftpConfig = new FTPConnectionConfig().setAddress("speedtest.tele2.net").setPort(23).setLoginId("ftp").setPassword("foo@bar")
                .setUseSFTP(true);

        docApi.putDoc(sftpConfigUri, JacksonUtil.jsonFromObject(sftpConfig));
        try {
        	setupMailMost();
        } catch (Exception e) {
        	e.printStackTrace();
        }
        if (!blobApi.blobRepoExists("blob://tmp")) {
            blobApi.createBlobRepo("blob://tmp", "BLOB {} USING MONGODB {prefix=\"/tmp/B" + auth + "\"}", "NREP {} USING MONGODB {prefix=\"/tmp/M" + auth + "\"}");
        }
        
        if (!blobApi.blobRepoExists("blob://matrix")) {
            blobApi.createBlobRepo("blob://matrix", "BLOB {} USING MONGODB {prefix=\"/tmp/Bmatrix" + auth + "\"}", "NREP {} USING MONGODB {prefix=\"/tmp/Mmatrix" + auth + "\"}");
        }
        
	}
    
	@Test
	public void testFileRetrieveWorkflow () {
		createFilesRetrieveWorkflow();
		initialiseCalendarDocuments();

		Map<String, String>treasuries_args=new HashMap<String,String>();
        treasuries_args.put("SLACK_WEBHOOK", "https://hooks.slack.com/services/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");
        treasuries_args.put("NOTIFY_TYPE", "SLACK,EMAIL");
        treasuries_args.put("EMAIL_RECIPIENTS", "jeremy.flieder@incapturetechnologies.com");
        treasuries_args.put("MESSAGE_TEMPLATE", templateName);

        treasuries_args.put("CALENDAR", calendarSSSUri);
        treasuries_args.put("TRANSLATOR", translationUri);
        treasuries_args.put("JOBNAME", "downloadtreasuries");

        treasuries_args.put("FETCH_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("MATRXFTPH2/treasuries.${TODAY}", "blob://matrix/treasuries")));
        treasuries_args.put("COPY_FILES",
                JacksonUtil.jsonFromObject(
                        ImmutableMap.of("blob://matrix/treasuries",
                                ImmutableList.of("file://mnt/32osData/matrix/IncomingFTP/${TODAY2}/SSS/treasuries.${TODAY}",
                                        "file://mnt/32osData/matrix/IncomingFTP/${TODAY2}/SSS/treasuries.txt"))));

		String woUri=decisionApi.createWorkOrder(fileFetchWorkflowUri, treasuries_args);
		System.out.println("wo="+woUri);
		try {
			Thread.sleep(10000);
		} catch (Exception e ) {}
		
		WorkOrderDebug woDebug = decisionApi.getWorkOrderDebug(woUri);
		System.out.println("woDebug="+woDebug.debug());
		List<WorkerDebug> woDebugsList=woDebug.getWorkerDebugs();
        for (WorkerDebug wd : woDebugsList) {
        	System.out.println("wd="+wd.toString());
        }
	}
	
	//@Test
	public void testFileExistWorkflow () {
		createFilesExistWorkflow();
	}
	
	
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    	docApi.deleteDocRepo(configRepo);
    }
	
    
    void initialiseCalendarDocuments() {
        Map<String, Object> translationMap = new LinkedHashMap<>();
        translationMap.put("2015-01-01", "New Year's Day");
        translationMap.put("2015-01-19", "Martin Luther King Jr Day");
        translationMap.put("2015-02-16", "President's Day");
        translationMap.put("2015-04-03", "Good Friday");
        translationMap.put("2015-05-25", "Memorial Day");
        translationMap.put("2015-07-03", ImmutableList.of("Independence Day", "July 4th"));
        translationMap.put("2015-09-07", "Labor Day");
        translationMap.put("2015-10-12", "Columbus Day");
        translationMap.put("2015-11-11", "Veteran's Day");
        translationMap.put("2015-11-26", "Thanksgiving Day");
        translationMap.put("2015-12-25", "Christmas Day");
        translationMap.put("2016-01-01", "New Year's Day");
        translationMap.put("2016-01-18", "Martin Luther King, Jr. Day");
        translationMap.put("2016-02-15", "President's Day");
        translationMap.put("2016-03-25", "Good Friday");
        translationMap.put("2016-05-30", "Memorial Day");
        translationMap.put("2016-07-04", "July 4th");
        translationMap.put("2016-09-05", "Labor Day");
        translationMap.put("2016-10-10", "Columbus Day");
        translationMap.put("2016-11-11", "Veteran's Day");
        translationMap.put("2016-11-24", "Thanksgiving Day");
        translationMap.put("2016-12-26", "Christmas Day");
        translationMap.put("02Feb", "Groundhog Day");
        docApi.putDoc(translationUri, JacksonUtil.jsonFromObject(translationMap));

        Map<String, Object> calendarMap = new LinkedHashMap<>();
        calendarMap.put("New Year's Day", HOLIDAY);
        calendarMap.put("Martin Luther King, Jr. Day", HOLIDAY);
        calendarMap.put("President's Day", HOLIDAY);
        calendarMap.put("Memorial Day", HOLIDAY);
        calendarMap.put("July 4th ", HOLIDAY);
        calendarMap.put("Labor Day", HOLIDAY);
        calendarMap.put("Columbus Day", HOLIDAY);
        calendarMap.put("Veteran's Day", HOLIDAY);
        calendarMap.put("Thanksgiving Day", HOLIDAY);
        calendarMap.put("Christmas Day", HOLIDAY);
        calendarMap.put("Saturday", WEEKEND);
        calendarMap.put("Sunday", WEEKEND);
        docApi.putDoc(calendarMGCUri, JacksonUtil.jsonFromObject(calendarMap));
        docApi.putDoc(calendarSCBUri, JacksonUtil.jsonFromObject(calendarMap));
        calendarMap.put("Good Friday", HOLIDAY);
        docApi.putDoc(calendarSSSUri, JacksonUtil.jsonFromObject(calendarMap));
        docApi.putDoc(calendarCMIUri, JacksonUtil.jsonFromObject(calendarMap));
    }
    
    private static void setupMailMost() throws Exception {
        // create smtp config
        String area = "CONFIG";
        SMTPConfig smtpConfig = new SMTPConfig().setFrom("bmsdrama2016@gmail.com").setUsername("bmsdrama2016@gmail.com").setPassword("gM3xpat/")
                .setHost("smtp.gmail.com").setPort(587);
        sysApi.writeSystemConfig(area, Mailer.SMTP_CONFIG_URL, JacksonUtil.jsonFromObject(smtpConfig));

        // create dummy email template
        String template = "{\"emailTo\":\"${EMAIL_RECIPIENTS}\",\"subject\":\"Ignore - generated from test\",\"msgBody\":\"This email is generated from test\"}";
        String url = Mailer.EMAIL_TEMPLATE_DIR + templateName;
        sysApi.writeSystemConfig( area, url, template);

        // update user email
        adminApi.updateUserEmail( "rapture", "support@incapturetechnologies.com");
    }
	// TODO: REMOVE WHEN WORKFLOW AVAILABLE IN PLUGIN
    static final String GETDATESTEP = "getDate";
    static final String CHECKCALENDARSTEP = "checkCalendarStep";
    static final String FILEEXISTSSTEP = "fileExistsStep";
    static final String GETFILESTEP = "getFileStep";
    static final String COPYFILESTEP = "copyFileStep";
    static final String NOTIFICATIONSTEPSUCCESS = "notificationStepSuccess";
    static final String NOTIFICATIONSTEPERROR = "notificationStepError";

    static String WEEKEND = "Weekend";
    static String HOLIDAY = "Holiday";
    static String CANCEL = "$CANCEL";
    static final String successTemplateName = "TESTING";
    static final String successTemplate = "{\"emailTo\":\"${EMAIL_RECIPIENTS}\",\"subject\":\"Job ${JOBNAME} succeeded\",\"msgBody\":\"Job ${JOBNAME} succeeded on ${TODAY} \"}";

    static final String fileExistsError = "{\"emailTo\":\"${EMAIL_RECIPIENTS}\",\"subject\":\"Error in Job ${JOBNAME} step " + FILEEXISTSSTEP
            + "\",\"msgBody\":\"Error in Job ${JOBNAME} " + FILEEXISTSSTEP + ": ${" + FILEEXISTSSTEP + "Error} on ${TODAY} \"}";
		  
    static final String getFileError = "{\"emailTo\":\"${EMAIL_RECIPIENTS}\",\"subject\":\"Error in Job ${JOBNAME} step " + GETFILESTEP
            + "\",\"msgBody\":\"Error in Job ${JOBNAME} " + GETFILESTEP + ": ${" + GETFILESTEP + "Error} on ${TODAY}  \"}";
    		
	Workflow createFilesRetrieveWorkflow() {
		Workflow filesRetrieveWorkflow = new Workflow();
		filesRetrieveWorkflow.setStartStep(GETDATESTEP);
		filesRetrieveWorkflow.setJarUriDependencies(ImmutableList.of("jar://workflows/dynamic/*"));
		filesRetrieveWorkflow.setSteps(ImmutableList.of(getDateStep(CHECKCALENDARSTEP), calendarLookupStep(GETFILESTEP),
				getFileStep(NOTIFICATIONSTEPSUCCESS), notificationStepSuccess(), notificationStepError()));
		filesRetrieveWorkflow.setView(new HashMap<String, String>());
		filesRetrieveWorkflow.setWorkflowURI(fileFetchWorkflowUri);
		decisionApi.deleteWorkflow(fileFetchWorkflowUri);
		decisionApi.putWorkflow(filesRetrieveWorkflow);
		return filesRetrieveWorkflow;
	}

	Workflow createFilesExistWorkflow() {
		Workflow filesExistWorkflow = new Workflow();
		filesExistWorkflow.setStartStep(GETDATESTEP);
		filesExistWorkflow.setJarUriDependencies(ImmutableList.of("jar://workflows/dynamic/*"));
		filesExistWorkflow.setSteps(ImmutableList.of(getDateStep(CHECKCALENDARSTEP), calendarLookupStep(FILEEXISTSSTEP),
				fileExistsStep(COPYFILESTEP), copyFileStep(NOTIFICATIONSTEPSUCCESS), notificationStepSuccess(),
				notificationStepError()));
		filesExistWorkflow.setView(new HashMap<String, String>());
		filesExistWorkflow.setWorkflowURI(fileExistsWorkflowUri);
		decisionApi.deleteWorkflow(fileExistsWorkflowUri);
		decisionApi.putWorkflow(filesExistWorkflow);
		return filesExistWorkflow;
	}
	
	Transition errorTransition() {
		Transition errorTransition = new Transition();
		errorTransition.setName("");
		errorTransition.setTargetStep(NOTIFICATIONSTEPERROR);
		return errorTransition;
	}

	Step calendarLookupStep(String nextStepName) {

		Step calendarLookupStep = new Step();
		calendarLookupStep.setExecutable("dp_java_invocable://calendar.steps.CalendarLookupStep");
		calendarLookupStep.setName(CHECKCALENDARSTEP);
		calendarLookupStep.setDescription("Check if today is defined as a holiday.");
		calendarLookupStep.setView(ImmutableMap.of("DEFAULT_TRANSLATOR", "#" + translationUri));
		calendarLookupStep.setView(ImmutableMap.of("DEFAULT_TRANSLATOR", translationUri));
		List<Transition> calendarLookupTransitions = new LinkedList<>();
		Transition calendarNext = new Transition();
		calendarNext.setName(Steps.NEXT.toString());
		calendarNext.setTargetStep(nextStepName);
		calendarLookupTransitions.add(calendarNext);

		Transition calendarWeekend = new Transition();
		calendarWeekend.setName(WEEKEND);
		calendarWeekend.setTargetStep("$RETURN");
		calendarLookupTransitions.add(calendarWeekend);

		Transition calendarHoliday = new Transition();
		calendarHoliday.setName(HOLIDAY);
		calendarHoliday.setTargetStep("$RETURN");
		calendarLookupTransitions.add(calendarHoliday);

		calendarLookupTransitions.add(errorTransition());

		calendarLookupStep.setTransitions(calendarLookupTransitions);
		return calendarLookupStep;
	}

	private Step getDateStep(String nextStepName) {
		String scriptURI = "script://matrix/getDate";
		if (scriptApi.doesScriptExist(scriptURI))
			scriptApi.deleteScript(scriptURI);

		RaptureScript script = new RaptureScript();
		script.setLanguage(RaptureScriptLanguage.REFLEX);
		script.setPurpose(RaptureScriptPurpose.PROGRAM);
		script.setName("getDate");
        script.setScript("today = date();\n now = time();\n yesterday = today - 1;\n workerURI = _params['$DP_WORKER_URI'];\n "
                + "#decision.setContextLiteral(workerURI, 'YESTERDAY', dateformat(yesterday, 'yyyyMMdd', 'America/New_York'));\n "
                + "#decision.setContextLiteral(workerURI, 'YESTERDAY2', dateformat(yesterday, 'yyyy-MM-dd', 'America/New_York'));\n "
                + "#decision.setContextLiteral(workerURI, 'TODAY', dateformat(today, 'yyyyMMdd', 'America/New_York'));\n "
                + "#decision.setContextLiteral(workerURI, 'TODAY2', dateformat(today, 'yyyy-MM-dd', 'America/New_York'));\n "
                + "#decision.setContextLiteral(workerURI, 'TIME', dateformat(now, 'hhmmss', 'America/New_York'));\n " + "return'next';");
		
		
		script.setAuthority("matrix");
		scriptApi.putScript(scriptURI, script);

		Step getDateStep = new Step();
		getDateStep.setExecutable(scriptURI);
		getDateStep.setName(GETDATESTEP);
		getDateStep.setDescription("Get today's date as a string");
		Transition next = new Transition();
		next.setName(Steps.NEXT.toString());
		next.setTargetStep(nextStepName);
		getDateStep.setTransitions(ImmutableList.of(next));
		return getDateStep;
	}

	Step fileExistsStep(String nextStepName) {
		Step fileExistsStep = new Step();
		fileExistsStep.setExecutable("dp_java_invocable://ftp.steps.CheckFileExistsStep");
		fileExistsStep.setName(FILEEXISTSSTEP);
		fileExistsStep.setDescription("Get file from a remote location");
		List<Transition> fileExistsTransitions = new LinkedList<>();

		Transition existsNext = new Transition();
		existsNext.setName(Steps.NEXT.toString());
		existsNext.setTargetStep(nextStepName);
		fileExistsTransitions.add(existsNext);
		fileExistsTransitions.add(errorTransition());

		fileExistsStep.setTransitions(fileExistsTransitions);
		return fileExistsStep;
	}

	Step getFileStep(String nextStepName) {
		Step getFileStep = new Step();
		getFileStep.setExecutable("dp_java_invocable://ftp.steps.GetFileStep");
		getFileStep.setName(GETFILESTEP);
		getFileStep.setDescription("Retrieve files");
		List<Transition> getFileTransitions = new LinkedList<>();
		Transition getFileNext = new Transition();
		getFileNext.setName(Steps.NEXT.toString());
		getFileNext.setTargetStep(nextStepName);
		getFileTransitions.add(getFileNext);
		getFileTransitions.add(errorTransition());
		getFileStep.setTransitions(getFileTransitions);
		return getFileStep;
	}

	Step copyFileStep(String nextStepName) {
		Step copyFileStep = new Step();
		copyFileStep.setExecutable("dp_java_invocable://ftp.steps.CopyFileStep");
		copyFileStep.setName(COPYFILESTEP);
		copyFileStep.setDescription("Copy files");
		List<Transition> copyFileTransitions = new LinkedList<>();
		Transition copyFileNext = new Transition();
		copyFileNext.setName(Steps.NEXT.toString());
		copyFileNext.setTargetStep(nextStepName);
		copyFileTransitions.add(copyFileNext);
		copyFileTransitions.add(errorTransition());
		copyFileStep.setTransitions(copyFileTransitions);
		return copyFileStep;
	}

	Step notificationStepError() {
		Step notificationStepError = new Step();
		notificationStepError.setExecutable("dp_java_invocable://notification.steps.NotificationStep");
		notificationStepError.setName(NOTIFICATIONSTEPERROR);
		notificationStepError.setDescription("description");
		// Use the template for the step that failed
		notificationStepError.setView(ImmutableMap.of("NOTIFY_TEMPLATE", "$STEPNAME"));
		return notificationStepError;
	}

	Step notificationStepSuccess() {
		Step notificationStepSuccess = new Step();
		notificationStepSuccess.setExecutable("dp_java_invocable://notification.steps.NotificationStep");
		notificationStepSuccess.setName(NOTIFICATIONSTEPSUCCESS);
		notificationStepSuccess.setView(ImmutableMap.of("NOTIFY_TEMPLATE", "#" + successTemplateName));
		notificationStepSuccess.setDescription("description");
		return notificationStepSuccess;
	}



}
