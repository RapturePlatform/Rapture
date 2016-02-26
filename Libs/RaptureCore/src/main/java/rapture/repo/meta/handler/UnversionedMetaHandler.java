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

import rapture.common.RaptureURI;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentWithMeta;
import rapture.repo.KeyStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bardhi
 * @since 11/12/14.
 */
public class UnversionedMetaHandler extends AbstractMetaHandler {

    public static final int DEFAULT_VERSION = -1;

    public UnversionedMetaHandler(KeyStore documentStore, KeyStore metaStore, KeyStore attributeStore) {
        super(documentStore, metaStore, attributeStore);
    }

    @Override
    protected boolean isVersioned() {
        return false;
    }

    @Override
    protected void addToVersionStore(String docPath, String value, DocumentMetadata newMetaData) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    protected String createLatestKey(String docPath) {
        return docPath + "?" + DEFAULT_VERSION;
    }

    @Override
    protected void updateMetaOnDelete(String user, String docPath) {
        String latestKey = createLatestKey(docPath);
        DocumentMetadata deletionMeta = createDeletionMeta(user, docPath);
        String newMetaJson = JacksonUtil.jsonFromObject(deletionMeta);
        metaStore.put(latestKey, newMetaJson);
        if (indexHandler.isPresent()) {
            indexHandler.get().removeAll(docPath);
        }
    }

    protected DocumentMetadata createDeletionMeta(String user, String key) {
        return createMetadataFromLatest(user, "Deleted", key, true, DEFAULT_VERSION, false);
    }

    @Override
    public List<DocumentWithMeta> getDocAndMetas(List<RaptureURI> uris) {
        if (uris == null || uris.isEmpty()) {
            return new ArrayList<DocumentWithMeta>();
        }
        DocumentWithMeta[] ret = new DocumentWithMeta[uris.size()];
        Map<Integer, String> latestKeys = new LinkedHashMap<Integer, String>();
        List<String> latestKeysWithVersion = new ArrayList<String>();

        int position = 0;
        for (RaptureURI uri : uris) {
            String docPath = uri.getDocPath();
            latestKeys.put(position, docPath);
            latestKeysWithVersion.add(createLatestKey(docPath));
            ++position;
        }

        List<Integer> latestPositionList = new ArrayList<Integer>(latestKeys.keySet());
        List<String> latestContents = documentStore.getBatch(new ArrayList<String>(latestKeys.values()));
        List<String> latestMeta = metaStore.getBatch(latestKeysWithVersion);
        constructDocumentWithMetaList(ret, uris, latestPositionList, latestContents, latestMeta);

        return Arrays.asList(ret);
    }

    public Map<String, String> getStatus() {
        Map<String, String> ret = new HashMap<String, String>();
        Object[] checks = { documentStore, "Latest", metaStore, "Meta" };
        long totalSize = 0L;
        for (int i = 0; i < checks.length; i += 2) {
            KeyStore ks = (KeyStore) checks[i];
            if (ks != null) {
                long size = ks.getSize();
                if (size != -1L) {
                    ret.put(checks[i + 1].toString(), readableFileSize(size));
                    totalSize += size;
                }
            }
        }
        if (totalSize != 0L) {
            ret.put("Total", readableFileSize(totalSize));
        }
        return ret;
    }

    @Override
    protected DocumentMetadata createNewMetadata(String user, String comment, String docPath) {
        return createMetadataFromLatest(user, comment, docPath, false, DEFAULT_VERSION, false);
    }

}
