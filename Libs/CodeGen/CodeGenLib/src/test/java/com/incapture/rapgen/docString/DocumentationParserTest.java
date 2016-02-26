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
package com.incapture.rapgen.docString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import rapture.common.exception.RaptureException;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by seanchen on 6/8/15.
 */
public class DocumentationParserTest {

    String noParam = "This is the description w/o param after";
    String oneParam = "This is the description with param after @param var1 description of var1";
    String twoParams = "This is the description with params after @param var1 description of var1 @param var2 description of var2";
    String oneParamOneSince = "This is a description with param and since after @param var1 description of var1 @since x.y";
    String oneParamOneSinceOneReturn = "This is a description with param, since, and return after @param var1 description of var1 @since x.y @return something";
    String longDescription =
            "This is a long description This is a long description This is a long description This is a long description This is a long description " +
                    "This is a long description This is a long description This is a long description This is a long description This is a long description "
                    + "This is a long description "
                    +
                    "This is a long description This is a long description This is a long description This is a long description This is a long description "
                    + "This is a long description "
                    +
                    "This is a long description This is a long description This is a long description This is a long description This is a long description "
                    + "This is a long description ";
    String longParamDescription =
            "This is the description with long param after @param var1 description of var1 description of var1 description of var1 description of var1" +
                    " description of var1 description of var1 description of var1 description of var1 description of var1 description of var1 description of "
                    + "var1 description of var1"
                    +
                    " description of var1 description of var1 description of var1 description of var1 description of var1 description of var1 description of "
                    + "var1 description of var1";
    String malformedParams = "This is a description with param, since, and param after @param var1 description of var1 @since x.y @param var2 description of "
            + "var2";
    String incorrectTags1 = "This is a description with incorrect tags @para var1 description of var1";
    String incorrectTags2 = "This is a description with incorrect tags @return @param var1 description of var1";
    String incorrectTags3 = "This is a description with incorrect tags @return @since var1 description of var1";
    String incorrectTags4 = "This is a description with incorrect tags @return @sinc var1 description of var1";

    @Test
    public void testRetrieveDescription() {
        Assert.assertEquals("This is the description w/o param after\n", DocumentationParser.retrieveDescription(noParam));
        Assert.assertEquals("This is the description with param after\n", DocumentationParser.retrieveDescription(oneParam));
        Assert.assertEquals("This is the description with params after\n", DocumentationParser.retrieveDescription(twoParams));
        Assert.assertEquals("This is a description with param and since after\n", DocumentationParser.retrieveDescription(oneParamOneSince));
        Assert.assertEquals("This is a description with param, since, and return after\n", DocumentationParser.retrieveDescription(oneParamOneSinceOneReturn));
        Assert.assertEquals(
                "This is a long description This is a long description This is a long description\nThis is a long description This is a long description " +
                        "This is a long description\nThis is a long description This is a long description This is a long description\nThis is a long "
                        + "description This is a long description "
                        +
                        "This is a long description\nThis is a long description This is a long description This is a long description\nThis is a long "
                        + "description This is a long description "
                        +
                        "This is a long description\nThis is a long description This is a long description This is a long description\nThis is a long "
                        + "description This is a long description\n",
                DocumentationParser.retrieveDescription(longDescription));
        Assert.assertEquals("This is the description with long param after\n", DocumentationParser.retrieveDescription(longParamDescription));
        try {
            DocumentationParser.retrieveDescription(malformedParams);
            fail("Should have raised RaptureException for incorrect tags");
        } catch (RaptureException e) {
        }
        try {
            DocumentationParser.retrieveDescription(incorrectTags1);
            fail("Should have raised RaptureException for incorrect tags");
        } catch (RaptureException e) {
        }
        try {
            DocumentationParser.retrieveDescription(incorrectTags2);
            fail("Should have raised RaptureException for incorrect tags");
        } catch (RaptureException e) {
        }
        try {
            DocumentationParser.retrieveDescription(incorrectTags3);
            fail("Should have raised RaptureException for incorrect tags");
        } catch (RaptureException e) {
        }
        try {
            DocumentationParser.retrieveDescription(incorrectTags4);
            fail("Should have raised RaptureException for incorrect tags");
        } catch (RaptureException e) {
        }
    }

    @Test
    public void testRetrieveParams() {
        Map<String, String> retMap;
        Assert.assertEquals(DocumentationParser.retrieveParams(noParam).size(), 0);
        retMap = DocumentationParser.retrieveParams(oneParam);
        Assert.assertEquals("description of var1", retMap.get("var1"));
        retMap = DocumentationParser.retrieveParams(twoParams);
        Assert.assertEquals("description of var1", retMap.get("var1"));
        Assert.assertEquals("description of var2", retMap.get("var2"));
        retMap = DocumentationParser.retrieveParams(oneParamOneSince);
        Assert.assertEquals("description of var1", retMap.get("var1"));
        retMap = DocumentationParser.retrieveParams(oneParamOneSinceOneReturn);
        Assert.assertEquals("description of var1", retMap.get("var1"));
        Assert.assertEquals(0, DocumentationParser.retrieveParams(longDescription).size());
        retMap = DocumentationParser.retrieveParams(longParamDescription);
        Assert.assertEquals(
                "description of var1 description of var1 description of var1 description of var1\ndescription of var1 description of var1 description of" +
                        " var1 description of var1\ndescription of var1 description of var1 description of var1 description of var1\ndescription of var1 "
                        + "description of var1 description of var1"
                        +
                        " description of var1\ndescription of var1 description of var1 description of var1 description of var1", retMap.get("var1"));

        try {
            DocumentationParser.retrieveParams(malformedParams);
            fail("Should have raised RaptureException for malformed params");
        } catch (RaptureException e) {
        }
        try {
            DocumentationParser.retrieveParams(incorrectTags1);
            fail("Should have raised RaptureException for incorrect tags");
        } catch (RaptureException e) {
        }
        try {
            DocumentationParser.retrieveParams(incorrectTags2);
            fail("Should have raised RaptureException for incorrect tags");
        } catch (RaptureException e) {
        }
        try {
            DocumentationParser.retrieveParams(incorrectTags3);
            fail("Should have raised RaptureException for incorrect tags");
        } catch (RaptureException e) {
        }
        try {
            DocumentationParser.retrieveParams(incorrectTags4);
            fail("Should have raised RaptureException for incorrect tags");
        } catch (RaptureException e) {
        }
    }

    @Test
    public void testRetrieveSince() {
        Assert.assertEquals("", DocumentationParser.retrieveSince(noParam));
        Assert.assertEquals("", DocumentationParser.retrieveSince(oneParam));
        Assert.assertEquals("x.y", DocumentationParser.retrieveSince(oneParamOneSince));
        Assert.assertEquals("x.y", DocumentationParser.retrieveSince(oneParamOneSinceOneReturn));
        Assert.assertEquals("", DocumentationParser.retrieveSince(longDescription));
        Assert.assertEquals("", DocumentationParser.retrieveSince(longParamDescription));
        try {
            DocumentationParser.retrieveSince(malformedParams);
            fail("Should have raised RaptureException for malformed params");
        } catch (RaptureException e) {
        }
        try {
            DocumentationParser.retrieveSince(incorrectTags1);
            fail("Should have raised RaptureException for incorrect tags");
        } catch (RaptureException e) {
        }
        try {
            DocumentationParser.retrieveSince(incorrectTags2);
            fail("Should have raised RaptureException for incorrect tags");
        } catch (RaptureException e) {
        }
        try {
            DocumentationParser.retrieveSince(incorrectTags3);
            fail("Should have raised RaptureException for incorrect tags");
        } catch (RaptureException e) {
        }
        try {
            DocumentationParser.retrieveSince(incorrectTags4);
            fail("Should have raised RaptureException for incorrect tags");
        } catch (RaptureException e) {
        }
    }

    @Test
    public void testRetrieveReturn() {
        Assert.assertEquals("", DocumentationParser.retrieveReturn(noParam));
        Assert.assertEquals("", DocumentationParser.retrieveReturn(oneParam));
        Assert.assertEquals("", DocumentationParser.retrieveReturn(oneParamOneSince));
        Assert.assertEquals("something", DocumentationParser.retrieveReturn(oneParamOneSinceOneReturn));
        Assert.assertEquals("", DocumentationParser.retrieveReturn(longDescription));
        Assert.assertEquals("", DocumentationParser.retrieveReturn(longParamDescription));

        Assert.assertEquals("something", DocumentationParser.retrieveReturn(oneParamOneSinceOneReturn));
        assertEquals(
                "A DocWriteHandle object that contains a value on whether the write was successful, the uri of the document that was written, and a handle to"
                        + " the event uri that was fired, if any",
                DocumentationParser.retrieveReturn("Store a document in the Rapture system, passing in an event context to be added to any events spawned "
                        + "off by this put. Parts of\n"
                        + "    the uri could be automatically generated\n"
                        + "    @return A DocWriteHandle object that contains a value on whether the write was successful, the uri of the document that was "
                        + "written, "
                        + "and\n"
                        + "    a handle to the event uri that was fired, if any"));
        try {
            DocumentationParser.retrieveReturn(malformedParams);
            fail("Should have raised RaptureException for malformed params");
        } catch (RaptureException e) {
        }
        try {
            DocumentationParser.retrieveReturn(incorrectTags1);
            fail("Should have raised RaptureException for incorrect tags");
        } catch (RaptureException e) {
        }
        try {
            DocumentationParser.retrieveReturn(incorrectTags2);
            fail("Should have raised RaptureException for incorrect tags");
        } catch (RaptureException e) {
        }
        try {
            DocumentationParser.retrieveReturn(incorrectTags3);
            fail("Should have raised RaptureException for incorrect tags");
        } catch (RaptureException e) {
        }
        try {
            DocumentationParser.retrieveReturn(incorrectTags4);
            fail("Should have raised RaptureException for incorrect tags");
        } catch (RaptureException e) {
        }
    }
}