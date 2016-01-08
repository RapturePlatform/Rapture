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
package rapture.repo.meta.handler;

import rapture.common.MessageFormat;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentWithMeta;
import rapture.index.IndexProducer;
import rapture.repo.KeyStore;
import rapture.repo.Messages;
import rapture.repo.RepoUtil;

import java.net.HttpURLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    public static final String AS_OF_TIME_FORMAT = "yyyyMMdd'T'HHmmss";
    public static final String AS_OF_TIME_FORMAT_TZ = AS_OF_TIME_FORMAT + "Z";

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
        metaStore.put(versionKey, newMetadataJson);
        versionStore.put(versionKey, value);
    }

    private String createVersionKey(String key, int version) {
        return key + "?" + version;
    }

    @Override
    protected String createLatestKey(String docPath) {
        return docPath + "?latest";
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

    protected DocumentMetadata createDeletionMeta(String user, String key) {
        return createMetadataFromLatest(user, "Deleted", key, true, INITIAL_VERSION, true);
    }

    public DocumentWithMeta getDocumentWithMeta(String key, int version) {
        String versionKey = createVersionKey(key, version);
        String content = versionStore.get(versionKey);
        DocumentWithMeta md = new DocumentWithMeta();
        md.setDisplayName(key);
        md.setContent(content);
        md.setMetaData(getMetaFromVersionKey(versionKey));
        return md;
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

    @Override
    protected DocumentMetadata createNewMetadata(String user, String comment, String docPath) {
        return createMetadataFromLatest(user, comment, docPath, false, INITIAL_VERSION, true);
    }

    public Integer getVersionNumberAsOfTime(String docUri, String asOfTime) {
        // modifiedTimestamp is in milliseconds, but the user only supplies whole second precision.
        // Use the less precise measure to avoid the situation where the user supplies the exact datetime
        // a document was created/modified and we say it didn't exist yet because it was created a few
        // milliseconds later.
        long asOfTimeUnix;

        if (asOfTime.matches("t\\d+")) {
            asOfTimeUnix = Long.parseLong(asOfTime.substring(1));
        }
        else {
            String fmt = null;
            if (asOfTime.indexOf('-') > -1) {
                fmt = AS_OF_TIME_FORMAT_TZ;
            }
            else {
                fmt = AS_OF_TIME_FORMAT;
            }

            SimpleDateFormat format = new SimpleDateFormat(fmt);
            try {
                asOfTimeUnix = format.parse(asOfTime).getTime() / 1000L;
            }
            catch (ParseException e) {
                String[] parameters = {asOfTime, AS_OF_TIME_FORMAT};
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                        new MessageFormat(Messages.getString("UserInput.InvalidDatetimeFormat"), parameters));
            }
        }

        DocumentMetadata metadata = getLatestMeta(docUri);
        if (metadata.getCreatedTimestamp() / 1000L > asOfTimeUnix) {
            return null;
        }

        int version = metadata.getVersion();
        while (metadata != null && metadata.getModifiedTimestamp() / 1000L > asOfTimeUnix) {
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
}
