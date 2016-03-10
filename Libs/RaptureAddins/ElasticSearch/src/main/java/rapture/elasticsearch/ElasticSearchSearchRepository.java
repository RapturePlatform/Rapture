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
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;

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

    private Logger log = Logger.getLogger(ElasticSearchSearchRepository.class);

    /*
     * An ElasticSearch 'index' is akin to a database in SQL or a database in mongo
     */
    private String index;

    private String host;
    private int port;
    private TransportClient client;

    @Override
    public void put(RaptureURI uri, String content) {
        IndexResponse response = client.prepareIndex(index, getType(uri), uri.getDocPath()).setSource(content).get();
        log.info("Version is: " + response.getVersion());
    }

    @Override
    public String get(RaptureURI uri) {
        GetResponse response = client.prepareGet(index, getType(uri), uri.getDocPath()).get();
        return response.getSourceAsString();
    }

    @Override
    public String search(String query) {
        SearchResponse response = client.prepareSearch()
                .setQuery(QueryBuilders.simpleQueryStringQuery(query))
                .execute().actionGet();
        return response.toString();
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
            client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port));
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
    private String getType(RaptureURI uri) {
        if (uri.hasScheme()) {
            return String.format("%s.%s", uri.getScheme().toString(), uri.getAuthority());
        }
        throw RaptureExceptionFactory.create("No scheme in URI, cannot extract type for indexing.");
    }

}
