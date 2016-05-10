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

import com.google.common.collect.ImmutableMap;
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
        String json = "{" + "\"user\":\"kimchy\"," + "\"postDate\":\"2014-01-30\"," + "\"message\":\"trying out Elasticsearch\"" + "}";
        RaptureURI uri = new RaptureURI("document://unittest/doc1", Scheme.DOCUMENT);
        DocumentWithMeta d = createDummyDocumentWithMeta(uri.getFullPath(), json);
        e.put(new DocUpdateObject(d));
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
        SearchResponse response = c.prepareSearch().setExplain(false).setQuery(QueryBuilders.queryStringQuery(query)).setTypes(SearchRepoType.DOC.toString())
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
        DocumentWithMeta d = createDummyDocumentWithMeta(docPath, "{\"k1\":\"v1\"}");
        e.put(new DocUpdateObject(d));
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
        // getId used to return docPath, now it returns series://docpath -
        // this new behaviour seems to be consistent with document (see above)
        // so I'm not considering it to be a regression.
        assertEquals("series://" + docPath, r.getSearchHits().get(0).getId());
        assertEquals("series://" + docPath, r.getSearchHits().get(0).getUri());
        assertEquals("{\"k1\":\"v1\"}", r.getSearchHits().get(0).getSource());
        s = new SeriesUpdateObject("testme/x/y", Arrays.asList("k2"), Arrays.asList("v2"));
        e.put(s);
        e.refresh();
        r = e.search(Arrays.asList(SearchRepoType.SERIES.toString()), "v1");
        assertEquals(1L, r.getTotal().longValue());
        assertEquals(1, r.getSearchHits().size());
        assertEquals(SearchRepoType.SERIES.toString(), r.getSearchHits().get(0).getIndexType());
        // getId used to return docPath, now it returns series://docpath -
        // this new behaviour seems to be consistent with document (see above)
        // so I'm not considering it to be a regression.
        assertEquals("series://" + docPath, r.getSearchHits().get(0).getId());
        assertEquals("series://" + docPath, r.getSearchHits().get(0).getUri());
        assertEquals("{\"k1\":\"v1\",\"k2\":\"v2\"}", r.getSearchHits().get(0).getSource());

        r = e.search(Arrays.asList(SearchRepoType.URI.toString()), "x");
        assertEquals(1L, r.getTotal().longValue());
        assertEquals(1, r.getSearchHits().size());
        assertEquals(SearchRepoType.URI.toString(), r.getSearchHits().get(0).getIndexType());
        // Hmm. Inconsistent.
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

        // Unfortunately it seems that ElasticSearch requires a JSON document.
        // It won't accept a JSON list or a CSV. So we have to munge it.

        // This is a minimal PDF. Next step is to have one with data in it and show that we can index the contents
        String minimalPdf = "%PDF-1.0\n"
                + "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj 3 0 obj<</Type/Page/MediaBox[0 0 3 3]>>endobj\n"
                + "xref\n" + "0 4\n" + "0000000000 65535 f\n" + "0000000010 00000 n\n" + "0000000053 00000 n\n" + "0000000102 00000 n\n"
                + "trailer<</Size 4/Root 1 0 R>>\n" + "startxref\n" + "149\n" + "%EOF\n";

        Map<String, String> copout = new HashMap<>();
        copout.put("premier", premier);
        String leagueJson = JacksonUtil.jsonFromObject(copout);
        RaptureURI epl = new RaptureURI.Builder(Scheme.BLOB, "unittest").docPath("English/Premier").build();
        e.put(new BlobUpdateObject(epl, leagueJson.getBytes(), MediaType.JSON_UTF_8.toString()));
        // e.put(new BlobUpdateObject(epl, premier.getBytes(), MediaType.JSON_UTF_8.toString()));

        RaptureURI champ = new RaptureURI.Builder(Scheme.BLOB, "unittest").docPath("English/Championship").build();
        e.put(new BlobUpdateObject(champ, championship.getBytes(), MediaType.CSV_UTF_8.toString()));


        // Try reading a real PDF from a file

        File pdf = new File("src/test/resources/www-bbc-com.pdf");
        try {
            byte[] content = Files.readAllBytes(pdf.toPath());
            RaptureURI firstDiv = new RaptureURI.Builder(Scheme.BLOB, "unittest").docPath("English/First").build();
            e.put(new BlobUpdateObject(firstDiv, content, MediaType.PDF.toString()));
        } catch (IOException e2) {
            fail(pdf.getAbsolutePath() + " : " + ExceptionToString.format(e2));
        }

        File pdf2 = new File("src/test/resources/HelloWorld.pdf");
        try {
            byte[] content = Files.readAllBytes(pdf2.toPath());
            RaptureURI hello = new RaptureURI.Builder(Scheme.BLOB, "Hello").docPath("Hello/Wurld").build();
            e.put(new BlobUpdateObject(hello, content, MediaType.PDF.toString()));
        } catch (IOException e3) {
            fail(pdf2.getAbsolutePath() + " : " + ExceptionToString.format(e3));
        }

        try {
            // Tika parsing happens in a separate thread so give it a chance
            Thread.sleep(3000);
        } catch (InterruptedException e1) {
        }

        rapture.common.SearchResponse r = e.searchForRepoUris(Scheme.BLOB.toString(), epl.getAuthority(), null);
        assertEquals(3L, r.getTotal().longValue());
        assertEquals(3, r.getSearchHits().size());
        assertEquals(SearchRepoType.URI.toString(), r.getSearchHits().get(0).getIndexType());
        assertEquals(epl.getShortPath(), r.getSearchHits().get(0).getId());
        assertEquals(epl.toString(), r.getSearchHits().get(0).getUri());
        assertEquals("{\"parts\":[\"English\",\"Premier\"],\"repo\":\"" + epl.getAuthority() + "\",\"scheme\":\"blob\"}", r.getSearchHits().get(0).getSource());
        r = e.searchForRepoUris(Scheme.BLOB.toString(), epl.getAuthority(), null);
        assertEquals(3, r.getSearchHits().size());
        assertEquals(SearchRepoType.URI.toString(), r.getSearchHits().get(0).getIndexType());
        assertEquals(epl.getShortPath(), r.getSearchHits().get(0).getId());
        assertEquals(epl.toString(), r.getSearchHits().get(0).getUri());
        assertEquals("{\"parts\":[\"English\",\"Premier\"],\"repo\":\"" + epl.getAuthority() + "\",\"scheme\":\"blob\"}", r.getSearchHits().get(0).getSource());

        // Search inside the blob

        String query = "blob:*City";
        rapture.common.SearchResponse res = e.searchWithCursor(Arrays.asList(SearchRepoType.BLOB.toString()), null, 10, query);
        assertNotNull(res.getCursorId());
        // The PDF has no data yet so shouldn't match
        assertEquals(2, res.getSearchHits().size());
        assertNotNull(res.getMaxScore());

        // Can we assume anything about the ordering?

        for (int i = 0; i < res.getSearchHits().size(); i++) {
            rapture.common.SearchHit hit = res.getSearchHits().get(i);
            assertTrue(hit.getUri().startsWith("blob://unittest/English/"));
        }

        query = "blob:*Wigan*";
        res = e.searchWithCursor(SearchRepoType.valuesAsList(), null, 10, query);
        assertNotNull(res.getCursorId());
        assertEquals(1, res.getSearchHits().size());

        query = "blob:*World*";
        res = e.searchWithCursor(SearchRepoType.valuesAsList(), null, 10, query);
        assertNotNull(res.getCursorId());
        assertEquals(1, res.getSearchHits().size());
    }

    private void insertTestDocs() {
        for (int i = 0; i < 100; i++) {
            String json = "{" + "\"user\":\"user" + i + "\"," + "\"postDate\":\"2014-01-30\"," + "\"message\":\"trying out Elasticsearch\"" + "}";
            RaptureURI uri = new RaptureURI("document://unittest/doc" + i, Scheme.DOCUMENT);
            DocumentWithMeta d = createDummyDocumentWithMeta(uri.getFullPath(), json);
            e.put(new DocUpdateObject(d));
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
