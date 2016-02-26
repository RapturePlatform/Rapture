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

import rapture.common.MessageFormat;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentWithMeta;
import rapture.dsl.dparse.AsOfTimeDirectiveParser;
import rapture.index.IndexProducer;
import rapture.repo.KeyStore;
import rapture.repo.Messages;
import rapture.repo.RepoUtil;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class handle the bringing together of three key stores - for latest
 * version, for historical versions and for the meta data about versions
 *
 * @author amkimian
 */
public class VersionedMetaHandler extends AbstractMetaHandler {
    private static final int INITIAL_VERSION = 1;
    private KeyStore versionStore;

    public VersionedMetaHandler(KeyStore latestStore, KeyStore versionStore, KeyStore metaStore, KeyStore attributeStore) {
        super(latestStore, metaStore, attributeStore);
        this.versionStore = versionStore;
    }

    public boolean addDocumentWithExpectedVersion(String key, String value, String user, String comment, int expectedVersion, IndexProducer producer) {
        // Check current version
        DocumentMetadata latestMeta = getLatestMeta(key);
        if (latestMeta != null && latestMeta.getVersion() != expectedVersion) {
            return false;
        }
        addDocument(key, value, user, comment, producer);
        return true;
    }

    @Override
    protected boolean isVersioned() {
        return true;
    }

    @Override
    protected void addToVersionStore(String docPath, String value, DocumentMetadata newMetaData) {
        String versionKey = createVersionKey(docPath, newMetaData.getVersion());
        String newMetadataJson = JacksonUtil.jsonFromObject(newMetaData);

        if (supportsVersionLookupByTime()) {
            // Make sure all the timestamps agree with each other.
            long timestamp = newMetaData.getModifiedTimestamp();
            metaStore.put(versionKey, timestamp, newMetadataJson);
            versionStore.put(versionKey, timestamp, value);
        }
        else {
            metaStore.put(versionKey, newMetadataJson);
            versionStore.put(versionKey, value);
        }
    }

    private String createVersionKey(String key, int version) {
        if (supportsVersionLookupByTime()) {
            // The version is indicated in a separate field in Cassandra
            return key;
        }
        else {
            return key + "?" + version;
        }
    }

    @Override
    protected String createLatestKey(String docPath) {
        if (supportsVersionLookupByTime()) {
            // The version is indicated in a separate field in Cassandra
            return docPath;
        }
        else {
            return docPath + "?latest";
        }
    }

    @Override
    public void dropAll() {
        super.dropAll();
        versionStore.dropKeyStore();
    }

    @Override
    protected void updateMetaOnDelete(String user, String docPath) {
        DocumentMetadata deletionMeta = createDeletionMeta(user, docPath);
        String deletionMetaJson = JacksonUtil.jsonFromObject(deletionMeta);
        String latestKey = createLatestKey(docPath);
        metaStore.put(latestKey, deletionMetaJson);
        String versionKey = createVersionKey(docPath, deletionMeta.getVersion());
        metaStore.put(versionKey, deletionMetaJson);

        if (indexHandler.isPresent()) {
            indexHandler.get().removeAll(docPath);
        }

    }

    @Override
    protected void addLatestToMetaStore(String latestKey, DocumentMetadata newMetaData) {
        // We don't need an extra copy with the ?latest key in timestamp repos.
        if (!supportsVersionLookupByTime()) {
            metaStore.put(latestKey, JacksonUtil.jsonFromObject(newMetaData));
        }
    }

    protected DocumentMetadata createDeletionMeta(String user, String key) {
        return createMetadataFromLatest(user, "Deleted", key, true, INITIAL_VERSION, true);
    }

    public DocumentWithMeta getDocumentWithMeta(String key, int version) {
        if (supportsVersionLookupByTime()) {
            return getDocumentWithMetaFromTimestampRepo(key, version);
        }
        else {
            return getDocumentWithMetaFromStandardRepo(key, version);
        }
    }

    protected DocumentWithMeta getDocumentWithMetaFromStandardRepo(String key, int version) {
        String versionKey = createVersionKey(key, version);
        String content = versionStore.get(versionKey);
        DocumentWithMeta md = new DocumentWithMeta();
        md.setDisplayName(key);
        md.setContent(content);
        md.setMetaData(getMetaFromVersionKey(versionKey));
        return md;
    }

    protected DocumentWithMeta getDocumentWithMetaFromTimestampRepo(String key, int version) {
        // Figure out what timestamp to look for by brute forcing it for the metadata lookup,
        // then use that timestamp to fetch the data directly for that version.
        DocumentMetadata metadata = getMetaFromTimestampRepo(key, version);
        String content = versionStore.get(key, metadata.getModifiedTimestamp());

        DocumentWithMeta md = new DocumentWithMeta();
        md.setDisplayName(key);
        md.setContent(content);
        md.setMetaData(metadata);
        return md;
    }

    protected DocumentMetadata getMetaFromTimestampRepo(String key, int version) {
        DocumentMetadata meta = getLatestMeta(key);

        while (meta != null && meta.getVersion() > version) {
            int numVersionsBack = meta.getVersion() - version;
            long maxPossibleTimestamp = meta.getModifiedTimestamp() - numVersionsBack;
            meta = getMetaAtTimestamp(key, maxPossibleTimestamp);
        }

        return meta;
    }

    public DocumentWithMeta getDocumentWithMeta(String key, long millisTimeStamp) {
        if (!supportsVersionLookupByTime()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR,
                    new MessageFormat(Messages.getString("MetaBasedRepo.NotSupported")));
        }

        String content = versionStore.get(key, millisTimeStamp);
        if (content == null) {
            return null;
        }

        DocumentWithMeta md = new DocumentWithMeta();
        md.setDisplayName(key);
        md.setContent(content);
        md.setMetaData(getMetaAtTimestamp(key, millisTimeStamp));
        return md;
    }

    public DocumentMetadata getMetaAtTimestamp(String key, long millisTimeStamp) {
        if (!supportsVersionLookupByTime()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR,
                    new MessageFormat(Messages.getString("MetaBasedRepo.NotSupported")));
        }

        String metaData = metaStore.get(key, millisTimeStamp);
        if (metaData != null) {
            return JacksonUtil.objectFromJson(metaData, DocumentMetadata.class);
        }
        return null;
    }

    public List<DocumentWithMeta> getDocAndMetas(List<RaptureURI> uris) {
        if (uris == null || uris.isEmpty()) {
            return new ArrayList<DocumentWithMeta>();
        }
        DocumentWithMeta[] ret = new DocumentWithMeta[uris.size()];
        Map<Integer, String> versionedKeys = new LinkedHashMap<Integer, String>();
        Map<Integer, String> latestKeys = new LinkedHashMap<Integer, String>();
        List<String> latestKeysWithVersion = new ArrayList<String>();

        int position = 0;
        for (RaptureURI uri : uris) {
            String docPath = uri.getDocPath();
            Integer version = null;

            if (uri.hasVersion()) {
                version = Integer.parseInt(uri.getVersion());
            }
            else if (uri.hasAsOfTime()) {
                version = getVersionNumberAsOfTime(docPath, uri.getAsOfTime());
            }

            if (version != null) {
                versionedKeys.put(position, createVersionKey(docPath, version));
            } else {
                latestKeys.put(position, docPath);
                latestKeysWithVersion.add(createLatestKey(docPath));
            }
            ++position;
        }

        List<String> versionedKeysList = new ArrayList<String>(versionedKeys.values());
        List<Integer> versionedPositionList = new ArrayList<Integer>(versionedKeys.keySet());
        List<String> versionedContents = versionStore.getBatch(versionedKeysList);
        List<String> versionedMeta = metaStore.getBatch(versionedKeysList);
        constructDocumentWithMetaList(ret, uris, versionedPositionList, versionedContents, versionedMeta);

        List<Integer> latestPositionList = new ArrayList<Integer>(latestKeys.keySet());
        List<String> latestContents = documentStore.getBatch(new ArrayList<String>(latestKeys.values()));

        List<String> latestMeta = metaStore.getBatch(latestKeysWithVersion);
        constructDocumentWithMetaList(ret, uris, latestPositionList, latestContents, latestMeta);

        return Arrays.asList(ret);
    }

    public DocumentMetadata getVersionMeta(String key, int version) {
        return getMetaFromVersionKey(createVersionKey(key, version));
    }

    public DocumentWithMeta revertDoc(String key, IndexProducer producer) {
        DocumentMetadata latest = getLatestMeta(key);
        DocumentWithMeta previous = getDocumentWithMeta(key, latest.getVersion() - 1);
        addDocument(key, previous.getContent(), previous.getMetaData().getUser(), previous.getMetaData().getComment() + " - REVERTED", producer);
        return getLatestDocAndMeta(key);
    }

    public List<RaptureFolderInfo> removeChildren(String displayNamePart, Boolean force, IndexProducer producer) {
        List<RaptureFolderInfo> rfis = super.removeChildren(displayNamePart, force, producer);
        if (rfis != null) versionStore.delete(RepoUtil.extractNonFolderKeys(rfis));
        return rfis;
    }

    public Map<String, String> getStatus() {
        Map<String, String> ret = new HashMap<String, String>();
        Object[] checks = { documentStore, "Latest", versionStore, "Version", metaStore, "Meta" };
        long totalSize = 0L;
        for (int i = 0; i < checks.length; i += 2) {
            KeyStore ks = (KeyStore) checks[i];
            if (ks != null) {
                long size = ks.getSize();
                if (size != -1L) {
                    ret.put(checks[i + 1].toString(), readableFileSize(size));
                    ret.put(checks[i + 1].toString() + "_Raw", "" + size);
                    totalSize += size;
                }
            }
        }
        if (totalSize != 0L) {
            ret.put("Total", readableFileSize(totalSize));
            ret.put("Total_Raw", "" + totalSize);
        }
        return ret;
    }

    // delete versions older than cutoffVersion (version# <= cutoffVersion)
    public boolean deleteOldVersions(String docUri, int cutoffVersion) {
        List<String> keys = new ArrayList<String>();
        for (int version = 1; version <= cutoffVersion; version++) {
            keys.add(docUri + "?" + version);
        }
        metaStore.delete(keys);
        versionStore.delete(keys);
        return true;
    }

    // delete versions older than cutoffMillis (lastModifiedTimestamp <= cutoffMillis)
    public boolean deleteOldVersions(String docUri, long cutoffMillis) {
        metaStore.deleteUpTo(docUri, cutoffMillis);
        versionStore.deleteUpTo(docUri, cutoffMillis);
        return true;
    }

    @Override
    protected DocumentMetadata createNewMetadata(String user, String comment, String docPath) {
        return createMetadataFromLatest(user, comment, docPath, false, INITIAL_VERSION, true);
    }

    public Integer getVersionNumberAsOfTime(String docUri, String asOfTime) {
        AsOfTimeDirectiveParser parser = new AsOfTimeDirectiveParser(asOfTime);
        long asOfTimeMillis = parser.getMillisTimestamp();

        DocumentMetadata metadata = getLatestMeta(docUri);
        if (metadata.getCreatedTimestamp() > asOfTimeMillis) {
            return null;
        }

        int version = metadata.getVersion();
        while (metadata != null && metadata.getModifiedTimestamp() > asOfTimeMillis) {
            version--;
            if (version < 1) {
                // Shouldn't happen because we checked for this above, but...
                return null;
            }

            metadata = getVersionMeta(docUri, version);
        }

        if (metadata == null) {
            String[] parameters = {docUri, asOfTime};
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_NOT_FOUND,
                    new MessageFormat(Messages.getString("NVersionedRepository.IncalculableVersion"), parameters));
        }

        return version;
    }

    public boolean supportsVersionLookupByTime() {
       return versionStore.supportsVersionLookupByTime();
    }
}
