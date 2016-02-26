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
package rapture.dsl.dparse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;

import rapture.common.model.DocumentAttribute;
import rapture.generated.DParseLexer;
import rapture.generated.DParseParser;
import rapture.generated.DParseParser.displayname_return;

public class DParseTest {
    private void testDisplayName(String config, String t, String disp) throws RecognitionException {
        DParseLexer lexer = new DParseLexer();
        lexer.setCharStream(new ANTLRStringStream(config));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        DParseParser parser = new DParseParser(tokens);
        displayname_return ret = parser.displayname();
        System.out.println("Type is " + ret.type);
        assertTrue(ret.type.equals(t));
        if (disp != null) {
            System.out.println("Disp is " + ret.disp);
            assertTrue(ret.disp.equals(disp));
        } else {
            assertTrue(ret.disp == null);
        }
        // BUT what about the cool bit after the ?

        BaseDirective bd = ret.directive;
        if (bd != null) {
            System.out.println("Type of directive is " + bd.getClass().getCanonicalName());
            System.out.println("Directive is " + bd.toString());
        }

        DocumentAttribute ext = ret.attribute;
        if (ext != null) {
            System.out.println("Attribute type is: [" + ext.getAttributeType() + "]");
            System.out.println("Attribute key is: [" + ext.getKey() + "]");
        }
    }

    @Test
    public void testDisplayNameParsing() throws RecognitionException {
        testDisplayName("a/b", "a", "b");
        testDisplayName("test/alan", "test", "alan");
        testDisplayName("thetype/parta/partb@5", "thetype", "parta/partb");
        testDisplayName("order/1", "order", "1");
        testDisplayName("order", "order", null);
    }

    @Test
    public void testDocumentAttributeParsing() throws RecognitionException {
        testDisplayNameWithAttribute("myt/myd/myc/myp@239/$link/\"thislink\"", "myt", "myd/myc/myp", "link");
        testDisplayNameWithAttribute("myt/myd/myc/myp/$link/\"thislink\"", "myt", "myd/myc/myp", "link");
        testDisplayNameWithAttribute("myt/myd/myc/myp@239/$meta/\"thislink\"", "myt", "myd/myc/myp", "meta");
    }

    private void testDisplayNameWithAttribute(String config, String t, String disp, String attributeType) throws RecognitionException {
        DParseLexer lexer = new DParseLexer();
        lexer.setCharStream(new ANTLRStringStream(config));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        DParseParser parser = new DParseParser(tokens);
        displayname_return ret = parser.displayname();
        assertTrue(ret.type.equals(t));
        assertTrue(ret.disp.equals(disp));
        DocumentAttribute ext = ret.attribute;
        assertNotNull(ext);
        assertEquals(attributeType, ext.getAttributeType());
        assertEquals(config, ext.getKey());
    }

    @Test
    public void testInvalidParsing() throws RecognitionException {
        testDisplayName("a1/b", "a1", "b");
    }

    @Test(expected = java.lang.AssertionError.class)
    public void testBadCharParsing() throws RecognitionException {
        testDisplayName("a/b._###2!a", "a", "b");
    }
}
