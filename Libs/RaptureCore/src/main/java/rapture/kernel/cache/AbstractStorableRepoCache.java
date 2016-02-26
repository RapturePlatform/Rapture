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

import rapture.common.exception.ExceptionToString;
import rapture.index.IndexProducer;
import rapture.object.storage.StorableIndexInfo;
import rapture.repo.Repository;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * @author bardhi
 * @since 4/23/15.
 */
public abstract class AbstractStorableRepoCache<CONFIG> extends AbstractRepoCache<CONFIG, Repository> {
    private static final Logger log = Logger.getLogger(AbstractStorableRepoCache.class);
    private final Cache<String, Repository> cache;

    public AbstractStorableRepoCache(String type) {
        super(type);
        cache = CacheBuilder.newBuilder().maximumSize(500).expireAfterAccess(12, TimeUnit.HOURS).build();
    }

    public Optional<Repository> getStorableRepo(final String repoName, final StorableIndexInfo indexInfo) {
        try {
            return Optional.fromNullable(cache.get(repoName, new Callable<Repository>() {
                @Override
                public Repository call() throws Exception {
                    Repository repo = getStorableRepo(repoName);
                    if (repo != null) {
                        if (indexInfo != null && indexInfo.getIndexDefinitions().size() > 0) {
                            repo.setIndexProducer(new IndexProducer(indexInfo.getIndexDefinitions()));
                        }
                        return repo;
                    } else {
                        return null;
                    }
                }
            }));
        } catch (ExecutionException e) {
            log.error(String.format("Error initializing repo for name [%s]: %s", repoName, ExceptionToString.format(e)));
            return Optional.absent();
        }
    }

    public Repository getStorableRepo(String repoName) {
        reloadIfNotExist(repoName, false);
        return repoCache.get(repoName);
    }

}
