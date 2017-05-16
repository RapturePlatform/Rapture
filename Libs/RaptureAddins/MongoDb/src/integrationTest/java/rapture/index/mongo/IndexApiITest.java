package rapture.index.mongo;

import java.util.List;

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
import rapture.kernel.AbstractFileTest;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class IndexApiITest extends AbstractFileTest {

    private static final String REPO_USING_MONGODB = "REP {} USING MONGODB {prefix=\"/tmp/" + auth + "\"}";
    private static CallingContext callingContext;

    static String planetURI = "";
    static String INDEXCFG = "";

    static DocApi document = null;
    static ScriptApi script = null;
    static IndexApi index = null;
    static CallingContext context = ContextFactory.getKernelUser();

    private static IndexConfig planetIndex = null;

    @BeforeClass
    public static void setUp() {

        document = Kernel.getDoc();
        script = Kernel.getScript();
        index = Kernel.getIndex();

        AbstractFileTest.setUp();
        config.RaptureRepo = REPO_USING_MONGODB;
        config.InitSysConfig = "NREP {} USING MONGODB { prefix=\"/tmp/" + auth + "/sys.config\"}";
        // config.DefaultPipelineTaskStatus = "TABLE {} USING MONGODB {prefix=\"/tmp/" + auth + "\"}";

        callingContext = new CallingContext();
        callingContext.setUser("dummy");

        Kernel.initBootstrap();
        callingContext = ContextFactory.getKernelUser();

        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MONGODB {prefix=\"/tmp/" + auth + "\"}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");

    }

    void data1() {
        document.putDoc(context, planetURI + "/Mercury/None",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "A", "two", new Double(1.5), "three", "constant", "inner", ImmutableMap.of("alpha", "Z")),
                        true));
        document.putDoc(context, planetURI + "/Venus/None",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "B", "two", new Double(2.4), "three", "constant", "inner", ImmutableMap.of("alpha", "Y")),
                        true));
        document.putDoc(context, planetURI + "/Earth/Moon",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "C", "two", new Double(2.6), "three", "constant", "inner", ImmutableMap.of("alpha", "X")),
                        true));
        document.putDoc(context, planetURI + "/Mars/Phobos",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "D", "two", new Integer(4), "three", "constant", "inner", ImmutableMap.of("alpha", "W")),
                        true));
        document.putDoc(context, planetURI + "/Mars/Deimos",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "E", "two", new Integer(5), "three", "constant", "inner", ImmutableMap.of("alpha", "V")),
                        true));
    }

    void data2() {
        document.putDoc(context, planetURI + "/Jupiter/Ganymede",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "F", "two", new Integer(6), "three", "constant", "inner", ImmutableMap.of("alpha", "U")),
                        true));
        document.putDoc(context, planetURI + "/Jupiter/Europa",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "G", "two", new Integer(7), "three", "constant", "inner", ImmutableMap.of("alpha", "T")),
                        true));
        document.putDoc(context, planetURI + "/Jupiter/Titan",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "H", "two", new Integer(8), "three", "constant", "inner", ImmutableMap.of("alpha", "S")),
                        true));
        document.putDoc(context, planetURI + "/Jupiter/Io",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "I", "two", new Integer(9), "three", "constant", "inner", ImmutableMap.of("alpha", "R")),
                        true));
    }

    void data3() {
        document.putDoc(context, planetURI + "/Earth/Moon/Foo",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "Q", "two", new Double(-1), "three", "constant", "inner", ImmutableMap.of("alpha", "X")),
                        true));
        document.putDoc(context, planetURI + "/Earth/Moon/Bar",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "W", "two", new Double(-2), "three", "constant", "inner", ImmutableMap.of("alpha", "X")),
                        true));
        document.putDoc(context, planetURI + "/Earth/Moon/Baz",
                JacksonUtil.jsonFromObject(ImmutableMap.of("one", "E", "two", new Double(-3), "three", "constant", "inner", ImmutableMap.of("alpha", "X")),
                        true));

    }

    @Test
    public void updateDataTest() {
        for (String implementation : ImmutableList.of("MONGODB")) {
            String authorityName = "docplanetdata1." + System.nanoTime();
            planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();

            String ver_config = "NREP {} USING " + implementation + " {prefix=\"planet.%s\"}"; // versioned repository

            // setup planet test data
            INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
            document.createDocRepo(context, planetURI, String.format(ver_config, System.nanoTime()));

            String query = "SELECT planet, moon, fieldOne, fieldTwo WHERE fieldTwo > 2.5";

            TableQueryResult res = index.findIndex(context, planetURI, query);
            List<List<Object>> resList = res.getRows();
            Reporter.log(JacksonUtil.jsonFromObject(resList, true));
            Assert.assertNull(resList);

            data1();
            data3();

            planetIndex = index.createIndex(context, planetURI, INDEXCFG);
            Reporter.log("Index details: " + implementation + " " + planetIndex.getName(), true);

            Reporter.log("Query: " + query, true);
            res = index.findIndex(context, planetURI, query);
            resList = res.getRows();
            Reporter.log(JacksonUtil.jsonFromObject(resList, true));
            Assert.assertEquals(implementation + " : " + JacksonUtil.jsonFromObject(resList, true), 3, resList.size());

            data2();

            Reporter.log("Query: " + query, true);
            res = index.findIndex(context, planetURI, query);
            resList = res.getRows();
            Reporter.log(JacksonUtil.jsonFromObject(resList, true));
            Assert.assertEquals(implementation + " : " + JacksonUtil.jsonFromObject(resList, true), 7, resList.size());

            query = "SELECT planet, moon, fieldOne, fieldTwo WHERE fieldTwo < -1.5";
            Reporter.log("Query: " + query, true);
            res = index.findIndex(context, planetURI, query);
            resList = res.getRows();
            Reporter.log(JacksonUtil.jsonFromObject(resList, true));
            Assert.assertEquals(implementation + " : " + JacksonUtil.jsonFromObject(resList, true), 2, resList.size());

            query = "SELECT planet, moon, fieldOne, fieldTwo ORDER BY fieldTwo";
            Reporter.log("Query: " + query, true);
            res = index.findIndex(context, planetURI, query);
            resList = res.getRows();
            Reporter.log(JacksonUtil.jsonFromObject(resList, true));
            Assert.assertNotNull(resList);
            Assert.assertEquals(implementation + " : " + JacksonUtil.jsonFromObject(resList, true), 12, resList.size());
            for (int i = 1; i < resList.size(); i++) {
                Number n0 = (Number) resList.get(i - 1).get(3);
                Number n1 = (Number) resList.get(i).get(3);
                Assert.assertTrue(n1.doubleValue() > n0.doubleValue());
            }
        }
    }

    @Test
    public void limitTest() {
        for (String implementation : ImmutableList.of("MONGODB")) {
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
            Reporter.log(implementation + " : " + JacksonUtil.jsonFromObject(limitList, true));
            Assert.assertEquals(implementation + " : " + JacksonUtil.jsonFromObject(limitList, true), 4, limitList.size());

            limitQuery = "SELECT planet, moon LIMIT -1";
            Reporter.log("Query: " + limitQuery, true);
            res = index.findIndex(context, planetURI, limitQuery);
            limitList = res.getRows();
            Reporter.log(implementation + " : " + JacksonUtil.jsonFromObject(limitList, true));
            Assert.assertEquals(implementation + " : " + JacksonUtil.jsonFromObject(limitList, true), 1, limitList.size());

            limitQuery = "SELECT DISTINCT planet, moon ORDER BY planet, moon ASC LIMIT 2";
            Reporter.log("Query: " + limitQuery, true);
            res = index.findIndex(context, planetURI, limitQuery);
            limitList = res.getRows();
            Reporter.log(implementation + " : " + JacksonUtil.jsonFromObject(limitList, true));
            Assert.assertEquals(implementation + " : " + JacksonUtil.jsonFromObject(limitList, true), 2, limitList.size());

            limitQuery = "select distinct planet, moon ORDER BY planet, moon Asc Limit 2 Skip 2";

            Reporter.log("Query: " + limitQuery, true);
            res = index.findIndex(context, planetURI, limitQuery);
            List<List<Object>> limitList2 = res.getRows();
            Reporter.log(implementation + " : " + JacksonUtil.jsonFromObject(limitList2, true));
            Assert.assertEquals(implementation + " : " + JacksonUtil.jsonFromObject(limitList2, true), 2, limitList.size());

            System.out.println(implementation + " : " + JacksonUtil.jsonFromObject(limitList, true));
            System.out.println(implementation + " : " + JacksonUtil.jsonFromObject(limitList2, true));

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
        for (String implementation : ImmutableList.of("MONGODB")) {
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
            Reporter.log(JacksonUtil.jsonFromObject(orderList, true));
            Assert.assertEquals(implementation + " : " + JacksonUtil.jsonFromObject(orderList, true), 5, orderList.size());
        }
    }

    @Test
    public void orderAscTest() {
        for (String implementation : ImmutableList.of("MONGODB")) {
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
            System.out.println(JacksonUtil.jsonFromObject(orderList, true));
            Assert.assertEquals(implementation + " : " + JacksonUtil.jsonFromObject(orderList, true), 5, orderList.size());

            orderQuery = index.findIndex(context, planetURI, "SELECT DISTINCT moon");
            Reporter.log("Query: " + orderQuery, true);
            orderList = orderQuery.getRows();
            Assert.assertNotNull(implementation, orderList);
            System.out.println(JacksonUtil.jsonFromObject(orderList, true));
            Assert.assertEquals(implementation + " : " + JacksonUtil.jsonFromObject(orderList, true), 8, orderList.size());

            data3();

            orderQuery = index.findIndex(context, planetURI, "SELECT DISTINCT planet, moon ORDER BY moon, planet ASC");
            Reporter.log("Query: " + orderQuery, true);
            orderList = orderQuery.getRows();
            Assert.assertNotNull(implementation, orderList);
            System.out.println(JacksonUtil.jsonFromObject(orderList, true));
            Assert.assertEquals(implementation + " : " + JacksonUtil.jsonFromObject(orderList, true), 9, orderList.size());
            String last = "Aaaa";
            for (List<Object> next : orderList) {
                String nextStr = next.get(1).toString();
                Assert.assertTrue(implementation + " : " + JacksonUtil.jsonFromObject(orderList, true), nextStr.compareTo(last) >= 0);
                last = nextStr;
            }
            orderQuery = index.findIndex(context, planetURI, "SELECT planet, moon Order By moon, planet ASC");
            Reporter.log("Query: " + orderQuery, true);
            orderList = orderQuery.getRows();
            Assert.assertNotNull(implementation, orderList);
            System.out.println(JacksonUtil.jsonFromObject(orderList, true));
            Assert.assertEquals(implementation + " : " + JacksonUtil.jsonFromObject(orderList, true), 12, orderList.size());
            last = "Aaaa";
            for (List<Object> next : orderList) {
                String nextStr = next.get(1).toString();
                Assert.assertTrue(implementation + " : " + JacksonUtil.jsonFromObject(orderList, true), nextStr.compareTo(last) >= 0);
                last = nextStr;
            }
            orderQuery = index.findIndex(context, planetURI, "SELECT planet, moon ORDER BY moon, planet ASC");
            Reporter.log("Query: " + orderQuery, true);
            orderList = orderQuery.getRows();
            Assert.assertNotNull(implementation, orderList);
            System.out.println(JacksonUtil.jsonFromObject(orderList, true));
            Assert.assertEquals(implementation + " : " + JacksonUtil.jsonFromObject(orderList, true), 12, orderList.size());
            last = "Aaaa";
            for (List<Object> next : orderList) {
                String nextStr = next.get(1).toString();
                Assert.assertTrue(implementation + " : " + JacksonUtil.jsonFromObject(orderList, true), nextStr.compareTo(last) >= 0);
                last = nextStr;
            }
        }
    }

    @Test
    public void orderDescTest() {

        for (String implementation : ImmutableList.of("MONGODB")) {
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
            System.out.println(JacksonUtil.jsonFromObject(orderList, true));
            Assert.assertEquals(implementation + " : " + JacksonUtil.jsonFromObject(orderList, true), 5, orderList.size());
            String last = "Zzzz";
            for (List<Object> next : orderList) {
                String nextStr = next.get(0).toString();
                Assert.assertTrue(implementation + " : " + JacksonUtil.jsonFromObject(orderList, true), nextStr.compareTo(last) < 0);
                last = nextStr;
            }
        }
    }

    @Test
    public void orderLimitTest() {
        for (String implementation : ImmutableList.of("MONGODB")) {
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
            Reporter.log(JacksonUtil.jsonFromObject(orderList, true));
            Assert.assertEquals(implementation + " : " + JacksonUtil.jsonFromObject(orderList, true), 3, orderList.size());
        }
    }
}
