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
package rapture.series.children.cleanup;

import rapture.common.LockHandle;
import rapture.common.exception.ExceptionToString;
import rapture.repo.RepoLockHandler;
import rapture.series.children.PathConstants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import rapture.config.ConfigLoader;

/**
 * @author bardhi
 * @since 5/11/15.
 */
public class DefaultFolderCleanupService extends FolderCleanupService {

    private static final Logger log = Logger.getLogger(DefaultFolderCleanupService.class);

    private Map<String, CleanupInfo> repoIdToInfo;
    private ScheduledExecutorService executor;

    DefaultFolderCleanupService() {
        repoIdToInfo = new HashMap<>();
        executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("FolderCleanup").setDaemon(true).build());
        Integer initialDelay = ConfigLoader.getConf().folderCleanup.initialDelay;
        Integer delay = ConfigLoader.getConf().folderCleanup.delay;
        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    runCleanup();
                } catch (Exception e) {
                    log.error(ExceptionToString.format(e));
                }
            }
        }, initialDelay, delay, TimeUnit.MILLISECONDS);

    }

    @Override
    public void register(final CleanupInfo cleanupInfo) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                repoIdToInfo.put(cleanupInfo.uniqueId, cleanupInfo);

            }
        });
    }

    @Override
    public void addForReview(final String uniqueId, final String folderPath) {
        if (!StringUtils.isBlank(StringUtils.strip(folderPath, PathConstants.PATH_SEPARATOR))) {
            if (log.isTraceEnabled()) {
                log.trace(String.format("Adding folder [%s] in repo [%s]", folderPath, uniqueId));
            }
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    final CleanupInfo cleanupInfo = repoIdToInfo.get(uniqueId);
                    if (cleanupInfo != null) {
                        Set<String> foldersToReview = cleanupInfo.foldersForReview;
                        if (foldersToReview == null) {
                            foldersToReview = new HashSet<String>();
                        }
                        foldersToReview.add(folderPath);
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Trying to clean up folder [%s] in unregistered repo [%s]", folderPath, uniqueId));
                        }
                    }
                }
            });
        }
    }

    @Override
    public void unregister(final String uniqueId) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                repoIdToInfo.remove(uniqueId);
            }
        });
    }

    private void runCleanup() {
        if (log.isTraceEnabled()) {
            log.info(String.format("repos are %s", repoIdToInfo.keySet()));
        }
        for (CleanupInfo cleanupInfo : repoIdToInfo.values()) {
            runCleanup(cleanupInfo.repoDescription, cleanupInfo.cleanupFunction, cleanupInfo.isEmptyPredicate, cleanupInfo.repoLockHandler,
                    cleanupInfo.foldersForReview);
        }
    }

    private void runCleanup(String repoDescription, Function<String, Boolean> cleanupFunction, Predicate<String> isEmptyPredicate,
            RepoLockHandler repoLockHandler, Set<String> foldersForReview) {
        Iterator<String> iterator = foldersForReview.iterator();
        while (iterator.hasNext()) {
            String folderPath = iterator.next();
            iterator.remove();
            if (isEmptyPredicate.apply(folderPath)) {
                deleteFolderIfEmpty(repoDescription, cleanupFunction, isEmptyPredicate, repoLockHandler, folderPath);
            } else {
                ignoreFolder(repoDescription, folderPath);
            }
        }
    }

    protected void ignoreFolder(String repoDescription, String folderPath) {
        log.info(String.format("Folder [%s] [%s] is NOT empty, NOT cleaning up", repoDescription, folderPath));
    }

    protected void deleteFolderIfEmpty(String repoDescription, Function<String, Boolean> cleanupFunction, Predicate<String> isEmptyPredicate,
            RepoLockHandler repoLockHandler, String folderPath) {
        log.info(String.format("Folder [%s] [%s] is empty, cleaning up", repoDescription, folderPath));
        String lockHolder = null;
        if (repoLockHandler != null) {
            lockHolder = repoLockHandler.generateLockHolder();
        }
        LockHandle lockHandle = null;
        try {
            if (repoLockHandler != null) {
                lockHandle = repoLockHandler.acquireLock(lockHolder, folderPath);
            }
            if (isEmptyPredicate.apply(folderPath)) {
                cleanupFunction.apply(folderPath);
            }
        } catch (Exception e) {
            log.error(String.format("Error cleaning up [%s] [%s]: %s", repoDescription, folderPath, ExceptionToString.format(e)));
        } finally {
            if (repoLockHandler != null && lockHandle != null) {
                repoLockHandler.releaseLock(lockHolder, lockHandle, folderPath);
            }
        }
    }

}
