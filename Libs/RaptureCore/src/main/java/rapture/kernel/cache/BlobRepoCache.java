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
package rapture.kernel.cache;

import rapture.blob.BlobStoreFactory;
import rapture.blob.BlobStore;
import rapture.repo.BlobRepo;
import rapture.common.Scheme;
import rapture.common.model.BlobRepoConfig;
import rapture.common.model.BlobRepoConfigStorage;
import rapture.repo.RepoFactory;
import rapture.repo.Repository;

/**
 * Created by yanwang on 6/23/14.
 */
public class BlobRepoCache extends AbstractRepoCache<BlobRepoConfig, BlobRepo> {

    public BlobRepoCache() {
        super(Scheme.BLOB.toString());
    }

    @Override
    protected void reloadRepo(String authority, boolean autoloadIndex) {
        BlobRepoConfig config = reloadConfig(authority);
        if (config != null) {
            BlobRepo repo = reloadRepository(config, true);
            addRepo(authority, config, repo);
        }
    }

    @Override
    public BlobRepoConfig reloadConfig(String authority) {
        return BlobRepoConfigStorage.readByFields(authority);
    }

    @Override
    public BlobRepo reloadRepository(BlobRepoConfig blobRepoConfig, boolean autoloadIndex) {
        BlobStore store = BlobStoreFactory.createBlobStore(blobRepoConfig.getConfig());
        store.init();
        Repository metaDataRepo = reloadMetaDataRepo(blobRepoConfig);
        return new BlobRepo(store, metaDataRepo);
    }

    private Repository reloadMetaDataRepo(BlobRepoConfig blobRepoConfig) {
        return RepoFactory.getRepo(blobRepoConfig.getMetaConfig());
    }
}
