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

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import rapture.common.MessageFormat;
import rapture.common.Messages;
import rapture.common.RaptureNativeQueryResult;
import rapture.common.RaptureQueryResult;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JsonContent;

public abstract class AbstractKeyStore implements KeyStore {

    protected static Messages apiMessageCatalog;
    protected static final MessageFormat NOT_IMPLEMENTED;
    
    static {
        apiMessageCatalog = new Messages("Api");
        NOT_IMPLEMENTED = apiMessageCatalog.getMessage("CannotReadKeys");
    }

    @Override
    public void setRepoLockHandler(RepoLockHandler repoLockHandler) {
    }

    @Override
    public boolean containsKey(String ref) {
        return false;
    }

    @Override
    public boolean delete(String key) {
        return false;
    }

    @Override
    public boolean delete(List<String> keys) {
        for (String key : keys) {
            delete(key);
        }
        return true;
    }

    @Override
    public boolean dropKeyStore() {
        return false;
    }

    @Override
    public String get(String k) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, NOT_IMPLEMENTED);
    }

    @Override
    public List<String> getBatch(List<String> keys) {
        List<String> ret = new ArrayList<String>(keys.size());
        for (String key : keys) {
            ret.add(get(key));
        }
        return ret;
    }

    @Override
    public void put(String k, String v) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, NOT_IMPLEMENTED);
    }

    @Override
    public RaptureQueryResult runNativeQuery(String repoType, List<String> queryParams) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, NOT_IMPLEMENTED);
    }

    @Override
    public RaptureNativeQueryResult runNativeQueryWithLimitAndBounds(String repoType, List<String> queryParams, int limit, int offset) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, NOT_IMPLEMENTED);
    }

    @Override
    public void visit(String folderPrefix, final RepoVisitor iRepoVisitor) {
        // Here we want to visit all of the keys that match that prefix
        visitKeys(folderPrefix, new StoreKeyVisitor() {

            @Override
            public boolean visit(String key, String value) {
                return iRepoVisitor.visit(key, new JsonContent(value), false);
            }

        });
    }

    @Override
    public void visitKeys(String prefix, StoreKeyVisitor iStoreKeyVisitor) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, NOT_IMPLEMENTED);
    }

    @Override
    public void visitKeysFromStart(String startPoint, StoreKeyVisitor iStoreKeyVisitor) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, NOT_IMPLEMENTED);
    }

    @Override
    public boolean matches(String key, String value) {
        String val = get(key);
        return val != null && val.equals(value);
    }

}
