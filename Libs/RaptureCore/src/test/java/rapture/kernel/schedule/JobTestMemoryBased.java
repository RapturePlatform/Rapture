/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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
package rapture.kernel.schedule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.JobExecStatus;
import rapture.common.JobLink;
import rapture.common.JobLinkStatus;
import rapture.common.RaptureJob;
import rapture.common.RaptureJobExec;
import rapture.common.RaptureJobExecStorage;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.UpcomingJobExec;
import rapture.common.UpcomingJobExecStorage;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JsonContent;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.ScheduleApiImplWrapper;
import rapture.repo.RepoVisitor;

public class JobTestMemoryBased {

    private static final int JOB_LINK_STATUS = 2;

    private static final Logger log = Logger.getLogger(JobTestMemoryBased.class);
    static String saveRaptureRepo;
    static String saveInitSysConfig;

    @After
    public void after() throws IOException {
        CallingContext context = ContextFactory.getKernelUser();
        ScheduleApiImplWrapper schedule = Kernel.getSchedule();
        List<String> jobs = schedule.getJobs(context);

        for (String jobURI : jobs) {
            log.trace("deleting job " + jobURI);
            schedule.deleteJob(context, jobURI);
        }

        ConfigLoader.getConf().InitSysConfig = saveInitSysConfig;
        ConfigLoader.getConf().RaptureRepo = saveRaptureRepo;
    }

    @BeforeClass
    public static void setup() {

        RaptureConfig.setLoadYaml(false);
        RaptureConfig config = ConfigLoader.getConf();

        saveRaptureRepo = config.RaptureRepo;
        saveInitSysConfig = config.InitSysConfig;

        config.RaptureRepo = "REP {} USING MEMORY { }";
        config.InitSysConfig = "NREP {} USING MEMORY { }";

        System.setProperty("LOGSTASH-ISENABLED", "false");
        Kernel.INSTANCE.restart();
        Kernel.initBootstrap();
        CallingContext context = ContextFactory.getKernelUser();

        ScheduleApiImplWrapper schedule = Kernel.getSchedule();
        if (schedule == null) throw new RuntimeException("Schedule is null");
        List<String> jobs = schedule.getJobs(context);
        if (jobs != null) {
            for (String jobURI : jobs) {
                log.trace("deleting job " + jobURI);
                schedule.deleteJob(context, jobURI);
            }
        }

    }

    @Test
    public void testGetAll() {
        CallingContext context = ContextFactory.getKernelUser();

        String job1 = "//testGetAll/A";
        String job2 = "//testGetAll/B";
        createJob(job1, job2);
        List<String> jobs = Kernel.getSchedule().getJobs(context);
        assertEquals(2, jobs.size());
        String job1Uri = new RaptureURI(job1, Scheme.JOB).toString();
        String job2Uri = new RaptureURI(job2, Scheme.JOB).toString();
        assertTrue("Job 1 found", job1Uri.equals(jobs.get(0)) || job1Uri.equals(jobs.get(1)));
        assertTrue("Job 2 found", job2Uri.equals(jobs.get(0)) || job2Uri.equals(jobs.get(1)));
    }

    @Test
    public void testGetAndDelete() {
        CallingContext context = ContextFactory.getKernelUser();

        String job1 = "//testGetAnDelete/A";
        String job2 = "//testGetAnDelete/B";
        RaptureURI job1URI = new RaptureURI(job1, Scheme.JOB);
        RaptureURI job2URI = new RaptureURI(job2, Scheme.JOB);

        createJob(job1, job2);
        RaptureJob ret1 = Kernel.getSchedule().retrieveJob(context, job1);
        RaptureJob ret2 = Kernel.getSchedule().retrieveJob(context, job2);
        assertEquals("job:" + job1, ret1.getJobURI());
        assertEquals("job:" + job2, ret2.getJobURI());

        List<JobLink> linksTo2 = Kernel.getSchedule().getTrusted().getLinksTo(context, job2);
        assertEquals(1, linksTo2.size());
        assertEquals(job1URI.toString(), linksTo2.get(0).getFrom());

        List<JobLinkStatus> statusList = Kernel.getSchedule().getTrusted().getJobLinkStatus(context, job1);
        assertEquals(1, statusList.size());
        JobLinkStatus status = statusList.get(0);
        assertEquals(job2URI.toString(), status.getTo());
        assertEquals(job1URI.toString(), status.getFrom());

        Kernel.getSchedule().deleteJob(context, job1);
        ret1 = Kernel.getSchedule().retrieveJob(context, job1);
        ret2 = Kernel.getSchedule().retrieveJob(context, job2);
        assertNull(ret1);
        assertEquals("job:" + job2, ret2.getJobURI());
        linksTo2 = Kernel.getSchedule().getTrusted().getLinksTo(context, job2);
        assertEquals(0, linksTo2.size());

        statusList = Kernel.getSchedule().getTrusted().getJobLinkStatus(context, job1);
        assertEquals(0, statusList.size());
    }

    @Test
    public void testResetLink() {
        CallingContext context = ContextFactory.getKernelUser();

        String job1 = "//testResetLink/1";
        String job2 = "//testResetLink/2";
        String job3 = "//testResetLink/3";
        String job4 = "//testResetLink/4";

        createJob(job1, job2);
        createJob(job3, job4);
        Kernel.getSchedule().getTrusted().setJobLink(context, job2, job3);
        Kernel.getSchedule().getTrusted().setJobLinkStatus(context, job2, job3, JOB_LINK_STATUS);
        Kernel.getSchedule().getTrusted().setJobLink(context, job2, job4);
        Kernel.getSchedule().getTrusted().setJobLinkStatus(context, job2, job4, JOB_LINK_STATUS);
        Kernel.getSchedule().getTrusted().setJobLink(context, job3, job4);
        Kernel.getSchedule().getTrusted().setJobLinkStatus(context, job3, job4, JOB_LINK_STATUS);

        List<JobLinkStatus> statusList = Kernel.getSchedule().getTrusted().getJobLinkStatus(context, job1);
        assertEquals(JOB_LINK_STATUS, statusList.get(0).getLevel().intValue());
        Kernel.getSchedule().getTrusted().resetJobLink(context, job1);
        statusList = Kernel.getSchedule().getTrusted().getJobLinkStatus(context, job1);
        checkStatusList(statusList, 4);
        statusList = Kernel.getSchedule().getTrusted().getJobLinkStatus(context, job2);
        checkStatusList(statusList, 3);
        statusList = Kernel.getSchedule().getTrusted().getJobLinkStatus(context, job3);
        checkStatusList(statusList, 1);
        statusList = Kernel.getSchedule().getTrusted().getJobLinkStatus(context, job4);
        checkStatusList(statusList, 0);

    }
    
    @Test(expected=RaptureException.class)
    public void testNonExistent() {
        CallingContext context = ContextFactory.getKernelUser();
        String job1 = "//DOESNOTEXIST";
        Kernel.getSchedule().getTrusted().deleteJob(context, job1);
    }

    private void checkStatusList(List<JobLinkStatus> statusList, int expectedSize) {
        assertEquals(expectedSize, statusList.size());
        for (JobLinkStatus status : statusList) {
            assertEquals("status from " + status.getFrom() + " to " + status.getTo(), 0, status.getLevel().intValue());
        }
    }

    private void createJob(String job1, String job2) {
        CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> jobParams = Collections.emptyMap();
        String timeZone = "America/Los_Angeles";
        RaptureJob j1 = Kernel.getSchedule().createJob(context, job1, job1 + " desc", "document://foo/bar", "* * * * * *", timeZone, jobParams, false);
        assertNotNull(j1);
        RaptureJob j2 = Kernel.getSchedule().createJob(context, job2, job2 + " desc", "document://foo2/bar2", "* * * * * *", timeZone, jobParams, false);
        assertNotNull(j2);
        Boolean b1 = Kernel.getSchedule().getTrusted().setJobLink(context, job1, job2);
        assertTrue(b1);
        JobLinkStatus js = Kernel.getSchedule().getTrusted().setJobLinkStatus(context, job1, job2, JOB_LINK_STATUS);
        assertNotNull(js);
    }

    @Test
    public void testRetrieveJobs() {
        CallingContext context = ContextFactory.getKernelUser();
        String job1 = "//testRetrieve/job1";
        String job2 = "//testRetrieve/job2";
        RaptureURI job1URI = new RaptureURI(job1, Scheme.JOB);
        RaptureURI job2URI = new RaptureURI(job2, Scheme.JOB);
        createJob(job1, job2);
        List<RaptureJob> jobs = Kernel.getSchedule().retrieveJobs(context, "testRetrieve");
        assertEquals(2, jobs.size());
        for (RaptureJob j : jobs) {
            assertNotNull(j);
            if (!j.getAddressURI().equals(job1URI) && !j.getAddressURI().equals(job2URI)) {
                fail("Returned list did not contain correct elements");
            }
        }

        jobs = Kernel.getSchedule().retrieveJobs(context, "testRetrieve/job1");
        assertEquals(1, jobs.size());
        assertEquals(job1URI.toString(), jobs.get(0).getAddressURI().toString());
    }

    @Test
    public void testBulkGetJobExecs() {
        CallingContext context = ContextFactory.getKernelUser();
        Map<String, String> jobParams = Collections.emptyMap();
        List<String> jobUris = new ArrayList<String>();
        int numJobs = 10;
        for (int i = 0; i < numJobs; i++) {
            String jobUri = "//testBulkGetJobExecs/a" + i;
            Kernel.getSchedule().createJob(context, jobUri, jobUri + " desc", "document://foo/bar" + i, "24 * * * * *", "America/Los_Angeles", jobParams, true);
            jobUris.add(jobUri);

        }
        List<RaptureJob> jobs = Kernel.getSchedule().retrieveJobs(context, "");
        assertEquals(numJobs, jobs.size());
        for (RaptureJob j : jobs) {
            assertNotNull(j);
        }

        List<RaptureJobExec> execs = Kernel.getSchedule().batchGetJobExecs(context, jobUris, 0, 2, false);
        assertEquals(numJobs, execs.size());
        execs = Kernel.getSchedule().batchGetJobExecs(context, jobUris, 0, 1, false);
        assertEquals(numJobs, execs.size());
    }

    @Test
    public void testExec() throws InterruptedException {
        String job1 = "//testExec/job1";
        String job2 = "//testExec/job2";
        RaptureURI job1URI = new RaptureURI(job1, Scheme.JOB);
        RaptureURI job2URI = new RaptureURI(job2, Scheme.JOB);

        createJob(job1URI.toString(), job2URI.toString());
        CallingContext context = ContextFactory.getKernelUser();
        Kernel.getSchedule().getTrusted().removeJobLink(context, job1, job2);

        List<RaptureJobExec> upcoming = Kernel.getSchedule().getUpcomingJobs(context);
        assertEquals(0, upcoming.size());

        List<RaptureJobExec> execs = Kernel.getSchedule().getJobExecs(context, job1, 0, 10, false);
        assertEquals(0, execs.size());

        Thread.sleep(1);
        Kernel.getSchedule().runJobNow(context, job1, null);
        execs = Kernel.getSchedule().getJobExecs(context, job1, 0, 10, false);
        assertEquals(1, execs.size());
        RaptureJobExec firstExec = execs.get(0);
        log.trace(firstExec.toString());

        RaptureJobExec exec = Kernel.getSchedule().getNextExec(context, job1);
        assertEquals(execs.get(0).getExecTime(), exec.getExecTime());
        assertEquals(JobExecStatus.WAITING, exec.getStatus());

        upcoming = Kernel.getSchedule().getUpcomingJobs(context);
        assertEquals(1, upcoming.size());
        assertEquals(execs.get(0).getExecTime(), upcoming.get(0).getExecTime());

        Thread.sleep(1);
        Kernel.getSchedule().runJobNow(context, job2, null);
        upcoming = Kernel.getSchedule().getUpcomingJobs(context);
        assertEquals(2, upcoming.size());

        Kernel.getSchedule().resetJob(context, job2);
        upcoming = Kernel.getSchedule().getUpcomingJobs(context);
        assertEquals(1, upcoming.size());

        Kernel.getSchedule().resetJob(context, job1);
        upcoming = Kernel.getSchedule().getUpcomingJobs(context);
        assertEquals(0, upcoming.size());

        exec = Kernel.getSchedule().getNextExec(context, job1);
        assertNull(exec);

        exec = Kernel.getSchedule().retrieveJobExec(context, job1, firstExec.getExecTime());
        assertEquals(exec.debug(), JobExecStatus.FINISHED, exec.getStatus());
    }

    @Test
    public void testURI() {
        String notValid = "Not/a/valid/URI";
        String valid = "job://" + "歡天喜地" + "نشوة" + "ہرنویش" + "Восторг" + "1234567890" + "/" + notValid;
        String fullURI = "job://abc/def/ghi@5/$mno/pqr#stu";

        RaptureURI uri = new RaptureURI(fullURI);
        assertEquals("Version", "5", uri.getVersion());
        assertEquals("Attribute", "mno/pqr", uri.getAttribute());
        assertEquals("Element", "stu", uri.getElement());
        assertEquals(fullURI, uri.toString());

        RaptureJob job = new RaptureJob();

        job.setJobURI(notValid);
        String storagePath = job.getStoragePath();
        assertEquals("job://" + notValid, storagePath);
        RaptureURI addressURI = job.getAddressURI();
        assertEquals("job://" + notValid, addressURI.toString());

        // RaptureURI storageLocation = job.getStorageLocation();
        // assertEquals(valid, storageLocation);

        job.setJobURI(valid);
        storagePath = job.getStoragePath();
        assertEquals(valid, storagePath);
        addressURI = job.getAddressURI();
        assertEquals(valid, addressURI.toString());
    }

    @Test
    public void testTime() {
        CallingContext context = ContextFactory.getKernelUser();
        String timeZone = "America/New_York";
        Map<String, String> jobParams = Collections.emptyMap();
        String jobURI = "job://testTime";
        Kernel.getSchedule().createJob(context, jobURI, jobURI + " desc", "document://foo/bar", "5 10 * * * *", timeZone, jobParams, true);
        // RaptureJob job = RaptureJobStorage.readByFields(jobURI);
        UpcomingJobExec upcomingExec = UpcomingJobExecStorage.readByFields(jobURI);
        DateTime now = new DateTime();
        DateTime expectedDate = new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), 10, 5, 0, DateTimeZone.forID(timeZone));
        if (now.isAfter(expectedDate)) {
            // if we're past the scheduled time for today, then the next run
            // will be tomorrow
            expectedDate = expectedDate.plusDays(1);
        }
        assertEquals(expectedDate.getMillis(), upcomingExec.getExecTime().longValue());
    }

    @Test
    public void testUpcomingAndTTL() throws InterruptedException {
        // test upcoming job can still be retrieved, even if the corresponding RaptureJobExec object has been deleted due to ttl

        CallingContext context = ContextFactory.getKernelUser();

        String jobURI = new RaptureURI("//testUpcomingAndTTL/A", Scheme.JOB).toString();
        Map<String, String> jobParams = Collections.emptyMap();
        String timeZone = DateTimeZone.getDefault().getID();
        DateTime nextScheduledDt = new DateTime().plusMinutes(5);
        nextScheduledDt = nextScheduledDt.minusSeconds(nextScheduledDt.getSecondOfMinute()).minusMillis(nextScheduledDt.getMillisOfSecond());
        int minuteOfHour = nextScheduledDt.getMinuteOfHour();
        Kernel.getSchedule()
                .createJob(context, jobURI, jobURI + " desc", "document://foo/bar", minuteOfHour + " * * * * *", timeZone, jobParams, true);

        assertUpcomingAndTtlEq(context, jobURI, 1, nextScheduledDt);

        long before = System.currentTimeMillis();
        Thread.sleep(1);
        Kernel.getSchedule().runJobNow(context, jobURI, null);
        Thread.sleep(1);
        assertUpcomingAndTtlRange(context, jobURI, 2, before, System.currentTimeMillis());

        before = System.currentTimeMillis();
        Thread.sleep(1);
        Kernel.getSchedule().runJobNow(context, jobURI, null);
        Thread.sleep(1);
        assertUpcomingAndTtlRange(context, jobURI, 3, before, System.currentTimeMillis());

        Kernel.getSchedule().resetJob(context, jobURI); // should not increase count, just resets upcoming date
        assertUpcomingAndTtlEq(context, jobURI, 3, nextScheduledDt);

        RaptureJobExecStorage.visitAll(new RepoVisitor() {
            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                RaptureJobExec exec = RaptureJobExecStorage.readFromJson(content);
                RaptureJobExecStorage.deleteByFields(exec.getJobURI(), exec.getExecTime(), "test", "test");
                return true;
            }
        });

        assertUpcomingAndTtlEq(context, jobURI, 0, nextScheduledDt);

        Kernel.getSchedule().resetJob(context, jobURI);
        assertUpcomingAndTtlEq(context, jobURI, 1, nextScheduledDt);

        before = System.currentTimeMillis();
        Thread.sleep(1);
        Kernel.getSchedule().runJobNow(context, jobURI, null);
        Thread.sleep(1);
        assertUpcomingAndTtlRange(context, jobURI, 2, before, System.currentTimeMillis());

        Kernel.getSchedule().resetJob(context, jobURI);
        assertUpcomingAndTtlEq(context, jobURI, 2, nextScheduledDt);
    }

    private void assertUpcomingAndTtlEq(CallingContext context, String jobURI, int expectedTotal, DateTime expectedUpcoming) {
        List<RaptureJobExec> upcoming = Kernel.getSchedule().getUpcomingJobs(context);
        Long upcomingExecTime = upcoming.get(0).getExecTime();
        assertEquals(String.format("expected time=%s, actual=%s", expectedUpcoming, new DateTime(upcomingExecTime)), expectedUpcoming.getMillis(),
                upcomingExecTime.longValue());
        assertNumExecs(context, jobURI, expectedTotal, upcoming);
    }

    private void assertUpcomingAndTtlRange(CallingContext context, String jobURI, int expectedTotal, Long beforeUpcoming, Long afterUpcoming) {
        List<RaptureJobExec> upcoming = Kernel.getSchedule().getUpcomingJobs(context);
        Long upcomingExecTime = upcoming.get(0).getExecTime();

        System.out.println("*** This test case appears to be prone to race conditions\n");
        System.out.println("jobURI = " + jobURI);
        System.out.println("upcoming job count = " + upcoming.size() + "\n");

        for (RaptureJobExec ec : upcoming)
            System.out.println(ec.debug() + "\n");

        assertTrue(String.format("expected before=%s, actual=%s", new DateTime(beforeUpcoming), new DateTime(upcomingExecTime)),
                beforeUpcoming - 1 < upcomingExecTime);
        assertNumExecs(context, jobURI, expectedTotal, upcoming);
        Long nextExecTime = getEarliestExecTime(context, jobURI, upcoming);
        assertTrue(String.format("expected after=%s, actual=%s", new DateTime(afterUpcoming), new DateTime(nextExecTime)),
                afterUpcoming + 1 > nextExecTime);
    }

    private void assertNumExecs(CallingContext context, String jobURI, int expectedTotal, List<RaptureJobExec> upcoming) {
        assertEquals(1, upcoming.size());
        List<RaptureJobExec> execs = Kernel.getSchedule().getJobExecs(context, jobURI, 0, 10, false);
        assertEquals(expectedTotal, execs.size());
    }

    private Long getEarliestExecTime(CallingContext context, String jobURI, List<RaptureJobExec> upcoming) {
        List<RaptureJobExec> execs = Kernel.getSchedule().getJobExecs(context, jobURI, 0, 10, false);
        Long ret = execs.get(0).getExecTime();
        for (RaptureJobExec e : execs) {
            if (e.getExecTime() < ret) {
                ret = e.getExecTime();
            }
        }
        return ret;
    }
}
