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
package rapture.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import rapture.common.CallingContext;
import rapture.common.EntitlementSet;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.DocApi;
import rapture.common.api.EntitlementApi;
import rapture.common.api.UserApi;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.MD5Utils;
import rapture.common.model.RaptureEntitlement;
import rapture.common.model.RaptureEntitlementGroup;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;

public class EntitlementApiImplTest {

    CallingContext rootContext;
    CallingContext ozzyContext;
    CallingContext tonyContext;
    CallingContext billContext;
    CallingContext geezerContext;
    String ozzy = "ozzy";
    String tony = "tony";
    String bill = "bill";
    String geezer = "geezer";
    UserApi api = null;
    EntitlementApi entApi = null;
    DocApi docApi = null;

    private String auth;
    private String REPO_USING_FILE;

    String saveRaptureRepo;
    String saveInitSysConfig;

    @Before
    public void setUp() {
        auth = "test" + System.nanoTime();
        REPO_USING_FILE = "REP {} USING FILE {prefix=\"/tmp/" + auth + "\"}";
        new File("/tmp/" + auth).mkdir();
        new File("/tmp/" + auth + "_meta").mkdir();
        RaptureConfig.setLoadYaml(false);
        RaptureConfig config = ConfigLoader.getConf();

        saveRaptureRepo = config.RaptureRepo;
        saveInitSysConfig = config.InitSysConfig;

        config.RaptureRepo = REPO_USING_FILE;
        config.InitSysConfig = "NREP {} USING FILE { prefix=\"/tmp/" + auth + "/sys.config\"}";
        // config.DefaultPipelineTaskStatus = "TABLE {} USING FILE {prefix=\"/tmp/" + auth + "\"}";

        System.setProperty("LOGSTASH-ISENABLED", "false");

        Kernel.INSTANCE.restart();
        Kernel.initBootstrap();
        rootContext = ContextFactory.getKernelUser();

        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");

        api = Kernel.getUser();
        if (!Kernel.getAdmin().doesUserExist(rootContext, ozzy)) {
            Kernel.getAdmin().addUser(rootContext, ozzy, "Ozzy Osbourne", MD5Utils.hash16(ozzy), "ozzy@sabbath.com");
        }
        if (!Kernel.getAdmin().doesUserExist(rootContext, geezer)) {
            Kernel.getAdmin().addUser(rootContext, geezer, "Geezer Butler", MD5Utils.hash16(geezer), "geezer@sabbath.com");
        }
        if (!Kernel.getAdmin().doesUserExist(rootContext, bill)) {
            Kernel.getAdmin().addUser(rootContext, bill, "Bill Ward", MD5Utils.hash16(bill), "bill@sabbath.com");
        }
        if (!Kernel.getAdmin().doesUserExist(rootContext, tony)) {
            Kernel.getAdmin().addUser(rootContext, tony, "Tony Iommi", MD5Utils.hash16(tony), "tony@sabbath.com");
        }

        if (!Kernel.getDoc().docRepoExists(rootContext, "//Black")) {
            Kernel.getDoc().createDocRepo(rootContext, "//Black", "NREP {} USING MEMORY {}");
        }

        ozzyContext = Kernel.getLogin().login(ozzy, ozzy, null);
        tonyContext = Kernel.getLogin().login(tony, tony, null);
        billContext = Kernel.getLogin().login(bill, bill, null);
        geezerContext = Kernel.getLogin().login(geezer, geezer, null);
        entApi = Kernel.getEntitlement();

    }

    @After
    public void after() throws IOException {
        FileUtils.deleteDirectory(new File("/tmp/" + auth));
        FileUtils.deleteDirectory(new File("/tmp/" + auth + "_meta"));
        FileUtils.deleteDirectory(new File("/tmp/" + auth + "_sheet"));

        ConfigLoader.getConf().InitSysConfig = saveInitSysConfig;
        ConfigLoader.getConf().RaptureRepo = saveRaptureRepo;
    }

    @Test
    public void testGetAllEntitlements() {
        List<RaptureEntitlement> rootEnts = entApi.getEntitlements(rootContext);
        assertFalse(rootEnts.isEmpty());
    }

    @Test
    public void testGetAllEntitlementGroups() {
        List<RaptureEntitlementGroup> ozzEnts = entApi.getEntitlementGroups(ozzyContext);
        assertNotNull(ozzEnts);
        assertEquals(0, ozzEnts.size());

        String vocals = "Vocalist";
        String ent = EntitlementSet.Doc_putDoc.getPath().replaceAll("\\$.*", "Black/Sabbath");
        entApi.addEntitlement(rootContext, ent, vocals);
        entApi.addEntitlementGroup(rootContext, vocals);
        entApi.addGroupToEntitlement(rootContext, ent, vocals);
        assertTrue(entApi.getEntitlementGroup(rootContext, vocals).getUsers().isEmpty());
        assertTrue(entApi.getEntitlement(rootContext, ent).getGroups().contains(vocals));

        ozzEnts = entApi.getEntitlementGroups(ozzyContext);
        assertNotNull(ozzEnts);
        assertEquals(1, ozzEnts.size());
        assertEquals(0, ozzEnts.get(0).getUsers().size());

        entApi.addUserToEntitlementGroup(rootContext, vocals, ozzy);
        ozzEnts = entApi.getEntitlementGroups(ozzyContext);
        assertNotNull(ozzEnts);
        assertEquals(1, ozzEnts.size());
        assertEquals(1, ozzEnts.get(0).getUsers().size());
        RaptureEntitlementGroup reg = ozzEnts.get(0);
        String uri = reg.getAddressURI().toString();
        assertEquals("entitlementgroup://Vocalist/", uri);
    }

    @Test
    public void checkBulkLoophole() {
        // There was a security hole that if you used the 'batch' doc api methods then you could bypass entitlement checks.

        // Start by creating two docs owned by different users.

        String vocals = "Vocalist";
        String heavenAndHell = "document://Black/Sabbath/Dio/HeavenAndHell";
        String neverSayDie = "document://Black/Sabbath/Ozzy/NeverSayDie";
        String ronnie = EntitlementSet.Doc_putDoc.getPath().replaceAll("\\$.*", "Black/Sabbath/Dio");
        String john = EntitlementSet.Doc_putDoc.getPath().replaceAll("\\$.*", "Black/Sabbath/Ozzy");

        entApi.addEntitlement(rootContext, john, vocals);
        entApi.addEntitlementGroup(rootContext, vocals);
        entApi.addGroupToEntitlement(rootContext, john, vocals);
        assertTrue(entApi.getEntitlementGroup(rootContext, vocals).getUsers().isEmpty());
        assertTrue(entApi.getEntitlement(rootContext, john).getGroups().contains(vocals));
        entApi.addUserToEntitlementGroup(rootContext, vocals, ozzy);

        String guitars = "Guitars";
        entApi.addEntitlement(rootContext, ronnie, guitars);
        entApi.addEntitlementGroup(rootContext, guitars);
        entApi.addGroupToEntitlement(rootContext, ronnie, guitars);
        entApi.addUserToEntitlementGroup(rootContext, guitars, tony);

        // Okay so at this point Ozzy should have permission to access Never Say Die but not Heaven And Hell

        assertTrue("At this point Ozzy should have access", api.isPermitted(ozzyContext, "doc.putDoc", neverSayDie));
        assertFalse("At this point Ozzy should not have access", api.isPermitted(ozzyContext, "doc.putDoc", heavenAndHell));

        docApi = Kernel.getDoc();

        docApi.putDoc(tonyContext, heavenAndHell, "{ \"singer\" : \"Sing me a song\" , \"bringer_of_evil\" : \"Do me a wrong\" }");
        docApi.putDoc(ozzyContext, neverSayDie, "{ \"worry\" : false, \"wonder_why\" : false, \"say_die\" : false };");
        assertTrue("At this point Ozzy should have access", api.isPermitted(ozzyContext, "doc.putDoc", neverSayDie));
        assertFalse("At this point Ozzy should not have access", api.isPermitted(ozzyContext, "doc.putDoc", heavenAndHell));

        String blizzard = "document://Blizzard/Of/Ozz";
        List<String> fromDocUris = new ImmutableList.Builder<String>().add(neverSayDie).add(heavenAndHell).build();
        List<String> toDocUris = new ImmutableList.Builder<String>().add(blizzard + "/NeverSayDie").add(blizzard + "/HeavenAndHell").build();

        assertFalse(docApi.docExists(rootContext, blizzard + "/NeverSayDie"));
        assertTrue(docApi.docExists(rootContext, neverSayDie));
        assertFalse(docApi.docExists(rootContext, blizzard + "/HeavenAndHell"));
        assertTrue(docApi.docExists(rootContext, heavenAndHell));

        //
        assertFalse("Tony can't write to the doc", api.isPermitted(tonyContext, "doc.putDoc", neverSayDie));

        docApi.renameDocs(ozzyContext, blizzard, "Going solo", fromDocUris, toDocUris);

        // So that should have successfully moved Never Say Die, but Heaven And Hell should be untouched.

        assertTrue("Never Say Die got moved", docApi.docExists(rootContext, blizzard + "/NeverSayDie"));
        assertFalse("Never Say Die got moved", docApi.docExists(rootContext, neverSayDie));
        assertFalse("Heaven And Hell did not get moved", docApi.docExists(rootContext, blizzard + "/HeavenAndHell"));
        assertTrue("Heaven And Hell did not get moved", docApi.docExists(rootContext, heavenAndHell));

        // Note that in moving the document the entitlements don't follow

        assertTrue("Should Tony have write access to the new doc?", api.isPermitted(tonyContext, "doc.putDoc", blizzard + "/NeverSayDie"));
    }

    @Test
    public void testDeleteDocsByUriPrefix() {
        // There was a security hole that if you used the 'batch' doc api methods then you could bypass entitlement checks.

        // Start by creating two docs owned by different users.

        String vocals = "Vocalist";
        String heavenAndHell = "document://Black/Sabbath/Dio/HeavenAndHell";
        String neverSayDie = "document://Black/Sabbath/Ozzy/NeverSayDie";
        String ronnie = EntitlementSet.Doc_putDoc.getPath().replaceAll("\\$.*", "Black/Sabbath/Dio");
        String john = EntitlementSet.Doc_putDoc.getPath().replaceAll("\\$.*", "Black/Sabbath/Ozzy");

        entApi.addEntitlement(rootContext, john, vocals);
        entApi.addEntitlementGroup(rootContext, vocals);
        entApi.addGroupToEntitlement(rootContext, john, vocals);
        assertTrue(entApi.getEntitlementGroup(rootContext, vocals).getUsers().isEmpty());
        assertTrue(entApi.getEntitlement(rootContext, john).getGroups().contains(vocals));
        entApi.addUserToEntitlementGroup(rootContext, vocals, ozzy);

        String guitars = "Guitars";
        entApi.addEntitlement(rootContext, ronnie, guitars);
        entApi.addEntitlementGroup(rootContext, guitars);
        entApi.addGroupToEntitlement(rootContext, ronnie, guitars);
        entApi.addUserToEntitlementGroup(rootContext, guitars, tony);

        // Okay so at this point Ozzy should have permission to access Never Say Die but not Heaven And Hell

        assertTrue("At this point Ozzy should have access", api.isPermitted(ozzyContext, "doc.putDoc", neverSayDie));
        assertFalse("At this point Ozzy should not have access", api.isPermitted(ozzyContext, "doc.putDoc", heavenAndHell));

        docApi = Kernel.getDoc();

        docApi.putDoc(tonyContext, heavenAndHell, "{ \"singer\" : \"Sing me a song\" , \"bringer_of_evil\" : \"Do me a wrong\" }");
        docApi.putDoc(ozzyContext, neverSayDie, "{ \"worry\" : false, \"wonder_why\" : false, \"say_die\" : false };");
        assertTrue("At this point Ozzy should have access", api.isPermitted(ozzyContext, "doc.putDoc", neverSayDie));
        assertFalse("At this point Ozzy should not have access", api.isPermitted(ozzyContext, "doc.putDoc", heavenAndHell));

        List<String> deleted = docApi.deleteDocsByUriPrefix(ozzyContext, "document://Black");
        assertEquals("Deleted one document", deleted.size(), 1);

        deleted = docApi.deleteDocsByUriPrefix(ozzyContext, "document://Black");
        assertEquals("Should be zero because we just deleted it", deleted.size(), 0);
    }

    @Test
    public void testMultipleEntitlementGroups() {
        String vocals = "Vocalist";
        String ent = EntitlementSet.Doc_putDoc.getPath().replaceAll("\\$.*", "Black/Sabbath");

        entApi.addEntitlement(rootContext, ent, vocals);
        entApi.addEntitlementGroup(rootContext, vocals);
        entApi.addGroupToEntitlement(rootContext, ent, vocals);
        assertTrue(entApi.getEntitlementGroup(rootContext, vocals).getUsers().isEmpty());
        assertTrue(entApi.getEntitlement(rootContext, ent).getGroups().contains(vocals));

        assertFalse("At this point nobody should have access", api.isPermitted(ozzyContext, "doc.putDoc", "document://Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(ozzyContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(ozzyContext, "/data/write/$f(docURI)", "document://Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(ozzyContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(ozzyContext, "/data/write/Black/Sabbath/Paranoid", null));

        assertFalse("At this point nobody should have access", api.isPermitted(tonyContext, "doc.putDoc", "document://Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(tonyContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(tonyContext, "/data/write/$f(docURI)", "document://Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(tonyContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(tonyContext, "/data/write/Black/Sabbath/Paranoid", null));

        assertFalse("At this point nobody should have access", api.isPermitted(billContext, "doc.putDoc", "document://Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(billContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(billContext, "/data/write/$f(docURI)", "document://Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(billContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(billContext, "/data/write/Black/Sabbath/Paranoid", null));

        assertFalse("At this point nobody should have access", api.isPermitted(geezerContext, "doc.putDoc", "document://Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(geezerContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(geezerContext, "/data/write/$f(docURI)", "document://Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(geezerContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point nobody should have access", api.isPermitted(geezerContext, "/data/write/Black/Sabbath/Paranoid", null));

        entApi.addUserToEntitlementGroup(rootContext, vocals, ozzy);
        assertTrue("At this point Ozzy should have access", api.isPermitted(ozzyContext, "doc.putDoc", "document://Black/Sabbath/Paranoid"));
        assertTrue("At this point Ozzy should have access", api.isPermitted(ozzyContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertTrue("At this point Ozzy should have access", api.isPermitted(ozzyContext, "/data/write/$f(docURI)", "document://Black/Sabbath/Paranoid"));
        assertTrue("At this point Ozzy should have access", api.isPermitted(ozzyContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertTrue("At this point Ozzy should have access", api.isPermitted(ozzyContext, "/data/write/Black/Sabbath/Paranoid", null));

        assertFalse("At this point only Ozzy should have access", api.isPermitted(tonyContext, "doc.putDoc", "document://Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(tonyContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(tonyContext, "/data/write/$f(docURI)", "document://Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(tonyContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(tonyContext, "/data/write/Black/Sabbath/Paranoid", null));

        assertFalse("At this point only Ozzy should have access", api.isPermitted(billContext, "doc.putDoc", "document://Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(billContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(billContext, "/data/write/$f(docURI)", "document://Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(billContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(billContext, "/data/write/Black/Sabbath/Paranoid", null));

        assertFalse("At this point only Ozzy should have access", api.isPermitted(geezerContext, "doc.putDoc", "document://Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(geezerContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(geezerContext, "/data/write/$f(docURI)", "document://Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(geezerContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertFalse("At this point only Ozzy should have access", api.isPermitted(geezerContext, "/data/write/Black/Sabbath/Paranoid", null));

        assertTrue(new File("/tmp/" + auth + "/sys.config/entitlementgroup/Vocalist.txt").exists());
        assertTrue(new File("/tmp/" + auth + "/sys.config_meta/entitlementgroup/Vocalist-3f1.txt").exists());
        assertTrue(new File("/tmp/" + auth + "/sys.config_meta/entitlementgroup/Vocalist-3f2.txt").exists());
        assertTrue(new File("/tmp/" + auth + "/sys.config_meta/entitlementgroup/Vocalist-3flatest.txt").exists());
        assertFalse(new File("/tmp/" + auth + "/sys.config_meta/entitlementgroup/Vocalist-3f3.txt").exists());

        String guitars = "Guitars";
        entApi.addEntitlement(rootContext, ent, guitars);
        assertTrue("The entitlement groups set should still contain the group " + vocals, entApi.getEntitlement(rootContext, ent).getGroups().contains(vocals));
        entApi.addEntitlementGroup(rootContext, guitars);
        entApi.addGroupToEntitlement(rootContext, ent, guitars);
        assertTrue(entApi.getEntitlementGroup(rootContext, guitars).getUsers().isEmpty());
        assertTrue("The entitlement groups set should contain the group " + guitars, entApi.getEntitlement(rootContext, ent).getGroups().contains(guitars));
        assertTrue("The entitlement groups set should still contain the group " + vocals, entApi.getEntitlement(rootContext, ent).getGroups().contains(vocals));

        entApi.addUserToEntitlementGroup(rootContext, guitars, geezer);
        entApi.addUserToEntitlementGroup(rootContext, guitars, tony);

        assertTrue("At this point Ozzy should still have access", api.isPermitted(ozzyContext, "doc.putDoc", "document://Black/Sabbath/Paranoid"));
        assertTrue("At this point Ozzy should still have access", api.isPermitted(ozzyContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertTrue("At this point Ozzy should still have access", api.isPermitted(ozzyContext, "/data/write/$f(docURI)", "document://Black/Sabbath/Paranoid"));
        assertTrue("At this point Ozzy should still have access", api.isPermitted(ozzyContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertTrue("At this point Ozzy should still have access", api.isPermitted(ozzyContext, "/data/write/Black/Sabbath/Paranoid", null));

        assertTrue("At this point Tony should have access", api.isPermitted(tonyContext, "doc.putDoc", "document://Black/Sabbath/Paranoid"));
        assertTrue("At this point Tony should have access", api.isPermitted(tonyContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertTrue("At this point Tony should have access", api.isPermitted(tonyContext, "/data/write/$f(docURI)", "document://Black/Sabbath/Paranoid"));
        assertTrue("At this point Tony should have access", api.isPermitted(tonyContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertTrue("At this point Tony should have access", api.isPermitted(tonyContext, "/data/write/Black/Sabbath/Paranoid", null));

        assertFalse("Bill should still not have access", api.isPermitted(billContext, "doc.putDoc", "document://Black/Sabbath/Paranoid"));
        assertFalse("Bill should still not have access", api.isPermitted(billContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertFalse("Bill should still not have access", api.isPermitted(billContext, "/data/write/$f(docURI)", "document://Black/Sabbath/Paranoid"));
        assertFalse("Bill should still not have access", api.isPermitted(billContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertFalse("Bill should still not have access", api.isPermitted(billContext, "/data/write/Black/Sabbath/Paranoid", null));

        assertTrue("At this point Geezer should have access", api.isPermitted(geezerContext, "doc.putDoc", "document://Black/Sabbath/Paranoid"));
        assertTrue("At this point Geezer should have access", api.isPermitted(geezerContext, "doc.putDoc", "//Black/Sabbath/Paranoid"));
        assertTrue("At this point Geezer should have access", api.isPermitted(geezerContext, "/data/write/$f(docURI)", "document://Black/Sabbath/Paranoid"));
        assertTrue("At this point Geezer should have access", api.isPermitted(geezerContext, "/data/write/$f(docURI)", "//Black/Sabbath/Paranoid"));
        assertTrue("At this point Geezer should have access", api.isPermitted(geezerContext, "/data/write/Black/Sabbath/Paranoid", null));

        assertTrue(new File("/tmp/" + auth + "/sys.config/entitlementgroup/Guitars.txt").exists());
        assertTrue(new File("/tmp/" + auth + "/sys.config_version/entitlementgroup/Guitars-3f1.txt").exists());
        assertTrue(new File("/tmp/" + auth + "/sys.config_version/entitlementgroup/Guitars-3f2.txt").exists());
        assertTrue(new File("/tmp/" + auth + "/sys.config_version/entitlementgroup/Guitars-3f3.txt").exists());
        assertFalse(new File("/tmp/" + auth + "/sys.config_version/entitlementgroup/Guitars-3f4.txt").exists());
    }

    // This is another case that's more of a doc API test than an entitlement API test
    @Test
    public void checkDocRename() {
        // There was a security hole that if you used the 'batch' doc api methods then you could bypass entitlement checks.

        // Start by creating two docs owned by different users.

        String vocals = "Vocalist";
        String band = "document://Black/Sabbath";
        String firstLineup = "document://Black/Sabbath/Ozzy";
        String secondLineup = "document://Black/Sabbath/Dio";

        entApi.addEntitlement(rootContext, "/data/write/Black/Sabbath/Ozzy", vocals);
        entApi.addEntitlementGroup(rootContext, vocals);
        entApi.addGroupToEntitlement(rootContext, "/data/write/Black/Sabbath/Ozzy", vocals);
        assertTrue(entApi.getEntitlementGroup(rootContext, vocals).getUsers().isEmpty());
        assertTrue(entApi.getEntitlement(rootContext, "/data/write/Black/Sabbath/Ozzy").getGroups().contains(vocals));
        entApi.addUserToEntitlementGroup(rootContext, vocals, ozzy);

        String guitars = "Guitars";
        entApi.addEntitlement(rootContext, "/data/write/Black/Sabbath", guitars);
        entApi.addEntitlementGroup(rootContext, guitars);
        entApi.addGroupToEntitlement(rootContext, "/data/write/Black/Sabbath", guitars);
        entApi.addUserToEntitlementGroup(rootContext, guitars, tony);

        // Okay so at this point Ozzy should have permission to write to Black/Sabbath/Ozzy but not Black/Sabbath/Dio

        assertTrue("At this point Ozzy should have access", api.isPermitted(ozzyContext, "doc.putDoc", "document://Black/Sabbath/Ozzy"));
        assertTrue("At this point Ozzy should have access", api.isPermitted(ozzyContext, "doc.putDoc", "document://Black/Sabbath/Ozzy/Paranoid"));

        String neverSayDie = firstLineup + "/NeverSayDie";
        String sabotage = firstLineup + "/Sabotage";
        String mobRules = secondLineup + "/MobRules";

        docApi = Kernel.getDoc();
        docApi.putDoc(ozzyContext, neverSayDie, "{ \"worry\" : false, \"wonder_why\" : false, \"say_die\" : false };");
        docApi.renameDoc(ozzyContext, neverSayDie, sabotage);
        // that should work

        // this should fail
        docApi.putDoc(ozzyContext, neverSayDie, "{ \"worry\" : false, \"wonder_why\" : false, \"say_die\" : false };");
        try {
            docApi.renameDoc(ozzyContext, neverSayDie, mobRules);
            fail("You don't have permission (Die young!)");
        } catch (Exception e) {
            // expected
        }

        docApi.putDoc(tonyContext, mobRules, "{ \"listen_to_fools\" : \"The mob rules\" }");
        docApi.deleteDoc(ozzyContext, sabotage);
        try {
            docApi.renameDoc(ozzyContext, mobRules, sabotage);
            fail("You don't have permission (Die young!)");
        } catch (Exception e) {
            // expected
        }

        // If we do this then Ozzy no longer has read access
        entApi.addEntitlement(rootContext, "/data/read/Black/Sabbath", guitars);

        docApi.putDoc(ozzyContext, neverSayDie, "{ \"worry\" : false, \"wonder_why\" : false, \"say_die\" : false };");
        docApi.deleteDoc(ozzyContext, sabotage);
        try {
            // So although this worked earlier it shouldn't work now
            docApi.renameDoc(ozzyContext, neverSayDie, sabotage);
            fail("You don't have permission (Die young!)");
        } catch (Exception e) {
            // expected
        }

    }

    @Test
    public void getEntitlementsForGroup() {
        String vocals = "Vocalist";
        String guitars = "Guitar";
        String drums = "Drums";
        String album = "Album";

        entApi.addEntitlementGroup(rootContext, vocals);
        entApi.addEntitlementGroup(rootContext, guitars);
        entApi.addEntitlementGroup(rootContext, drums);

        entApi.addEntitlement(rootContext, "/data/write/BlackSabbath/Paranoid", album);
        entApi.addEntitlement(rootContext, "/data/write/BlackSabbath/Sabotage", album);
        entApi.addEntitlement(rootContext, "/data/write/BlackSabbath/MasterOfReality", album);
        entApi.addEntitlement(rootContext, "/data/write/BlackSabbath/Volume4", album);

        entApi.addGroupToEntitlement(rootContext, "/data/write/BlackSabbath/Paranoid", vocals);
        entApi.addGroupToEntitlement(rootContext, "/data/write/BlackSabbath/Sabotage", vocals);
        entApi.addGroupToEntitlement(rootContext, "/data/write/BlackSabbath/Volume4", vocals);

        entApi.addGroupToEntitlement(rootContext, "/data/write/BlackSabbath/MasterOfReality", drums);

        entApi.addGroupToEntitlement(rootContext, "/data/write/BlackSabbath/Sabotage", guitars);

        entApi.addUserToEntitlementGroup(rootContext, vocals, "OzzyOsbourne");
        entApi.addUserToEntitlementGroup(rootContext, vocals, "RonnieJamesDio");
        entApi.addUserToEntitlementGroup(rootContext, vocals, "IanGillan");

        entApi.addUserToEntitlementGroup(rootContext, guitars, "TonyIommi");
        entApi.addUserToEntitlementGroup(rootContext, guitars, "GeezerButler");

        entApi.addUserToEntitlementGroup(rootContext, drums, "BillWard");

        List<RaptureEntitlement> ents = entApi.getEntitlementsForGroup(rootContext, vocals);

        System.out.println(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(ents)));

        assertEquals(3, ents.size());
        for (RaptureEntitlement re : ents) {
            assertTrue(re.getGroups().contains(vocals));
        }
    }
}
