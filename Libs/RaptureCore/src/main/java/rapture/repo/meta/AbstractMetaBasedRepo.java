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
package rapture.repo.meta;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.base.Optional;

import rapture.common.LockHandle;
import rapture.common.RaptureDNCursor;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureNativeQueryResult;
import rapture.common.RaptureQueryResult;
import rapture.common.RaptureURI;
import rapture.common.TableQueryResult;
import rapture.common.exception.RaptNotSupportedException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JsonContent;
import rapture.common.model.DocumentAttribute;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentWithMeta;
import rapture.dsl.dparse.BaseDirective;
import rapture.dsl.dparse.VersionDirective;
import rapture.index.IndexHandler;
import rapture.index.IndexProducer;
import rapture.kernel.Kernel;
import rapture.lock.ILockingHandler;
import rapture.lock.dummy.DummyLockHandler;
import rapture.repo.BaseSimpleRepo;
import rapture.repo.KeyStore;
import rapture.repo.Messages;
import rapture.repo.RepoFolderVisitor;
import rapture.repo.RepoLockHandler;
import rapture.repo.RepoVisitor;
import rapture.repo.Repository;
import rapture.repo.StoreKeyVisitor;
import rapture.repo.meta.handler.AbstractMetaHandler;

/**
 * @author bardhi
 * @since 11/11/14.
 */
public abstract class AbstractMetaBasedRepo<MH extends AbstractMetaHandler> extends BaseSimpleRepo implements Repository {
    private static final Logger log = Logger.getLogger(AbstractMetaBasedRepo.class);
    protected MH metaHandler;
    protected String storeId;
    protected IndexProducer producer;

    public AbstractMetaBasedRepo(KeyStore store, ILockingHandler lockHandler) {
        boolean lockIsDummy = false;
        if (lockHandler instanceof DummyLockHandler) {
            lockIsDummy = true;
        }
        this.storeId = store.getStoreId();
        this.repoLockHandler = new RepoLockHandler(lockIsDummy, lockHandler, storeId);
        store.setRepoLockHandler(repoLockHandler);
    }

    public MH getMetaHandler() {
        return metaHandler;
    }

    @Override
    public void setIndexProducer(IndexProducer producer) {
        if (this.producer != null) {
            System.out.println(this.producer.toString());
            System.out.println(producer.toString());
            if (this.producer.equals(producer)) {
                if (this.producer.toString().equals(producer.toString())) {
                    log.debug("Producer is already set to " + this.producer.toString());
                    return;
                }
            }
            log.info("Updating Producer from " + this.producer.toString() + " to " + producer.toString());
        }
        this.producer = producer;
        metaHandler.setIndexProducer(producer);
        rebuild();
    }

    @Override
    public boolean hasIndexProducer() {
        return producer != null;
    }

    public void rebuild() {
        metaHandler.rebuildIndex(producer);
    }

    @Override
    public DocumentWithMeta addDocument(String docPath, String value, String user, String comment, boolean mustBeNew) {
        Long startTime = System.currentTimeMillis();

        String lockHolder = repoLockHandler.generateLockHolder();
        DocumentWithMeta ret = null;
        LockHandle lockHandle = repoLockHandler.acquireLock(lockHolder);
        if (lockHandle != null) {
            try {
                if (log.isTraceEnabled()) {
                    log.trace("addDocument docPath=" + docPath + " value=" + value + " user=" + user + " comment=" + comment + " producer=" + producer);
                }
                ret = metaHandler.addDocument(docPath, value, user, comment, producer);
                Kernel.getMetricsService().recordTimeDifference("repo.doc.single.wrote", System.currentTimeMillis() - startTime);
            } finally {
                repoLockHandler.releaseLock(lockHolder, lockHandle);
            }
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("NVersionedRepo.nolockWrite")); //$NON-NLS-1$
        }
        return ret;
    }

    @Override
    public TableQueryResult findIndex(String query) {
        Long startTime = System.currentTimeMillis();
        TableQueryResult result = metaHandler.findIndex(query);
        Kernel.getMetricsService().recordTimeDifference("repo.doc.query.ran", System.currentTimeMillis() - startTime);

        return result;
    }

    @Override
    public void addDocuments(List<String> dispNames, String content, String user, String comment) {
        for (String displayName : dispNames) {
            addDocument(displayName, content, user, comment, false);
        }
    }

    @Override
    public void addToStage(String stage, String docPath, String value, boolean mustBeNew) {
        addDocument(docPath, value, null, null, mustBeNew);
    }

    @Override
    public long countDocuments() throws RaptNotSupportedException {
        return metaHandler.documentCount();
    }

    @Override
    public void drop() {
        String lockHolder = repoLockHandler.generateLockHolder();
        LockHandle lockHandle = repoLockHandler.acquireLock(lockHolder);
        if (lockHandle != null) {
            try {
                metaHandler.dropAll();
                // Drop other stores as well
            } finally {
                repoLockHandler.releaseLock(lockHolder, lockHandle);

            }
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("MetaBasedRepo.nolockWrite")); //$NON-NLS-1$
        }
    }

    @Override
    public String getDocument(String docPath) {
        Long startTime = System.currentTimeMillis();
        String retVal = metaHandler.getDocument(docPath);
        Kernel.getMetricsService().recordTimeDifference("repo.doc.single.read", System.currentTimeMillis() - startTime);

        return retVal;
    }

    @Override
    public String getDocument(String docPath, BaseDirective directive) {
        if (directive == null) {
            return getDocument(docPath);
        } else if (isVersioned() && directive instanceof VersionDirective) {
            return getVersionedDocumentWithMeta(docPath, (VersionDirective) directive).getContent();
        }
        return null;
    }

    protected abstract DocumentWithMeta getVersionedDocumentWithMeta(String docPath, VersionDirective directive);

    @Override
    public DocumentWithMeta getDocAndMeta(String docPath, BaseDirective directive) {
        if (isVersioned() && directive != null && directive instanceof VersionDirective) {
            return getVersionedDocumentWithMeta(docPath, (VersionDirective) directive);
        } else {
            return metaHandler.getLatestDocAndMeta(docPath);
        }
    }

    @Override
    public List<DocumentWithMeta> getDocAndMetas(List<RaptureURI> uris) {
        return metaHandler.getDocAndMetas(uris);
    }

    @Override
    public DocumentMetadata getMeta(String docPath, BaseDirective directive) {
        if (isVersioned() && directive != null && directive instanceof VersionDirective) {
            return getVersionedDocumentWithMeta(docPath, (VersionDirective) directive).getMetaData();
        } else {
            return metaHandler.getLatestMeta(docPath);
        }
    }

    @Override
    public List<String> getDocuments(List<String> docPaths) {
        Long startTime = System.currentTimeMillis();
        List<String> retVal = metaHandler.getBatch(docPaths);
        String batchName;
        if (docPaths.size() < 20) {
            batchName = "0-20";
        } else if (docPaths.size() < 50) {
            batchName = "20-50";
        } else if (docPaths.size() < 100) {
            batchName = "50-100";
        } else {
            batchName = "100_plus";
        }
        Kernel.getMetricsService()
                .recordTimeDifference(String.format("repo.doc.batch_%s.read", batchName), (System.currentTimeMillis() - startTime) / docPaths.size());

        return retVal;
    }

    @Override
    public boolean[] getExistence(List<String> displays) {
        boolean[] ret = new boolean[displays.size()];
        for (int i = 0; i < displays.size(); i++) {
            ret[i] = metaHandler.contains(displays.get(i));
        }
        return ret;
    }

    @Override
    public RaptureDNCursor getNextDNCursor(final RaptureDNCursor cursor, final int count) {
        if (cursor.isFinished()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("MetaBasedRepo.finishCursor")); //$NON-NLS-1$
        }
        String startPoint = cursor.getContinueContext();
        final List<String> docPaths = new ArrayList<>();
        cursor.setFinished(true);

        metaHandler.visitKeysFromStart(startPoint, new StoreKeyVisitor() {

            @Override
            public boolean visit(String key, String value) {
                docPaths.add(key);
                if (docPaths.size() == count) {
                    cursor.setContinueContext(key);
                    cursor.setFinished(false);
                    return false;
                }
                return true;
            }

        });
        cursor.setDisplayNames(docPaths);
        return cursor;
    }

    @Override
    public boolean removeDocument(String docPath, String user, String comment) {
        String lockHolder = repoLockHandler.generateLockHolder();
        boolean removed = false;
        LockHandle lockHandle = repoLockHandler.acquireLock(lockHolder);

        if (lockHandle != null) {
            try {
                removed = metaHandler.deleteLatest(user, docPath, producer);
            } finally {
                repoLockHandler.releaseLock(lockHolder, lockHandle);
            }
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("NVersionedRepo.nolockWrite")); //$NON-NLS-1$
        }
        return removed;
    }

    @Override
    public boolean removeFromStage(String stage, String docPath) {
        return removeDocument(docPath, "", Messages.getString("NVersionedRepo.removeStage")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public RaptureQueryResult runNativeQuery(String repoType, List<String> queryParams) {
        return metaHandler.runNativeQuery(repoType, queryParams);
    }

    @Override
    public RaptureNativeQueryResult runNativeQueryWithLimitAndBounds(String repoType, List<String> queryParams, int limit, int offset) {
        return metaHandler.runNativeQueryWithLimitAndBounds(repoType, queryParams, limit, offset);
    }

    @Override
    public void visitAll(String prefix, BaseDirective directive, final RepoVisitor visitor) {
        // Simply loop through all of the keys and return them
        metaHandler.visitKeys(prefix, new StoreKeyVisitor() {

            @Override
            public boolean visit(String key, String value) {
                return visitor.visit(key, new JsonContent(value), false);
            }

        });
    }

    @Override
    public void visitFolder(final String folder, BaseDirective directive, RepoVisitor visitor) {
        // Need to visit the keys given this prefix, but construct a Set
        // containing
        // the unique points at this point in the hiearchy
        // So if we visit x/y, we need to only return x/y/a, x/y/b, and not
        // x/y/b/d

        log.info(String.format(Messages.getString("NVersionedRepo.visitRep"), folder)); //$NON-NLS-1$

        final Map<String, JsonContent> values = new HashMap<>();

        metaHandler.visitKeys(folder, new StoreKeyVisitor() {

            @Override
            public boolean visit(String key, String value) {
                String partialKey = folder.isEmpty() ? key : key.substring(folder.length() + 1);
                int indexPoint = partialKey.indexOf('/');
                if (indexPoint == -1) {
                    values.put(partialKey, new JsonContent(value));
                } else {
                    String innerKey = partialKey.substring(0, indexPoint);
                    if (!values.containsKey(innerKey)) {
                        values.put(innerKey, null);
                    }
                }
                return true;
            }

        });

        for (Map.Entry<String, JsonContent> entry : values.entrySet()) {
            visitor.visit(entry.getKey(), entry.getValue(), entry.getValue() == null);
        }
    }

    @Override
    public void visitFolders(final String folderPrefix, BaseDirective directive, final RepoFolderVisitor visitor) {
        // Here we need to go through all of the keys and see if there is some
        // part of the path that begins with this prefix
        // so if the prefix is CUSIP
        // CUSIP/X
        // CUSIP/Y
        // all return yes
        // while CUSOP/X
        // does not match
        // CUSIP/X/Y should emit CUSIP/X
        // It's up to the visitor to emit into a set to avoid duplicates

        metaHandler.visitKeys(folderPrefix, new StoreKeyVisitor() {

            @Override
            public boolean visit(String key, String value) {
                // Remove prefix from key
                // Find all of the unique folders in this key
                String[] parts = key.split("/"); //$NON-NLS-1$
                StringBuilder f = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    f.append(parts[i]);
                    visitor.folder(f.toString());
                    f.append("/"); //$NON-NLS-1$
                }
                return true;
            }

        });
    }

    @Override
    public List<RaptureFolderInfo> getChildren(String displayNamePart) {
        return metaHandler.getChildren(displayNamePart);
    }

    @Override
    public List<RaptureFolderInfo> removeChildren(String displayNamePart, Boolean force) {
        return metaHandler.removeChildren(displayNamePart, force, producer);
    }

    @Override
    public boolean hasMetaContent() {
        return true;
    }

    @Override
    public List<String> getAllChildren(String area) {
        return metaHandler.getAllChildren(area);
    }

    @Override
    public void setDocAttribute(RaptureURI uri, DocumentAttribute attribute) {
        metaHandler.setDocAttribute(uri, attribute);
    }

    @Override
    public DocumentAttribute getDocAttribute(RaptureURI uri) {
        return metaHandler.getDocAttribute(uri);
    }

    @Override
    public List<DocumentAttribute> getDocAttributes(RaptureURI uri) {
        return metaHandler.getDocAttributes(uri);
    }

    @Override
    public Boolean deleteDocAttribute(RaptureURI uri) {
        return metaHandler.deleteDocAttribute(uri);
    }

    @Override
    public Boolean validate() {
        return metaHandler.validate();
    }

    @Override
    public Map<String, String> getStatus() {
        return metaHandler.getStatus();
    }

    @Override
    public String toString() {
        return super.toString() + "{" +
                "storeId='" + storeId + '\'' +
                '}';
    }

    @Override
    public Optional<IndexHandler> getIndexHandler() {
        return metaHandler.getIndexHandler();
    }
    
    @Override
	public DocumentWithMeta addTagToDocument(String user, String docPath, String tagUri,
			String value) {
    	return metaHandler.addDocumentTag(user, docPath, tagUri, value);
	}
    @Override
    public DocumentWithMeta addTagsToDocument(String user, String docPath,
			Map<String, String> tagMap) {
    	return metaHandler.addDocumentTags(user, docPath, tagMap);
    }

    @Override
	public DocumentWithMeta removeTagFromDocument(String user, String docPath,
			String tagUri) {
    	return metaHandler.removeDocumentTag(user, docPath, tagUri);
    }

    @Override
	public DocumentWithMeta removeTagsFromDocument(String user, String docPath,
			List<String> tags) {
    	return metaHandler.removeDocumentTags(user, docPath, tags);
    }
}
