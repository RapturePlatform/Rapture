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
package rapture.kernel;

import static rapture.common.Scheme.JOB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;

import rapture.common.CallingContext;
import rapture.common.JobErrorAck;
import rapture.common.JobErrorAckStorage;
import rapture.common.JobErrorType;
import rapture.common.JobExecStatus;
import rapture.common.JobLink;
import rapture.common.JobLinkPathBuilder;
import rapture.common.JobLinkStatus;
import rapture.common.JobLinkStatusPathBuilder;
import rapture.common.JobLinkStatusStorage;
import rapture.common.JobLinkStorage;
import rapture.common.JobType;
import rapture.common.Messages;
import rapture.common.RaptureJob;
import rapture.common.RaptureJobExec;
import rapture.common.RaptureJobExecStorage;
import rapture.common.RaptureJobStorage;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.TimedEventRecord;
import rapture.common.UpcomingJobExec;
import rapture.common.UpcomingJobExecStorage;
import rapture.common.WorkOrderExecutionState;
import rapture.common.WorkflowExecsStatus;
import rapture.common.WorkflowJobExecDetails;
import rapture.common.api.ScheduleApi;
import rapture.common.dp.WorkOrder;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.JsonContent;
import rapture.config.MultiValueConfigLoader;
import rapture.event.generator.RangedEventGenerator;
import rapture.kernel.schedule.CronParser;
import rapture.kernel.schedule.ExecStatusHelper;
import rapture.kernel.schedule.JobExecSort;
import rapture.kernel.schedule.MultiCronParser;
import rapture.kernel.schedule.ScheduleManager;
import rapture.kernel.schedule.WorkflowExecsRepo;
import rapture.repo.RepoVisitor;

/**
 * Manage the schedule of Rapture
 *
 * @author amkimian
 */
public class ScheduleApiImpl extends KernelBase implements ScheduleApi {
    private static Logger logger = Logger.getLogger(ScheduleApiImpl.class);

    public ScheduleApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    @Override
    public RaptureJob createJob(CallingContext context, String jobURI, String description, String scriptURI, String cronExpression, String timeZone,
            Map<String, String> jobParams, Boolean autoActivate) {
        return createJob(context, jobURI, description, scriptURI, cronExpression, timeZone, jobParams, autoActivate, JobType.SCRIPT, -1, null);
    }

    @Override
    public RaptureJob createWorkflowJob(CallingContext context, String jobURI, String description, String workflowURI, String cronExpression, String timeZone,
            Map<String, String> jobParams, Boolean autoActivate, int maxRuntimeMinutes, String appStatusNamePattern) {
        return createJob(context, jobURI, description, workflowURI, cronExpression, timeZone, jobParams, autoActivate, JobType.WORKFLOW, maxRuntimeMinutes,
                appStatusNamePattern);
    }

    private RaptureJob createJob(CallingContext context, String jobURI, String description, String executableURI, String cronExpression, String timeZone,
            Map<String, String> jobParams, Boolean autoActivate, JobType type, int maxRuntimeMinutes, String appStatusNamePattern) {
        RaptureJob job = new RaptureJob();
        job.setCronSpec(cronExpression);
        // validate timezone
        DateTimeZone dateTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone(timeZone));
        if (!dateTimeZone.getID().equals(timeZone)) {
            throw RaptureExceptionFactory.create("Invalid TimeZone " + timeZone);
        }
        job.setTimeZone(timeZone);
        RaptureURI uri = new RaptureURI(jobURI, Scheme.JOB);
        job.setJobURI(uri.toString());
        if (type == JobType.SCRIPT) {
            job.setScriptURI(executableURI);
        } else {
            job.setScriptURI(new RaptureURI(executableURI, Scheme.WORKFLOW).toString());
        }
        job.setParams(jobParams);
        job.setDescription(description);
        job.setAutoActivate(autoActivate);
        if (autoActivate) {
            job.setActivated(true);
        } else {
            job.setActivated(false);
        }
        job.setJobType(type);
        job.setMaxRuntimeMinutes(maxRuntimeMinutes);
        if (appStatusNamePattern != null) {
            job.setAppStatusNamePattern(appStatusNamePattern);
        }
        RaptureJobStorage.add(uri, job, context.getUser(), Messages.getString("Schedule.createNew")); //$NON-NLS-1$
        ScheduleManager.handleJobChanged(job, false, null, null);
        return job;
    }

    @Override
    public RaptureJob retrieveJob(CallingContext context, String jobURI) {
        RaptureURI parsedURI = new RaptureURI(jobURI, JOB);
        if (logger.isTraceEnabled()) {
            logger.trace("RetrieveJob: parsedURI = " + parsedURI.toString() + "\n" + parsedURI.debug());
        }
        RaptureJob job = RaptureJobStorage.readByAddress(parsedURI);
        if (logger.isTraceEnabled()) {
            logger.trace("Job is " + ((job != null) ? job.debug() : "NULL"));
        }
        return job;
    }

    @Override
    public void deleteJob(CallingContext context, String jobURI) {
        RaptureURI parsedURI = new RaptureURI(jobURI, JOB);
        if (retrieveJob(context, jobURI) == null) {
            throw RaptureExceptionFactory.create(String.format("Job at %s does not exist", jobURI));
        }
        logger.info(String.format(Messages.getString("Schedule.removeRecords"), parsedURI.toString())); //$NON-NLS-1$
        ScheduleManager.removeJob(jobURI);

        // Remove links from this job
        logger.info(String.format(Messages.getString("Schedule.removeFromJobLinks"), parsedURI.toString()));
        for (JobLink link : getLinksFrom(context, jobURI)) {
            removeJobLinkStrings(link.getFrom(), link.getTo(), context.getUser());
        }

        // Remove links to this job
        logger.info(String.format(Messages.getString("Schedule.removeToJobLinks"), parsedURI.toString()));
        for (JobLink link : getLinksTo(context, jobURI)) {
            removeJobLinkStrings(link.getFrom(), link.getTo(), context.getUser());
        }

        logger.info(Messages.getString("Schedule.removeJob")); //$NON-NLS-1$
        RaptureJobStorage.deleteByAddress(parsedURI, context.getUser(), Messages.getString("Schedule.removedJob")); //$NON-NLS-1$
    }

    @Override
    public List<String> getJobs(final CallingContext context) {
        final List<String> ret = new ArrayList<String>();
        RaptureJobStorage.visitAll(new RepoVisitor() {

            @Override
            public boolean visit(String uri, JsonContent content, boolean isFolder) {
                if (!isFolder) {
                    RaptureJob retrieveJob = RaptureJobStorage.readFromJson(content);
                    if (retrieveJob == null) {
                        throw RaptureExceptionFactory.create("No job found for " + uri);
                    }
                    if (retrieveJob.getAddressURI() == null) {
                        throw RaptureExceptionFactory.create("Address URI is null. " + retrieveJob.toString());
                    }
                    ret.add(retrieveJob.getAddressURI().toString());
                }
                return true;
            }

        });
        return ret;
    }

    @Override
    public List<RaptureJob> retrieveJobs(CallingContext context, String prefix) {
        RaptureURI internalUri = new RaptureURI(prefix, Scheme.JOB);
        return RaptureJobStorage.readAll(internalUri.getFullPath());
    }

    @Override
    public List<RaptureJobExec> batchGetJobExecs(CallingContext context, List<String> jobURIs, int start, int count, Boolean reversed) {
        List<RaptureJobExec> ret = new ArrayList<RaptureJobExec>();
        for (String uri : jobURIs) {
            ret.addAll(getJobExecs(context, uri, start, count, reversed));
        }
        return ret;
    }

    @Override
    public List<RaptureJobExec> getUpcomingJobs(CallingContext context) {
        final List<RaptureJobExec> ret = new ArrayList<RaptureJobExec>();
        UpcomingJobExecStorage.visitAll(new RepoVisitor() {

            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                if (!isFolder) {
                    ret.add(RaptureJobExecStorage.readFromJson(content));
                }
                return true;
            }

        });
        return ret;
    }

    @Override
    public RaptureJobExec getNextExec(CallingContext context, String jobURI) {
        UpcomingJobExec upcomingExec = UpcomingJobExecStorage.readByFields(jobURI);
        if (upcomingExec != null) {
            return JacksonUtil.objectFromJson(JacksonUtil.jsonFromObject(upcomingExec), RaptureJobExec.class);
        } else {
            return null;
        }

    }

    @Override
    public List<RaptureJobExec> getRunningWorkflowJobs(CallingContext context) {
        List<RaptureJobExec> ret = new ArrayList<>();
        Map<RaptureJobExec, WorkOrder> je = Kernel.getDecision().getJobExecsAndWorkOrdersByDay(context, System.currentTimeMillis());
        for (Map.Entry<RaptureJobExec, WorkOrder> entry : je.entrySet()) {
            WorkOrder wo = entry.getValue();
            if (wo.getStatus() == WorkOrderExecutionState.ACTIVE ||
                    wo.getStatus() == WorkOrderExecutionState.FAILING ||
                    wo.getStatus() == WorkOrderExecutionState.CANCELLING ||
                    wo.getStatus() == WorkOrderExecutionState.NEW) {
                ret.add(entry.getKey());
            }
        }
        return ret;
    }

    @Override
    public List<RaptureJobExec> getJobExecs(CallingContext context, String jobURI, int start, int count, Boolean reversed) {
        String jobExecURI = new RaptureURI(jobURI, Scheme.JOB).getShortPath();

        // Retrieve the job execs for this job
        // If going forward, we simply load the RaptureJobExecs in order
        // If going backward we need to get the latest job exec number from the
        // RaptureJob and
        // go down from that. When we don't find one we stop.
        List<RaptureJobExec> ret = new ArrayList<RaptureJobExec>();

        if (!jobExecURI.endsWith("/")) jobExecURI = jobExecURI + "/";

        List<RaptureJobExec> execs = RaptureJobExecStorage.readAll(jobExecURI);
        execs = JobExecSort.sortByExecTime(execs);
        if (start > execs.size() && !reversed) {
            logger.warn(String.format("Will not return any execs, requested to start at position %s, but we only have %s execs total", start, execs.size()));
        } else {
            int last;
            int first;
            if (reversed) {
                if (execs.size() > start) {
                    last = execs.size() - start;
                } else {
                    last = execs.size();
                }
                first = Math.max(0, last - count);
            } else {
                first = start;
                last = Math.min(execs.size(), start + count);
            }
            for (int i = first; i < last; i++) {
                ret.add(execs.get(i));
            }
        }

        return ret;
    }

    @Override
    public RaptureJobExec retrieveJobExec(CallingContext context, String jobURI, Long execTime) {
        RaptureURI parsedURI = new RaptureURI(jobURI, Scheme.JOB);
        return RaptureJobExecStorage.readByFields(parsedURI.toString(), execTime);
    }

    @Override
    public void activateJob(CallingContext context, String jobURI, Map<String, String> extraJobParams) {
        RaptureURI parsedURI = new RaptureURI(jobURI, JOB);
        RaptureJob job = RaptureJobStorage.readByAddress(parsedURI);
        job.setActivated(true);
        RaptureJobStorage.add(parsedURI, job, context.getUser(), "job activated");
        ScheduleManager.handleJobChanged(job, false, extraJobParams, null);
    }

    @Override
    public void deactivateJob(CallingContext context, String jobURI) {
        RaptureURI parsedURI = new RaptureURI(jobURI, JOB);
        RaptureJob job = RaptureJobStorage.readByAddress(parsedURI);
        job.setActivated(false);
        RaptureJobStorage.add(parsedURI, job, context.getUser(), "job deactivated");
        ScheduleManager.handleJobChanged(job, false, null, null);
    }

    @Override
    public String runJobNow(CallingContext context, String jobURI, Map<String, String> extraJobParams) {
        RaptureURI parsedURI = new RaptureURI(jobURI, JOB);
        RaptureJob job = RaptureJobStorage.readByAddress(parsedURI);
        if (job == null) {
            throw RaptureExceptionFactory.create("Cannot load job for URI " + parsedURI);
        }
        return ScheduleManager.runJobNow(job, extraJobParams);
    }

    @Override
    public void resetJob(CallingContext context, String jobURI) {
        RaptureJobExec jobExec = getNextExec(context, jobURI);
        if (jobExec != null) {
            ScheduleManager.handleJobExecutionCompleted(jobExec);
        }
    }

    public Boolean setJobLink(CallingContext context, String fromJobURI, String toJobURI) {
        RaptureURI parsedFromURI = new RaptureURI(fromJobURI, JOB);
        RaptureURI parsedToURI = new RaptureURI(toJobURI, JOB);

        JobLink link = new JobLink();
        link.setFrom(parsedFromURI.toString());
        link.setTo(parsedToURI.toString());
        JobLinkStorage.add(link, context.getUser(), "Set job link");
        setJobLinkStatus(context, fromJobURI, toJobURI, 0);
        return true;
    }

    public Boolean removeJobLink(CallingContext context, String fromJobURI, String toJobURI) {
        RaptureURI parsedFromURI = new RaptureURI(fromJobURI, JOB);
        RaptureURI parsedToURI = new RaptureURI(toJobURI, JOB);
        return removeJobLinkStrings(parsedFromURI.toString(), parsedToURI.toString(), context.getUser());

    }

    /**
     * Remove the links, passing in strings
     *
     * @param from
     * @param to
     * @param user
     * @return
     */
    private Boolean removeJobLinkStrings(String from, String to, String user) {
        // remove JobLinkStatus
        JobLinkStatusStorage.deleteByFields(to, from, user, "Remove job link");
        // remove JobLink
        return JobLinkStorage.deleteByFields(from, to, user, "Remove job link");
    }

    public List<JobLinkStatus> resetJobLink(CallingContext context, String fromJobURI) {
        // Find job links starting at this fromPoint, collecting and ensure that
        // we don't visit circular
        // Set these job links to 0
        Set<String> seenFrom = new HashSet<String>();
        List<JobLinkStatus> statusUpdates = updateLinkStatus(context, fromJobURI, seenFrom);
        for (JobLinkStatus status : statusUpdates) {
            JobLinkStatusStorage.add(status, context.getUser(), "reset job link");
        }
        return statusUpdates;
    }

    private List<JobLinkStatus> updateLinkStatus(CallingContext context, String jobURI, Set<String> seen) {
        RaptureURI parsedURI = new RaptureURI(jobURI, JOB);
        List<JobLinkStatus> ret = new ArrayList<JobLinkStatus>();
        seen.add(jobURI);
        List<JobLink> links = getLinksFrom(context, jobURI);
        for (JobLink l : links) {
            JobLinkStatus newLink = new JobLinkStatus();
            newLink.setFrom(parsedURI.toString());
            newLink.setTo(l.getTo());
            newLink.setLevel(0);
            newLink.setLastChange(new Date());
            ret.add(newLink);
            if (!seen.contains(newLink.getTo())) {
                ret.addAll(updateLinkStatus(context, newLink.getTo(), seen));
            }
        }
        return ret;
    }

    public JobLinkStatus setJobLinkStatus(CallingContext context, String fromJobURI, String toJobURI, int level) {
        RaptureURI parsedFromURI = new RaptureURI(fromJobURI, JOB);
        RaptureURI parsedToURI = new RaptureURI(toJobURI, JOB);
        JobLinkStatus newLink = new JobLinkStatus();
        newLink.setFrom(parsedFromURI.toString());
        newLink.setTo(parsedToURI.toString());
        newLink.setLevel(level);
        newLink.setLastChange(new Date());
        JobLinkStatusStorage.add(newLink, context.getUser(), "set job link status");
        return newLink;
    }

    public List<JobLinkStatus> getJobLinkStatus(CallingContext context, String fromJobURI) {
        Set<String> seenFrom = new HashSet<String>();
        return getLinkStatus(context, fromJobURI, seenFrom);
    }

    private List<JobLinkStatus> getLinkStatus(CallingContext context, String jobURI, Set<String> seen) {
        List<JobLinkStatus> ret = new ArrayList<JobLinkStatus>();
        seen.add(jobURI);
        List<JobLink> links = getLinksFrom(context, jobURI);
        for (JobLink l : links) {
            JobLinkStatus status = JobLinkStatusStorage.readByFields(l.getTo(), l.getFrom());
            if (status != null) {
                ret.add(status);
            }
            if (!seen.contains(l.getTo())) {
                ret.addAll(getLinkStatus(context, l.getTo(), seen));
            }
        }
        return ret;
    }

    public List<JobLink> getLinksFrom(CallingContext context, String fromJobURI) {
        RaptureURI parsedFromURI = new RaptureURI(fromJobURI, JOB);
        // Look for those documents with the prefix of "fromJob" (as these will
        // be the links we are after)
        String prefix = new JobLinkPathBuilder().from(parsedFromURI.toString()).buildStorageLocation().getDocPath();
        final List<JobLink> ret = new ArrayList<JobLink>();
        getConfigRepo().visitAll(prefix, null, new RepoVisitor() {
            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                if (!isFolder) {
                    JobLink link = JobLinkStorage.readFromJson(content);
                    if (link != null) {
                        ret.add(link);
                    }
                }
                return true;
            }

        });

        logger.trace(String.format(Messages.getString("Schedule.childRequestSize"), ret.size())); //$NON-NLS-1$
        return ret;
    }

    public List<JobLink> getLinksTo(CallingContext context, String toJobURI) {

        RaptureURI parsedToURI = new RaptureURI(toJobURI, JOB);
        String prefix = new JobLinkStatusPathBuilder().to(parsedToURI.toString()).buildStorageLocation().getDocPath();

        final List<JobLink> ret = new ArrayList<JobLink>();
        getEphemeralRepo().visitAll(prefix, null, new RepoVisitor() {
            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                if (!isFolder) {
                    JobLinkStatus status = JobLinkStatusStorage.readFromJson(content);
                    if (status != null) {
                        JobLink link = JobLinkStorage.readByFields(status.getFrom(), status.getTo());
                        if (link != null) {
                            ret.add(link);
                        }
                    }
                }
                return true;
            }

        });
        return ret;
    }

    @Override
    public Boolean isJobReadyToRun(CallingContext context, String toJobURI) {
        // A Job is ready to run if all of its predecessors have a job link
        // status > 0
        List<JobLink> linksTo = getLinksTo(context, toJobURI);
        boolean ready = true;
        for (JobLink l : linksTo) {
            JobLinkStatus status = JobLinkStatusStorage.readByFields(l.getTo(), l.getFrom());
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append(String.format(Messages.getString("Schedule.linkStatus"), l.getFrom(), l.getTo())); //$NON-NLS-1$
            if (status == null || status.getLevel() == 0) {
                ready = false;
                if (status == null) {
                    logBuilder.append(Messages.getString("Schedule.nullStatus")); //$NON-NLS-1$
                } else {
                    logBuilder.append(Messages.getString("Schedule.levelZeroStatus")); //$NON-NLS-1$
                }
                logBuilder.append("\nNot scheduling ").append(l.getTo()).append(" yet because a job it depends on is not yet ready: ").append(l.getFrom());
                logger.info(logBuilder.toString());
                break;
            } else {
                logBuilder.append(String.format(Messages.getString("Schedule.levelStatus"), status.getLevel())); //$NON-NLS-1$
                logger.info(logBuilder.toString());
            }
        }
        return ready;
    }

    @Override
    public List<TimedEventRecord> getCurrentWeekTimeRecords(CallingContext context, int weekOffsetfromNow) {
        // Based on today, get current events
        MutableDateTime now = new MutableDateTime();
        now.setDayOfWeek(1);
        return RangedEventGenerator.generateWeeklyEvents(now.toDateTime());
    }

    @Override
    public List<TimedEventRecord> getCurrentDayJobs(CallingContext context) {
        List<TimedEventRecord> records = new ArrayList<>();
        for (String jobUri : getJobs(context)) {
            TimedEventRecord record = getEventRecordForJob(context, jobUri);
            if (record != null) {
                records.add(record);
            }
        }
        // sort by job start time, then description
        Collections.sort(records, new Comparator<TimedEventRecord>() {
            @Override
            public int compare(TimedEventRecord o1, TimedEventRecord o2) {
                int timeResult = o1.getWhen().compareTo(o2.getWhen());
                return timeResult == 0 ? o1.getEventName().compareTo(o2.getEventName()) : timeResult;
            }
        });
        return records;
    }

    private TimedEventRecord getEventRecordForJob(CallingContext context, String jobUri) {
        RaptureJob job = retrieveJob(context, jobUri);
        CronParser parser = MultiCronParser.create(job.getCronSpec());

        DateTime midnight = DateMidnight.now().toDateTime(DateTimeZone.forID(job.getTimeZone()));
        DateTime nextRunDate = parser.nextRunDate(midnight);
        if (nextRunDate != null) {
            TimedEventRecord record = new TimedEventRecord();
            record.setEventName(job.getDescription());
            record.setEventContext(jobUri);
            record.setInfoContext(job.getActivated().toString());
            record.setWhen(nextRunDate.toDate());
            return record;
        } else {
            return null;
        }
    }

    @Override
    public WorkflowExecsStatus getWorkflowExecsStatus(CallingContext context) {
        WorkflowExecsRepo execsRepo = new WorkflowExecsRepo();
        Long lateThreshold = execsRepo.createLateThreshold();
        WorkflowExecsStatus status = new WorkflowExecsStatus();
        List<WorkflowJobExecDetails> okList = new LinkedList<WorkflowJobExecDetails>();
        status.setOk(okList);
        List<WorkflowJobExecDetails> failedList = new LinkedList<WorkflowJobExecDetails>();
        status.setFailed(failedList);
        List<WorkflowJobExecDetails> overrunList = new LinkedList<WorkflowJobExecDetails>();
        status.setOverrun(overrunList);
        List<WorkflowJobExecDetails> successList = new LinkedList<WorkflowJobExecDetails>();
        status.setSuccess(successList);

        List<WorkflowJobExecDetails> lastList = execsRepo.getLastWorkflowJobExecs(context);
        for (WorkflowJobExecDetails currentExecDetails : lastList) {
            WorkOrderExecutionState workOrderStatus = currentExecDetails.getWorkOrderStatus();
            JobExecStatus jobStatus = currentExecDetails.getJobStatus();
            if (ExecStatusHelper.isSuccess(workOrderStatus, jobStatus)) {
                successList.add(currentExecDetails);
            } else if (ExecStatusHelper.isOk(workOrderStatus, jobStatus, currentExecDetails)) {
                okList.add(currentExecDetails);
            } else if (ExecStatusHelper.isFailed(workOrderStatus, jobStatus)) {
                failedList.add(currentExecDetails);
            } else if (ExecStatusHelper.isOverrun(currentExecDetails)) {
                overrunList.add(currentExecDetails);
            } else {
                logger.error(String.format("Unrecognized status %s %s for %s-%s", workOrderStatus, jobStatus, currentExecDetails.getJobURI(),
                        currentExecDetails.getStartDate()));
            }
        }

        List<WorkflowJobExecDetails> upcomingList = execsRepo.getUpcomingWorkflowJobExecs(context);

        /*
         * Upcoming tasks can be either scheduled in the future, which makes them OK, or scheduled in the past, which means they haven't run for some reason --
         * which is a problem
         */
        List<WorkflowJobExecDetails> lateList = new LinkedList<WorkflowJobExecDetails>();
        for (WorkflowJobExecDetails current : upcomingList) {
            if (ExecStatusHelper.isLate(lateThreshold, current)) {
                lateList.add(current);
            } else {
                okList.add(current);
            }
        }
        overrunList.addAll(0, lateList);

        return status;
    }

    @Override
    public JobErrorAck ackJobError(CallingContext context, String jobURI, Long execTime, String jobErrorType) {
        logger.info(String.format("acking job %s execTime %s errorType %s from user %s", jobURI, execTime, jobErrorType, context.getUser()));
        if (!allowAllAcks() && "rapture".equals(context.getUser()) || "raptureApi".equals(context.getUser())) {
            throw RaptureExceptionFactory.create(HttpStatus.SC_BAD_REQUEST,
                    String.format(
                            "You need to be logged in using your own credentials to ack a job. You are currently logged in using the system account \"%s\".",
                            context.getUser()));
        } else {

            JobErrorType errorType;
            try {
                errorType = JobErrorType.valueOf(jobErrorType);
            } catch (IllegalArgumentException | NullPointerException e) {
                throw RaptureExceptionFactory.create(HttpStatus.SC_BAD_REQUEST, String.format("Bad jobErrorType: '%s'", jobErrorType));
            }
            JobErrorAck ack = new JobErrorAck();
            ack.setErrorType(errorType);
            ack.setExecTime(execTime);
            ack.setJobURI(jobURI);
            ack.setTimestamp(System.currentTimeMillis());
            ack.setUser(context.getUser());
            JobErrorAckStorage.add(ack, context.getUser(), "Adding ack");
            return ack;
        }
    }

    private boolean allowAllAcks() {
        return Boolean.valueOf(MultiValueConfigLoader.getConfig("DECISION-allowAllAcks"));
    }
}
