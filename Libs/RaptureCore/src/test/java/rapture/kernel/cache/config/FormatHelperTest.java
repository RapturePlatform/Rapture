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
package rapture.kernel.cache.config;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class FormatHelperTest {
    FormatHelper helper = new FormatHelper();

    @Test
    public void testGetNumExpected() throws Exception {
        String template = "NREP {} USING MONGODB {prefix=\"%s\"}";
        assertEquals(1, helper.getNumExpected(template));
        template = "REP {} USING MEMORY {}";
        assertEquals(0, helper.getNumExpected(template));
        template = "NREP {} USING CASSANDRA { keyspace=\"rapture\", cf=\"%s\", rowPrefix=\"%s\"}";
        assertEquals(2, helper.getNumExpected(template));
        template = "NREP {} USING CASSANDRA { keyspace=\"rapture\", cf=\"\", rowPrefix=\"\"}";
        assertEquals(0, helper.getNumExpected(template));
    }

    @Test
    public void testPadExtras() throws Exception {
        List<String> original = Arrays.asList("a", "B");
        List<String> padded = helper.padExtras(3, original);
        assertEquals(3, padded.size());
        assertEquals("a", padded.get(0));
        assertEquals("B", padded.get(1));
        assertEquals("", padded.get(2));
    }

    @Test
    public void testFillInPlaceholders() throws Exception {
        String template = "REP {} USING MEMORY {}";
        assertEquals("REP {} USING MEMORY {}", helper.fillInPlaceholders(template));

        template = "NREP {} USING MONGODB {prefix=\"\"}";
        assertEquals("NREP {} USING MONGODB {prefix=\"%s\"}", helper.fillInPlaceholders(template));
        template = "NREP {} USING MONGODB {prefix=\"%s\"}";
        assertEquals("NREP {} USING MONGODB {prefix=\"%s\"}", helper.fillInPlaceholders(template));

        template = "NREP {} USING CASSANDRA { keyspace=\"rapture\", cf=\"%s\", rowPrefix=\"%s\"}";
        assertEquals("NREP {} USING CASSANDRA { keyspace=\"rapture\", cf=\"%s\", rowPrefix=\"%s\"}", helper.fillInPlaceholders(template));
        template = "NREP {} USING CASSANDRA { keyspace=\"rapture\", cf=\"\", rowPrefix=\"\"}";
        assertEquals("NREP {} USING CASSANDRA { keyspace=\"rapture\", cf=\"%s\", rowPrefix=\"%s\"}", helper.fillInPlaceholders(template));

    }
}