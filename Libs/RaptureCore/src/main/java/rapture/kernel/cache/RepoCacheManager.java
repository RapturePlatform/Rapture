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

import com.google.common.base.Optional;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.SeriesRepoConfig;
import rapture.common.model.BlobRepoConfig;
import rapture.common.model.DocumentRepoConfig;
import rapture.kernel.search.SearchRepository;
import rapture.object.storage.StorableIndexInfo;
import rapture.repo.BlobRepo;
import rapture.repo.Repository;
import rapture.repo.SeriesRepo;
import rapture.repo.StructuredRepo;

/**
 * Created by yanwang on 6/26/14.
 */
public class RepoCacheManager {

    private SysRepoCache sysRepoCache;
    private BlobRepoCache blobRepoCache;
    private DocRepoCache docRepoCache;
    private SeriesRepoCache seriesRepoCache;
    private StructuredRepoCache structuredRepoCache;
    private SearchRepoCache searchRepoCache;

    public RepoCacheManager() {
        sysRepoCache = new SysRepoCache();
        blobRepoCache = new BlobRepoCache();
        docRepoCache = new DocRepoCache();
        seriesRepoCache = new SeriesRepoCache();
        structuredRepoCache = new StructuredRepoCache();
        searchRepoCache = new SearchRepoCache();
    }

    /**
     * Backward compatible with Kernel.getRepo() Historically it's used for Document repo and Sys repo
     *
     * @param repoName
     * @return
     */
    public Repository getRepo(String repoName) {
        if (repoName.startsWith(SysRepoCache.SYS_PREFIX)) {
            return sysRepoCache.getRepo(repoName);
        } else {
            RaptureURI uri = new RaptureURI(repoName, Scheme.DOCUMENT);
            return docRepoCache.getRepo(uri.getAuthority());
        }
    }

    public Optional<Repository> getStorableRepo(String repoName, StorableIndexInfo indexInfo) {
        if (repoName.startsWith(SysRepoCache.SYS_PREFIX)) {
            Optional<Repository> repo = sysRepoCache.getStorableRepo(repoName, indexInfo);
            if (!repo.isPresent()) {
                System.out.println("No repo named " + repoName);
            }
            return repo;
        } else {
            RaptureURI uri = new RaptureURI(repoName, Scheme.DOCUMENT);
            return docRepoCache.getStorableRepo(uri.getAuthority(), indexInfo);
        }
    }

    public Repository getSysRepo(String authority) {
        return sysRepoCache.getRepo(authority);
    }

    public BlobRepo getBlobRepo(String authority) {
        return blobRepoCache.getRepo(authority);
    }

    public BlobRepoConfig getBlobConfig(String authority) {
        return blobRepoCache.getConfig(authority);
    }

    public Repository getDocRepo(String authority) {
        return docRepoCache.getRepo(authority);
    }

    public DocumentRepoConfig getDocConfig(String authority) {
        return docRepoCache.getConfig(authority);
    }

    public SeriesRepo getSeriesRepo(String authority) {
        return seriesRepoCache.getRepo(authority);
    }

    public SeriesRepoConfig getSeriesRepoConfig(String authority) {
        return seriesRepoCache.getConfig(authority);
    }

    public StructuredRepo getStructuredRepo(String authority) {
        return structuredRepoCache.getRepo(authority);
    }

    public SearchRepository getSearchRepo(String authority) {
        return searchRepoCache.getRepo(authority);
    }

    /**
     * Backward compatible with Kernel.removeRepo Historically it's used for Sys and Document repo
     *
     * @param authority
     */
    public void removeRepo(String authority) {
        if (authority.startsWith(SysRepoCache.SYS_PREFIX)) {
            sysRepoCache.removeRepo(authority);
        } else {
            docRepoCache.removeRepo(authority);
        }
    }

    public void removeRepo(String type, String authority) {
        if (Scheme.BLOB.toString().equalsIgnoreCase(type)) {
            blobRepoCache.removeRepo(authority);
        } else if (Scheme.DOCUMENT.toString().equalsIgnoreCase(type)) {
            docRepoCache.removeRepo(authority);
        } else if (Scheme.SERIES.toString().equalsIgnoreCase(type)) {
            seriesRepoCache.removeRepo(authority);
        } else if (Scheme.STRUCTURED.toString().equalsIgnoreCase(type)) {
            structuredRepoCache.removeRepo(authority);
        } else if (Scheme.SEARCH.toString().equalsIgnoreCase(type)) {
            searchRepoCache.removeRepo(authority);
        }
    }

    public void createDefaultUsers() {
        sysRepoCache.createDefaultUsers();
    }

    public void resetAllCache() {
        sysRepoCache = null;
        blobRepoCache = null;
        docRepoCache = null;
        seriesRepoCache = null;
        structuredRepoCache = null;
        searchRepoCache = null;
    }
}
