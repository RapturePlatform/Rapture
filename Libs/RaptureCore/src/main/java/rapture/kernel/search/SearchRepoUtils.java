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
package rapture.kernel.search;

import org.apache.commons.lang3.StringUtils;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.config.ConfigLoader;
import rapture.kernel.Kernel;
import rapture.object.Searchable;

/**
 * Commonly used search repo related functionality
 * 
 * @author dukenguyen
 *
 */
public enum SearchRepoUtils {
    INSTANCE;

    /**
     * Given a repo uri (such as document://x or series://y), get the associated search repo uri e.g. search://main
     * 
     * @param ctx
     * @param repoUri
     * @return
     */
    public static String getSearchRepo(CallingContext ctx, String repoUri) {
        RaptureURI uri = new RaptureURI(repoUri);
        Searchable searchableRepo;
        switch (uri.getScheme()) {
        case SERIES:
            searchableRepo = Kernel.getSeries().getSeriesRepoConfig(ctx, repoUri);
            break;
        case BLOB:
            searchableRepo = Kernel.getBlob().getBlobRepoConfig(ctx, repoUri);
            break;
        default:
            searchableRepo = Kernel.getDoc().getDocRepoConfig(ctx, repoUri);
            break;
        }
        return getSearchRepo(searchableRepo);
    }

    /**
     * Given a Searchable object, get the search repo associated with it. If it's not defined on the object (null), the default is returned
     * 
     * @param searchableRepo
     * @return
     */
    public static String getSearchRepo(Searchable searchableRepo) {
        if (searchableRepo == null) {
            return null;
        }
        String searchRepo = searchableRepo.getFtsIndexRepo();
        if (StringUtils.isBlank(searchRepo)) {
            return ConfigLoader.getConf().FullTextSearchDefaultRepo;
        }
        return searchRepo;
    }
}
