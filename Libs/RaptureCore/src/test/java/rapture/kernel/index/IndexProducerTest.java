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
package rapture.kernel.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import rapture.common.model.IndexConfig;
import rapture.dsl.idef.IndexDefinition;
import rapture.dsl.idef.IndexDefinitionFactory;
import rapture.index.IndexProducer;
import rapture.index.IndexRecord;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class IndexProducerTest {
    @Test
    public void testProducer() {
        IndexConfig config = new IndexConfig();
        config.setName("Test");
        config.setConfig("field1($0) string, field2(a.b) string");

        IndexDefinition definition = IndexDefinitionFactory.getDefinition(config.getConfig());
        IndexProducer producer = new IndexProducer(Collections.singletonList(definition));
        String uriPath = "Level1/Level2";
        String doc = "{ \"a\" : { \"b\" : \"Here is the value\" } }";
        List<IndexRecord> indexRecords = producer.getIndexRecords(uriPath, doc, null);
        assertEquals(1, indexRecords.size());
        IndexRecord record = indexRecords.get(0);
        System.out.println(record);
        Map<String, Object> ret = record.getValues();
        assertEquals("Here is the value", ret.get("field2"));
        assertEquals("Level1", ret.get("field1"));
    }

    @Test
    public void testProducerList() {
        IndexConfig config = new IndexConfig();
        config.setName("Test");
        config.setConfig("field1($0) string, field2(a.b) string, field3(a.list.1.fieldInList) string");

        IndexDefinition definition = IndexDefinitionFactory.getDefinition(config.getConfig());
        IndexProducer producer = new IndexProducer(Collections.singletonList(definition));
        String uriPath = "Level1/Level2";
        String doc = "{ \"a\" : { \"b\" : \"Here is the value\", \"list\" : [{\"fieldInList\" : \"dontgetme\"},{\"fieldInList\" : \"getme\"} ] } }";
        List<IndexRecord> indexRecords = producer.getIndexRecords(uriPath, doc, null);
        assertEquals(1, indexRecords.size());
        IndexRecord record = indexRecords.get(0);
        System.out.println(record);
        Map<String, Object> ret = record.getValues();
        assertEquals("Here is the value", ret.get("field2"));
        assertEquals("Level1", ret.get("field1"));
        assertEquals("getme", ret.get("field3"));
    }

    @Test
    public void testProducerAllInList() {
        IndexConfig config = new IndexConfig();
        config.setName("Test");
        config.setConfig("field1($0) string, field2(a.b) string, field3(a.list.*.fieldInList) string");

        IndexDefinition definition = IndexDefinitionFactory.getDefinition(config.getConfig());
        IndexProducer producer = new IndexProducer(Collections.singletonList(definition));
        String uriPath = "Level1/Level2";
        String doc = "{ \"a\" : { \"b\" : \"Here is the value\", \"list\" : [{\"fieldInList\" : \"getme\"},{\"fieldInList\" : \"getmetoo\"} ] } }";
        List<IndexRecord> indexRecords = producer.getIndexRecords(uriPath, doc, null);
        assertEquals(1, indexRecords.size());
        IndexRecord record = indexRecords.get(0);
        System.out.println(record);
        Map<String, Object> ret = record.getValues();
        assertEquals("Here is the value", ret.get("field2"));
        assertEquals("Level1", ret.get("field1"));
        assertTrue(ret.get("field3") instanceof List<?>);
        @SuppressWarnings("unchecked")
        List<String> vals = (List<String>) ret.get("field3");
        assertEquals(2, vals.size());
        assertEquals("getme", vals.get(0));
        assertEquals("getmetoo", vals.get(1));
    }
}
