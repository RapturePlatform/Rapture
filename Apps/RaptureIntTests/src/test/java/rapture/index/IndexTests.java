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
package rapture.index;

import java.util.List;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import com.google.common.collect.ImmutableMap;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.TableQueryResult;
import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpIndexApi;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.IndexConfig;
import rapture.helper.IntegrationTestHelper;

public class IndexTests {

    HttpDocApi docApi = null;
    HttpIndexApi index = null;
    IntegrationTestHelper helper = null;

    @BeforeClass(groups = { "index", "mongo", "nightly" })
    @Parameters({ "RaptureURL", "RaptureUser", "RapturePassword" })
    public void setUp(@Optional("http://localhost:8665/rapture") String url, @Optional("rapture") String username, @Optional("rapture") String password) {
        helper = new IntegrationTestHelper(url, username, password);
        docApi = helper.getDocApi();
        index = helper.getIndexApi();

    }

    @Test(groups = { "index", "mongo", "nightly" }, dataProvider = "implTypes")
    public void testUpdateIndex(String implType) {

        String INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
        IndexConfig planetIndex = null;

        RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, implType);
        String planetURI = repo.getAuthority();
        Reporter.log("Document URI: document://" + planetURI, true);

        planetIndex = index.createIndex(planetURI, INDEXCFG);
        Reporter.log("Created index: " + planetIndex.getName(), true);

        String query = "SELECT planet, moon, fieldOne, fieldTwo WHERE fieldTwo > 2.5";
        TableQueryResult res = index.findIndex(planetURI, query);
        List<List<Object>> resList = res.getRows();
        if (resList != null) Assert.assertEquals(resList.size(), 0);

        createData1(planetURI);
        createData3(planetURI);

        Reporter.log("Updated index: " + planetIndex.getName(), true);
        Reporter.log("Testing query: " + query, true);
        res = index.findIndex(planetURI, query);
        resList = res.getRows();
        Assert.assertEquals(resList.size(), 3);

        createData2(planetURI);

        Reporter.log("Testing query: " + query, true);
        res = index.findIndex(planetURI, query);
        resList = res.getRows();
        Assert.assertEquals(resList.size(), 7);

        query = "SELECT planet, moon, fieldOne, fieldTwo WHERE fieldTwo < -1.5";
        Reporter.log("Testing query: " + query, true);
        res = index.findIndex(planetURI, query);
        resList = res.getRows();
        Assert.assertEquals(resList.size(), 2);
    }

    @Test(groups = { "index", "mongo", "nightly" }, dataProvider = "implTypes")
    public void testUpdateIndexAfterDelete(String implType) {

        String INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
        IndexConfig planetIndex = null;

        RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, implType);
        String planetURI = repo.getAuthority();
        Reporter.log("Document URI: document://" + planetURI, true);

        planetIndex = index.createIndex(planetURI, INDEXCFG);
        Reporter.log("Created index: " + planetIndex.getName(), true);

        String query = "SELECT planet, moon, fieldOne, fieldTwo WHERE fieldTwo > 2.5";
        TableQueryResult res = index.findIndex(planetURI, query);
        List<List<Object>> resList = res.getRows();
        if (resList != null) Assert.assertEquals(resList.size(), 0);

        createData1(planetURI);
        createData2(planetURI);
        Reporter.log("Updated index: " + planetIndex.getName(), true);
        Reporter.log("Testing query: " + query, true);
        res = index.findIndex(planetURI, query);
        resList = res.getRows();
        Assert.assertEquals(resList.size(), 7);

        deleteData2(planetURI);
        Reporter.log("Testing query: " + query, true);
        res = index.findIndex(planetURI, query);
        resList = res.getRows();
        Assert.assertEquals(resList.size(), 3);

        query = "SELECT planet, moon, fieldOne, fieldTwo WHERE fieldTwo < -1.5";
        Reporter.log("Testing query: " + query, true);
        res = index.findIndex(planetURI, query);
        resList = res.getRows();
        if (resList != null) Assert.assertEquals(resList.size(), 0);
    }

    @Test(groups = { "index", "mongo", "nightly" }, dataProvider = "implTypes")
    public void testLimitQuery(String implType) {
        String INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
        IndexConfig planetIndex = null;

        RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, implType);
        String planetURI = repo.getAuthority();
        Reporter.log("Document URI: document://" + planetURI, true);
        planetIndex = index.createIndex(planetURI, INDEXCFG);
        Reporter.log("Created index: " + planetIndex.getName(), true);
        createData1(planetURI);
        createData2(planetURI);
        createData3(planetURI);

        String limitQuery = "SELECT planet, moon LIMIT 4";
        Reporter.log("Testing query: " + limitQuery, true);
        TableQueryResult res = index.findIndex(planetURI, limitQuery);
        List<List<Object>> limitList = res.getRows();
        Assert.assertNotNull(limitList);
        Assert.assertEquals(limitList.size(), 4);

        limitQuery = "SELECT DISTINCT planet, moon ORDER BY planet, moon ASC LIMIT 2";
        Reporter.log("Testing query: " + limitQuery, true);
        res = index.findIndex(planetURI, limitQuery);
        limitList = res.getRows();

        Assert.assertEquals(limitList.size(), 2);

        limitQuery = "SELECT DISTINCT distinct planet, moon ORDER BY planet, moon Asc Limit 2 Skip 2";

        Reporter.log("Testing query: " + limitQuery, true);
        res = index.findIndex(planetURI, limitQuery);
        List<List<Object>> limitList2 = res.getRows();
        Assert.assertEquals(limitList.size(), 2);

        Assert.assertEquals(limitList.get(0).get(0).toString(), "Earth");
        Assert.assertEquals(limitList.get(1).get(0).toString(), "Jupiter");
        Assert.assertEquals(limitList.get(1).get(1).toString(), "Europa");

        Assert.assertEquals(limitList2.get(0).get(0).toString(), "Jupiter");
        Assert.assertEquals(limitList2.get(0).get(1).toString(), "Ganymede");
        Assert.assertEquals(limitList2.get(1).get(0).toString(), "Jupiter");
        Assert.assertEquals(limitList2.get(1).get(1).toString(), "Io");
    }

    @Test(groups = { "index", "mongo", "nightly" }, dataProvider = "implTypes")
    public void testDistinctQuery(String implType) {
        String INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
        IndexConfig planetIndex = null;

        RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, implType);
        String planetURI = repo.getAuthority();
        Reporter.log("Document URI: document://" + planetURI, true);
        planetIndex = index.createIndex(planetURI, INDEXCFG);
        Reporter.log("Created index: " + planetIndex.getName(), true);
        createData1(planetURI);
        createData2(planetURI);
        String distinctQuery = "SELECT DISTINCT planet";
        TableQueryResult distinctResults = index.findIndex(planetURI, distinctQuery);
        Reporter.log("Testing query: " + distinctQuery, true);
        List<List<Object>> distinctList = distinctResults.getRows();
        Assert.assertNotNull(distinctList);
        Assert.assertEquals(distinctList.size(), 5);
    }

    @Test(groups = { "index", "mongo", "nightly" }, dataProvider = "implTypes")
    public void testAscendingOrder(String implType) {
        String INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
        IndexConfig planetIndex = null;

        RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, implType);
        String planetURI = repo.getAuthority();
        Reporter.log("Document URI: document://" + planetURI, true);
        planetIndex = index.createIndex(planetURI, INDEXCFG);
        Reporter.log("Created index: " + planetIndex.getName(), true);
        createData1(planetURI);
        createData2(planetURI);

        String orderQuery = "SELECT DISTINCT planet";
        TableQueryResult orderResult = index.findIndex(planetURI, orderQuery);
        Reporter.log("Testing query: " + orderQuery, true);
        List<List<Object>> orderList = orderResult.getRows();
        Assert.assertEquals(orderList.size(), 5);

        orderQuery = "SELECT DISTINCT moon";
        orderResult = index.findIndex(planetURI, orderQuery);
        Reporter.log("Testing query: " + orderQuery, true);
        orderList = orderResult.getRows();

        Assert.assertEquals(orderList.size(), 8);

        createData3(planetURI);
        orderQuery = "SELECT DISTINCT planet, moon ORDER BY moon, planet ASC";
        orderResult = index.findIndex(planetURI, orderQuery);
        Reporter.log("Testing query: " + orderQuery, true);
        orderList = orderResult.getRows();
        Assert.assertNotNull(orderList);
        Assert.assertEquals(orderList.size(), 9);
        Reporter.log("Verifying order of results", true);
        String last = "Aaaa";
        for (List<Object> next : orderList) {
            String nextStr = next.get(1).toString();
            Assert.assertTrue(nextStr.compareTo(last) >= 0);
            last = nextStr;
        }

        orderQuery = "SELECT planet, moon Order By moon, planet ASC";
        orderResult = index.findIndex(planetURI, orderQuery);
        Reporter.log("Testing query: " + orderQuery, true);
        orderList = orderResult.getRows();
        Assert.assertEquals(orderList.size(), 12);
        last = "Aaaa";
        for (List<Object> next : orderList) {
            String nextStr = next.get(1).toString();
            Assert.assertTrue(nextStr.compareTo(last) >= 0);
            last = nextStr;
        }

        orderQuery = "SELECT planet, moon ORDER BY moon, planet ASC";
        orderResult = index.findIndex(planetURI, orderQuery);
        Reporter.log("Testing query: " + orderQuery, true);
        orderList = orderResult.getRows();
        Assert.assertEquals(orderList.size(), 12);
        last = "Aaaa";
        for (List<Object> next : orderList) {
            String nextStr = next.get(1).toString();
            Assert.assertTrue(nextStr.compareTo(last) >= 0);
            last = nextStr;
        }

    }

    @Test(groups = { "index", "mongo", "nightly" }, dataProvider = "implTypes")
    public void testDescendingOrder(String implType) {
        String INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
        IndexConfig planetIndex = null;

        RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, implType);
        String planetURI = repo.getAuthority();
        Reporter.log("Document URI: document://" + planetURI, true);
        planetIndex = index.createIndex(planetURI, INDEXCFG);
        Reporter.log("Created index: " + planetIndex.getName(), true);
        createData1(planetURI);
        createData2(planetURI);
        String orderQuery = "SELECT DISTINCT planet ORDER BY planet DESC";
        TableQueryResult orderResult = index.findIndex(planetURI, orderQuery);
        Reporter.log("Testing query: " + orderQuery, true);
        List<List<Object>> orderList = orderResult.getRows();
        Assert.assertNotNull(orderList);
        Assert.assertEquals(orderList.size(), 5);
        String last = "Zzzz";
        Reporter.log("Verifying order of results", true);
        for (List<Object> next : orderList) {
            String nextStr = next.get(0).toString();
            Assert.assertTrue(nextStr.compareTo(last) < 0);
            last = nextStr;
        }
    }

    @Test(groups = { "index", "mongo", "nightly" }, dataProvider = "implTypes")
    public void testOrderLimit(String implType) {
        String INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
        IndexConfig planetIndex = null;

        RaptureURI repo = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repo, implType);
        String planetURI = repo.getAuthority();
        Reporter.log("Document URI: document://" + planetURI, true);
        planetIndex = index.createIndex(planetURI, INDEXCFG);
        Reporter.log("Created index: " + planetIndex.getName(), true);
        createData1(planetURI);
        createData2(planetURI);
        String orderQuery = "SELECT DISTINCT planet ORDER BY planet DESC LIMIT 3";
        TableQueryResult orderResults = index.findIndex(planetURI, orderQuery);
        Reporter.log("Query: " + orderQuery, true);
        List<List<Object>> orderList = orderResults.getRows();
        Assert.assertNotNull(orderList);
        Reporter.log("Verifying results size", true);
        Assert.assertEquals(orderList.size(), 3);
        String last = "Zzzz";
        Reporter.log("Verifying order of results", true);
        for (List<Object> next : orderList) {
            String nextStr = next.get(0).toString();
            Assert.assertTrue(nextStr.compareTo(last) < 0);
            last = nextStr;
        }
    }

    @AfterClass(groups = { "index", "mongo", "nightly" })
    public void AfterTest() {
        helper.cleanAllAssets();
    }

    @DataProvider
    public Object[][] implTypes() {
        return new Object[][] { new Object[] { "MONGODB" }, new Object[] { "MEMORY" }, };
    }

    private void createData1(String planetURI) {
        docApi.putDoc(planetURI + "/Mercury/None",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "A", "two", new Double(1.5), "three", "constant", "inner", ImmutableMap.of("alpha", "Z"))));
        docApi.putDoc(planetURI + "/Venus/None",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "B", "two", new Integer(2), "three", "constant", "inner", ImmutableMap.of("alpha", "Y"))));
        docApi.putDoc(planetURI + "/Earth/Moon",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "C", "two", new Double(3.7), "three", "constant", "inner", ImmutableMap.of("alpha", "X"))));
        docApi.putDoc(planetURI + "/Mars/Phobos",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "D", "two", new Integer(4), "three", "constant", "inner", ImmutableMap.of("alpha", "W"))));
        docApi.putDoc(planetURI + "/Mars/Deimos",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "E", "two", new Integer(5), "three", "constant", "inner", ImmutableMap.of("alpha", "V"))));
    }

    private void createData2(String planetURI) {
        docApi.putDoc(planetURI + "/Jupiter/Ganymede",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "F", "two", new Integer(6), "three", "constant", "inner", ImmutableMap.of("alpha", "U"))));
        docApi.putDoc(planetURI + "/Jupiter/Europa",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "G", "two", new Integer(7), "three", "constant", "inner", ImmutableMap.of("alpha", "T"))));
        docApi.putDoc(planetURI + "/Jupiter/Titan",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "H", "two", new Integer(8), "three", "constant", "inner", ImmutableMap.of("alpha", "S"))));
        docApi.putDoc(planetURI + "/Jupiter/Io",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "I", "two", new Integer(9), "three", "constant", "inner", ImmutableMap.of("alpha", "R"))));
    }

    private void deleteData2(String planetURI) {
        docApi.deleteDoc(planetURI + "/Jupiter/Ganymede");
        docApi.deleteDoc(planetURI + "/Jupiter/Europa");
        docApi.deleteDoc(planetURI + "/Jupiter/Titan");
        docApi.deleteDoc(planetURI + "/Jupiter/Io");

    }

    private void createData3(String planetURI) {
        docApi.putDoc(planetURI + "/Earth/Moon/Foo",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "Q", "two", new Double(-1), "three", "constant", "inner", ImmutableMap.of("alpha", "X"))));
        docApi.putDoc(planetURI + "/Earth/Moon/Bar",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "W", "two", new Double(-2), "three", "constant", "inner", ImmutableMap.of("alpha", "X"))));
        docApi.putDoc(planetURI + "/Earth/Moon/Baz",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "E", "two", new Double(-3), "three", "constant", "inner", ImmutableMap.of("alpha", "X"))));
    }

}
