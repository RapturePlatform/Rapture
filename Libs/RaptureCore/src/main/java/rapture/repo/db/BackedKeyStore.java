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
package rapture.repo.db;

import java.util.List;
import java.util.Map;

import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureQueryResult;
import rapture.common.exception.RaptNotSupportedException;
import rapture.index.IndexProducer;
import rapture.index.IndexHandler;
import rapture.repo.AbstractKeyStore;
import rapture.repo.KeyStore;
import rapture.repo.StoreKeyVisitor;

/**
 * A backed key store has two stores - a cache for retrieval (and potentially
 * storage) and a real store
 *
 * @author alan
 */
public class BackedKeyStore extends AbstractKeyStore implements KeyStore {
    private KeyStore primary;
    private KeyStore cache;

    public BackedKeyStore(KeyStore primary, KeyStore cache) {
        this.primary = primary;
        this.cache = cache;
    }

    @Override
    public boolean containsKey(String ref) {
        if (cache.containsKey(ref)) {
            return true;
        }
        return primary.containsKey(ref);
    }

    @Override
    public long countKeys() throws RaptNotSupportedException {
        return primary.countKeys();
    }

    @Override
    public KeyStore createRelatedKeyStore(String relation) {
        return null;
    }

    @Override
    public boolean delete(String key) {
        cache.delete(key);
        return primary.delete(key);
    }

    @Override
    public String get(String k) {
        if (cache.containsKey(k)) {
            return cache.get(k);
        }
        String v = primary.get(k);
        if (v != null) {
            cache.put(k, v);
        }
        return v;
    }

    @Override
    public String getStoreId() {
        return primary.getStoreId();
    }

    @Override
    public void put(String k, String v) {
        cache.put(k, v);
        primary.put(k, v);
    }

    @Override
    public RaptureQueryResult runNativeQuery(String repoType, List<String> queryParams) {
        return primary.runNativeQuery(repoType, queryParams);
    }

    @Override
    public void setConfig(Map<String, String> config) {
    }

    @Override
    public void visitKeys(String prefix, StoreKeyVisitor iStoreKeyVisitor) {
        primary.visitKeys(prefix, iStoreKeyVisitor);
    }

    @Override
    public void visitKeysFromStart(String startPoint, StoreKeyVisitor iStoreKeyVisitor) {
        primary.visitKeysFromStart(startPoint, iStoreKeyVisitor);
    }

    @Override
    public boolean matches(String key, String value) {
        return primary.matches(key, value);
    }

    @Override
    public void setInstanceName(String name) {
    }

    @Override
    public List<RaptureFolderInfo> getSubKeys(String prefix) {
        return primary.getSubKeys(prefix);
    }

    @Override
    public List<RaptureFolderInfo> removeSubKeys(String folder, Boolean force) {
        return primary.removeSubKeys(folder, force);
    }

    @Override
    public void resetFolderHandling() {
        primary.resetFolderHandling();
        cache.resetFolderHandling();
    }

    @Override
    public List<String> getAllSubKeys(String displayNamePart) {
        return primary.getAllSubKeys(displayNamePart);
    }

    @Override
    public IndexHandler createIndexHandler(IndexProducer indexProducer) {
        return null;
    }

    @Override
    public Boolean validate() {
        return primary.validate();
    }

    @Override
    public long getSize() {
        long primarySize = primary.getSize();
        long ret = -1;
        if (primarySize != -1L) {
            ret = primarySize;
        }
        long secondarySize = cache.getSize();
        if (secondarySize != -1L) {
            if (ret == -1L) {
                ret = secondarySize;
            } else {
                ret += secondarySize;
            }
        }
        return ret;
    }
}
