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
package rapture.repo.cassandra;

import rapture.cassandra.CassandraConstants;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureNativeQueryResult;
import rapture.common.RaptureQueryResult;
import rapture.common.exception.RaptNotSupportedException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JsonContent;
import rapture.index.IndexProducer;
import rapture.index.IndexHandler;
import rapture.repo.KeyStore;
import rapture.repo.RepoLockHandler;
import rapture.repo.RepoVisitor;
import rapture.repo.StoreKeyVisitor;
import rapture.repo.cassandra.key.PathBuilder;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/*
 * A key store is a relation store. The keys will be rows, the value will be in a special column
 * storing a document (the string)
 * 
 * The folderHandling needs to solve the main request problem - what are the immediate children of the
 * given folder, and are those children themselves folders or documents. The prefix will be a row, the
 * children will be named columns associated with that row, the data will be a structure that determines
 * how many hits there are (so we can update it and possibly remove it) or, if the content is -1 then the child
 * is a document.
 */

public class CassandraKeyStore implements KeyStore {

    private CassFolderHandler folderHandler;
    private AstyanaxRepoConnection repoConnection;
    private String instance = "default";
    private Map<String, String> config;
    private boolean usesFolderHandling = true;
    private boolean useVersionedRepoConnection = false;

    @Override
    public void setConfig(Map<String, String> config) {
        this.config = config;
        if (useVersionedRepoConnection) {
            repoConnection = new AstyanaxVersionedRepoConnection(instance, this.config);
        }
        else {
            repoConnection = new AstyanaxRepoConnection(instance, this.config);
        }

        folderHandler = new CassFolderHandler(repoConnection, repoConnection.getColumnFamilyName());
    }

    public void setUseVersionedRepoConnection(Boolean useIt) {
        useVersionedRepoConnection = useIt;
    }

    @Override
    public void resetFolderHandling() {
        usesFolderHandling = false;
    }

    @Override
    public boolean containsKey(String ref) {
        // Is the given row available with the column name 'data'
        return get(ref) != null;
    }

    @Override
    public long countKeys() throws RaptNotSupportedException {
        // This is a bit tricky - it is basically "how many rows do we have"
        return 0;
    }

    @Override
    public KeyStore createRelatedKeyStore(String relation) {
        // Setup a config like this, but with the columnFamily adjusted to add
        // the relation, then
        // fire it up.
        Map<String, String> configCopy = new HashMap<String, String>(config);
        configCopy.put(CassandraConstants.CFCFG, repoConnection.getColumnFamilyName() + "_" + relation);
        if (repoConnection.getPKeyPrefix().isPresent()) {
            configCopy.put(CassandraConstants.PARTITION_KEY_PREFIX, repoConnection.getPKeyPrefix().get());
        }

        CassandraKeyStore ret = new CassandraKeyStore();
        ret.setInstanceName(this.instance);
        if (relation.equals("version") || relation.equals("meta")) {
            ret.setUseVersionedRepoConnection(true);
        }
        ret.setConfig(configCopy);
        if (!usesFolderHandling) {
            ret.resetFolderHandling();
        }
        return ret;
    }

    @Override
    public boolean delete(String key) {
        // Remove the data column for this row
        // Also update folder handling
        if (usesFolderHandling) {
            folderHandler.removeDocument(key);
        }
        return repoConnection.deleteEntry(key);
    }

    @Override
    public boolean delete(List<String> keys) {
        if (usesFolderHandling) {
            for (String key : keys) {
                folderHandler.removeDocument(key);
            }
        }
        return repoConnection.deleteEntries(keys);
    }

    @Override
    public boolean deleteUpTo(String key, long millisTimestamp) {
        return repoConnection.deleteVersionsUpTo(key, "" + millisTimestamp);
    }

    @Override
    public boolean dropKeyStore() {
        if (usesFolderHandling) {
            folderHandler.drop();
        }
        repoConnection.dropRepo();
        return true;
    }

    @Override
    public String get(String k) {
        return repoConnection.get(k);
    }

    @Override
    public String get(String k, long millisTimestamp) {
        return repoConnection.get(k, Long.toString(millisTimestamp));
    }

    @Override
    public List<String> getBatch(List<String> keys) {
        return repoConnection.batchGet(keys);
    }

    @Override
    public String getStoreId() {
        return repoConnection.getUniqueId();
    }

    @Override
    public void put(String k, String v) {
        repoConnection.putData(k, v);
        if (usesFolderHandling) {
            folderHandler.registerDocument(k);
        }
    }

    @Override
    public void put(String k, long millisTimestamp, String v) {
        repoConnection.putData(k, "" + millisTimestamp, v);
        if (usesFolderHandling) {
            folderHandler.registerDocument(k);
        }
    }

    @Override
    public RaptureQueryResult runNativeQuery(String repoType, List<String> queryParams) {
        throw new UnsupportedOperationException("Native queries not supported in Cassandra yet!");
    }

    @Override
    public RaptureNativeQueryResult runNativeQueryWithLimitAndBounds(String repoType, List<String> queryParams, int limit, int offset) {
        throw new UnsupportedOperationException("Native queries not supported in Cassandra yet!");
    }

    @Override
    public void setInstanceName(String instanceName) {
        this.instance = instanceName;
    }

    @Override
    public void visit(String folderPrefix, RepoVisitor iRepoVisitor) {
        // Look for rows with a data column, starting with the row
        // "folderPrefix" and ending when we
        // see a row key that doesn't begin with folderPrefix
        // Use the folder handling for this.
        if (!usesFolderHandling) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "No folder handling");
        }
        List<RaptureFolderInfo> thisFolder = folderHandler.getChildren(folderPrefix);
        for (RaptureFolderInfo folder : thisFolder) {
            String fullName = new PathBuilder(folderPrefix).subPath(folder.getName()).build();
            String content = folder.isFolder() ? null : get(fullName);
            JsonContent jc = new JsonContent(content);
            if (!iRepoVisitor.visit(fullName, jc, folder.isFolder())) break;
        }
    }

    @Override
    public void visitKeys(String prefix, StoreKeyVisitor iStoreKeyVisitor) {
        List<String> docKeys = getAllSubKeys(prefix);
        for (String dK : docKeys) {
            String content = get(dK);
            if (!iStoreKeyVisitor.visit(dK, content)) {
                break;
            }
        }
    }

    @Override
    public void visitKeysFromStart(String startPoint, StoreKeyVisitor iStoreKeyVisitor) {
        visitKeys(startPoint, iStoreKeyVisitor);
    }

    @Override
    public List<RaptureFolderInfo> getSubKeys(String prefix) {
        if (usesFolderHandling) {
            return folderHandler.getChildren(prefix);
        }
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "No folder handling supported");
    }

    @Override
    public List<RaptureFolderInfo> removeSubKeys(String folder, Boolean force) {
        if (usesFolderHandling) {
            return folderHandler.removeChildren(folder, force);
        }
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "No folder handling supported");
    }

    @Override
    public boolean matches(String key, String value) {
        String v = get(key);
        if (v != null) {
            return v.equals(value);
        }
        return false;
    }

    @Override
    public List<String> getAllSubKeys(String prefix) {
        // Recursively retrieve the documents below this displayNamePart by
        // calling getSubKeys multiple times
        // OR use folder handling to store this information during a save (why
        // not... ?)
        if (usesFolderHandling) {
            return folderHandler.getAllChildren(prefix);
        }
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "No folder handling supported");
    }

    @Override
    public IndexHandler createIndexHandler(IndexProducer indexProducer) {
        // TODO AM - return an instance of a perspective table store using some
        // prefix
        // from this key store
        return null;
    }

    @Override
    public Boolean validate() {
        return repoConnection.validate();
    }

    @Override
    public long getSize() {
        long result = repoConnection.getRowNumber();
        return result;
    }

    @Override
    public void setRepoLockHandler(RepoLockHandler repoLockHandler) {

    }

    @Override
    public boolean supportsVersionLookupByTime() {
        return true;
    }
}
