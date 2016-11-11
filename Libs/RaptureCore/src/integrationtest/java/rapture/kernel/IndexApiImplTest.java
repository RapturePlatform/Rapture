package rapture.kernel;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Reporter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.TableQueryResult;
import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpEventApi;
import rapture.common.client.HttpIdGenApi;
import rapture.common.client.HttpIndexApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.HttpScriptApi;
import rapture.common.client.SimpleCredentialsProvider;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.IndexConfig;

public class IndexApiImplTest extends AbstractFileTest {

    private static final Logger log = Logger.getLogger(DocApiFileTest.class);
    private static final String REPO_USING_FILE = "REP {} USING FILE {prefix=\"/tmp/" + auth + "\"}";
    private static final String docAuthorityURI = "document://" + auth;

    private static CallingContext callingContext;
    private static DocApiImpl docImpl;

    static String planetURI = "";
    static String INDEXCFG = "";

    static HttpLoginApi raptureLogin = null;
    static HttpDocApi document = null;
    static HttpScriptApi script = null;
    static HttpIndexApi index = null;
    static HttpEventApi event = null;
    static HttpIdGenApi fountain = null;

    private static final String url = "http://192.168.99.100:8665/rapture";
    // private static final String url = "http://54.67.82.29:8665/rapture";
    private static final String username = "rapture";
    private static final String password = "rapture";
    private static IndexConfig planetIndex = null;

    @BeforeClass
    public static void setUp() {

        raptureLogin = new HttpLoginApi(url, new SimpleCredentialsProvider(username, password));
        raptureLogin.login();
        document = new HttpDocApi(raptureLogin);
        script = new HttpScriptApi(raptureLogin);
        index = new HttpIndexApi(raptureLogin);
        event = new HttpEventApi(raptureLogin);
        fountain = new HttpIdGenApi(raptureLogin);

        AbstractFileTest.setUp();
        config.RaptureRepo = REPO_USING_FILE;
        config.InitSysConfig = "NREP {} USING FILE { prefix=\"/tmp/" + auth + "/sys.config\"}";
        // config.DefaultPipelineTaskStatus = "TABLE {} USING FILE {prefix=\"/tmp/" + auth + "\"}";

        callingContext = new CallingContext();
        callingContext.setUser("dummy");

        Kernel.initBootstrap();
        callingContext = ContextFactory.getKernelUser();

        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using FILE {prefix=\"/tmp/" + auth + "\"}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");
        docImpl = new DocApiImpl(Kernel.INSTANCE);

    }

    void data1() {
        document.putDoc(planetURI + "/Mercury/None",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "A", "two", new Double(1.5), "three", "constant", "inner", ImmutableMap.of("alpha", "Z"))));
        document.putDoc(planetURI + "/Venus/None",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "B", "two", new Integer(2), "three", "constant", "inner", ImmutableMap.of("alpha", "Y"))));
        document.putDoc(planetURI + "/Earth/Moon",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "C", "two", new Double(3.7), "three", "constant", "inner", ImmutableMap.of("alpha", "X"))));
        document.putDoc(planetURI + "/Mars/Phobos",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "D", "two", new Integer(4), "three", "constant", "inner", ImmutableMap.of("alpha", "W"))));
        document.putDoc(planetURI + "/Mars/Deimos",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "E", "two", new Integer(5), "three", "constant", "inner", ImmutableMap.of("alpha", "V"))));
    }

    void data2() {
        document.putDoc(planetURI + "/Jupiter/Ganymede",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "F", "two", new Integer(6), "three", "constant", "inner", ImmutableMap.of("alpha", "U"))));
        document.putDoc(planetURI + "/Jupiter/Europa",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "G", "two", new Integer(7), "three", "constant", "inner", ImmutableMap.of("alpha", "T"))));
        document.putDoc(planetURI + "/Jupiter/Titan",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "H", "two", new Integer(8), "three", "constant", "inner", ImmutableMap.of("alpha", "S"))));
        document.putDoc(planetURI + "/Jupiter/Io",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "I", "two", new Integer(9), "three", "constant", "inner", ImmutableMap.of("alpha", "R"))));
    }

    @Test
    public void updateDataTest() {
        for (String implementation : ImmutableList.of("MONGODB", "FILE", "MEMORY")) {
            String authorityName = "docplanetdata1." + System.nanoTime();
            planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();

            String ver_config = "NREP {} USING " + implementation + " {prefix=\"planet.%s\"}"; // versioned repository

            // setup planet test data
            INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
            document.createDocRepo(planetURI, String.format(ver_config, System.nanoTime()));

            String query = "SELECT planet, moon, fieldOne, fieldTwo WHERE fieldTwo > 2.5";

            TableQueryResult res = index.findIndex(planetURI, query);
            List<List<Object>> resList = res.getRows();
            Reporter.log(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(resList)));
            Assert.assertNull(resList);

            data1();

            planetIndex = index.createIndex(planetURI, INDEXCFG);
            Reporter.log("Index details: " + implementation + " " + planetIndex.getName(), true);

            Reporter.log("Query: " + query, true);
            res = index.findIndex(planetURI, query);
            resList = res.getRows();
            Reporter.log(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(resList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(resList)), 3, resList.size());

            data2();

            Reporter.log("Query: " + query, true);
            res = index.findIndex(planetURI, query);
            resList = res.getRows();
            Reporter.log(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(resList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(resList)), 7, resList.size());
        }
    }

    @Test
    public void limitTest() {
        for (String implementation : ImmutableList.of("MONGODB", "FILE", "MEMORY")) {
            String authorityName = "docplanetdata1." + System.nanoTime();
            planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();
            String ver_config = "NREP {} USING " + implementation + " {prefix=\"planet.%s\"}"; // versioned repository
            document.createDocRepo(planetURI, String.format(ver_config, System.nanoTime()));

            // setup planet test data
            INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
            planetIndex = index.createIndex(planetURI, INDEXCFG);

            data1();
            data2();

            String limitQuery = "SELECT planet, moon LIMIT 4";
            Reporter.log("Query: " + limitQuery, true);
            TableQueryResult res = index.findIndex(planetURI, limitQuery);
            List<List<Object>> limitList = res.getRows();
            Reporter.log(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(limitList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(limitList)), 4, limitList.size());
        }
    }

    @Test
    public void distinctTest() {
        for (String implementation : ImmutableList.of("FILE", "MEMORY", "MONGODB")) {
            String authorityName = "docplanetdata1." + System.nanoTime();
            planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();

            String ver_config = "NREP {} USING " + implementation + " {prefix=\"planet.%s\"}"; // versioned repository
            document.createDocRepo(planetURI, String.format(ver_config, System.nanoTime()));

            // setup planet test data
            INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
            planetIndex = index.createIndex(planetURI, INDEXCFG);

            data1();
            data2();

            // RAP-3685
            TableQueryResult orderQuery = index.findIndex(planetURI, "SELECT DISTINCT planet");
            Reporter.log("Query: " + orderQuery, true);
            List<List<Object>> orderList = orderQuery.getRows();
            Assert.assertNotNull(implementation, orderList);
            Reporter.log(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)), 5, orderList.size());
        }
    }

    @Test
    public void orderAscTest() {
        for (String implementation : ImmutableList.of("FILE", "MEMORY", "MONGODB")) {
            String authorityName = "docplanetdata1." + System.nanoTime();
            planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();

            String ver_config = "NREP {} USING " + implementation + " {prefix=\"planet.%s\"}"; // versioned repository
            document.createDocRepo(planetURI, String.format(ver_config, System.nanoTime()));

            // setup planet test data
            INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
            planetIndex = index.createIndex(planetURI, INDEXCFG);

            data1();
            data2();

            // RAP-3685
            TableQueryResult orderQuery = index.findIndex(planetURI, "SELECT DISTINCT planet ORDER BY planet ASC");
            Reporter.log("Query: " + orderQuery, true);
            List<List<Object>> orderList = orderQuery.getRows();
            Assert.assertNotNull(implementation, orderList);
            System.out.println(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)), 5, orderList.size());
            String last = "Aaaa";
            for (List<Object> next : orderList) {
                String nextStr = next.get(0).toString();
                Assert.assertTrue(implementation + " : " + JacksonUtil.jsonFromObject(orderList), nextStr.compareTo(last) > 0);
                last = nextStr;
            }
        }
    }

    @Test
    public void orderDescTest() {
        for (String implementation : ImmutableList.of("FILE", "MEMORY", "MONGODB")) {
            String authorityName = "docplanetdata1." + System.nanoTime();
            planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();

            String ver_config = "NREP {} USING " + implementation + " {prefix=\"planet.%s\"}"; // versioned repository
            document.createDocRepo(planetURI, String.format(ver_config, System.nanoTime()));

            // setup planet test data
            INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
            planetIndex = index.createIndex(planetURI, INDEXCFG);

            data1();
            data2();

            // RAP-3685
            TableQueryResult orderQuery = index.findIndex(planetURI, "SELECT DISTINCT planet ORDER BY planet DESC");
            Reporter.log("Query: " + orderQuery, true);
            List<List<Object>> orderList = orderQuery.getRows();
            Assert.assertNotNull(implementation, orderList);
            System.out.println(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)), 5, orderList.size());
            String last = "Zzzz";
            for (List<Object> next : orderList) {
                String nextStr = next.get(0).toString();
                Assert.assertTrue(implementation + " : " + JacksonUtil.jsonFromObject(orderList), nextStr.compareTo(last) < 0);
                last = nextStr;
            }
        }
    }

    @Test
    public void orderLimitTest() {
        for (String implementation : ImmutableList.of("FILE", "MEMORY", "MONGODB")) {
            String authorityName = "docplanetdata1." + System.nanoTime();
            planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();

            String ver_config = "NREP {} USING " + implementation + " {prefix=\"planet.%s\"}"; // versioned repository
            document.createDocRepo(planetURI, String.format(ver_config, System.nanoTime()));

            // setup planet test data
            INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
            planetIndex = index.createIndex(planetURI, INDEXCFG);

            data1();
            data2();

            // RAP-3685
            TableQueryResult orderQuery = index.findIndex(planetURI, "SELECT DISTINCT planet ORDER BY planet DESC LIMIT 3");
            Reporter.log("Query: " + orderQuery, true);
            List<List<Object>> orderList = orderQuery.getRows();
            Assert.assertNotNull(implementation, orderList);
            Reporter.log(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)), 3, orderList.size());
        }
    }

}
