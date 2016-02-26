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
package com.incapture.rapgen.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

import com.incapture.rapgen.parser.SampleCodeParser.SourceType;

public class SampleCodeParserTest {

    @Test
    public void testFullRun() throws SampleCodeParserException {
        SampleCodeParser p = new SampleCodeParser();
        p.addSourcePath(SourceType.java, "../../RaptureCore/src/test/java");
        p.parse();
        String result = p.getSampleCode(SourceType.java, "doc", "getDoc");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("\n" +
                "    CallingContext ctx = ContextFactory.getKernelUser();\n" +
                "    String docUri = \"document://myrepo/mydoc\";\n" +
                "    // setup our json content\n" +
                "    String jsonContent = \"{ \\\"key\\\" : \\\"value\\\" }\";\n" +
                "    // put a document first\n" +
                "    assertEquals(docUri, api.putDoc(ctx, docUri, jsonContent));\n" +
                "    // now get the same document out\n" +
                "    assertEquals(jsonContent, api.getDoc(ctx, docUri));\n" +
                "    // attempt to get a document that does not exist, should return null\n" +
                "    assertNull(api.getDoc(ctx, \"document://unknown/doesntexist\"));\n", result);
    }

    @Test
    public void testStripCurlyBrackets() {
        SampleCodeParser s = new SampleCodeParser();
        assertNull(s.stripCurlyBrackets(null));
        assertNull(s.stripCurlyBrackets(""));
        assertNull(s.stripCurlyBrackets("  "));
        assertEquals("\n hello\n ", s.stripCurlyBrackets("{\n hello\n }"));
        assertEquals("\n hello\n ", s.stripCurlyBrackets("  {\n hello\n }\n\n"));
        assertEquals("\n hello\n one more line;\n ok; ", s.stripCurlyBrackets("{\n hello\n one more line;\n ok; }"));
    }

    @Test
    public void testStripTestPrefix() {
        SampleCodeParser s = new SampleCodeParser();
        assertNull(s.stripTestPrefix(null));
        assertNull(s.stripTestPrefix(""));
        assertNull(s.stripTestPrefix("  "));
        assertEquals("hello", s.stripTestPrefix("hello"));
        assertEquals("helloTestMe", s.stripTestPrefix("helloTestMe"));
        assertEquals("hellotest", s.stripTestPrefix("hellotest"));
        assertEquals("getMe", s.stripTestPrefix("TestGetMe"));
        assertEquals("getMe", s.stripTestPrefix("testGetMe"));
        assertEquals("getMe", s.stripTestPrefix("TESTgetMe"));
    }

    @Test
    public void testBadInput() {
        try {
            SampleCodeParser p = new SampleCodeParser();
            p.addSourcePath(SourceType.java, "unknownPath_ksjksjf");
            p.parse();
            fail("Should have thrown exception");
        } catch (SampleCodeParserException | IllegalArgumentException e) {
        }
    }

}