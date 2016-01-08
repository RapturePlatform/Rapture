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
package rapture.dp.invocable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import rapture.common.CallingContext;
import rapture.common.dp.AbstractInvocable;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.Kernel;
import rapture.repo.NVersionedRepo;
import rapture.repo.Repository;

public class ArchiveRepoStep extends AbstractInvocable {

    private static Logger log = Logger.getLogger(ArchiveRepoStep.class);

    private static final String DOC_REPO_NAMES = "DOC_REPO_NAMES";
    private static final String SYS_REPO_NAMES = "SYS_REPO_NAMES";

    private static final String VERSION_LIMIT = "VERSION_LIMIT";
    private static final String TIME_LIMIT = "TIME_LIMIT";
    private static final String ENSURE_VERSION_LIMIT = "ENSURE_VERSION_LIMIT";

    public ArchiveRepoStep(String workerURI) {
        super(workerURI);
    }

    @Override
    public String invoke(CallingContext context) {

        List<String> docRepoNames = readListFromContext(context, DOC_REPO_NAMES);
        List<String> sysRepoNames = readListFromContext(context, SYS_REPO_NAMES);

        String versionLimitStr = Kernel.getDecision().getContextValue(context, getWorkerURI(), VERSION_LIMIT);
        String timeLimitStr = Kernel.getDecision().getContextValue(context, getWorkerURI(), TIME_LIMIT);
        if (StringUtils.isEmpty(versionLimitStr) && StringUtils.isEmpty(timeLimitStr)) {
            log.warn("No version limit or time limit is specified, nothing to archive");
            return NEXT;
        }

        int versionLimit = StringUtils.isEmpty(versionLimitStr) ? Integer.MAX_VALUE : Integer.parseInt(versionLimitStr);
        long timeLimit = 0;
        if (!StringUtils.isEmpty(timeLimitStr)) {
            DateTime dateTime = DateTime.now();
            int number = Integer.valueOf(timeLimitStr.substring(0, timeLimitStr.length() - 1));
            char dateUnit = timeLimitStr.charAt(timeLimitStr.length() - 1);
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
                default:
                    throw RaptureExceptionFactory.create("Invalid date unit " + dateUnit);
            }
            timeLimit = dateTime.getMillis();
        }

        boolean ensureVersionLimit = Boolean.parseBoolean(Kernel.getDecision().getContextValue(context, getWorkerURI(), ENSURE_VERSION_LIMIT));

        log.info(String.format("Configured to archive %s doc repos and %s system repos.", docRepoNames.size(), sysRepoNames.size()));

        for (String repoName : docRepoNames) {
            log.info(String.format("About to archive doc repo %s", repoName));
            Kernel.getDoc().archiveRepoDocs(context, repoName, versionLimit, timeLimit, ensureVersionLimit);
        }

        for (String repoName : sysRepoNames) {
            Repository repo = Kernel.INSTANCE.getRepo(repoName);
            if (repo == null) {
                log.info(String.format("Repo not found for name '%s'", repoName));
            } else {
                if (repo.isVersioned() && repo instanceof NVersionedRepo) {
                    log.info(String.format("About to archive sys repo %s", repoName));
                    ((NVersionedRepo) repo).archiveRepoVersions(repoName, versionLimit, timeLimit, ensureVersionLimit, context.getUser());
                } else {
                    log.info(String.format("Sys repo %s is not of type NVersionedRepo, instead it is of type %s, skipping...", repoName,
                            repo.getClass().getName()));
                }
            }
        }

        return NEXT;
    }

    protected List<String> readListFromContext(CallingContext context, String varName) {
        List<String> list;
        String json = Kernel.getDecision().getContextValue(context, getWorkerURI(), varName);
        if (!StringUtils.isEmpty(json)) {
            list = JacksonUtil.objectFromJson(json, LinkedList.class);
        } else {
            list = Collections.emptyList();
        }
        return list;
    }
}
