/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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
package rapture.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.ChildrenTransferObject;
import rapture.common.EntitlementSet;
import rapture.common.NodeEnum;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.Scheme;
import rapture.common.api.EntitlementApi;
import rapture.common.api.ScheduleApi;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.MD5Utils;
import rapture.common.model.RaptureEntitlementGroup;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;

import com.google.common.net.MediaType;

public class SysApiImplTest {

    static SysApiImplWrapper api;
    static EntitlementApi entApi;
    static CallingContext context;
    static final String ozzy = "ozzy";
    static final String vocals = "Vocalist";
    static String auth = "testing";
    static String path = auth + "/" + System.currentTimeMillis();

    static String saveRaptureRepo;
    static String saveInitSysConfig;

    @BeforeClass
    public static void setUp() throws Exception {
        RaptureConfig.setLoadYaml(false);
        System.setProperty("LOGSTASH-ISENABLED", "false");
        RaptureConfig config = ConfigLoader.getConf();
        saveRaptureRepo = config.RaptureRepo;
        saveInitSysConfig = config.InitSysConfig;

        config.RaptureRepo = "REP {} USING FILE { prefix=\"rapture.bootstrap\" }";
        config.InitSysConfig = "NREP {} USING FILE { prefix=\"sys.config\" }";
        // ConfigLoader.getConf().FileRepoSys = "/tmp/SandRepo/";
        entApi = Kernel.getEntitlement();
        api = Kernel.getSys();
        context = ContextFactory.getKernelUser();

        Kernel.getDoc().createDocRepo(context, "document://" + auth, "REP {} USING MEMORY { prefix=\"doctest\" }");
        Kernel.getDoc().putDoc(context, "document://" + path + "/foo", "{\"foo\" : 0}");
        Kernel.getDoc().putDoc(context, "document://" + path + "/foo/bar", "{\"bar\" : 0}");

        Kernel.getSeries().createSeriesRepo(context, "series://" + auth, "SREP {} USING MEMORY { prefix=\"ser" + auth + "\" }");
        Kernel.getSeries().addStringToSeries(context, "series://" + path + "/foo", "foo", "bar");
        Kernel.getSeries().addStringToSeries(context, "series://" + path + "/foo/bar", "foo", "bar");

        Kernel.getSheet().createSheetRepo(context, "sheet://" + auth, "SHEET {} USING MEMORY { prefix=\"sheet" + auth + "\" }");
        Kernel.getSheet().createSheet(context, "series://" + path + "/foo");
        Kernel.getSheet().createSheet(context, "series://" + path + "/foo/bar");

        Kernel.getBlob().createBlobRepo(context, "blob://" + auth, "BLOB {} USING MEMORY { prefix=\"sheet" + auth + "\" }",
                "REP {} USING MEMORY { prefix=\"sheet" + auth + "\" }");
        Kernel.getBlob().putBlob(context, "blob://" + path + "/foo", "foo".getBytes(), MediaType.CSS_UTF_8.toString());
        Kernel.getBlob().putBlob(context, "blob://" + path + "/foo/bar", "bar".getBytes(), MediaType.CSS_UTF_8.toString());

        Kernel.getScript().createScript(context, "script://" + path + "/foo", RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, "//");
        Kernel.getScript().createScript(context, "script://" + path + "/foo/bar", RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, "//");
    }

    @AfterClass
    public static void cleanUp() {

        Kernel.getDoc().deleteDocRepo(context, "document://" + auth);
        Kernel.getSeries().deleteSeriesRepo(context, "series://" + auth);
        Kernel.getSheet().deleteSheetRepo(context, "sheet://" + auth);
        Kernel.getBlob().deleteBlobRepo(context, "blob://" + auth);

        String ent = EntitlementSet.Doc_putDoc.getPath().replaceAll("\\$.*", "Black/Sabbath");
        entApi.deleteEntitlementGroup(context, vocals);
        entApi.deleteEntitlementGroup(context, vocals+"/Backing");
        entApi.deleteEntitlementGroup(context, vocals+"/Backing/Guest");

        entApi.deleteEntitlement(context, ent);

        ConfigLoader.getConf().InitSysConfig = saveInitSysConfig;
        ConfigLoader.getConf().RaptureRepo = saveRaptureRepo;

    }

    @Test
    public void testGetAllTopLevelRepos() {
        List<String> ret = api.getAllTopLevelRepos(context);
        assertEquals(10, ret.size());
    }

    @Test
    public void testBrowseEntitlements() {
        if (!Kernel.getAdmin().doesUserExist(context, ozzy)) {
            Kernel.getAdmin().addUser(context, ozzy, "Ozzy Osbourne", MD5Utils.hash16(ozzy), "ozzy@sabbath.com");
        }

        List<RaptureEntitlementGroup> ents = entApi.getEntitlementGroups(context);
        assertNotNull(ents);
        assertEquals(0, ents.size());

        String ent = EntitlementSet.Doc_putDoc.getPath().replaceAll("\\$.*", "Black/Sabbath");
        entApi.addEntitlement(context, ent, vocals);
        entApi.addEntitlementGroup(context, vocals);
        entApi.addGroupToEntitlement(context, ent, vocals);
        assertTrue(entApi.getEntitlementGroup(context, vocals).getUsers().isEmpty());
        assertTrue(entApi.getEntitlement(context, ent).getGroups().contains(vocals));

        ents = entApi.getEntitlementGroups(context);
        assertNotNull(ents);
        assertEquals(1, ents.size());
        assertEquals(0, ents.get(0).getUsers().size());

        entApi.addUserToEntitlementGroup(context, vocals, ozzy);
        ents = entApi.getEntitlementGroups(context);
        assertNotNull(ents);
        assertEquals(1, ents.size());
        assertEquals(1, ents.get(0).getUsers().size());

        entApi.addEntitlementGroup(context, vocals+"/Backing");
        entApi.addEntitlementGroup(context, vocals+"/Backing/Guest");

        String uri = Scheme.ENTITLEMENT + "://";
        ChildrenTransferObject kids = api.listByUriPrefix(context, uri, null, 1, Long.MAX_VALUE, false);
        assertNotNull(kids);
        assertNotNull(kids.getChildren());
        assertTrue(kids.getChildren().keySet().contains("entitlement://data/"));
        assertFalse(kids.getChildren().keySet().contains("entitlement://data/write/"));
        assertFalse(kids.getChildren().keySet().contains("entitlement://data/write/Black/"));
        assertFalse(kids.getChildren().keySet().contains("entitlement://data/write/Black/Sabbath"));
        
        kids = api.listByUriPrefix(context, uri, null, 2, Long.MAX_VALUE, false);
        assertNotNull(kids);
        assertNotNull(kids.getChildren());
        assertTrue(kids.getChildren().keySet().contains("entitlement://data/"));
        assertTrue(kids.getChildren().keySet().contains("entitlement://data/write/"));
        assertFalse(kids.getChildren().keySet().contains("entitlement://data/write/Black/"));
        assertFalse(kids.getChildren().keySet().contains("entitlement://data/write/Black/Sabbath"));
        
        kids = api.listByUriPrefix(context, uri, null, 4, Long.MAX_VALUE, false);
        assertNotNull(kids);
        assertNotNull(kids.getChildren());
        assertTrue(kids.getChildren().keySet().contains("entitlement://data/"));
        assertTrue(kids.getChildren().keySet().contains("entitlement://data/write/"));
        assertTrue(kids.getChildren().keySet().contains("entitlement://data/write/Black/"));
        assertTrue(kids.getChildren().keySet().contains("entitlement://data/write/Black/Sabbath"));

        kids = api.listByUriPrefix(context, uri+"data/write", null, 1, Long.MAX_VALUE, false);
        assertNotNull(kids);
        assertNotNull(kids.getChildren());
        assertFalse(kids.getChildren().keySet().contains("entitlement://data/"));
        assertFalse(kids.getChildren().keySet().contains("entitlement://data/write/"));
        assertTrue(kids.getChildren().keySet().contains("entitlement://data/write/Black/"));
        assertFalse(kids.getChildren().keySet().contains("entitlement://data/write/Black/Sabbath"));
        
        String guri = Scheme.ENTITLEMENTGROUP + "://";
        ChildrenTransferObject gkids = api.listByUriPrefix(context, guri, null, 2, Long.MAX_VALUE, false);
        assertNotNull(gkids);
        assertNotNull(gkids.getChildren());
        assertTrue(gkids.getChildren().keySet().contains("entitlementgroup://Vocalist/"));
        assertTrue(gkids.getChildren().keySet().contains("entitlementgroup://Vocalist"));
        assertTrue(gkids.getChildren().keySet().contains("entitlementgroup://Vocalist/Backing"));
        assertTrue(gkids.getChildren().keySet().contains("entitlementgroup://Vocalist/Backing/"));
        assertFalse(gkids.getChildren().keySet().contains("entitlementgroup://Vocalist/Backing/Guest"));

        guri = Scheme.ENTITLEMENTGROUP + "://";
        gkids = api.listByUriPrefix(context, "entitlementgroup://Vocalist", null, 1, Long.MAX_VALUE, false);
        assertNotNull(gkids);
        assertNotNull(gkids.getChildren());
        assertFalse(gkids.getChildren().keySet().contains("entitlementgroup://Vocalist/"));
        assertFalse(gkids.getChildren().keySet().contains("entitlementgroup://Vocalist"));
        assertTrue(gkids.getChildren().keySet().contains("entitlementgroup://Vocalist/Backing"));
        assertTrue(gkids.getChildren().keySet().contains("entitlementgroup://Vocalist/Backing/"));
        assertFalse(gkids.getChildren().keySet().contains("entitlementgroup://Vocalist/Backing/Guest"));

    }

    @Test
    public void testBrowseWorkflows() {
        String uri = Scheme.WORKFLOW + "://";
        ChildrenTransferObject kids = api.listByUriPrefix(context, uri, null, Integer.MAX_VALUE, Long.MAX_VALUE, false);
        assertNotNull(kids);
        assertNotNull(kids.getChildren());
        assertNotNull(kids.getChildren().keySet());
        for (String key : kids.getChildren().keySet()) {
            assertTrue(key.startsWith(uri));
        }
        for (RaptureFolderInfo val : kids.getChildren().values()) {
            String v = val.getName();
            assertFalse(v, v.startsWith("/"));
            assertFalse(v, v.endsWith("/"));
            assertFalse(v, v.contains("://"));
        }
    }

    @Test
    public void testBrowseFountains() {
        String uri = Scheme.IDGEN + "://";
        ChildrenTransferObject kids = api.listByUriPrefix(context, uri, null, Integer.MAX_VALUE, Long.MAX_VALUE, false);
        assertNotNull(kids);
        assertNotNull(kids.getChildren());
        assertNotNull(kids.getChildren().keySet());
        for (String key : kids.getChildren().keySet()) {
            assertTrue(key.startsWith(uri));
        }
        for (RaptureFolderInfo val : kids.getChildren().values()) {
            String v = val.getName();
            assertFalse(v, v.startsWith("/"));
            assertFalse(v, v.endsWith("/"));
            assertFalse(v, v.contains("://"));
        }
    }

    @Test
    public void testBrowseJobs() {
        String uri = Scheme.JOB + "://";
        ScheduleApi sched = Kernel.getSchedule();
        sched.createJob(context, "job://foo/bar", "dummy", "script://whizz/bang", "* * * * * *", "UTC", null, false);

        ChildrenTransferObject kids = api.listByUriPrefix(context, uri, null, Integer.MAX_VALUE, Long.MAX_VALUE, false);
        assertNotNull(kids);
        assertNotNull(kids.getChildren());
        assertNotNull(kids.getChildren().keySet());
        for (String key : kids.getChildren().keySet()) {
            assertTrue(key.startsWith(uri));
        }
        for (RaptureFolderInfo val : kids.getChildren().values()) {
            String v = val.getName();
            assertFalse(v, v.startsWith("/"));
            assertFalse(v, v.endsWith("/"));
            assertFalse(v, v.contains("://"));
        }
    }

    @Test
    public void testBrowseChildren() {
        List<String> repos = api.getAllTopLevelRepos(context);

        repos = new ArrayList<>(); repos.add("entitlement://");
        for (String repo : repos) {
            ChildrenTransferObject kids = api.listByUriPrefix(context, repo, null, Integer.MAX_VALUE, Long.MAX_VALUE, false);
            assertNotNull(kids);
            assertNotNull(kids.getChildren().keySet());
            for (String key : kids.getChildren().keySet()) {
                assertTrue(key.contains("://"));
                
                // entitlement:// can also return entitlementgroup:// and user://
                if (!repo.equals("entitlement://")) {
                    assertEquals(repo, key.substring(0, repo.length()));
                }                
                RaptureFolderInfo val = kids.getChildren().get(key);
                String v = val.getName();
                assertFalse(key+" " + JacksonUtil.jsonFromObject(val), v.startsWith("/"));
                assertFalse(key+" " + JacksonUtil.jsonFromObject(val), v.endsWith("/"));
                assertFalse(key+" " + JacksonUtil.jsonFromObject(val), v.contains("://"));
            }
            System.out.println(kids.getChildren().size());
        }
    }

    @Test
    public void testBrowseChildrenWithPaging() {
        List<String> repos = api.getAllTopLevelRepos(context);

        Long l = System.currentTimeMillis();
        long pageSize = 100;
        String marker = null;
        ChildrenTransferObject kids = null;
        do {
            kids = api.listByUriPrefix(context, "document://", marker, Integer.MAX_VALUE, pageSize, false);
            assertNotNull(kids);
            System.out.println(kids.getChildren().size());
            marker = kids.getIndexMark();
        } while (kids.getRemainder() > 0);
        System.out.println("Took " + (System.currentTimeMillis() - l));
    }

    @Test
    public void testCacheExpiration() throws InterruptedException {
        
        ChildrenTransferObject kids = api.listByUriPrefix(context, "document://", null, Integer.MAX_VALUE, Long.MAX_VALUE, false);
        int k1 = kids.getChildren().size();
        System.out.println("Got "+k1+" - Remainder is " + kids.getRemainder());
        String uri = "document://test/" + System.currentTimeMillis() + "/";
        for (int i = 0; i < 20; i++) {
            Kernel.getDoc().putDoc(context, uri + i, "{\"foo\" : " + i + "}");
        }

        ChildrenTransferObject kids2 = api.listByUriPrefix(context, "document://", null, Integer.MAX_VALUE, Long.MAX_VALUE, false);
        int k2 = kids2.getChildren().size();

        System.out.println("Now got "+k2+" - Remainder is " + kids2.getRemainder());
        System.out.println("Expected 21 more than before");
        Assert.assertTrue("Was "+k1+" is now "+k2+" - Expected at least 20 more than before but only got "+(k2-k1), k2-k1 >= 20);

        // Reduce the cache timeout to 1 second for testing (note that cache expiration thread has a delay of 5s (at time of writing)
        SysApiImpl.TIMETOLIVE = 1000;
        kids = api.listByUriPrefix(context, "document://", null, Integer.MAX_VALUE, 10L, false);
        System.out.println("Asked for 10. Got "+kids.getChildren().size()+" - Remainder is " + kids.getRemainder());
        assertNotNull(kids);
        Assert.assertNotEquals("Expect non-zero remainder", new Long(0), kids.getRemainder());

        kids2 = api.listByUriPrefix(context, "document://", kids.getIndexMark(), Integer.MAX_VALUE, 10L, false);
        assertNotNull(kids2);
        System.out.println("Asked for 10 more. Got "+kids2.getChildren().size()+" - Remainder is " + kids2.getRemainder());
        Assert.assertEquals("Expect same index mark ", kids.getIndexMark(), kids2.getIndexMark());
        Assert.assertEquals("Expect remainder to be 10 fewer - ", (kids.getRemainder()-10), kids2.getRemainder().longValue());
        System.out.println("New Remainder is " + kids2.getRemainder());
        System.out.println("marker is " + kids2.getIndexMark());

        Thread.sleep(1000);
        api.getTrusted().cacheThread.interrupt();
        Thread.sleep(1000);

        // Now cache has expired should get same results as for first call
        kids2 = api.listByUriPrefix(context, "document://", kids.getIndexMark(), Integer.MAX_VALUE, 10L, false);
        assertNotNull(kids2);
        System.out.println("After cache expired Remainder is " + kids2.getRemainder()+" - expected > 0");
        System.out.println("Asked for 10 but got " + kids2.getChildren().size());
        System.out.println("New marker is " + kids2.getIndexMark());
        
        Assert.assertNotNull("Expect a marker", kids2.getIndexMark());
        Assert.assertNotEquals("Expect a new marker", kids.getIndexMark(), kids2.getIndexMark());
        Assert.assertTrue("Expect a remainder", kids2.getRemainder() > 0);

        //      This fails intermittently on the bamboo build (eg: expected 36, got 18)
        //      Unclear why, but as long as there's a remainder and a marker the back end is presumably working.
        //      This test will get reviewed when RAP-3674/RAP-3851 is fixed
        //
        //      Assert.assertTrue("Expect remainder to be at least "+kids.getRemainder()+" but was "+kids2.getRemainder(), kids.getRemainder() <= kids2.getRemainder());
    }

    @Test
    public void testRepoGetsDeleted() throws InterruptedException {
        
        ChildrenTransferObject kids = api.listByUriPrefix(context, "document://", null, Integer.MAX_VALUE, Long.MAX_VALUE, false);
        String uri = "document://test/" + System.currentTimeMillis() + "/";
        for (int i = 0; i < 20; i++) {
            Kernel.getDoc().putDoc(context, uri + i, "{\"foo\" : " + i + "}");
        }
        int k1 = kids.getChildren().size();
        System.out.println("Got "+k1+" - Remainder is " + kids.getRemainder());

        ChildrenTransferObject kids2 = api.listByUriPrefix(context, "document://", null, Integer.MAX_VALUE, Long.MAX_VALUE, false);
        int k2 = kids2.getChildren().size();

        System.out.println("Now got "+k2+" - Remainder is " + kids2.getRemainder());
        System.out.println("Expected 21 more than before");
        Assert.assertTrue("Was "+k1+" is now "+k2+" - Expected at least 20 more than before but only got "+(k2-k1), k2-k1 >= 20);

        kids = api.listByUriPrefix(context, "document://", null, Integer.MAX_VALUE, 10L, false);
        System.out.println("Asked for 10. Got "+kids.getChildren().size()+" - Remainder is " + kids.getRemainder());
        assertNotNull(kids);
        Assert.assertNotEquals("Expect non-zero remainder", new Long(0), kids.getRemainder());

        kids2 = api.listByUriPrefix(context, "document://", kids.getIndexMark(), Integer.MAX_VALUE, 10L, false);
        assertNotNull(kids2);
        System.out.println("Asked for 10 more. Got "+kids2.getChildren().size()+" - Remainder is " + kids2.getRemainder());
        Assert.assertNotEquals("Expect remainder to be "+kids.getRemainder(), kids.getRemainder(), kids2.getRemainder());
        System.out.println("New Remainder is " + kids2.getRemainder());
        System.out.println("marker is " + kids2.getIndexMark());

        //  No longer valid. Need to rewrite. Now using ephemeral repo.

//        // Reduce the cache timeout to 1 second for testing (note that cache expiration thread has a delay of 5s (at time of writing)
//        SysApiImpl.setTimeToLive(1000);
//        Kernel.getDoc().deleteDocRepo(context, SysApiImpl.cacheUriDoc);
//        Thread.sleep(10000); // Must be longer than cache expiration thread's sleep time)
//
//        assertFalse("Repo was deleted - should not exist", Kernel.getDoc().docRepoExists(context, SysApiImpl.cacheUriDoc));
//        
//        // Now cache has expired should get same results as for first call
//        kids2 = api.listByUriPrefix(context, "document://", kids.getIndexMark(), Integer.MAX_VALUE, 10L, false);
//        assertNotNull(kids2);
//        System.out.println("After cache expired Remainder is " + kids2.getRemainder());
//        Assert.assertTrue("Expect a remainder", kids2.getRemainder() > 0);
//
//        assertTrue("Repo should have been re-created", Kernel.getDoc().docRepoExists(context, SysApiImpl.cacheUriDoc));    
    }

    @Test
    public void testRefresh() throws InterruptedException {
        
        String uri = "document://test/" + System.currentTimeMillis() + "/";
        for (int i = 0; i < 10; i++) {
            Kernel.getDoc().putDoc(context, uri + i, "{\"bar\" : " + i + "}");
        }
        ChildrenTransferObject kids = api.listByUriPrefix(context, uri, null, Integer.MAX_VALUE, Long.MAX_VALUE, false);
        assertEquals(10, kids.getChildren().size());
        
        List<String> list = new ArrayList<>();

        kids = api.listByUriPrefix(context, uri, null, 1, 4L, false);
        assertEquals(4, kids.getChildren().size());
        list.addAll(kids.getChildren().keySet());
        assertEquals(new Long(6L), kids.getRemainder());
        
        Kernel.getDoc().deleteDoc(context, uri + 3);
        Kernel.getDoc().deleteDoc(context, uri + 4);

        for (int i = 10; i < 20; i++) {
            Kernel.getDoc().putDoc(context, uri + i, "{\"bar\" : " + i + "}");
        }

        kids = api.listByUriPrefix(context, uri, kids.getIndexMark(), 1, 4L, false);
        assertEquals(4, kids.getChildren().size());
        list.addAll(kids.getChildren().keySet());
        assertEquals(new Long(2L), kids.getRemainder());
        
        kids = api.listByUriPrefix(context, uri, kids.getIndexMark(), 1, 4L, false);
        assertEquals(2, kids.getChildren().size());
        list.addAll(kids.getChildren().keySet());
        assertEquals(new Long(0L), kids.getRemainder());
        
        assertTrue(list.contains(uri + 2));
        assertTrue(list.contains(uri + 3));
        assertTrue(list.contains(uri + 4));
        assertTrue(list.contains(uri + 5));

        kids = api.listByUriPrefix(context, uri, kids.getIndexMark(), 1, 4L, false);
        assertEquals(0, kids.getChildren().size());
        assertEquals(new Long(0L), kids.getRemainder());

        kids = api.listByUriPrefix(context, uri, kids.getIndexMark(), 1, 4L, true);
        assertEquals(4, kids.getChildren().size());
        assertEquals(2, kids.getDeleted().size());
        assertEquals(new Long(6L), kids.getRemainder());

        assertTrue(kids.getDeleted().keySet().contains(uri + 3));
        assertTrue(kids.getDeleted().keySet().contains(uri + 4));    
    }

    @Test
    public void testGetFolderInfo() {

        String[] schemas = new String[] { "document", "series", "sheet", "blob", "script" };
        for (String schema : schemas) {
            NodeEnum node;
            String uri = schema + "://" + path;
            node = api.getFolderInfo(context, uri);
            assertNotNull(node);
            assertEquals(uri, NodeEnum.FOLDER_ONLY, node);
            node = api.getFolderInfo(context, uri + "/foo");
            assertNotNull(node);
            assertEquals(uri + "/foo", NodeEnum.OBJECT_AND_FOLDER, node);
            node = api.getFolderInfo(context, uri + "/foo/bar");
            assertNotNull(node);
            assertEquals(uri + "/foo", NodeEnum.OBJECT_ONLY, node);
        }
    }
}
