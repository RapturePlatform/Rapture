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
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.SearchResponse;
import rapture.common.api.SearchApi;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.model.DocumentWithMeta;
import rapture.common.model.SearchRepoConfig;
import rapture.common.model.SearchRepoConfigStorage;
import rapture.config.ConfigLoader;
import rapture.kernel.search.SearchRepoType;
import rapture.kernel.search.SearchRepository;

public class SearchApiImpl extends KernelBase implements SearchApi {
    private static Logger logger = Logger.getLogger(SearchApiImpl.class);

    public SearchApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    // Trusted calls
    
    public void writeSearchEntry(String repo, DocumentWithMeta doc) {
    	logger.info("Writing search entry to " + repo);
    	SearchRepository r = Kernel.getRepoCacheManager().getSearchRepo(repo);
    	r.put(doc);
    }
    
    @Override
    public SearchResponse search(CallingContext context, String query) {
    	// By default this searches the default search repository/index (search://main)
    	// And within that the "doc" table
    	
    	List<String> types = Arrays.asList(SearchRepoType.DOC.toString(), SearchRepoType.META.toString(), SearchRepoType.URI.toString());
        return Kernel.getRepoCacheManager().getSearchRepo(
        		ConfigLoader.getConf().FullTextSearchDefaultRepo)
        		.search(types,
        				query);
    }

    @Override
    public SearchResponse searchWithCursor(CallingContext context, String cursorId, int size, String query) {
      	List<String> types = Arrays.asList(SearchRepoType.DOC.toString(), SearchRepoType.META.toString(), SearchRepoType.URI.toString());
        return Kernel.getRepoCacheManager().getSearchRepo(
        		ConfigLoader.getConf().FullTextSearchDefaultRepo)
        		.searchWithCursor(types,cursorId, size,
        				query);
    }

	@Override
	public Boolean validateSearchRepo(CallingContext context,
			String searchRepoUri) {
		return true; // for now
	}

	@Override
	public void createSearchRepo(CallingContext context, String searchRepoUri,
			String config) {
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

        // blob repo will be cached when it's accessed the first time
        logger.info("Creating Repository " + searchRepoUri);
	}

	@Override
	public Boolean searchRepoExists(CallingContext context, String searchRepoUri) {
        return (getSearchRepoConfig(context, searchRepoUri) != null);
	}

	@Override
	public SearchRepoConfig getSearchRepoConfig(CallingContext context,
			String searchRepoUri) {
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

}
