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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rapture.common.RaptureURI;
import rapture.common.Scheme;

public class ElasticSearchSearchRepositoryTest {

    private ElasticSearchSearchRepository e;
    private EmbeddedServer es;

    @Before
    public void setup() {
        e = new ElasticSearchSearchRepository();
        e.setIndex("unittest");
        es = new EmbeddedServer();
        e.setClient(es.getClient());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail(e.toString());
        }
    }

    @Test
    public void testEasySearch() {
        String json = "{" +
                "\"user\":\"kimchy\"," +
                "\"postDate\":\"2014-01-30\"," +
                "\"message\":\"trying out Elasticsearch\"" +
                "}";
        RaptureURI uri = new RaptureURI("document://unittest/doc1", Scheme.DOCUMENT);
        // TODO:
        //e.put(uri, json);
        //assertEquals(json, e.get(uri));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail(e.toString());
        }
        rapture.common.SearchResponse response = e.search(Arrays.asList("test"), "kimchy");
        assertEquals(1, response.getTotal().longValue());
        assertEquals(1, response.getSearchHits().size());
        assertEquals(uri.toString(), response.getSearchHits().get(0).getUri());
        assertEquals(json, response.getSearchHits().get(0).getSource());
    }

    @Test
    public void testGetUri() {
        assertEquals("document://unittest/doc38", e.getUri("document.unittest", "doc38"));
        assertEquals("document://unittest.testme/doc38", e.getUri("document.unittest.testme", "doc38"));

    }

    @Test
    public void testGetType() {
        assertEquals("document.unittest", e.getType(new RaptureURI("document://unittest")));
        assertEquals("document.unittest.kk", e.getType(new RaptureURI("document://unittest.kk")));
        assertEquals("document.unittest", e.getType(new RaptureURI("document://unittest/kk")));
        assertEquals("document.unittest", e.getType(new RaptureURI("unittest/kk", Scheme.DOCUMENT)));
    }

    @Test
    public void testSearchWithCursor() {
        insertTestDocs();
        int size = 25;
        String query = "u*er*";
        rapture.common.SearchResponse res = e.searchWithCursor(Arrays.asList("test"), null, size, query);
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
            res = e.searchWithCursor(Arrays.asList("test"), res.getCursorId(), size, query);
            if (res.getSearchHits().size() == 0) {
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
    public void testSearchAndConvert() throws IOException {
        insertTestDocs();
        String query = "*";
        Client c = es.getClient();
        SearchResponse response = c.prepareSearch().setExplain(false)
                .setQuery(QueryBuilders.queryStringQuery(query))
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

    private void insertTestDocs() {
        for (int i = 0; i < 100; i++) {
            String json = "{" +
                    "\"user\":\"user" + i + "\"," +
                    "\"postDate\":\"2014-01-30\"," +
                    "\"message\":\"trying out Elasticsearch\"" +
                    "}";
            RaptureURI uri = new RaptureURI("document://unittest/doc" + i, Scheme.DOCUMENT);
            //e.put(uri, json);
            //assertEquals(json, e.get(uri));
        }

        for (int i = 0; i < 100; i++) {
            String json = "{\n" +
                    "    \"id\": " + i + ",\n" +
                    "    \"name\": \"A green door\",\n" +
                    "    \"price\": 12.50,\n" +
                    "    \"tags\": [\"home\", \"green\"]\n" +
                    "}";
            RaptureURI uri = new RaptureURI("document://unittest/otherstuff" + i, Scheme.DOCUMENT);
           // e.put(uri, json);
           // assertEquals(json, e.get(uri));
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail(e.toString());
        }
    }

    @After
    public void teardown() {
        es.shutdown();
    }
}
