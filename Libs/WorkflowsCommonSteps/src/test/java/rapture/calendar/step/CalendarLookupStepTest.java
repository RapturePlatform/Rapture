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
package rapture.calendar.step;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import rapture.common.CallingContext;
import rapture.common.CreateResponse;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.api.DocApi;
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

public class CalendarLookupStepTest {

    static String saveInitSysConfig;
    static String saveRaptureRepo;
    private static final String auth = "test" + System.currentTimeMillis();
    private static final String REPO_USING_MEMORY = "REP {} USING MEMORY {prefix=\"/tmp/" + auth + "\"}";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CallingContext context = ContextFactory.getKernelUser();
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

            List<RaptureExchangeQueue> queues = new ArrayList<RaptureExchangeQueue>();
            RaptureExchangeQueue queue = new RaptureExchangeQueue();
            queue.setName("default");
            queue.setRouteBindings(new ArrayList<String>());
            queues.add(queue);

            exchange.setQueueBindings(queues);

            Kernel.getPipeline().getTrusted().registerPipelineExchange(context, "kernel", exchange);
            Kernel.getPipeline().getTrusted().bindPipeline(context, "alpha", "kernel", "default");

            // Now that the binding is setup, register our server as being part
            // of
            // "alpha"

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
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testCalendarLookupStep() {

        CallingContext context = ContextFactory.getKernelUser();
        DocApi dapi = Kernel.getDoc();

        Map<String, Object> translationMap = new LinkedHashMap<>();
        translationMap.put("01Jan", "New Year's Day");
        translationMap.put("18Jan2016", "Martin Luther King Day");
        translationMap.put("02Feb", "Groundhog Day");
        translationMap.put("09Feb2016", ImmutableList.of("Shrove Tuesday", "Mardi Gras"));
        String translationUri = "document://test/translation";
        Kernel.getDoc().putDoc(context, translationUri, JacksonUtil.jsonFromObject(translationMap));

        Map<String, Object> calendarMap = new LinkedHashMap<>();
        calendarMap.put("New Year's Day", "$CANCEL");
        calendarMap.put("25Jan2016", "$CANCEL");
        calendarMap.put("Mardi Gras", "$CANCEL");
        calendarMap.put("Saturday", "$WEEKEND");
        calendarMap.put("Sunday", "$WEEKEND");
        String calendarUri = "document://test/calendar";
        Kernel.getDoc().putDoc(context, calendarUri, JacksonUtil.jsonFromObject(calendarMap));
        
        String workflowUri = "workflow://foo/bar/baz";

        Workflow w = new Workflow();
        w.setStartStep("step1");
        List<Step> steps = new LinkedList<>();
        Step step = new Step();
        step.setExecutable("dp_java_invocable://calendar.steps.CalendarLookupStep");
        step.setName("step1");
        step.setDescription("description");
        steps.add(step);
        Map<String, String> viewMap = new HashMap<>();
        viewMap.put("CALENDAR", "#" + calendarUri);
        viewMap.put("TRANSLATOR", "#" + translationUri);
        w.setSteps(steps);
        w.setView(viewMap);
        w.setWorkflowURI(workflowUri);
        Kernel.getDecision().putWorkflow(context, w);

        // New Year's Day is a holiday every year
        viewMap.put("DATE", "#" + "2016-01-01");
        w.setView(viewMap);
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
        assertEquals("$CANCEL", sr.getRetVal());
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());

        // Groundhog Day is not a holiday so expect NEXT
        viewMap.put("DATE", "#" + "2016-02-02");
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
        assertEquals(Steps.NEXT.toString(), sr.getRetVal());
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());


        // Let's make Mardi Gras a holiday - expect CANCEL
        viewMap.put("DATE", "#" + "2016-02-09");
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
        assertEquals("$CANCEL", sr.getRetVal());
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());

        // Saturday is not a work day - expect WEEKEND
        viewMap.put("DATE", "#" + "2016-09-03");
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
        assertEquals("$WEEKEND", sr.getRetVal());
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
    }

}
