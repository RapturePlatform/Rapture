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
package rapture.repo.meta.handler;

import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureNativeQueryResult;
import rapture.common.RaptureQueryResult;
import rapture.common.RaptureURI;
import rapture.common.TableQueryResult;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentAttribute;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentWithMeta;
import rapture.index.IndexHandler;
import rapture.index.IndexProducer;
import rapture.repo.DocumentAttributeHandler;
import rapture.repo.KeyStore;
import rapture.repo.RepoUtil;
import rapture.repo.StoreKeyVisitor;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.base.Optional;
import rapture.repo.file.FileDataStore;

/**
 * This class handle the bringing together of three key stores - for latest
 * version, for historical versions and for the meta data about versions
 *
 * @author amkimian
 */
public abstract class AbstractMetaHandler {
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "AbstractMetaHandler [metaStore=" + metaStore.getClass().getCanonicalName() + 
                ", documentStore=" + documentStore.getClass().getCanonicalName() + 
                ", attributeStore=" + attributeStore.getClass().getCanonicalName() + 
                ", indexHandler="  + indexHandler.getClass().getCanonicalName() + 
                ", documentAttributeHandler=" + documentAttributeHandler.getClass().getCanonicalName() + "]";
    }

    private static Logger log = Logger.getLogger(AbstractMetaHandler.class);
    public static final String LATEST = "l";
    protected KeyStore metaStore;
    protected KeyStore documentStore;
    private KeyStore attributeStore;
    protected Optional<IndexHandler> indexHandler = Optional.absent();
    private DocumentAttributeHandler documentAttributeHandler;

    public AbstractMetaHandler(KeyStore documentStore, KeyStore metaStore, KeyStore attributeStore) {
        this.documentStore = documentStore;
        this.metaStore = metaStore;
        this.attributeStore = attributeStore;
        documentAttributeHandler = new DocumentAttributeHandler();
    }

    public static String readableFileSize(long size) {
        if (size <= 0)
            return "0";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    /**
     * A long running task that (a) drops the index, (b) recreates it by going
     * through all latest documents and applying the indexproducer to them
     */

    public void rebuildIndex(final IndexProducer producer) {
        if (producer == null || !indexHandler.isPresent())
            return;
        log.info("Index rebuild, removing existing values");
        indexHandler.get().deleteTable();
        documentStore.visitKeysFromStart(null, new StoreKeyVisitor() {

            @Override
            public boolean visit(String key, String value) {
                log.info("Index rebuild, rebuilding " + key);
                DocumentMetadata mdLatest = getLatestMeta(key);
                indexHandler.get().addedRecord(key, value, mdLatest);
                return true;
            }

        });
    }

    public long addDocument(String docPath, String value, String user, String comment, IndexProducer producer) {
        // if docPath contains element #, which is the local file path,
        // remove element from docPath, as only FileDataStore uses that.
        // This is hacky: a cleaner solution should be in place after RAP-3587
        String docPathWithElement = docPath;
        int index = docPath.lastIndexOf("#");
        if(index > 0) {
            docPath = docPath.substring(0, index);
        }
        // Add a new version of a document
        // When we add a document, we need to do the following
        // (a) look for meta data about that document
        // (b) if it exists, determine the new version of this document
        // (c) record this document with that version (in the versionKeyStore,
        // metaKeyStore)
        // (d) record the updated meta information for the latest version
        // (e) record this document in the store (for latest)
        long versionToReturn = -1L;
        String latestKey = createLatestKey(docPath);
        // Do not do anything if the content is the same
        if (documentStore.matches(docPath, value)) {
            return versionToReturn;
        } else {
            DocumentMetadata newMetaData = createNewMetadata(user, comment, docPath);

            versionToReturn = newMetaData.getVersion();

            addLatestToMetaStore(latestKey, newMetaData);

            // Now store the document in the latest, and in the version repo
            // Pass on docPathWithElement to FileDataStore, so it could create sym links
            // A cleaner solution should be in place after RAP-3587
            if(documentStore instanceof FileDataStore) {
                documentStore.put(docPathWithElement, value);
            } else {
                documentStore.put(docPath, value);
            }

            if (isVersioned()) {
                addToVersionStore(docPath, value, newMetaData);
            }

            // If we have a producer, create an index record and put them into the
            // index
            if (producer != null) {
                // And put this record into the index store
                if (indexHandler.isPresent()) {
                    indexHandler.get().addedRecord(docPath, value, newMetaData);
                } else {
                    log.error("We created an index record and there is no where to store it!");
                }
            }
            return versionToReturn;
        }
    }

    protected void addLatestToMetaStore(String latestKey, DocumentMetadata newMetaData) {
        metaStore.put(latestKey, JacksonUtil.jsonFromObject(newMetaData));
    }

    protected abstract DocumentMetadata createNewMetadata(String user, String comment, String docPath);

    protected abstract boolean isVersioned();

    protected abstract void addToVersionStore(String docPath, String value, DocumentMetadata newMetaData);

    /**
     * Drop all repositories
     */
    public void dropAll() {
        documentStore.dropKeyStore();
        metaStore.dropKeyStore();
        attributeStore.dropKeyStore();
        if (indexHandler.isPresent()) {
            indexHandler.get().deleteTable();
        }
    }

    public long documentCount() {
        return documentStore.countKeys();
    }

    public String getDocument(String docPath) {
        return documentStore.get(docPath);
    }

    public List<String> getBatch(List<String> docPaths) {
        return documentStore.getBatch(docPaths);
    }

    public boolean contains(String docPath) {
        return documentStore.containsKey(docPath);
    }

    public void visitKeysFromStart(String startPoint, StoreKeyVisitor storeKeyVisitor) {
        documentStore.visitKeysFromStart(startPoint, storeKeyVisitor);
    }

    /**
     * Return the docPath of the symlink to the latest instance of this element
     *
     * @param docPath
     * @return
     */
    protected abstract String createLatestKey(String docPath);

    public boolean deleteLatest(String user, String docPath, IndexProducer producer) {
        // The removal needs to look at existing meta data. Basically remove the
        // current document, but update the meta information and write a deleted
        // record in the version history (but only if the document actually
        // exists)
        boolean retVal = documentStore.delete(docPath);
        updateMetaOnDelete(user, docPath);

        return retVal;
    }

    protected abstract void updateMetaOnDelete(String user, String docPath);

    public RaptureQueryResult runNativeQuery(String repoType, List<String> queryParams) {
        return documentStore.runNativeQuery(repoType, queryParams);
    }

    public RaptureNativeQueryResult runNativeQueryWithLimitAndBounds(String repoType, List<String> queryParams, int limit, int offset) {
        return documentStore.runNativeQueryWithLimitAndBounds(repoType, queryParams, limit, offset);
    }

    public void visitKeys(String prefix, StoreKeyVisitor storeKeyVisitor) {
        documentStore.visitKeys(prefix, storeKeyVisitor);
    }

    public DocumentMetadata getLatestMeta(String docPath) {
        return getMetaFromVersionKey(createLatestKey(docPath));
    }

    protected DocumentMetadata getMetaFromVersionKey(String versionDocPath) {
        String metaData = metaStore.get(versionDocPath);
        if (metaData != null) {
            return JacksonUtil.objectFromJson(metaData, DocumentMetadata.class);
        }
        return null;
    }

    public DocumentWithMeta getLatestDocAndMeta(String docPath) {
        DocumentWithMeta dm = new DocumentWithMeta();
        dm.setContent(documentStore.get(docPath));
        dm.setDisplayName(docPath);
        dm.setMetaData(getLatestMeta(docPath));
        return dm;
    }

    /**
     * Given a list of uris that can contain a mix of versioned URIs vs latest
     * URIs, return a list of DocumentWithMeta objects. We need to split up the
     * URIs based on whether they are versioned first and also preserve the
     * original position, so that the list we return will maintain the order in
     * which the URIs were requested. Furthermore, the actual keystores are
     * different based on whether a URI is versioned or not, so this is further
     * reason why the requested URIs need to be split up.
     *
     * @param uris - a mix of versioned and unversioned Rapture URIs
     * @return - a List of DocumentWithMeta objects that have the same order in
     * relation to the request
     */
    public abstract List<DocumentWithMeta> getDocAndMetas(List<RaptureURI> uris);

    /**
     * Insert values into the array, preserving the original order of the
     * request
     *
     * @param docsWithMeta - array to set values in with the correct order
     * @param positions    - the original ordering
     * @param contents     - the document contents
     * @param metaContents - the meta document contents
     */
    protected void constructDocumentWithMetaList(DocumentWithMeta[] docsWithMeta, List<RaptureURI> uris, List<Integer> positions, List<String> contents,
            List<String> metaContents) {
        if (contents.size() != metaContents.size()) {
            log.error("Batch getDocAndMetas failed due to different size of content vs metaContent");
            return;
        }
        for (int i = 0; i < contents.size(); i++) {
            DocumentWithMeta dwm = new DocumentWithMeta();
            String meta = metaContents.get(i);
            if (meta != null) {
                dwm.setMetaData(JacksonUtil.objectFromJson(meta, DocumentMetadata.class));
            }
            dwm.setContent(contents.get(i));
            dwm.setDisplayName(uris.get(positions.get(i)).getDocPath());
            docsWithMeta[positions.get(i)] = dwm;
        }
    }

    public List<RaptureFolderInfo> getChildren(String displayNamePart) {
        return documentStore.getSubKeys(displayNamePart);
    }

    public List<RaptureFolderInfo> removeChildren(String displayNamePart, Boolean force, IndexProducer producer) {
        List<RaptureFolderInfo> rfis = documentStore.removeSubKeys(displayNamePart, force);
        if (rfis != null) {
            List<String> keys = RepoUtil.extractNonFolderKeys(rfis);
            metaStore.delete(keys);
            if (indexHandler.isPresent()) {
                for (String key : keys) {
                    indexHandler.get().removeAll(key);
                }
            }
        } else {
            log.debug("No sub keys found for "+displayNamePart);
        }
        return rfis;
    }
    
    public List<String> getAllChildren(String area) {
        return documentStore.getAllSubKeys(area);
    }

    public void setDocAttribute(RaptureURI uri, DocumentAttribute attribute) {
        documentAttributeHandler.setDocAttribute(attributeStore, uri, attribute);
    }

    public DocumentAttribute getDocAttribute(RaptureURI uri) {
        return documentAttributeHandler.getDocAttribute(attributeStore, uri);
    }

    public List<DocumentAttribute> getDocAttributes(RaptureURI uri) {
        return documentAttributeHandler.getDocAttributes(attributeStore, uri);
    }

    public Boolean deleteDocAttribute(RaptureURI uri) {
        return documentAttributeHandler.deleteDocAttribute(attributeStore, uri);
    }

    public void setIndexProducer(IndexProducer indexProducer) {
        indexHandler = Optional.fromNullable(documentStore.createIndexHandler(indexProducer));
    }

    public TableQueryResult findIndex(String query) {
        return (indexHandler.isPresent()) ? indexHandler.get().query(query) : new TableQueryResult();
    }

    public Boolean validate() {
        return documentStore.validate();
    }

    public abstract Map<String, String> getStatus();

    protected DocumentMetadata createMetadataFromLatest(String user, String comment, String docPath, Boolean isDeleted, int defaultVersion,
            boolean incrementVersion) {
        Long now = System.currentTimeMillis();
        DocumentMetadata metadata = getLatestMeta(docPath);
        if (metadata != null) {
            if (incrementVersion) {
                metadata.setVersion(metadata.getVersion() + 1);
            }
        } else {
            metadata = new DocumentMetadata();
            metadata.setVersion(defaultVersion);
            metadata.setCreatedTimestamp(now);
        }
        metadata.setComment(comment);
        metadata.setUser(user);
        metadata.setModifiedTimestamp(now);
        metadata.setDeleted(isDeleted);

        return metadata;
    }

    public Optional<IndexHandler> getIndexHandler() {
        return indexHandler;
    }
}
