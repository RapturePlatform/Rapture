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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SimpleTimeZone;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

import rapture.common.JobExecStatus;
import rapture.common.JobLink;
import rapture.common.JobType;
import rapture.common.LastJobExec;
import rapture.common.LastJobExecStorage;
import rapture.common.RaptureJob;
import rapture.common.RaptureJobExec;
import rapture.common.RaptureJobExecStorage;
import rapture.common.RaptureJobStorage;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.UpcomingJobExec;
import rapture.common.UpcomingJobExecStorage;
import rapture.common.WorkflowJobDetails;
import rapture.common.dp.ContextVariables;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.mime.MimeScheduleReflexScriptRef;
import rapture.common.pipeline.PipelineConstants;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.pipeline.TaskSubmitter;

/**
 * The schedule manage handles the interaction between Jobs and their executions, and is used by a Scheduler application (an application that is a RaptureCore
 * app, so can access this class) to convert jobs to job-execs, and to handle the execution of a job-exec when its time has come. The schedule manage will use
 * the Kernel to retrieve documents and interact with the Pipeline for task submission.
 * <p/>
 * The kernel schedule api will also interact with this class when jobs are updated or changed - so that their next execution job can be set correctly.
 * 
 * @author amkimian
 */
public class ScheduleManager {
    private static Logger logger = Logger.getLogger(ScheduleManager.class);

    /**
     * The definition of the RaptureJob has changed, so make sure it's still scheduled to run at the right time (upcoming job schedule etc)
     * <p/>
     * Need to watch out of the upcoming job exec is not WAITING though.
     * 
     * @param job
     * @param passedParams
     */
    public static void handleJobChanged(RaptureJob job, boolean withAdvance, Map<String, String> passedParams, RaptureJobExec jobExec) {
        // 1. Compute next execution time given a the cron spec for this job
        // 2. Store that in a RaptureJobExec, with the counter set to the job,
        // name from the job
        // 3. Status is WAITING
        // 4. Store job (warn if jobexec already exists and is not WAITING)
        logger.info("Job " + job.getJobURI() + " has changed, processing results");
        if (job.getActivated()) {
            CronParser parser = MultiCronParser.create(job.getCronSpec());
            DateTime dt = new DateTime();
            DateTime cal = dt.withZone(DateTimeZone.forID(job.getTimeZone()));
            logger.info("cal is " + cal.toString());
            DateTime nextRunDate = parser.nextRunDate(cal);
            RaptureJobExec exec = new RaptureJobExec();
            exec.setJobType(job.getJobType());
            exec.setJobURI(job.getAddressURI().toString());
            if (nextRunDate != null) {
                logger.info("Updated next run date for the job is " + nextRunDate.toString("dd MMM yyyy HH:mm:ss_SSS z"));
                exec.setExecTime(nextRunDate.getMillis());
                exec.setStatus(JobExecStatus.WAITING);
            } else {
                logger.info("Job is finished for good.  No more future runs.");
                if (jobExec != null) {
                    exec.setExecTime(jobExec.getExecTime());
                }
                exec.setStatus(JobExecStatus.FINISHED);
            }

            if (passedParams != null) {
                exec.setPassedParams(passedParams);
            }
            String user = ContextFactory.getKernelUser().getUser();
            String comment = "Job changed";
            RaptureJobExecStorage.add(exec, user, comment);
            updateUpcoming(exec, user, comment);
        } else {
            logger.info("Job " + job.getJobURI() + " is not activated, no execution will be created, clearing current execution");
            String user = ContextFactory.getKernelUser().getUser();
            UpcomingJobExecStorage.deleteByFields(job.getJobURI(), user, "job changed, not active");
        }
    }

    private static void updateUpcoming(RaptureJobExec exec, String user, String comment) {
        UpcomingJobExec upcoming = JacksonUtil.objectFromJson(JacksonUtil.jsonFromObject(exec), UpcomingJobExec.class);
        UpcomingJobExecStorage.add(upcoming, user, comment);
    }

    public static Boolean runJobNow(RaptureJob job, Map<String, String> passedParams) {
        logger.info("Request to run " + job.getAddressURI() + " as soon as possible");
        Calendar nextRunDate = Calendar.getInstance();
        nextRunDate.setTime(new Date());
        RaptureJobExec exec = new RaptureJobExec();
        exec.setJobURI(job.getAddressURI().toString());
        exec.setExecTime(nextRunDate.getTime().getTime());
        exec.setStatus(JobExecStatus.WAITING);
        exec.setJobType(job.getJobType());
        exec.setPassedParams(passedParams);
        // Reset dependent job status also
        Kernel.getSchedule().getTrusted().resetJobLink(ContextFactory.getKernelUser(), job.getAddressURI().toString());
        String user = ContextFactory.getKernelUser().getUser();
        String comment = "Running job";
        // save new one as upcoming
        RaptureJobExecStorage.add(exec, user, comment);
        // update whatever is currently marked as upcoming
        updateUpcoming(exec, user, comment);
        return true;
    }

    /**
     * A job execution has completed - need to 1. Update its status and save that 2. Calculate the upcoming job iteration and save that, so that it can be
     * spawned in the future 3. Update the last execution so we can retrieve it easily 4. We also update the RaptureJob as that contains the latest known
     * execution number.
     * 
     * @param jobexec
     */
    public static void handleJobExecutionCompleted(RaptureJobExec jobexec) {
        logger.info("Job " + jobexec.getJobURI() + " has finished");
        JobExecStatus status = JobExecStatus.FINISHED;
        RaptureJob job = jobExecCompleted(jobexec, status);
        // Now here is the interesting bit. We mark dependent links from *this*
        // job
        // as being done. Then, we look at those jobs which have a link to this
        // job.
        // If any of those jobs have all of their links satisfied we can
        // activate it.

        logger.info("Retrieving dependent jobs from " + job.getAddressURI());
        List<JobLink> linksFrom = Kernel.getSchedule().getTrusted().getLinksFrom(ContextFactory.getKernelUser(), job.getAddressURI().toString());
        for (JobLink link : linksFrom) {
            logger.info(link.getTo() + " is dependent on " + job.getAddressURI() + ", activating link");
            Kernel.getSchedule().getTrusted().setJobLinkStatus(ContextFactory.getKernelUser(), link.getFrom(), link.getTo(), 1);
            // Check inbound link status
            // If ok, fire off a dependent job
            if (Kernel.getSchedule().isJobReadyToRun(ContextFactory.getKernelUser(), link.getTo())) {
                logger.info("Activating dependent job " + "//authority/" + link.getTo());
                RaptureJob dependent = Kernel.getSchedule().retrieveJob(ContextFactory.getKernelUser(), link.getTo());

                if (dependent != null) {
                    dependent.setActivated(true);
                    RaptureJobStorage.add(job, ContextFactory.getKernelUser().getUser(), "Parent job exec completed");
                    handleJobChanged(dependent, false, jobexec.getPassedParams(), jobexec);
                }
            } else {
                logger.info(link.getTo() + " is not yet ready to run.");
            }
        }

        handleJobChanged(job, true, null, jobexec);
    }

    private static RaptureJob jobExecCompleted(RaptureJobExec jobexec, JobExecStatus status) {
        jobexec.setStatus(status);
        String user = ContextFactory.getKernelUser().getUser();
        String comment = "Job execution completed";
        LastJobExec lastExec = JacksonUtil.objectFromJson(JacksonUtil.jsonFromObject(jobexec), LastJobExec.class);
        LastJobExecStorage.add(lastExec, user, comment);
        RaptureJobExecStorage.add(jobexec, user, comment);
        RaptureJob job = Kernel.getSchedule().retrieveJob(ContextFactory.getKernelUser(), jobexec.getJobURI());
        if (job.getAutoActivate()) {
            job.setActivated(true);
            RaptureJobStorage.add(job, user, "Job exec completed");
        }
        return job;
    }

    public static void handleJobExecutionFailed(RaptureJobExec jobexec) {
        logger.info("Job " + jobexec.getJobURI() + " has finished in error");
        jobexec.setStatus(JobExecStatus.FAILED);
        RaptureJob job = jobExecCompleted(jobexec, JobExecStatus.FAILED);
        handleJobChanged(job, true, null, jobexec);
    }

    /**
     * Look at the latest job executions.
     * <p/>
     * They are either WAITING, so do we need to schedule this? (is its time for execution in the past) SCHEDULED, how long has it been on the pipeline? (abort
     * if too long?) RUNNING, how long has it been running? (abort if too long?)
     * <p/>
     * The main one is WAITING, and this will submit an appropriate task to the Pipeline engine to ultimately execute this task.
     */
    public static void manageJobExecStatus() {
        List<RaptureJobExec> jobs = Kernel.getSchedule().getUpcomingJobs(ContextFactory.getKernelUser());
        Date now = new Date();
        for (RaptureJobExec jobexec : jobs) {
            logger.debug("Checking job " + jobexec.getJobURI() + " with status of " + jobexec.getStatus());
            if (jobexec.getStatus() == JobExecStatus.WAITING) {

                logger.debug("Job is waiting, next run date is " + toGMTFormat(new Date(jobexec.getExecTime())));
                if (jobexec.getExecTime() < now.getTime()) {
                    // Need to run this
                    // submit job
                    logger.info("Will run job " + jobexec.getJobURI());
                    // Change status to SCHEDULED - do this first as the
                    // execution can happen far too quickly

                    RaptureJob job = Kernel.getSchedule().retrieveJob(ContextFactory.getKernelUser(), jobexec.getJobURI());
                    if (job != null) {
                        jobexec.setStatus(JobExecStatus.SCHEDULED);

                        String user = ContextFactory.getKernelUser().getUser();
                        String comment = "Scheduling job";
                        RaptureJobExecStorage.add(jobexec, user, comment);
                        logger.debug("Job Status is: " + jobexec.getStatus());

                        job.setActivated(false);
                        RaptureJobStorage.add(job, user, "Job about to run");

                        // Reset dependent job status also
                        Kernel.getSchedule().getTrusted().resetJobLink(ContextFactory.getKernelUser(), jobexec.getJobURI());

                        executeJob(jobexec, job);
                    } else {
                        logger.error(String.format("Unable to find job %s for execution %s ", jobexec.getJobURI(), jobexec.getExecTime()));
                    }
                }
            }
        }
    }

    private static void executeJob(RaptureJobExec jobexec, RaptureJob job) {
        if (job.getJobType() == JobType.WORKFLOW) {
            String workflowURI = job.getScriptURI(); // we need to rename this
            // to executableURI
            // eventually...
            Map<String, String> contextMap = job.getParams();
            long timestamp = jobexec.getExecTime();
            contextMap.put(ContextVariables.TIMESTAMP, timestamp + "");
            DateTimeZone timezone;
            if (job.getTimeZone() != null) {
                timezone = DateTimeZone.forID(job.getTimeZone());
            } else {
                timezone = DateTimeZone.UTC;
            }

            LocalDate ld = new LocalDate(timestamp, timezone);
            contextMap.put(ContextVariables.LOCAL_DATE, ContextVariables.FORMATTER.print(ld));

            contextMap.put(ContextVariables.PARENT_JOB_URI, job.getJobURI());
            contextMap.putAll(jobexec.getPassedParams()); // these override
            // everything
            String workOrderURI = null;
            try {
                if (job.getAppStatusNamePattern() != null && job.getAppStatusNamePattern().length() > 0) {
                    workOrderURI = Kernel.getDecision()
                            .createWorkOrderP(ContextFactory.getKernelUser(), workflowURI, contextMap, job.getAppStatusNamePattern()).getUri();
                } else {
                    workOrderURI = Kernel.getDecision().createWorkOrder(ContextFactory.getKernelUser(), workflowURI, contextMap);
                }
                logger.info(String.format("Execution %s of job %s created work order %s", jobexec.getExecTime(), job.getJobURI(), workOrderURI));
            } catch (Exception e) {
                logger.error(String.format("Error executing job %s: %s", job.getJobURI(), ExceptionToString.format(e)));
            }
            if (workOrderURI == null) {
                handleJobExecutionFailed(jobexec);
            } else {
                WorkflowJobDetails workflowJobDetails = new WorkflowJobDetails();
                workflowJobDetails.setWorkOrderURI(workOrderURI);
                jobexec.setExecDetails(JacksonUtil.jsonFromObject(workflowJobDetails));
                handleJobExecutionCompleted(jobexec);
            }
        } else {
            MimeScheduleReflexScriptRef scriptRef = new MimeScheduleReflexScriptRef();
            scriptRef.setScriptURI(job.getScriptURI());
            scriptRef.setJobURI(jobexec.getJobURI());
            Map<String, Object> execParams = new HashMap<String, Object>();
            for (Map.Entry<String, String> entry : jobexec.getPassedParams().entrySet()) {
                execParams.put(entry.getKey(), entry.getValue());
            }
            if (job.getParams() != null) {
                // Also add in standard params
                for (Map.Entry<String, String> entry : job.getParams().entrySet()) {
                    execParams.put(entry.getKey(), entry.getValue());
                }
            }
            scriptRef.setParameters(execParams);
            TaskSubmitter.submitLoadBalancedToCategory(ContextFactory.getKernelUser(), scriptRef, MimeScheduleReflexScriptRef.getMimeType(),
                    PipelineConstants.CATEGORY_ALPHA);
            handleJobExecutionCompleted(jobexec);
        }
    }

    private static String toGMTFormat(Date d) {
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
        sdf.applyPattern("dd MMM yyyy HH:mm:ss z");
        return sdf.format(d);
    }


    /**
     * A job has been removed - remove all traces in job execution. Watch out for running jobs.
     * 
     * @param jobURI
     */
    public static void removeJob(String jobURI) {
        // Remove all documents below RaptureJobExec.
        String user = ContextFactory.getKernelUser().getUser();
        String comment = "Removed job";

        if (jobURI == null) throw RaptureExceptionFactory.create("Illegal argument: jobURI is null");

        final List<RaptureJobExec> deleteList = RaptureJobExecStorage.readAll(new RaptureURI(jobURI, Scheme.JOB).getShortPath());
        if (deleteList == null) {
            throw RaptureExceptionFactory.create("URI could not retrieve workorder");
        }
        for (RaptureJobExec exec : deleteList) {
            logger.info(String.format("Removing exec %s, %s", exec.getJobURI(), exec.getExecTime()));
            RaptureJobExecStorage.deleteByStorageLocation(exec.getStorageLocation(), user, comment);
        }
        LastJobExecStorage.deleteByFields(jobURI, user, comment);
        UpcomingJobExecStorage.deleteByFields(jobURI, user, comment);
    }

    /**
     * Retrieve a consolidated view of what the scheduler status is, by looking at the complete set of jobs, and for each job showing its dependencies, the next
     * run time (if activated) and the last run time. Try to sort so that related information is kept together.
     * 
     * @return
     */
    public static List<ScheduleStatusLine> getSchedulerStatus() {
        List<ScheduleStatusLine> ret = new ArrayList<ScheduleStatusLine>();
        List<String> jobs = Kernel.getSchedule().getJobs(ContextFactory.getKernelUser());
        for (String jobName : jobs) {
            RaptureJob job = Kernel.getSchedule().retrieveJob(ContextFactory.getKernelUser(), jobName);
            if (job.getActivated()) {
                RaptureJobExec exec = Kernel.getSchedule().getNextExec(ContextFactory.getKernelUser(), jobName);
                ScheduleStatusLine thisLine = new ScheduleStatusLine();
                thisLine.setName(job.getAddressURI().toString());
                thisLine.setSchedule(job.getCronSpec());
                thisLine.setDescription(job.getDescription());
                thisLine.setWhen(new Date(exec.getExecTime()));
                thisLine.setActivated(job.getActivated() ? "ACTIVE" : "INACTIVE");
                thisLine.setStatus(exec.getStatus().toString());
                thisLine.setPredecessor("");
                ret.add(thisLine);
            }
        }

        Set<String> seenJobs = new HashSet<String>();
        // Sort scheduleStatusLine
        Collections.sort(ret, new Comparator<ScheduleStatusLine>() {

            @Override
            public int compare(ScheduleStatusLine arg0, ScheduleStatusLine arg1) {
                return arg0.getWhen().compareTo(arg1.getWhen());
            }

        });

        List<ScheduleStatusLine> fullRet = new ArrayList<ScheduleStatusLine>();

        for (ScheduleStatusLine line : ret) {
            seenJobs.add(line.getName());
            fullRet.add(line);
            Kernel.getSchedule().retrieveJob(ContextFactory.getKernelUser(), line.getName());
        }

        for (String jobName : jobs) {
            if (!seenJobs.contains(jobName)) {
                addInactiveLine(fullRet, null, jobName);
            }
        }

        return fullRet;
    }

    private static void addInactiveLine(List<ScheduleStatusLine> fullRet, ScheduleStatusLine line, String dependency) {
        ScheduleStatusLine newLine = new ScheduleStatusLine();
        RaptureJob dependentJob = Kernel.getSchedule().retrieveJob(ContextFactory.getKernelUser(), dependency);
        newLine.setName(dependentJob.getJobURI());
        newLine.setSchedule(dependentJob.getCronSpec());
        newLine.setDescription(dependentJob.getDescription());
        newLine.setWhen(null);
        newLine.setActivated(dependentJob.getActivated() ? "ACTIVE" : "INACTIVE");
        newLine.setStatus("");
        newLine.setPredecessor(line != null ? line.getName() : "");
        fullRet.add(newLine);
    }
}
