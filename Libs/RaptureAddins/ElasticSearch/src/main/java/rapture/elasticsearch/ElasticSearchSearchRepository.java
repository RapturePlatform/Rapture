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
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import rapture.common.RaptureURI;
import rapture.common.exception.RaptureExceptionFactory;
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

    private String host;
    private int port;
    private Client client;

    @Override
    public void put(RaptureURI uri, String content) {
        client.prepareIndex(index, getType(uri), uri.getDocPath()).setSource(content).get();
    }

    @Override
    public String get(RaptureURI uri) {
        GetResponse response = client.prepareGet(index, getType(uri), uri.getDocPath()).get();
        return response.getSourceAsString();
    }

    @Override
    public rapture.common.SearchResponse search(String query) {
        SearchResponse response = client.prepareSearch().setQuery(QueryBuilders.queryStringQuery(query)).get();
        return convert(response);
    }

    @Override
    public rapture.common.SearchResponse searchWithCursor(String cursorId, int size, String query) {
        SearchResponse response;
        if (StringUtils.isBlank(cursorId)) {
            response = client.prepareSearch()
                    .setQuery(QueryBuilders.queryStringQuery(query))
                    .setScroll(new TimeValue(CURSOR_KEEPALIVE))
                    .setSize(size)
                    .get();
        } else {
            response = client.prepareSearchScroll(cursorId).setScroll(new TimeValue(CURSOR_KEEPALIVE)).get();
        }
        return convert(response);
    }

    @Override
    public void setConfig(Map<String, String> config) {
        String hostStr = config.get("host");
        if (!StringUtils.isBlank(hostStr)) {
            host = hostStr;
        } else {
            log.info("Using default host [localhost]");
            host = "localhost";
        }
        String portStr = config.get("port");
        if (!StringUtils.isBlank(portStr)) {
            port = Integer.parseInt(portStr);
        } else {
            log.info("Using default port [9300]");
            port = 9300;
        }
    }

    @Override
    public void start() {
        client = TransportClient.builder().build();
        try {
            ((TransportClient) client).addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port));
            log.info(String.format("ElasticSearch connection configured to [%s:%d]", host, port));
        } catch (UnknownHostException e) {
            log.error(e);
        }
    }

    @Override
    public void setInstanceName(String instanceName) {
        this.index = instanceName;
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

}
