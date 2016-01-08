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
import rapture.common.RaptureURI;
import rapture.common.TableQueryResult;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentWithMeta;
import rapture.dsl.dparse.BaseDirective;
import rapture.index.IndexProducer;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseRepo implements Repository {
    protected KeyStore store;

    public BaseRepo(KeyStore store2, Map<String, String> config) {
        this.setStore(store2);
    }

    public KeyStore getStore() {
        return store;
    }

    public void setStore(KeyStore store) {
        this.store = store;
    }

    @Override
    public DocumentWithMeta getDocAndMeta(String disp, BaseDirective directive) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public List<DocumentWithMeta> getDocAndMetas(List<RaptureURI> uris) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public DocumentMetadata getMeta(String key, BaseDirective directive) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public DocumentWithMeta revertDoc(String disp, BaseDirective directive) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public List<RaptureFolderInfo> getChildren(String prefix) {
        return store.getSubKeys(prefix);
    }

    @Override
    public List<String> getAllChildren(String displayNamePart) {
        return store.getAllSubKeys(displayNamePart);
    }

    @Override
    public void setIndexProducer(IndexProducer producer) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public boolean hasIndexProducer() {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public TableQueryResult findIndex(String query) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseRepo.notsupp")); //$NON-NLS-1$        
    }

    @Override
    public Map<String, String> getStatus() {
        return new HashMap<String, String>();
    }
}
