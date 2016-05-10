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
package rapture.repo.mem;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;

import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureQueryResult;
import rapture.common.exception.RaptNotSupportedException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.index.IndexHandler;
import rapture.index.IndexProducer;
import rapture.repo.AbstractKeyStore;
import rapture.repo.KeyStore;
import rapture.repo.StoreKeyVisitor;
import rapture.table.memory.MemoryIndexHandler;
import rapture.util.IDGenerator;

public class MemKeyStore extends AbstractKeyStore implements KeyStore {
    // Trivial implementation at present - delegate to storage class later
    private Map<String, String> db;
    private String id;
    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger(MemKeyStore.class);

    public MemKeyStore() {
    }

    @Override
    public synchronized boolean containsKey(String ref) {
        return db.containsKey(ref);
    }

    @Override
    public synchronized long countKeys() throws RaptNotSupportedException {
        return db.size();
    }

    @Override
    public KeyStore createRelatedKeyStore(String relation) {
        Map<String, String> config = new HashMap<String, String>();
        KeyStore related = new MemKeyStore();
        related.setConfig(config);
        return related;
    }

    @Override
    public synchronized boolean delete(String key) {
        return null != db.remove(key);
    }

    @Override
    public synchronized boolean dropKeyStore() {
        db = new HashMap<String, String>();
        return true;
    }

    @Override
    public synchronized String get(String k) {
        return db.get(k);
    }

    @Override
    public String getStoreId() {
        return id;
    }

    @Override
    public synchronized void put(String k, String v) {
        db.put(k, v);
    }

    @Override
    public RaptureQueryResult runNativeQuery(String repoType, List<String> queryParams) {
        if (repoType.toUpperCase().equals("MEMORY")) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not yet implemented");
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "RepoType mismatch. Repo is of type MEMORY, asked for " + repoType);
        }
    }

    @Override
    public synchronized void setConfig(Map<String, String> config) {
        db = new HashMap<String, String>();
        id = IDGenerator.getUUID();
    }

    @Override
    public synchronized void visitKeys(String prefix, StoreKeyVisitor iStoreKeyVisitor) {
        for (String k : new CopyOnWriteArrayList<>(db.keySet())) {
            if (k.startsWith(prefix)) {
                if (!iStoreKeyVisitor.visit(k, db.get(k))) {
                    break;
                }
            }
        }
    }

    @Override
    public synchronized void visitKeysFromStart(String startPoint, StoreKeyVisitor iStoreKeyVisitor) {
        boolean canStart = startPoint == null;
        for (String k : db.keySet()) {
            if (canStart) {
                if (!iStoreKeyVisitor.visit(k, db.get(k))) {
                    break;
                }
            } else {
                if (k.equals(startPoint)) {
                    canStart = true;
                }
            }
        }
    }

    @Override
    public void setInstanceName(String name) {
    }

    @Override
    public synchronized List<RaptureFolderInfo> getSubKeys(String prefix) {
        if (!prefix.isEmpty() && !prefix.endsWith("/")) prefix += "/";
        Map<String, RaptureFolderInfo> retMap = new HashMap<String, RaptureFolderInfo>();
        for (String key : db.keySet()) {
            if (key.startsWith(prefix)) {
                String tail = key.substring(prefix.length());
                int nextSlash = tail.indexOf('/');
                String name;
                if (nextSlash < 0) {
                    name = tail;
                } else {
                    name = tail.substring(0, nextSlash);
                }
                if (!name.isEmpty()) {
                    RaptureFolderInfo info = new RaptureFolderInfo();
                    info.setName(name);
                    info.setFolder(nextSlash >= 0);
                    retMap.put((info.isFolder()) ? name + "/" : name, info);
                }
            }
        }
        List<RaptureFolderInfo> ret = new ArrayList<RaptureFolderInfo>();
        ret.addAll(retMap.values());
        return ret;
    }

    @Override
    public List<RaptureFolderInfo> removeSubKeys(String folder, Boolean force) {
        List<RaptureFolderInfo> ret = new ArrayList<RaptureFolderInfo>();
        removeEntries(ret, folder);
        return ret;

    }

    private void removeEntries(List<RaptureFolderInfo> ret, String folder) {
        List<RaptureFolderInfo> entries = getSubKeys(folder);
        for (RaptureFolderInfo rfi : entries) {
            String nextLevel = folder + "/" + rfi.getName();
            if (rfi.isFolder()) {
                removeEntries(ret, nextLevel);
            } else {
                delete(nextLevel);
                RaptureFolderInfo nextRfi = new RaptureFolderInfo();
                nextRfi.setName(folder);
                nextRfi.setFolder(false);
                ret.add(nextRfi);
            }
        }
        db.remove(folder);
        RaptureFolderInfo topRfi = new RaptureFolderInfo();
        topRfi.setFolder(true);
        topRfi.setName(folder);
        ret.add(topRfi);
    }

    @Override
    public void resetFolderHandling() {

    }

    @Override
    public synchronized List<String> getAllSubKeys(String displayNamePart) {
        List<String> ret = new ArrayList<String>();
        for (String key : db.keySet()) {
            if (key.startsWith(displayNamePart)) {
                if (key.length() > displayNamePart.length() + 1) {
                    ret.add(key.substring(displayNamePart.length()));
                } else {
                    // ret.add(key); //TODO do we need this?
                }
            }
        }
        return ret;
    }

    @Override
    public IndexHandler createIndexHandler(IndexProducer indexProducer) {
        MemoryIndexHandler indexHandler = new MemoryIndexHandler();
        indexHandler.setIndexProducer(indexProducer);
        return indexHandler;

    }

    @Override
    public Boolean validate() {
        return true;
    }
    
    @Override
    public long getSize() {
        return db.size();
    }
}
