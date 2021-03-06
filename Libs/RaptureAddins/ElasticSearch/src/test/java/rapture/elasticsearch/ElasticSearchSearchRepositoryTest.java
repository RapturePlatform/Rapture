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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;

import rapture.common.BlobUpdateObject;
import rapture.common.DocUpdateObject;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.exception.ExceptionToString;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentWithMeta;
import rapture.common.series.SeriesUpdateObject;
import rapture.search.SearchRepoType;

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
        String json = "{" + "\"user\":\"kimchy\"," + "\"postDate\":\"2014-01-30\"," + "\"message\":\"trying out Elasticsearch\"" + "}";
        RaptureURI uri = new RaptureURI("document://unittest/doc1", Scheme.DOCUMENT);
        DocumentWithMeta d = createDummyDocumentWithMeta(uri.toString(), json);
        e.put(new DocUpdateObject(d));
        e.refresh();
        rapture.common.SearchResponse response = e.search(Arrays.asList(Scheme.DOCUMENT.toString()), "kimchy");
        assertEquals(1, response.getTotal().longValue());
        assertEquals(1, response.getSearchHits().size());
        assertEquals(uri.toShortString(), response.getSearchHits().get(0).getUri());
        assertEquals(uri.toShortString(), response.getSearchHits().get(0).getId());
        assertEquals(json, response.getSearchHits().get(0).getSource());
    }

    @Test
    public void testSearchWithCursor() {
        insertTestDocs();
        int size = 25;
        String query = "u*er*";
        rapture.common.SearchResponse res = e.searchWithCursor(Arrays.asList(Scheme.DOCUMENT.toString()), null, size, query);
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
            res = e.searchWithCursor(Arrays.asList(Scheme.DOCUMENT.toString()), res.getCursorId(), size, query);
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
        SearchResponse response = c.prepareSearch().setExplain(false).setQuery(QueryBuilders.queryStringQuery(query)).setTypes(Scheme.DOCUMENT.toString())
                .setScroll(new TimeValue(60000)).setSize(20).get();
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
        DocumentWithMeta d = createDummyDocumentWithMeta(Scheme.DOCUMENT.toString() + "://" + docPath, "{\"k1\":\"v1\"}");
        e.put(new DocUpdateObject(d));
        e.refresh();
        rapture.common.SearchResponse r = e.search(Arrays.asList(Scheme.DOCUMENT.toString()), "v1");
        assertEquals(1L, r.getTotal().longValue());
        assertEquals(1, r.getSearchHits().size());
        assertEquals("document", r.getSearchHits().get(0).getIndexType());
        assertEquals("document://" + docPath, r.getSearchHits().get(0).getId());
        assertEquals("document://" + docPath, r.getSearchHits().get(0).getUri());
        assertEquals("{\"k1\":\"v1\"}", r.getSearchHits().get(0).getSource());

        r = e.search(Arrays.asList(SearchRepoType.uri.toString()), "d2");
        assertEquals(1L, r.getTotal().longValue());
        assertEquals(1, r.getSearchHits().size());
        assertEquals(SearchRepoType.uri.toString(), r.getSearchHits().get(0).getIndexType());
        assertEquals("document://" + docPath, r.getSearchHits().get(0).getId());
        assertEquals("document://" + docPath, r.getSearchHits().get(0).getUri());
        assertEquals("{\"parts\":[\"d2\",\"d1\"],\"repo\":\"dubnation\",\"scheme\":\"document\"}", r.getSearchHits().get(0).getSource());
    }

    @Test
    public void testScriptPut() {
        String docPath = "somescript/d2/d1";
        DocumentWithMeta d = createDummyDocumentWithMeta(Scheme.SCRIPT.toString() + "://" + docPath, "{\"k1\":\"v1\"}");
        e.put(new DocUpdateObject(d));
        e.refresh();
        rapture.common.SearchResponse r = e.search(Arrays.asList(Scheme.SCRIPT.toString()), "v1");
        assertEquals(1L, r.getTotal().longValue());
        assertEquals(1, r.getSearchHits().size());
        assertEquals("script", r.getSearchHits().get(0).getIndexType());
        assertEquals("script://" + docPath, r.getSearchHits().get(0).getId());
        assertEquals("script://" + docPath, r.getSearchHits().get(0).getUri());
        assertEquals("{\"k1\":\"v1\"}", r.getSearchHits().get(0).getSource());

        r = e.search(Arrays.asList(SearchRepoType.uri.toString()), "d2");
        assertEquals(1L, r.getTotal().longValue());
        assertEquals(1, r.getSearchHits().size());
        assertEquals(SearchRepoType.uri.toString(), r.getSearchHits().get(0).getIndexType());
        assertEquals("script://" + docPath, r.getSearchHits().get(0).getId());
        assertEquals("script://" + docPath, r.getSearchHits().get(0).getUri());
        assertEquals("{\"parts\":[\"d2\",\"d1\"],\"repo\":\"somescript\",\"scheme\":\"script\"}", r.getSearchHits().get(0).getSource());
    }

    @Test
    public void testSeriesPut() {
        String docPath = "testme/x/y";
        SeriesUpdateObject s = new SeriesUpdateObject(docPath, Arrays.asList("k1"), Arrays.asList("v1"));
        e.put(s);
        e.refresh();
        rapture.common.SearchResponse r = e.search(Arrays.asList(Scheme.SERIES.toString()), "v1");
        assertEquals(1L, r.getTotal().longValue());
        assertEquals(1, r.getSearchHits().size());
        assertEquals("series", r.getSearchHits().get(0).getIndexType());
        assertEquals("series://" + docPath, r.getSearchHits().get(0).getId());
        assertEquals("series://" + docPath, r.getSearchHits().get(0).getUri());
        assertEquals("{\"k1\":\"v1\"}", r.getSearchHits().get(0).getSource());
        s = new SeriesUpdateObject("testme/x/y", Arrays.asList("k2"), Arrays.asList("v2"));
        e.put(s);
        e.refresh();
        r = e.search(Arrays.asList(Scheme.SERIES.toString()), "v1");
        assertEquals(1L, r.getTotal().longValue());
        assertEquals(1, r.getSearchHits().size());
        assertEquals("series", r.getSearchHits().get(0).getIndexType());
        assertEquals("series://" + docPath, r.getSearchHits().get(0).getId());
        assertEquals("series://" + docPath, r.getSearchHits().get(0).getUri());
        assertEquals("{\"k1\":\"v1\",\"k2\":\"v2\"}", r.getSearchHits().get(0).getSource());

        r = e.search(Arrays.asList(SearchRepoType.uri.toString()), "x");
        assertEquals(1L, r.getTotal().longValue());
        assertEquals(1, r.getSearchHits().size());
        assertEquals(SearchRepoType.uri.toString(), r.getSearchHits().get(0).getIndexType());
        assertEquals("series://" + docPath, r.getSearchHits().get(0).getId());
        assertEquals("series://" + docPath, r.getSearchHits().get(0).getUri());
        assertEquals("{\"parts\":[\"x\",\"y\"],\"repo\":\"testme\",\"scheme\":\"series\"}", r.getSearchHits().get(0).getSource());
    }

    @Test
    public void testSearchForRepoUris() {
        String authority = "unittest";
        String seriesDocPath = authority + "/x/y";
        String docDocPath = authority + "/doc0";
        SeriesUpdateObject s = new SeriesUpdateObject(seriesDocPath, Arrays.asList("k1", "k2", "k3"), Arrays.asList(1.0, 2.0, 4.0));
        e.put(s);
        insertTestDocs();
        rapture.common.SearchResponse r = e.searchForRepoUris(Scheme.SERIES.toString(), authority, null);
        assertEquals(1L, r.getTotal().longValue());
        assertEquals(1, r.getSearchHits().size());
        assertEquals(SearchRepoType.uri.toString(), r.getSearchHits().get(0).getIndexType());
        assertEquals("series://" + seriesDocPath, r.getSearchHits().get(0).getId());
        assertEquals("series://" + seriesDocPath, r.getSearchHits().get(0).getUri());
        assertEquals("{\"parts\":[\"x\",\"y\"],\"repo\":\"" + authority + "\",\"scheme\":\"series\"}", r.getSearchHits().get(0).getSource());
        r = e.searchForRepoUris(Scheme.DOCUMENT.toString(), authority, null);
        assertEquals(100, r.getTotal().longValue());
        assertEquals(10, r.getSearchHits().size());
        assertEquals(SearchRepoType.uri.toString(), r.getSearchHits().get(0).getIndexType());
        assertEquals("document://" + docDocPath, r.getSearchHits().get(0).getId());
        assertEquals("document://" + docDocPath, r.getSearchHits().get(0).getUri());
        assertEquals("{\"parts\":[\"doc0\"],\"repo\":\"" + authority + "\",\"scheme\":\"document\"}", r.getSearchHits().get(0).getSource());
    }

    @Test
    public void testRemove() {
        insertTestDocs();
        rapture.common.SearchResponse r = e.search(Arrays.asList(Scheme.DOCUMENT.toString()), "trying out Elasticsearch");
        assertEquals(100, r.getTotal().longValue());

        e.remove(new RaptureURI("document://unittest/doc24"));
        e.remove(new RaptureURI("document://unittest/doc69"));
        e.remove(new RaptureURI("document://unittest/doc77"));
        e.refresh();

        r = e.search(Arrays.asList(Scheme.DOCUMENT.toString()), "trying out Elasticsearch");
        assertEquals(97, r.getTotal().longValue());

        SeriesUpdateObject s = new SeriesUpdateObject("removerepo/a/b/c", Arrays.asList("k1", "k2", "k3"), Arrays.asList(1.0, 2.0, 4.0));
        e.put(s);
        e.refresh();

        r = e.search(Arrays.asList(Scheme.SERIES.toString()), "4.0");
        assertEquals(1, r.getTotal().longValue());

        e.remove(new RaptureURI("series://removerepo/a/b/c"));
        e.refresh();

        r = e.search(Arrays.asList(Scheme.SERIES.toString()), "4.0");
        assertEquals(0, r.getTotal().longValue());

        r = e.search(Arrays.asList(Scheme.SERIES.toString()), "v3");
        assertEquals(0, r.getTotal().longValue());
        r = e.search(Arrays.asList(Scheme.DOCUMENT.toString()), "trying out Elasticsearch");
        assertEquals(97, r.getTotal().longValue());
    }

    @Test
    public void testSearchBlob() {
        String premier = "1,Leicester City,36,30,77\n2,Tottenham Hotspur,36,39,70\n3,Arsenal,36,25,67\n4,Manchester City,36,30,64\n5,Manchester United,35,12,60\n"
                + "6,West Ham United,35,17,59\n7,Southampton,36,14,57\n8,Liverpool,35,11,55\n9,Chelsea,35,7,48\n10,Stoke City,36,-14,48\n"
                + "11,Everton,35,6,44\n12,Watford,35,-6,44\n13,Swansea City,36,-13,43\n14,West Bromwich Albion,36,-14,41\n15,Bournemouth,36,-20,41\n1"
                + "6,Crystal Palace,36,-10,39\n17,Newcastle United,36,-25,33\n18,Sunderland,35,-18,32\n19,Norwich City,35,-26,31\n20,Aston Villa,36,-45,16\n";

        String championship = "1,Burnley,45,34,90\n2,Middlesbrough,45,32,88\n3,Brighton & Hove Albion,45,30,88\n4,Hull City,45,30,80\n"
                + "5,Derby County,45,24,78\n6,Sheffield Wednesday,45,22,74\n7,Cardiff City,45,5,67\n8,Ipswich Town,45,1,66\n9,Birmingham City,45,4,62\n"
                + "10,Brentford,45,1,62\n11,Preston North End,45,0,61\n12,Leeds United,45,-8,58\n13,Queens Park Rangers,45,-1,57\n14,Wolverhampton Wanderers,45,-6,55\n"
                + "15,Blackburn Rovers,45,-2,52\n16,Reading,45,-5,52\n17,Nottingham Forest,45,-5,52\n18,Bristol City,45,-16,52\n19,Huddersfield Town,45,-7,51\n"
                + "20,Rotherham United,45,-14,49\n21,Fulham,45,-14,48\n22,Charlton Athletic,45,-37,40\n23,Milton Keynes Dons,45,-29,39\n24,Bolton Wanderers,45,-39,30\n";

        // @SuppressWarnings("unchecked")
        // ImmutableList<ImmutableList<String>> leagueList = ImmutableList.of(
        // ImmutableList.of("1", "Leicester", "36", "30", "77"),
        // ImmutableList.of("2", "Tottenham", "36", "39", "70"),
        // ImmutableList.of("3", "Arsenal", "36", "25", "67"),
        // ImmutableList.of("4", "Man City", "36", "30", "64"),
        // ImmutableList.of("5", "Man Utd", "35", "12", "60"),
        // ImmutableList.of("6", "West Ham", "35", "17", "59"),
        // ImmutableList.of("7", "Southampton", "36", "14", "57"),
        // ImmutableList.of("8", "Liverpool", "35", "11", "55"),
        // ImmutableList.of("9", "Chelsea", "35", "7", "48"),
        // ImmutableList.of("10", "Stoke", "36", "-14", "48"),
        // ImmutableList.of("11", "Everton", "35", "6", "44"),
        // ImmutableList.of("12", "Watford", "35", "-6", "44"),
        // ImmutableList.of("13", "Swansea", "36", "-13", "43"),
        // ImmutableList.of("14", "West Brom", "36", "-14", "41"),
        // ImmutableList.of("15", "Bournemouth", "36", "-20", "41"),
        // ImmutableList.of("16", "Crystal Palace", "36", "-10", "39"),
        // ImmutableList.of("17", "Newcastle", "36", "-25", "33"),
        // ImmutableList.of("18", "Sunderland", "35", "-18", "32"),
        // ImmutableList.of("19", "Norwich", "35", "-26", "31"),
        // ImmutableList.of("20", "Aston Villa", "36", "-45", "16"));
        // String leagueJson = JacksonUtil.jsonFromObject(leagueList);

        // Try reading a real PDF from a file

        File pdf = new File("src/test/resources/www-bbc-com.pdf");
        RaptureURI firstDiv = new RaptureURI.Builder(Scheme.BLOB, "unittest").docPath("English/First").build();
        try {
            byte[] content = Files.readAllBytes(pdf.toPath());
            e.put(new BlobUpdateObject(firstDiv, content, MediaType.PDF.toString()));
        } catch (IOException e2) {
            fail(pdf.getAbsolutePath() + " : " + ExceptionToString.format(e2));
        }

        Map<String, String> copout = new HashMap<>();
        copout.put("premier", premier);
        String leagueJson = JacksonUtil.jsonFromObject(copout);
        RaptureURI epl = new RaptureURI.Builder(Scheme.BLOB, "unittest").docPath("English/Premier").build();
        e.put(new BlobUpdateObject(epl, leagueJson.getBytes(), MediaType.JSON_UTF_8.toString()));
        e.refresh();

        RaptureURI champ = new RaptureURI.Builder(Scheme.BLOB, "unittest").docPath("English/Championship").build();
        e.put(new BlobUpdateObject(champ, championship.getBytes(), MediaType.CSV_UTF_8.toString()));
        e.refresh();

        rapture.common.SearchResponse r = e.searchForRepoUris(Scheme.BLOB.toString(), epl.getAuthority(), null);
        assertEquals(3L, r.getTotal().longValue());
        assertEquals(3, r.getSearchHits().size());
        rapture.common.SearchHit hit = null;
        for (rapture.common.SearchHit h : r.getSearchHits()) {
            if (h.getUri().contains("Premier")) {
                hit = h;
                break;
            }
        }
        assertNotNull(hit);
        assertEquals(SearchRepoType.uri.toString(), hit.getIndexType());
        assertEquals(epl.toShortString(), hit.getId());
        assertEquals(epl.toString(), hit.getUri());
        assertEquals("{\"parts\":[\"English\",\"Premier\"],\"repo\":\"" + epl.getAuthority() + "\",\"scheme\":\"blob\"}", hit.getSource());
        
        r = e.searchForRepoUris(Scheme.BLOB.toString(), epl.getAuthority(), null);
        assertEquals(3, r.getSearchHits().size());
        for (rapture.common.SearchHit h : r.getSearchHits()) {
            if (h.getUri().contains("Premier")) {
                hit = h;
                break;
            }
        }
        assertNotNull(hit);
        assertEquals(SearchRepoType.uri.toString(), hit.getIndexType());
        assertEquals(epl.toShortString(), hit.getId());
        assertEquals(epl.toString(), hit.getUri());
        assertEquals("{\"parts\":[\"English\",\"Premier\"],\"repo\":\"" + epl.getAuthority() + "\",\"scheme\":\"blob\"}", hit.getSource());

        // Search inside the blob

        String query = "blob:*City";
        rapture.common.SearchResponse res = e.searchWithCursor(Arrays.asList(""), null, 10, query);
        assertNotNull(res.getCursorId());
        // The PDF has no data yet so shouldn't match
        assertEquals(2, res.getSearchHits().size());
        assertNotNull(res.getMaxScore());

        // Can we assume anything about the ordering?

        for (rapture.common.SearchHit h : res.getSearchHits()) {
            assertTrue(h.getUri().startsWith("blob://unittest/English/"));
        }

        int i;
        query = "blob:*Wigan*";
        for (i = 0; i < 10; i++) {
            res = e.searchWithCursor(null, null, 10, query);
            if ((res != null) && (res.getSearchHits().size() > 0)) break;
            try {
                // Tika parsing may not have completed yet.
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
            }
            e.refresh();
        }
        assertNotNull(res.getCursorId());
        assertEquals(1, res.getSearchHits().size());

        e.remove(new RaptureURI(firstDiv.toString()));
        e.refresh();
        res = e.searchWithCursor(ImmutableList.of(""), null, 10, query);
        assertEquals(0, res.getSearchHits().size());
    }

    private void insertTestDocs() {
        for (int i = 0; i < 100; i++) {
            String json = "{" + "\"user\":\"user" + i + "\"," + "\"postDate\":\"2014-01-30\"," + "\"message\":\"trying out Elasticsearch\"" + "}";
            RaptureURI uri = new RaptureURI("document://unittest/doc" + i, Scheme.DOCUMENT);
            DocumentWithMeta d = createDummyDocumentWithMeta(uri.toString(), json);
            e.put(new DocUpdateObject(d));
        }
        e.refresh();
    }

    private DocumentWithMeta createDummyDocumentWithMeta(String semanticUri, String json) {
        DocumentWithMeta d = new DocumentWithMeta();
        DocumentMetadata dm = new DocumentMetadata();
        dm.setComment("comment");
        dm.setUser("user");
        dm.setVersion(1);
        dm.setSemanticUri(semanticUri);
        d.setMetaData(dm);
        d.setDisplayName(semanticUri);
        d.setContent(json);
        return d;
    }

    @After
    public void teardown() {
        es.shutdown();
    }
}
