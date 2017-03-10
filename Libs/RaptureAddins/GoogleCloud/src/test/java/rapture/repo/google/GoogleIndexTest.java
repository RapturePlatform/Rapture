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
package rapture.repo.google;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.joda.time.Duration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.cloud.datastore.testing.LocalDatastoreHelper;
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
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class GoogleIndexTest {
    private static CallingContext ctx = ContextFactory.getKernelUser();
    static String auth = UUID.randomUUID().toString();
    static DocApi document = null;
    static ScriptApi script = null;
    static IndexApi index = null;
    static CallingContext context = ContextFactory.getKernelUser();

    private static IndexConfig planetIndex = null;
    final String authorityName = "docplanetdata1." + System.nanoTime();
    static LocalDatastoreHelper helper = LocalDatastoreHelper.create();

    @BeforeClass
    public static void setUp() {
        System.setProperty("LOGSTASH-ISENABLED", "false");
        try {
            GoogleDatastoreKeyStore.setDatastoreOptionsForTesting(helper.getOptions());
            GoogleIndexHandler.setDatastoreOptionsForTesting(helper.getOptions());
            helper.start();
        } catch (IOException | InterruptedException e) {
            Assume.assumeNoException("Cannot start helper " + e.getMessage(), e);
        } // Starts the local Datastore emulator in a separate process
        document = Kernel.getDoc();
        script = Kernel.getScript();
        index = Kernel.getIndex();
        Kernel.initBootstrap();

        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using FILE {prefix=\"/tmp/" + auth + "\"}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");
        if (!Kernel.getDoc().docRepoExists(ctx, "//docTest")) Kernel.getDoc().createDocRepo(ctx, "//docTest",
                "NREP {} USING GCP_DATASTORE {prefix =\"" + auth + "\"}");
        Kernel.getIndex().createIndex(ctx, "//docTest", "field1($0) string, field2(test) string");
    }

    @Before
    public void setup() throws IOException, InterruptedException {
    }

    @After
    public void clean() {
        planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();
        document.deleteDocRepo(context, planetURI);
    }

    @AfterClass
    public static void cleanUp() {
        Kernel.getDoc().deleteDocRepo(ctx, "//docTest");
        Kernel.getIndex().deleteIndex(ctx, "//docTest");
        try {
            helper.stop(new Duration(6000L));
        } catch (Exception e) {
            System.out.println("Exception shutting down LocalDatastoreHelper: " + e.getMessage());
        }
    }

    @Test
    public void writeADoc() {
        String doc = "//docTest/one/two";
        String content = "{ \"test\" : \"hello\" }";
        Kernel.getDoc().putDoc(ctx, doc, content);
    }

    static String planetURI = "";
    static String INDEXCFG = "";

    void data1() {
        document.putDoc(context, planetURI + "/Mercury/None", JacksonUtil
                .jsonFromObject(ImmutableMap.of("one", "A", "two", new Double(1.5), "three", "constant", "inner", ImmutableMap.of("alpha", "Z")), true));
        document.putDoc(context, planetURI + "/Venus/None", JacksonUtil
                .jsonFromObject(ImmutableMap.of("one", "B", "two", new Double(2.4), "three", "constant", "inner", ImmutableMap.of("alpha", "Y")), true));
        document.putDoc(context, planetURI + "/Earth/Moon", JacksonUtil
                .jsonFromObject(ImmutableMap.of("one", "C", "two", new Double(2.6), "three", "constant", "inner", ImmutableMap.of("alpha", "X")), true));
        document.putDoc(context, planetURI + "/Mars/Phobos", JacksonUtil
                .jsonFromObject(ImmutableMap.of("one", "D", "two", new Integer(4), "three", "constant", "inner", ImmutableMap.of("alpha", "W")), true));
        document.putDoc(context, planetURI + "/Mars/Deimos", JacksonUtil
                .jsonFromObject(ImmutableMap.of("one", "E", "two", new Integer(5), "three", "constant", "inner", ImmutableMap.of("alpha", "V")), true));
    }

    void data2() {
        document.putDoc(context, planetURI + "/Jupiter/Ganymede", JacksonUtil
                .jsonFromObject(ImmutableMap.of("one", "F", "two", new Integer(6), "three", "constant", "inner", ImmutableMap.of("alpha", "U")), true));
        document.putDoc(context, planetURI + "/Jupiter/Europa", JacksonUtil
                .jsonFromObject(ImmutableMap.of("one", "G", "two", new Integer(7), "three", "constant", "inner", ImmutableMap.of("alpha", "T")), true));
        document.putDoc(context, planetURI + "/Jupiter/Titan", JacksonUtil
                .jsonFromObject(ImmutableMap.of("one", "H", "two", new Integer(8), "three", "constant", "inner", ImmutableMap.of("alpha", "S")), true));
        document.putDoc(context, planetURI + "/Jupiter/Io", JacksonUtil
                .jsonFromObject(ImmutableMap.of("one", "I", "two", new Integer(9), "three", "constant", "inner", ImmutableMap.of("alpha", "R")), true));
    }

    void data3() {
        document.putDoc(context, planetURI + "/Earth/Moon/Foo", JacksonUtil
                .jsonFromObject(ImmutableMap.of("one", "Q", "two", new Double(-1), "three", "constant", "inner", ImmutableMap.of("alpha", "X")), true));
        document.putDoc(context, planetURI + "/Earth/Moon/Bar", JacksonUtil
                .jsonFromObject(ImmutableMap.of("one", "W", "two", new Double(-2), "three", "constant", "inner", ImmutableMap.of("alpha", "X")), true));
        document.putDoc(context, planetURI + "/Earth/Moon/Baz", JacksonUtil
                .jsonFromObject(ImmutableMap.of("one", "E", "two", new Double(-3), "three", "constant", "inner", ImmutableMap.of("alpha", "X")), true));

    }

    @Test
    public void updateDataTest() {
        planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();

        String ver_config = "NREP {} USING GCP_DATASTORE {prefix=\"planet.%s\"}"; // versioned repository

        // setup planet test data
        INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
        document.createDocRepo(context, planetURI, String.format(ver_config, System.nanoTime()));

        String query = "SELECT planet, moon, fieldOne, fieldTwo WHERE fieldTwo > 2.5";

        TableQueryResult res = index.findIndex(context, planetURI, query);
        List<List<Object>> resList = res.getRows();
        System.out.println(JacksonUtil.jsonFromObject(resList, true));
        Assert.assertNull(resList);

        data1();
        data3();

        planetIndex = index.createIndex(context, planetURI, INDEXCFG);
        System.out.println("Index details: GCP_DATASTORE " + planetIndex.getName());

        System.out.println("Query: " + query);
        res = index.findIndex(context, planetURI, query);
        resList = res.getRows();
        System.out.println(JacksonUtil.jsonFromObject(resList, true));
        Assert.assertEquals(3, resList.size());

        data2();

        System.out.println("Query: " + query);
        res = index.findIndex(context, planetURI, query);
        resList = res.getRows();
        System.out.println(JacksonUtil.jsonFromObject(resList, true));
        Assert.assertEquals(7, resList.size());

        query = "SELECT planet, moon, fieldOne, fieldTwo WHERE fieldTwo < -1.5";
        System.out.println("Query: " + query);
        res = index.findIndex(context, planetURI, query);
        resList = res.getRows();
        System.out.println(JacksonUtil.jsonFromObject(resList, true));
        Assert.assertEquals(2, resList.size());

        query = "SELECT planet, moon, fieldOne, fieldTwo ORDER BY fieldTwo";
        System.out.println("Query: " + query);
        res = index.findIndex(context, planetURI, query);
        resList = res.getRows();
        System.out.println(JacksonUtil.jsonFromObject(resList, true));
        Assert.assertNotNull(resList);
        Assert.assertEquals(12, resList.size());
        for (int i = 1; i < resList.size(); i++) {
            Number n0 = (Number) resList.get(i - 1).get(3);
            Number n1 = (Number) resList.get(i).get(3);
            Assert.assertTrue(n1.doubleValue() > n0.doubleValue());
        }
    }

    @Test
    public void limitTest() {
        planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();
        String ver_config = "NREP {} USING GCP_DATASTORE {prefix=\"planet.%s\"}"; // versioned repository
        document.createDocRepo(context, planetURI, String.format(ver_config, System.nanoTime()));

        // setup planet test data
        INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
        planetIndex = index.createIndex(context, planetURI, INDEXCFG);

        data1();
        data2();
        data3();

        String limitQuery = "Select planet, moon limit 4";
        System.out.println("Query: " + limitQuery);
        TableQueryResult res = index.findIndex(context, planetURI, limitQuery);
        List<List<Object>> limitList = res.getRows();
        System.out.println(JacksonUtil.jsonFromObject(limitList, true));
        Assert.assertEquals(JacksonUtil.jsonFromObject(limitList, true), 4, limitList.size());

        limitQuery = "SELECT planet, moon LIMIT -1";
        System.out.println("Query: " + limitQuery);
        res = index.findIndex(context, planetURI, limitQuery);
        limitList = res.getRows();
        System.out.println(JacksonUtil.jsonFromObject(limitList, true));
        Assert.assertEquals(JacksonUtil.jsonFromObject(limitList, true), 1, limitList.size());

        limitQuery = "SELECT DISTINCT planet, moon ORDER BY planet, moon ASC LIMIT 2";
        System.out.println("Query: " + limitQuery);
        res = index.findIndex(context, planetURI, limitQuery);
        limitList = res.getRows();
        System.out.println(JacksonUtil.jsonFromObject(limitList, true));
        Assert.assertEquals(JacksonUtil.jsonFromObject(limitList, true), 2, limitList.size());

        limitQuery = "select distinct planet, moon ORDER BY planet, moon Asc Limit 2 Skip 2";

        System.out.println("Query: " + limitQuery);
        res = index.findIndex(context, planetURI, limitQuery);
        List<List<Object>> limitList2 = res.getRows();
        System.out.println(JacksonUtil.jsonFromObject(limitList2, true));
        Assert.assertEquals(JacksonUtil.jsonFromObject(limitList2, true), 2, limitList.size());

        System.out.println(JacksonUtil.jsonFromObject(limitList, true));
        System.out.println(JacksonUtil.jsonFromObject(limitList2, true));

        Assert.assertEquals("Earth", limitList.get(0).get(0).toString());
        Assert.assertEquals("Jupiter", limitList.get(1).get(0).toString());
        Assert.assertEquals("Europa", limitList.get(1).get(1).toString());

        Assert.assertEquals("Jupiter", limitList2.get(0).get(0).toString());
        Assert.assertEquals("Ganymede", limitList2.get(0).get(1).toString());
        Assert.assertEquals("Jupiter", limitList2.get(1).get(0).toString());
        Assert.assertEquals("Io", limitList2.get(1).get(1).toString());
    }

    @Test
    public void distinctTest() {

        planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();

        String ver_config = "NREP {} USING GCP_DATASTORE {prefix=\"planet.%s\"}"; // versioned repository
        document.createDocRepo(context, planetURI, String.format(ver_config, System.nanoTime()));

        // setup planet test data
        INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
        planetIndex = index.createIndex(context, planetURI, INDEXCFG);

        data1();
        data2();

        // RAP-3685
        TableQueryResult orderQuery = index.findIndex(context, planetURI, "SELECT DISTINCT planet");
        System.out.println("Query: " + orderQuery);
        List<List<Object>> orderList = orderQuery.getRows();
        Assert.assertNotNull(orderList);
        System.out.println(JacksonUtil.jsonFromObject(orderList, true));
        Assert.assertEquals(JacksonUtil.jsonFromObject(orderList, true), 5, orderList.size());
    }

    @Test
    public void orderAscTest() {
        planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();

        String ver_config = "NREP {} USING GCP_DATASTORE {prefix=\"planet.%s\"}"; // versioned repository
        document.createDocRepo(context, planetURI, String.format(ver_config, System.nanoTime()));

        // setup planet test data
        INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
        planetIndex = index.createIndex(context, planetURI, INDEXCFG);

        data1();
        data2();

        // RAP-3685
        TableQueryResult orderQuery = index.findIndex(context, planetURI, "SELECT DISTINCT planet");
        System.out.println("Query: " + orderQuery);
        List<List<Object>> orderList = orderQuery.getRows();
        Assert.assertNotNull(orderList);
        System.out.println(JacksonUtil.jsonFromObject(orderList, true));
        Assert.assertEquals(JacksonUtil.jsonFromObject(orderList, true), 5, orderList.size());

        orderQuery = index.findIndex(context, planetURI, "SELECT DISTINCT moon");
        System.out.println("Query: " + orderQuery);
        orderList = orderQuery.getRows();
        Assert.assertNotNull(orderList);
        System.out.println(JacksonUtil.jsonFromObject(orderList, true));
        Assert.assertEquals(JacksonUtil.jsonFromObject(orderList, true), 8, orderList.size());

        data3();

        orderQuery = index.findIndex(context, planetURI, "SELECT DISTINCT planet, moon ORDER BY moon, planet ASC");
        System.out.println("Query: " + orderQuery);
        orderList = orderQuery.getRows();
        Assert.assertNotNull(orderList);
        System.out.println(JacksonUtil.jsonFromObject(orderList, true));
        Assert.assertEquals(JacksonUtil.jsonFromObject(orderList, true), 9, orderList.size());
        String last = "Aaaa";
        for (List<Object> next : orderList) {
            String nextStr = next.get(1).toString();
            Assert.assertTrue(JacksonUtil.jsonFromObject(orderList, true), nextStr.compareTo(last) >= 0);
            last = nextStr;
        }
        orderQuery = index.findIndex(context, planetURI, "SELECT planet, moon Order By moon, planet ASC");
        System.out.println("Query: " + orderQuery);
        orderList = orderQuery.getRows();
        Assert.assertNotNull(orderList);
        System.out.println(JacksonUtil.jsonFromObject(orderList, true));
        Assert.assertEquals(JacksonUtil.jsonFromObject(orderList, true), 12, orderList.size());
        last = "Aaaa";
        for (List<Object> next : orderList) {
            String nextStr = next.get(1).toString();
            Assert.assertTrue(JacksonUtil.jsonFromObject(orderList, true), nextStr.compareTo(last) >= 0);
            last = nextStr;
        }
        orderQuery = index.findIndex(context, planetURI, "SELECT planet, moon ORDER BY moon, planet ASC");
        System.out.println("Query: " + orderQuery);
        orderList = orderQuery.getRows();
        Assert.assertNotNull(orderList);
        System.out.println(JacksonUtil.jsonFromObject(orderList, true));
        Assert.assertEquals(JacksonUtil.jsonFromObject(orderList, true), 12, orderList.size());
        last = "Aaaa";
        for (List<Object> next : orderList) {
            String nextStr = next.get(1).toString();
            Assert.assertTrue(JacksonUtil.jsonFromObject(orderList, true), nextStr.compareTo(last) >= 0);
            last = nextStr;
        }
    }

    @Test
    public void orderDescTest() {
        planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();

        String ver_config = "NREP {} USING GCP_DATASTORE {prefix=\"planet.%s\"}"; // versioned repository
        document.createDocRepo(context, planetURI, String.format(ver_config, System.nanoTime()));

        // setup planet test data
        INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
        planetIndex = index.createIndex(context, planetURI, INDEXCFG);

        data1();
        data2();

        // RAP-3685
        TableQueryResult orderQuery = index.findIndex(context, planetURI, "SELECT DISTINCT planet ORDER BY planet DESC");
        System.out.println("Query: " + orderQuery);
        List<List<Object>> orderList = orderQuery.getRows();
        Assert.assertNotNull(orderList);
        System.out.println(JacksonUtil.jsonFromObject(orderList, true));
        Assert.assertEquals(JacksonUtil.jsonFromObject(orderList, true), 5, orderList.size());
        String last = "Zzzz";
        for (List<Object> next : orderList) {
            String nextStr = next.get(0).toString();
            Assert.assertTrue(JacksonUtil.jsonFromObject(orderList, true), nextStr.compareTo(last) < 0);
            last = nextStr;
        }
    }

    @Test
    public void orderLimitTest() {
        planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();

        String ver_config = "NREP {} USING GCP_DATASTORE {prefix=\"planet.%s\"}"; // versioned repository
        document.createDocRepo(context, planetURI, String.format(ver_config, System.nanoTime()));

        // setup planet test data
        INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
        planetIndex = index.createIndex(context, planetURI, INDEXCFG);

        data1();
        data2();

        // RAP-3685
        TableQueryResult orderQuery = index.findIndex(context, planetURI, "SELECT DISTINCT planet ORDER BY planet DESC LIMIT 3");
        System.out.println("Query: " + orderQuery);
        List<List<Object>> orderList = orderQuery.getRows();
        Assert.assertNotNull(orderList);
        System.out.println(JacksonUtil.jsonFromObject(orderList, true));
        Assert.assertEquals(3, orderList.size());
    }

    @Test
    public void deleteTest() {
        // RAP-4343
        planetURI = RaptureURI.builder(Scheme.DOCUMENT, authorityName).build().toString();

        String ver_config = "NREP {} USING GCP_DATASTORE {prefix=\"planet.%s\"}"; // versioned repository
        document.createDocRepo(context, planetURI, String.format(ver_config, System.nanoTime()));

        // setup planet test data
        INDEXCFG = "planet($0) string, moon($1) string, fieldOne(one) string, fieldTwo(two) integer, fieldInner(inner.alpha) string";
        planetIndex = index.createIndex(context, planetURI, INDEXCFG);
        data1();

        String doc = document.getDoc(context, planetURI + "/Mercury/None");
        Assert.assertNotNull(doc);

        TableQueryResult orderQuery = index.findIndex(context, planetURI, "SELECT DISTINCT planet ORDER BY planet");
        List<List<Object>> orderList = orderQuery.getRows();
        Assert.assertEquals(4, orderList.size());

        document.deleteDoc(context, planetURI + "/Mercury/None");
        doc = document.getDoc(context, planetURI + "/Mercury/None");
        Assert.assertNull(doc);
        orderQuery = index.findIndex(context, planetURI, "SELECT DISTINCT planet ORDER BY planet");
        orderList = orderQuery.getRows();
        Assert.assertEquals(3, orderList.size());

    }
}
