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
package rapture.kernel.stat;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.RaptureConstants;
import rapture.dsl.sgen.StatStoreFactory;
import rapture.kernel.Kernel;
import rapture.repo.Repository;
import rapture.stat.BaseStat;
import rapture.stat.IRaptureStatApi;

import rapture.config.ConfigLoader;

/**
 * The stat helper encapsulates interaction with stat
 *
 * @author amkimian
 *
 */
public class StatHelper {
    private static final Logger log = Logger.getLogger(StatHelper.class);
    private IRaptureStatApi stat;

    private Thread updateThread;

    public StatHelper(Repository repository) {
        String statConfig = (repository == null) ? null : repository.getDocument("stat/config");
        if (statConfig == null) {
            log.info("No stat config found, using default");
            statConfig = ConfigLoader.getConf().DefaultStatus;
        }
        log.debug("Stat config is " + statConfig);
        stat = StatStoreFactory.getStatImpl(statConfig);
        // Setup standard fields
        setupStandardFields();
        // createStandardSchedule();

        updateThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    while (true) {
                        try {
                            Thread.sleep(30000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        registerMe();
                    }
                } catch (Exception e) {
                    log.error("Error when updating statistics");
                    e.printStackTrace();
                }
            }

        });

        updateThread.setDaemon(true);
        updateThread.setName("StatsUpdate Thread");
        updateThread.start();
    }

    /*
     * private void createStandardSchedule() { JobBuilder builder =
     * JobBuilder.newJob(RefreshStat.class).withIdentity( "refreshSchedule",
     * "kernel");
     * 
     * JobDetail job = builder.build();
     * 
     * CronTrigger trigger = TriggerBuilder.newTrigger()
     * .withIdentity("refreshSchedule", "kernel")
     * .withSchedule(cronSchedule("0 0/1 * * * ?")).withPriority(10) .build();
     * 
     * try { Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
     * scheduler.scheduleJob(job, trigger); } catch (SchedulerException e) {
     * throw new RaptureException(e); } }
     */

    public Map<String, BaseStat> getCurrentStats() {
        runPeriodic();
        return stat.getCurrentStat();
    }

    public List<? extends BaseStat> getHistory(String key) {
        return stat.getHistory(key);
    }

    public List<String> getKeys() {
        return stat.getStatKeys();
    }

    public void registerPipelineSubmission() {
        stat.recordCounter(StatConstants.PIPELINEWAITCOUNT, 1);
    }

    public void registerPipelineStart() {
        stat.recordCounter(StatConstants.PIPELINEWAITCOUNT, -1);
        stat.recordCounter(StatConstants.PIPELINERUNNINGCOUNT, 1);
    }

    public void registerPipelineExecution() {
        stat.recordCounter(StatConstants.PIPELINERUNNINGCOUNT, -1);
        stat.recordCounter(StatConstants.PIPELINETASKCOUNT, 1);
        stat.recordValue(StatConstants.PIPELINERATE, 1);
    }

    public void registerApiCall() {
        stat.recordValue(StatConstants.CALLCOUNT, 1);
        stat.recordCounter(StatConstants.TOTALCALLS, 1);
    }

    public void registerApiThroughput(long size) {
        stat.recordValue(StatConstants.THROUGHPUT, size);
    }

    public void registerEventRun() {
        stat.recordValue(StatConstants.EVENTRUN, 1);
        stat.recordCounter(StatConstants.EVENTSFIRED, 1);
    }

    public void registerMe() {
        stat.recordPresence(StatConstants.APPPREFIX + Kernel.getKernel().getAppStyle(), Kernel.getKernel().getAppId());
    }

    public void registerRunScript() {
        stat.recordCounter(StatConstants.SCRIPTSRUN, 1);
        stat.recordValue(StatConstants.SCRIPTRATE, 1);
    }

    public void registerTriggerRun() {
        stat.recordValue(StatConstants.TRIGGERRUN, 1);
    }

    public void registerUser(String userName) {
        stat.recordPresence(StatConstants.USERS, userName);
    }

    public void runPeriodic() {
        for (String k : stat.getStatKeys()) {
            stat.computeRecord(k);
        }
    }

    private void setupStandardFields() {
        stat.defineStat(StatConstants.USERS, "PRESENCE WITH { seconds = \"60\" }");
        stat.defineStat(StatConstants.WEBAPPS, "PRESENCE WITH { seconds = \"60\"}");
        stat.defineStat(StatConstants.WORKERS, "PRESENCE WITH { seconds = \"60\"}");
        stat.defineStat(StatConstants.CALLCOUNT, "VALUE WITH { seconds = \"60\", operation=\"SUM\" }");
        stat.defineStat(StatConstants.THROUGHPUT, "VALUE WITH { seconds = \"60\", operation=\"SUM\" }");
        stat.defineStat(StatConstants.EVENTRUN, "VALUE WITH { seconds = \"60\", operation=\"SUM\" }");
        stat.defineStat(StatConstants.TRIGGERRUN, "VALUE WITH { seconds = \"60\", operation=\"SUM\"}");
        stat.defineStat(StatConstants.TOTALCALLS, "COUNTER WITH {}");
        stat.defineStat(StatConstants.EVENTSFIRED, "COUNTER WITH {}");
        stat.defineStat(StatConstants.SCRIPTRATE, "VALUE WITH { seconds = \"60\", operation=\"SUM\" }");
        stat.defineStat(StatConstants.SCRIPTSRUN, "COUNTER WITH {}");
        stat.defineStat(StatConstants.PIPELINETASKCOUNT, "COUNTER WITH {}");
        stat.defineStat(StatConstants.PIPELINEWAITCOUNT, "COUNTER WITH {}");
        stat.defineStat(StatConstants.PIPELINERUNNINGCOUNT, "COUNTER WITH {}");
        stat.defineStat(StatConstants.PIPELINERATE, "VALUE WITH { seconds = \"60\", operation=\"SUM\" }");
    }

}
