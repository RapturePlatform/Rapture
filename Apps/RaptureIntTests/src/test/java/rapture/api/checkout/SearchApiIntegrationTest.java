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
package rapture.api.checkout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.common.net.MediaType;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.client.HttpBlobApi;
import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.HttpSearchApi;
import rapture.common.client.HttpSeriesApi;
import rapture.common.client.SimpleCredentialsProvider;

/**
 * Tests to exercise the Mongo repo to check for breakages in migrating to Mongo 3.0
 */

public class SearchApiIntegrationTest {

    private HttpLoginApi raptureLogin = null;
    private HttpSeriesApi seriesApi = null;
    private HttpSearchApi searchApi = null;
    private HttpDocApi docApi = null;
    private HttpBlobApi blobApi = null;
    CallingContext callingContext = null;

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
    @BeforeMethod
    @BeforeClass(groups = { "mongo" })
    @Parameters({ "RaptureURL", "RaptureUser", "RapturePassword" })
    public void setUp(@Optional("http://localhost:8665/rapture") String url, @Optional("rapture") String username, @Optional("rapture") String password) {

        // If running from eclipse set env var -Penv=docker or use the following
        // url variable settings:
        // url="http://192.168.99.101:8665/rapture"; //docker
        // url="http://localhost:8665/rapture";

        System.out.println("Using url " + url);
        raptureLogin = new HttpLoginApi(url, new SimpleCredentialsProvider(username, password));
        raptureLogin.login();
        seriesApi = new HttpSeriesApi(raptureLogin);
        docApi = new HttpDocApi(raptureLogin);
        blobApi = new HttpBlobApi(raptureLogin);
        searchApi = new HttpSearchApi(raptureLogin);
        callingContext = raptureLogin.getContext();
    }

    /**
     * TestNG method to cleanup.
     *
     * @param none
     * @return none
     */
    @AfterClass(groups = { "mongo" })
    public void afterTest() {
        raptureLogin = null;
    }

    @Test
    public void testSearchBlob() throws IOException {
        String premier = "1,Leicester City,36,30,77\n2,Tottenham Hotspur,36,39,70\n3,Arsenal,36,25,67\n4,Manchester City,36,30,64\n5,Manchester United,35,12,60\n"
                + "6,West Ham United,35,17,59\n7,Southampton,36,14,57\n8,Liverpool,35,11,55\n9,Chelsea,35,7,48\n10,Stoke City,36,-14,48\n"
                + "11,Everton,35,6,44\n12,Watford,35,-6,44\n13,Swansea City,36,-13,43\n14,West Bromwich Albion,36,-14,41\n15,Bournemouth,36,-20,41\n1"
                + "6,Crystal Palace,36,-10,39\n17,Newcastle United,36,-25,33\n18,Sunderland,35,-18,32\n19,Norwich City,35,-26,31\n20,Aston Villa,36,-45,16\n";

        String championship = "1,Burnley,45,34,90\n2,Middlesbrough,45,32,88\n3,Brighton & Hove Albion,45,30,88\n4,Hull City,45,30,80\n"
                + "5,Derby County,45,24,78\n6,Sheffield Wednesday,45,22,74\n7,Cardiff City,45,5,67\n8,Ipswich Town,45,1,66\n9,Birmingham City,45,4,62\n"
                + "10,Brentford,45,1,62\n11,Preston North End,45,0,61\n12,Leeds United,45,-8,58\n13,Queens Park Rangers,45,-1,57\n14,Wolverhampton Wanderers,45,-6,55\n"
                + "15,Blackburn Rovers,45,-2,52\n16,Reading,45,-5,52\n17,Nottingham Forest,45,-5,52\n18,Bristol City,45,-16,52\n19,Huddersfield Town,45,-7,51\n"
                + "20,Rotherham United,45,-14,49\n21,Fulham,45,-14,48\n22,Charlton Athletic,45,-37,40\n23,Milton Keynes Dons,45,-29,39\n24,Bolton Wanderers,45,-39,30\n";

        File pdf = new File("src/test/resources/www-bbc-com.pdf");


        RaptureURI repo = new RaptureURI.Builder(Scheme.BLOB, "unittest").build();
        RaptureURI epl = new RaptureURI.Builder(repo).docPath("English/Premier").build();
        RaptureURI champ = new RaptureURI.Builder(repo).docPath("English/Championship").build();
        RaptureURI firstDiv = new RaptureURI.Builder(repo).docPath("English/First").build();

        if (blobApi.blobRepoExists(repo.toString())) blobApi.deleteBlobRepo(repo.toString());

        blobApi.createBlobRepo(repo.toString(), "BLOB {} USING MONGODB {prefix=\"unittest\"}", "NREP {} USING MONGODB {prefix=\"Meta_unittest\"}");

        blobApi.putBlob(epl.toString(), premier.getBytes(), MediaType.ANY_TEXT_TYPE.toString());
        blobApi.putBlob(champ.toAuthString(), championship.getBytes(), MediaType.CSV_UTF_8.toString());
        blobApi.putBlob(firstDiv.toString(), Files.readAllBytes(pdf.toPath()), MediaType.PDF.toString());

        String query = "blob:*City";
        rapture.common.SearchResponse res = searchApi.searchWithCursor(null, null, 10, query);
        assertNotNull(res.getCursorId());
        assertEquals(2, res.getSearchHits().size());

        // Can we assume anything about the ordering?

        for (rapture.common.SearchHit h : res.getSearchHits()) {
            assertTrue(h.getUri().startsWith("blob://unittest/English/"));
        }

        res = searchApi.searchWithCursor(null, null, 10, "blob:*Wigan*");
        assertNotNull(res.getCursorId());
        assertEquals(1, res.getSearchHits().size());

        blobApi.deleteBlob(champ.toString());
        res = searchApi.searchWithCursor(null, null, 10, query);
        assertNotNull(res.getCursorId());
        assertEquals(1, res.getSearchHits().size());

        blobApi.deleteBlobRepo(repo.toString());
        res = searchApi.searchWithCursor(null, null, 10, query);
        assertNotNull(res.getCursorId());
        assertEquals(0, res.getSearchHits().size());
    }
}
