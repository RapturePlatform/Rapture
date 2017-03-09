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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import com.google.common.collect.ImmutableMap;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentWithMeta;
import rapture.dsl.dparse.AbsoluteVersion;
import rapture.dsl.dparse.AsOfTimeDirective;
import rapture.dsl.dparse.AsOfTimeDirectiveParser;
import rapture.lock.dummy.DummyLockHandler;
import rapture.repo.KeyStore;
import rapture.repo.NVersionedRepo;

public class GoogleDatastoreKeyStoreTest {
    private NVersionedRepo repo;

    KeyStore store;
    KeyStore meta;
    KeyStore version;
    KeyStore attribute;

    LocalDatastoreHelper helper = LocalDatastoreHelper.create();

    @Before
    public void setup() throws IOException, InterruptedException {
        helper.start(); // Starts the local Datastore emulator in a separate process

        Datastore localDatastore = helper.getOptions().getService();

        store = new GoogleDatastoreKeyStore(localDatastore);
        store.setConfig(ImmutableMap.of("prefix", "store"));
        meta = new GoogleDatastoreKeyStore(localDatastore);
        meta.setConfig(ImmutableMap.of("prefix", "meta"));
        version = new GoogleDatastoreKeyStore(localDatastore);
        version.setConfig(ImmutableMap.of("prefix", "version"));
        attribute = new GoogleDatastoreKeyStore(localDatastore);
        attribute.setConfig(ImmutableMap.of("prefix", "attribute"));
        repo = new NVersionedRepo(new HashMap<String, String>(), store, version, meta, attribute, new DummyLockHandler());
    }

    @After
    public void tidyUp() throws IOException, InterruptedException, TimeoutException {
        store.dropKeyStore();
        meta.dropKeyStore();
        version.dropKeyStore();
        attribute.dropKeyStore();
        helper.stop(new Duration(6000));
    }


    @Test
    public void multipleVersions() {
        // If we add multiple versions, do we get the latest version when we
        // return the content?
        repo.addDocument("test1", "{\"test\":1}", "arthur", "test version 1", false);
        repo.addDocument("test1", "{\"test\":2}", "arthur", "this is version 2", false);

        String result = repo.getDocument("test1");
        assertEquals("{\"test\":2}", result);
    }

    @Test
    public void deletion() {
        // If we add two documents, then delete, we should have nothing visible
        // If we add multiple versions, do we get the latest version when we
        // return the content?
        repo.addDocument("test1", "{\"test\":1}", "arthur", "test version 1", false);
        repo.addDocument("test1", "{\"test\":2}", "arthur", "this is version 2", false);
        repo.removeDocument("test1", "arthur", "removal");

        String result = repo.getDocument("test1");
        assertTrue(result == null);

    }

    @Test
    public void retrieveMeta() throws InterruptedException {
        String key = "test1";
        String value = "{\"test\":1}";
        String user = "arthur";
        String comment = "test version 1";
        long before = beforeAndSleep();
        repo.addDocument(key, value, user, comment, false);
        long after = afterAndSleep();
        DocumentWithMeta found = repo.getDocAndMeta(key, null);

        assertDocWithMeta(key, value, user, comment, before, after, found);
    }

    @Test
    public void retrieveMetaNoContent() {
        repo.addDocument("test1", "{\"test\":1}", "arthur", "test version 1", false);
        DocumentMetadata metaContent = repo.getMeta("test1", null);
        System.out.println(JacksonUtil.jsonFromObject(metaContent));
    }

    @Test
    public void revert() {
        repo.addDocument("test1", "{\"test\":1}", "arthur", "test version 1", false);
        repo.addDocument("test1", "{\"test\":2}", "arthur", "test version 2", false);
        repo.revertDoc("test1", null);
        DocumentMetadata metaContent = repo.getMeta("test1", null);
        System.out.println(JacksonUtil.jsonFromObject(metaContent));
    }

    @Test
    public void testGetDocAndMetas() {
        final String uriPre = "document://guide/";
        repo.addDocument("test1", "{\"test\":1}", "ford", "test version 1", false);
        repo.addDocument("test2", "{\"test\":2}", "ford", "test version 1", false);
        repo.addDocument("test3", "{\"test\":3}", "ford", "test version 1", false);
        repo.addDocument("test4", "{\"test\":4}", "ford", "test version 1", false);

        List<DocumentWithMeta> dwms = repo.getDocAndMetas(makeRaptureURIList(uriPre, "test1", "test2", "test3", "test4"));
        assertEquals(4, dwms.size());
        int i = 1;
        for (DocumentWithMeta dwm : dwms) {
            assertEquals("{\"test\":" + i + "}", dwm.getContent());
            assertEquals("test version 1", dwm.getMetaData().getComment());
            assertEquals("ford", dwm.getMetaData().getUser());
            assertEquals(1, dwm.getMetaData().getVersion().intValue());
            assertEquals("test" + i++, dwm.getDisplayName());
        }

        repo.addDocument("test1", "{\"test\":99}", "ford", "test version 2", false);

        dwms = repo.getDocAndMetas(makeRaptureURIList(uriPre, "test1", "test2", "test3", "test4"));
        assertEquals(4, dwms.size());
        i = 1;
        for (DocumentWithMeta dwm : dwms) {
            if (i == 1) {
                assertEquals("{\"test\":99}", dwm.getContent());
                assertEquals("test version 2", dwm.getMetaData().getComment());
                assertEquals(2, dwm.getMetaData().getVersion().intValue());
            } else {
                assertEquals("{\"test\":" + i + "}", dwm.getContent());
                assertEquals("test version 1", dwm.getMetaData().getComment());
                assertEquals(1, dwm.getMetaData().getVersion().intValue());
            }
            assertEquals("test" + i++, dwm.getDisplayName());
            assertEquals("ford", dwm.getMetaData().getUser());
        }

        dwms = repo.getDocAndMetas(makeRaptureURIList(uriPre, "test1@1", "test2@1", "test3@1", "test4"));
        assertEquals(4, dwms.size());
        i = 1;
        for (DocumentWithMeta dwm : dwms) {
            assertEquals("{\"test\":" + i + "}", dwm.getContent());
            assertEquals("test version 1", dwm.getMetaData().getComment());
            assertEquals(1, dwm.getMetaData().getVersion().intValue());
            assertEquals("ford", dwm.getMetaData().getUser());
            assertEquals("test" + i++, dwm.getDisplayName());
        }

        repo.addDocument("test4", "{\"test\":\"xx\"}", "ford", "test version 2", false);
        dwms = repo.getDocAndMetas(makeRaptureURIList(uriPre, "test1@1", "test2@1", "test3@1", "test4"));
        assertEquals(4, dwms.size());
        assertEquals("{\"test\":\"xx\"}", dwms.get(3).getContent());
        assertEquals("test4", dwms.get(3).getDisplayName());

        dwms = repo.getDocAndMetas(makeRaptureURIList(uriPre, "test1@1", "test2@1", "test3@1", "test4@1"));
        assertEquals(4, dwms.size());
        assertEquals("{\"test\":4}", dwms.get(3).getContent());
        assertEquals("test4", dwms.get(3).getDisplayName());
    }

    private List<RaptureURI> makeRaptureURIList(String pre, String... strs) {
        List<RaptureURI> ret = new ArrayList<>();
        for (String s : strs) {
            ret.add(new RaptureURI(pre + s, Scheme.DOCUMENT));
        }
        return ret;
    }

    @Test
    public void getWithMetaVersions() throws InterruptedException {
        String key = "test1";
        String value = "{\"test\":1}";
        String user = "arthur";
        String comment = "test version 1";
        long before = beforeAndSleep();
        repo.addDocument(key, value, user, comment, false);
        long after = afterAndSleep();

        DocumentWithMeta found = repo.getDocAndMeta(key, null);
        assertDocWithMeta(key, value, user, comment, before, after, found);
        Long created1 = found.getMetaData().getCreatedTimestamp();
        assertTrue(String.format("before =%s, after =%s, created=%s", before, after, created1), created1 > before && created1 < after);
        assertEquals(created1, found.getMetaData().getModifiedTimestamp());

        String value2 = "{\"some other value\":999}";
        long before2 = beforeAndSleep();
        repo.addDocument(key, value2, user, comment, false);
        long after2 = afterAndSleep();
        found = repo.getDocAndMeta(key, null);
        assertDocWithMeta(key, value2, user, comment, before2, after2, found);
        Long created2 = found.getMetaData().getCreatedTimestamp();
        assertEquals(created1, created2);

        AbsoluteVersion absoluteDirective = new AbsoluteVersion();
        absoluteDirective.setVersion(1);
        found = repo.getDocAndMeta(key, absoluteDirective);
        assertDocWithMeta(key, value, user, comment, before, after, found);
        Long created3 = found.getMetaData().getCreatedTimestamp();
        assertEquals(created1, created3);

        absoluteDirective.setVersion(2);
        found = repo.getDocAndMeta(key, absoluteDirective);
        assertDocWithMeta(key, value2, user, comment, before2, after2, found);
        Long created4 = found.getMetaData().getCreatedTimestamp();
        assertEquals(created4, created3);

        SimpleDateFormat asOfTimeFormat = new SimpleDateFormat(AsOfTimeDirectiveParser.AS_OF_TIME_FORMAT_MS);

        AsOfTimeDirective asOfTimeDirective = new AsOfTimeDirective();
        asOfTimeDirective.setAsOfTime(asOfTimeFormat.format(new Date(after)));
        found = repo.getDocAndMeta(key, asOfTimeDirective);
        assertDocWithMeta(key, value, user, comment, before, after, found);
        Long created5 = found.getMetaData().getCreatedTimestamp();
        assertEquals(created1, created5);

        asOfTimeDirective.setAsOfTime(asOfTimeFormat.format(new Date(after2)));
        found = repo.getDocAndMeta(key, asOfTimeDirective);
        assertDocWithMeta(key, value2, user, comment, before2, after2, found);
        Long created6 = found.getMetaData().getCreatedTimestamp();
        assertEquals(created4, created6);
    }

    protected long afterAndSleep() throws InterruptedException {
        Thread.sleep(1);
        return System.currentTimeMillis();
    }

    protected long beforeAndSleep() throws InterruptedException {
        long before = System.currentTimeMillis();
        Thread.sleep(1);
        return before;
    }

    protected void assertDocWithMeta(String key, String value, String user, String comment, long before, long after, DocumentWithMeta found) {
        assertEquals(key, found.getDisplayName());
        assertEquals(value, found.getContent());
        assertEquals(user, found.getMetaData().getUser());
        assertEquals(comment, found.getMetaData().getComment());
        assertFalse(found.getMetaData().getDeleted());
    }
}
