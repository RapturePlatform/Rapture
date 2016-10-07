package rapture.notification.step;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.CreateResponse;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.dp.Step;
import rapture.common.dp.StepRecord;
import rapture.common.dp.Steps;
import rapture.common.dp.WorkOrderDebug;
import rapture.common.dp.Workflow;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.RaptureExchange;
import rapture.common.model.RaptureExchangeQueue;
import rapture.common.model.RaptureExchangeType;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.script.KernelScript;
import rapture.mail.Mailer;
import rapture.mail.SMTPConfig;

public class NotificationStepTest {

    static String saveInitSysConfig;
    static String saveRaptureRepo;
    private static final String auth = "test" + System.currentTimeMillis();
    private static final String REPO_USING_MEMORY = "REP {} USING MEMORY {prefix=\"/tmp/" + auth + "\"}";
    static CallingContext context;
    static final String templateName = "TESTING";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        context = ContextFactory.getKernelUser();
        RaptureConfig.setLoadYaml(false);
        RaptureConfig config = ConfigLoader.getConf();
        saveRaptureRepo = config.RaptureRepo;
        saveInitSysConfig = config.InitSysConfig;

        config.RaptureRepo = REPO_USING_MEMORY;
        config.InitSysConfig = "NREP {} USING MEMORY { prefix=\"/tmp/" + auth + "/sys.config\"}";

        System.setProperty("LOGSTASH-ISENABLED", "false");
        Kernel.initBootstrap();

        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {prefix=\"/tmp/" + auth + "\"}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");
        Kernel.getIdGen().createIdGen(context, "idgen://sys/dp/workOrder", "IDGEN {} USING MEMORY {}");
        Kernel.getIdGen().createIdGen(context, "idgen://sys/activity/id", "IDGEN {} USING MEMORY {}");

        try {
            Kernel.INSTANCE.restart();
            Kernel.initBootstrap(null, null, true);

            Kernel.getPipeline().getTrusted().registerServerCategory(context, "alpha", "Primary servers");
            Kernel.getPipeline().getTrusted().registerServerCategory(context, "beta", "Secondary servers");

            Kernel.getPipeline().registerExchangeDomain(context, "//main", "EXCHANGE {} USING MEMORY {}");

            RaptureExchange exchange = new RaptureExchange();
            exchange.setName("kernel");
            exchange.setName("kernel");
            exchange.setExchangeType(RaptureExchangeType.FANOUT);
            exchange.setDomain("main");

            List<RaptureExchangeQueue> queues = new ArrayList<>();
            RaptureExchangeQueue queue = new RaptureExchangeQueue();
            queue.setName("default");
            queue.setRouteBindings(new ArrayList<String>());
            queues.add(queue);

            exchange.setQueueBindings(queues);

            Kernel.getPipeline().getTrusted().registerPipelineExchange(context, "kernel", exchange);
            Kernel.getPipeline().getTrusted().bindPipeline(context, "alpha", "kernel", "default");

            // Now that the binding is setup, register our server as being part of "alpha"

            Kernel.setCategoryMembership("alpha");

            KernelScript ks = new KernelScript();
            ks.setCallingContext(context);

        } catch (RaptureException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        // create smtp config
        String area = "CONFIG";
        CallingContext anon = ContextFactory.getAnonymousUser();
        SMTPConfig smtpConfig = new SMTPConfig().setFrom("bmsdrama2016@gmail.com").setUsername("bmsdrama2016@gmail.com").setPassword("gM3xpat/")
                .setHost("smtp.gmail.com").setPort(587);
        Kernel.getSys().writeSystemConfig(context, area, Mailer.SMTP_CONFIG_URL, JacksonUtil.jsonFromObject(smtpConfig));

        // create dummy email template
        String template = "{\"emailTo\":\"bmsdrama2016@gmail.com\",\"subject\":\"Ignore this message\",\"msgBody\":\"This email is generated from NotificationStepTest in WorkflowCommonSteps\"}";
        String url = Mailer.EMAIL_TEMPLATE_DIR + templateName;
        Kernel.getSys().writeSystemConfig(context, area, url, template);

        // update user email
        Kernel.getAdmin().updateUserEmail(context, "rapture", "support@incapturetechnologies.com");
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testNotificationStep() {

        String workflowUri = "workflow://foo/bar/baz";
        Workflow w = new Workflow();
        w.setStartStep("step1");
        List<Step> steps = new LinkedList<>();
        Step step = new Step();
        step.setExecutable("dp_java_invocable://notification.steps.NotificationStep");
        step.setName("step1");
        step.setDescription("description");
        steps.add(step);
        Map<String, String> viewMap = new HashMap<>();
        viewMap.put("NOTIFY_TYPE", "#" + "EMAIL");
        viewMap.put("MESSAGE_TEMPLATE", "#" + templateName);
        viewMap.put("RECIPIENT", "#" + "dave.tong@incapturetechnologies.com");
        w.setSteps(steps);
        w.setView(viewMap);
        w.setWorkflowURI(workflowUri);
        Kernel.getDecision().putWorkflow(context, w);

        CreateResponse response = Kernel.getDecision().createWorkOrderP(context, workflowUri, null, null);
        assertTrue(response.getIsCreated());
        WorkOrderDebug debug;
        WorkOrderExecutionState state = WorkOrderExecutionState.NEW;
        long timeout = System.currentTimeMillis() + 60000;
        do {
            debug = Kernel.getDecision().getWorkOrderDebug(context, response.getUri());
            state = debug.getOrder().getStatus();
        } while (((state == WorkOrderExecutionState.NEW) || (state == WorkOrderExecutionState.ACTIVE)) && (System.currentTimeMillis() < timeout));

        StepRecord sr = debug.getWorkerDebugs().get(0).getStepRecordDebugs().get(0).getStepRecord();
        assertEquals(sr.toString(), Steps.NEXT.toString(), sr.getRetVal());
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());

        viewMap.put("RECIPIENT", "#" + "https://hooks.slack.com/services/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");
        viewMap.put("NOTIFY_TYPE", "#" + "SLACK");
        w.setView(viewMap);
        Kernel.getDecision().putWorkflow(context, w);

        response = Kernel.getDecision().createWorkOrderP(context, workflowUri, null, null);
        assertTrue(response.getIsCreated());
        state = WorkOrderExecutionState.NEW;
        timeout = System.currentTimeMillis() + 60000;
        do {
            debug = Kernel.getDecision().getWorkOrderDebug(context, response.getUri());
            state = debug.getOrder().getStatus();
        } while (((state == WorkOrderExecutionState.NEW) || (state == WorkOrderExecutionState.ACTIVE)) && (System.currentTimeMillis() < timeout));

        sr = debug.getWorkerDebugs().get(0).getStepRecordDebugs().get(0).getStepRecord();
        // TODO
        // assertEquals(Steps.NEXT.toString(), sr.getRetVal());
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());

    }

}
