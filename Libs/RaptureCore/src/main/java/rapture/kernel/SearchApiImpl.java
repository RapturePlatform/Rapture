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
package rapture.kernel;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.AbstractUpdateObject;
import rapture.common.CallingContext;
import rapture.common.DocUpdateObject;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.SearchHit;
import rapture.common.SearchResponse;
import rapture.common.SeriesPoint;
import rapture.common.api.SearchApi;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.model.DocumentWithMeta;
import rapture.common.model.SearchRepoConfig;
import rapture.common.model.SearchRepoConfigStorage;
import rapture.common.pipeline.PipelineConstants;
import rapture.common.series.SeriesUpdateObject;
import rapture.config.ConfigLoader;
import rapture.kernel.pipeline.SearchPublisher;
import rapture.kernel.search.SearchRepoType;
import rapture.kernel.search.SearchRepository;

public class SearchApiImpl extends KernelBase implements SearchApi {
    private static Logger logger = Logger.getLogger(SearchApiImpl.class);

    public SearchApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    // Trusted calls

    @Deprecated
    public void writeSearchEntry(String searchRepo, DocumentWithMeta doc) {
        logger.debug(String.format("Writing doc search entry to [%s]", searchRepo));
        getRepoOrFail(searchRepo).put(new DocUpdateObject(doc));
    }

    public void writeSearchEntry(String searchRepo, AbstractUpdateObject updateObject) {
        logger.debug(String.format("Writing series search entry to [%s]", searchRepo));
        getRepoOrFail(searchRepo).put(updateObject);
    }

    public void deleteSearchEntry(String searchRepo, RaptureURI uri) {
        logger.debug(String.format("Removing uri [%s] search entry from search repo [%s]", uri.toString(), searchRepo));
        getRepoOrFail(searchRepo).remove(uri);
    }

    @Override
    public SearchResponse search(CallingContext context, String query) {
        List<String> types = Arrays.asList(SearchRepoType.DOC.toString(), SearchRepoType.META.toString(), SearchRepoType.URI.toString(),
                SearchRepoType.SERIES.toString());
        return qualifiedSearch(context, ConfigLoader.getConf().FullTextSearchDefaultRepo, types, query);
    }

    @Override
    public SearchResponse searchWithCursor(CallingContext context, String cursorId, int size, String query) {
        List<String> types = Arrays.asList(SearchRepoType.DOC.toString(), SearchRepoType.META.toString(), SearchRepoType.URI.toString(),
                SearchRepoType.SERIES.toString());
        return qualifiedSearchWithCursor(context, ConfigLoader.getConf().FullTextSearchDefaultRepo, types, cursorId, size, query);
    }

    @Override
    public Boolean validateSearchRepo(CallingContext context, String searchRepoUri) {
        return true; // for now
    }

    @Override
    public void createSearchRepo(CallingContext context, String searchRepoUri, String config) {
        checkParameter("Repository URI", searchRepoUri);
        checkParameter("Config", config);

        RaptureURI interimUri = new RaptureURI(searchRepoUri, Scheme.SEARCH);
        String authority = interimUri.getAuthority();
        if ((authority == null) || authority.isEmpty()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoAuthority")); //$NON-NLS-1$
        }
        if (interimUri.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", searchRepoUri)); //$NON-NLS-1$
        }
        if (searchRepoExists(context, searchRepoUri)) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("Exists", searchRepoUri)); //$NON-NLS-1$
        }

        // store repo config
        SearchRepoConfig rbc = new SearchRepoConfig();
        rbc.setConfig(config);
        rbc.setAuthority(interimUri.getAuthority());
        SearchRepoConfigStorage.add(rbc, context.getUser(), "create repo");
        logger.info("Created search repository config for uri: " + searchRepoUri);
    }

    @Override
    public Boolean searchRepoExists(CallingContext context, String searchRepoUri) {
        return (getSearchRepoConfig(context, searchRepoUri) != null);
    }

    @Override
    public SearchRepoConfig getSearchRepoConfig(CallingContext context, String searchRepoUri) {
        RaptureURI uri = new RaptureURI(searchRepoUri, Scheme.SEARCH);
        if (uri.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", searchRepoUri)); //$NON-NLS-1$
        }
        return SearchRepoConfigStorage.readByAddress(uri);
    }

    @Override
    public List<SearchRepoConfig> getSearchRepoConfigs(CallingContext context) {
        return SearchRepoConfigStorage.readAll();
    }

    @Override
    public void deleteSearchRepo(CallingContext context, String searchRepoUri) {
        SearchRepoConfigStorage.deleteByAddress(new RaptureURI(searchRepoUri, Scheme.SEARCH), context.getUser(), "Remove search repo");
    }

    @Override
    public void rebuildRepoIndex(CallingContext context, String repoUri) {
        RaptureURI uri = new RaptureURI(repoUri);
        // Push this onto an async queue
        SearchPublisher.publishRebuildMessage(context, uri.toString());
    }

    @Override
    public void dropRepoIndex(CallingContext context, String repoUri) {
        RaptureURI uri = new RaptureURI(repoUri);
        // Push this onto an async queue
        SearchPublisher.publishDropMessage(context, uri.toString());
    }

    // In get trusted - these are called asynchronously from the above calls
    public void rebuild(String repoUriStr, String searchRepoUriStr) {
        drop(repoUriStr, searchRepoUriStr);
        RaptureURI repoUri = new RaptureURI(repoUriStr);
        if (StringUtils.isNotBlank(searchRepoUriStr)) {
            workOn(searchRepoUriStr, repoUri.toShortString(), repoUri.getScheme());
        }
    }

    private void workOn(String searchRepo, String prefix, Scheme scheme) {
        SearchRepository r = getRepoOrFail(searchRepo);
        Map<String, RaptureFolderInfo> info;
        switch (scheme) {
        case SERIES:
            info = Kernel.getSeries().listSeriesByUriPrefix(ContextFactory.getKernelUser(), prefix, 1);
            break;
        default:
            info = Kernel.getDoc().listDocsByUriPrefix(ContextFactory.getKernelUser(), prefix, 1);
            break;
        }
        for (Map.Entry<String, RaptureFolderInfo> e : info.entrySet()) {
            String newPrefix = prefix + "/" + e.getValue().getName();
            if (e.getValue().isFolder()) {
                log.debug("Diving into " + newPrefix);
                workOn(searchRepo, newPrefix, scheme);
            } else {
                log.debug("Placing " + newPrefix);
                switch (scheme) {
                case SERIES:
                    List<SeriesPoint> pts = Kernel.getSeries().getPoints(ContextFactory.getKernelUser(), newPrefix);
                    List<String> keys = new ArrayList<>();
                    List<String> values = new ArrayList<>();
                    for (SeriesPoint pt : pts) {
                        keys.add(pt.getColumn());
                        values.add(pt.getValue());
                    }
                    SeriesUpdateObject ser = new SeriesUpdateObject(newPrefix, keys, values);
                    r.put(ser);
                    break;
                default:
                    DocumentWithMeta dm = Kernel.getDoc().getDocAndMeta(ContextFactory.getKernelUser(), newPrefix);
                    dm.setDisplayName(newPrefix);
                    r.put(new DocUpdateObject(dm));
                    break;
                }
            }
        }
    }

    public void drop(String repo, String searchRepo) {
        // 1. Search for documents in the given search repo in the URI area that match this repo
        // 2. For each one, drop that from the index (with appropriate batching)
        RaptureURI repoUri = new RaptureURI(repo);
        if (searchRepo != null) {
            SearchRepository r = getRepoOrFail(searchRepo);
            if (r != null) {
                SearchResponse resp = r.searchForRepoUris(repoUri.getScheme().toString(), repoUri.getAuthority(), null);
                while (!resp.getSearchHits().isEmpty()) {
                    for (SearchHit h : resp.getSearchHits()) {
                        log.debug("URI is " + h.getUri());
                        r.remove(new RaptureURI(h.getUri()));
                    }
                    resp = r.searchForRepoUris(repoUri.getScheme().toString(), repoUri.getAuthority(), resp.getCursorId());
                }
                log.info("End of drop");
            } else {
                logger.error("Could not find search repo for " + searchRepo);
            }
        } else {
            logger.info("No search repo for " + repoUri);
        }
    }

    @Override
    public SearchResponse qualifiedSearch(CallingContext context, String searchRepo, List<String> types, String query) {
        return getRepoOrFail(searchRepo).search(types, query);
    }

    @Override
    public SearchResponse qualifiedSearchWithCursor(CallingContext context, String searchRepo, List<String> types, String cursorId, int size, String query) {
        return getRepoOrFail(searchRepo).searchWithCursor(types, cursorId, size, query);
    }

    /**
     * Return the {@link SearchRepository} for the given uri or throw a
     * {@link RaptureException} with an error message
     *
     * @param uri
     * @return
     */
    private SearchRepository getRepoOrFail(RaptureURI uri) {
        SearchRepository repo = getRepoFromCache(uri.getAuthority());
        if (repo == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoSuchRepo", uri.toAuthString()));
        } else {
            return repo;
        }
    }

    /**
     * We need to start all the search repos at startup
     */
    @Override
    public void startSearchRepos(CallingContext context) {
        if (ConfigLoader.getConf().FullTextSearchOn) {
            List<SearchRepoConfig> configs = getSearchRepoConfigs(context);
            for (SearchRepoConfig config : configs) {
                getRepoOrFail(config.getAuthority()).start();
            }
            Kernel.getPipeline().setupStandardCategory(context, PipelineConstants.CATEGORY_SEARCH);
            Kernel.setCategoryMembership(PipelineConstants.CATEGORY_SEARCH);
        }
    }

    private SearchRepository getRepoOrFail(String searchRepoUri) {
        return getRepoOrFail(new RaptureURI(searchRepoUri, Scheme.SEARCH));
    }

    private SearchRepository getRepoFromCache(String authority) {
        return Kernel.getRepoCacheManager().getSearchRepo(authority);
    }
}