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

import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureNativeQueryResult;
import rapture.common.RaptureQueryResult;
import rapture.common.exception.RaptNotSupportedException;
import rapture.index.IndexProducer;
import rapture.index.IndexHandler;

import java.util.List;
import java.util.Map;

/**
 * How repositories store data
 *
 * @author alan
 */
public interface KeyStore {

    void resetFolderHandling();

    boolean containsKey(String ref);

    long countKeys() throws RaptNotSupportedException;

    // Create a related key store, one that is similar to this one (i.e. shares
    // some of its config) but is
    // different by the "relation"
    KeyStore createRelatedKeyStore(String relation);

    boolean delete(String key);

    boolean delete(List<String> keys);
    
    // Remove this key store (make it empty)
    boolean dropKeyStore();

    String get(String k);

    List<String> getBatch(List<String> keys);

    String getStoreId();

    void put(String k, String v);

    RaptureQueryResult runNativeQuery(String repoType, List<String> queryParams);

    RaptureNativeQueryResult runNativeQueryWithLimitAndBounds(String repoType, List<String> queryParams, int limit, int offset);

    void setConfig(Map<String, String> config);

    void setInstanceName(String name);

    void visit(String folderPrefix, RepoVisitor iRepoVisitor);

    /**
     * This is used to visit every key in the store
     *
     * @param prefix
     * @param iStoreKeyVisitor
     * @
     */
    void visitKeys(String prefix, StoreKeyVisitor iStoreKeyVisitor);

    void visitKeysFromStart(String startPoint, StoreKeyVisitor iStoreKeyVisitor);

    List<RaptureFolderInfo> getSubKeys(String prefix);

    List<RaptureFolderInfo> removeSubKeys(String folder, Boolean force);

    boolean matches(String key, String value);

    List<String> getAllSubKeys(String displayNamePart);


    /**
     * Create a perspective index store associated with this key store
     *
     * @param indexProducer
     * @return
     */
    IndexHandler createIndexHandler(IndexProducer indexProducer);


    Boolean validate();

    /**
     * Return the total size used, or -1 if this information cannot be calculated at this time
     *
     * @return
     */
    long getSize();

    void setRepoLockHandler(RepoLockHandler repoLockHandler);
}
