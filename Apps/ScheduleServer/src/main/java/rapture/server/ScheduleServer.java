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
package rapture.server;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.config.ConfigLoader;
import rapture.apiutil.StatusHelper;
import rapture.app.RaptureAppService;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.schedule.ScheduleManager;
import rapture.util.IDGenerator;

/**
 * This is the entry point for the RaptureServer
 *
 * @author alan
 */
public final class ScheduleServer {

    private ScheduleServer() {
    }

    private static Logger log = Logger.getLogger(ScheduleServer.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            RaptureAppService.setupApp("ScheduleServer");

            try {
                Map<String, String> templates = new HashMap<String, String>();
                templates.put("STD", ConfigLoader.getConf().StandardTemplate);
                Kernel.initBootstrap(templates, ScheduleServer.class, false);
                Kernel.getKernel().setAppStyle("schedule");
                Kernel.getKernel().setAppId(IDGenerator.getUUID());
            } catch (RaptureException e1) {
                log.error("Failed to start Rapture with " + ExceptionToString.format(e1));
                System.exit(-1);
            }

        } catch (RaptureException e) {
            log.error("Error when running Rapture - " + ExceptionToString.format(e));
        }

        Map<String, Object> cap = new HashMap<String, Object>();
        StatusHelper.setStatusAndCapability(ContextFactory.getKernelUser(), "RUNNING", cap, Kernel.getRunner());
        StatusHelper.startStatusUpdating(ContextFactory.getKernelUser(), Kernel.getRunner());

        // We will call the schedule manager periodically (after locking)
        while (true) {
            try {
                Thread.sleep(1000);
                try {
                    ScheduleManager.manageJobExecStatus();
                } catch (Exception e) {
                    log.error(String.format("Got exception %s when running a task, the show goes on. stack: %s", e.getMessage(), ExceptionToString.format(e)));
                }
            } catch (InterruptedException e) {
                log.error("Error while sleeping on main thread");
                System.exit(-1);
            }
        }
    }
}
