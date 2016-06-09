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
package rapture.nightly.java.search;

import static org.junit.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;

import rapture.common.CallingContext;
import rapture.common.RaptureScript;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.SearchHit;
import rapture.common.SearchResponse;
import rapture.common.SeriesPoint;
import rapture.common.client.HttpBlobApi;
import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.HttpScriptApi;
import rapture.common.client.HttpSearchApi;
import rapture.common.client.HttpSeriesApi;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.config.ConfigLoader;
import rapture.nightly.IntegrationTestHelper;
import rapture.nightly.QueryWithRetry;
import rapture.search.SearchRepoType;

/**
 * Tests to exercise Full Text Search. Note: We should be able to use any repo type; Memory, Mongo, Postgres etc. as the purpose of the test is to verify that
 * the API methods successfully interact with ElasticSearch;
 */

public class SearchApiIntegrationTest {

    private HttpLoginApi raptureLogin = null;
    private HttpSeriesApi seriesApi = null;
    private HttpScriptApi scriptApi = null;
    private HttpSearchApi searchApi = null;
    private HttpDocApi docApi = null;
    private HttpBlobApi blobApi = null;
    CallingContext callingContext = null;
    boolean cleanUpPrevious = true;
    IntegrationTestHelper helper;

    /**
     * Setup TestNG method to create Rapture login object and objects.
     *
     * @param RaptureURL
     *            Passed in from <env>_testng.xml suite file
     * @param RaptureUser
     *            Passed in from <env>_testng.xml suite file
     * @param RapturePassword
     *            Passed in from <env>_testng.xml suite file
     * @return none
     */
    @BeforeClass(groups = { "nightly", "search" })
    @Parameters({ "RaptureURL", "RaptureUser", "RapturePassword" })
    public void setUp(@Optional("http://localhost:8665/rapture") String url, @Optional("rapture") String username, @Optional("rapture") String password) {

        // If running from eclipse set env var -Penv=docker or use the following
        // url variable settings:
        // url="http://192.168.99.101:8665/rapture"; //docker
        // url="http://localhost:8665/rapture";

        helper = new IntegrationTestHelper(url, username, password);
        raptureLogin = helper.getRaptureLogin();
        seriesApi = helper.getSeriesApi();
        scriptApi = helper.getScriptApi();
        docApi = helper.getDocApi();
        blobApi = helper.getBlobApi();
        searchApi = new HttpSearchApi(raptureLogin);
        callingContext = raptureLogin.getContext();
        forceCleanUp();
    }

    @AfterMethod(groups = { "nightly", "search" })
    public void afterMethod() {
        helper.cleanAllAssets();
    }

    /**
     * TestNG method to cleanup.
     *
     * @param none
     * @return none
     */
    @AfterClass(groups = { "nightly", "search" })
    public void afterTest() {
        helper.cleanAllAssets();
        raptureLogin = null;
    }

    // For cleaning up after failed test cases only
    public void forceCleanUp() {
        SearchResponse res = searchApi.qualifiedSearch(ConfigLoader.getConf().FullTextSearchDefaultRepo, ImmutableList.of(SearchRepoType.meta.toString()),
                "user:rapture");

        for (String uri : uris(res)) {
            System.out.println("FORCE CLEAN UP: Deleted " + uri);
            if (uri.startsWith("script")) {
                scriptApi.deleteScript(uri);
            } else {
                RaptureURI ruri = new RaptureURI(uri);
                helper.cleanTestRepo(new RaptureURI(ruri.getAuthority(), ruri.getScheme()));
            }
        }

        res = searchApi.searchWithCursor(null, null, 10, "blob:*Utd");
        for (String uri : uris(res)) {
            System.out.println("FORCE CLEAN UP: Deleted " + uri);
            RaptureURI ruri = new RaptureURI(uri);
            helper.cleanTestRepo(new RaptureURI(ruri.getAuthority(), ruri.getScheme()));
        }
    }

    static String premier = "1,Leicester City,36,30,77\n2,Tottenham Hotspur,36,39,70\n3,Arsenal,36,25,67\n4,Manchester City,36,30,64\n5,Manchester Utd,35,12,60\n"
            + "6,West Ham Utd,35,17,59\n7,Southampton,36,14,57\n8,Liverpool,35,11,55\n9,Chelsea,35,7,48\n10,Stoke City,36,-14,48\n"
            + "11,Everton,35,6,44\n12,Watford,35,-6,44\n13,Swansea City,36,-13,43\n14,West Bromwich Albion,36,-14,41\n15,Bournemouth,36,-20,41\n1"
            + "6,Crystal Palace,36,-10,39\n17,Newcastle Utd,36,-25,33\n18,Sunderland,35,-18,32\n19,Norwich City,35,-26,31\n20,Aston Villa,36,-45,16\n";

    static String championship = "1,Burnley,45,34,90\n2,Middlesbrough,45,32,88\n3,Brighton & Hove Albion,45,30,88\n4,Hull City,45,30,80\n"
            + "5,Derby County,45,24,78\n6,Sheffield Wednesday,45,22,74\n7,Cardiff City,45,5,67\n8,Ipswich Town,45,1,66\n9,Birmingham City,45,4,62\n"
            + "10,Brentford,45,1,62\n11,Preston North End,45,0,61\n12,Leeds Utd,45,-8,58\n13,Queens Park Rangers,45,-1,57\n14,Wolverhampton Wanderers,45,-6,55\n"
            + "15,Blackburn Rovers,45,-2,52\n16,Reading,45,-5,52\n17,Nottingham Forest,45,-5,52\n18,Bristol City,45,-16,52\n19,Huddersfield Town,45,-7,51\n"
            + "20,Rotherham Utd,45,-14,49\n21,Fulham,45,-14,48\n22,Charlton Athletic,45,-37,40\n23,Milton Keynes Dons,45,-29,39\n24,Bolton Wanderers,45,-39,30\n";

    // Return the unique URIs
    private List<String> uris(SearchResponse sr) {
        List<SearchHit> hits = sr.getSearchHits();
        Set<String> set = new HashSet<>();
        for (SearchHit hit : hits)
            set.add(hit.getUri());
        List<String> ret = new ArrayList<>(set);
        return ret;
    }

    private String toString(SearchResponse sr) {
        StringBuilder sb = new StringBuilder();
        for (SearchHit hit : sr.getSearchHits())
            sb.append(hit.getUri()).append("\n");
        return sb.toString();
    }

    /**
     * Requires: ElasticSearch. Tests: Blob creation, deletion. Repo creation, deletion. Search
     * 
     * @throws IOException
     */
    @Test(groups = { "nightly", "search" })
    public void testBlobSearch() throws IOException {

        File pdf = new File("src/test/resources/www-bbc-com.pdf");
        RaptureURI repo = helper.getRandomAuthority(Scheme.BLOB);
        helper.configureTestRepo(repo, "MEMORY");

        RaptureURI epl = new RaptureURI.Builder(repo).docPath("English/Premier").build();
        RaptureURI champ = new RaptureURI.Builder(repo).docPath("English/Championship").build();
        RaptureURI firstDiv = new RaptureURI.Builder(repo).docPath("English/First").build();

        blobApi.putBlob(epl.toString(), premier.getBytes(), MediaType.ANY_TEXT_TYPE.toString());
        blobApi.putBlob(champ.toString(), championship.getBytes(), MediaType.CSV_UTF_8.toString());
        blobApi.putBlob(firstDiv.toString(), Files.readAllBytes(pdf.toPath()), MediaType.PDF.toString());

        SearchResponse res = QueryWithRetry.query(3, 5, () -> {
            return searchApi.searchWithCursor(null, null, 10, "blob:*Utd");
        });

        List<String> uris = uris(res);
        Assert.assertEquals(res.getTotal().intValue(), 3, toString(res));
        Assert.assertTrue(uris.contains(champ.toString()), "Expected to find " + champ.toString() + " in " + toString(res));

        res = QueryWithRetry.query(1, 5, () -> {
            return searchApi.search(String.format("mimetype:*CSV*"));
        });
        Assert.assertEquals(res.getTotal().intValue(), 1, toString(res));


        // Can we assume anything about the ordering?

        for (rapture.common.SearchHit h : res.getSearchHits()) {
            Assert.assertTrue(h.getUri().startsWith("blob://" + repo.getAuthority() + "/English"), h.getUri());
        }

        res = QueryWithRetry.query(3, 5, () -> {
            return searchApi.search(String.format("repo:%s", repo.getAuthority()));
        });
        Assert.assertEquals(res.getTotal().intValue(), 3, toString(res));

        res = QueryWithRetry.query(3, 5, () -> {
            return searchApi.search(String.format("scheme:%s AND parts:Eng*", repo.getScheme()));
        });
        Assert.assertEquals(res.getTotal().intValue(), 3, toString(res));

        res = QueryWithRetry.query(1, 5, () -> {
            return searchApi.search("blob:Wigan");
        });
        Assert.assertNull(res.getCursorId());
        Assert.assertEquals(res.getTotal().intValue(), 1, toString(res));


        blobApi.deleteBlob(champ.toString());
        res = QueryWithRetry.query(2, 5, () -> {
            return searchApi.searchWithCursor(null, 10, "blob:*Utd");
        });

        assertNotNull(res.getCursorId());
        uris = uris(res);
        Assert.assertFalse(uris.contains(champ.toString()));
        Assert.assertEquals(res.getTotal().intValue(), 2, toString(res));

        // Put it back
        blobApi.putBlob(champ.toString(), championship.getBytes(), MediaType.CSV_UTF_8.toString());
        res = QueryWithRetry.query(3, 5, () -> {
            return searchApi.search("blob:*Utd");
        });
        uris = uris(res);
        Assert.assertEquals(res.getTotal().intValue(), 3, toString(res));
        Assert.assertTrue(uris.contains(champ.toString()));

        // Drop the repo
        blobApi.deleteBlobRepo(repo.toString());
        res = QueryWithRetry.query(0, 5, () -> {
            return searchApi.search("blob:*Utd");
        });
        Assert.assertEquals(res.getTotal().intValue(), 0, toString(res));
    }

    /**
     * Requires: ElasticSearch. Tests: Series creation, deletion. Repo creation, deletion. Search
     * 
     * @throws IOException
     */
    @Test(groups = { "nightly", "search" })
    public void testSeriesSearch() throws IOException {
        RaptureURI repo = helper.getRandomAuthority(Scheme.SERIES);
        helper.configureTestRepo(repo, "MEMORY");

        RaptureURI epl = new RaptureURI.Builder(repo).docPath("English/Premier").build();
        RaptureURI champ = new RaptureURI.Builder(repo).docPath("English/Championship").build();

        String[] premierArr = premier.split("\n");
        String[] champArr = championship.split("\n");

        List<String> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();

        for (String prem : premierArr) {
            String[] line = prem.split(",");
            keys.add(line[1]);
            values.add(line[4]);
        }
        seriesApi.addStringsToSeries(epl.toString(), keys, values);

        String champUri = champ.toString();
        for (String c : champArr) {
            String[] line = c.split(",");
            seriesApi.addStringToSeries(champUri, line[1], line[4]);
        }

        SearchResponse res = QueryWithRetry.query(2, 5, () -> {
            return searchApi.search(String.format("repo:%s", repo.getAuthority()));
        });
        Assert.assertEquals(res.getTotal().intValue(), 2, toString(res));

        res = QueryWithRetry.query(2, 5, () -> {
            return searchApi.search(String.format("scheme:%s AND parts:Eng*", repo.getScheme()));
        });
        Assert.assertEquals(res.getTotal().intValue(), 2, toString(res));

        res = QueryWithRetry.query(1, 5, () -> {
            return searchApi.search("Watford:*");
        });

        // Assert.assertTrue(values.contains(query));
        Assert.assertEquals(res.getTotal().intValue(), 1, toString(res));

        // Delete keys from 4-16
        seriesApi.deletePointsFromSeriesByPointKey(epl.toString(), keys.subList(4, 16));
        List<SeriesPoint> splist = seriesApi.getPoints(epl.toString());
        for (SeriesPoint sp : splist) {
            Reporter.log(sp.getColumn());
            Assert.assertFalse(sp.getColumn().equals("Watford"));
        }

        res = QueryWithRetry.query(0, 5, () -> {
            return searchApi.search("Watford:*");
        });
        for (SearchHit sh : res.getSearchHits()) {
            Reporter.log(sh.getSource());
        }
        Assert.assertEquals(res.getTotal().intValue(), 0, toString(res));

        // Put them back
        seriesApi.addStringsToSeries(epl.toString(), keys, values);
        res = QueryWithRetry.query(1, 5, () -> {
            return searchApi.search("Watford:*");
        });
        Assert.assertNull(res.getCursorId());
        Assert.assertEquals(res.getTotal().intValue(), 1, toString(res));

        // Drop the repo
        seriesApi.deleteSeriesRepo(repo.toString());
        res = QueryWithRetry.query(0, 5, () -> {
            return searchApi.searchWithCursor(null, 10, "Watford:*");
        });
        assertNotNull(res.getCursorId());
        Assert.assertEquals(res.getTotal().intValue(), 0, toString(res));
    }

    /**
     * Requires: ElasticSearch. Tests: various search types.
     * 
     * @throws IOException
     */
    @Test(groups = { "nightly", "search" })
    public void testDocSearch() throws IOException {
        RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, "MEMORY");

        RaptureURI epl = new RaptureURI.Builder(repo).docPath("English/Premier").build();
        RaptureURI champ = new RaptureURI.Builder(repo).docPath("English/Championship").build();

        String[] premierArr = premier.split("\n");
        String[] champArr = championship.split("\n");

        Map<String, String> premMap = new HashMap<>();

        for (String prem : premierArr) {
            String[] line = prem.split(",");
            premMap.put(line[1], line[4]);
        }
        docApi.putDoc(epl.toString(), JacksonUtil.jsonFromObject(premMap));

        Map<String, String> champMap = new HashMap<>();
        for (String cham : champArr) {
            String[] line = cham.split(",");
            champMap.put(line[1], line[4]);
        }
        docApi.putDoc(champ.toString(), JacksonUtil.jsonFromObject(champMap));

        SearchResponse res = QueryWithRetry.query(2, 5, () -> {
            return searchApi.search(String.format("repo:%s", repo.getAuthority()));
        });
        Assert.assertEquals(res.getTotal().intValue(), 2, toString(res));

        res = QueryWithRetry.query(2, 5, () -> {
            return searchApi.qualifiedSearch(ConfigLoader.getConf().FullTextSearchDefaultRepo, ImmutableList.of(SearchRepoType.uri.toString()),
                    String.format("scheme:%s AND parts:Eng*", repo.getScheme()));
        });
        Assert.assertEquals(res.getTotal().intValue(), 2, toString(res));

        res = searchApi.qualifiedSearch(ConfigLoader.getConf().FullTextSearchDefaultRepo, ImmutableList.of(SearchRepoType.meta.toString()), "user:rapture");
        int count = 0;
        for (String uri : uris(res)) {
            if (uri.startsWith("script")) continue;
            count++;
        }
        Assert.assertEquals(count, 2, toString(res));

        Assert.assertNotNull(premMap.get("Watford"));
        res = QueryWithRetry.query(1, 5, () -> {
            return searchApi.search("Watford:*");
        });
        Assert.assertEquals(res.getTotal().intValue(), 1, toString(res));

        // Delete keys from 4-16
        assertNotNull(premMap.remove("Watford"));
        docApi.putDoc(epl.toString(), JacksonUtil.jsonFromObject(premMap));

        res = QueryWithRetry.query(0, 5, () -> {
            return searchApi.search("Watford:*");
        });
        for (SearchHit sh : res.getSearchHits()) {
            Reporter.log(sh.getSource());
        }
        Assert.assertEquals(res.getTotal().intValue(), 0, toString(res));

        // Drop the repo
        docApi.deleteDocRepo(repo.toString());
        res = QueryWithRetry.query(0, 5, () -> {
            return searchApi.searchWithCursor(null, 10, "Everton:*");
        });
        assertNotNull(res.getCursorId());
        Assert.assertEquals(res.getTotal().intValue(), 0, toString(res));
    }

    /**
     * Requires: ElasticSearch. Tests: Script creation, deletion. Repo creation, deletion. Search
     * 
     * @throws IOException
     */
    @Test(groups = { "nightly", "search" })
    public void testScriptSearch() throws IOException {
        RaptureURI repo = helper.getRandomAuthority(Scheme.SCRIPT);
        helper.configureTestRepo(repo, "MEMORY");

        String docApi;

        RaptureScript script1 = new RaptureScript();
        script1.setLanguage(RaptureScriptLanguage.REFLEX);
        script1.setAuthority(repo.getAuthority());
        script1.setName("Single/Three_Boats/Down_From/The_Candy");
        script1.setPurpose(RaptureScriptPurpose.OPERATION);
        script1.setParameters(Collections.EMPTY_LIST);
        String scriptWrite1 = "// I'm a market square hero\n// Marillion";
        script1.setScript(scriptWrite1);
        scriptApi.putScript(script1.getAddressURI().toString(), script1);
        RaptureScript scriptRead1 = scriptApi.getScript(script1.getAddressURI().toString());
        assertEquals(scriptWrite1, scriptRead1.getScript());

        RaptureScript script2 = new RaptureScript();
        script2.setLanguage(RaptureScriptLanguage.REFLEX);
        script2.setAuthority(repo.getAuthority());
        script2.setName("Single/Punch/And/Judy");
        script2.setPurpose(RaptureScriptPurpose.OPERATION);
        script2.setParameters(Collections.EMPTY_LIST);
        String scriptWrite2 = "// Grendel stalks the night\n// Marillion";
        script2.setScript(scriptWrite2);
        scriptApi.putScript(script2.getAddressURI().toString(), script2);
        RaptureScript scriptRead2 = scriptApi.getScript(script2.getAddressURI().toString());
        assertEquals(scriptWrite2, scriptRead2.getScript());

        try {
            SearchResponse res = QueryWithRetry.query(2, 5, () -> {
                return searchApi.search(String.format("repo:%s", repo.getAuthority()));
            });
            Assert.assertEquals(res.getTotal().intValue(), 2, toString(res));
            res = QueryWithRetry.query(2, 5, () -> {
                return searchApi.qualifiedSearch(ConfigLoader.getConf().FullTextSearchDefaultRepo, ImmutableList.of(SearchRepoType.uri.toString()),
                        String.format("scheme:%s AND parts:Single", repo.getScheme()));
            });
            Assert.assertEquals(res.getTotal().intValue(), 2, toString(res));
            res = searchApi.qualifiedSearch(ConfigLoader.getConf().FullTextSearchDefaultRepo, ImmutableList.of(SearchRepoType.meta.toString()), "user:rapture");
            int count = 0;
            String auth = repo.getAuthority();
            for (String uri : uris(res)) {
                if (!uri.contains(auth)) continue;
                count++;
            }
            Assert.assertEquals(count, 2, toString(res));
            res = QueryWithRetry.query(2, 5, () -> {
                return searchApi.search("*Marillion*");
            });
            Assert.assertEquals(res.getTotal().intValue(), 2, toString(res));
            scriptApi.deleteScript(script1.getAddressURI().toString());
            res = QueryWithRetry.query(1, 5, () -> {
                return searchApi.search("*Marillion*");
            });
            Assert.assertEquals(res.getTotal().intValue(), 1, toString(res));
            scriptApi.deleteScript(script2.getAddressURI().toString());
            res = QueryWithRetry.query(0, 5, () -> {
                return searchApi.search("*Marillion*");
            });
            Assert.assertEquals(res.getTotal().intValue(), 0, toString(res));
        } finally {
            scriptApi.deleteScript(script1.getAddressURI().toString());
            scriptApi.deleteScript(script2.getAddressURI().toString());
        }
    }
}
