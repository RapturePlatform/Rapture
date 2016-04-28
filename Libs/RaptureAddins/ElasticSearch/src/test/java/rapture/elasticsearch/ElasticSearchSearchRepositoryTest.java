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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentWithMeta;
import rapture.common.series.SeriesUpdateObject;
import rapture.kernel.search.SearchRepoType;

public class ElasticSearchSearchRepositoryTest {

    private ElasticSearchSearchRepository e;
    private EmbeddedServer es;

    @Before
    public void setup() {
        e = new ElasticSearchSearchRepository();
        e.setIndex("unittest");
        es = new EmbeddedServer();
        e.setClient(es.getClient());
    }

    @Test
    public void testEasySearch() {
        String json = "{" +
                "\"user\":\"kimchy\"," +
                "\"postDate\":\"2014-01-30\"," +
                "\"message\":\"trying out Elasticsearch\"" +
                "}";
        RaptureURI uri = new RaptureURI("document://unittest/doc1", Scheme.DOCUMENT);
        DocumentWithMeta d = createDummyDocumentWithMeta(uri.getFullPath(), json);
        e.put(d);
        e.refresh();
        rapture.common.SearchResponse response = e.search(Arrays.asList(SearchRepoType.DOC.toString()), "kimchy");
        assertEquals(1, response.getTotal().longValue());
        assertEquals(1, response.getSearchHits().size());
        assertEquals(uri.toString(), response.getSearchHits().get(0).getUri());
        assertEquals(json, response.getSearchHits().get(0).getSource());
    }

    @Test
    public void testGetUri() {
        assertEquals("document://unittest/doc38", e.getUri(SearchRepoType.DOC.toString(), "unittest/doc38", null));
        assertEquals("document://unittest.testme/doc38", e.getUri(SearchRepoType.DOC.toString(), "unittest.testme/doc38", null));
        assertEquals("series://a/b/c/d", e.getUri(SearchRepoType.SERIES.toString(), "a/b/c/d", null));
        assertEquals("document://already/there", e.getUri(SearchRepoType.URI.toString(), "already/there", ImmutableMap.of("a", "b", "scheme", "document")));
    }

    @Test
    public void testSearchWithCursor() {
        insertTestDocs();
        int size = 25;
        String query = "u*er*";
        rapture.common.SearchResponse res = e.searchWithCursor(Arrays.asList(SearchRepoType.DOC.toString()), null, size, query);
        assertNotNull(res.getCursorId());
        assertEquals(25, res.getSearchHits().size());
        assertEquals(100, res.getTotal().longValue());
        assertNotNull(res.getMaxScore());
        for (int i = 0; i < res.getSearchHits().size(); i++) {
            rapture.common.SearchHit hit = res.getSearchHits().get(i);
            assertEquals("document://unittest/doc" + i, hit.getUri());
        }
        int counter = size;
        while (true) {
            res = e.searchWithCursor(Arrays.asList(SearchRepoType.DOC.toString()), res.getCursorId(), size, query);
            if (res.getSearchHits().isEmpty()) {
                break;
            }
            assertNotNull(res.getCursorId());
            assertEquals(25, res.getSearchHits().size());
            assertEquals(100, res.getTotal().longValue());
            assertNotNull(res.getMaxScore());
            for (int i = 0; i < res.getSearchHits().size(); i++) {
                rapture.common.SearchHit hit = res.getSearchHits().get(i);
                assertEquals("document://unittest/doc" + (i + counter), hit.getUri());
            }
            counter += size;
        }
    }

    @Test
    public void testSearchAndConvert() {
        insertTestDocs();
        String query = "*";
        Client c = es.getClient();
        SearchResponse response = c.prepareSearch().setExplain(false)
                .setQuery(QueryBuilders.queryStringQuery(query))
                .setTypes(SearchRepoType.DOC.toString())
                .setScroll(new TimeValue(60000))
                .setSize(20)
                .get();
        rapture.common.SearchResponse ret = e.convert(response);
        assertEquals(response.getHits().getTotalHits(), ret.getTotal().longValue());
        assertEquals(response.getScrollId(), ret.getCursorId());
        assertEquals(new Double(response.getHits().getMaxScore()), ret.getMaxScore());
        assertEquals(response.getHits().hits().length, ret.getSearchHits().size());
        for (int i = 0; i < ret.getSearchHits().size(); i++) {
            rapture.common.SearchHit hit = ret.getSearchHits().get(i);
            SearchHit eHit = response.getHits().hits()[i];
            assertEquals(new Double(Double.parseDouble(Float.toString(eHit.getScore()))), hit.getScore());
            assertEquals(eHit.getSourceAsString(), hit.getSource());
            assertEquals("document://unittest/doc" + i, hit.getUri());
        }
    }

    @Test
    public void testDocPut() {
        String docPath = "dubnation/d2/d1";
        DocumentWithMeta d = createDummyDocumentWithMeta(docPath, "{\"k1\":\"v1\"}");
        e.put(d);
        e.refresh();
        rapture.common.SearchResponse r = e.search(Arrays.asList(SearchRepoType.DOC.toString()), "v1");
        assertEquals(1L, r.getTotal().longValue());
        assertEquals(1, r.getSearchHits().size());
        assertEquals(SearchRepoType.DOC.toString(), r.getSearchHits().get(0).getIndexType());
        assertEquals("document://" + docPath, r.getSearchHits().get(0).getId());
        assertEquals("document://" + docPath, r.getSearchHits().get(0).getUri());
        assertEquals("{\"k1\":\"v1\"}", r.getSearchHits().get(0).getSource());

        r = e.search(Arrays.asList(SearchRepoType.URI.toString()), "d2");
        assertEquals(1L, r.getTotal().longValue());
        assertEquals(1, r.getSearchHits().size());
        assertEquals(SearchRepoType.URI.toString(), r.getSearchHits().get(0).getIndexType());
        assertEquals(docPath, r.getSearchHits().get(0).getId());
        assertEquals("document://" + docPath, r.getSearchHits().get(0).getUri());
        assertEquals("{\"parts\":[\"d2\",\"d1\"],\"repo\":\"dubnation\",\"scheme\":\"document\"}", r.getSearchHits().get(0).getSource());
    }

    @Test
    public void testSeriesPut() {
        String docPath = "testme/x/y";
        SeriesUpdateObject s = new SeriesUpdateObject(docPath, Arrays.asList("k1"), Arrays.asList("v1"));
        e.put(s);
        e.refresh();
        rapture.common.SearchResponse r = e.search(Arrays.asList(SearchRepoType.SERIES.toString()), "v1");
        assertEquals(1L, r.getTotal().longValue());
        assertEquals(1, r.getSearchHits().size());
        assertEquals(SearchRepoType.SERIES.toString(), r.getSearchHits().get(0).getIndexType());
        assertEquals(docPath, r.getSearchHits().get(0).getId());
        assertEquals("series://" + docPath, r.getSearchHits().get(0).getUri());
        assertEquals("{\"k1\":\"v1\"}", r.getSearchHits().get(0).getSource());
        s = new SeriesUpdateObject("testme/x/y", Arrays.asList("k2"), Arrays.asList("v2"));
        e.put(s);
        e.refresh();
        r = e.search(Arrays.asList(SearchRepoType.SERIES.toString()), "v1");
        assertEquals(1L, r.getTotal().longValue());
        assertEquals(1, r.getSearchHits().size());
        assertEquals(SearchRepoType.SERIES.toString(), r.getSearchHits().get(0).getIndexType());
        assertEquals(docPath, r.getSearchHits().get(0).getId());
        assertEquals("series://" + docPath, r.getSearchHits().get(0).getUri());
        assertEquals("{\"k1\":\"v1\",\"k2\":\"v2\"}", r.getSearchHits().get(0).getSource());

        r = e.search(Arrays.asList(SearchRepoType.URI.toString()), "x");
        assertEquals(1L, r.getTotal().longValue());
        assertEquals(1, r.getSearchHits().size());
        assertEquals(SearchRepoType.URI.toString(), r.getSearchHits().get(0).getIndexType());
        assertEquals(docPath, r.getSearchHits().get(0).getId());
        assertEquals("series://" + docPath, r.getSearchHits().get(0).getUri());
        assertEquals("{\"parts\":[\"x\",\"y\"],\"repo\":\"testme\",\"scheme\":\"series\"}", r.getSearchHits().get(0).getSource());
    }

    @Test
    public void testSearchForRepoUris() {
        String repo = "unittest";
        String seriesDocPath = repo + "/x/y";
        String docDocPath = repo + "/doc0";
        SeriesUpdateObject s = new SeriesUpdateObject(seriesDocPath, Arrays.asList("k1", "k2", "k3"), Arrays.asList(1.0, 2.0, 4.0));
        e.put(s);
        insertTestDocs();
        rapture.common.SearchResponse r = e.searchForRepoUris(Scheme.SERIES.toString(), repo, null);
        assertEquals(1L, r.getTotal().longValue());
        assertEquals(1, r.getSearchHits().size());
        assertEquals(SearchRepoType.URI.toString(), r.getSearchHits().get(0).getIndexType());
        assertEquals(seriesDocPath, r.getSearchHits().get(0).getId());
        assertEquals("series://" + seriesDocPath, r.getSearchHits().get(0).getUri());
        assertEquals("{\"parts\":[\"x\",\"y\"],\"repo\":\"" + repo + "\",\"scheme\":\"series\"}", r.getSearchHits().get(0).getSource());
        r = e.searchForRepoUris(Scheme.DOCUMENT.toString(), repo, null);
        assertEquals(100, r.getTotal().longValue());
        assertEquals(10, r.getSearchHits().size());
        assertEquals(SearchRepoType.URI.toString(), r.getSearchHits().get(0).getIndexType());
        assertEquals(docDocPath, r.getSearchHits().get(0).getId());
        assertEquals("document://" + docDocPath, r.getSearchHits().get(0).getUri());
        assertEquals("{\"parts\":[\"doc0\"],\"repo\":\"" + repo + "\",\"scheme\":\"document\"}", r.getSearchHits().get(0).getSource());
    }

    @Test
    public void testRemove() {
        insertTestDocs();
        rapture.common.SearchResponse r = e.search(Arrays.asList(SearchRepoType.DOC.toString()), "trying out Elasticsearch");
        assertEquals(100, r.getTotal().longValue());

        e.remove(new RaptureURI("document://unittest/doc24"));
        e.remove(new RaptureURI("document://unittest/doc69"));
        e.remove(new RaptureURI("document://unittest/doc77"));
        e.refresh();

        r = e.search(Arrays.asList(SearchRepoType.DOC.toString()), "trying out Elasticsearch");
        assertEquals(97, r.getTotal().longValue());

        SeriesUpdateObject s = new SeriesUpdateObject("removerepo/a/b/c", Arrays.asList("k1", "k2", "k3"), Arrays.asList(1.0, 2.0, 4.0));
        e.put(s);
        e.refresh();

        r = e.search(Arrays.asList(SearchRepoType.SERIES.toString()), "4.0");
        assertEquals(1, r.getTotal().longValue());

        e.remove(new RaptureURI("series://removerepo/a/b/c"));
        e.refresh();

        r = e.search(Arrays.asList(SearchRepoType.SERIES.toString()), "v3");
        assertEquals(0, r.getTotal().longValue());
        r = e.search(Arrays.asList(SearchRepoType.DOC.toString()), "trying out Elasticsearch");
        assertEquals(97, r.getTotal().longValue());

    }

    private void insertTestDocs() {
        for (int i = 0; i < 100; i++) {
            String json = "{" +
                    "\"user\":\"user" + i + "\"," +
                    "\"postDate\":\"2014-01-30\"," +
                    "\"message\":\"trying out Elasticsearch\"" +
                    "}";
            RaptureURI uri = new RaptureURI("document://unittest/doc" + i, Scheme.DOCUMENT);
            DocumentWithMeta d = createDummyDocumentWithMeta(uri.getFullPath(), json);
            e.put(d);
        }
        e.refresh();
    }

    private DocumentWithMeta createDummyDocumentWithMeta(String uriDocPath, String json) {
        DocumentWithMeta d = new DocumentWithMeta();
        DocumentMetadata dm = new DocumentMetadata();
        dm.setComment("comment");
        dm.setUser("user");
        dm.setVersion(1);
        d.setMetaData(dm);
        d.setDisplayName(uriDocPath);
        d.setContent(json);
        return d;
    }

    @After
    public void teardown() {
        es.shutdown();
    }
}
