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
package rapture.dp.invocable;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.USCalendar;
import rapture.common.SeriesPoint;
import rapture.common.dp.AbstractInvocable;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.Kernel;

public class CheckPrerequisiteStep extends AbstractInvocable {

    public static final String CONFIG_URI = "prerequisiteConfigUri";
    private static final Logger log = Logger.getLogger(CheckPrerequisiteStep.class);
    private Set<String> isDataReady;

    public CheckPrerequisiteStep(String workerURI) {
        super(workerURI);
        isDataReady = new HashSet<String>();
    }

    @Override
    public String invoke(CallingContext ctx) {
        PrerequisiteConfig config = getPrerequisiteConfig(ctx);
        log.info("config = " + JacksonUtil.jsonFromObject(config));
        DateTime cutoffTime = null;
        if (StringUtils.trimToNull(config.getCutoffTime()) == null) {
            log.warn("No timeout/cutoff time defined - Will wait forever");
        } else {
            cutoffTime = getDateTime(config.getCutoffTime());
        }
        String cutoffAction = getCutoffAction(config.getCutoffAction());

        while (true) {
            // check if reached cutoff time
            if ((cutoffTime != null) && !cutoffTime.isAfterNow()) {
                log.info("Reached cut off time, return " + cutoffAction);
                return cutoffAction;
            }
            // check last data point
            if (isDataReady(ctx, config)) {
                log.info("Data is ready, return next");
                return NEXT;
            }
            // data not ready, get next retry time
            DateTime now = DateTime.now();
            DateTime nextRetryTime = now.plusMillis(config.getRetryInMillis());
            if ((cutoffTime != null) && nextRetryTime.isAfter(cutoffTime)) {
                log.info("Next retry is after cutoff time, set it to " + cutoffTime);
                nextRetryTime = cutoffTime;
            }
            // sleep and retry later
            long sleepTime = nextRetryTime.getMillis() - now.getMillis();
            log.info("Sleep for " + sleepTime + " ms");
            try {
                if (sleepTime > 0) Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                log.error("Interrupted, check if data is ready", e);
            }
        }
    }

    private PrerequisiteConfig getPrerequisiteConfig(CallingContext ctx) {
        String configUri = Kernel.getDecision().getContextValue(ctx, getWorkerURI(), CONFIG_URI);
        log.info("getPrequisiteConfig, configURI: " + configUri);
        String content = Kernel.getDoc().getDoc(ctx, configUri);
        return JacksonUtil.objectFromJson(content, PrerequisiteConfig.class);
    }

    // check if all required data is ready
    private boolean isDataReady(CallingContext ctx, PrerequisiteConfig config) {
        outer: for (PrerequisiteConfig.RequiredData requiredData : config.getRequiredData()) {
            if (log.isDebugEnabled()) log.debug("Check requirement " + requiredData.toString());
            String uri = requiredData.getUri();
            int chevron = uri.indexOf('<');
            RaptureURI ruri = new RaptureURI((chevron > 0) ? uri.substring(0, chevron - 1) : uri);
            String keyFormat = requiredData.getKeyFormat();
            if (StringUtils.isEmpty(keyFormat)) keyFormat = "yyyyMMdd";
            String specificDate = StringUtils.trimToNull(requiredData.getSpecificDate());
            if ((specificDate != null) && specificDate.startsWith("$")) {
                specificDate = Kernel.getDecision().getContextValue(ctx, getWorkerURI(), specificDate.substring(1));
            }
                    
            // For series we look for a data point with the valid date
            if (ruri.getScheme().equals(Scheme.SERIES)) {
                if (specificDate != null) log.warn("specificDate not supported for series data");
                DateTime acceptableTime = getDateTime(DateTime.now().withTimeAtStartOfDay(), requiredData.getDateWithin(), requiredData.getTimeNoEarlierThan());

                if (!isDataReady.contains(uri)) {
                    if (!Kernel.getSeries().seriesExists(ctx, uri)) {
                        log.debug("Series not found "+uri);
                        return false;
                    }
                    SeriesPoint lastDataPoint = Kernel.getSeries().getLastPoint(ctx, uri);
                    DateTime lastDataPointTime = DateTime.parse(lastDataPoint.getColumn(), DateTimeFormat.forPattern(keyFormat));
                    log.debug("Last data point is at " + lastDataPointTime);
                    if (lastDataPointTime.isBefore(acceptableTime)) {
                        log.debug("data point is outside acceptable range");
                        return false;
                    } else {
                        log.debug("data point is valid");
                        isDataReady.add(uri);
                    }
                } else {
                    log.debug("data point already seen");
                }
            } else {
                // For types other than series we simply check for the existence of the blob/doc/sheet etc.
                
                DateTime start = (specificDate != null) ? DateTime.parse(specificDate, DateTimeFormat.forPattern(keyFormat)) : DateTime.now().withTimeAtStartOfDay();
                DateTime acceptableTime = getDateTime(start, requiredData.getDateWithin(), requiredData.getTimeNoEarlierThan());

                DateTime lastDataPointTime = (specificDate == null) ? new DateTime() : acceptableTime;
                SimpleDateFormat sdf = new SimpleDateFormat(keyFormat);

                while (!lastDataPointTime.isBefore(acceptableTime)) {
                    String datedUri = uri;
                    if (uri.contains("<DATE>")) datedUri = uri.replace("<DATE>", sdf.format(lastDataPointTime.toDate()));
                    if (isDataReady.contains(datedUri)) {
                        if (log.isDebugEnabled()) log.debug(datedUri.toString() + " already seen");
                        continue outer;
                    }

                    boolean uriExists = false;
                    // There is no generic exists method.
                    switch (ruri.getScheme()) {
                        case BLOB:
                            uriExists = Kernel.getBlob().blobExists(ctx, datedUri);
                            break;
                        case DOCUMENT:
                            uriExists = Kernel.getDoc().docExists(ctx, datedUri);
                            break;
                        default:
                            log.warn("Unexpected URI type : " + uri);
                            return false;
                    }
                    if (uriExists) {
                        if (log.isDebugEnabled()) log.debug(datedUri.toString() + " found");
                        isDataReady.add(uri);
                        continue outer;
                    }
                    // Go back one day
                    lastDataPointTime = lastDataPointTime.minusDays(1);
                }

                // Fail because we went outside acceptable time range
                // - we continue the outer for loop if successful
                log.debug("requirement not met");
                return false;
            }
        }
        // All found
        return true;
    }

    private String getCutoffAction(PrerequisiteConfig.CutoffAction cutoffAction) {
        if (cutoffAction == PrerequisiteConfig.CutoffAction.START) return NEXT;
        return QUIT;
    }

    /**
     * Converts time string to DateTime
     * 
     * @param timeStr
     *            hh:mm:ss timezone (eg 15:00:00 America/New_York)
     * @return DateTime
     */
    private DateTime getDateTime(String timeStr) {
        String[] parts = timeStr.split(" ");
        if (parts.length != 2) {
            throw RaptureExceptionFactory.create("Time format should be hh:mm:ss timezone (eg. 15:30:00 America/New_York)");
        }
        DateTimeZone timeZone = DateTimeZone.forID(parts[1]);
        return LocalTime.parse(parts[0]).toDateTimeToday(timeZone);
    }

    private DateTime getDateTime(DateTime dateTime, String dateWithin, String timeNoEarlierThan) {
        if (!StringUtils.isEmpty(timeNoEarlierThan)) {
            DateTime hours = getDateTime(timeNoEarlierThan);
            if (dateTime == null) dateTime = hours;
            else {
                dateTime = dateTime.withMillisOfDay(hours.getMillisOfDay()).withZone(hours.getZone());
            }
        }
        if (!StringUtils.isEmpty(dateWithin)) {
            int number = Integer.valueOf(dateWithin.substring(0, dateWithin.length() - 1));
            char dateUnit = dateWithin.charAt(dateWithin.length() - 1);
            switch (dateUnit) {
                case 'M':
                    dateTime = dateTime.minusMonths(number);
                    break;
                case 'W':
                    dateTime = dateTime.minusWeeks(number);
                    break;
                case 'D':
                    dateTime = dateTime.minusDays(number);
                    break;
                case 'H':
                    dateTime = dateTime.minusHours(number);
                    break;
                case 'B':
                    dateTime = USCalendar.minusBusinessDays(dateTime, number);
                    break;
                default:
                    throw RaptureExceptionFactory.create("Invalid date unit " + dateUnit);
            }
        }
        return dateTime;
    }

}
