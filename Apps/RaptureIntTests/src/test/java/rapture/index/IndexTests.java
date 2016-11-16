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
	public void setUp(@Optional("http://localhost:8665/rapture") String url, @Optional("rapture") String username,
			@Optional("rapture") String password) {
		helper = new IntegrationTestHelper(url, username, password);
		docApi = helper.getDocApi();
		index = helper.getIndexApi();

	}

	@Test(groups = { "index", "mongo", "nightly" }, dataProvider="implTypes")
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
		Assert.assertEquals(resList.size(), 0);

		createData1(planetURI);

		planetIndex = index.createIndex(planetURI, INDEXCFG);
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
	}

	
	@Test(groups = { "index", "mongo", "nightly" }, dataProvider="implTypes")
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

        String limitQuery = "SELECT planet, moon LIMIT 4";
        Reporter.log("Testing query: " + limitQuery, true);
        TableQueryResult res = index.findIndex(planetURI, limitQuery);
        List<List<Object>> limitList = res.getRows();
        Assert.assertNotNull(limitList);
        Assert.assertEquals(limitList.size(),4);
	}
	
	@Test(groups = { "index", "mongo", "nightly" }, dataProvider="implTypes")
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
		String distinctQuery="SELECT DISTINCT planet";
		TableQueryResult distinctResults = index.findIndex(planetURI, distinctQuery);
		Reporter.log("Testing query: " + distinctQuery, true);
        List<List<Object>> distinctList = distinctResults.getRows();
        Assert.assertNotNull(distinctList);
        Assert.assertEquals(distinctList.size(),5 );
	}
	
	@Test(groups = { "index", "mongo", "nightly" }, dataProvider="implTypes")
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
		String orderQuery="SELECT DISTINCT planet ORDER BY planet ASC";
		TableQueryResult orderResult = index.findIndex(planetURI, orderQuery);
		Reporter.log("Testing query: " + orderQuery, true);
		List<List<Object>> orderList = orderResult.getRows();
		Assert.assertNotNull(orderList);
		Assert.assertEquals(orderList.size(), 5);
		Reporter.log("Verifying order of results", true);
		String last = "Aaaa";   
		for (List<Object> next : orderList) {
            String nextStr = next.get(0).toString();
            Assert.assertTrue(nextStr.compareTo(last) > 0);
            last = nextStr;
		}
	}
	
	@Test(groups = { "index", "mongo", "nightly" }, dataProvider="implTypes")
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
		String orderQuery="SELECT DISTINCT planet ORDER BY planet DESC";
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

	@Test(groups = { "index", "mongo", "nightly" }, dataProvider="implTypes")
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
		String orderQuery="SELECT DISTINCT planet ORDER BY planet DESC LIMIT 3";
		TableQueryResult orderResults = index.findIndex(planetURI, orderQuery);
        Reporter.log("Query: " + orderQuery, true);
        List<List<Object>> orderList = orderResults.getRows();
        Assert.assertNotNull(orderList);
        Reporter.log("Verifying results size", true);
        Assert.assertEquals(orderList.size(),3);	
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
        return new Object[][] {
                new Object[] {"MONGODB"},
                new Object[] {"MEMORY"},
        };
    } 
	
	private void createData1(String planetURI) {
		docApi.putDoc(planetURI + "/Mercury/None", JacksonUtil.jsonFromObject(ImmutableMap.of("one", "A", "two",
				new Double(1.5), "three", "constant", "inner", ImmutableMap.of("alpha", "Z"))));
		docApi.putDoc(planetURI + "/Venus/None", JacksonUtil.jsonFromObject(ImmutableMap.of("one", "B", "two",
				new Integer(2), "three", "constant", "inner", ImmutableMap.of("alpha", "Y"))));
		docApi.putDoc(planetURI + "/Earth/Moon", JacksonUtil.jsonFromObject(ImmutableMap.of("one", "C", "two",
				new Double(3.7), "three", "constant", "inner", ImmutableMap.of("alpha", "X"))));
		docApi.putDoc(planetURI + "/Mars/Phobos", JacksonUtil.jsonFromObject(ImmutableMap.of("one", "D", "two",
				new Integer(4), "three", "constant", "inner", ImmutableMap.of("alpha", "W"))));
		docApi.putDoc(planetURI + "/Mars/Deimos", JacksonUtil.jsonFromObject(ImmutableMap.of("one", "E", "two",
				new Integer(5), "three", "constant", "inner", ImmutableMap.of("alpha", "V"))));
	}

	private void createData2(String planetURI) {
		docApi.putDoc(planetURI + "/Jupiter/Ganymede", JacksonUtil.jsonFromObject(ImmutableMap.of("one", "F", "two",
				new Integer(6), "three", "constant", "inner", ImmutableMap.of("alpha", "U"))));
		docApi.putDoc(planetURI + "/Jupiter/Europa", JacksonUtil.jsonFromObject(ImmutableMap.of("one", "G", "two",
				new Integer(7), "three", "constant", "inner", ImmutableMap.of("alpha", "T"))));
		docApi.putDoc(planetURI + "/Jupiter/Titan", JacksonUtil.jsonFromObject(ImmutableMap.of("one", "H", "two",
				new Integer(8), "three", "constant", "inner", ImmutableMap.of("alpha", "S"))));
		docApi.putDoc(planetURI + "/Jupiter/Io", JacksonUtil.jsonFromObject(ImmutableMap.of("one", "I", "two",
				new Integer(9), "three", "constant", "inner", ImmutableMap.of("alpha", "R"))));
	}

}
