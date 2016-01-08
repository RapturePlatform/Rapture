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
package rapture.repo;

import rapture.common.LockHandle;
import rapture.common.MessageFormat;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JsonContent;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentWithMeta;
import rapture.dsl.dparse.AbsoluteVersion;
import rapture.dsl.dparse.AsOfTimeDirective;
import rapture.dsl.dparse.BaseDirective;
import rapture.lock.ILockingHandler;
import rapture.repo.meta.AbstractMetaBasedRepo;
import rapture.repo.meta.handler.VersionedMetaHandler;

import java.net.HttpURLConnection;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * A repo that manages a "latest" version and a history of versions
 * <p/>
 * Also stores meta data about the current and historical versions
 *
 * @author amkimian
 */
public class NVersionedRepo extends AbstractMetaBasedRepo<VersionedMetaHandler> {

    private static final Logger log = Logger.getLogger(NVersionedRepo.class);

    public NVersionedRepo(Map<String, String> config, KeyStore store, KeyStore versionKeyStore, KeyStore metaKeyStore, KeyStore attributeStore,
            ILockingHandler lockHandler) {
        super(store, lockHandler);
        metaHandler = new VersionedMetaHandler(store, versionKeyStore, metaKeyStore, attributeStore);
    }

    @Override
    public DocumentWithMeta revertDoc(String disp, BaseDirective directive) {
        return metaHandler.revertDoc(disp, producer);
    }

    @Override
    public boolean isVersioned() {
        return true;
    }

    /**
     * Archive old versions of the entire repo, including all docs.
     *
     * @param authority
     * @param versionLimit
     * @param timeLimit
     * @param ensureVersionLimit
     * @param user
     * @return
     */
    public boolean archiveRepoVersions(final String authority, final int versionLimit, final long timeLimit, final boolean ensureVersionLimit,
            final String user) {
        log.info("Archive repo " + authority);
        RepoVisitor visitor = new RepoVisitor() {
            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                archiveDocumentVersions(name, versionLimit, timeLimit, ensureVersionLimit, user);
                return true;
            }
        };
        visitAll("", null, visitor);
        return true;
    }

    /**
     * Archives old versions
     *
     * @param docPath            doc path
     * @param versionLimit       number of versions to retain
     * @param timeLimit          commits older than timeLimit will be archived
     * @param ensureVersionLimit ensure number of versions to retain even if commit is older
     *                           than timeLimit
     * @param user
     * @return
     */
    public boolean archiveDocumentVersions(String docPath, int versionLimit, long timeLimit, boolean ensureVersionLimit, String user) {
        if (versionLimit <= 0) {
            log.error("versionLimit should > 0");
            return false;
        }
        try {
            int latestVersion = metaHandler.getLatestMeta(docPath).getVersion();
            int cutoffVersion = getCutoffVersion(docPath, versionLimit, timeLimit, ensureVersionLimit);
            log.info(String.format("Archive doc %s: latestVersion=%d, cutoffVersion=%d", docPath, latestVersion, cutoffVersion));
            // no version to archive
            if (cutoffVersion < 1) {
                return true;
            }
            // cutoff version is the latest version, delete the document
            if (cutoffVersion == latestVersion) {
                return removeDocument(docPath, user, "Archive old versions");
            }
            return metaHandler.deleteOldVersions(docPath, cutoffVersion);

        } catch (Exception e) {
            log.error("Failed to archive " + docPath, e);
            return false;
        }
    }

    private int getCutoffVersion(String docPath, int versionLimit, long timeLimit, boolean ensureVersionLimit) {
        int versionsRemaining = versionLimit;
        int cutoffVersion = -1;

        DocumentMetadata metadata = metaHandler.getLatestMeta(docPath);
        while (metadata != null) {
            Long time = metadata.getModifiedTimestamp();
            if (time == null && metadata.getWriteTime() != null) {
                time = metadata.getWriteTime().getTime();
            }
            if (time == null || (time < timeLimit && !ensureVersionLimit)) {
                // reached the cut off version
                cutoffVersion = metadata.getVersion();
                break;
            } else {
                versionsRemaining--;
                if (versionsRemaining <= 0) {
                    cutoffVersion = metadata.getVersion() - 1;
                    break;
                } else {
                    // move on to previous version
                    metadata = metaHandler.getVersionMeta(docPath, metadata.getVersion() - 1);
                }
            }
        }
        return cutoffVersion;
    }

    @Override
    protected DocumentWithMeta getVersionedDocumentWithMeta(String docPath, AbsoluteVersion directive) {
        return metaHandler.getDocumentWithMeta(docPath, directive.getVersion());
    }

    @Override
    public boolean addDocumentWithVersion(String docPath, String content, String user, String comment, boolean mustBeNew, int expectedVersion) {
        String lockHolder = repoLockHandler.generateLockHolder();
        boolean ret = false;
        LockHandle lockHandle = repoLockHandler.acquireLock(lockHolder);
        if (lockHandle != null) {
            try {
                ret = metaHandler.addDocumentWithExpectedVersion(docPath, content, user, comment, expectedVersion, producer);
            } finally {
                repoLockHandler.releaseLock(lockHolder, lockHandle);
            }
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("NVersionedRepo.nolockWrite")); //$NON-NLS-1$
        }
        return ret;
    }

    protected AbsoluteVersion getAbsoluteVersionFromAsOfTimeDirective(String docPath, AsOfTimeDirective directive) {
        Integer version = metaHandler.getVersionNumberAsOfTime(docPath, directive.getAsOfTime());
        if (version == null) {
            // Document didn't exist at that time.
            String[] parameters = {docPath, directive.getAsOfTime()};
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_NOT_FOUND,
                    new MessageFormat(Messages.getString("InvalidAsOfTime"), parameters));
        }

        AbsoluteVersion newDirective = new AbsoluteVersion();
        newDirective.setVersion(version);
        return newDirective;
    }

    @Override
    public DocumentWithMeta getDocAndMeta(String docPath, BaseDirective directive) {
        if (directive instanceof AsOfTimeDirective) {
            directive = getAbsoluteVersionFromAsOfTimeDirective(docPath, (AsOfTimeDirective) directive);
        }

        return super.getDocAndMeta(docPath, directive);
    }

    @Override
    public DocumentMetadata getMeta(String docPath, BaseDirective directive) {
        if (directive instanceof AsOfTimeDirective) {
            directive = getAbsoluteVersionFromAsOfTimeDirective(docPath, (AsOfTimeDirective) directive);
        }

        return super.getMeta(docPath, directive);
    }

    @Override
    public String getDocument(String docPath, BaseDirective directive) {
        if (directive instanceof AsOfTimeDirective) {
            directive = getAbsoluteVersionFromAsOfTimeDirective(docPath, (AsOfTimeDirective) directive);
        }

        return super.getDocument(docPath, directive);
    }
}
