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
package rapture.dp.invocable.purge;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.dp.AbstractInvocable;
import rapture.common.exception.ExceptionToString;
import rapture.common.impl.jackson.JsonContent;
import rapture.common.model.DocumentMetadata;
import rapture.common.apigen.purge.PurgeInfoReader;
import rapture.object.storage.ObjectStorage;
import rapture.object.storage.StorableIndexInfo;
import rapture.object.storage.StorablePurgeInfo;
import rapture.repo.RepoVisitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import com.google.common.base.Optional;

/**
 * @author bardhi
 * @since 11/14/14.
 */
public class PurgeOldStorablesStep extends AbstractInvocable {
    private static final Logger log = Logger.getLogger(PurgeOldStorablesStep.class);

    public PurgeOldStorablesStep(String workerURI) {
        super(workerURI);
    }

    @Override
    public String invoke(final CallingContext context) {
        PurgeInfoReader ps = new PurgeInfoReader();
        final long now = System.currentTimeMillis();
        List<Class<? extends StorablePurgeInfo>> all = ps.readAll();
        log.info(String.format("Found %s classes that have a TTL", all.size()));
        for (final Class<? extends StorablePurgeInfo> psClass : all) {
            StorablePurgeInfo purgeInfo = null;
            try {
                purgeInfo = psClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                log.error(ExceptionToString.format(e));
            }
            if (purgeInfo != null) {
                try {
                    purgeStorable(context, now, purgeInfo);
                } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    log.error(String.format("Error purging class %s: %s", purgeInfo.getStorableClass(), ExceptionToString.format(e)));
                }
            }

        }
        return "next";
    }

    private void purgeStorable(final CallingContext context, final long now, StorablePurgeInfo purgeInfo)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final long ttl = purgeInfo.getTTL();
        String printableTtl = getPrintableTtl(ttl);
        final Class sClass = purgeInfo.getStorableClass();
        String sdkName = purgeInfo.getSdkName();
        String classNameDetails;
        if (StringUtils.isEmpty(sdkName)) {
            classNameDetails = String.format("core-%s, ttl=%s", sClass.getSimpleName(), printableTtl);
        } else {
            classNameDetails = String.format("%s-%s, ttl=%s", sdkName, sClass.getSimpleName(), printableTtl);
        }

        final RaptureURI baseURI = purgeInfo.getBaseURI();
        final AtomicInteger countUntouched = new AtomicInteger();
        final AtomicInteger countDeleted = new AtomicInteger();
        final AtomicInteger countTotal = new AtomicInteger();
        final AtomicInteger currentCount = new AtomicInteger();
        final StorableIndexInfo indexInfoClass = purgeInfo.getIndexInfo();

        String storageClassName = purgeInfo.getStorableClass().getName() + "Storage";
        Class<?> storageClass = Class.forName(storageClassName);
        Method visitAllMethod = storageClass.getMethod("visitAll", RepoVisitor.class);
        visitAllMethod.invoke(null, new RepoVisitor() {
            @Override
            public boolean visit(String key, JsonContent content, boolean isFolder) {
                countTotal.incrementAndGet();
                return true;
            }
        });

        log.info(String.format("Processing %s, which has a total of %s entries...", classNameDetails, countTotal.get()));
        final int printSize = 100;
        visitAllMethod.invoke(null, new RepoVisitor() {
            @Override
            public boolean visit(String key, JsonContent content, boolean isFolder) {
                int count = currentCount.incrementAndGet();
                if (count % printSize == 0) {
                    log.info(String.format("Processed %s of %s...", count, countTotal.get()));
                }
                RaptureURI storageLocation = RaptureURI.builder(baseURI).docPath(key).build();
                if (log.isTraceEnabled()) {
                    log.trace(String.format("Checking out item at %s, class %s...", storageLocation, sClass.getName()));
                }
                Optional<DocumentMetadata> metaOptional = ObjectStorage.getLatestMeta(storageLocation, indexInfoClass);
                if (metaOptional.isPresent()) {
                    DocumentMetadata meta = metaOptional.get();
                    Long lastModified = meta.getModifiedTimestamp();
                    if (lastModified == null) {
                        Date writeTime = meta.getWriteTime();
                        if (writeTime != null) {
                            lastModified = writeTime.getTime();
                        }
                    }
                    if (lastModified != null) {
                        DateTime dt = new DateTime(lastModified);
                        String printableDate = formatDate(dt);
                        if (now - lastModified > ttl) {
                            if (log.isTraceEnabled()) {
                                log.trace(String.format("Will delete entry at %s, latModified=%s", storageLocation, printableDate));
                            }
                            ObjectStorage.delete(context.getUser(), storageLocation, indexInfoClass, "removed from purgeOldStorables");
                            countDeleted.incrementAndGet();
                        } else {
                            if (log.isTraceEnabled()) {
                                log.trace(String.format("Will not delete entry at %s, latModified=%s", storageLocation, printableDate));
                            }
                            countUntouched.incrementAndGet();
                        }
                    } else {
                        log.debug(String.format("No last modified found for entry at %s", storageLocation));
                    }
                } else {
                    log.debug(String.format("No metadata available for entry at %s", storageLocation));
                }
                return true;
            }
        });
        log.info(String.format("%s: %s entries purged, %s left unchanged", classNameDetails, countDeleted.get(), countUntouched.get()));
    }

    static PeriodFormatter PERIOD_FORMATTER = new PeriodFormatterBuilder()
            .printZeroNever()
            .appendDays()
            .appendSuffix(" day", " days")
            .appendSeparator(" ")
            .appendHours()
            .appendSeparator("h ")
            .appendMinutes()
            .appendSeparator("m ")
            .appendSecondsWithOptionalMillis()
            .appendSuffix("s ")
            .toFormatter();

    private String getPrintableTtl(long ttl) {
        return PERIOD_FORMATTER.print(new Period(ttl).normalizedStandard(PeriodType.dayTime()));
    }

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss");

    private String formatDate(DateTime dt) {
        return DATE_TIME_FORMATTER.print(dt);
    }

}
