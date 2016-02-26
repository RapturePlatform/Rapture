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
package rapture.kernel.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static rapture.common.Scheme.DOCUMENT;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.testng.Assert;

import rapture.common.CallingContext;
import rapture.common.DocumentAttributeFactory;
import rapture.common.RaptureIdGenConfig;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.XferDocumentAttribute;
import rapture.common.exception.RaptureException;
import rapture.common.model.DocWriteHandle;
import rapture.common.model.DocumentAttribute;
import rapture.common.model.DocumentRepoConfig;
import rapture.common.model.DocumentWithMeta;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class DocumentAttributeTest {

    private static final CallingContext CTX = ContextFactory.getKernelUser();
    private static final String authority = "docAttrTest";
    private static final String docPathPrefix = "documentPrefix";

    @Before
    public void setup() {
        if (!Kernel.getDoc().docRepoExists(CTX, RaptureURI.builder(Scheme.DOCUMENT, authority).asString())) {
            Kernel.getDoc().createDocRepo(CTX, RaptureURI.builder(Scheme.DOCUMENT, authority).asString(), "NREP {} USING MEMORY {}");
        }
    }

    @Test
    public void testLinkAttribute() {
        String docPath = docPathPrefix + "/opt";
        String attributeType = DocumentAttributeFactory.LINK_TYPE;
        String link1 = "doc";
        String link2 = "doc2";

        Kernel.getDoc().setDocAttribute(CTX, "//" + authority + "/" + docPath + "/$" + attributeType + "/" + link1, docPathPrefix + "/other");
        DocumentAttribute doc = Kernel.getDoc().getDocAttribute(CTX, "//" + authority + "/" + docPath + "/$" + attributeType + "/" + link1);
        assertEquals(docPathPrefix + "/other", doc.getValue());
        Kernel.getDoc().setDocAttribute(CTX, "//" + authority + "/" + docPath + "/$" + attributeType + "/" + link2, docPathPrefix + "/other2");
        doc = Kernel.getDoc().getDocAttribute(CTX, "//" + authority + "/" + docPath + "/$" + attributeType + "/" + link2);
        assertEquals(docPathPrefix + "/other2", doc.getValue());
        List<XferDocumentAttribute> docAttributes = Kernel.getDoc().getDocAttributes(CTX, "//" + authority + "/" + docPath + "/$" + attributeType);
        assertEquals(2, docAttributes.size());
        for (DocumentAttribute de : docAttributes) {
            assertEquals(attributeType, de.getAttributeType());
            System.out.println(de.getKey() + ":" + de.getValue());
        }
    }

    @Test
    public void testAlotOfLinks() {
        String docPath = docPathPrefix + "/optx";
        String attributeType = DocumentAttributeFactory.LINK_TYPE;
        String key = "doc";
        String linkdoc = docPathPrefix + "/other";

        List<String> keys = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        for (int i = 0; i < 1000; i++) {
            keys.add(key + i);
            values.add(linkdoc + i);
        }
        Kernel.getDoc().setDocAttributes(CTX, "//" + authority + "/" + docPath + "/$" + attributeType, keys, values);
        List<XferDocumentAttribute> docAttributes = Kernel.getDoc().getDocAttributes(CTX, "//" + authority + "/" + docPath + "/$" + attributeType);
        assertEquals(1000, docAttributes.size());
        for (DocumentAttribute de : docAttributes) {
            assertEquals(attributeType, de.getAttributeType());
        }

        try {
            docAttributes = Kernel.getDoc().getDocAttributes(CTX, "//" + authority + "/" + docPath + "/$" + "unknown");
            fail();
        } catch (Exception e) {

        }
    }

    @Test
    public void testOverwriteAttribute() {
        String docPath = docPathPrefix + "/opt/xyz";
        String attributeType = DocumentAttributeFactory.META_TYPE;
        String key = "size";

        Kernel.getDoc().setDocAttribute(CTX, "//" + authority + "/" + docPath + "/$" + attributeType + "/" + key, "190");
        DocumentAttribute value = Kernel.getDoc().getDocAttribute(CTX, "//" + authority + "/" + docPath + "/$" + attributeType + "/" + key);
        assertEquals("190", value.getValue());
        Kernel.getDoc().setDocAttribute(CTX, "//" + authority + "/" + docPath + "/$" + attributeType + "/" + key, "210");
        value = Kernel.getDoc().getDocAttribute(CTX, "//" + authority + "/" + docPath + "/$" + attributeType + "/" + key);
        assertEquals("210", value.getValue());

        List<XferDocumentAttribute> docAttributes = Kernel.getDoc().getDocAttributes(CTX, "//" + authority + "/" + docPath + "/$" + attributeType);
        assertEquals(1, docAttributes.size());
    }

    @Test
    public void testMetaAttribute() {
        String dispName = docPathPrefix + "/opt";
        String attributeType = DocumentAttributeFactory.META_TYPE;
        String mime1 = "mime-type";

        Kernel.getDoc().setDocAttribute(CTX, "//" + authority + "/" + dispName + "/$" + attributeType + "/" + mime1, "text/plain");
        DocumentAttribute value = Kernel.getDoc().getDocAttribute(CTX, "//" + authority + "/" + dispName + "/$" + attributeType + "/" + mime1);
        assertEquals("text/plain", value.getValue());

        // shouldnt work
        try {
            Kernel.getDoc().setDocAttribute(CTX, "//" + authority + "/" + dispName + "/$" + attributeType, null);
            fail();
        } catch (RaptureException re) {

        }
    }

    @Test
    public void testRemoveAttribute() {
        String docPath = docPathPrefix + "/optr";
        String attributeType = DocumentAttributeFactory.META_TYPE;
        String mime1 = "mime-type";

        Kernel.getDoc().setDocAttribute(CTX, "//" + authority + "/" + docPath + "/$" + attributeType + "/" + mime1, "text/plain");
        DocumentAttribute value = Kernel.getDoc().getDocAttribute(CTX, "//" + authority + "/" + docPath + "/$" + attributeType + "/" + mime1);
        assertEquals("text/plain", value.getValue());
        assertTrue(Kernel.getDoc().deleteDocAttribute(CTX, "//" + authority + "/" + docPath + "/$" + attributeType + "/" + mime1));
        value = Kernel.getDoc().getDocAttribute(CTX, "//" + authority + "/" + docPath + "/$" + attributeType + "/" + mime1);
        assertNull(value);

    }

    @Test
    public void testPutContentP() {
        String docPathWithoutAtt = docPathPrefix + "/docx1";
        String docPath = docPathWithoutAtt + "/$link/\"docref\"";
        String content = "\"mytype/mydoc\"";
        assertEquals("document://docAttrTest/documentPrefix/docx1/$link/\"docref\"",
                Kernel.getDoc().putDoc(CTX, "//" + authority + "/" + docPath, content));
        DocumentAttribute value = Kernel.getDoc().getDocAttribute(CTX, "//" + authority + "/" + docPath);
        assertEquals(content, value.getValue());
        String ret = Kernel.getDoc().getDoc(CTX, "//" + authority + "/" + docPath);
        assertEquals(content, ret);
    }

    @Test
    public void testPutDocument() {
        String docPathWithoutAtt = docPathPrefix + "/docx1";
        String docPath = docPathWithoutAtt + "/$link/\"docref\"";
        String content = "\"mytype/mydoc\"";
        String uri = "//" + authority + "/" + docPath;
        String returi = Kernel.getDoc().putDoc(CTX, uri, content);
        assertNotNull(returi);
        assertEquals("document:" + uri, returi);
        DocumentAttribute value = Kernel.getDoc().getDocAttribute(CTX, "//" + authority + "/" + docPath);
        assertEquals(content, value.getValue());
        String ret = Kernel.getDoc().getDoc(CTX, "//" + authority + "/" + docPath);
        assertEquals(content, ret);
    }

    @Test
    public void testPutDocumentWithAutoID() {
        @SuppressWarnings("unused")
        String docPathWithoutAtt = docPathPrefix + "/docx1";
        //        String docPath = docPathWithoutAtt + "/$link/\"docref\"";
        String content = "{\"Foo\" : \"#id\"}";
        String uri = "//" + authority + "/" + "#id";

        String idGenCfg = "IDGEN { base=\"10\",length=\"5\"} USING MEMORY { }";

        RaptureURI internalUri = new RaptureURI(uri, DOCUMENT);

        String idGenUri = RaptureURI.builder(Scheme.IDGEN, internalUri.getAuthority()).build().toString();
        System.out.println("IdGen uri: " + idGenUri);
        
        if (Kernel.getIdGen().idGenExists(CTX, idGenUri)) {
            System.err.println("Warning idGen already exists - deleting");
            Kernel.getIdGen().deleteIdGen(CTX, idGenUri);
        }

        @SuppressWarnings("unused")
        RaptureIdGenConfig createIdGen = Kernel.getIdGen().createIdGen(CTX, idGenUri, idGenCfg);

        DocumentRepoConfig config = Kernel.getRepoCacheManager().getDocConfig(internalUri.getAuthority());
        config.setIdGenUri(idGenUri);

        if ((config != null) && (config.getIdGenUri() != null)) {
            String returi = Kernel.getDoc().putDoc(CTX, uri, content);
            assertNotNull(returi);
            // New URI should be different - should have an auto ID
            Assert.assertNotEquals("document:" + uri, returi);

            // Contents should be different - #id should have been expanded
            String ret = Kernel.getDoc().getDoc(CTX, returi);
            assertNotNull(ret);
            Assert.assertNotEquals(content, ret);
        } else {
            System.err.println("Can't test this - " + config + " has no IdGen URI set");
        }
    }

    @Test
    public void testPutDocumentWithoutAutoID() {
        String content = "{\"Foo\" : \"#id\"}";
        String uri = "//" + authority + "/" + "t#flaccid";
        String returi = Kernel.getDoc().putDoc(CTX, uri, content);
        assertNotNull(returi);
        assertEquals("New document name should NOT have auto ID - should be unchanged", "document:" + uri, returi);
        // Contents should be unchanged - #id should not have been expanded
        String ret = Kernel.getDoc().getDoc(CTX, returi);
        assertEquals(content, ret);
    }

    @Test
    public void testPutDocumentWithVersion() {
        String content1 = "{\"Foo\" : \"#id1\"}";
        String content2 = "{\"Foo\" : \"#id2a\"}";
        String content3 = "{\"Foo\" : \"#id3\"}";

        String docPath = "somePath";
        RaptureURI baseURI = RaptureURI.builder(DOCUMENT, authority).docPath(docPath).build();
        RaptureURI uri1 = RaptureURI.builder(baseURI).version("" + 1).build();
        RaptureURI uri2 = RaptureURI.builder(baseURI).version("" + 2).build();

        DocWriteHandle handle = Kernel.getDoc().putDocWithEventContext(CTX, uri1.toString(), content1, null);
        assertEquals(uri1.toString(), handle.getDocumentURI());
        assertTrue(handle.getIsSuccess());
        assertVersionContentMatches(content1, baseURI, 1);

        handle = Kernel.getDoc().putDocWithEventContext(CTX, uri1.toString(), content2, null);
        assertEquals(uri1.toString(), handle.getDocumentURI());
        assertTrue(handle.getIsSuccess());
        assertVersionContentMatches(content1, baseURI, 1); //put does not affect v1
        assertVersionContentMatches(content2, baseURI, 2);

        handle = Kernel.getDoc().putDocWithEventContext(CTX, uri2.toString(), content3, null);
        assertEquals(uri2.toString(), handle.getDocumentURI());
        assertTrue(handle.getIsSuccess());
        assertVersionContentMatches(content1, baseURI, 1);
        assertVersionContentMatches(content2, baseURI, 2);
        assertVersionContentMatches(content3, baseURI, 3);

        handle = Kernel.getDoc().putDocWithEventContext(CTX, uri2.toString(), content3, null);
        assertEquals(uri2.toString(), handle.getDocumentURI());
        assertFalse(handle.getIsSuccess());

        assertVersionContentMatches(content1, baseURI, 1);
        assertVersionContentMatches(content2, baseURI, 2);
        assertVersionContentMatches(content3, baseURI, 3);

    }

    protected void assertVersionContentMatches(String content, RaptureURI uri, int version) {
        uri = RaptureURI.builder(uri).version(version + "").build();
        DocumentWithMeta ret = Kernel.getDoc().getDocAndMeta(CTX, uri.toString());
        assertEquals(version, ret.getMetaData().getVersion().intValue());
        assertEquals(String.format("version %s content matches", version), content, ret.getContent());
    }

}
