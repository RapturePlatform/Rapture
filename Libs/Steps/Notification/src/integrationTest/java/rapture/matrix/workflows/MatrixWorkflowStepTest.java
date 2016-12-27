package rapture.matrix.workflows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import rapture.common.CallingContext;
import rapture.common.CreateResponse;
import rapture.common.RaptureJobExec;
import rapture.common.RaptureScript;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.WorkOrderExecutionState;
import rapture.common.client.HttpBlobApi;
import rapture.common.client.HttpDecisionApi;
import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpJarApi;
import rapture.common.client.HttpScheduleApi;
import rapture.common.client.HttpScriptApi;
import rapture.common.dp.Step;
import rapture.common.dp.StepRecord;
import rapture.common.dp.StepRecordDebug;
import rapture.common.dp.Steps;
import rapture.common.dp.Transition;
import rapture.common.dp.WorkOrderDebug;
import rapture.common.dp.WorkerDebug;
import rapture.common.dp.Workflow;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.ftp.common.FTPConnectionConfig;
import rapture.helper.IntegrationTestHelper;
import rapture.mail.Mailer;

public class MatrixWorkflowStepTest {

    static String saveInitSysConfig;
    static String saveRaptureRepo;
    private static final String auth = "test" + System.currentTimeMillis();
    private static final String REPO_USING_MEMORY = "REP {} USING MEMORY {prefix=\"/tmp/" + auth + "\"}";
    private static final String BLOB_USING_MEMORY = "BLOB {} USING MEMORY {prefix=\"/tmp/B" + auth + "\"}";
    private static final String META_USING_MEMORY = "REP {} USING MEMORY {prefix=\"/tmp/M" + auth + "\"}";

    static String url = "http://192.168.99.100:8665/rapture";
    static String username = "rapture";
    static String password = "rapture";

    private static CallingContext context;

    private static IntegrationTestHelper helper;
    private static HttpJarApi jarApi;
    private static HttpDocApi docApi;
    private static HttpBlobApi blobApi;
    private static HttpScriptApi scriptApi;
    private static HttpScheduleApi scheduleApi;
    private static HttpDecisionApi decisionApi;

    private static String date;

    static final String GETDATESTEP = "getDate";
    static final String DATECHANGESTEP = "dateChange";
    static final String TIMEOUTSTEP = "timeout";
    static final String CHECKCALENDARSTEP = "checkCalendarStep";
    static final String FILEEXISTSSTEP = "fileExistsStep";
    static final String GETFILESTEP = "getFileStep";
    static final String COPYFILESTEP = "copyFileStep";
    static final String NOTIFICATIONSTEPSUCCESS = "notificationStepSuccess";
    static final String NOTIFICATIONSTEPERROR = "notificationStepError";

    static final String WEBHOOK_MATRIX = "https://hooks.slack.com/services/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";
    static final String WEBHOOK_TEST = "https://hooks.slack.com/services/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";

    static final String area = "CONFIG";
    static final String successTemplateName = "TESTING";
    static final String successTemplate = "{\"emailTo\":\"${EMAIL_RECIPIENTS}\",\"subject\":\"Job ${JOBNAME} succeeded\",\"msgBody\":\"Job ${JOBNAME} succeeded on ${TODAY} \"}";

    static final String fileExistsError = "{\"emailTo\":\"${EMAIL_RECIPIENTS}\",\"subject\":\"Error in Job ${JOBNAME} step " + FILEEXISTSSTEP
            + "\",\"msgBody\":\"Error in Job ${JOBNAME} " + FILEEXISTSSTEP + ": ${" + FILEEXISTSSTEP + "Error} on ${TODAY} \"}";

    static final String getFileError = "{\"emailTo\":\"${EMAIL_RECIPIENTS}\",\"subject\":\"Error in Job ${JOBNAME} step " + GETFILESTEP
            + "\",\"msgBody\":\"Error in Job ${JOBNAME} " + GETFILESTEP + ": ${" + GETFILESTEP + "Error} on ${TODAY}  \"}";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        helper = new IntegrationTestHelper(url, username, password);
        jarApi = helper.getJarApi();
        blobApi = helper.getBlobApi();
        docApi = helper.getDocApi();
        scriptApi = helper.getScriptApi();
        scheduleApi = helper.getScheduleApi();
        decisionApi = helper.getDecisionApi();

        date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        context = helper.getRaptureLogin().getContext();
        helper.getSysApi().writeSystemConfig(context, area, Mailer.EMAIL_TEMPLATE_DIR + successTemplateName, successTemplate);
        helper.getSysApi().writeSystemConfig(context, area, Mailer.EMAIL_TEMPLATE_DIR + FILEEXISTSSTEP, fileExistsError);
        helper.getSysApi().writeSystemConfig(context, area, Mailer.EMAIL_TEMPLATE_DIR + GETFILESTEP, getFileError);

        if (jarApi.jarExists("jar://workflows/dynamic/Calendar.jar")) jarApi.deleteJar("jar://workflows/dynamic/Calendar.jar");
        if (jarApi.jarExists("jar://workflows/dynamic/FTPCommon.jar")) jarApi.deleteJar("jar://workflows/dynamic/FTPCommon.jar");

        Path calendarJar = Paths.get("./build/libs/Calendar.jar");
        jarApi.putJar("jar://workflows/dynamic/Calendar.jar", Files.readAllBytes(calendarJar));
        Path ftpcommonJar = Paths.get("../FTPCommon/build/libs/FTPCommon-1.0.0.jar");
        jarApi.putJar("jar://workflows/dynamic/FTPCommon.jar", Files.readAllBytes(ftpcommonJar));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    static String translationUri = "document://test/translation";
    static String calendarCMIUri = "document://test/calendarCMI";
    static String calendarSSSUri = "document://test/calendarSSS";
    static String calendarSCBUri = "document://test/calendarSCB";
    static String calendarMGCUri = "document://test/calendarMGC";

    static String configRepo = "document://test" + System.currentTimeMillis();
    static String ftpConfigUri = configRepo + "/FTPConfiguration";
    static String sftpConfigUri = configRepo + "/SFTPConfiguration";
    static String localConfigUri = configRepo + "/LocalConfiguration";
    static String mcpricesFTPconfigUri = configRepo + "/mcpricesFTPconf";
    static String treasuriesFTPconfigUri = configRepo + "/treasuriesFTPconf";

    static String WEEKEND = "Weekend";
    static String HOLIDAY = "Holiday";
    static String CANCEL = "$CANCEL";

    static boolean initialiseCalendarDocuments = false;

    void initialiseCalendarDocuments() {
        if (initialiseCalendarDocuments) return;
        initialiseCalendarDocuments = true;

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
        translationMap.put("2016-01-18", "Martin Luther King Jr Day");
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
        docApi.putDoc(context, translationUri, JacksonUtil.jsonFromObject(translationMap));

        Map<String, Object> calendarMap = new LinkedHashMap<>();
        calendarMap.put("New Year's Day", HOLIDAY);
        calendarMap.put("Martin Luther King Jr Day", HOLIDAY);
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
        docApi.putDoc(context, calendarMGCUri, JacksonUtil.jsonFromObject(calendarMap));
        docApi.putDoc(context, calendarSCBUri, JacksonUtil.jsonFromObject(calendarMap));
        calendarMap.put("Good Friday", HOLIDAY);
        docApi.putDoc(context, calendarSSSUri, JacksonUtil.jsonFromObject(calendarMap));
        docApi.putDoc(context, calendarCMIUri, JacksonUtil.jsonFromObject(calendarMap));

        FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("speedtest.tele2.net").setPort(23).setLoginId("ftp").setPassword("foo@bar")
                .setUseSFTP(false);

        docApi.putDoc(context, ftpConfigUri, JacksonUtil.jsonFromObject(ftpConfig));

        FTPConnectionConfig sftpConfig = new FTPConnectionConfig().setAddress("speedtest.tele2.net").setPort(23).setLoginId("ftp").setPassword("foo@bar")
                .setUseSFTP(true);

        docApi.putDoc(context, sftpConfigUri, JacksonUtil.jsonFromObject(sftpConfig));

        docApi.putDoc(context, localConfigUri, "{}");

        FTPConnectionConfig mcpricesFTP = new FTPConnectionConfig().setAddress("ftpprod.intdata.com").setPort(23).setLoginId("MATRXFTPH1")
                .setPassword("KritPccH").setUseSFTP(false);
        docApi.putDoc(context, mcpricesFTPconfigUri, JacksonUtil.jsonFromObject(mcpricesFTP));

        FTPConnectionConfig treasuriesFTP = new FTPConnectionConfig().setAddress("ftpprod.intdata.com").setPort(23).setLoginId("MATRXFTPH2")
                .setPassword("fMg3C3q7").setUseSFTP(false);
        docApi.putDoc(context, treasuriesFTPconfigUri, JacksonUtil.jsonFromObject(mcpricesFTP));

        if (!blobApi.blobRepoExists("blob://tmp")) {
            blobApi.createBlobRepo(context, "blob://tmp", BLOB_USING_MEMORY, META_USING_MEMORY);
        }
    }

    String fileExistsWorkflowUri = "workflow://matrix/fileExistsFlow";
    String fileFetchWorkflowUri = "workflow://matrix/fileFetchFlow";
    String untilMidnightWorkflowUri = "workflow://matrix/untilMidnightWorkflow";
    String untilTimeoutWorkflowUri = "workflow://matrix/untilTimeoutWorkflow";

    Transition errorTransition() {
        Transition errorTransition = new Transition();
        errorTransition.setName("");
        errorTransition.setTargetStep(NOTIFICATIONSTEPERROR);
        return errorTransition;
    }

    Step calendarLookupStep(String nextStepName) {
        initialiseCalendarDocuments();

        Step calendarLookupStep = new Step();
        calendarLookupStep.setExecutable("dp_java_invocable://calendar.steps.CalendarLookupStep");
        calendarLookupStep.setName(CHECKCALENDARSTEP);
        calendarLookupStep.setDescription("Check if today is defined as a holiday.");
        // calendarLookupStep.setView(ImmutableMap.of("DEFAULT_TRANSLATOR", "#" + translationUri));
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
        if (scriptApi.doesScriptExist(scriptURI)) scriptApi.deleteScript(context, scriptURI);

        RaptureScript script = new RaptureScript();
        script.setLanguage(RaptureScriptLanguage.REFLEX);
        script.setPurpose(RaptureScriptPurpose.PROGRAM);
        script.setName("getDate");
        script.setScript("today = date();\n now = time();\n yesterday = today - 1;\n workerURI = _params['$DP_WORKER_URI'];\n "
                + "#decision.setContextLiteral(workerURI, 'YESTERDAY', dateformat(yesterday, 'yyyyMMdd', 'America/New_York'));\n "
                + "#decision.setContextLiteral(workerURI, 'YESTERDAY2', dateformat(yesterday, 'yyyy-MM-dd', 'America/New_York'));\n "
                + "#decision.setContextLiteral(workerURI, 'TODAY', dateformat(today, 'yyyyMMdd', 'America/New_York'));\n "
                + "#decision.setContextLiteral(workerURI, 'TODAY2', dateformat(today, 'yyyy-MM-dd', 'America/New_York'));\n "
                + "#decision.setContextLiteral(workerURI, 'TIME', dateformat(now, 'hhmmss', 'America/New_York'));\n "
                + "println('QQQQ TODAY = '+#decision.getContextValue(workerURI, 'TODAY'));\n"
                + "println('QQQQ NOTIFY_TYPE = '+#decision.getContextValue(workerURI, 'NOTIFY_TYPE'));\n"
                + "println('QQQQ CALENDAR = '+#decision.getContextValue(workerURI, 'CALENDAR'));\n" + "return'next';");

        script.setAuthority("matrix");
        scriptApi.putScript(scriptURI, script);
        
        Step getDateStep = new Step();
        getDateStep.setExecutable(scriptURI);
        getDateStep.setName(GETDATESTEP);
        getDateStep.setDescription("Get today's date as a string");
        Transition next = new Transition();
        next.setName(Steps.NEXT.toString());
        next.setTargetStep(nextStepName);
        getDateStep.setTransitions(ImmutableList.of(next, errorTransition()));
        return getDateStep;
    }

    private Step dateChangeStep(String nextStepName) {
        String scriptURI = "script://matrix/dateChange";
        if (scriptApi.doesScriptExist(scriptURI)) scriptApi.deleteScript(context, scriptURI);

        RaptureScript dateChangeScript = new RaptureScript();
        dateChangeScript.setLanguage(RaptureScriptLanguage.REFLEX);
        dateChangeScript.setPurpose(RaptureScriptPurpose.PROGRAM);
        dateChangeScript.setName("dateChange");
        dateChangeScript.setScript(
                "checkDay1 = #decision.getContextLiteral(workerURI, 'TODAY');\n " + "checkDay2 = dateformat(date(), 'yyyy-MM-dd', 'America/New_York');\n "
                        + "if (checkDay1 == checkDay2) do \n sleep(300); \n return 'next';\n else do \n return 'error';\n end");
        dateChangeScript.setAuthority("matrix");
        scriptApi.putScript(scriptURI, dateChangeScript);

        Step dateChangeStep = new Step();
        dateChangeStep.setExecutable(scriptURI);
        dateChangeStep.setName(DATECHANGESTEP);
        dateChangeStep.setDescription("Get today's date as a string");
        Transition next = new Transition();
        next.setName(Steps.NEXT.toString());
        next.setTargetStep(nextStepName);
        dateChangeStep.setTransitions(ImmutableList.of(next, errorTransition()));
        return dateChangeStep;
    }

    private Step timeoutStep(String nextStepName) {
        String scriptURI = "script://matrix/timeout";
        if (scriptApi.doesScriptExist(scriptURI)) scriptApi.deleteScript(context, scriptURI);

        RaptureScript timeoutScript = new RaptureScript();
        timeoutScript.setLanguage(RaptureScriptLanguage.REFLEX);
        timeoutScript.setPurpose(RaptureScriptPurpose.PROGRAM);
        timeoutScript.setName("timeout");
        timeoutScript.setScript("t1 = time(#decision.getContextLiteral(workerURI, 'TIMEOUT'));\nt2 = time(''+time());\n"
                + "if (t2 > t1) do \n return \'timeout\';\nelse do\n return 'next';\nend");
        timeoutScript.setAuthority("matrix");
        scriptApi.putScript(scriptURI, timeoutScript);

        Step timeoutStep = new Step();
        timeoutStep.setExecutable(scriptURI);
        timeoutStep.setName(TIMEOUTSTEP);
        timeoutStep.setDescription("Return 'timeout' if the current time is beyond the specified value");
        Transition next = new Transition();
        next.setName(Steps.NEXT.toString());
        next.setTargetStep(nextStepName);
        Transition timeout = new Transition();
        timeout.setName("timeout");
        timeout.setTargetStep(NOTIFICATIONSTEPERROR);
        timeoutStep.setTransitions(ImmutableList.of(next, timeout, errorTransition()));
        return timeoutStep;
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
        // notificationStepError.setView(ImmutableMap.of("MESSAGE_TEMPLATE", "$STEPNAME", "SLACK_WEBHOOK", "#" + WEBHOOK_TEST, "NOTIFY_TYPE", "#SLACK"));
        // notificationStepError.setView(ImmutableMap.of("MESSAGE_TEMPLATE", "$STEPNAME"));

        
        return notificationStepError;
    }

    Step notificationStepSuccess() {
        Step notificationStepSuccess = new Step();
        notificationStepSuccess.setExecutable("dp_java_invocable://notification.steps.NotificationStep");
        notificationStepSuccess.setName(NOTIFICATIONSTEPSUCCESS);
        // notificationStepSuccess
        // .setView(ImmutableMap.of("MESSAGE_TEMPLATE", "#" + successTemplateName, "SLACK_WEBHOOK", "#" + WEBHOOK_TEST, "NOTIFY_TYPE", "#SLACK"));
        // notificationStepSuccess
        // .setView(ImmutableMap.of("MESSAGE_TEMPLATE", "#" + successTemplateName));

        notificationStepSuccess.setDescription("description");
        return notificationStepSuccess;
    }

    Workflow filesRetrieveWorkflow() {
        Workflow filesRetrieveWorkflow = new Workflow();
        filesRetrieveWorkflow.setStartStep(GETDATESTEP);
        filesRetrieveWorkflow.setJarUriDependencies(ImmutableList.of("jar://workflows/dynamic/*"));
        filesRetrieveWorkflow.setSteps(ImmutableList.of(getDateStep(CHECKCALENDARSTEP),calendarLookupStep(GETFILESTEP), getFileStep(NOTIFICATIONSTEPSUCCESS), notificationStepSuccess(),
                notificationStepError()));
        filesRetrieveWorkflow.setView(new HashMap<String, String>());
        filesRetrieveWorkflow.setWorkflowURI(fileFetchWorkflowUri);
        decisionApi.deleteWorkflow(context, fileFetchWorkflowUri);
        decisionApi.putWorkflow(context, filesRetrieveWorkflow);
        return filesRetrieveWorkflow;
    }

    Workflow filesExistWorkflow() {
        Workflow filesExistWorkflow = new Workflow();
        filesExistWorkflow.setStartStep(GETDATESTEP);
        filesExistWorkflow.setJarUriDependencies(ImmutableList.of("jar://workflows/dynamic/*"));
        filesExistWorkflow.setSteps(ImmutableList.of(getDateStep(CHECKCALENDARSTEP), calendarLookupStep(FILEEXISTSSTEP),
                fileExistsStep(COPYFILESTEP), copyFileStep(NOTIFICATIONSTEPSUCCESS), notificationStepSuccess(),
                notificationStepError()));
        filesExistWorkflow.setView(new HashMap<String, String>());
        filesExistWorkflow.setWorkflowURI(fileExistsWorkflowUri);
        decisionApi.deleteWorkflow(context, fileExistsWorkflowUri);
        decisionApi.putWorkflow(context, filesExistWorkflow);
        return filesExistWorkflow;
    }

    Workflow untilMidnightWorkflow(String file1, String file2) {
        Workflow untilMidnightWorkflow = new Workflow();
        untilMidnightWorkflow.setStartStep(GETDATESTEP);
        untilMidnightWorkflow.setJarUriDependencies(ImmutableList.of("jar://workflows/dynamic/*"));

        // First get yesterday's date
        Step getDateStep = getDateStep(CHECKCALENDARSTEP);

        // See if yesterday was a holiday
        Step calendarLookupYesterdayStep = calendarLookupStep(FILEEXISTSSTEP);
        // calendarLookupYesterdayStep.setView(ImmutableMap.of("DATE", "$YESTERDAY2", "DEFAULT_TRANSLATOR", "#" + translationUri));

        // See if the first step completed
        Step fileExistsStep = fileExistsStep(FILEEXISTSSTEP + "2");
        fileExistsStep.setView(ImmutableMap.of("EXIST_FILENAMES", "#" + JacksonUtil.jsonFromObject(ImmutableMap.of(file1, Boolean.TRUE))));

        // See if the second step completed
        Step fileExistsStep2 = fileExistsStep(NOTIFICATIONSTEPSUCCESS);
        fileExistsStep2.setName(FILEEXISTSSTEP + "2");
        fileExistsStep2.setView(ImmutableMap.of("EXIST_FILENAMES", "#" + JacksonUtil.jsonFromObject(ImmutableMap.of(file2, Boolean.TRUE))));
        List<Transition> transitions = new ArrayList<>();
        transitions.addAll(fileExistsStep.getTransitions());
        Transition notFinished = new Transition();
        notFinished.setName(Steps.ERROR.toString());
        notFinished.setTargetStep(DATECHANGESTEP);
        transitions.add(notFinished);
        fileExistsStep.setTransitions(transitions);

        // Check date and repeat
        Step dateChangeStep = dateChangeStep(FILEEXISTSSTEP + "2");

        untilMidnightWorkflow.setSteps(ImmutableList.of(getDateStep, calendarLookupYesterdayStep, fileExistsStep, fileExistsStep2, dateChangeStep,
                notificationStepSuccess(), notificationStepError()));
        untilMidnightWorkflow.setView(new HashMap<String, String>());
        untilMidnightWorkflow.setWorkflowURI(untilMidnightWorkflowUri);
        decisionApi.deleteWorkflow(context, untilMidnightWorkflowUri);
        decisionApi.putWorkflow(context, untilMidnightWorkflow);
        return untilMidnightWorkflow;
    }

    Workflow untilTimeoutWorkflow() {
        Workflow untilTimeoutWorkflow = new Workflow();
        untilTimeoutWorkflow.setStartStep(GETDATESTEP);
        untilTimeoutWorkflow.setJarUriDependencies(ImmutableList.of("jar://workflows/dynamic/*"));

        untilTimeoutWorkflow
                .setSteps(ImmutableList.of(getDateStep(CHECKCALENDARSTEP), calendarLookupStep(FILEEXISTSSTEP), fileExistsStep(TIMEOUTSTEP),
                        timeoutStep(FILEEXISTSSTEP), notificationStepSuccess(), notificationStepError()));
        untilTimeoutWorkflow.setView(new HashMap<String, String>());
        untilTimeoutWorkflow.setWorkflowURI(untilTimeoutWorkflowUri);
        decisionApi.deleteWorkflow(context, untilTimeoutWorkflowUri);
        decisionApi.putWorkflow(context, untilTimeoutWorkflow);
        return untilTimeoutWorkflow;
    }

    @Test
    public void testMatrixWorkflowStep() {

        // If the arguments are part of the job definition then they cannot be overridden.
        Map<String, String> args = new HashMap<>();
        args.put("SLACK_WEBHOOK", WEBHOOK_TEST);
        args.put("NOTIFY_TYPE", "SLACK");

        args.put("CALENDAR", calendarCMIUri);
        args.put("TRANSLATOR", translationUri);

        args.put("FTP_CONFIGURATION", ftpConfigUri);
        args.put("COPY_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("1KB.zip", "blob://tmp/1KB.zip")));

        CreateResponse response = decisionApi.createWorkOrderP(context, filesExistWorkflow().getWorkflowURI(), args, null);
        assertTrue(response.getIsCreated());
        WorkOrderDebug debug;
        WorkOrderExecutionState state = WorkOrderExecutionState.NEW;
        long timeout = System.currentTimeMillis() + 60000;
        do {
            debug = decisionApi.getWorkOrderDebug(context, response.getUri());
            state = debug.getOrder().getStatus();
        } while (((state == WorkOrderExecutionState.NEW) || (state == WorkOrderExecutionState.ACTIVE)) && (System.currentTimeMillis() < timeout));

        for (WorkerDebug wd : debug.getWorkerDebugs()) {
            for (StepRecordDebug srd : wd.getStepRecordDebugs()) {
                StepRecord sr = srd.getStepRecord();
                System.out.println(sr.toString());
            }
        }

        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
    }

    // This job flags an error if any of three files are present
    public Map<String, String> chkcurrent_CMI_args() {
        Map<String, String> chkcurrent_CMI_args = new HashMap<>();
        chkcurrent_CMI_args.put("EMAIL_RECIPIENTS", "DL_FilesDetectedInCurrent@matrixapps.com");
        chkcurrent_CMI_args.put("SLACK_WEBHOOK", WEBHOOK_TEST);
        chkcurrent_CMI_args.put("NOTIFY_TYPE", "SLACK");
        chkcurrent_CMI_args.put("CALENDAR", calendarCMIUri);
        chkcurrent_CMI_args.put("TRANSLATOR", translationUri);
        chkcurrent_CMI_args.put("JOBNAME", "chkcurrent_CMI");
        chkcurrent_CMI_args.put("EXIST_FILENAMES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("file://mnt/apps/tbOCMI/appServer/feeds/current/bbtsprice.txt", Boolean.FALSE,
                        "file://mnt/apps/tbOCMI/appServer/feeds/current/embs_ready.txt", Boolean.FALSE,
                        "file://mnt/apps/tbOCMI/appServer/feeds/current/bb_intraday_positions.txt", Boolean.FALSE)));

        scheduleApi.createWorkflowJob(context, "job://matrix/chkcurrent_CMI", "SampleMatrixScript", filesExistWorkflow().getWorkflowURI(), "30 06-11 * * 1-5 *",
                "America/New_York", chkcurrent_CMI_args, true, 5, "");
        return chkcurrent_CMI_args;
    }

    // This job flags an error if any of three files are present
    public Map<String, String> chkcurrent_SCB_args() {
        Map<String, String> chkcurrent_SCB_args = new HashMap<>();
        chkcurrent_SCB_args.put("EMAIL_RECIPIENTS", "DL_FilesDetectedInCurrent@matrixapps.com");
        chkcurrent_SCB_args.put("SLACK_WEBHOOK", WEBHOOK_TEST);
        chkcurrent_SCB_args.put("NOTIFY_TYPE", "SLACK");
        chkcurrent_SCB_args.put("CALENDAR", calendarSCBUri);
        chkcurrent_SCB_args.put("TRANSLATOR", translationUri);
        chkcurrent_SCB_args.put("JOBNAME", "chkcurrent_SCB");
        chkcurrent_SCB_args.put("EXIST_FILENAMES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("file://mnt/apps/tbOSCB/appServer/feeds/current/bbtsprice.txt", Boolean.FALSE,
                        "file://mnt/apps/tbOSCB/appServer/feeds/current/embs_ready.txt", Boolean.FALSE,
                        "file://mnt/apps/tbOSCB/appServer/feeds/current/bb_intraday_positions.txt", Boolean.FALSE)));

        scheduleApi.createWorkflowJob(context, "job://matrix/chkcurrent_SCB", "SampleMatrixScript", filesExistWorkflow().getWorkflowURI(), "30 06-11 * * 1-5 *",
                "America/New_York", chkcurrent_SCB_args, true, 5, "");
        return chkcurrent_SCB_args;
    }

    // This job flags an error if one file is not present
    public Map<String, String> idsialert_args() {
        Map<String, String> idsialert_args = new HashMap<>();
        idsialert_args.put("EMAIL_RECIPIENTS", "steve.greig@matrixapps.com");
        idsialert_args.put("SLACK_WEBHOOK", WEBHOOK_TEST);
        idsialert_args.put("NOTIFY_TYPE", "SLACK");
        idsialert_args.put("CALENDAR", calendarCMIUri);
        idsialert_args.put("TRANSLATOR", translationUri);
        idsialert_args.put("JOBNAME", "idsialert");
        idsialert_args.put("EXIST_FILENAMES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("file://mnt/apps/tbOCMI/appServer/feeds/current/idsiPrice.txt", Boolean.TRUE)));

        scheduleApi.createWorkflowJob(context, "job://matrix/idsialert", "SampleMatrixScript", filesExistWorkflow().getWorkflowURI(), "15 19 * * 1-5 *",
                "America/New_York", idsialert_args, true, 5, "");
        return idsialert_args;
    }

    // This job flags an error if one file is not present
    public Map<String, String> idsimbsalert_args() {
        Map<String, String> idsimbsalert_args = new HashMap<>();
        idsimbsalert_args.put("EMAIL_RECIPIENTS", "steve.greig@matrixapps.com");
        idsimbsalert_args.put("SLACK_WEBHOOK", WEBHOOK_TEST);
        idsimbsalert_args.put("NOTIFY_TYPE", "SLACK");
        idsimbsalert_args.put("CALENDAR", calendarCMIUri);
        idsimbsalert_args.put("TRANSLATOR", translationUri);
        idsimbsalert_args.put("JOBNAME", "idsimbsalert");
        idsimbsalert_args.put("EXIST_FILENAMES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("file://mnt/apps/tbOCMI/appServer/feeds/current/idsi_mbs.txt", Boolean.TRUE)));

        scheduleApi.createWorkflowJob(context, "job://matrix/idsimbsalert", "SampleMatrixScript", filesExistWorkflow().getWorkflowURI(), "15 19 * * 1-5 *",
                "America/New_York", idsimbsalert_args, true, 5, "");
        return idsimbsalert_args;
    }

    // This job flags an error if one file is not present
    public Map<String, String> nosalert_args() {
        Map<String, String> nosalert_args = new HashMap<>();
        nosalert_args.put("EMAIL_RECIPIENTS", "steve.greig@matrixapps.com");
        nosalert_args.put("SLACK_WEBHOOK", WEBHOOK_TEST);
        nosalert_args.put("NOTIFY_TYPE", "SLACK");
        nosalert_args.put("CALENDAR", calendarCMIUri);
        nosalert_args.put("TRANSLATOR", translationUri);
        nosalert_args.put("JOBNAME", "nosalert");
        nosalert_args.put("EXIST_FILENAMES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("file://mnt/apps/tbOCMI/appServer/feeds/current/MBSDFEED", Boolean.TRUE)));

        scheduleApi.createWorkflowJob(context, "job://matrix/chkcurrent_SCB", "SampleMatrixScript", filesExistWorkflow().getWorkflowURI(), "15 19 * * 1-5 *",
                "America/New_York", nosalert_args, true, 5, "");
        return nosalert_args;
    }

    // This job retrieves a file via FTP and stores it in Rapture, then copies it to 3 places
    public Map<String, String> mcprices_args() {
        Map<String, String> mcprices_args = new HashMap<>();
        mcprices_args.put("EMAIL_RECIPIENTS", "steve.greig@matrixapps.com");
        mcprices_args.put("SLACK_WEBHOOK", WEBHOOK_TEST);
        mcprices_args.put("NOTIFY_TYPE", "SLACK");
        mcprices_args.put("CALENDAR", calendarMGCUri);
        mcprices_args.put("TRANSLATOR", translationUri);
        mcprices_args.put("JOBNAME", "mcprices");
        // mcprices_args.put("FTP_CONFIGURATION", mcpricesFTPconfigUri);
        mcprices_args.put("GET_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("MATRXFTPH1/mcprice.${TODAY}", "blob://matrix/mcprice.${TODAY}")));
        mcprices_args.put("COPY_FILES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("blob://matrix/mcprice.${TODAY}",
                        ImmutableList.of("file://mnt/apps/tbOMGC/appServer/feeds/current/idsiPrice_NP.txt",
                                "file://mnt/appsqa/tbOMGC/appServer/feeds/current/idsiPrice_NP.txt",
                                "file://mnt/tapps/tbOMGC/appServer/feeds/current/idsiPrice_NP.txt"))));

        scheduleApi.createWorkflowJob(context, "job://matrix/mcprices", "SampleMatrixScript", filesRetrieveWorkflow().getWorkflowURI(), "15 19 * * 1-5 *",
                "America/New_York", mcprices_args, true, 5, "");
        return mcprices_args;
    }

    // This job retrieves a file via FTP and stores it in Rapture, then copies it to 2 places
    public Map<String, String> downloadtreasuries_args() {
        Map<String, String> treasuries_args = new HashMap<>();
        treasuries_args.put("EMAIL_RECIPIENTS", "steve.greig@matrixapps.com,mohammad.ejaz@matrixapps.com");
        treasuries_args.put("SLACK_WEBHOOK", WEBHOOK_TEST);
        treasuries_args.put("NOTIFY_TYPE", "SLACK");
        treasuries_args.put("CALENDAR", calendarSSSUri);
        treasuries_args.put("TRANSLATOR", translationUri);
        treasuries_args.put("JOBNAME", "downloadtreasuries");
        // mcprices_args.put("FTP_CONFIGURATION", treasuriesFTPconfigUri);
        treasuries_args.put("GET_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of("MATRXFTPH2/treasuries.${TODAY}", "blob://matrix/treasuries.${TODAY}")));
        treasuries_args.put("COPY_FILES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("blob://matrix/treasuries.${TODAY}",
                        ImmutableList.of("file://mnt/32osData/matrix/IncomingFTP/${TODAY2}/SSS/treasuries.${TODAY}",
                                "file://mnt/32osData/matrix/IncomingFTP/${TODAY2}/SSS/treasuries.txt"))));

        scheduleApi.createWorkflowJob(context, "job://matrix/downloadtreasuries", "SampleMatrixScript", filesRetrieveWorkflow().getWorkflowURI(),
                "15 19 * * 1-5 *",
                "America/New_York", treasuries_args, true, 5, "");
        return treasuries_args;
    }

    // This job checks if a file exists and if so copies it to 2 places
    public Map<String, String> secfeed_args() {
        Map<String, String> secfeed_args = new HashMap<>();
        secfeed_args.put("EMAIL_RECIPIENTS", "steve.greig@matrixapps.com");
        secfeed_args.put("SLACK_WEBHOOK", WEBHOOK_TEST);
        secfeed_args.put("NOTIFY_TYPE", "SLACK");
        secfeed_args.put("CALENDAR", calendarCMIUri);
        secfeed_args.put("TRANSLATOR", translationUri);
        secfeed_args.put("JOBNAME", "secfeed");
        secfeed_args.put("EXIST_FILENAMES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("file://mnt/xdrive/sss/Futures/secfeed.txt", Boolean.TRUE)));
        secfeed_args.put("COPY_FILES",
                JacksonUtil.jsonFromObject(
                        ImmutableMap.of("file://mnt/xdrive/sss/Futures/secfeed.txt", ImmutableList.of("blob://matrix/${TODAY}/secfeed.txt",
                                "file://mnt/xdrive/sss/Futures/secfeed_${TODAY}_${TIME}.txt", "file://mnt/apps/tbOSSS/appServer/feeds/current/secfeed.txt"))));

        scheduleApi.createWorkflowJob(context, "job://matrix/secfeed", "SampleMatrixScript", filesExistWorkflow().getWorkflowURI(), "15 19 * * 1-5 *",
                "America/New_York", secfeed_args, true, 5, "");
        return secfeed_args;
    }

    // This job checks for the existence of a file. If it exists then the original script tries to create a 'lock file' but we should be able to accomplish that
    // with workflow semaphores
    public Map<String, String> bbtsprice_args() {
        Map<String, String> bbtsprice_args = new HashMap<>();
        bbtsprice_args.put("EMAIL_RECIPIENTS", "stephen.rehm@bnymellon.com john.e.kelly@bnymellon.com mohammad.ejaz@matrixapps.com steve.greig@matrixapps.com");
        bbtsprice_args.put("SLACK_WEBHOOK", WEBHOOK_TEST);
        bbtsprice_args.put("NOTIFY_TYPE", "SLACK");
        bbtsprice_args.put("CALENDAR", calendarCMIUri);
        bbtsprice_args.put("TRANSLATOR", translationUri);
        bbtsprice_args.put("JOBNAME", "bbtsprice");
        bbtsprice_args.put("EXIST_FILENAMES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("file://mnt/apps/tbOCMI/appServer/feeds/current/bbtsprice.txt", Boolean.TRUE)));
        bbtsprice_args.put("COPY_FILES", JacksonUtil.jsonFromObject(ImmutableMap.of()));

        scheduleApi.createWorkflowJob(context, "job://matrix/bbtsprice", "SampleMatrixScript", filesExistWorkflow().getWorkflowURI(), "15 19 * * 1-5 *",
                "America/New_York", bbtsprice_args, true, 5, "");
        return bbtsprice_args;
    }

    // This job checks for the existence of a file. If it exists then the original script tries to create a 'lock file' but we should be able to accomplish that
    // with workflow semaphores
    public Map<String, String> cmi_secondwatch_args() {
        Map<String, String> cmi_secondwatch_args = new HashMap<>();
        cmi_secondwatch_args.put("EMAIL_RECIPIENTS",
                "steve.greig@matrixapps.com mohammad.ejaz@matrixapps.com stephen.mellert@matrixapps.com ben.schulson@matrixapps.com robert.fossella@matrixapps.com james.t.neese@matrixapps.com");
        cmi_secondwatch_args.put("SLACK_WEBHOOK", WEBHOOK_TEST);
        cmi_secondwatch_args.put("NOTIFY_TYPE", "SLACK");
        cmi_secondwatch_args.put("CALENDAR", calendarCMIUri);
        cmi_secondwatch_args.put("TRANSLATOR", translationUri);
        cmi_secondwatch_args.put("JOBNAME", "cmi_secondwatch");

        scheduleApi.createWorkflowJob(context, "job://matrix/cmi_secondwatch", "SampleMatrixScript",
                untilMidnightWorkflow("file://tmp/cmicompleted", "file://mnt/apps/tbOCMI/appServer/logs/${TODAY2}/BATCHDONE_.{4}.log").getWorkflowURI(),
                "15 19 * * 1-5 *", "America/New_York", cmi_secondwatch_args, true, 5, "");
        return cmi_secondwatch_args;
    }

    // This job checks for the existence of a file. If it exists then the original script tries to create a 'lock file' but we should be able to accomplish that
    // with workflow semaphores
    public Map<String, String> scb_secondwatch_args() {
        Map<String, String> scb_secondwatch_args = new HashMap<>();
        scb_secondwatch_args.put("EMAIL_RECIPIENTS",
                "9176914139@cingularme.com 9177108012@vtext.com 2016009107@vtext.com 9177976025@vtext.com  steve.greig@matrixapps.com mohammad.ejaz@matrixapps.com stephen.mellert@matrixapps.com");
        scb_secondwatch_args.put("SLACK_WEBHOOK", WEBHOOK_TEST);
        scb_secondwatch_args.put("NOTIFY_TYPE", "SLACK");
        scb_secondwatch_args.put("CALENDAR", calendarSCBUri);
        scb_secondwatch_args.put("TRANSLATOR", translationUri);
        scb_secondwatch_args.put("JOBNAME", "scb_secondwatch");

        scheduleApi.createWorkflowJob(context, "job://matrix/scb_secondwatch", "SampleMatrixScript",
                untilMidnightWorkflow("file://tmp/scbcompleted", "file://mnt/apps/tbOSCB/appServer/logs/${TODAY2}/BATCHDONE_.{4}.log").getWorkflowURI(),
                "15 19 * * 1-5 *", "America/New_York", scb_secondwatch_args, true, 5, "");
        return scb_secondwatch_args;
    }

    // This job checks for the existence of a file. If it exists then the original script tries to create a 'lock file' but we should be able to accomplish that
    // with workflow semaphores
    public Map<String, String> sss_secondwatch_args() {
        Map<String, String> sss_secondwatch_args = new HashMap<>();
        sss_secondwatch_args.put("EMAIL_RECIPIENTS",
                "9177108012@vtext.com 2016009107@vtext.com 9177976025@vtext.com steve.greig@matrixapps.com mohammad.ejaz@matrixapps.com stephen.mellert@matrixapps.com ben.schulson@matrixapps.com 9176914139@vtext.com robert.fossella@matrixapps.com james.t.neese@matrixapps.com");
        sss_secondwatch_args.put("SLACK_WEBHOOK", WEBHOOK_TEST);
        sss_secondwatch_args.put("NOTIFY_TYPE", "SLACK");
        sss_secondwatch_args.put("CALENDAR", calendarSSSUri);
        sss_secondwatch_args.put("TRANSLATOR", translationUri);
        sss_secondwatch_args.put("JOBNAME", "sss_secondwatch");

        scheduleApi.createWorkflowJob(context, "job://matrix/sss_secondwatch", "SampleMatrixScript",
                untilMidnightWorkflow("file://tmp/ssscompleted", "file://mnt/apps/tbOSSS/appServer/logs/${TODAY2}/BATCHDONE_.{4}.log").getWorkflowURI(),
                "15 19 * * 1-5 *", "America/New_York", sss_secondwatch_args, true, 5, "");
        return sss_secondwatch_args;
    }

    public Map<String, String> cmi_batchstartmon_args() {
        Map<String, String> cmi_batchstartmon_args = new HashMap<>();
        cmi_batchstartmon_args.put("EMAIL_RECIPIENTS",
                "steve.greig@matrixapps.com mohammad.ejaz@matrixapps.com stephen.mellert@matrixapps.com ben.schulson@matrixapps.com robert.fossella@matrixapps.com james.t.neese@matrixapps.com stevegreig@txt.att.net mohammad@txt.att.net 6464988748@txt.att.net 9176914139@vtext.com 9173013166@vtext.com 9177018483@vtext.com");
        cmi_batchstartmon_args.put("SLACK_WEBHOOK", WEBHOOK_TEST);
        cmi_batchstartmon_args.put("NOTIFY_TYPE", "SLACK");
        cmi_batchstartmon_args.put("CALENDAR", calendarCMIUri);
        cmi_batchstartmon_args.put("TRANSLATOR", translationUri);
        cmi_batchstartmon_args.put("JOBNAME", "cmi_batchstartmon");
        cmi_batchstartmon_args.put("EXIST_FILENAMES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("file://mnt/apps/tbOCMI/appServer/logs/${TODAY2}/TBLDUMP0_{4}.log", Boolean.TRUE)));
        cmi_batchstartmon_args.put("TIMEOUT", "17:30:00");
        scheduleApi.createWorkflowJob(context, "job://matrix/cmi_batchstartmon", "SampleMatrixScript",
                untilTimeoutWorkflow().getWorkflowURI(),
                "15 19 * * 1-5 *", "America/New_York", cmi_batchstartmon_args, true, 5, "");
        return cmi_batchstartmon_args;
    }

    public Map<String, String> scb_batchstartmon_args() {
        Map<String, String> scb_batchstartmon_args = new HashMap<>();
        scb_batchstartmon_args.put("EMAIL_RECIPIENTS",
                "steve.greig@matrixapps.com mohammad.ejaz@matrixapps.com stephen.mellert@matrixapps.com ben.schulson@matrixapps.com robert.fossella@matrixapps.com james.t.neese@matrixapps.com stevegreig@txt.att.net mohammad@txt.att.net 6464988748@txt.att.net 9176914139@vtext.com 9173013166@vtext.com 9177018483@vtext.com");
        scb_batchstartmon_args.put("SLACK_WEBHOOK", WEBHOOK_TEST);
        scb_batchstartmon_args.put("NOTIFY_TYPE", "SLACK");
        scb_batchstartmon_args.put("CALENDAR", calendarSCBUri);
        scb_batchstartmon_args.put("TRANSLATOR", translationUri);
        scb_batchstartmon_args.put("JOBNAME", "scb_batchstartmon");
        scb_batchstartmon_args.put("EXIST_FILENAMES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("file://mnt/apps/tbOscb/appServer/logs/${TODAY2}/TBLDUMP0_{4}.log", Boolean.TRUE)));
        scb_batchstartmon_args.put("TIMEOUT", "17:30:00");
        scheduleApi.createWorkflowJob(context, "job://matrix/scb_batchstartmon", "SampleMatrixScript", untilTimeoutWorkflow().getWorkflowURI(),
                "15 19 * * 1-5 *", "America/New_York", scb_batchstartmon_args, true, 5, "");
        return scb_batchstartmon_args;
    }

    public Map<String, String> sss_batchstartmon_args() {
        Map<String, String> sss_batchstartmon_args = new HashMap<>();
        sss_batchstartmon_args.put("EMAIL_RECIPIENTS",
                "steve.greig@matrixapps.com mohammad.ejaz@matrixapps.com stephen.mellert@matrixapps.com ben.schulson@matrixapps.com robert.fossella@matrixapps.com james.t.neese@matrixapps.com stevegreig@txt.att.net mohammad@txt.att.net 6464988748@txt.att.net 9176914139@vtext.com 9173013166@vtext.com 9177018483@vtext.com");
        sss_batchstartmon_args.put("SLACK_WEBHOOK", WEBHOOK_TEST);
        sss_batchstartmon_args.put("NOTIFY_TYPE", "SLACK");
        sss_batchstartmon_args.put("CALENDAR", calendarSSSUri);
        sss_batchstartmon_args.put("TRANSLATOR", translationUri);
        sss_batchstartmon_args.put("JOBNAME", "sss_batchstartmon");
        sss_batchstartmon_args.put("EXIST_FILENAMES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("file://mnt/apps/tbOSSS/appServer/logs/${TODAY2}/TBLDUMP0_{4}.log", Boolean.TRUE)));
        sss_batchstartmon_args.put("TIMEOUT", "17:30:00");
        scheduleApi.createWorkflowJob(context, "job://matrix/sss_batchstartmon", "SampleMatrixScript", untilTimeoutWorkflow().getWorkflowURI(),
                "15 19 * * 1-5 *", "America/New_York", sss_batchstartmon_args, true, 5, "");
        return sss_batchstartmon_args;
    }

    public Map<String, String> cmi_batchmon_args() {
        Map<String, String> cmi_batchmon_args = new HashMap<>();
        cmi_batchmon_args.put("EMAIL_RECIPIENTS",
                "john.e.kelly@bnymellon.com steve.greig@matrixapps.com mohammad.ejaz@matrixapps.com stephen.mellert@matrixapps.com ben.schulson@matrixapps.com robert.fossella@matrixapps.com james.t.neese@matrixapps.com");
        cmi_batchmon_args.put("SLACK_WEBHOOK", WEBHOOK_TEST);
        cmi_batchmon_args.put("NOTIFY_TYPE", "SLACK");
        cmi_batchmon_args.put("CALENDAR", calendarCMIUri);
        cmi_batchmon_args.put("TRANSLATOR", translationUri);
        cmi_batchmon_args.put("JOBNAME", "cmi_batchmon");
        cmi_batchmon_args.put("EXIST_FILENAMES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("file://mnt/apps/tbOCMI/appServer/logs/${TODAY2}/BATCHDONE_{4}.log", Boolean.TRUE)));
        cmi_batchmon_args.put("TIMEOUT", "23:00:00");
        scheduleApi.createWorkflowJob(context, "job://matrix/cmi_batchmon", "SampleMatrixScript", untilTimeoutWorkflow().getWorkflowURI(), "15 19 * * 1-5 *",
                "America/New_York", cmi_batchmon_args, true, 5, "");
        return cmi_batchmon_args;
    }

    public Map<String, String> scb_batchmon_args() {
        Map<String, String> scb_batchmon_args = new HashMap<>();
        scb_batchmon_args.put("EMAIL_RECIPIENTS",
                "john.e.kelly@bnymellon.com steve.greig@matrixapps.com mohammad.ejaz@matrixapps.com stephen.mellert@matrixapps.com ben.schulson@matrixapps.com robert.fossella@matrixapps.com james.t.neese@matrixapps.com");
        scb_batchmon_args.put("SLACK_WEBHOOK", WEBHOOK_TEST);
        scb_batchmon_args.put("NOTIFY_TYPE", "SLACK");
        scb_batchmon_args.put("CALENDAR", calendarSCBUri);
        scb_batchmon_args.put("TRANSLATOR", translationUri);
        scb_batchmon_args.put("JOBNAME", "scb_batchmon");
        scb_batchmon_args.put("EXIST_FILENAMES",
                JacksonUtil.jsonFromObject(ImmutableMap.of("file://mnt/apps/tbOSCB/appServer/logs/${TODAY2}/BATCHDONE_{4}.log", Boolean.TRUE)));
        scb_batchmon_args.put("TIMEOUT", "23:00:00");
        scheduleApi.createWorkflowJob(context, "job://matrix/scb_batchmon", "SampleMatrixScript", untilTimeoutWorkflow().getWorkflowURI(), "15 19 * * 1-5 *",
                "America/New_York", scb_batchmon_args, true, 5, "");
        return scb_batchmon_args;
    }

    // DONE
    // /opt/scripts/client/common/chkcurrent.sh CMI
    // /opt/scripts/client/common/chkcurrent.sh SCB
    // /opt/scripts/client/cmi/idsialert.sh
    // /opt/scripts/client/cmi/nosalert.sh
    // /opt/scripts/client/mc/mcprices.sh
    // /opt/scripts/client/nyu/downloadtreasuries.sh
    // /opt/scripts/client/sss/secfeed.sh
    // /opt/scripts/client/cmi/bbtsprice.sh
    // /opt/scripts/client/cmi/idsimbsalert.sh
    // /opt/scripts/client/cmi/secondwatch.sh
    // /opt/scripts/client/scb/secondwatch.sh
    // /opt/scripts/client/sss/secondwatch.sh
    // /opt/scripts/client/cmi/batchstartmon.sh
    // /opt/scripts/client/scb/batchstartmon.sh
    // /opt/scripts/client/sss/batchstartmon.sh

    // TODO

    // /opt/scripts/client/cmi/batchmon.sh
    // /opt/scripts/client/scb/batchmon.sh
    // /opt/scripts/client/sss/idsimon.sh
    // /opt/scripts/client/sss/batchmon.sh
    // /opt/scripts/client/mc/mcbatchmon.sh
    // /opt/script/client/mc/mcbatchstartmon.sh
    // /opt/script/client/mc/mcsecondwatch.sh

    @Test
    public void matrix_args_test() throws InterruptedException {

        List<String> jobs = scheduleApi.getJobs();
        for (String job : jobs) {
            scheduleApi.deleteJob(job);
        }

        System.out.println(JacksonUtil.jsonFromObject(chkcurrent_CMI_args()));
        System.out.println(JacksonUtil.jsonFromObject(chkcurrent_SCB_args()));
        System.out.println(JacksonUtil.jsonFromObject(idsialert_args()));
        System.out.println(JacksonUtil.jsonFromObject(idsimbsalert_args()));
        System.out.println(JacksonUtil.jsonFromObject(nosalert_args()));
        System.out.println(JacksonUtil.jsonFromObject(mcprices_args()));
        System.out.println(JacksonUtil.jsonFromObject(downloadtreasuries_args()));
        System.out.println(JacksonUtil.jsonFromObject(secfeed_args()));
        System.out.println(JacksonUtil.jsonFromObject(bbtsprice_args()));
        System.out.println(JacksonUtil.jsonFromObject(cmi_secondwatch_args()));
        System.out.println(JacksonUtil.jsonFromObject(scb_secondwatch_args()));
        System.out.println(JacksonUtil.jsonFromObject(sss_secondwatch_args()));
        System.out.println(JacksonUtil.jsonFromObject(cmi_batchstartmon_args()));
        System.out.println(JacksonUtil.jsonFromObject(scb_batchstartmon_args()));
        System.out.println(JacksonUtil.jsonFromObject(sss_batchstartmon_args()));

        // scheduleApi.createWorkflowJob(context, "job://matrix/chkcurrent_CMI", "SampleMatrixScript", filesExistWorkflow().getWorkflowURI(), "* * * * 1-5 *",
        // "America/New_York", chkcurrent_CMI_args(), true, 5, "");
        //
        // scheduleApi.createWorkflowJob(context, "job://matrix/chkcurrent_SCB", "SampleMatrixScript", filesExistWorkflow().getWorkflowURI(), "* * * * 1-5 *",
        // "America/New_York", chkcurrent_SCB_args(), true, 5, "");
        //
        // scheduleApi.createWorkflowJob(context, "job://matrix/idsialert", "SampleMatrixScript", filesExistWorkflow().getWorkflowURI(), "* * * * 1-5 *",
        // "America/New_York", idsialert_args(), true, 5, "");
        //
        // scheduleApi.createWorkflowJob(context, "job://matrix/nosalert", "SampleMatrixScript", filesExistWorkflow().getWorkflowURI(), "* * * * 1-5 *",
        // "America/New_York", nosalert_args(), true, 5, "");
        //
        // scheduleApi.createWorkflowJob(context, "job://matrix/mcprices", "SampleMatrixScript", filesRetrieveWorkflow().getWorkflowURI(), "* * * * 1-5 *",
        // "America/New_York", mcprices_args(), true, 5, "");

        scheduleApi.createWorkflowJob(context, "job://matrix/downloadtreasuries", "SampleMatrixScript", filesRetrieveWorkflow().getWorkflowURI(),
                "* * * * 1-5 *", "America/New_York", downloadtreasuries_args(), true, 5, "");

        // scheduleApi.createWorkflowJob(context, "job://matrix/secfeed", "SampleMatrixScript", filesExistWorkflow().getWorkflowURI(), "* * * * 1-5 *",
        // "America/New_York", secfeed_args(), true, 5, "");


        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-hh:mm:ss");

        List<RaptureJobExec> execs = scheduleApi.getUpcomingJobs(context);
        // Assert.assertEquals(5, execs.size());
        for (RaptureJobExec e : execs) {
            System.out.println(e.getStoragePath() + " scheduled at " + sdf.format(new Date(e.getExecTime())));
        }
        try {
            Thread.sleep(61000);
        } catch (InterruptedException e) {
        }

        List<RaptureJobExec> execs2 = scheduleApi.getUpcomingJobs(context);

        Assert.assertEquals(execs.get(0).getJobURI(), execs2.get(0).getJobURI());
        Assert.assertNotSame(execs.get(0).getExecTime(), execs2.get(0).getExecTime());

    }

}
