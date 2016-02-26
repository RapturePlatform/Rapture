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
package rapture.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.slf4j.MDC;

import rapture.common.exception.ExceptionToString;

public class MDCTest {
    private static final Logger logger = Logger.getLogger(MDCTest.class);

    @Test
    public void TestMdc() {
        LogManager.configureLogging();

        final ExecutorService executor = Executors.newCachedThreadPool();

        logger.info("Before");
        assertNull(MDC.getCopyOfContextMap());
        final long stepStartTime = System.currentTimeMillis();
        final String stepName = "step1";
        final String workerId = "1";
        final String workOrderURI = "WOUT";
        MDCService.INSTANCE.setWorkOrderMDC(workOrderURI, workerId);
        assertEquals(3, MDC.getCopyOfContextMap().size());
        assertEquals(workerId, MDC.get(MDCService.WORKER_ID));
        assertEquals(workOrderURI, MDC.get(MDCService.WORK_ORDER_URI));

        MDCService.INSTANCE.setWorkOrderStepMDC(stepName, stepStartTime);
        assertWorkflow(stepStartTime, stepName, workerId, workOrderURI);
        assertEquals(5, MDC.getCopyOfContextMap().size());

        final String scriptName = "OUTERSCRIPT.rfx";
        MDCService.INSTANCE.setReflexMDC(scriptName);
        assertRfx(scriptName);

        assertEquals(6, MDC.getCopyOfContextMap().size());
        logger.info("Starting");
        for (int i = 0; i < 2; i++) {
            final int current = i;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    assertWorkflow(stepStartTime, stepName, workerId, workOrderURI);

                    assertRfx(scriptName);

                    logger.info("Nothing set " + current);
                    final long stepStartTimeOuter = System.currentTimeMillis();
                    final String stepNameOuter = "stepOuter" + current;
                    final String workerIdOuter = "0";
                    final String workOrderURIOuter = "workorder-in-" + current;
                    MDCService.INSTANCE.setWorkOrderMDC(workOrderURIOuter, workerIdOuter);
                    logger.info("Done setting workorder");
                    MDCService.INSTANCE.setWorkOrderStepMDC(stepNameOuter, stepStartTimeOuter);
                    logger.info("Done setting workorder AND step " + current);

                    assertWorkflow(stepStartTimeOuter, stepNameOuter, workerIdOuter, workOrderURIOuter);

                    final String scriptNameOuter = "script-outer-" + current + ".rfx";
                    MDCService.INSTANCE.setReflexMDC(scriptNameOuter);
                    assertRfx(scriptNameOuter);

                    Callable<Boolean> callable = new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            logger.info("pre INNER inner " + current);
                            String workOrderURIInner = "W-inner-" + current;
                            String workerIdInner = "0";
                            String stepNameInner = "stepInner" + current;
                            long stepStartTimeInner = System.currentTimeMillis();
                            MDCService.INSTANCE.setWorkOrderMDC(workOrderURIInner, workerIdInner);
                            MDCService.INSTANCE.setWorkOrderStepMDC(stepNameInner, stepStartTimeInner);
                            assertWorkflow(stepStartTimeInner, stepNameInner, workerIdInner, workOrderURIInner);
                            String scriptNameInner = "RFX-inner-" + current + ".rfx";
                            MDCService.INSTANCE.setReflexMDC(scriptNameInner);
                            logger.info("middle INNER inner " + current);
                            assertRfx(scriptNameInner);

                            MDCService.INSTANCE.clearReflexMDC();
                            assertRfx(scriptNameOuter);
                            MDCService.INSTANCE.clearWorkOrderMDC();
                            MDCService.INSTANCE.clearWorkOrderStepMDC(stepNameInner, stepStartTimeInner);
                            assertWorkflow(stepStartTimeOuter, stepNameOuter, workerIdOuter, workOrderURIOuter);
                            logger.info("end INNER inner " + current);
                            MDCService.INSTANCE.clearWorkOrderStepMDC(stepNameInner, stepStartTimeInner);
                            assertWorkflow(stepStartTimeOuter, stepNameOuter, workerIdOuter, workOrderURIOuter);
                            logger.info("end INNER, same again " + current);

                            MDCService.INSTANCE.clearReflexMDC();
                            assertRfx(scriptName);
                            MDCService.INSTANCE.clearWorkOrderMDC();
                            MDCService.INSTANCE.clearWorkOrderStepMDC(stepNameOuter, stepStartTimeOuter);
                            assertWorkflow(stepStartTime, stepName, workerId, workOrderURI);
                            logger.info("end2 INNER inner " + current);

                            MDCService.INSTANCE.clearReflexMDC();
                            MDCService.INSTANCE.clearWorkOrderMDC();
                            MDCService.INSTANCE.clearWorkOrderStepMDC(stepName, stepStartTime);
                            logger.info("end3 INNER inner " + current);
                            assertNull(MDC.getCopyOfContextMap());

                            MDCService.INSTANCE.clearReflexMDC();
                            MDCService.INSTANCE.clearWorkOrderMDC();
                            MDCService.INSTANCE.clearWorkOrderStepMDC(stepName, stepStartTime);
                            logger.info("end4 INNER inner " + current);
                            assertNull(MDC.getCopyOfContextMap());

                            return true;
                        }
                    };
                    Future<Boolean> future = executor.submit(callable);
                    try {
                        future.get();
                    } catch (Exception e) {
                        fail("Failed during callable " + current + "\n" + ExceptionToString.format(e));
                    }

                    logger.info("all set " + current);
                    assertWorkflow(stepStartTimeOuter, stepNameOuter, workerIdOuter, workOrderURIOuter);
                    assertRfx(scriptNameOuter);
                    assertEquals(6, MDC.getCopyOfContextMap().size());

                    MDCService.INSTANCE.clearReflexMDC();
                    logger.info("unset reflex " + current);
                    assertEquals(6, MDC.getCopyOfContextMap().size());
                    assertWorkflow(stepStartTimeOuter, stepNameOuter, workerIdOuter, workOrderURIOuter);
                    assertRfx(scriptName);

                    MDCService.INSTANCE.clearWorkOrderStepMDC(stepNameOuter, stepStartTimeOuter);
                    logger.info("Unset workorder step");
                    assertWorkflow(stepStartTime, stepName, workerIdOuter, workOrderURIOuter);

                    MDCService.INSTANCE.clearWorkOrderMDC();
                    assertWorkflow(stepStartTime, stepName, workerId, workOrderURI);
                    logger.info("none " + current);

                    assertRfx(scriptName);
                }

            });
            logger.info("After " + i);
            assertWorkflow(stepStartTime, stepName, workerId, workOrderURI);
            assertRfx(scriptName);
        }
        try {
            TimeUnit.SECONDS.sleep(3);
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Interrupted " + ExceptionToString.format(e));
        }
        logger.info("Done");
    }

    private void assertRfx(String scriptName) {
        assertEquals(scriptName, MDC.get(MDCService.RFX_SCRIPT));
    }

    private void assertWorkflow(long stepStartTime, String stepName, String workerId, String workOrderURI) {
        assertEquals(workerId, MDC.get(MDCService.WORKER_ID));
        assertEquals(workOrderURI, MDC.get(MDCService.WORK_ORDER_URI));
        assertEquals(stepName, MDC.get(MDCService.STEP_NAME));
        assertEquals(stepStartTime, Long.parseLong(MDC.get(MDCService.STEP_START_TIME)));
    }
}
