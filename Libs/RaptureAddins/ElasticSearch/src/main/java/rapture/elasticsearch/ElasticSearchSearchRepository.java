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

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.apache.tika.detect.EmptyDetector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.pdf.PDFParser;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.google.common.net.MediaType;

import rapture.common.AbstractUpdateObject;
import rapture.common.BlobContainer;
import rapture.common.BlobUpdateObject;
import rapture.common.ConnectionInfo;
import rapture.common.DocUpdateObject;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.connection.ConnectionType;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptNotSupportedException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentWithMeta;
import rapture.common.series.SeriesUpdateObject;
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
     * Default number of times to retry on version confict for optimistic concurrency control, matches default number of pipeline threads
     */
    private static final int DEFAULT_RETRY_ON_CONFLICT = 50;

    /*
     * An ElasticSearch 'index' is akin to a database in SQL or a database in mongo
     */
    private String index;
    private String instanceName;
    private ConnectionInfo connectionInfo;
    private Client client = null;

    private Client ensureClient() {
        if (client == null) {
            start();
        }
        return client;
    }

    static Tika tikaPDF = new Tika(new EmptyDetector(), new PDFParser());

    @SuppressWarnings("rawtypes")
    @Override
    public void put(AbstractUpdateObject updateObject) {
        RaptureURI uri = updateObject.getUri();
        log.info("URI for indexing is " + uri.toString());
        putUriStore(uri);
        switch (uri.getScheme()) {
        case DOCUMENT:
            DocumentWithMeta docMeta = ((DocUpdateObject) updateObject).getPayload();
            ensureClient().prepareIndex(index, SearchRepoType.DOC.toString(), uri.toString()).setSource(docMeta.getContent()).get();
            String meta = JacksonUtil.jsonFromObject(docMeta.getMetaData());
            ensureClient().prepareIndex(index, SearchRepoType.META.toString(), uri.toString()).setSource(meta).get();
            break;
        case SERIES:
            Map<String, String> map = ((SeriesUpdateObject) updateObject).asStringMap();
            if (!map.isEmpty()) {
                synchronized (client) {
                    ensureClient().prepareUpdate(index, SearchRepoType.SERIES.toString(), uri.toString()).setDoc(map).setUpsert(map)
                            .setRetryOnConflict(DEFAULT_RETRY_ON_CONFLICT).get();
                }
            }
            break;
        case BLOB:
            BlobContainer content = ((BlobUpdateObject) updateObject).getPayload();
            /**
             * You have to give ElasticSearch a JSON document. We can determine what it is by checking updateObject.getMimeType() Do we need to index the blob's
             * mime type and any other metadata too?
             */

            // Tika can handle other types too, but at present all we really care about are blobs and CSVs
            if (updateObject.getMimeType().equals(MediaType.PDF.toString())) {
                // Tika can take a while so do it in the background
                new Thread() {
                    @SuppressWarnings("synthetic-access")
                    @Override
                    public void run() {
                        try {
                            String contentStr = tikaPDF.parseToString(new ByteArrayInputStream(content.getContent()));
                            XContentBuilder source = jsonBuilder().startObject().field("blob", contentStr).endObject();
                            ensureClient().prepareIndex(index, SearchRepoType.BLOB.toString(), uri.toString()).setSource(source).get();
                        } catch (IOException | TikaException e) {
                            log.error("Cannot index PDF " + e.getMessage());
                            log.debug(ExceptionToString.format(e));
                            throw RaptureExceptionFactory.create("Cannot index PDF " + e.getMessage(), e);
                        }
                    }
                }.start();
            } else {
                try {
                    XContentBuilder source = jsonBuilder().startObject().field("blob", new String(content.getContent())).endObject();
                    ensureClient().prepareIndex(index, SearchRepoType.BLOB.toString(), uri.toString()).setSource(source).get();
                } catch (IOException ioe) {
                    log.error("Cannot index CSV " + ioe.getMessage());
                    log.debug(ExceptionToString.format(ioe));
                    throw RaptureExceptionFactory.create("Cannot index blob " + ioe.getMessage(), ioe);
                }
            }
            try {
                Map<String, String> headers = content.getHeaders();
                XContentBuilder source = jsonBuilder().startObject();
                if (headers != null) {
                    for (Entry<String, String> entry : headers.entrySet()) {
                        source.field(entry.getKey(), entry.getValue());
                    }
                }
                source.field("mimetype", updateObject.getMimeType()).endObject();
                ensureClient().prepareIndex(index, SearchRepoType.META.toString(), uri.toString()).setSource(source).get();
            } catch (IOException ioe) {
                log.error("Cannot index blob metadata " + ioe.getMessage());
                log.debug(ExceptionToString.format(ioe));
                throw RaptureExceptionFactory.create("Cannot index blob " + ioe.getMessage(), ioe);
            }
            break;
        default:
            throw new RaptNotSupportedException("Indexing not yet supported for " + uri.getScheme());
        }
    }

    private void putUriStore(RaptureURI uri) {
        SimpleURI uriStore = new SimpleURI();
        uriStore.setParts(Arrays.asList(uri.getDocPath().split("/")));
        uriStore.setRepo(uri.getAuthority());
        uriStore.setScheme(uri.getScheme().toString());
        ensureClient().prepareIndex(index, SearchRepoType.URI.toString(), uri.getShortPath()).setSource(JacksonUtil.jsonFromObject(uriStore)).get();
    }

    /**
     * Remove this entry from elastic search
     */
    @Override
    public void remove(RaptureURI uri) {
        switch (uri.getScheme()) {
        case SERIES:
            ensureClient().prepareDelete(index, SearchRepoType.SERIES.toString(), uri.getShortPath()).get();
            break;
        case DOCUMENT:
            ensureClient().prepareDelete(index, SearchRepoType.DOC.toString(), uri.toString()).get();
            ensureClient().prepareDelete(index, SearchRepoType.META.toString(), uri.toString()).get();
            break;
        case BLOB:
            ensureClient().prepareDelete(index, SearchRepoType.BLOB.toString(), uri.getShortPath()).get();
            break;
        default:
            log.warn("Called remove() on an unsupported scheme: " + uri.getScheme().toString());
            break;
        }
        ensureClient().prepareDelete(index, SearchRepoType.URI.toString(), uri.getShortPath()).get();
    }

    @Override
    public void dropIndexForRepo(String repoName) {
        // Now the way we can find out the documents to delete is to do a search for "repo:repoName" which will return us the URI search hits
        // We delete those, and can replace URI with META and DOC to delete in the other tables too
        // ensureClient().prepare

        // So do a search with cursor, and page through it..., and do prepareDeletes (multideletes?) after extracting the displayeName
        // ideally on a worker thread, but for now, right here (as we should be on a pipeline thread)

    }

    @Override
    public rapture.common.SearchResponse search(List<String> types, String query) {
        SearchResponse response = ensureClient().prepareSearch().setIndices(index).setTypes(types.toArray(new String[types.size()]))
                .setQuery(QueryBuilders.queryStringQuery(query)).get();
        return convert(response);
    }

    @Override
    public rapture.common.SearchResponse searchForRepoUris(String scheme, String repo, String cursorId) {
        String searchQuery = String.format("scheme:%s AND repo:%s", scheme, repo);
        return searchWithCursor(Arrays.asList(SearchRepoType.URI.toString()), cursorId, 10, searchQuery);
    }

    @Override
    public rapture.common.SearchResponse searchWithCursor(List<String> types, String cursorId, int size, String query) {
        SearchResponse response;
        if (StringUtils.isBlank(cursorId)) {
            response = ensureClient().prepareSearch().setQuery(QueryBuilders.queryStringQuery(query)).setScroll(new TimeValue(CURSOR_KEEPALIVE))
                    .setIndices(index).setTypes(types.toArray(new String[types.size()])).setSize(size).get();
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
     * Convert from scheme.authority and id back to scheme://authority/id
     * 
     * @param type
     * @param id
     * @param map
     * @return
     */
    String getUri(String type, String id, Map<String, Object> map) {
        Scheme scheme = null;

        if (map != null) {
            Object schemeName = map.get("scheme");
            if (schemeName != null) scheme = Scheme.getScheme(schemeName.toString());
        }

        if (scheme == null) {
            switch (SearchRepoType.valueOf(type)) {
            case META:
            case URI:
            case DOC:
                scheme = Scheme.DOCUMENT;
                break;
            case SERIES:
                scheme = Scheme.SERIES;
                break;
            case BLOB:
                scheme = Scheme.BLOB;
                break;
            }
        }
        return new RaptureURI(id, scheme).toShortString();
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
            rHit.setIndexType(hit.getType());
            rHit.setId(hit.getId());
            rHit.setUri(getUri(hit.getType(), hit.getId(), hit.getSource()));
            ret.getSearchHits().add(rHit);
        }
        return ret;
    }

    private void getConnectionInfo() {
        if (StringUtils.isBlank(instanceName)) {
            instanceName = "default";
        }

        Map<String, ConnectionInfo> map = Kernel.getSys().getConnectionInfo(ContextFactory.getKernelUser(), ConnectionType.ES.toString());
        connectionInfo = map.get(instanceName);
        if (connectionInfo == null) {
            throw RaptureExceptionFactory.create("Elastic search for instance " + instanceName + " is not defined.");
        }
        index = connectionInfo.getDbName();
    }

    /**
     * Used for synchronous unit-testing, not to be used for regular code
     */
    RefreshResponse refresh() {
        ActionFuture<RefreshResponse> future = client.admin().indices().refresh(new RefreshRequest(index));
        return future.actionGet(1000);
    }

    @Override
    public void setConfig(Map<String, String> config) {
        setIndex(config.get("index"));
    }
}
