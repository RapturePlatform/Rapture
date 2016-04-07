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
package rapture.elasticsearch;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import rapture.common.ConnectionInfo;
import rapture.common.RaptureURI;
import rapture.common.connection.ConnectionType;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentWithMeta;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.search.SearchRepoType;
import rapture.kernel.search.SearchRepository;

/**
 * ElasticSearch implementation of our search repository. All ElasticSearch specific stuff should be found in here.
 * 
 * @author dukenguyen
 *
 */
public class ElasticSearchSearchRepository implements SearchRepository {

    private static final Logger log = Logger.getLogger(ElasticSearchSearchRepository.class);

    /*
     * how long to keep the cursor alive between paginated search queries, in milliseconds
     */
    private static final long CURSOR_KEEPALIVE = 600000;

    /*
     * An ElasticSearch 'index' is akin to a database in SQL or a database in mongo
     */
    private String index;
    private String instanceName;
    private ConnectionInfo connectionInfo;
    private Client client = null;

    private Client ensureClient() {
    	if (client==null) {
    		start();
    		return client;
    	}
    	return client;
    }
    
    @Override
    public void put(DocumentWithMeta docMeta) {
    	String uri = docMeta.getDisplayName();
    	String[] parts = uri.split("/");
    	SimpleURI uriStore = new SimpleURI();
    	uriStore.setParts(Arrays.asList(parts));
    	
        ensureClient().prepareIndex(index, SearchRepoType.DOC.toString(), uri).setSource(docMeta.getContent()).get();
        String meta = JacksonUtil.jsonFromObject(docMeta.getMetaData());
        ensureClient().prepareIndex(index, SearchRepoType.META.toString(), uri).setSource(meta).get();
        ensureClient().prepareIndex(index, SearchRepoType.URI.toString(), uri).setSource(JacksonUtil.jsonFromObject(uriStore)).get();
    }

    @Override
    public rapture.common.SearchResponse search(List<String> types, String query) {
        SearchResponse response = ensureClient().prepareSearch().setIndices(index).setTypes(types.toArray(new String[types.size()])).setQuery(QueryBuilders.queryStringQuery(query)).get();
        return convert(response);
    }

    @Override
    public rapture.common.SearchResponse searchWithCursor(List<String> types, String cursorId, int size, String query) {
        SearchResponse response;
        if (StringUtils.isBlank(cursorId)) {
            response = ensureClient().prepareSearch()
                    .setQuery(QueryBuilders.queryStringQuery(query))
                    .setScroll(new TimeValue(CURSOR_KEEPALIVE))
                    .setIndices(index)
                    .setTypes(types.toArray(new String[types.size()]))
                    .setSize(size)
                    .get();
        } else {
            response = ensureClient().prepareSearchScroll(cursorId).setScroll(new TimeValue(CURSOR_KEEPALIVE)).get();
        }
        return convert(response);
    }

    @Override
    public void start() {
        getConnectionInfo();
        client = TransportClient.builder().build();
        try {
            ((TransportClient) client)
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(connectionInfo.getHost()), connectionInfo.getPort()));
            log.info(String.format("ElasticSearch connection configured to [%s:%d]", connectionInfo.getHost(), connectionInfo.getPort()));
        } catch (UnknownHostException e) {
            log.error(e);
        }
    }

    void setIndex(String index) {
        this.index = index;
    }

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    /**
     * A 'type' in ElasticSearch is equivalent to a table name in Sql or a collection name in mongo. We will simply use <scheme>.<authority> as our type.
     * 
     * @param uri
     * @return
     */
    String getType(RaptureURI uri) {
        if (uri.hasScheme()) {
            return String.format("%s.%s", uri.getScheme().toString(), uri.getAuthority());
        }
        throw RaptureExceptionFactory.create("No scheme in URI, cannot extract type for indexing.");
    }

    /**
     * Convert from scheme.authority and id back to scheme://authority/id
     * 
     * @param type
     * @param id
     * @return
     */
    String getUri(String type, String id) {
        return type.replaceFirst("\\.", "://") + "/" + id;
    }

    /**
     * For unit testing
     * 
     * @param client
     */
    void setClient(Client client) {
        this.client = client;
    }

    rapture.common.SearchResponse convert(SearchResponse response) {
        rapture.common.SearchResponse ret = new rapture.common.SearchResponse();
        ret.setCursorId(response.getScrollId());
        ret.setMaxScore(Double.parseDouble(Float.toString(response.getHits().getMaxScore())));
        ret.setTotal(response.getHits().getTotalHits());
        ret.setSearchHits(new ArrayList<>());
        for (SearchHit hit : response.getHits().getHits()) {
            rapture.common.SearchHit rHit = new rapture.common.SearchHit();
            rHit.setScore(Double.parseDouble(Float.toString(hit.getScore())));
            rHit.setSource(hit.getSourceAsString());
            rHit.setUri(getUri(hit.getType(), hit.getId()));
            ret.getSearchHits().add(rHit);
        }
        return ret;
    }

    private void getConnectionInfo() {
        if (StringUtils.isBlank(instanceName)) {
            instanceName = "default";
        }
        
        Map<String, ConnectionInfo> map = Kernel.getSys().getConnectionInfo(
                ContextFactory.getKernelUser(),
                ConnectionType.ES.toString());
        connectionInfo = map.get(instanceName);
        if (connectionInfo == null) {
            throw RaptureExceptionFactory.create("Elastic search for instance " + instanceName + " is not defined.");
        }
        index = connectionInfo.getDbName();
    }

	@Override
	public void setConfig(Map<String, String> config) {
		setIndex(config.get("index"));
	}
}