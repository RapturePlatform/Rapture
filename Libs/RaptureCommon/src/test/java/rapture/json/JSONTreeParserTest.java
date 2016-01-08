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
package rapture.json;

import org.antlr.runtime.*;
import org.junit.Test;

import rapture.generated.JSONTree;

import java.io.*;
import java.util.*;

public class JSONTreeParserTest extends AbstractJSONTests {

    @Test
    public void testString() throws IOException, RecognitionException {
        testViaTreeParser("\"testing\"", "testing");
    }

    @Test
    public void testUri() throws IOException, RecognitionException {
        testViaTreeParser("\"http://www.someuri.com\"", "http://www.someuri.com");
    }

    @Test
    public void testStringWithNewine() throws IOException, RecognitionException {
        testViaTreeParser("\"new\\nline\"", "new\nline");
    }

    @Test
    public void testStringWithUnicode() throws IOException, RecognitionException {
        testViaTreeParser("\" \\u007E \"", " ~ "); // \u007E is a tilde.
    }


    @Test
    public void testBooleans() throws IOException, RecognitionException {
        testViaTreeParser("true", Boolean.TRUE);
        testViaTreeParser("false", Boolean.FALSE);
    }

    @Test
    public void testNull() throws IOException, RecognitionException {
        JSONTree parser = createTreeParser("null");
        Object result = parser.value();
        assert result == null : "Expected null, found " + result;
    }

    @Test
    public void testInteger() throws IOException, RecognitionException {
        testViaTreeParser("12345", new Integer(12345));
        testViaTreeParser("-12345", new Integer(-12345));
    }

    @Test
    public void testFloat() throws IOException, RecognitionException {
        testViaTreeParser("123.45", new Double(123.45));
        testViaTreeParser("-123.45", new Double(-123.45));
    }

    @Test
    public void testObject() throws IOException, RecognitionException {
        JSONTree parser = createTreeParser("{\"one\":2,\"two\":3}");
        @SuppressWarnings("unchecked")
        Map<String, Integer> result = (Map<String, Integer>) parser.value();
        assert result != null : "null result";
        int firstValue = (Integer) result.get("one");
        assert firstValue == 2 : "Expected integer 2 at key \"one\" but found " + firstValue;
        int secondValue = (Integer) result.get("two");
        assert secondValue == 3 : "Expected integer 3 at key \"two\" but found " + secondValue;
    }


    @Test
    public void testArray() throws IOException, RecognitionException {
        JSONTree parser = createTreeParser("[\"one\",2]");
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) parser.value();
        assert result != null : "null result";
        String firstValue = (String) result.get(0);
        assert "one".equals(firstValue) : "Expected \"one\" at (0) but found " + firstValue;
        int secondValue = (Integer) result.get(1);
        assert secondValue == 2 : "Expected integer 2 at (1) but found " + secondValue;
    }
    

    @Test(expected = NoViableAltException.class)
    public void testSyntaxError() throws RecognitionException, IOException {
        JSONTree parser = createTreeParser("[\"one\n\",]");
        parser.value();
    }

    protected void testViaTreeParser(String testString, Object expected) throws IOException, RecognitionException {
        JSONTree parser = createTreeParser(testString);
        Object result = parser.value();
        assert expected.equals(result) : "Expected " + expected + " but found " + result;
    }

}
