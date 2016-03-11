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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;

import rapture.common.ConnectionInfo;
import rapture.common.RaptureURI;
import rapture.common.connection.ConnectionType;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
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

    private String instanceName;
    private ConnectionInfo connectionInfo;

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
    public void start() {
        getConnectionInfo();
        client = TransportClient.builder().build();
        try {
            client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(connectionInfo.getHost()), connectionInfo.getPort()));
            log.info(String.format("ElasticSearch connection configured to [%s:%d]", connectionInfo.getHost(), connectionInfo.getPort()));
        } catch (UnknownHostException e) {
            log.error(e);
        }
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
    private String getType(RaptureURI uri) {
        if (uri.hasScheme()) {
            return String.format("%s.%s", uri.getScheme().toString(), uri.getAuthority());
        }
        throw RaptureExceptionFactory.create("No scheme in URI, cannot extract type for indexing.");
    }

    private void getConnectionInfo() {
        if(StringUtils.isBlank(instanceName)) {
            instanceName = "default";
        }
        Map<String, ConnectionInfo> map = Kernel.getSys().getConnectionInfo(
                ContextFactory.getKernelUser(),
                ConnectionType.ES.toString());
        connectionInfo = map.get(instanceName);
        if(connectionInfo == null) {
            throw RaptureExceptionFactory.create("Elastic search for instance " + instanceName + " is not defined.");
        }
        index = connectionInfo.getDbName();
    }

}
