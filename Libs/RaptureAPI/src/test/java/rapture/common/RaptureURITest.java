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
package rapture.common;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.replay;

import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
//mock imports
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ RaptureURI.class })
public class RaptureURITest {

    @Test
    public void testParse() {
        RaptureURI docURI = new RaptureURI("document://authority.type/folder/document", Scheme.DOCUMENT);
        assertEquals(Scheme.DOCUMENT, docURI.getScheme());
        assertEquals("authority.type", docURI.getAuthority());
        assertEquals("folder/document", docURI.getDocPath());
    }

    @Test
    public void testSuperfluousSlashesIgnored() {
        RaptureURI docURI = new RaptureURI("/////foo//bar", Scheme.DOCUMENT);
        assertNotNull(docURI);
        assertEquals("foo", docURI.getAuthority());
        assertEquals("bar", docURI.getDocPath());
    }

    @Test(expected = RaptureException.class)
    public void testBadAuthority() {
        new RaptureURI("document://authority$5/type/folder/document", Scheme.DOCUMENT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullAuthority() {
        RaptureURI.builder(Scheme.DOCUMENT, null).build();
    }

    @Test
    public void testDoesNotExist() {
        new RaptureURI("job://DOESNOTEXIST", Scheme.WORKFLOW);
    }

    @Test
    public void testOnlyAuthorityParse() {
        RaptureURI docURI = new RaptureURI("authority://authority/", Scheme.AUTHORITY);
        assertEquals(Scheme.AUTHORITY, docURI.getScheme());
        assertEquals("authority", docURI.getAuthority());
    }

    @Test
    public void testDoubleSlashParse() {
        RaptureURI docURI = new RaptureURI("authority://authority//a///b////c", Scheme.AUTHORITY);
        assertEquals(Scheme.AUTHORITY, docURI.getScheme());
        // The URI standard allows null path elements but doesn't specify what you need to do about them.
        // I think we should explicitly ignore them.
        assertEquals("a/b/c", docURI.getDocPath());
    }

    @Test
    public void testColonInPathParse() {
        RaptureURI docURI = new RaptureURI("series://DeltaOne/BuyAll/Fut/RX1_R:00_0_D_Comdty", Scheme.SERIES);
        assertEquals("BuyAll/Fut/RX1_R:00_0_D_Comdty", docURI.getDocPath());
    }

    @Test
    public void testNoScheme() {
        RaptureURI docURI = new RaptureURI("//authority/a/b/c", Scheme.DOCUMENT);
        assertEquals(Scheme.DOCUMENT, docURI.getScheme());
        assertEquals("authority", docURI.getAuthority());
        assertEquals("a/b/c", docURI.getDocPath());
    }

    @Test
    public void testNoSchemeNoPath() {
        RaptureURI docURI = new RaptureURI("//authority", Scheme.DOCUMENT);
        assertEquals(Scheme.DOCUMENT, docURI.getScheme());
        assertEquals("authority", docURI.getAuthority());
    }

    @Test
    public void testDocPathDepth() {
        RaptureURI docURI = new RaptureURI("document://authority/folder/document", Scheme.DOCUMENT);
        assertEquals(3, docURI.getDocPathDepth());
        RaptureURI emptyURI = new RaptureURI("document://authority/", Scheme.DOCUMENT);
        assertEquals(1, emptyURI.getDocPathDepth());
    }

    @Test
    public void testAttributeParse() {
        RaptureURI docURI = new RaptureURI("document://authority/document/$meta/content-type", Scheme.DOCUMENT);
        assertEquals(Scheme.DOCUMENT, docURI.getScheme());
        assertEquals("authority", docURI.getAuthority());
        assertEquals("document/$meta/content-type", docURI.getDocPathWithAttributes());
        assertEquals("meta/content-type", docURI.getAttribute());
        assertEquals("content-type", docURI.getAttributeKey());
        assertEquals("meta", docURI.getAttributeName());
    }

    @Test
    public void testElementParse() {
        RaptureURI docURI = new RaptureURI("document://sys.RaptureConfig/type/junit/testtype#DocumentRepo", Scheme.DOCUMENT);
        assertEquals(Scheme.DOCUMENT, docURI.getScheme());
        assertEquals("sys.RaptureConfig", docURI.getAuthority());
        assertEquals("type/junit/testtype", docURI.getDocPath());
        assertEquals("DocumentRepo", docURI.getElement());
    }

    @Test
    public void testShortElementParse() {
        RaptureURI docURI = new RaptureURI("document://junit.testtype/docPath#DocumentRepo", Scheme.DOCUMENT);
        assertEquals(Scheme.DOCUMENT, docURI.getScheme());
        assertEquals("junit.testtype", docURI.getAuthority());
        assertEquals("DocumentRepo", docURI.getElement());
        assertEquals("docPath", docURI.getDocPath());
    }

    @Test
    public void testVersionParse() {
        RaptureURI docURI = new RaptureURI("document://authority/document/@1", Scheme.DOCUMENT);
        assertEquals(Scheme.DOCUMENT, docURI.getScheme());
        assertEquals("authority", docURI.getAuthority());
        assertEquals("document", docURI.getDocPath());
        assertEquals("1", docURI.getVersion());
        assertNull(docURI.getAsOfTime());
    }

    @Test
    public void testAsOfTimeParse() {
        RaptureURI docURI = new RaptureURI("document://authority/document/@t1446836246", Scheme.DOCUMENT);
        assertEquals(Scheme.DOCUMENT, docURI.getScheme());
        assertEquals("authority", docURI.getAuthority());
        assertEquals("document", docURI.getDocPath());
        assertEquals("t1446836246", docURI.getAsOfTime());
        assertNull(docURI.getVersion());

        docURI = new RaptureURI("document://authority/document/@20151106T101202-0800", Scheme.DOCUMENT);
        assertEquals(Scheme.DOCUMENT, docURI.getScheme());
        assertEquals("authority", docURI.getAuthority());
        assertEquals("document", docURI.getDocPath());
        assertEquals("20151106T101202-0800", docURI.getAsOfTime());
        assertNull(docURI.getVersion());
    }

    @Test
    public void testVersionAttributeParse() {
        RaptureURI docURI = new RaptureURI("document://authority/document/@1$meta/Content-Type", Scheme.DOCUMENT);
        assertEquals(Scheme.DOCUMENT, docURI.getScheme());
        assertEquals("authority", docURI.getAuthority());
        assertEquals("document/$meta/Content-Type", docURI.getDocPathWithAttributes());
        assertEquals("1", docURI.getVersion());
        assertEquals("meta/Content-Type", docURI.getAttribute());
    }

    @Test
    public void testAsOfTimeAttributeParse() {
        RaptureURI docURI = new RaptureURI("document://authority/document/@20151106T101202-0800$meta/Content-Type", Scheme.DOCUMENT);
        assertEquals(Scheme.DOCUMENT, docURI.getScheme());
        assertEquals("authority", docURI.getAuthority());
        assertEquals("document/$meta/Content-Type", docURI.getDocPathWithAttributes());
        assertEquals("20151106T101202-0800", docURI.getAsOfTime());
        assertEquals("meta/Content-Type", docURI.getAttribute());
    }

    @Test
    public void testNakedAuthority() {
        RaptureURI uri = new RaptureURI("//authority/", Scheme.FIELD);
        assertEquals("authority/", uri.getFullPath());
    }

    @Test
    public void testNoTypeScheme() {
        RaptureURI docURI = new RaptureURI("field://authority/document", Scheme.FIELD);
        assertEquals("authority/document", docURI.getFullPath());
        assertEquals("document", docURI.getDocPath());
        assertEquals("document", docURI.getDisplayName());
    }

    @Test
    public void testDisplayName() {
        RaptureURI docURI = new RaptureURI("document://authority/document", Scheme.DOCUMENT);
        assertEquals("document", docURI.getDisplayName());
    }

    @Test
    public void testEmptyDisplayName() {
        RaptureURI mockUri = EasyMock.createMock(RaptureURI.class);
        expect(mockUri.getDisplayName()).andReturn("");
        replay(mockUri);

        assertEquals("", mockUri.getDisplayName());
        // verify();
    }

    @Test
    public void testNullDisplayName() {
        RaptureURI mockUri = EasyMock.createMock(RaptureURI.class);
        expect(mockUri.getDisplayName()).andReturn(null);
        replay(mockUri);

        assertEquals(null, mockUri.getDisplayName());
        // verify();
    }

    @Test
    public void testShortPath() {
        RaptureURI docURI = new RaptureURI("document://authority/document#ele$x", Scheme.DOCUMENT);
        assertEquals("authority/document", docURI.getShortPath());
    }

    @Test
    public void testToShortString() {
        RaptureURI docURI = new RaptureURI("document://authority/document#ele$x", Scheme.DOCUMENT);
        assertEquals("document://authority/document", docURI.toShortString());
    }

    @Test
    public void testFullPath() {
        RaptureURI docURI = new RaptureURI("document://authority/document", Scheme.DOCUMENT);
        assertEquals("authority/document", docURI.getFullPath());
    }

    @Test
    public void testToString() {
        RaptureURI docURI = new RaptureURI("document://authority/document", Scheme.DOCUMENT);
        assertEquals("document://authority/document", docURI.toString());
    }

    @Test
    public void testOptionalScheme() {
        RaptureURI docURI = new RaptureURI("//authority/folder/document", Scheme.DOCUMENT);
        assertEquals("document://authority/folder/document", docURI.toString());

    }

    @Test
    public void testDocPath() {
        RaptureURI uri = RaptureURI.builder(Scheme.BLOB, "authority").docPath("two/things").attribute("attribute").build();
        assertEquals("attribute", uri.getAttribute());
        assertEquals("two/things/$attribute", uri.getDocPathWithAttributes());
        uri = RaptureURI.builder(Scheme.BLOB, "authority").docPath("two/things").build();
        assertTrue(uri.getAttribute() == null);
        assertEquals("two/things", uri.getDocPath());
    }

    @Test
    public void testBuilder() {
        // RAP-1101
        RaptureURI docURI = new RaptureURI("//authority/folder/document", Scheme.DOCUMENT);
        RaptureURI docURI2 = new RaptureURI("//authority/folder/document", Scheme.DOCUMENT);
        assertEquals(docURI, docURI2);
        RaptureURI builtURI = RaptureURI.builder(docURI).attribute("Foo").docPath("Bar").element("Baz").version("-1").build();
        assertEquals(docURI, docURI2);
        assertNotEquals(docURI, builtURI);
    }

    @Test
    public void testHasDocPath() {
        RaptureURI docURI = new RaptureURI("document://authority/", Scheme.DOCUMENT);
        assertFalse(docURI.hasDocPath());

        docURI = new RaptureURI("document://authority/some/path/or/other", Scheme.DOCUMENT);
        assertTrue(docURI.hasDocPath());

    }

    @Test
    public void testJacksonSerializeAndDeserialize() {
        RaptureURI docURI = new RaptureURI("document://authority/", Scheme.DOCUMENT);
        String jsonFromObject = JacksonUtil.jsonFromObject(docURI);
        RaptureURI unmarshalledURI = JacksonUtil.objectFromJson(jsonFromObject, RaptureURI.class);
        assertEquals(docURI, unmarshalledURI);
    }

    @Test
    public void testDocPathCharset() {
        for (int i = 32; i < 128; i++) {
            char c = (char) i;
            String uri = "//authority/folder/docu" + c + "ment/path/foo";
            switch (c) {
            case '&':
            case '?':
            case ' ':
            case '`':
            case '\'':
            case '+':
            case ',':
            case '}':
            case '[':
            case '<':
            case ';':
            case '^':
            case '{':
            case '>':
            case ']':
            case '=':
            case '\\':
                try {
                    new RaptureURI(uri, Scheme.DOCUMENT);
                    fail("Character " + c + " is not legal in DocPath");
                } catch (rapture.common.exception.RaptureException e1) {
                    // expected
                } catch (Throwable e2) {
                    fail("Expected rapture.common.exception.RaptureException (wrapping java.net.URISyntaxException) not " + e2);
                }
                break;
            default:
                RaptureURI docURI = new RaptureURI(uri, Scheme.DOCUMENT);
                assertNotNull(docURI);
            }
        }
    }

    @Test
    public void testAuthCharset() {
        for (int i = 32; i < 128; i++) {
            char c = (char) i;
            String uri = "//auth" + c + "ority/folder/document/path/foo";
            switch (c) {
            case '+': // We currently do not allow + as a substitute for space
                // see below for ':'
            case '&':
            case '?':
            case ' ':
            case '`':
            case '\'':
            case ',':
            case '}':
            case '[':
            case '<':
            case ';':
            case '^':
            case '{':
            case '>':
            case ']':
            case '=':
            case '\\':

            case '#': // Different from Doc Path
            case '$': // Different from Doc Path
                try {
                    new RaptureURI(uri, Scheme.DOCUMENT);
                    fail("Character " + c + " should not be legal in Authority");
                } catch (Exception e1) {
                    System.out.println("Character " + c + " is not legal in Authority");
                }
                break;

            case '"': // Is part of the whitelist but in practice it causes a problem with antlr. (Bug?)
            case '/': // considered legal but doesn't work - because ://
            case ':': // considered legal but doesn't work - because ://
            case '@': // legal and works
            default:
                RaptureURI docURI = new RaptureURI(uri, Scheme.DOCUMENT);
                assertNotNull(docURI);
            }
        }
    }

    @Test
    public void testGetLeafName() {
        assertEquals("c", RaptureURI.builder(Scheme.BLOB, "foo").docPath("a/b/c").build().getLeafName());
        assertEquals("d", RaptureURI.builder(Scheme.BLOB, "foo").docPath("d").build().getLeafName());
        assertEquals(null, RaptureURI.builder(Scheme.BLOB, "foo").docPath("/").build().getLeafName());
        assertEquals(null, RaptureURI.builder(Scheme.BLOB, "foo").docPath("").build().getLeafName());
        assertEquals(null, RaptureURI.builder(Scheme.BLOB, "foo").docPath(null).build().getLeafName());
    }

    @Test
    public void testGetParentURI() {
        assertEquals(RaptureURI.builder(Scheme.BLOB, "foo").docPath("a/b").build(),
                RaptureURI.builder(Scheme.BLOB, "foo").docPath("a/b/c").build().getParentURI());
        assertEquals(RaptureURI.builder(Scheme.BLOB, "foo").docPath("").build(), RaptureURI.builder(Scheme.BLOB, "foo").docPath("d").build().getParentURI());
        assertEquals(null, RaptureURI.builder(Scheme.BLOB, "foo").docPath("").build().getParentURI());
        assertEquals(null, RaptureURI.builder(Scheme.BLOB, "foo").docPath(null).build().getParentURI());
    }

    @Test
    public void testCreateFromFullPathWithAttribute() {
        RaptureURI result = RaptureURI.createFromFullPathWithAttribute("test/me", null, Scheme.DOCUMENT);
        assertEquals("document://test/me", result.toString());
        result = RaptureURI.createFromFullPathWithAttribute("test", null, Scheme.DOCUMENT);
        assertEquals("document://test/", result.toString());
        result = RaptureURI.createFromFullPathWithAttribute("test/x/y", null, Scheme.DOCUMENT);
        assertEquals("document://test/x/y", result.toString());
        result = RaptureURI.createFromFullPathWithAttribute("/test/x/y", null, Scheme.DOCUMENT);
        assertEquals("document://test/x/y", result.toString());
    }
}
