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
import rapture.common.api.DocApi;
import rapture.common.api.IndexApi;
import rapture.common.api.ScriptApi;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.IndexConfig;

public class IndexApiITest extends AbstractFileTest {

    private static final Logger log = Logger.getLogger(DocApiFileTest.class);
    private static final String REPO_USING_FILE = "REP {} USING FILE {prefix=\"/tmp/" + auth + "\"}";
    private static final String docAuthorityURI = "document://" + auth;

    private static CallingContext callingContext;
    private static DocApiImpl docImpl;

    static String planetURI = "";
    static String INDEXCFG = "";

    static DocApi document = null;
    static ScriptApi script = null;
    static IndexApi index = null;
    static CallingContext context = ContextFactory.getKernelUser();

    private static final String url = "http://192.168.99.100:8665/rapture";
    // private static final String url = "http://54.67.82.29:8665/rapture";
    private static final String username = "rapture";
    private static final String password = "rapture";
    private static IndexConfig planetIndex = null;

    @BeforeClass
    public static void setUp() {

        document = Kernel.getDoc();
        script = Kernel.getScript();
        index = Kernel.getIndex();

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
        document.putDoc(context, planetURI + "/Mercury/None",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "A", "two", new Double(1.5), "three", "constant", "inner", ImmutableMap.of("alpha", "Z"))));
        document.putDoc(context, planetURI + "/Venus/None",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "B", "two", new Double(2.4), "three", "constant", "inner", ImmutableMap.of("alpha", "Y"))));
        document.putDoc(context, planetURI + "/Earth/Moon",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "C", "two", new Double(2.6), "three", "constant", "inner", ImmutableMap.of("alpha", "X"))));
        document.putDoc(context, planetURI + "/Mars/Phobos",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "D", "two", new Integer(4), "three", "constant", "inner", ImmutableMap.of("alpha", "W"))));
        document.putDoc(context, planetURI + "/Mars/Deimos",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "E", "two", new Integer(5), "three", "constant", "inner", ImmutableMap.of("alpha", "V"))));
    }

    void data2() {
        document.putDoc(context, planetURI + "/Jupiter/Ganymede",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "F", "two", new Integer(6), "three", "constant", "inner", ImmutableMap.of("alpha", "U"))));
        document.putDoc(context, planetURI + "/Jupiter/Europa",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "G", "two", new Integer(7), "three", "constant", "inner", ImmutableMap.of("alpha", "T"))));
        document.putDoc(context, planetURI + "/Jupiter/Titan",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "H", "two", new Integer(8), "three", "constant", "inner", ImmutableMap.of("alpha", "S"))));
        document.putDoc(context, planetURI + "/Jupiter/Io",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "I", "two", new Integer(9), "three", "constant", "inner", ImmutableMap.of("alpha", "R"))));
    }

    void data3() {
        document.putDoc(context, planetURI + "/Earth/Moon/Foo",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "Q", "two", new Double(-1), "three", "constant", "inner", ImmutableMap.of("alpha", "X"))));
        document.putDoc(context, planetURI + "/Earth/Moon/Bar",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "W", "two", new Double(-2), "three", "constant", "inner", ImmutableMap.of("alpha", "X"))));
        document.putDoc(context, planetURI + "/Earth/Moon/Baz",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "E", "two", new Double(-3), "three", "constant", "inner", ImmutableMap.of("alpha", "X"))));

    }

    @Test
    public void updateDataTest() {
        for (String implementation : ImmutableList.of("FILE", "MEMORY")) {
            String authorityName = "docplanetdata1." + System.nanoTime();
            planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();

            String ver_config = "NREP {} USING " + implementation + " {prefix=\"planet.%s\"}"; // versioned repository

            // setup planet test data
            INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
            document.createDocRepo(context, planetURI, String.format(ver_config, System.nanoTime()));

            String query = "SELECT planet, moon, fieldOne, fieldTwo WHERE fieldTwo > 2.5";

            TableQueryResult res = index.findIndex(context, planetURI, query);
            List<List<Object>> resList = res.getRows();
            Reporter.log(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(resList)));
            Assert.assertNull(resList);

            data1();
            data3();

            planetIndex = index.createIndex(context, planetURI, INDEXCFG);
            Reporter.log("Index details: " + implementation + " " + planetIndex.getName(), true);

            Reporter.log("Query: " + query, true);
            res = index.findIndex(context, planetURI, query);
            resList = res.getRows();
            Reporter.log(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(resList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(resList)), 3, resList.size());

            data2();

            Reporter.log("Query: " + query, true);
            res = index.findIndex(context, planetURI, query);
            resList = res.getRows();
            Reporter.log(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(resList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(resList)), 7, resList.size());

            query = "SELECT planet, moon, fieldOne, fieldTwo WHERE fieldTwo < -1.5";
            Reporter.log("Query: " + query, true);
            res = index.findIndex(context, planetURI, query);
            resList = res.getRows();
            Reporter.log(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(resList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(resList)), 2, resList.size());

        }
    }

    @Test
    public void limitTest() {
        for (String implementation : ImmutableList.of("FILE", "MEMORY")) {
            String authorityName = "docplanetdata1." + System.nanoTime();
            planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();
            String ver_config = "NREP {} USING " + implementation + " {prefix=\"planet.%s\"}"; // versioned repository
            document.createDocRepo(context, planetURI, String.format(ver_config, System.nanoTime()));

            // setup planet test data
            INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
            planetIndex = index.createIndex(context, planetURI, INDEXCFG);

            data1();
            data2();
            data3();

            String limitQuery = "Select planet, moon limit 4";
            Reporter.log("Query: " + limitQuery, true);
            TableQueryResult res = index.findIndex(context, planetURI, limitQuery);
            List<List<Object>> limitList = res.getRows();
            Reporter.log(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(limitList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(limitList)), 4, limitList.size());

            limitQuery = "SELECT planet, moon LIMIT -1";
            Reporter.log("Query: " + limitQuery, true);
            res = index.findIndex(context, planetURI, limitQuery);
            limitList = res.getRows();
            Reporter.log(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(limitList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(limitList)), 12, limitList.size());

            limitQuery = "SELECT DISTINCT planet, moon ORDER BY planet, moon ASC LIMIT 2";
            Reporter.log("Query: " + limitQuery, true);
            res = index.findIndex(context, planetURI, limitQuery);
            limitList = res.getRows();
            Reporter.log(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(limitList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(limitList)), 2, limitList.size());

            limitQuery = "select distinct planet, moon ORDER BY planet, moon Asc Limit 2 Skip 2";

            Reporter.log("Query: " + limitQuery, true);
            res = index.findIndex(context, planetURI, limitQuery);
            List<List<Object>> limitList2 = res.getRows();
            Reporter.log(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(limitList2)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(limitList2)), 2, limitList.size());

            System.out.println(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(limitList)));
            System.out.println(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(limitList2)));

            Assert.assertEquals("Earth", limitList.get(0).get(0).toString());
            Assert.assertEquals("Jupiter", limitList.get(1).get(0).toString());
            Assert.assertEquals("Europa", limitList.get(1).get(1).toString());

            Assert.assertEquals("Jupiter", limitList2.get(0).get(0).toString());
            Assert.assertEquals("Ganymede", limitList2.get(0).get(1).toString());
            Assert.assertEquals("Jupiter", limitList2.get(1).get(0).toString());
            Assert.assertEquals("Io", limitList2.get(1).get(1).toString());
        }
    }

    @Test
    public void distinctTest() {
        for (String implementation : ImmutableList.of("FILE", "MEMORY")) {
            String authorityName = "docplanetdata1." + System.nanoTime();
            planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();

            String ver_config = "NREP {} USING " + implementation + " {prefix=\"planet.%s\"}"; // versioned repository
            document.createDocRepo(context, planetURI, String.format(ver_config, System.nanoTime()));

            // setup planet test data
            INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
            planetIndex = index.createIndex(context, planetURI, INDEXCFG);

            data1();
            data2();

            // RAP-3685
            TableQueryResult orderQuery = index.findIndex(context, planetURI, "SELECT DISTINCT planet");
            Reporter.log("Query: " + orderQuery, true);
            List<List<Object>> orderList = orderQuery.getRows();
            Assert.assertNotNull(implementation, orderList);
            Reporter.log(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)), 5, orderList.size());
        }
    }

    @Test
    public void orderAscTest() {
        for (String implementation : ImmutableList.of("FILE", "MEMORY")) {
            String authorityName = "docplanetdata1." + System.nanoTime();
            planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();

            String ver_config = "NREP {} USING " + implementation + " {prefix=\"planet.%s\"}"; // versioned repository
            document.createDocRepo(context, planetURI, String.format(ver_config, System.nanoTime()));

            // setup planet test data
            INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
            planetIndex = index.createIndex(context, planetURI, INDEXCFG);

            data1();
            data2();

            // RAP-3685
            TableQueryResult orderQuery = index.findIndex(context, planetURI, "SELECT DISTINCT planet");
            Reporter.log("Query: " + orderQuery, true);
            List<List<Object>> orderList = orderQuery.getRows();
            Assert.assertNotNull(implementation, orderList);
            System.out.println(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)), 5, orderList.size());

            orderQuery = index.findIndex(context, planetURI, "SELECT DISTINCT moon");
            Reporter.log("Query: " + orderQuery, true);
            orderList = orderQuery.getRows();
            Assert.assertNotNull(implementation, orderList);
            System.out.println(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)), 8, orderList.size());

            data3();

            orderQuery = index.findIndex(context, planetURI, "SELECT DISTINCT planet, moon ORDER BY moon, planet ASC");
            Reporter.log("Query: " + orderQuery, true);
            orderList = orderQuery.getRows();
            Assert.assertNotNull(implementation, orderList);
            System.out.println(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)), 9, orderList.size());
            String last = "Aaaa";
            for (List<Object> next : orderList) {
                String nextStr = next.get(1).toString();
                Assert.assertTrue(implementation + " : " + JacksonUtil.jsonFromObject(orderList), nextStr.compareTo(last) >= 0);
                last = nextStr;
            }
            orderQuery = index.findIndex(context, planetURI, "SELECT planet, moon Order By moon, planet ASC");
            Reporter.log("Query: " + orderQuery, true);
            orderList = orderQuery.getRows();
            Assert.assertNotNull(implementation, orderList);
            System.out.println(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)), 12, orderList.size());
            last = "Aaaa";
            for (List<Object> next : orderList) {
                String nextStr = next.get(1).toString();
                Assert.assertTrue(implementation + " : " + JacksonUtil.jsonFromObject(orderList), nextStr.compareTo(last) >= 0);
                last = nextStr;
            }
            orderQuery = index.findIndex(context, planetURI, "SELECT planet, moon ORDER BY moon, planet ASC");
            Reporter.log("Query: " + orderQuery, true);
            orderList = orderQuery.getRows();
            Assert.assertNotNull(implementation, orderList);
            System.out.println(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)), 12, orderList.size());
            last = "Aaaa";
            for (List<Object> next : orderList) {
                String nextStr = next.get(1).toString();
                Assert.assertTrue(implementation + " : " + JacksonUtil.jsonFromObject(orderList), nextStr.compareTo(last) >= 0);
                last = nextStr;
            }
        }
    }

    @Test
    public void orderDescTest() {

        for (String implementation : ImmutableList.of("FILE", "MEMORY")) {
            String authorityName = "docplanetdata1." + System.nanoTime();
            planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();

            String ver_config = "NREP {} USING " + implementation + " {prefix=\"planet.%s\"}"; // versioned repository
            document.createDocRepo(context, planetURI, String.format(ver_config, System.nanoTime()));

            // setup planet test data
            INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
            planetIndex = index.createIndex(context, planetURI, INDEXCFG);

            data1();
            data2();

            // RAP-3685
            TableQueryResult orderQuery = index.findIndex(context, planetURI, "SELECT DISTINCT planet ORDER BY planet DESC");
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
        for (String implementation : ImmutableList.of("FILE", "MEMORY")) {
            String authorityName = "docplanetdata1." + System.nanoTime();
            planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();

            String ver_config = "NREP {} USING " + implementation + " {prefix=\"planet.%s\"}"; // versioned repository
            document.createDocRepo(context, planetURI, String.format(ver_config, System.nanoTime()));

            // setup planet test data
            INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
            planetIndex = index.createIndex(context, planetURI, INDEXCFG);

            data1();
            data2();

            // RAP-3685
            TableQueryResult orderQuery = index.findIndex(context, planetURI, "SELECT DISTINCT planet ORDER BY planet DESC LIMIT 3");
            Reporter.log("Query: " + orderQuery, true);
            List<List<Object>> orderList = orderQuery.getRows();
            Assert.assertNotNull(implementation, orderList);
            Reporter.log(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)));
            Assert.assertEquals(implementation + " : " + JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(orderList)), 3, orderList.size());
        }
    }
}
