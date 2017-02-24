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
package rapture.repo.google;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.log4j.Logger;

import com.google.cloud.storage.Blob;
import com.google.common.collect.ImmutableMap;

import rapture.blob.google.GoogleBlobStore;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureNativeQueryResult;
import rapture.common.RaptureQueryResult;
import rapture.common.RaptureURI;
import rapture.common.TableQuery;
import rapture.common.TableQueryResult;
import rapture.common.TableRecord;
import rapture.common.exception.RaptNotSupportedException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.model.DocumentMetadata;
import rapture.index.IndexHandler;
import rapture.index.IndexProducer;
import rapture.repo.AbstractKeyStore;
import rapture.repo.KeyStore;
import rapture.repo.RepoLockHandler;
import rapture.repo.RepoVisitor;
import rapture.repo.StoreKeyVisitor;
import rapture.util.IDGenerator;

public class GoogleStorageKeyStore extends AbstractKeyStore implements KeyStore {
    private static final Logger log = Logger.getLogger(GoogleStorageKeyStore.class);
    private GoogleBlobStore blobStore;
    private String id;

    public GoogleStorageKeyStore() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.repo.AbstractKeyStore#setRepoLockHandler(rapture.repo. RepoLockHandler)
     */
    @Override
    public void setRepoLockHandler(RepoLockHandler repoLockHandler) {
        super.setRepoLockHandler(repoLockHandler);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.repo.AbstractKeyStore#delete(java.util.List)
     */
    @Override
    public boolean delete(List<String> keys) {
        return super.delete(keys);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.repo.AbstractKeyStore#dropKeyStore()
     */
    @Override
    public boolean dropKeyStore() {
        return super.dropKeyStore();
    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.repo.AbstractKeyStore#getBatch(java.util.List)
     */
    @Override
    public List<String> getBatch(List<String> keys) {
        return super.getBatch(keys);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.repo.AbstractKeyStore#runNativeQueryWithLimitAndBounds(java.lang. String, java.util.List, int, int)
     */
    @Override
    public RaptureNativeQueryResult runNativeQueryWithLimitAndBounds(String repoType, List<String> queryParams, int limit, int offset) {
        return super.runNativeQueryWithLimitAndBounds(repoType, queryParams, limit, offset);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.repo.AbstractKeyStore#visit(java.lang.String, rapture.repo.RepoVisitor)
     */
    @Override
    public void visit(String folderPrefix, RepoVisitor iRepoVisitor) {
        super.visit(folderPrefix, iRepoVisitor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.repo.AbstractKeyStore#matches(java.lang.String, java.lang.String)
     */
    @Override
    public boolean matches(String key, String value) {
        return super.matches(key, value);
    }

    @Override
    public boolean containsKey(String ref) {
        return blobStore.blobExists(null, new RaptureURI(ref));
    }

    @Override
    public long countKeys() throws RaptNotSupportedException {
        throw new RaptNotSupportedException("Not yet supported");
    }

    Map<String, String> config;

    @Override
    public void setConfig(Map<String, String> config) {
        if (blobStore != null) throw new RuntimeException("Already configured");
        blobStore = new GoogleBlobStore();
        blobStore.setConfig(config);
        this.config = config;
        id = IDGenerator.getUUID();
    }

    @Override
    public KeyStore createRelatedKeyStore(String relation) {
        KeyStore ks = new GoogleStorageKeyStore();
        ks.setConfig(ImmutableMap.of("prefix", config.get("prefix") + relation));
        return ks;
    }

    @Override
    public boolean delete(String key) {
        return blobStore.deleteBlob(key);
    }

    @Override
    public String get(String key) {
        InputStream blob = blobStore.getBlob(key);
        if (blob != null) {
            try (Scanner s = new Scanner(blob)) {
                return s.useDelimiter("\\A").hasNext() ? s.next() : null;
            }
        }
        return null;
    }

    @Override
    public String getStoreId() {
        return id;
    }

    @Override
    public void put(String k, String v) {
        // A key is a blob name, the value is the contents
        blobStore.storeBlob(k, false, new ByteArrayInputStream(v.getBytes()));
    }

    @Override
    public RaptureQueryResult runNativeQuery(String repoType, List<String> queryParams) {
        if (repoType.toUpperCase().equals("GOOGLE_STORE")) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not yet implemented");
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "RepoType mismatch. Repo is of type GOOGLE, asked for " + repoType);
        }
    }

    /**
     * What is the difference?
     */
    @Override
    public void visitKeys(String prefix, StoreKeyVisitor iStoreKeyVisitor) {
        for (Blob key : blobStore.listBlobs(prefix)) {
            if (!iStoreKeyVisitor.visit(key.getName(), new String(key.getContent()))) break;

        }
    }

    @Override
    public void visitKeysFromStart(String startPoint, StoreKeyVisitor iStoreKeyVisitor) {
        visitKeys(startPoint, iStoreKeyVisitor);
    }

    @Override
    public void setInstanceName(String name) {
        // Not sure what this is for
    }

    /**
     * Note that this only gets immediate children - it's not recursive. Should it be?
     */
    @Override
    public List<RaptureFolderInfo> getSubKeys(String prefix) {
        List<RaptureFolderInfo> list = new ArrayList<>();
        Map<String, RaptureFolderInfo> map = new HashMap<>();
        for (Blob key : blobStore.listBlobs(prefix)) {
            String name = key.getName();
            if (name.startsWith(prefix)) {
                String keegan = name.substring(prefix.length());
                int idx = keegan.indexOf('/');
                if (idx > 0) {
                    list.add(new RaptureFolderInfo(keegan.substring(0, idx), true));
                } else {
                    list.add(new RaptureFolderInfo(keegan, false));
                }
            }
        }
        list.addAll(map.values());
        return list;
    }

    @Override
    public List<RaptureFolderInfo> removeSubKeys(String folder, Boolean force) {
        throw new RaptNotSupportedException("Not yet supported");
    }

    @Override
    public List<String> getAllSubKeys(String displayNamePart) {
        throw new RaptNotSupportedException("Not yet supported");
    }

    @Override
    public void resetFolderHandling() {

    }

    @Override
    public IndexHandler createIndexHandler(IndexProducer indexProducer) {
        IndexHandler indexHandler = new IndexHandler() {
            IndexProducer indexProducer;

            @Override
            public void deleteTable() {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public void removeAll(String rowId) {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public void setConfig(Map<String, String> config) {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public List<TableRecord> queryTable(TableQuery query) {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public void setInstanceName(String instanceName) {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public TableQueryResult query(String query) {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public Long getLatestEpoch() {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public void setIndexProducer(IndexProducer indexProducer) {
                this.indexProducer = indexProducer;
            }

            @Override
            public void initialize() {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public void addedRecord(String key, String value, DocumentMetadata mdLatest) {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public void updateRow(String key, Map<String, Object> recordValues) {
                throw new RaptNotSupportedException("Not yet supported");
            }

            @Override
            public void ensureIndicesExist() {
                throw new RaptNotSupportedException("Not yet supported");
            }
        };

        indexHandler.setIndexProducer(indexProducer);
        return indexHandler;
    }

    @Override
    public Boolean validate() {
        return true;
    }

    // Not sure what the point of this is
    @Override
    public long getSize() {
        return -1;
    }
}
